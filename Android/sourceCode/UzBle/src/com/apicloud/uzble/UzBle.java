package com.apicloud.uzble;

import java.util.Map;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import com.apicloud.uzble.BleUtil.BLESDK;
import com.uzmap.pkg.uzcore.UZWebView;
import com.uzmap.pkg.uzcore.uzmodule.UZModule;
import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;

@SuppressLint("NewApi")
public class UzBle extends UZModule {
	private IBle mIBle;
	private BluetoothAdapter mBluetoothAdapter;
	private boolean mIsBleServiceAlive;

	public UzBle(UZWebView webView) {
		super(webView);
	}

	public void jsmethod_initManager(UZModuleContext moduleContext) {
		if (!BleUtil.isBlePermission(mContext)) {
			initCallBack(moduleContext, "unauthorized");
		} else if (!BleUtil.isBleSupported(mContext)) {
			initCallBack(moduleContext, "unsupported");
		} else {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			switch (mBluetoothAdapter.getState()) {
			case BluetoothAdapter.STATE_OFF:
				initCallBack(moduleContext, "poweredOff");
				break;
			case BluetoothAdapter.STATE_ON:
				initCallBack(moduleContext, "poweredOn");
				break;
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

	public void jsmethod_scan(UZModuleContext moduleContext) {
		if (mIsBleServiceAlive) {
			initIBle();
			mIBle.scan(getUUIDS(moduleContext));
		}
		statusCallBack(moduleContext, mIsBleServiceAlive);
	}

	public void jsmethod_getPeripheral(UZModuleContext moduleContext) {
		if (mIBle != null) {
			Map<String, BleDeviceInfo> deviceMap = mIBle.getPeripheral();
			getPeripheralCallBack(moduleContext, deviceMap);
		} else {
			getPeripheralCallBack(moduleContext, null);
		}
	}

	public void jsmethod_isScanning(UZModuleContext moduleContext) {
		if (mIBle != null) {
			statusCallBack(moduleContext, mIBle.isScanning());
		} else {
			statusCallBack(moduleContext, false);
		}
	}

	public void jsmethod_stopScan(UZModuleContext moduleContext) {
		if (mIBle != null) {
			mIBle.stopScan();
		}
	}

	public void jsmethod_connect(UZModuleContext moduleContext) {
		if (mIBle != null) {
			String address = moduleContext.optString("peripheralUUID");
			mIBle.connect(moduleContext, address);
		}
	}

	public void jsmethod_connectPeripherals(UZModuleContext moduleContext) {
		if (mIBle != null) {
			JSONArray address = moduleContext.optJSONArray("peripheralUUIDs");
			mIBle.connectPeripherals(moduleContext, address);
		}
	}

	public void jsmethod_disconnect(UZModuleContext moduleContext) {
		if (mIBle != null) {
			String address = moduleContext.optString("peripheralUUID");
			mIBle.disconnect(moduleContext, address);
		}
	}

	public void jsmethod_isConnected(UZModuleContext moduleContext) {
		String address = moduleContext.optString("peripheralUUID");
		if (mIBle != null) {
			isConnectedCallBack(moduleContext, mIBle.isConnected(address),
					address);
		} else {
			isConnectedCallBack(moduleContext, false, address);
		}
	}

	public void jsmethod_discoverService(UZModuleContext moduleContext) {
		if (mIBle != null) {
			String address = moduleContext.optString("peripheralUUID");
			mIBle.discoverService(moduleContext, address);
		} else {
			errcodeCallBack(moduleContext, 2);
		}
	}

	public void jsmethod_discoverCharacteristics(UZModuleContext moduleContext) {
		if (mIBle != null) {
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
			mIBle.discoverCharacteristics(moduleContext, address, serviceUUID);
		} else {
			errcodeCallBack(moduleContext, 4);
		}
	}

	public void jsmethod_discoverDescriptorsForCharacteristic(
			UZModuleContext moduleContext) {
		if (mIBle != null) {
			String address = moduleContext.optString("peripheralUUID");
			String serviceUUID = moduleContext.optString("serviceUUID");
			String characteristicUUID = moduleContext
					.optString("characteristicUUID");
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
			mIBle.discoverDescriptorsForCharacteristic(moduleContext, address,
					serviceUUID, characteristicUUID);
		} else {
			errcodeCallBack(moduleContext, 6);
		}
	}

	public void jsmethod_setNotify(UZModuleContext moduleContext) {
		if (mIBle != null) {
			String address = moduleContext.optString("peripheralUUID");
			String serviceUUID = moduleContext.optString("serviceUUID");
			String characteristicUUID = moduleContext
					.optString("characteristicUUID");
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
			mIBle.setNotify(moduleContext, address, serviceUUID,
					characteristicUUID);
		} else {
			errcodeCallBack(moduleContext, 6);
		}
	}

	public void jsmethod_setSimpleNotify(UZModuleContext moduleContext) {
		if (mIBle != null) {
			String address = moduleContext.optString("peripheralUUID");
			String serviceUUID = moduleContext.optString("serviceUUID");
			String characteristicUUID = moduleContext
					.optString("characteristicUUID");
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
			mIBle.setSimpleNotify(moduleContext, address, serviceUUID,
					characteristicUUID);
		} else {
			errcodeCallBack(moduleContext, 6);
		}
	}

	public void jsmethod_getAllSimpleNotifyData(UZModuleContext moduleContext) {
		if (mIBle != null) {
			mIBle.getAllSimpleNotifyData(moduleContext);
		}
	}

	public void jsmethod_clearAllSimpleNotifyData(UZModuleContext moduleContext) {
		if (mIBle != null) {
			mIBle.clearAllSimpleNotifyData();
		}
	}

	public void jsmethod_readValueForCharacteristic(
			UZModuleContext moduleContext) {
		if (mIBle != null) {
			String address = moduleContext.optString("peripheralUUID");
			String serviceUUID = moduleContext.optString("serviceUUID");
			String characteristicUUID = moduleContext
					.optString("characteristicUUID");
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
			mIBle.readValueForCharacteristic(moduleContext, address,
					serviceUUID, characteristicUUID);
		} else {
			errcodeCallBack(moduleContext, 6);
		}
	}

	public void jsmethod_readValueForDescriptor(UZModuleContext moduleContext) {
		if (mIBle != null) {
			String address = moduleContext.optString("peripheralUUID");
			String serviceUUID = moduleContext.optString("serviceUUID");
			String characteristicUUID = moduleContext
					.optString("characteristicUUID");
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
			mIBle.readValueForDescriptor(moduleContext, address, serviceUUID,
					characteristicUUID, descriptorUUID);
		} else {
			errcodeCallBack(moduleContext, 8);
		}
	}

	public void jsmethod_writeValueForCharacteristic(
			UZModuleContext moduleContext) {
		if (mIBle != null) {
			String address = moduleContext.optString("peripheralUUID");
			String serviceUUID = moduleContext.optString("serviceUUID");
			String characteristicUUID = moduleContext
					.optString("characteristicUUID");
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
			if (value == null || value.length() == 0) {
				errcodeCallBack(moduleContext, 4);
				return;
			}
			mIBle.writeValueForCharacteristic(moduleContext, address,
					serviceUUID, characteristicUUID, value);
		} else {
			errcodeCallBack(moduleContext, 7);
		}
	}

	public void jsmethod_writeValueForDescriptor(UZModuleContext moduleContext) {
		if (mIBle != null) {
			String address = moduleContext.optString("peripheralUUID");
			String serviceUUID = moduleContext.optString("serviceUUID");
			String characteristicUUID = moduleContext
					.optString("characteristicUUID");
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
			mIBle.writeValueForDescriptor(moduleContext, address, serviceUUID,
					characteristicUUID, descriptorUUID, value);
		} else {
			errcodeCallBack(moduleContext, 9);
		}
	}

	private void initIBle() {
		BLESDK sdk = BleUtil.getBleSDK(mContext);
		if (mIBle == null) {
			if (sdk == BLESDK.ANDROID) {
				mIBle = new AndroidBle(mContext);
			} else if (sdk == BLESDK.SAMSUNG) {
			} else if (sdk == BLESDK.BROADCOM) {
			} else {
				return;
			}
		}
	}

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

	private UUID[] getUUIDS(UZModuleContext moduleContext) {
		JSONArray serviceUUIDs = moduleContext.optJSONArray("serviceUUIDs");
		if (serviceUUIDs != null && serviceUUIDs.length() > 0) {
			UUID[] uuids = new UUID[serviceUUIDs.length()];
			for (int i = 0; i < serviceUUIDs.length(); i++) {
				uuids[i] = UUID.fromString(serviceUUIDs.optString(i));
			}
			return uuids;
		}
		return null;
	}

	public void getPeripheralCallBack(UZModuleContext moduleContext,
			Map<String, BleDeviceInfo> deviceMap) {
		JSONObject ret = new JSONObject();
		JSONArray peripherals = new JSONArray();
		try {
			ret.put("peripherals", peripherals);
			if (deviceMap != null) {
				for (Map.Entry<String, BleDeviceInfo> entry : deviceMap
						.entrySet()) {
					JSONObject peripheral = new JSONObject();
					BleDeviceInfo bleDeviceInfo = entry.getValue();
					peripheral.put("uuid", bleDeviceInfo.getBluetoothDevice()
							.getAddress());
					peripheral.put("name", bleDeviceInfo.getBluetoothDevice()
							.getName());
					peripheral.put("rssi", bleDeviceInfo.getRssi());
					peripherals.put(peripheral);
				}
			}
			moduleContext.success(ret, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void statusCallBack(UZModuleContext moduleContext, boolean status) {
		JSONObject ret = new JSONObject();
		try {
			ret.put("status", status);
			moduleContext.success(ret, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void isConnectedCallBack(UZModuleContext moduleContext,
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
}
