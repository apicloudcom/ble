package com.apicloud.uzble;
import android.bluetooth.BluetoothDevice;

public class BleDeviceInfo {
	//扫描设备的device
	private BluetoothDevice bluetoothDevice;
	//信号强度;
	private int rssi;
	//广播包数据
	private String strScanRecord;
	public BleDeviceInfo(BluetoothDevice bluetoothDevice, int rssi,String strScanRecord) {
		this.bluetoothDevice = bluetoothDevice;
		this.rssi = rssi;
		this.strScanRecord=strScanRecord;
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
	
	public String getstrScanRecord() {
		return strScanRecord;
		
	}

	public void setstrScanRecord(String strScanRecord) {
		this.strScanRecord = strScanRecord;
	}
	
	
}
