package com.apicloud.uzble;

import android.bluetooth.BluetoothDevice;

public class BleDeviceInfo {
	private BluetoothDevice bluetoothDevice;
	private int rssi;

	public BleDeviceInfo(BluetoothDevice bluetoothDevice, int rssi) {
		this.bluetoothDevice = bluetoothDevice;
		this.rssi = rssi;
	}

	public BluetoothDevice getBluetoothDevice() {
		return bluetoothDevice;
	}

	public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
		this.bluetoothDevice = bluetoothDevice;
	}

	public int getRssi() {
		return rssi;
	}

	public void setRssi(int rssi) {
		this.rssi = rssi;
	}
}
