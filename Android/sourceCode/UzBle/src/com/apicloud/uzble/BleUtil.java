package com.apicloud.uzble;

import java.util.ArrayList;
import android.content.Context;
import android.content.pm.PackageManager;

public class BleUtil {
	public static boolean isBlePermission(Context context) {
		PackageManager pm = context.getPackageManager();
		/***
		 * 判断应用程序是否允许程序连接到已配对的蓝牙设备访问的权限；
		 */
		boolean permission = (PackageManager.PERMISSION_GRANTED == pm
				.checkPermission("android.permission.BLUETOOTH",
						context.getPackageName()));
		return permission;
	}
	
	/***
	 *  判断 Android版本是否支持蓝牙；
	 * @param context
	 * @return
	 */

	public static boolean isBleSupported(Context context) {
		if (context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			// android 4.3
			return true;
		}
		ArrayList<String> libraries = new ArrayList<String>();
		for (String i : context.getPackageManager()
				.getSystemSharedLibraryNames()) {
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
		if (context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			// android 4.3
			return BLESDK.ANDROID;
		}

		ArrayList<String> libraries = new ArrayList<String>();
		for (String i : context.getPackageManager()
				.getSystemSharedLibraryNames()) {
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
}
