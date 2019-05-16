package com.apicloud.uzble;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.content.Context;
import android.content.pm.PackageManager;

public class BleUtil {
	public static boolean isBlePermission(Context context) {
		PackageManager pm = context.getPackageManager();
		/***
		 * 判断应用程序是否允许程序连接到已配对的蓝牙设备访问的权限；
		 */
		boolean permission = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.BLUETOOTH", context.getPackageName()));
		return permission;
	}

	/***
	 * 判断 Android版本是否支持蓝牙；
	 * 
	 * @param context
	 * @return
	 */

	public static boolean isBleSupported(Context context) {
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			// android 4.3
			return true;
		}
		ArrayList<String> libraries = new ArrayList<String>();
		for (String i : context.getPackageManager().getSystemSharedLibraryNames()) {
			libraries.add(i);
		}
		if (android.os.Build.VERSION.SDK_INT >= 17) {
			// android 4.2.2
			if (libraries.contains("com.samsung.android.sdk.bt")) {
				return true;
			} else if (libraries.contains("com.broadcom.bt")) {
				return true;
			}
		}
		return false;
	}

	public static BLESDK getBleSDK(Context context) {
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			// android 4.3
			return BLESDK.ANDROID;
		}

		ArrayList<String> libraries = new ArrayList<String>();
		for (String i : context.getPackageManager().getSystemSharedLibraryNames()) {
			libraries.add(i);
		}

		if (android.os.Build.VERSION.SDK_INT >= 17) {
			// android 4.2.2
			if (libraries.contains("com.samsung.android.sdk.bt")) {
				return BLESDK.SAMSUNG;
			} else if (libraries.contains("com.broadcom.bt")) {
				return BLESDK.BROADCOM;
			}
		}
		return BLESDK.NOT_SUPPORTED;
	}

	public enum BLESDK {
		NOT_SUPPORTED, ANDROID, SAMSUNG, BROADCOM
	}

	public static BleAdvertisedData parseAdertisedData(byte[] advertisedData) {
		List<UUID> uuids = new ArrayList<UUID>();
		String name = null;
		if (advertisedData == null) {
			return new BleAdvertisedData(uuids, name);
		}

		ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
		while (buffer.remaining() > 2) {
			byte length = buffer.get();
			if (length == 0)
				break;

			byte type = buffer.get();
			switch (type) {
			case 0x02: // Partial list of 16-bit UUIDs
			case 0x03: // Complete list of 16-bit UUIDs
				while (length >= 2) {
					uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
					length -= 2;
				}
				break;
			case 0x06: // Partial list of 128-bit UUIDs
			case 0x07: // Complete list of 128-bit UUIDs
				while (length >= 16) {
					long lsb = buffer.getLong();
					long msb = buffer.getLong();
					uuids.add(new UUID(msb, lsb));
					length -= 16;
				}
				break;
			case 0x09:
				byte[] nameBytes = new byte[length - 1];
				buffer.get(nameBytes);
				try {
					name = new String(nameBytes, "utf-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				break;
			default:
				buffer.position(buffer.position() + length - 1);
				break;
			}
		}
		return new BleAdvertisedData(uuids, name);
	}
}
