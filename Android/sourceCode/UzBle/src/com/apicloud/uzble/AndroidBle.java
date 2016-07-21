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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;

@SuppressLint("NewApi")
@SuppressWarnings("deprecation")
public class AndroidBle implements IBle {
	public static final UUID DESC_CCC = UUID
			.fromString("00002902-0000-1000-8000-00805f9b34fb");
	private Context mContext;
	private BluetoothAdapter mBluetoothAdapter;
	private Map<String, BluetoothGatt> mBluetoothGattMap;
	private Map<String, UZModuleContext> mConnectCallBackMap;
	private Map<String, UZModuleContext> mConnectsCallBackMap;
	private Map<String, UZModuleContext> mDiscoverServiceCallBackMap;
	private Map<String, UZModuleContext> mNotifyCallBackMap;
	private List<Ble> mSimpleNotifyCallBackMap;
	private Map<String, UZModuleContext> mReadCharacteristicCallBackMap;
	private Map<String, UZModuleContext> mWriteCharacteristicCallBackMap;
	private Map<String, UZModuleContext> mReadDescriptorCallBackMap;
	private Map<String, UZModuleContext> mWriteDescriptorCallBackMap;
	private Map<String, BleDeviceInfo> mScanBluetoothDeviceMap;
	private Map<String, List<BluetoothGattService>> mServiceMap;
	private boolean mIsScanning;
	private JSONObject mNotifyData;

	public AndroidBle(Context context) {
		mContext = context;
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
	}

	@Override
	public void scan(UUID[] uuids) {
		if (uuids != null) {
			mBluetoothAdapter.startLeScan(uuids, mLeScanCallback);
		} else {
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		}
		mIsScanning = true;
	}

	@Override
	public Map<String, BleDeviceInfo> getPeripheral() {
		return mScanBluetoothDeviceMap;
	}

	@Override
	public boolean isScanning() {
		return mIsScanning;
	}

	@Override
	public void stopScan() {
		mScanBluetoothDeviceMap.clear();
		mBluetoothAdapter.stopLeScan(mLeScanCallback);
		mIsScanning = false;
	}

	@Override
	public void connect(UZModuleContext moduleContext, String address) {
		mConnectCallBackMap.put(address, moduleContext);
		if (address == null || address.length() == 0) {
			connectCallBack(moduleContext, false, 1);
			return;
		}
		try {
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
			device.connectGatt(mContext, false, mBluetoothGattCallback);
		} catch (Exception e) {
			connectCallBack(moduleContext, false, 2);
		}
	}

	@Override
	public void connectPeripherals(UZModuleContext moduleContext,
			JSONArray address) {
		for (int i = 0; i < address.length(); i++) {
			mConnectsCallBackMap.put(address.optString(i), moduleContext);
			if (address == null || address.length() == 0) {
				connectCallBack(moduleContext, false, 1);
				return;
			}
			try {
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address.optString(i));
				device.connectGatt(mContext, false, mBluetoothGattCallback);
			} catch (Exception e) {
				connectsCallBack(moduleContext, false, 2, address.optString(i));
			}
		}
	}

	@Override
	public void disconnect(UZModuleContext moduleContext, String address) {
		BluetoothGatt bluetoothGatt = mBluetoothGattMap.get(address);
		if (bluetoothGatt != null) {
			bluetoothGatt.disconnect();
			disconnectCallBack(moduleContext, true, address);
		} else {
			disconnectCallBack(moduleContext, false, address);
		}
	}

	@Override
	public boolean isConnected(String address) {
		return mConnectCallBackMap.containsKey(address)||mConnectsCallBackMap.containsKey(address);
	}

	@Override
	public void discoverService(UZModuleContext moduleContext, String address) {
		BluetoothGatt bluetoothGatt = mBluetoothGattMap.get(address);
		if (address == null || address.length() == 0) {
			discoverServiceCallBack(moduleContext, null, false, 1);
			return;
		}
		if (bluetoothGatt != null) {
			mDiscoverServiceCallBackMap.put(address, moduleContext);
			bluetoothGatt.discoverServices();
		} else {
			discoverServiceCallBack(moduleContext, null, false, 2);
		}
	}

	@Override
	public void discoverCharacteristics(UZModuleContext moduleContext,
			String address, String serviceUUID) {
		List<BluetoothGattCharacteristic> characteristics = characteristics(
				address, serviceUUID);
		if (characteristics == null) {
			errcodeCallBack(moduleContext, 3);
		} else {
			characteristicCallBack(moduleContext, characteristics);
		}
	}

	@Override
	public void discoverDescriptorsForCharacteristic(
			UZModuleContext moduleContext, String address, String serviceUUID,
			String characteristicUUID) {
		List<BluetoothGattCharacteristic> characteristics = characteristics(
				address, serviceUUID);
		if (characteristics == null) {
			errcodeCallBack(moduleContext, 5);
		} else {
			for (BluetoothGattCharacteristic characteristic : characteristics) {
				if (characteristic.getUuid().toString()
						.equals(characteristicUUID)) {
					List<BluetoothGattDescriptor> descriptors = characteristic
							.getDescriptors();
					descriptorsCallBack(moduleContext, descriptors,
							serviceUUID, characteristicUUID);
					return;
				}
			}
			errcodeCallBack(moduleContext, 4);
		}
	}

	@Override
	public void setNotify(UZModuleContext moduleContext, String address,
			String serviceUUID, String characteristicUUID) {
		mNotifyCallBackMap.put(characteristicUUID, moduleContext);
		BluetoothGatt bluetoothGatt = mBluetoothGattMap.get(address);
		if (bluetoothGatt != null) {
			BluetoothGattCharacteristic characteristic = characteristic(
					moduleContext, address, serviceUUID, characteristicUUID);
			if (characteristic != null) {
				boolean status = bluetoothGatt.setCharacteristicNotification(
						characteristic, true);
				if (status) {
					BluetoothGattDescriptor descriptor = characteristic
							.getDescriptor(DESC_CCC);
					if (descriptor == null) {
						errcodeCallBack(moduleContext, -1);
					} else {
						if (!descriptor
								.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
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

	@Override
	public void readValueForCharacteristic(UZModuleContext moduleContext,
			String address, String serviceUUID, String characteristicUUID) {
		mReadCharacteristicCallBackMap.put(characteristicUUID, moduleContext);
		BluetoothGatt bluetoothGatt = mBluetoothGattMap.get(address);
		if (bluetoothGatt != null) {
			BluetoothGattCharacteristic characteristic = characteristic(
					moduleContext, address, serviceUUID, characteristicUUID);
			if (characteristic != null) {
				boolean status = bluetoothGatt
						.readCharacteristic(characteristic);
				if (!status) {
					errcodeCallBack(moduleContext, -1);
				}
			}
		}
	}

	@Override
	public void readValueForDescriptor(UZModuleContext moduleContext,
			String address, String serviceUUID, String characteristicUUID,
			String descriptorUUID) {
		mReadDescriptorCallBackMap.put(descriptorUUID, moduleContext);
		BluetoothGatt bluetoothGatt = mBluetoothGattMap.get(address);
		BluetoothGattDescriptor descriptor = descriptor(moduleContext, address,
				serviceUUID, characteristicUUID, descriptorUUID);
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

	@Override
	public void writeValueForCharacteristic(UZModuleContext moduleContext,
			String address, String serviceUUID, String characteristicUUID,
			String value) {
		mWriteCharacteristicCallBackMap.put(characteristicUUID, moduleContext);
		BluetoothGatt bluetoothGatt = mBluetoothGattMap.get(address);
		if (bluetoothGatt != null) {
			BluetoothGattCharacteristic characteristic = characteristicWrite(
					moduleContext, address, serviceUUID, characteristicUUID);
			if (characteristic != null) {
				characteristic.setValue(value(value));
				boolean status = bluetoothGatt
						.writeCharacteristic(characteristic);
				if (!status) {
					errcodeCallBack(moduleContext, -1);
				}
			}
		}
	}

	private byte[] value(String valueStr) {
		byte[] value = new byte[valueStr.length() / 2];
		for (int i = 0; i < value.length; i++) {
			if (2 * i + 1 < valueStr.length()) {
				value[i] = Integer.valueOf(
						valueStr.substring(2 * i, 2 * i + 2), 16).byteValue();
			} else {
				value[i] = Integer.valueOf(
						String.valueOf(valueStr.charAt(2 * i)), 16).byteValue();
			}
		}
		return value;
	}

	@Override
	public void writeValueForDescriptor(UZModuleContext moduleContext,
			String address, String serviceUUID, String characteristicUUID,
			String descriptorUUID, String value) {
		mWriteDescriptorCallBackMap.put(descriptorUUID, moduleContext);
		BluetoothGatt bluetoothGatt = mBluetoothGattMap.get(address);
		BluetoothGattDescriptor descriptor = descriptorWrite(moduleContext,
				address, serviceUUID, characteristicUUID, descriptorUUID);
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

	private List<BluetoothGattCharacteristic> characteristics(String address,
			String serviceUUID) {
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

	private BluetoothGattCharacteristic characteristic(
			UZModuleContext moduleContext, String address, String serviceUUID,
			String characteristicUUID) {
		List<BluetoothGattService> services = mServiceMap.get(address);
		if (services != null) {
			for (BluetoothGattService service : services) {
				if (service.getUuid().toString().equals(serviceUUID)) {
					List<BluetoothGattCharacteristic> characteristics = service
							.getCharacteristics();
					if (characteristics != null) {
						for (BluetoothGattCharacteristic characteristic : characteristics) {
							if (characteristic.getUuid().toString()
									.equals(characteristicUUID)) {
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

	private BluetoothGattCharacteristic characteristicWrite(
			UZModuleContext moduleContext, String address, String serviceUUID,
			String characteristicUUID) {
		List<BluetoothGattService> services = mServiceMap.get(address);
		if (services != null) {
			for (BluetoothGattService service : services) {
				if (service.getUuid().toString().equals(serviceUUID)) {
					List<BluetoothGattCharacteristic> characteristics = service
							.getCharacteristics();
					if (characteristics != null) {
						for (BluetoothGattCharacteristic characteristic : characteristics) {
							if (characteristic.getUuid().toString()
									.equals(characteristicUUID)) {
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

	private BluetoothGattDescriptor descriptor(UZModuleContext moduleContext,
			String address, String serviceUUID, String characteristicUUID,
			String descriptorUUID) {
		List<BluetoothGattService> services = mServiceMap.get(address);
		if (services != null) {
			for (BluetoothGattService service : services) {
				if (service.getUuid().toString().equals(serviceUUID)) {
					List<BluetoothGattCharacteristic> characteristics = service
							.getCharacteristics();
					if (characteristics != null) {
						for (BluetoothGattCharacteristic characteristic : characteristics) {
							if (characteristic.getUuid().toString()
									.equals(characteristicUUID)) {
								return characteristic.getDescriptor(UUID
										.fromString(descriptorUUID));
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

	private BluetoothGattDescriptor descriptorWrite(
			UZModuleContext moduleContext, String address, String serviceUUID,
			String characteristicUUID, String descriptorUUID) {
		List<BluetoothGattService> services = mServiceMap.get(address);
		if (services != null) {
			for (BluetoothGattService service : services) {
				if (service.getUuid().toString().equals(serviceUUID)) {
					List<BluetoothGattCharacteristic> characteristics = service
							.getCharacteristics();
					if (characteristics != null) {
						for (BluetoothGattCharacteristic characteristic : characteristics) {
							if (characteristic.getUuid().toString()
									.equals(characteristicUUID)) {
								return characteristic.getDescriptor(UUID
										.fromString(descriptorUUID));
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

	private LeScanCallback mLeScanCallback = new LeScanCallback() {

		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			mScanBluetoothDeviceMap.put(device.getAddress(), new BleDeviceInfo(
					device, rssi));
		}
	};

	private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			String address = gatt.getDevice().getAddress();
			if (mConnectsCallBackMap.containsKey(address)) {
				if (status != BluetoothGatt.GATT_SUCCESS) {
					connectsCallBack(mConnectsCallBackMap.get(address), false,
							-1, address);
					mConnectsCallBackMap.remove(address);
					return;
				}
				if (newState == BluetoothProfile.STATE_CONNECTED) {
					mBluetoothGattMap.put(address, gatt);
					connectsCallBack(mConnectsCallBackMap.get(address), true,
							0, address);
				} else {
					mBluetoothGattMap.remove(address);
					connectsCallBack(mConnectsCallBackMap.get(address), false,
							-1, address);
					mConnectsCallBackMap.remove(address);
				}
				return;
			}
			UZModuleContext moduleContext = mConnectCallBackMap.get(address);
			if (status != BluetoothGatt.GATT_SUCCESS) {
				mConnectCallBackMap.remove(address);
				connectCallBack(moduleContext, false, -1);
				return;
			}
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				mBluetoothGattMap.put(address, gatt);
				connectCallBack(moduleContext, true, 0);
			} else {
				mBluetoothGattMap.remove(address);
				mConnectCallBackMap.remove(address);
				connectCallBack(moduleContext, false, -1);
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				String address = gatt.getDevice().getAddress();
				List<BluetoothGattService> service = gatt.getServices();
				mServiceMap.put(address, service);
				discoverServiceCallBack(
						mDiscoverServiceCallBackMap.get(address), service,
						true, 0);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			onCharacteristic(mReadCharacteristicCallBackMap, characteristic,
					false);
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			onCharacteristic(mWriteCharacteristicCallBackMap, characteristic,
					false);
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			if (getBle(gatt, characteristic) != null) {
				characteristicSimpleCallBack(getBle(gatt, characteristic),
						characteristic);
			} else {
				onCharacteristic(mNotifyCallBackMap, characteristic, false);
			}
		}

		@Override
		public void onDescriptorRead(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
			onDescript(mReadDescriptorCallBackMap, descriptor);
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
			onDescript(mWriteDescriptorCallBackMap, descriptor);
		}
	};

	private Ble getBle(BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic) {
		for (Ble ble : mSimpleNotifyCallBackMap) {
			if (ble.getPeripheralUUID().equals(gatt.getDevice().getAddress())
					&& ble.getServiceId().equals(
							characteristic.getService().getUuid().toString())) {
				return ble;
			}
		}
		return null;
	}

	private void onCharacteristic(Map<String, UZModuleContext> map,
			BluetoothGattCharacteristic characteristic, boolean isSimple) {
		UZModuleContext moduleContext = map.get(characteristic.getUuid()
				.toString());
		if (moduleContext != null)
			characteristicCallBack(moduleContext, characteristic);
	}

	private void onDescript(Map<String, UZModuleContext> map,
			BluetoothGattDescriptor descriptor) {
		UZModuleContext moduleContext = map
				.get(descriptor.getUuid().toString());
		if (moduleContext != null)
			descriptorCallBack(moduleContext, descriptor, descriptor
					.getCharacteristic().getService().getUuid().toString(),
					descriptor.getCharacteristic().getUuid().toString());
	}

	private void connectCallBack(UZModuleContext moduleContext, boolean status,
			int errCode) {
		JSONObject ret = new JSONObject();
		JSONObject err = new JSONObject();
		try {
			ret.put("status", status);
			if (status) {
				moduleContext.success(ret, false);
			} else {
				err.put("code", errCode);
				moduleContext.error(ret, err, false);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void connectsCallBack(UZModuleContext moduleContext,
			boolean status, int errCode, String uuid) {
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

	public void connectsCallBack(UZModuleContext moduleContext,
			BluetoothDevice device, boolean status,
			JSONArray mConnectedDeviceMap) {
		JSONObject ret = new JSONObject();
		try {
			ret.put("status", status);
			ret.put("peripheralUUID", device.getAddress());
			moduleContext.success(ret, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void discoverServiceCallBack(UZModuleContext moduleContext,
			List<BluetoothGattService> services, boolean status, int errCode) {
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

	private void disconnectCallBack(UZModuleContext moduleContext,
			boolean status, String uuid) {
		JSONObject ret = new JSONObject();
		try {
			ret.put("status", status);
			ret.put("peripheralUUID", uuid);
			moduleContext.success(ret, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void characteristicCallBack(UZModuleContext moduleContext,
			List<BluetoothGattCharacteristic> characteristics) {
		JSONObject ret = new JSONObject();
		JSONArray characteristicsJson = new JSONArray();
		try {
			ret.put("status", true);
			ret.put("characteristics", characteristicsJson);
			for (BluetoothGattCharacteristic characteristic : characteristics) {
				JSONObject item = new JSONObject();
				item.put("uuid", characteristic.getUuid());
				item.put("serviceUUID", characteristic.getService().getUuid()
						.toString());
				item.put("permissions",
						permissions(characteristic.getPermissions()));
				item.put("propertie",
						properties(characteristic.getProperties()));
				characteristicsJson.put(item);
			}
			moduleContext.success(ret, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void characteristicCallBack(UZModuleContext moduleContext,
			BluetoothGattCharacteristic characteristic) {
		JSONObject ret = new JSONObject();
		JSONObject characteristicJson = new JSONObject();
		try {
			ret.put("status", true);
			ret.put("characteristic", characteristicJson);
			characteristicJson.put("uuid", characteristic.getUuid());
			characteristicJson.put("serviceUUID", characteristic.getService()
					.getUuid().toString());
			characteristicJson.put("permissions",
					permissions(characteristic.getPermissions()));
			characteristicJson.put("propertie",
					properties(characteristic.getProperties()));
			characteristicJson.put("value",
					new String(Hex.encodeHex(characteristic.getValue())));
			moduleContext.success(ret, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void characteristicSimpleCallBack(Ble ble,
			BluetoothGattCharacteristic characteristic) {
		JSONObject ret = new JSONObject();
		try {
			ret.put("status", true);
			setNotifyData(characteristic, ble);
			ble.getModuleContext().success(ret, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void setNotifyData(BluetoothGattCharacteristic characteristic,
			Ble ble) {
		if (ble != null) {
			if (mNotifyData.isNull(ble.getPeripheralUUID())) {
				JSONObject notifyData = new JSONObject();
				try {
					notifyData.put("serviceUUID", ble.getServiceId());
					notifyData
							.put("characterUUID", ble.getCharacteristicUUID());
					JSONArray data = new JSONArray();
					data.put(new String(
							Hex.encodeHex(characteristic.getValue())));
					notifyData.put("data", data);
					mNotifyData.put(ble.getPeripheralUUID(), notifyData);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else {
				JSONObject notifyData = mNotifyData.optJSONObject(ble
						.getPeripheralUUID());
				JSONArray data = notifyData.optJSONArray("data");
				data.put(new String(Hex.encodeHex(characteristic.getValue())));
			}
		}
	}

	private void descriptorsCallBack(UZModuleContext moduleContext,
			List<BluetoothGattDescriptor> descriptors, String serviceUUID,
			String characteristicUUID) {
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

	private void descriptorCallBack(UZModuleContext moduleContext,
			BluetoothGattDescriptor descriptor, String serviceUUID,
			String characteristicUUID) {
		JSONObject ret = new JSONObject();
		JSONObject descriptorJson = new JSONObject();
		try {
			ret.put("status", true);
			ret.put("descriptor", descriptorJson);
			descriptorJson.put("uuid", descriptor.getUuid());
			descriptorJson.put("serviceUUID", serviceUUID);
			descriptorJson.put("characteristicUUID", characteristicUUID);
			descriptorJson.put("value",
					new String(Hex.encodeHex(descriptor.getValue())));
			moduleContext.success(ret, false);
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

	@Override
	public void setSimpleNotify(UZModuleContext moduleContext, String address,
			String serviceUUID, String characteristicUUID) {
		mSimpleNotifyCallBackMap.add(new Ble(address, serviceUUID,
				characteristicUUID, moduleContext));
		BluetoothGatt bluetoothGatt = mBluetoothGattMap.get(address);
		if (bluetoothGatt != null) {
			BluetoothGattCharacteristic characteristic = characteristic(
					moduleContext, address, serviceUUID, characteristicUUID);
			if (characteristic != null) {
				boolean status = bluetoothGatt.setCharacteristicNotification(
						characteristic, true);
				if (status) {
					BluetoothGattDescriptor descriptor = characteristic
							.getDescriptor(DESC_CCC);
					if (descriptor == null) {
						errcodeCallBack(moduleContext, -1);
					} else {
						if (!descriptor
								.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
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

	@Override
	public void getAllSimpleNotifyData(UZModuleContext moduleContext) {
		moduleContext.success(mNotifyData, false);
	}

	@Override
	public void clearAllSimpleNotifyData() {
		mNotifyData = new JSONObject();
	}
}
