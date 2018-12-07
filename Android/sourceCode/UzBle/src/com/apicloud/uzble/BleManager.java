package com.apicloud.uzble;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.content.Context;
import android.util.Log;

import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;

/***
 * 单例模式;
 * 
 * @author baoch
 */
public class BleManager {
	/** 单例实例对象 **/
	private static BleManager InStance;
	/** 蓝牙开启的成功标识 **/
	private boolean mIsBleServiceAlive;
	/** 得到本地的蓝牙适配器 **/
	private BluetoothAdapter mBluetoothAdapter;
	/** 标识是否正在扫描 **/
	private boolean mIsScanning;
	public static final UUID DESC_CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	// 将连接成功的设备mac地址和设备的管道（BluetoothGatt）保存起来；
	private Map<String, BluetoothGatt> mBluetoothGattMap;
	// 将h5传进来的要链接的设备参数保存在这里面；
	private Map<String, UZModuleContext> mConnectCallBackMap;
	private Map<String, UZModuleContext> mConnectsCallBackMap;
	// 已经连接的设备的mac,以及我们回调回去的变量；
	private Map<String, UZModuleContext> mDiscoverServiceCallBackMap;
	// 连接成功后能够获取到的可用服务列表集合；

	private Map<String, List<BluetoothGattService>> mServiceMap;
	private Map<String, UZModuleContext> mNotifyCallBackMap;
	private List<Ble> mSimpleNotifyCallBackMap;
	private Map<String, UZModuleContext> mReadCharacteristicCallBackMap;
	private Map<String, UZModuleContext> mWriteCharacteristicCallBackMap;
	private Map<String, UZModuleContext> mReadDescriptorCallBackMap;
	private Map<String, UZModuleContext> mWriteDescriptorCallBackMap;
	private Map<String, BleDeviceInfo> mScanBluetoothDeviceMap;
	private JSONObject mNotifyData;
	private BluetoothGatt bluetoothGatt;

	private String TAG;

	private BleManager() {
		// 得到蓝牙适配器
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mBluetoothGattMap = new HashMap<String, BluetoothGatt>();
		mScanBluetoothDeviceMap = new HashMap<String, BleDeviceInfo>();
		mConnectCallBackMap = new HashMap<String, UZModuleContext>();
		mDiscoverServiceCallBackMap = new HashMap<String, UZModuleContext>();
		mServiceMap = new HashMap<String, List<BluetoothGattService>>();
		mNotifyCallBackMap = new HashMap<String, UZModuleContext>();
		mReadCharacteristicCallBackMap = new HashMap<String, UZModuleContext>();
		mReadDescriptorCallBackMap = new HashMap<String, UZModuleContext>();
		mWriteCharacteristicCallBackMap = new HashMap<String, UZModuleContext>();
		mWriteDescriptorCallBackMap = new HashMap<String, UZModuleContext>();
		mSimpleNotifyCallBackMap = new ArrayList<Ble>();
		mConnectsCallBackMap = new HashMap<String, UZModuleContext>();
		mNotifyData = new JSONObject();
		TAG = BleManager.class.getSimpleName();

	}

	public static BleManager getInstance() {

		if (InStance == null) {
			synchronized (BleManager.class) {
				if (InStance == null) {
					InStance = new BleManager();

				}
			}

		}
		return InStance;

	}

	/***
	 * 初始化蓝牙设备;
	 * 
	 * @param context
	 *            ()
	 */
	public void init(UZModuleContext moduleContext, Context mContext) {
		/** 判断是否有蓝牙权限 **/
		if (!BleUtil.isBlePermission(mContext)) {
			initCallBack(moduleContext, "unauthorized");
			/** 判断api版本是否支持 **/
		} else if (!BleUtil.isBleSupported(mContext)) {
			/** 回调安卓版本是否支持 **/
			initCallBack(moduleContext, "unsupported");
		} else {
			int hashCode = mBluetoothAdapter.hashCode();
			L.i("mBluetoothAdapter", hashCode + "");
			switch (mBluetoothAdapter.getState()) {
			// 设备关闭状态
			case BluetoothAdapter.STATE_OFF:
				// 如果关闭状态要手动打开；
				initCallBack(moduleContext, "poweredOff");
				break;
			// 设备开启状态
			case BluetoothAdapter.STATE_ON:
				initCallBack(moduleContext, "poweredOn");
				break;
			// 重新制定设备；
			case BluetoothAdapter.STATE_TURNING_OFF:
			case BluetoothAdapter.STATE_TURNING_ON:
			case BluetoothAdapter.STATE_CONNECTING:
			case BluetoothAdapter.STATE_DISCONNECTING:
				initCallBack(moduleContext, "resetting");
				break;
			default:
				initCallBack(moduleContext, "unknown");
				break;
			}
		}
	}

	/****
	 * 扫描;
	 * 
	 * @param moduleContext
	 * @param context
	 *            ()
	 */
	@SuppressWarnings("deprecation")
	public void scan(UZModuleContext moduleContext) {
		if (mIsBleServiceAlive) {
			// 要扫描的蓝牙4.0设备的服务（service）的 UUID（字符串） 组成的数组，若不传则扫描附近的所有支持蓝牙4.0的设备
			UUID[] uuids = getUUIDS(moduleContext);
			if (uuids != null) {
				mBluetoothAdapter.startLeScan(uuids, mLeScanCallback);
			} else {
				mBluetoothAdapter.startLeScan(mLeScanCallback);
			}
			mIsScanning = true;
		}
		statusCallBack(moduleContext, mIsBleServiceAlive);
	}

	/****
	 * 要扫描的蓝牙4.0设备的服务（service）的 UUID（字符串） 组成的数组
	 * 
	 * @param moduleContext
	 * @return
	 */
	private UUID[] getUUIDS(UZModuleContext moduleContext) {
		// 由h5传递过来的serviceUUIDs
		JSONArray serviceUUIDs = moduleContext.optJSONArray("serviceUUIDs");
		// L.i("指定的扫描蓝牙设备的uuid:serviceUUIDs", serviceUUIDs.toString()+"");
		if (serviceUUIDs != null && serviceUUIDs.length() > 0) {
			UUID[] uuids = new UUID[serviceUUIDs.length()];
			for (int i = 0; i < serviceUUIDs.length(); i++) {
				uuids[i] = UUID.fromString(serviceUUIDs.optString(i));
			}
			return uuids;
		}
		return null;
	}

	private LeScanCallback mLeScanCallback = new LeScanCallback() {

		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			String strScanRecord = new String(Hex.encodeHex(scanRecord));
			BleDeviceInfo info = new BleDeviceInfo(device, rssi, strScanRecord);
		     info.deviceName = BleUtil.parseAdertisedData(scanRecord).getName();
			mScanBluetoothDeviceMap.put(device.getAddress(), info);
		}
	};

	/****
	 * getPeripheral
	 * 
	 * @param moduleContext
	 */
	public void getPeripheral(UZModuleContext moduleContext) {
		int hashCode = hashCode();
		L.i("getPeripheral", hashCode + "");
		getPeripheralCallBack(moduleContext, mScanBluetoothDeviceMap);

	}

	/***
	 * 是否正在扫描;
	 * 
	 * @param moduleContext
	 */
	public void isScanning(UZModuleContext moduleContext) {
		statusCallBack(moduleContext, mIsScanning);
	}

	/****
	 * 停止扫描;
	 * 
	 * @param moduleContext
	 */
	@SuppressWarnings("deprecation")
	public void stopScan(UZModuleContext moduleContext) {
		mBluetoothAdapter.stopLeScan(mLeScanCallback);
		mIsScanning = false;

	}
	


	/****
	 * 连接
	 * 
	 * @param moduleContext
	 * @param context
	 *            ()
	 */
	@SuppressLint("NewApi")
	public void connect(UZModuleContext moduleContext, final Context mContext) {
		String address = moduleContext.optString("peripheralUUID");
		this.mConnectCallBackMap.put(address, moduleContext);
		if ((address == null) || (address.length() == 0)) {
			connectCallBack(moduleContext, false, 1, "null address");
			return;
		}
		try {
			if (this.bluetoothGatt != null) {
				this.bluetoothGatt.disconnect();
				this.bluetoothGatt.close();
			}
			final BluetoothDevice device = this.mBluetoothAdapter.getRemoteDevice(address);

			((Activity) mContext).runOnUiThread(new Runnable() {
				public void run() {
					bluetoothGatt = device.connectGatt(mContext, false, mBluetoothGattCallback);
					bluetoothGatt.requestMtu(512);
					L.i(TAG, "Trying to create a new connection.");
				}
			});
		} catch (Exception e) {
			connectCallBack(moduleContext, false, 2, "Exception");
		}
	}

	/***
	 * 连接回调监听;
	 */
	private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			String address = gatt.getDevice().getAddress();
			UZModuleContext moduleContext = mConnectCallBackMap.get(address);
			if (status != 0) {
				gatt.close();
				mConnectCallBackMap.remove(address);
				connectCallBack(moduleContext, false, -1, "status:" + status);
				return;
			}
			if (newState == 2) {
				mBluetoothGattMap.put(address, gatt);
				connectCallBack(moduleContext, true, 0, "success");
				Log.e("走到这里了", "连接成功");
			} else if (newState == 0) {
				gatt.close();
				mBluetoothGattMap.remove(address);
				mConnectCallBackMap.remove(address);
				connectCallBack(moduleContext, false, -1, "newState:" + newState);
			}
		}

		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == 0) {
				String address = gatt.getDevice().getAddress();

				List<BluetoothGattService> service = gatt.getServices();

				mServiceMap.put(address, service);
				discoverServiceCallBack(mDiscoverServiceCallBackMap.get(address), service, true, 0);
			} else {
				String address = gatt.getDevice().getAddress();
				discoverServiceCallBack(mDiscoverServiceCallBackMap.get(address), null, false, status);
			}
		}

		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			onCharacteristic(mReadCharacteristicCallBackMap, characteristic, false);
		}

		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			onCharacteristic(mWriteCharacteristicCallBackMap, characteristic, false);
		}

		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			if (getBle(gatt, characteristic) != null) {
				characteristicSimpleCallBack(getBle(gatt, characteristic), characteristic);
			} else {
				onCharacteristic(mNotifyCallBackMap, characteristic, false);
			}
		}

		public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			onDescript(mReadDescriptorCallBackMap, descriptor);
		}

		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			onDescript(mWriteDescriptorCallBackMap, descriptor);
		}
	};

	private void onCharacteristic(Map<String, UZModuleContext> map, BluetoothGattCharacteristic characteristic, boolean isSimple) {
		UZModuleContext moduleContext = (UZModuleContext) map.get(characteristic.getUuid().toString());
		if (moduleContext != null) {
			characteristicCallBack(moduleContext, characteristic);
		}
	}

	private void characteristicCallBack(UZModuleContext moduleContext, BluetoothGattCharacteristic characteristic) {
		JSONObject ret = new JSONObject();
		JSONObject characteristicJson = new JSONObject();
		try {
			ret.put("status", true);
			ret.put("characteristic", characteristicJson);
			characteristicJson.put("uuid", characteristic.getUuid());
			characteristicJson.put("serviceUUID", characteristic.getService().getUuid().toString());
			characteristicJson.put("permissions", permissions(characteristic.getPermissions()));
			characteristicJson.put("propertie", properties(characteristic.getProperties()));
			characteristicJson.put("value", new String(Hex.encodeHex(characteristic.getValue())));
			moduleContext.success(ret, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void connectPeripherals(UZModuleContext moduleContext, Context mContext) {
		JSONArray address = moduleContext.optJSONArray("peripheralUUIDs");
		for (int i = 0; i < address.length(); i++) {
			this.mConnectsCallBackMap.put(address.optString(i), moduleContext);
			if ((address == null) || (address.length() == 0)) {
				connectCallBack(moduleContext, false, 1, "null adress");
				return;
			}
			try {
				BluetoothDevice device = this.mBluetoothAdapter.getRemoteDevice(address.optString(i));
				device.connectGatt(mContext, false, this.mBluetoothGattCallback);
			} catch (Exception e) {
				connectsCallBack(moduleContext, false, 2, address.optString(i));
			}
		}

	}

	/***
	 * 断开连接;
	 * 
	 * @param moduleContext
	 */
	public void disconnect(UZModuleContext moduleContext) {
		String address = moduleContext.optString("peripheralUUID");
		BluetoothGatt bluetoothGatt = (BluetoothGatt) this.mBluetoothGattMap.get(address);
		if (bluetoothGatt != null) {
			bluetoothGatt.disconnect();

			bluetoothGatt.close();
			this.mBluetoothGattMap.remove(address);
			this.mServiceMap.remove(address);
			remove2NotifyMap(address);
			disconnectCallBack(moduleContext, true, address);
		} else {
			disconnectCallBack(moduleContext, false, address);
		}
	}

	/**
	 * 判断当前连接的蓝牙的状态；
	 * 
	 * @param moduleContext
	 */
	public void isConnected(UZModuleContext moduleContext) {
		String address = moduleContext.optString("peripheralUUID");
		boolean isConnected = mBluetoothGattMap.containsKey(address);
		isConnectedCallBack(moduleContext, isConnected, address);
	}

	/***
	 * 发现服务;
	 * 
	 * @param moduleContext
	 */
	public void discoverService(UZModuleContext moduleContext) {
		String address = moduleContext.optString("peripheralUUID");

		// 从保存连接设备的集合中拿出连接成功的设备的（管道）
		BluetoothGatt bluetoothGatt = mBluetoothGattMap.get(address);
		if (address == null || address.length() == 0) {
			// 没有传入指定的外围设备 UUID
			discoverServiceCallBack(moduleContext, null, false, 1);
			return;
		}
		if (bluetoothGatt != null) {

			mDiscoverServiceCallBackMap.put(address, moduleContext);
			bluetoothGatt.discoverServices();
		} else {

			// 设备处于未连接状态；
			discoverServiceCallBack(moduleContext, null, false, 2);
		}

	}

	public void discoverCharacteristics(UZModuleContext moduleContext) {
		String address = moduleContext.optString("peripheralUUID");
		String serviceUUID = moduleContext.optString("serviceUUID");
		if (address == null || address.length() == 0) {
			errcodeCallBack(moduleContext, 1);
			return;
		}
		if (serviceUUID == null || serviceUUID.length() == 0) {
			errcodeCallBack(moduleContext, 2);
			return;
		}
		List<BluetoothGattCharacteristic> characteristics = characteristics(address, serviceUUID);
		if (characteristics == null) {
			errcodeCallBack(moduleContext, 3);
		} else {
			characteristicCallBack(moduleContext, characteristics);
		}

	}

	private List<BluetoothGattCharacteristic> characteristics(String address, String serviceUUID) {
		List<BluetoothGattService> services = mServiceMap.get(address);
		if (services != null) {
			for (BluetoothGattService service : services) {
				if (service.getUuid().toString().equals(serviceUUID)) {
					return service.getCharacteristics();
				}
			}
		}
		return null;
	}

	public void discoverDescriptorsForCharacteristic(UZModuleContext moduleContext) {

		String address = moduleContext.optString("peripheralUUID");
		String serviceUUID = moduleContext.optString("serviceUUID");
		String characteristicUUID = moduleContext.optString("characteristicUUID");
		if (address == null || address.length() == 0) {
			errcodeCallBack(moduleContext, 1);
			return;
		}
		if (serviceUUID == null || serviceUUID.length() == 0) {
			errcodeCallBack(moduleContext, 2);
			return;
		}
		if (characteristicUUID == null || characteristicUUID.length() == 0) {
			errcodeCallBack(moduleContext, 3);
			return;
		}

		List<BluetoothGattCharacteristic> characteristics = characteristics(address, serviceUUID);
		if (characteristics == null) {
			errcodeCallBack(moduleContext, 5);
		} else {
			for (BluetoothGattCharacteristic characteristic : characteristics) {
				if (characteristic.getUuid().toString().equals(characteristicUUID)) {
					List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
					descriptorsCallBack(moduleContext, descriptors, serviceUUID, characteristicUUID);
					return;
				}
			}
			errcodeCallBack(moduleContext, 4);
		}
	}

	/***
	 * 通知;
	 * 
	 * @param moduleContext
	 */
	public void setNotify(UZModuleContext moduleContext) {
		String address = moduleContext.optString("peripheralUUID");
		String serviceUUID = moduleContext.optString("serviceUUID");
		String characteristicUUID = moduleContext.optString("characteristicUUID");
		if (address == null || address.length() == 0) {
			errcodeCallBack(moduleContext, 1);
			return;
		}
		if (serviceUUID == null || serviceUUID.length() == 0) {
			errcodeCallBack(moduleContext, 2);
			return;
		}
		if (characteristicUUID == null || characteristicUUID.length() == 0) {
			errcodeCallBack(moduleContext, 3);
			return;
		}

		mNotifyCallBackMap.put(characteristicUUID, moduleContext);
		BluetoothGatt bluetoothGatt = mBluetoothGattMap.get(address);
		if (bluetoothGatt != null) {
			BluetoothGattCharacteristic characteristic = characteristic(moduleContext, address, serviceUUID, characteristicUUID);
			if (characteristic != null) {
				boolean status = bluetoothGatt.setCharacteristicNotification(characteristic, true);
				if (status) {
					BluetoothGattDescriptor descriptor = characteristic.getDescriptor(DESC_CCC);
					if (descriptor == null) {
						errcodeCallBack(moduleContext, -1);
					} else {
						if (!descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
							errcodeCallBack(moduleContext, -1);
						} else {
							bluetoothGatt.writeDescriptor(descriptor);
						}
					}
				} else {
					errcodeCallBack(moduleContext, -1);
				}
			}
		}
	}

	public void setSimpleNotify(UZModuleContext moduleContext) {
		String address = moduleContext.optString("peripheralUUID");
		String serviceUUID = moduleContext.optString("serviceUUID");
		String characteristicUUID = moduleContext.optString("characteristicUUID");
		if (address == null || address.length() == 0) {
			errcodeCallBack(moduleContext, 1);
			return;
		}
		if (serviceUUID == null || serviceUUID.length() == 0) {
			errcodeCallBack(moduleContext, 2);
			return;
		}
		if (characteristicUUID == null || characteristicUUID.length() == 0) {
			errcodeCallBack(moduleContext, 3);
			return;
		}

		mSimpleNotifyCallBackMap.add(new Ble(address, serviceUUID, characteristicUUID, moduleContext));
		BluetoothGatt bluetoothGatt = mBluetoothGattMap.get(address);
		if (bluetoothGatt != null) {
			BluetoothGattCharacteristic characteristic = characteristic(moduleContext, address, serviceUUID, characteristicUUID);
			if (characteristic != null) {
				boolean status = bluetoothGatt.setCharacteristicNotification(characteristic, true);
				if (status) {
					BluetoothGattDescriptor descriptor = characteristic.getDescriptor(DESC_CCC);
					if (descriptor == null) {
						errcodeCallBack(moduleContext, -1);
					} else {
						if (!descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
							errcodeCallBack(moduleContext, -1);
						} else {
							bluetoothGatt.writeDescriptor(descriptor);
						}
					}
				} else {
					errcodeCallBack(moduleContext, -1);
				}
			}
		}
	}

	public void getAllSimpleNotifyData(UZModuleContext moduleContext) {
		moduleContext.success(mNotifyData, false);
	}

	public void clearAllSimpleNotifyData(UZModuleContext moduleContext) {
		mNotifyData = new JSONObject();
	}

	public void readValueForCharacteristic(UZModuleContext moduleContext) {
		String address = moduleContext.optString("peripheralUUID");
		String serviceUUID = moduleContext.optString("serviceUUID");
		String characteristicUUID = moduleContext.optString("characteristicUUID");
		if (address == null || address.length() == 0) {
			errcodeCallBack(moduleContext, 1);
			return;
		}
		if (serviceUUID == null || serviceUUID.length() == 0) {
			errcodeCallBack(moduleContext, 2);
			return;
		}
		if (characteristicUUID == null || characteristicUUID.length() == 0) {
			errcodeCallBack(moduleContext, 3);
			return;
		}

		mReadCharacteristicCallBackMap.put(characteristicUUID, moduleContext);
		BluetoothGatt bluetoothGatt = mBluetoothGattMap.get(address);
		if (bluetoothGatt != null) {
			// 获取特征;
			BluetoothGattCharacteristic characteristic = characteristic(moduleContext, address, serviceUUID, characteristicUUID);
			if (characteristic != null) {
				boolean status = bluetoothGatt.readCharacteristic(characteristic);
				if (!status) {
					errcodeCallBack(moduleContext, -1);
				}
			}
		}
	}

	public void readValueForDescriptor(UZModuleContext moduleContext) {
		String address = moduleContext.optString("peripheralUUID");
		String serviceUUID = moduleContext.optString("serviceUUID");
		String characteristicUUID = moduleContext.optString("characteristicUUID");
		String descriptorUUID = moduleContext.optString("descriptorUUID");
		if (address == null || address.length() == 0) {
			errcodeCallBack(moduleContext, 1);
			return;
		}
		if (serviceUUID == null || serviceUUID.length() == 0) {
			errcodeCallBack(moduleContext, 2);
			return;
		}
		if (characteristicUUID == null || characteristicUUID.length() == 0) {
			errcodeCallBack(moduleContext, 3);
			return;
		}
		if (descriptorUUID == null || descriptorUUID.length() == 0) {
			errcodeCallBack(moduleContext, 4);
			return;
		}
		mReadDescriptorCallBackMap.put(descriptorUUID, moduleContext);
		BluetoothGatt bluetoothGatt = mBluetoothGattMap.get(address);
		BluetoothGattDescriptor descriptor = descriptor(moduleContext, address, serviceUUID, characteristicUUID, descriptorUUID);
		if (bluetoothGatt != null) {
			if (descriptor != null) {
				if (!bluetoothGatt.readDescriptor(descriptor)) {
					errcodeCallBack(moduleContext, -1);
				}
			} else {
				errcodeCallBack(moduleContext, 5);
			}
		}
	}

	public void writeValueForCharacteristic(UZModuleContext moduleContext) {
		String address = moduleContext.optString("peripheralUUID");
		String serviceUUID = moduleContext.optString("serviceUUID");
		String characteristicUUID = moduleContext.optString("characteristicUUID");
		String value = moduleContext.optString("value");
		String writeType = moduleContext.optString("writeType", "auto");
		if (address == null || address.length() == 0) {
			errcodeCallBack(moduleContext, 1);
			return;
		}
		if (serviceUUID == null || serviceUUID.length() == 0) {
			errcodeCallBack(moduleContext, 2);
			return;
		}
		if (characteristicUUID == null || characteristicUUID.length() == 0) {
			errcodeCallBack(moduleContext, 3);
			return;
		}
		if (value == null || value.length() == 0) {
			errcodeCallBack(moduleContext, 4);
			return;
		}
		int intWriteType = 2;
		if (writeType.equals("response")) {
			intWriteType = 4;
		} else if (writeType.equals("withoutResponse")) {
			intWriteType = 1;
		}

		this.mWriteCharacteristicCallBackMap.put(characteristicUUID, moduleContext);
		BluetoothGatt bluetoothGatt = (BluetoothGatt) this.mBluetoothGattMap.get(address);
		if (bluetoothGatt != null) {
			BluetoothGattCharacteristic characteristic = characteristicWrite(moduleContext, address, serviceUUID, characteristicUUID, intWriteType);
			if (characteristic != null) {
				characteristic.setValue(value(value));
				boolean status = bluetoothGatt.writeCharacteristic(characteristic);
				if (!status) {
					errcodeCallBack(moduleContext, -1);
				}
			}
		}
	}

	private BluetoothGattCharacteristic characteristic(UZModuleContext moduleContext, String address, String serviceUUID, String characteristicUUID) {
		List<BluetoothGattService> services = mServiceMap.get(address);
		if (services != null) {
			for (BluetoothGattService service : services) {
				if (service.getUuid().toString().equals(serviceUUID)) {
					List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
					if (characteristics != null) {
						for (BluetoothGattCharacteristic characteristic : characteristics) {
							if (characteristic.getUuid().toString().equals(characteristicUUID)) {
								return characteristic;
							}
						}
						errcodeCallBack(moduleContext, 4);
						return null;
					}
					errcodeCallBack(moduleContext, 4);
					return null;
				}
			}
			errcodeCallBack(moduleContext, 5);
			return null;
		}
		errcodeCallBack(moduleContext, 5);
		return null;
	}

	private BluetoothGattCharacteristic characteristicWrite(UZModuleContext moduleContext, String address, String serviceUUID, String characteristicUUID, int writeType) {
		List<BluetoothGattService> services = mServiceMap.get(address);
		if (services != null) {
			for (BluetoothGattService service : services) {
				if (service.getUuid().toString().equals(serviceUUID)) {
					List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
					if (characteristics != null) {
						for (BluetoothGattCharacteristic characteristic : characteristics) {

							if (characteristic.getUuid().toString().equals(characteristicUUID)) {
								characteristic.setWriteType(writeType);
								return characteristic;
							}
						}
						errcodeCallBack(moduleContext, 5);
						return null;
					}
					errcodeCallBack(moduleContext, 5);
					return null;
				}
			}
			errcodeCallBack(moduleContext, 6);
			return null;
		}
		errcodeCallBack(moduleContext, 6);
		return null;
	}

	public BluetoothGattCharacteristic getCharacteristic(String address, String serviceUUID, String characteristicUUID) {
		List<BluetoothGattService> services = mServiceMap.get(address);
		if (services != null) {
			for (BluetoothGattService service : services) {
				if (service.getUuid().toString().equals(serviceUUID)) {
					List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
					if (characteristics != null) {
						for (BluetoothGattCharacteristic characteristic : characteristics) {

							if (characteristic.getUuid().toString().equals(characteristicUUID)) {
								return characteristic;
							}
						}
						return null;
					}

					return null;
				}
			}
			return null;
		}
		return null;
	}

	public void writeValueForDescriptor(UZModuleContext moduleContext) {

		String address = moduleContext.optString("peripheralUUID");
		String serviceUUID = moduleContext.optString("serviceUUID");
		String characteristicUUID = moduleContext.optString("characteristicUUID");
		String descriptorUUID = moduleContext.optString("descriptorUUID");
		String value = moduleContext.optString("value");
		if (address == null || address.length() == 0) {
			errcodeCallBack(moduleContext, 1);
			return;
		}
		if (serviceUUID == null || serviceUUID.length() == 0) {
			errcodeCallBack(moduleContext, 2);
			return;
		}
		if (characteristicUUID == null || characteristicUUID.length() == 0) {
			errcodeCallBack(moduleContext, 3);
			return;
		}
		if (descriptorUUID == null || descriptorUUID.length() == 0) {
			errcodeCallBack(moduleContext, 4);
			return;
		}
		if (value == null || value.length() == 0) {
			errcodeCallBack(moduleContext, 5);
			return;
		}
		this.mWriteDescriptorCallBackMap.put(descriptorUUID, moduleContext);
		BluetoothGatt bluetoothGatt = (BluetoothGatt) this.mBluetoothGattMap.get(address);
		BluetoothGattDescriptor descriptor = descriptorWrite(moduleContext, address, serviceUUID, characteristicUUID, descriptorUUID);
		if (bluetoothGatt != null) {
			if (descriptor != null) {
				descriptor.setValue(value(value));
				if (!bluetoothGatt.writeDescriptor(descriptor)) {
					errcodeCallBack(moduleContext, -1);
				}
			} else {
				errcodeCallBack(moduleContext, 6);
			}
		}

	}

	// UZModuleContext moduleContext
	// FIXME
	private void remove2NotifyMap(String address) {
		if (mSimpleNotifyCallBackMap == null)
			return;
		for (Ble ble : mSimpleNotifyCallBackMap) {
			if (ble.getPeripheralUUID().equals(address)) {
				mSimpleNotifyCallBackMap.remove(ble);
			}
		}
	}

	private BluetoothGattDescriptor descriptor(UZModuleContext moduleContext, String address, String serviceUUID, String characteristicUUID, String descriptorUUID) {
		List<BluetoothGattService> services = mServiceMap.get(address);
		if (services != null) {
			for (BluetoothGattService service : services) {
				if (service.getUuid().toString().equals(serviceUUID)) {
					List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
					if (characteristics != null) {
						for (BluetoothGattCharacteristic characteristic : characteristics) {
							if (characteristic.getUuid().toString().equals(characteristicUUID)) {
								return characteristic.getDescriptor(UUID.fromString(descriptorUUID));
							}
						}
						errcodeCallBack(moduleContext, 6);
						return null;
					}
					errcodeCallBack(moduleContext, 6);
					return null;
				}
			}
			errcodeCallBack(moduleContext, 7);
			return null;
		}
		errcodeCallBack(moduleContext, 7);
		return null;
	}

	private BluetoothGattDescriptor descriptorWrite(UZModuleContext moduleContext, String address, String serviceUUID, String characteristicUUID, String descriptorUUID) {
		List<BluetoothGattService> services = mServiceMap.get(address);
		if (services != null) {
			for (BluetoothGattService service : services) {
				if (service.getUuid().toString().equals(serviceUUID)) {
					List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
					if (characteristics != null) {
						for (BluetoothGattCharacteristic characteristic : characteristics) {
							if (characteristic.getUuid().toString().equals(characteristicUUID)) {
								return characteristic.getDescriptor(UUID.fromString(descriptorUUID));
							}
						}
						errcodeCallBack(moduleContext, 7);
						return null;
					}
					errcodeCallBack(moduleContext, 7);
					return null;
				}
			}
			errcodeCallBack(moduleContext, 8);
			return null;
		}
		errcodeCallBack(moduleContext, 8);
		return null;
	}

	/***
	 * 转换成byte数组;
	 * 
	 * @param valueStr
	 * @return
	 */
	private byte[] value(String valueStr) {
		// 传进来的是十六进制字符串;
		byte[] value = new byte[valueStr.length() / 2];
		for (int i = 0; i < value.length; i++) {
			if (2 * i + 1 < valueStr.length()) {
				value[i] = Integer.valueOf(valueStr.substring(2 * i, 2 * i + 2), 16).byteValue();
			} else {
				value[i] = Integer.valueOf(String.valueOf(valueStr.charAt(2 * i)), 16).byteValue();
			}
		}
		return value;
	}

	@SuppressWarnings("unused")
	private void onCharacteristic(Map<String, UZModuleContext> map, BluetoothGattCharacteristic characteristic, boolean isSimple, int status) {
		UZModuleContext moduleContext = map.get(characteristic.getUuid().toString());
		if (moduleContext != null)
			characteristicCallBack(moduleContext, characteristic, isSimple, status);
	}

	private Ble getBle(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		for (Ble ble : mSimpleNotifyCallBackMap) {
			if (ble.getPeripheralUUID().equals(gatt.getDevice().getAddress()) && ble.getServiceId().equals(characteristic.getService().getUuid().toString())) {
				return ble;
			}
		}
		return null;
	}

	private void onDescript(Map<String, UZModuleContext> map, BluetoothGattDescriptor descriptor) {
		UZModuleContext moduleContext = map.get(descriptor.getUuid().toString());
		if (moduleContext != null)
			descriptorCallBack(moduleContext, descriptor, descriptor.getCharacteristic().getService().getUuid().toString(), descriptor.getCharacteristic().getUuid().toString());
	}

	private void descriptorCallBack(UZModuleContext moduleContext, BluetoothGattDescriptor descriptor, String serviceUUID, String characteristicUUID) {
		JSONObject ret = new JSONObject();
		JSONObject descriptorJson = new JSONObject();
		try {
			ret.put("status", true);
			ret.put("descriptor", descriptorJson);
			descriptorJson.put("uuid", descriptor.getUuid());
			descriptorJson.put("serviceUUID", serviceUUID);
			descriptorJson.put("characteristicUUID", characteristicUUID);
			descriptorJson.put("value", new String(Hex.encodeHex(descriptor.getValue())));
			moduleContext.success(ret, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/***
	 * 初始化的回调;
	 * 
	 * @param moduleContext
	 * @param state
	 */
	private void initCallBack(UZModuleContext moduleContext, String state) {
		JSONObject ret = new JSONObject();
		try {
			ret.put("state", state);
			mIsBleServiceAlive = state.equals("poweredOn");
			moduleContext.success(ret, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/****
	 * 扫描的回调;
	 * 
	 * @param moduleContext
	 * @param status
	 */
	private void statusCallBack(UZModuleContext moduleContext, boolean status) {
		JSONObject ret = new JSONObject();
		try {
			ret.put("status", status);
			moduleContext.success(ret, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void getPeripheralCallBack(UZModuleContext moduleContext, Map<String, BleDeviceInfo> deviceMap) {
		// 创建json对象
		JSONObject ret = new JSONObject();
		// 创建json数组；
		JSONArray peripherals = new JSONArray();
		try {
			ret.put("peripherals", peripherals);
			if (deviceMap != null) {
				for (Map.Entry<String, BleDeviceInfo> entry : deviceMap.entrySet()) {
					JSONObject peripheral = new JSONObject();
					BleDeviceInfo bleDeviceInfo = entry.getValue();
					peripheral.put("uuid", bleDeviceInfo.getBluetoothDevice().getAddress());
					peripheral.put("name", bleDeviceInfo.getBluetoothDevice().getName());

					peripheral.put("rssi", bleDeviceInfo.getRssi());
					peripheral.put("manufacturerData", bleDeviceInfo.getstrScanRecord() + "");
					peripherals.put(peripheral);
				}
			}
			moduleContext.success(ret, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/***
	 * 连接状态的回调真正走的方法；
	 * 
	 * @param moduleContext
	 * @param status
	 * @param errCode
	 * @param detailErrorCode
	 */
	// 连接状态目前走了这个回调；
	private void connectCallBack(UZModuleContext moduleContext, boolean status, int errCode, String detailErrorCode) {
		JSONObject ret = new JSONObject();
		JSONObject err = new JSONObject();
		try {
			ret.put("status", status);
			if (status) {
				L.e(TAG, "------------设备连接成功----------------");
				moduleContext.success(ret, false);
			} else {
				L.i(TAG, "设备连接失败" + "失败信息errCode" + errCode + "errCode" + "detailErrorCode" + detailErrorCode);
				err.put("code", errCode);
				err.put("detailErrorCode", detailErrorCode);
				moduleContext.error(ret, err, false);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/***
	 * 发现服务的回调
	 * 
	 * @param moduleContext
	 * @param services
	 * @param status
	 * @param errCode
	 */
	private void discoverServiceCallBack(UZModuleContext moduleContext, List<BluetoothGattService> services, boolean status, int errCode) {
		JSONObject ret = new JSONObject();
		JSONObject err = new JSONObject();
		try {
			ret.put("status", status);
			if (status) {
				JSONArray serviceArray = new JSONArray();
				for (BluetoothGattService service : services) {
					serviceArray.put(service.getUuid().toString());
				}
				ret.put("services", serviceArray);
				moduleContext.success(ret, false);
			} else {
				err.put("code", errCode);
				moduleContext.error(ret, err, false);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/***
	 * 特征回调;
	 * 
	 * @param moduleContext
	 * @param characteristic
	 */
	private void characteristicCallBack(UZModuleContext moduleContext, BluetoothGattCharacteristic characteristic, boolean status, int errCode) {
		JSONObject ret = new JSONObject();
		JSONObject err = new JSONObject();
		JSONObject characteristicJson = new JSONObject();
		try {
			ret.put("status", true);
			if (status) {
				ret.put("characteristic", characteristicJson);
				characteristicJson.put("uuid", characteristic.getUuid());
				characteristicJson.put("serviceUUID", characteristic.getService().getUuid().toString());
				characteristicJson.put("permissions", permissions(characteristic.getPermissions()));
				characteristicJson.put("propertie", properties(characteristic.getProperties()));
				characteristicJson.put("value", new String(Hex.encodeHex(characteristic.getValue())));
				moduleContext.success(ret, false);
			} else {
				err.put("code", errCode);
				moduleContext.error(ret, err, false);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void characteristicSimpleCallBack(Ble ble, BluetoothGattCharacteristic characteristic) {
		JSONObject ret = new JSONObject();
		try {
			ret.put("status", true);
			setNotifyData(characteristic, ble);
			ble.getModuleContext().success(ret, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private String permissions(int permissions) {
		switch (permissions) {
		case BluetoothGattCharacteristic.PERMISSION_READ:
			return "readable";
		case BluetoothGattCharacteristic.PERMISSION_WRITE:
			return "writeable";
		case BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED:
			return "readEncryptionRequired";
		case BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED:
			return "writeEncryptionRequired";
		}
		return String.valueOf(permissions);
	}

	private String properties(int propertie) {
		switch (propertie) {
		case BluetoothGattCharacteristic.PROPERTY_READ:
			return "read";
		case BluetoothGattCharacteristic.PROPERTY_BROADCAST:
			return "broadcast";
		case BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS:
			return "extendedProperties";
		case BluetoothGattCharacteristic.PROPERTY_INDICATE:
			return "indicate";
		case BluetoothGattCharacteristic.PROPERTY_NOTIFY:
			return "notify";
		case BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE:
			return "writeable";
		case BluetoothGattCharacteristic.PROPERTY_WRITE:
			return "write";
		case BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE:
			return "writeWithoutResponse";
		}
		return String.valueOf(propertie);
	}

	private void setNotifyData(BluetoothGattCharacteristic characteristic, Ble ble) {
		if (ble != null) {
			if (mNotifyData.isNull(ble.getPeripheralUUID())) {
				JSONObject notifyData = new JSONObject();
				try {
					notifyData.put("serviceUUID", ble.getServiceId());
					notifyData.put("characterUUID", ble.getCharacteristicUUID());
					JSONArray data = new JSONArray();
					data.put(new String(Hex.encodeHex(characteristic.getValue())));
					notifyData.put("data", data);
					mNotifyData.put(ble.getPeripheralUUID(), notifyData);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else {
				JSONObject notifyData = mNotifyData.optJSONObject(ble.getPeripheralUUID());
				JSONArray data = notifyData.optJSONArray("data");
				data.put(new String(Hex.encodeHex(characteristic.getValue())));
			}
		}
	}

	private void disconnectCallBack(UZModuleContext moduleContext, boolean status, String uuid) {
		JSONObject ret = new JSONObject();
		try {
			ret.put("status", status);
			ret.put("peripheralUUID", uuid);
			if (status) {
				L.i(TAG, "----------断开成功-----");
			}
			moduleContext.success(ret, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void connectsCallBack(UZModuleContext moduleContext, boolean status, int errCode, String uuid) {
		JSONObject ret = new JSONObject();
		JSONObject err = new JSONObject();
		try {
			ret.put("status", status);
			if (status) {
				ret.put("peripheralUUID", uuid);
				moduleContext.success(ret, false);
			} else {
				err.put("code", errCode);
				moduleContext.error(ret, err, false);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/***
	 * 连接状态的回调方法；
	 * 
	 * @param moduleContext
	 * @param status
	 * @param uuid
	 */

	private void isConnectedCallBack(UZModuleContext moduleContext, boolean status, String uuid) {
		JSONObject ret = new JSONObject();
		try {
			ret.put("status", status);
			ret.put("peripheralUUID", uuid);
			moduleContext.success(ret, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void errcodeCallBack(UZModuleContext moduleContext, int code) {
		JSONObject ret = new JSONObject();
		JSONObject err = new JSONObject();
		try {
			ret.put("status", false);
			err.put("code", code);
			moduleContext.error(ret, err, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void characteristicCallBack(UZModuleContext moduleContext, List<BluetoothGattCharacteristic> characteristics) {
		JSONObject ret = new JSONObject();
		JSONArray characteristicsJson = new JSONArray();
		try {
			ret.put("status", true);
			ret.put("characteristics", characteristicsJson);
			for (BluetoothGattCharacteristic characteristic : characteristics) {
				JSONObject item = new JSONObject();
				item.put("uuid", characteristic.getUuid());
				item.put("serviceUUID", characteristic.getService().getUuid().toString());
				item.put("permissions", permissions(characteristic.getPermissions()));
				item.put("properties", properties(characteristic.getProperties()));
				characteristicsJson.put(item);
			}
			moduleContext.success(ret, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void descriptorsCallBack(UZModuleContext moduleContext, List<BluetoothGattDescriptor> descriptors, String serviceUUID, String characteristicUUID) {
		JSONObject ret = new JSONObject();
		JSONArray descriptorsJson = new JSONArray();
		try {
			ret.put("status", true);
			ret.put("descriptors", descriptorsJson);
			for (BluetoothGattDescriptor descriptor : descriptors) {
				JSONObject item = new JSONObject();
				item.put("uuid", descriptor.getUuid());
				item.put("serviceUUID", serviceUUID);
				item.put("characteristicUUID", characteristicUUID);
				descriptorsJson.put(item);
			}
			moduleContext.success(ret, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	

	public void clean() {
		
		if (mScanBluetoothDeviceMap!=null&&mScanBluetoothDeviceMap.size()>0) {
			this.mScanBluetoothDeviceMap.clear();
		}

	}

}
