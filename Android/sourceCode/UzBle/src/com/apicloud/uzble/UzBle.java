package com.apicloud.uzble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import com.uzmap.pkg.uzcore.UZWebView;
import com.uzmap.pkg.uzcore.uzmodule.UZModule;
import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UzBle
  extends UZModule
{
 
  private static boolean single;
  private IBle mIBle;
  private BluetoothAdapter mBluetoothAdapter;
  private boolean mIsBleServiceAlive;
  
  public UzBle(UZWebView webView)
  {
    super(webView);
  }
  
  public void jsmethod_initManager(UZModuleContext moduleContext)
  {
    single = moduleContext.optBoolean("single", false);
    L.isDebug = true;
    if (!single)
    {
      L.i("NOinitManagersingle");
      if (!BleUtil.isBlePermission(this.mContext))
      {
        initCallBack(moduleContext, "unauthorized");
      }
      else if (!BleUtil.isBleSupported(this.mContext))
      {
        initCallBack(moduleContext, "unsupported");
      }
      else
      {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        switch (this.mBluetoothAdapter.getState())
        {
        case 10: 
          initCallBack(moduleContext, "poweredOff");
          break;
        case 12: 
          initCallBack(moduleContext, "poweredOn");
          break;
        case 1: 
        case 3: 
        case 11: 
        case 13: 
          initCallBack(moduleContext, "resetting");
          break;
        case 2: 
        case 4: 
        case 5: 
        case 6: 
        case 7: 
        case 8: 
        case 9: 
        default: 
          initCallBack(moduleContext, "unknown");
          break;
        }
      }
    }
    else
    {
      L.i("initManagersingle");
      BleManager.getInstance().init(moduleContext, this.mContext);
    }
  }
  
  public void jsmethod_scan(UZModuleContext moduleContext)
  {
    if (!single)
    {
      if (this.mIsBleServiceAlive)
      {
        initIBle();
        
        this.mIBle.scan(getUUIDS(moduleContext));
      }
      statusCallBack(moduleContext, this.mIsBleServiceAlive);
    }
    else
    {
      L.i("scansingle");
      BleManager.getInstance().scan(moduleContext);
    }
  }
  
  public void jsmethod_getPeripheral(UZModuleContext moduleContext)
  {
    if (!single)
    {
      if (this.mIBle != null)
      {
        Map<String, BleDeviceInfo> deviceMap = this.mIBle.getPeripheral();
        getPeripheralCallBack(moduleContext, deviceMap);
      }
      else
      {
        getPeripheralCallBack(moduleContext, null);
      }
    }
    else
    {
      L.i("getPeripheralsingle");
      BleManager.getInstance().getPeripheral(moduleContext);
    }
  }
  
  public void jsmethod_isScanning(UZModuleContext moduleContext)
  {
    if (!single)
    {
      if (this.mIBle != null) {
        statusCallBack(moduleContext, this.mIBle.isScanning());
      } else {
        statusCallBack(moduleContext, false);
      }
    }
    else
    {
      L.i("isScanningsingle");
      BleManager.getInstance().isScanning(moduleContext);
    }
  }
  
  public void jsmethod_stopScan(UZModuleContext moduleContext)
  {
    if (!single)
    {
      if (this.mIBle != null) {
        this.mIBle.stopScan();
      }
    }
    else
    {
      L.i("stopScansingle");
      BleManager.getInstance().stopScan(moduleContext);
    }
  }
  
  public void jsmethod_connect(UZModuleContext moduleContext)
  {
    if (!single)
    {
      if (this.mIBle != null)
      {
        String address = moduleContext.optString("peripheralUUID");
        this.mIBle.connect(moduleContext, address);
      }
    }
    else
    {
      L.i("connectsingle");
      BleManager.getInstance().connect(moduleContext, this.mContext);
    }
  }
  
  public void jsmethod_connectPeripherals(UZModuleContext moduleContext)
  {
    if (!single)
    {
      if (this.mIBle != null)
      {
        JSONArray address = moduleContext.optJSONArray("peripheralUUIDs");
        this.mIBle.connectPeripherals(moduleContext, address);
      }
    }
    else
    {
      L.i("connectPeripheralssingle");
      BleManager.getInstance().connectPeripherals(moduleContext, this.mContext);
    }
  }
  
  public void jsmethod_disconnect(UZModuleContext moduleContext)
  {
    if (!single)
    {
      if (this.mIBle != null)
      {
        String address = moduleContext.optString("peripheralUUID");
        this.mIBle.disconnect(moduleContext, address);
      }
    }
    else
    {
      L.i("disconnectsingle");
      BleManager.getInstance().disconnect(moduleContext);
    }
  }
  
  public void jsmethod_isConnected(UZModuleContext moduleContext)
  {
    if (!single)
    {
      String address = moduleContext.optString("peripheralUUID");
      if (this.mIBle != null) {
        isConnectedCallBack(moduleContext, this.mIBle.isConnected(address), 
          address);
      } else {
        isConnectedCallBack(moduleContext, false, address);
      }
    }
    else
    {
      L.i("isConnectedsingle");
      BleManager.getInstance().isConnected(moduleContext);
    }
  }
  
  public void jsmethod_discoverService(UZModuleContext moduleContext)
  {
    if (!single)
    {
      if (this.mIBle != null)
      {
        String address = moduleContext.optString("peripheralUUID");
        this.mIBle.discoverService(moduleContext, address);
      }
      else
      {
        errcodeCallBack(moduleContext, 2);
      }
    }
    else
    {
      L.i("discoverServicesingle");
      BleManager.getInstance().discoverService(moduleContext);
    }
  }
  
  public void jsmethod_discoverCharacteristics(UZModuleContext moduleContext)
  {
    if (!single)
    {
      if (this.mIBle != null)
      {
        String address = moduleContext.optString("peripheralUUID");
        String serviceUUID = moduleContext.optString("serviceUUID");
        if ((address == null) || (address.length() == 0))
        {
          errcodeCallBack(moduleContext, 1);
          return;
        }
        if ((serviceUUID == null) || (serviceUUID.length() == 0))
        {
          errcodeCallBack(moduleContext, 2);
          return;
        }
        this.mIBle.discoverCharacteristics(moduleContext, address, serviceUUID);
      }
      else
      {
        errcodeCallBack(moduleContext, 4);
      }
    }
    else {
      BleManager.getInstance().discoverCharacteristics(moduleContext);
    }
  }
  
  public void jsmethod_discoverDescriptorsForCharacteristic(UZModuleContext moduleContext)
  {
    if (!single)
    {
      if (this.mIBle != null)
      {
        String address = moduleContext.optString("peripheralUUID");
        String serviceUUID = moduleContext.optString("serviceUUID");
        String characteristicUUID = moduleContext
          .optString("characteristicUUID");
        if ((address == null) || (address.length() == 0))
        {
          errcodeCallBack(moduleContext, 1);
          return;
        }
        if ((serviceUUID == null) || (serviceUUID.length() == 0))
        {
          errcodeCallBack(moduleContext, 2);
          return;
        }
        if ((characteristicUUID == null) || (characteristicUUID.length() == 0))
        {
          errcodeCallBack(moduleContext, 3);
          return;
        }
        this.mIBle.discoverDescriptorsForCharacteristic(moduleContext, address, 
          serviceUUID, characteristicUUID);
      }
      else
      {
        errcodeCallBack(moduleContext, 6);
      }
    }
    else {
      BleManager.getInstance().discoverDescriptorsForCharacteristic(moduleContext);
    }
  }
  
  public void jsmethod_setNotify(UZModuleContext moduleContext)
  {
    if (!single)
    {
      if (this.mIBle != null)
      {
        String address = moduleContext.optString("peripheralUUID");
        String serviceUUID = moduleContext.optString("serviceUUID");
        String characteristicUUID = moduleContext
          .optString("characteristicUUID");
        if ((address == null) || (address.length() == 0))
        {
          errcodeCallBack(moduleContext, 1);
          return;
        }
        if ((serviceUUID == null) || (serviceUUID.length() == 0))
        {
          errcodeCallBack(moduleContext, 2);
          return;
        }
        if ((characteristicUUID == null) || (characteristicUUID.length() == 0))
        {
          errcodeCallBack(moduleContext, 3);
          return;
        }
        this.mIBle.setNotify(moduleContext, address, serviceUUID, 
          characteristicUUID);
      }
      else
      {
        errcodeCallBack(moduleContext, 6);
      }
    }
    else {
      BleManager.getInstance().setNotify(moduleContext);
    }
  }
  
  public void jsmethod_setSimpleNotify(UZModuleContext moduleContext)
  {
    if (!single)
    {
      if (this.mIBle != null)
      {
        String address = moduleContext.optString("peripheralUUID");
        String serviceUUID = moduleContext.optString("serviceUUID");
        String characteristicUUID = moduleContext
          .optString("characteristicUUID");
        if ((address == null) || (address.length() == 0))
        {
          errcodeCallBack(moduleContext, 1);
          return;
        }
        if ((serviceUUID == null) || (serviceUUID.length() == 0))
        {
          errcodeCallBack(moduleContext, 2);
          return;
        }
        if ((characteristicUUID == null) || (characteristicUUID.length() == 0))
        {
          errcodeCallBack(moduleContext, 3);
          return;
        }
        this.mIBle.setSimpleNotify(moduleContext, address, serviceUUID, 
          characteristicUUID);
      }
      else
      {
        errcodeCallBack(moduleContext, 6);
      }
    }
    else {
      BleManager.getInstance().setSimpleNotify(moduleContext);
    }
  }
  
  public void jsmethod_getAllSimpleNotifyData(UZModuleContext moduleContext)
  {
    if (!single)
    {
      if (this.mIBle != null)
      {
        this.mIBle.getAllSimpleNotifyData(moduleContext);
      }
      else
      {
        JSONObject ret = new JSONObject();
        try
        {
          ret.put("status", false);
          moduleContext.success(ret, true);
        }
        catch (JSONException e)
        {
          e.printStackTrace();
        }
      }
    }
    else {
      BleManager.getInstance().getAllSimpleNotifyData(moduleContext);
    }
  }
  
  public void jsmethod_clearAllSimpleNotifyData(UZModuleContext moduleContext)
  {
    if (!single)
    {
      if (this.mIBle != null) {
        this.mIBle.clearAllSimpleNotifyData();
      }
    }
    else {
      BleManager.getInstance().clearAllSimpleNotifyData(moduleContext);
    }
  }
  
  public void jsmethod_readValueForCharacteristic(UZModuleContext moduleContext)
  {
    if (!single)
    {
      if (this.mIBle != null)
      {
        String address = moduleContext.optString("peripheralUUID");
        String serviceUUID = moduleContext.optString("serviceUUID");
        String characteristicUUID = moduleContext
          .optString("characteristicUUID");
        if ((address == null) || (address.length() == 0))
        {
          errcodeCallBack(moduleContext, 1);
          return;
        }
        if ((serviceUUID == null) || (serviceUUID.length() == 0))
        {
          errcodeCallBack(moduleContext, 2);
          return;
        }
        if ((characteristicUUID == null) || (characteristicUUID.length() == 0))
        {
          errcodeCallBack(moduleContext, 3);
          return;
        }
        this.mIBle.readValueForCharacteristic(moduleContext, address, 
          serviceUUID, characteristicUUID);
      }
      else
      {
        errcodeCallBack(moduleContext, 6);
      }
    }
    else {
      BleManager.getInstance().readValueForCharacteristic(moduleContext);
    }
  }
  
  public void jsmethod_readValueForDescriptor(UZModuleContext moduleContext)
  {
    if (!single)
    {
      if (this.mIBle != null)
      {
        String address = moduleContext.optString("peripheralUUID");
        String serviceUUID = moduleContext.optString("serviceUUID");
        String characteristicUUID = moduleContext
          .optString("characteristicUUID");
        String descriptorUUID = moduleContext.optString("descriptorUUID");
        if ((address == null) || (address.length() == 0))
        {
          errcodeCallBack(moduleContext, 1);
          return;
        }
        if ((serviceUUID == null) || (serviceUUID.length() == 0))
        {
          errcodeCallBack(moduleContext, 2);
          return;
        }
        if ((characteristicUUID == null) || (characteristicUUID.length() == 0))
        {
          errcodeCallBack(moduleContext, 3);
          return;
        }
        if ((descriptorUUID == null) || (descriptorUUID.length() == 0))
        {
          errcodeCallBack(moduleContext, 4);
          return;
        }
        this.mIBle.readValueForDescriptor(moduleContext, address, serviceUUID, 
          characteristicUUID, descriptorUUID);
      }
      else
      {
        errcodeCallBack(moduleContext, 8);
      }
    }
    else {
      BleManager.getInstance().readValueForDescriptor(moduleContext);
    }
  }
  
  public void jsmethod_writeValueForCharacteristic(UZModuleContext moduleContext)
  {
    if (!single)
    {
      if (this.mIBle != null)
      {
        String address = moduleContext.optString("peripheralUUID");
        String serviceUUID = moduleContext.optString("serviceUUID");
        String characteristicUUID = moduleContext
          .optString("characteristicUUID");
        String value = moduleContext.optString("value");
        String writeType = moduleContext.optString("writeType", "auto");
        if ((address == null) || (address.length() == 0))
        {
          errcodeCallBack(moduleContext, 1);
          return;
        }
        if ((serviceUUID == null) || (serviceUUID.length() == 0))
        {
          errcodeCallBack(moduleContext, 2);
          return;
        }
        if ((characteristicUUID == null) || (characteristicUUID.length() == 0))
        {
          errcodeCallBack(moduleContext, 3);
          return;
        }
        if ((value == null) || (value.length() == 0))
        {
          errcodeCallBack(moduleContext, 4);
          return;
        }
        int intWriteType = 2;
        if (writeType.equals("response")) {
          intWriteType = 4;
        } else if (writeType.equals("withoutResponse")) {
          intWriteType = 1;
        }
        this.mIBle.writeValueForCharacteristic(moduleContext, address, 
          serviceUUID, characteristicUUID, value, intWriteType);
      }
      else
      {
        errcodeCallBack(moduleContext, 7);
      }
    }
    else {
      BleManager.getInstance().writeValueForCharacteristic(moduleContext);
    }
  }
  
  public void jsmethod_writeValueForDescriptor(UZModuleContext moduleContext)
  {
    if (!single)
    {
      if (this.mIBle != null)
      {
        String address = moduleContext.optString("peripheralUUID");
        String serviceUUID = moduleContext.optString("serviceUUID");
        String characteristicUUID = moduleContext
          .optString("characteristicUUID");
        String descriptorUUID = moduleContext.optString("descriptorUUID");
        String value = moduleContext.optString("value");
        if ((address == null) || (address.length() == 0))
        {
          errcodeCallBack(moduleContext, 1);
          return;
        }
        if ((serviceUUID == null) || (serviceUUID.length() == 0))
        {
          errcodeCallBack(moduleContext, 2);
          return;
        }
        if ((characteristicUUID == null) || (characteristicUUID.length() == 0))
        {
          errcodeCallBack(moduleContext, 3);
          return;
        }
        if ((descriptorUUID == null) || (descriptorUUID.length() == 0))
        {
          errcodeCallBack(moduleContext, 4);
          return;
        }
        if ((value == null) || (value.length() == 0))
        {
          errcodeCallBack(moduleContext, 5);
          return;
        }
        this.mIBle.writeValueForDescriptor(moduleContext, address, serviceUUID, 
          characteristicUUID, descriptorUUID, value);
      }
      else
      {
        errcodeCallBack(moduleContext, 9);
      }
    }
    else {
      BleManager.getInstance().writeValueForDescriptor(moduleContext);
    }
  }
  
  private void initIBle()
  {
    BleUtil.BLESDK sdk = BleUtil.getBleSDK(this.mContext);
    if (this.mIBle == null) {
      if (sdk == BleUtil.BLESDK.ANDROID) {
        this.mIBle = new AndroidBle(this.mContext);
      } else if (sdk == BleUtil.BLESDK.SAMSUNG) {
        this.mIBle = new SamsungBle(this.mContext);
      } else if (sdk == BleUtil.BLESDK.BROADCOM) {
        this.mIBle = new BroadcomBle(this.mContext);
      } else {}
    }
  }
  
  private void initCallBack(UZModuleContext moduleContext, String state)
  {
    JSONObject ret = new JSONObject();
    try
    {
      ret.put("state", state);
      this.mIsBleServiceAlive = state.equals("poweredOn");
      moduleContext.success(ret, false);
    }
    catch (JSONException e)
    {
      e.printStackTrace();
    }
  }
  
  private UUID[] getUUIDS(UZModuleContext moduleContext)
  {
    JSONArray serviceUUIDs = moduleContext.optJSONArray("serviceUUIDs");
    if ((serviceUUIDs != null) && (serviceUUIDs.length() > 0))
    {
      UUID[] uuids = new UUID[serviceUUIDs.length()];
      for (int i = 0; i < serviceUUIDs.length(); i++) {
        uuids[i] = UUID.fromString(serviceUUIDs.optString(i));
      }
      return uuids;
    }
    return null;
  }
  
  public void getPeripheralCallBack(UZModuleContext moduleContext, Map<String, BleDeviceInfo> deviceMap)
  {
    JSONObject ret = new JSONObject();
    
    JSONArray peripherals = new JSONArray();
    try
    {
      ret.put("peripherals", peripherals);
      if (deviceMap != null)
      {
        Iterator localIterator = deviceMap.entrySet().iterator();
        while (localIterator.hasNext())
        {
          Map.Entry<String, BleDeviceInfo> entry = (Map.Entry)localIterator.next();
          JSONObject peripheral = new JSONObject();
          BleDeviceInfo bleDeviceInfo = (BleDeviceInfo)entry.getValue();
          peripheral.put("uuid", bleDeviceInfo.getBluetoothDevice()
            .getAddress());
          peripheral.put("name", bleDeviceInfo.getBluetoothDevice()
            .getName());
          peripheral.put("rssi", bleDeviceInfo.getRssi());
          peripheral.put("manufacturerData", bleDeviceInfo.getstrScanRecord());
          peripherals.put(peripheral);
        }
      }
      moduleContext.success(ret, false);
    }
    catch (JSONException e)
    {
      e.printStackTrace();
    }
  }
  
  private void statusCallBack(UZModuleContext moduleContext, boolean status)
  {
    JSONObject ret = new JSONObject();
    try
    {
      ret.put("status", status);
      moduleContext.success(ret, false);
    }
    catch (JSONException e)
    {
      e.printStackTrace();
    }
  }
  
  private void isConnectedCallBack(UZModuleContext moduleContext, boolean status, String uuid)
  {
    JSONObject ret = new JSONObject();
    try
    {
      ret.put("status", status);
      ret.put("peripheralUUID", uuid);
      moduleContext.success(ret, false);
    }
    catch (JSONException e)
    {
      e.printStackTrace();
    }
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
  
  protected void onClean()
  {
    if (this.mBluetoothAdapter != null)
    {
      this.mBluetoothAdapter.cancelDiscovery();
      this.mBluetoothAdapter = null;
    }
  }
}
