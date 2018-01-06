package com.apicloud.uzble;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;
import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressLint({"NewApi"})
public class AndroidBle
  implements IBle
{
  public static final UUID DESC_CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
  private Context mContext;
  private BluetoothAdapter mBluetoothAdapter;
  private Map<String, BluetoothGatt> mBluetoothGattMap;
  private Map<String, UZModuleContext> mConnectCallBackMap;
  private Map<String, UZModuleContext> mConnectsCallBackMap;
  private Map<String, UZModuleContext> mDiscoverServiceCallBackMap;
  private Map<String, List<BluetoothGattService>> mServiceMap;
  private Map<String, UZModuleContext> mNotifyCallBackMap;
  private List<Ble> mSimpleNotifyCallBackMap;
  private Map<String, UZModuleContext> mReadCharacteristicCallBackMap;
  private Map<String, UZModuleContext> mWriteCharacteristicCallBackMap;
  private Map<String, UZModuleContext> mReadDescriptorCallBackMap;
  private Map<String, UZModuleContext> mWriteDescriptorCallBackMap;
  private Map<String, BleDeviceInfo> mScanBluetoothDeviceMap;
  private boolean mIsScanning;
  private JSONObject mNotifyData;
  public BluetoothGatt bluetoothGatt;
  
  public AndroidBle(Context context)
  {
    this.mContext = context;
    
    BluetoothManager bluetoothManager = (BluetoothManager)this.mContext
      .getSystemService("bluetooth");
    
    this.mBluetoothAdapter = bluetoothManager.getAdapter();
    
    this.mBluetoothGattMap = new HashMap();
    
    this.mScanBluetoothDeviceMap = new HashMap();
    this.mConnectCallBackMap = new HashMap();
    this.mDiscoverServiceCallBackMap = new HashMap();
    this.mServiceMap = new HashMap();
    this.mNotifyCallBackMap = new HashMap();
    this.mReadCharacteristicCallBackMap = new HashMap();
    this.mReadDescriptorCallBackMap = new HashMap();
    this.mWriteCharacteristicCallBackMap = new HashMap();
    this.mWriteDescriptorCallBackMap = new HashMap();
    this.mSimpleNotifyCallBackMap = new ArrayList();
    this.mConnectsCallBackMap = new HashMap();
    this.mNotifyData = new JSONObject();
  }
  
  public void scan(UUID[] uuids)
  {
    if (uuids != null) {
      this.mBluetoothAdapter.startLeScan(uuids, this.mLeScanCallback);
    } else {
      this.mBluetoothAdapter.startLeScan(this.mLeScanCallback);
    }
    this.mIsScanning = true;
  }
  
  public Map<String, BleDeviceInfo> getPeripheral()
  {
    return this.mScanBluetoothDeviceMap;
  }
  
  public boolean isScanning()
  {
    return this.mIsScanning;
  }
  
  public void stopScan()
  {
    this.mScanBluetoothDeviceMap.clear();
    this.mBluetoothAdapter.stopLeScan(this.mLeScanCallback);
    this.mIsScanning = false;
  }
  /***
   * 连接
   */
  public void connect(UZModuleContext moduleContext, String address)
  {
    this.mConnectCallBackMap.put(address, moduleContext);
    if ((address == null) || (address.length() == 0))
    {
      connectCallBack(moduleContext, false, 1, "null address");
      return;
    }
    try
    {
      if (this.bluetoothGatt != null)
      {
        this.bluetoothGatt.disconnect();
        this.bluetoothGatt.close();
      }
      final BluetoothDevice device = this.mBluetoothAdapter
        .getRemoteDevice(address);
      
      ((Activity)this.mContext).runOnUiThread(new Runnable()
      {
        public void run()
        {
          AndroidBle.this.bluetoothGatt = device.connectGatt(AndroidBle.this.mContext, false, 
            AndroidBle.this.mBluetoothGattCallback);
          

          AndroidBle.this.bluetoothGatt.requestMtu(512);
        }
      });
    }
    catch (Exception e)
    {
      connectCallBack(moduleContext, false, 2, "Exception");
    }
  }
  
  public void connectPeripherals(UZModuleContext moduleContext, JSONArray address)
  {
    for (int i = 0; i < address.length(); i++)
    {
      this.mConnectsCallBackMap.put(address.optString(i), moduleContext);
      if ((address == null) || (address.length() == 0))
      {
        connectCallBack(moduleContext, false, 1, "null adress");
        return;
      }
      try
      {
        BluetoothDevice device = this.mBluetoothAdapter
          .getRemoteDevice(address.optString(i));
        device.connectGatt(this.mContext, false, this.mBluetoothGattCallback);
      }
      catch (Exception e)
      {
        connectsCallBack(moduleContext, false, 2, address.optString(i));
      }
    }
  }
  
  public void disconnect(UZModuleContext moduleContext, String address)
  {
    BluetoothGatt bluetoothGatt = (BluetoothGatt)this.mBluetoothGattMap.get(address);
    if (bluetoothGatt != null)
    {
      bluetoothGatt.disconnect();
      
      bluetoothGatt.close();
      this.mBluetoothGattMap.remove(address);
      this.mServiceMap.remove(address);
      remove2NotifyMap(address);
      




      disconnectCallBack(moduleContext, true, address);
    }
    else
    {
      disconnectCallBack(moduleContext, false, address);
    }
  }
  
  private void remove2NotifyMap(String address)
  {
    if (this.mSimpleNotifyCallBackMap == null) {
      return;
    }
    for (Ble ble : this.mSimpleNotifyCallBackMap) {
      if (ble.getPeripheralUUID().equals(address)) {
        this.mSimpleNotifyCallBackMap.remove(ble);
      }
    }
  }
  
  public boolean isConnected(String address)
  {
    return this.mBluetoothGattMap.containsKey(address);
  }
  
  public void discoverService(UZModuleContext moduleContext, String address)
  {
    BluetoothGatt bluetoothGatt = (BluetoothGatt)this.mBluetoothGattMap.get(address);
    if ((address == null) || (address.length() == 0))
    {
      discoverServiceCallBack(moduleContext, null, false, 1);
      return;
    }
    if (bluetoothGatt != null)
    {
      this.mDiscoverServiceCallBackMap.put(address, moduleContext);
      bluetoothGatt.discoverServices();
    }
    else
    {
      discoverServiceCallBack(moduleContext, null, false, 2);
    }
  }
  
  public void discoverCharacteristics(UZModuleContext moduleContext, String address, String serviceUUID)
  {
    List<BluetoothGattCharacteristic> characteristics = characteristics(
      address, serviceUUID);
    if (characteristics == null) {
      errcodeCallBack(moduleContext, 3);
    } else {
      characteristicCallBack(moduleContext, characteristics);
    }
  }
  
  public void discoverDescriptorsForCharacteristic(UZModuleContext moduleContext, String address, String serviceUUID, String characteristicUUID)
  {
    List<BluetoothGattCharacteristic> characteristics = characteristics(
      address, serviceUUID);
    if (characteristics == null)
    {
      errcodeCallBack(moduleContext, 5);
    }
    else
    {
      for (BluetoothGattCharacteristic characteristic : characteristics) {
        if (characteristic.getUuid().toString().equals(characteristicUUID))
        {
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
  
  public void setNotify(UZModuleContext moduleContext, String address, String serviceUUID, String characteristicUUID)
  {
    this.mNotifyCallBackMap.put(characteristicUUID, moduleContext);
    BluetoothGatt bluetoothGatt = (BluetoothGatt)this.mBluetoothGattMap.get(address);
    if (bluetoothGatt != null)
    {
      BluetoothGattCharacteristic characteristic = characteristic(
        moduleContext, address, serviceUUID, characteristicUUID);
      if (characteristic != null)
      {
        boolean status = bluetoothGatt.setCharacteristicNotification(
          characteristic, true);
        if (status)
        {
          BluetoothGattDescriptor descriptor = characteristic
            .getDescriptor(DESC_CCC);
          if (descriptor == null) {
            errcodeCallBack(moduleContext, -1);
          } else if (!descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
            errcodeCallBack(moduleContext, -1);
          } else {
            bluetoothGatt.writeDescriptor(descriptor);
          }
        }
        else
        {
          errcodeCallBack(moduleContext, -1);
        }
      }
    }
  }
  
  public void readValueForCharacteristic(UZModuleContext moduleContext, String address, String serviceUUID, String characteristicUUID)
  {
    this.mReadCharacteristicCallBackMap.put(characteristicUUID, moduleContext);
    BluetoothGatt bluetoothGatt = (BluetoothGatt)this.mBluetoothGattMap.get(address);
    if (bluetoothGatt != null)
    {
      BluetoothGattCharacteristic characteristic = characteristic(
        moduleContext, address, serviceUUID, characteristicUUID);
      if (characteristic != null)
      {
        boolean status = bluetoothGatt
          .readCharacteristic(characteristic);
        if (!status) {
          errcodeCallBack(moduleContext, -1);
        }
      }
    }
  }
  
  public void readValueForDescriptor(UZModuleContext moduleContext, String address, String serviceUUID, String characteristicUUID, String descriptorUUID)
  {
    this.mReadDescriptorCallBackMap.put(descriptorUUID, moduleContext);
    BluetoothGatt bluetoothGatt = (BluetoothGatt)this.mBluetoothGattMap.get(address);
    BluetoothGattDescriptor descriptor = descriptor(moduleContext, address, 
      serviceUUID, characteristicUUID, descriptorUUID);
    if (bluetoothGatt != null) {
      if (descriptor != null)
      {
        if (!bluetoothGatt.readDescriptor(descriptor)) {
          errcodeCallBack(moduleContext, -1);
        }
      }
      else {
        errcodeCallBack(moduleContext, 5);
      }
    }
  }
  
  public void writeValueForCharacteristic(UZModuleContext moduleContext, String address, String serviceUUID, String characteristicUUID, String value, int writeType)
  {
    this.mWriteCharacteristicCallBackMap.put(characteristicUUID, moduleContext);
    BluetoothGatt bluetoothGatt = (BluetoothGatt)this.mBluetoothGattMap.get(address);
    if (bluetoothGatt != null)
    {
      BluetoothGattCharacteristic characteristic = characteristicWrite(
        moduleContext, address, serviceUUID, characteristicUUID, 
        writeType);
      if (characteristic != null)
      {
        characteristic.setValue(value(value));
        boolean status = bluetoothGatt
          .writeCharacteristic(characteristic);
        if (!status) {
          errcodeCallBack(moduleContext, -1);
        }
      }
    }
  }
  
  private byte[] value(String valueStr)
  {
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
  
  public void writeValueForDescriptor(UZModuleContext moduleContext, String address, String serviceUUID, String characteristicUUID, String descriptorUUID, String value)
  {
    this.mWriteDescriptorCallBackMap.put(descriptorUUID, moduleContext);
    BluetoothGatt bluetoothGatt = (BluetoothGatt)this.mBluetoothGattMap.get(address);
    BluetoothGattDescriptor descriptor = descriptorWrite(moduleContext, 
      address, serviceUUID, characteristicUUID, descriptorUUID);
    if (bluetoothGatt != null) {
      if (descriptor != null)
      {
        descriptor.setValue(value(value));
        if (!bluetoothGatt.writeDescriptor(descriptor)) {
          errcodeCallBack(moduleContext, -1);
        }
      }
      else
      {
        errcodeCallBack(moduleContext, 6);
      }
    }
  }
  
  private List<BluetoothGattCharacteristic> characteristics(String address, String serviceUUID)
  {
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
  
  private BluetoothGattCharacteristic characteristic(UZModuleContext moduleContext, String address, String serviceUUID, String characteristicUUID)
  {
    List<BluetoothGattService> services = mServiceMap.get(address);
    if (services != null)
    {
      for (BluetoothGattService service : services) {
        if (service.getUuid().toString().equals(serviceUUID))
        {
          List<BluetoothGattCharacteristic> characteristics = service
            .getCharacteristics();
          if (characteristics != null)
          {
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
  
  private BluetoothGattCharacteristic characteristicWrite(UZModuleContext moduleContext, String address, String serviceUUID, String characteristicUUID, int writeType)
  {
    List<BluetoothGattService> services = mServiceMap.get(address);
    if (services != null)
    {
      for (BluetoothGattService service : services) {
        if (service.getUuid().toString().equals(serviceUUID))
        {
          List<BluetoothGattCharacteristic> characteristics = service
            .getCharacteristics();
          if (characteristics != null)
          {
            for (BluetoothGattCharacteristic characteristic : characteristics) {
              if (characteristic.getUuid().toString().equals(characteristicUUID))
              {
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
  
  private BluetoothGattDescriptor descriptor(UZModuleContext moduleContext, String address, String serviceUUID, String characteristicUUID, String descriptorUUID)
  {
    List<BluetoothGattService> services = mServiceMap.get(address);
    if (services != null)
    {
      for (BluetoothGattService service : services) {
        if (service.getUuid().toString().equals(serviceUUID))
        {
          List<BluetoothGattCharacteristic> characteristics = service
            .getCharacteristics();
          if (characteristics != null)
          {
            for (BluetoothGattCharacteristic characteristic : characteristics) {
              if (characteristic.getUuid().toString().equals(characteristicUUID)) {
                return characteristic.getDescriptor(
                  UUID.fromString(descriptorUUID));
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
  
  private BluetoothGattDescriptor descriptorWrite(UZModuleContext moduleContext, String address, String serviceUUID, String characteristicUUID, String descriptorUUID)
  {
    List<BluetoothGattService> services = (List)this.mServiceMap.get(address);
    if (services != null)
    {
      for (BluetoothGattService service : services) {
        if (service.getUuid().toString().equals(serviceUUID))
        {
          List<BluetoothGattCharacteristic> characteristics = service
            .getCharacteristics();
          if (characteristics != null)
          {
            for (BluetoothGattCharacteristic characteristic : characteristics) {
              if (characteristic.getUuid().toString().equals(characteristicUUID)) {
                return characteristic.getDescriptor(
                  UUID.fromString(descriptorUUID));
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
  
  private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback()
  {
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
    {
      String strScanRecord = new String(Hex.encodeHex(scanRecord));
      Log.e("名字", device.getName() + "getAddress" + device.getAddress() + "device.getUuids();" + device.getUuids());
     mScanBluetoothDeviceMap.put(device.getAddress(), new BleDeviceInfo(
        device, rssi,strScanRecord));
    }
  };
  private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback()
  {
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
    {
      String address = gatt.getDevice().getAddress();
      UZModuleContext moduleContext = (UZModuleContext)AndroidBle.this.mConnectCallBackMap.get(address);
      if (status != 0)
      {
        gatt.close();
        AndroidBle.this.mConnectCallBackMap.remove(address);
        AndroidBle.this.connectCallBack(moduleContext, false, -1, "status:" + status);
        return;
      }
      if (newState == 2)
      {
        AndroidBle.this.mBluetoothGattMap.put(address, gatt);
        AndroidBle.this.connectCallBack(moduleContext, true, 0, "success");
        Log.e("走到这里了", "连接成功");
      }
      else if (newState == 0)
      {
        gatt.close();
        AndroidBle.this.mBluetoothGattMap.remove(address);
        AndroidBle.this.mConnectCallBackMap.remove(address);
        AndroidBle.this.connectCallBack(moduleContext, false, -1, "newState:" + 
          newState);
      }
    }
    
    public void onServicesDiscovered(BluetoothGatt gatt, int status)
    {
      if (status == 0)
      {
        String address = gatt.getDevice().getAddress();
        
        List<BluetoothGattService> service = gatt.getServices();
        
        AndroidBle.this.mServiceMap.put(address, service);
        AndroidBle.this.discoverServiceCallBack(
          (UZModuleContext)AndroidBle.this.mDiscoverServiceCallBackMap.get(address), service, 
          true, 0);
      }
      else
      {
        String address = gatt.getDevice().getAddress();
        AndroidBle.this.discoverServiceCallBack(
          (UZModuleContext)AndroidBle.this.mDiscoverServiceCallBackMap.get(address), null, false, 
          status);
      }
    }
    
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
    {
      AndroidBle.this.onCharacteristic(AndroidBle.this.mReadCharacteristicCallBackMap, characteristic, 
        false);
    }
    
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
    {
      AndroidBle.this.onCharacteristic(AndroidBle.this.mWriteCharacteristicCallBackMap, characteristic, 
        false);
    }
    
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
    {
      if (AndroidBle.this.getBle(gatt, characteristic) != null) {
        AndroidBle.this.characteristicSimpleCallBack( getBle(gatt, characteristic),characteristic);
      } else {
        AndroidBle.this.onCharacteristic(AndroidBle.this.mNotifyCallBackMap, characteristic, false);
      }
    }
    
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
    {
      AndroidBle.this.onDescript(AndroidBle.this.mReadDescriptorCallBackMap, descriptor);
    }
    
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
    {
      AndroidBle.this.onDescript(AndroidBle.this.mWriteDescriptorCallBackMap, descriptor);
    }
  };
  
  private Ble getBle(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
  {
    for (Ble ble : this.mSimpleNotifyCallBackMap) {
      if (ble.getPeripheralUUID().equals(gatt.getDevice().getAddress())) {
        if (ble.getServiceId().equals(characteristic.getService().getUuid().toString())) {
          return ble;
        }
      }
    }
    return null;
  }
  
  private void onCharacteristic(Map<String, UZModuleContext> map, BluetoothGattCharacteristic characteristic, boolean isSimple)
  {
    UZModuleContext moduleContext = (UZModuleContext)map.get(characteristic.getUuid()
      .toString());
    if (moduleContext != null) {
      characteristicCallBack(moduleContext, characteristic);
    }
  }
  
  private void onDescript(Map<String, UZModuleContext> map, BluetoothGattDescriptor descriptor)
  {
    UZModuleContext moduleContext = 
      (UZModuleContext)map.get(descriptor.getUuid().toString());
    if (moduleContext != null) {
      descriptorCallBack(moduleContext, descriptor, descriptor
        .getCharacteristic().getService().getUuid().toString(), 
        descriptor.getCharacteristic().getUuid().toString());
    }
  }
  
  private void connectCallBack(UZModuleContext moduleContext, boolean status, int errCode, String detailErrorCode)
  {
    JSONObject ret = new JSONObject();
    JSONObject err = new JSONObject();
    try
    {
      ret.put("status", status);
      if (status)
      {
        Log.e("设备连接成功", "66666666666666666666666666");
        moduleContext.success(ret, false);
      }
      else
      {
        Log.e("设备连接失败", "失败信息errCode" + errCode + "errCode" + "detailErrorCode" + detailErrorCode);
        err.put("code", errCode);
        
        err.put("detailErrorCode", detailErrorCode);
        moduleContext.error(ret, err, false);
      }
    }
    catch (JSONException e)
    {
      e.printStackTrace();
    }
  }
  
  private void connectsCallBack(UZModuleContext moduleContext, boolean status, int errCode, String uuid)
  {
    JSONObject ret = new JSONObject();
    JSONObject err = new JSONObject();
    try
    {
      ret.put("status", status);
      if (status)
      {
        ret.put("peripheralUUID", uuid);
        moduleContext.success(ret, false);
      }
      else
      {
        err.put("code", errCode);
        moduleContext.error(ret, err, false);
      }
    }
    catch (JSONException e)
    {
      e.printStackTrace();
    }
  }
  
  public void connectsCallBack(UZModuleContext moduleContext, BluetoothDevice device, boolean status, JSONArray mConnectedDeviceMap)
  {
    JSONObject ret = new JSONObject();
    try
    {
      ret.put("status", status);
      ret.put("peripheralUUID", device.getAddress());
      moduleContext.success(ret, false);
    }
    catch (JSONException e)
    {
      e.printStackTrace();
    }
  }
  
  private void discoverServiceCallBack(UZModuleContext moduleContext, List<BluetoothGattService> services, boolean status, int errCode)
  {
    JSONObject ret = new JSONObject();
    JSONObject err = new JSONObject();
    try
    {
      ret.put("status", status);
      if (status)
      {
        JSONArray serviceArray = new JSONArray();
        for (BluetoothGattService service : services) {
          serviceArray.put(service.getUuid().toString());
        }
        ret.put("services", serviceArray);
        moduleContext.success(ret, false);
      }
      else
      {
        err.put("code", errCode);
        moduleContext.error(ret, err, false);
      }
    }
    catch (JSONException e)
    {
      e.printStackTrace();
    }
  }
  
  private void disconnectCallBack(UZModuleContext moduleContext, boolean status, String uuid)
  {
    JSONObject ret = new JSONObject();
    try
    {
      ret.put("status", status);
      ret.put("peripheralUUID", uuid);
      if (status) {
        Log.e("断开成功", "77777777");
      }
      moduleContext.success(ret, false);
    }
    catch (JSONException e)
    {
      e.printStackTrace();
    }
  }
  
  private void characteristicCallBack(UZModuleContext moduleContext, List<BluetoothGattCharacteristic> characteristics)
  {
    JSONObject ret = new JSONObject();
    JSONArray characteristicsJson = new JSONArray();
    try
    {
      ret.put("status", true);
      ret.put("characteristics", characteristicsJson);
      for (BluetoothGattCharacteristic characteristic : characteristics)
      {
        JSONObject item = new JSONObject();
        item.put("uuid", characteristic.getUuid());
        item.put("serviceUUID", characteristic.getService().getUuid()
          .toString());
        item.put("permissions", 
          permissions(characteristic.getPermissions()));
        item.put("properties", 
          properties(characteristic.getProperties()));
        characteristicsJson.put(item);
      }
      moduleContext.success(ret, false);
    }
    catch (JSONException e)
    {
      e.printStackTrace();
    }
  }
  
  private void characteristicCallBack(UZModuleContext moduleContext, BluetoothGattCharacteristic characteristic)
  {
    JSONObject ret = new JSONObject();
    JSONObject characteristicJson = new JSONObject();
    try
    {
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
    }
    catch (JSONException e)
    {
      e.printStackTrace();
    }
  }
  
  private void characteristicSimpleCallBack(Ble ble, BluetoothGattCharacteristic characteristic)
  {
    JSONObject ret = new JSONObject();
    try
    {
      ret.put("status", true);
      setNotifyData(characteristic, ble);
      ble.getModuleContext().success(ret, false);
    }
    catch (JSONException e)
    {
      e.printStackTrace();
    }
  }
  
  private void setNotifyData(BluetoothGattCharacteristic characteristic, Ble ble)
  {
    if (ble != null) {
      if (this.mNotifyData.isNull(ble.getPeripheralUUID()))
      {
        JSONObject notifyData = new JSONObject();
        try
        {
          notifyData.put("serviceUUID", ble.getServiceId());
          notifyData
            .put("characterUUID", ble.getCharacteristicUUID());
          JSONArray data = new JSONArray();
          data.put(new String(
            Hex.encodeHex(characteristic.getValue())));
          notifyData.put("data", data);
          this.mNotifyData.put(ble.getPeripheralUUID(), notifyData);
        }
        catch (JSONException e)
        {
          e.printStackTrace();
        }
      }
      else
      {
        JSONObject notifyData = this.mNotifyData.optJSONObject(ble
          .getPeripheralUUID());
        JSONArray data = notifyData.optJSONArray("data");
        data.put(new String(Hex.encodeHex(characteristic.getValue())));
      }
    }
  }
  
  private void descriptorsCallBack(UZModuleContext moduleContext, List<BluetoothGattDescriptor> descriptors, String serviceUUID, String characteristicUUID)
  {
    JSONObject ret = new JSONObject();
    JSONArray descriptorsJson = new JSONArray();
    try
    {
      ret.put("status", true);
      ret.put("descriptors", descriptorsJson);
      for (BluetoothGattDescriptor descriptor : descriptors)
      {
        JSONObject item = new JSONObject();
        item.put("uuid", descriptor.getUuid());
        item.put("serviceUUID", serviceUUID);
        item.put("characteristicUUID", characteristicUUID);
        descriptorsJson.put(item);
      }
      moduleContext.success(ret, false);
    }
    catch (JSONException e)
    {
      e.printStackTrace();
    }
  }
  
  private void descriptorCallBack(UZModuleContext moduleContext, BluetoothGattDescriptor descriptor, String serviceUUID, String characteristicUUID)
  {
    JSONObject ret = new JSONObject();
    JSONObject descriptorJson = new JSONObject();
    try
    {
      ret.put("status", true);
      ret.put("descriptor", descriptorJson);
      descriptorJson.put("uuid", descriptor.getUuid());
      descriptorJson.put("serviceUUID", serviceUUID);
      descriptorJson.put("characteristicUUID", characteristicUUID);
      descriptorJson.put("value", 
        new String(Hex.encodeHex(descriptor.getValue())));
      moduleContext.success(ret, false);
    }
    catch (JSONException e)
    {
      e.printStackTrace();
    }
  }
  
  private String permissions(int permissions)
  {
    switch (permissions)
    {
    case 1: 
      return "readable";
    case 16: 
      return "writeable";
    case 2: 
      return "readEncryptionRequired";
    case 32: 
      return "writeEncryptionRequired";
    }
    return String.valueOf(permissions);
  }
  
  private String properties(int propertie)
  {
    switch (propertie)
    {
    case 2: 
      return "read";
    case 1: 
      return "broadcast";
    case 128: 
      return "extendedProperties";
    case 32: 
      return "indicate";
    case 16: 
      return "notify";
    case 64: 
      return "writeable";
    case 8: 
      return "write";
    case 4: 
      return "writeWithoutResponse";
    }
    return String.valueOf(propertie);
  }
  
  private void errcodeCallBack(UZModuleContext moduleContext, int code)
  {
    JSONObject ret = new JSONObject();
    JSONObject err = new JSONObject();
    try
    {
      ret.put("status", false);
      err.put("code", code);
      moduleContext.error(ret, err, false);
    }
    catch (JSONException e)
    {
      e.printStackTrace();
    }
  }
  
  public void setSimpleNotify(UZModuleContext moduleContext, String address, String serviceUUID, String characteristicUUID)
  {
    this.mSimpleNotifyCallBackMap.add(new Ble(address, serviceUUID, 
      characteristicUUID, moduleContext));
    BluetoothGatt bluetoothGatt = (BluetoothGatt)this.mBluetoothGattMap.get(address);
    if (bluetoothGatt != null)
    {
      BluetoothGattCharacteristic characteristic = characteristic(
        moduleContext, address, serviceUUID, characteristicUUID);
      if (characteristic != null)
      {
        boolean status = bluetoothGatt.setCharacteristicNotification(
          characteristic, true);
        if (status)
        {
          BluetoothGattDescriptor descriptor = characteristic
            .getDescriptor(DESC_CCC);
          if (descriptor == null) {
            errcodeCallBack(moduleContext, -1);
          } else if (!descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
            errcodeCallBack(moduleContext, -1);
          } else {
            bluetoothGatt.writeDescriptor(descriptor);
          }
        }
        else
        {
          errcodeCallBack(moduleContext, -1);
        }
      }
    }
  }
  
  public void getAllSimpleNotifyData(UZModuleContext moduleContext)
  {
    moduleContext.success(this.mNotifyData, false);
  }
  
  public void clearAllSimpleNotifyData()
  {
    this.mNotifyData = new JSONObject();
  }
}
