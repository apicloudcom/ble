package com.apicloud.uzble;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import com.samsung.android.sdk.bt.gatt.BluetoothGatt;
import com.samsung.android.sdk.bt.gatt.BluetoothGattAdapter;
import com.samsung.android.sdk.bt.gatt.BluetoothGattCallback;
import com.samsung.android.sdk.bt.gatt.BluetoothGattCharacteristic;
import com.samsung.android.sdk.bt.gatt.BluetoothGattDescriptor;
import com.samsung.android.sdk.bt.gatt.BluetoothGattService;
import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;

public class SamsungBle implements IBle {
	public static final UUID DESC_CCC = UUID
			.fromString("00002902-0000-1000-8000-00805f9b34fb");
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothGatt mBluetoothGatt;
	private Map<String, UZModuleContext> mConnectCallBackMap;
	private Map<String, UZModuleContext> mDiscoverServiceCallBackMap;
	private Map<String, UZModuleContext> mNotifyCallBackMap;
	private Map<String, UZModuleContext> mReadCharacteristicCallBackMap;
	private Map<String, UZModuleContext> mWriteCharacteristicCallBackMap;
	private Map<String, UZModuleContext> mReadDescriptorCallBackMap;
	private Map<String, UZModuleContext> mWriteDescriptorCallBackMap;
	private Map<String, BleDeviceInfo> mScanBluetoothDeviceMap;
	private Map<String, List<BluetoothGattService>> mServiceMap;
	private boolean mIsScanning;

	public SamsungBle(Context context) {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		BluetoothGattAdapter.getProfileProxy(context, mProfileServiceListener,
				BluetoothGattAdapter.GATT);
		mScanBluetoothDeviceMap = new HashMap<String, BleDeviceInfo>();
		mConnectCallBackMap = new HashMap<String, UZModuleContext>();
		mDiscoverServiceCallBackMap = new HashMap<String, UZModuleContext>();
		mServiceMap = new HashMap<String, List<BluetoothGattService>>();
		mNotifyCallBackMap = new HashMap<String, UZModuleContext>();
		mReadCharacteristicCallBackMap = new HashMap<String, UZModuleContext>();
		mReadDescriptorCallBackMap = new HashMap<String, UZModuleContext>();
		mWriteCharacteristicCallBackMap = new HashMap<String, UZModuleContext>();
		mWriteDescriptorCallBackMap = new HashMap<String, UZModuleContext>();
	}

	@Override
	public void scan(UUID[] uuids) {
		if (mBluetoothGatt != null) {
			if (uuids != null) {
				mBluetoothGatt.startScan(uuids);
			} else {
				mBluetoothGatt.startScan();
			}
			mIsScanning = true;
		}
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
		if (mBluetoothGatt != null) {
			mBluetoothGatt.stopScan();
			mIsScanning = false;
		}
	}

	@Override
	public void connect(UZModuleContext moduleContext, String address) {
		mConnectCallBackMap.put(address, moduleContext);
		if (address == null || address.length() == 0) {
			connectCallBack(moduleContext, false, 1);
			return;
		}
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		if (!mBluetoothGatt.connect(device, false)) {
			connectCallBack(moduleContext, false, 2);
		}
	}

	@Override
	public void disconnect(UZModuleContext moduleContext, String address) {
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		if (device != null) {
			mBluetoothGatt.cancelConnection(device);
			disconnectCallBack(moduleContext, true);
		} else {
			disconnectCallBack(moduleContext, false);
		}
	}

	@Override
	public boolean isConnected(String address) {
		return mConnectCallBackMap.containsKey(address);
	}

	@Override
	public void discoverService(UZModuleContext moduleContext, String address) {
		if (address == null || address.length() == 0) {
			discoverServiceCallBack(moduleContext, null, false, 1);
			return;
		}
		if (mBluetoothGatt != null) {
			mDiscoverServiceCallBackMap.put(address, moduleContext);
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
			if (device != null) {
				mBluetoothGatt.discoverServices(device);
			} else {
				discoverServiceCallBack(moduleContext, null, false, 2);
			}
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
					@SuppressWarnings("unchecked")
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
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		if (device == null) {
			errcodeCallBack(moduleContext, 6);
			return;
		}
		BluetoothGattService service = mBluetoothGatt.getService(device,
				UUID.fromString(serviceUUID));
		if (service == null) {
			errcodeCallBack(moduleContext, 5);
			return;
		}
		BluetoothGattCharacteristic characteristic = service
				.getCharacteristic(UUID.fromString(characteristicUUID));
		if (characteristic == null) {
			errcodeCallBack(moduleContext, 4);
			return;
		}
		if (!mBluetoothGatt.setCharacteristicNotification(characteristic, true)) {
			errcodeCallBack(moduleContext, -1);
			return;
		}
		BluetoothGattDescriptor descriptor = characteristic
				.getDescriptor(DESC_CCC);
		if (descriptor == null) {
			return;
		}
		mBluetoothGatt.readDescriptor(descriptor);
	}

	@Override
	public void readValueForCharacteristic(UZModuleContext moduleContext,
			String address, String serviceUUID, String characteristicUUID) {
		mReadCharacteristicCallBackMap.put(characteristicUUID, moduleContext);
		if (mBluetoothGatt != null) {
			BluetoothGattCharacteristic characteristic = characteristic(
					moduleContext, address, serviceUUID, characteristicUUID);
			if (characteristic != null) {
				boolean status = mBluetoothGatt
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
		BluetoothGattDescriptor descriptor = descriptor(moduleContext, address,
				serviceUUID, characteristicUUID, descriptorUUID);
		if (mBluetoothGatt != null) {
			if (descriptor != null) {
				if (!mBluetoothGatt.readDescriptor(descriptor)) {
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
		if (mBluetoothGatt != null) {
			BluetoothGattCharacteristic characteristic = characteristicWrite(
					moduleContext, address, serviceUUID, characteristicUUID);
			if (characteristic != null) {
				characteristic.setValue(value);
				boolean status = mBluetoothGatt
						.writeCharacteristic(characteristic);
				if (!status) {
					errcodeCallBack(moduleContext, -1);
				}
			}
		}
	}

	@Override
	public void writeValueForDescriptor(UZModuleContext moduleContext,
			String address, String serviceUUID, String characteristicUUID,
			String descriptorUUID, String value) {
		mWriteDescriptorCallBackMap.put(descriptorUUID, moduleContext);
		BluetoothGattDescriptor descriptor = descriptorWrite(moduleContext,
				address, serviceUUID, characteristicUUID, descriptorUUID);
		if (mBluetoothGatt != null) {
			if (descriptor != null) {
				try {
					descriptor.setValue(Hex.decodeHex(value.toCharArray()));
					if (!mBluetoothGatt.writeDescriptor(descriptor)) {
						errcodeCallBack(moduleContext, -1);
					}
				} catch (DecoderException e) {
					e.printStackTrace();
					errcodeCallBack(moduleContext, -1);
				}
			} else {
				errcodeCallBack(moduleContext, 6);
			}
		}
	}

	private final BluetoothProfile.ServiceListener mProfileServiceListener = new BluetoothProfile.ServiceListener() {
		@Override
		public void onServiceConnected(int profile, BluetoothProfile proxy) {
			mBluetoothGatt = (BluetoothGatt) proxy;
			mBluetoothGatt.registerApp(mGattCallbacks);
		}

		@Override
		public void onServiceDisconnected(int profile) {
			mBluetoothGatt = null;
		}
	};

	private final BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {

		@Override
		public void onCharacteristicChanged(
				BluetoothGattCharacteristic characteristic) {
			onCharacteristic(mNotifyCallBackMap, characteristic);
		}

		@Override
		public void onCharacteristicRead(
				BluetoothGattCharacteristic characteristic, int arg1) {
			onCharacteristic(mReadCharacteristicCallBackMap, characteristic);
		}

		@Override
		public void onCharacteristicWrite(
				BluetoothGattCharacteristic characteristic, int arg1) {
			onCharacteristic(mWriteCharacteristicCallBackMap, characteristic);
		}

		@Override
		public void onConnectionStateChange(BluetoothDevice device, int status,
				int newState) {
			String address = device.getAddress();
			UZModuleContext moduleContext = mConnectCallBackMap.get(address);
			if (status != BluetoothGatt.GATT_SUCCESS) {
				connectCallBack(moduleContext, false, -1);
				return;
			}
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				connectCallBack(moduleContext, true, 0);
			} else {
				mConnectCallBackMap.remove(address);
				connectCallBack(moduleContext, false, -1);
			}
		}

		@Override
		public void onDescriptorRead(BluetoothGattDescriptor descriptor,
				int arg1) {
			onDescript(mReadDescriptorCallBackMap, descriptor);
		}

		@Override
		public void onDescriptorWrite(BluetoothGattDescriptor descriptor,
				int arg1) {
			onDescript(mWriteDescriptorCallBackMap, descriptor);
		}

		@Override
		public void onScanResult(BluetoothDevice device, int rssi, byte[] arg2) {
			mScanBluetoothDeviceMap.put(device.getAddress(), new BleDeviceInfo(
					device, rssi));
		}

		@Override
		public void onServicesDiscovered(BluetoothDevice device, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				String address = device.getAddress();
				@SuppressWarnings("unchecked")
				List<BluetoothGattService> service = mBluetoothGatt
						.getServices(device);
				mServiceMap.put(address, service);
				discoverServiceCallBack(
						mDiscoverServiceCallBackMap.get(address), service,
						true, 0);
			}
		}
	};

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

	private void disconnectCallBack(UZModuleContext moduleContext,
			boolean status) {
		JSONObject ret = new JSONObject();
		try {
			ret.put("status", status);
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

	@SuppressWarnings("unchecked")
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

	private String permissions(int permission) {
		switch (permission) {
		case BluetoothGattCharacteristic.PERMISSION_READ:
			return "readable";
		case BluetoothGattCharacteristic.PERMISSION_WRITE:
			return "writeable";
		case BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED:
			return "readEncryptionRequired";
		case BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED:
			return "writeEncryptionRequired";
		}
		return String.valueOf(permission);
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

	private void onCharacteristic(Map<String, UZModuleContext> map,
			BluetoothGattCharacteristic characteristic) {
		UZModuleContext moduleContext = map.get(characteristic.getUuid()
				.toString());
		if (moduleContext != null)
			characteristicCallBack(moduleContext, characteristic);
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

	private void onDescript(Map<String, UZModuleContext> map,
			BluetoothGattDescriptor descriptor) {
		UZModuleContext moduleContext = map
				.get(descriptor.getUuid().toString());
		if (moduleContext != null)
			descriptorCallBack(moduleContext, descriptor, descriptor
					.getCharacteristic().getService().getUuid().toString(),
					descriptor.getCharacteristic().getUuid().toString());
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

	private BluetoothGattCharacteristic characteristic(
			UZModuleContext moduleContext, String address, String serviceUUID,
			String characteristicUUID) {
		List<BluetoothGattService> services = mServiceMap.get(address);
		if (services != null) {
			for (BluetoothGattService service : services) {
				if (service.getUuid().toString().equals(serviceUUID)) {
					@SuppressWarnings("unchecked")
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
					@SuppressWarnings("unchecked")
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
					@SuppressWarnings("unchecked")
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
					@SuppressWarnings("unchecked")
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
}
