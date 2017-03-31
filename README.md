# 概述

蓝牙4.0模块源码（含iOS和Android）

APICloud 的 ble 模块是一个蓝牙模块。它封装了 iOS 和 android 两个系统平台上的蓝牙4.0的相关功能接口，由于两个平台对蓝牙4.0开放接口的不一致性，所以本模块对原生提供的接口做了少许调整，以使两个平台做到统一规范，方便 APICloud 平台的开发者调用，达到一条代码即可生成适用于两个平台的目的。蓝牙4.0设备繁杂多样，每个蓝牙厂商可能提供的服务（service）、特征（Characteristics）、描述符（Descriptor）都不尽相同。每个蓝牙4.0厂商对自己的蓝牙设备读取数据格式也不尽相同。所以使用本模块时可能需要多次联调，而且收发数据的格式也要根据不同的蓝牙设备做相应的调整，为方便广大开发者迅速的开发蓝牙功能，APICloud 官方特将此模块源码开源。APICloud 的开发者可配合原生开发者联调测试蓝牙4.0的功能，加快开发进度。 

## 模块接口文档

<p style="color: #ccc; margin-bottom: 30px;">来自于：APICloud 官方</p>

<ul id="tab" class="clearfix">
	<li class="active"><a href="#method-content">Method</a></li>
</ul>

<div class="outline">

[initManager](#initManager)
[scan](#scan)
[getPeripheral](#getPeripheral)
[getPeripheralRssi](#getPeripheralRssi)
[isScanning](#isScanning)
[stopScan](#stopScan)
[connect](#connect)
[disconnect](#disconnect)
[isConnected](#isConnected)
[retrievePeripheral](#retrievePeripheral)
[retrieveConnectedPeripheral](#retrieveConnectedPeripheral)
[discoverService](#discoverService)
[discoverCharacteristics](#discoverCharacteristics)
[discoverDescriptorsForCharacteristic](#discoverDescriptorsForCharacteristic)
[setNotify](#setNotify)
[stopNotify](#stopNotify)
[readValueForCharacteristic](#readValueForCharacteristic)
[readValueForDescriptor](#readValueForDescriptor)
[writeValueForCharacteristic](#writeValueForCharacteristic)
[writeValueForDescriptor](#writeValueForDescriptor)
[connectPeripherals](#connectPeripherals)
[setSimpleNotify](#setSimpleNotify)
[getAllSimpleNotifyData](#getAllSimpleNotifyData)
[clearAllSimpleNotifyData](#clearAllSimpleNotifyData)

</div>

# **概述**

**背景**

***蓝牙的适用场景***

- 可用于第三方蓝牙设备交互，必须要支持蓝牙 4.0。
- iOS上：硬件至少是 iphone4s，系统至少是 iOS6。
- android上：系统版本至少是 android4.3。

蓝牙 4.0 以低功耗著称，一般也叫 BLE（BluetoothLowEnergy）。目前应用比较多的案例：运动手坏、嵌入式设备、智能家居

**蓝牙通讯原理概述**

在蓝牙通讯中有两个主要的部分,Central 和 Peripheral，有一点类似Client Server。Peripheral 作为周边设备是服务器。Central 作为中心设备是客户端。所有可用的蓝牙设备可以作为周边（Peripheral）也可以作为中央（Central），但不可以同时既是周边也是中央。

一般手机是客户端， 设备（比如手环）是服务器，因为是手机去连接手环这个服务器。周边（Peripheral）是生成或者保存了数据的设备，中央（Central）是使用这些数据的设备。你可以认为周边是一个广播数据的设备，他广播到外部世界说他这儿有数据，并且也说明了能提供的服务。另一边，中央开始扫描附近有没有服务，如果中央发现了想要的服务，然后中央就会请求连接周边，一旦连接建立成功，两个设备之间就开始交换传输数据了。

除了中央和周边，我们还要考虑他俩交换的数据结构。这些数据在服务中被结构化，每个服务由不同的特征（Characteristics）组成，特征是包含一个单一逻辑值的属性类型。

**服务和特性**

上文中提到了特征（Characteristics），这里简单说明下什么是特征。

特征是与外界交互的最小单位。蓝牙4.0设备通过服务（Service）、特征（Characteristics）和描述符（Descriptor）来形容自己，同一台设备可能包含一个或多个服务，每个服务下面又包含若干个特征，每个特征下面有包含若干个描述符（Descriptor）。比如某台蓝牙4.0设备，用特征A来描述设备信息、用特征B和描述符b来收发数据等。而每个服务、特征和描述符都是用 UUID 来区分和标识的。

***注意：***

若要支持后台使用蓝牙功能需配置 [config.xml](/APICloud/技术专题/app-config-manual) 文件 [bluetooth-central、bluetooth-peripheral](http://docs.apicloud.com/APICloud/%E6%8A%80%E6%9C%AF%E4%B8%93%E9%A2%98/app-config-manual#14-2) 字段。

***本模块源码已开源，地址为：https://github.com/apicloudcom/ble***

# 模块接口

<dive id="initManager"></div>

# **initManager**

初始化蓝牙4.0管理器

initManager(cllback(ret))

## callback(ret)

ret:

- 类型：JSON 对象
- 内部字段：

```js
{
    state: 'poweredOn'       //字符串类型；蓝牙4.0设备连接状态，取值范围如下：
                             //poweredOn：设备开启状态 -- 可用状态
                             //poweredOff：设备关闭状态
                             //resetting：正在重置状态
                             //unauthorized：设备未授权状态
                             //unknown：初始的时候是未知的
                             //unsupported：设备不支持的状态
}
```
## 示例代码

```js
var ble = api.require('ble');
ble.initManager(function(ret) {
    if (ret.state == "poweredOn") {
        api.alert({ msg: "初始化成功" });
    }
});
```

## 可用性

iOS系统，Android系统

可提供的1.0.0及更高版本

<div id="scan"></div>

# **scan**

开始搜索蓝牙4.0设备，模块内部会不断的扫描更新附近的蓝牙4.0设备信息，可通过 getPeripheral 接口来获取扫描到的设备信息。若要停止、清空扫描则调用 stopScan 接口

scan({params}, callback(ret, err))

## params

serviceUUIDs

- 类型：数组
- 描述：（可选项）要扫描的蓝牙4.0设备的服务（service）的 UUID（字符串） 组成的数组，若不传则扫描附近的所有支持蓝牙4.0的设备

## callback(ret)

ret：

- 类型：JSON 对象
- 内部字段：

```js
{
    status: true   //布尔类型；是否获取成功，true|false
}
```

## 示例代码

```js
var ble = api.require('ble');
ble.scan({
    serviceUUIDs: ['', '']
}, function(ret) {
    if (ret.status) {
        alert('开始扫描');
    }
});
```

## 可用性

iOS系统，Android系统

可提供的1.0.0及更高版本

<div id="getPeripheral"></div>

# **getPeripheral**

获取当前扫描到的所有外围设备信息

getPeripheral(callback(ret))

## callback(ret)

ret：

- 类型：JSON 对象
- 描述：每发现新设备便会回调当前发现的所有蓝牙4.0设备信息
- 内部字段：

```js
{
    peripherals:[{ //数组类型；获取到的当前扫描到的蓝牙4.0设备
      uuid: '',    //字符串类型；扫描到的蓝牙设备的 UUID
      name: '',    //字符串类型；扫描到的蓝牙设备的名字
      rssi:        //数字类型；扫描到的蓝牙设备的信号强度
    },...]
}
```

## 示例代码

```js
var ble = api.require('ble');
ble.getPeripheral(function(ret) {
    if (ret) {
        api.alert({ msg: JSON.stringify(ret) });
    }
});
```

## 可用性

iOS系统，Android系统

可提供的1.0.0及更高版本


<div id="getPeripheralRssi"></div>

# **getPeripheralRssi**

获取当前扫描到的所有外围设备的 rssi

注意：

本接口仅支持iOS平台，且仅在 iOS8（含）以上系统上使用。iOS7以下系统可在Peripheral的返回信息里获得。

getPeripheralRssi({params},callback(ret, err))

## params

peripheralUUID：

- 类型：字符串
- 描述：要获取rssi值的外围设备的 UUID 

## callback(ret, err)

ret：

- 类型：JSON 对象
- 描述：获取的rssi信息
- 内部字段：

```js
{
     status: true,       //布尔类型；是否成功获取rssi，true|false
     rssi:''             //数字类型；rssi 值
}
```

err：

- 类型：JSON 对象
- 描述：获取rssi失败错误码
- 内部字段：

```js
{
     code:1       //数字类型；错误码，取值范围如下：
                  //1：peripheralUUID不正确
                  //2： peripheral 不存在
                  //3：未知错误
}
```

## 示例代码

```js
var ble = api.require('ble');
ble.getPeripheralRssi(function(ret) {
    if (ret.status) {
        api.alert({ msg: JSON.stringify(ret) });
    }
});
```

## 可用性

iOS系统，Android系统

可提供的1.0.0及更高版本

<div id="isScanning"></div>

# **isScanning**

判断是否正在扫描

isScanning(callback(ret))


## callback(ret)

ret：

- 类型：JSON 对象
- 内部字段：

```js
{
    status: true   //布尔类型；是否在扫描，true|false
}
```

## 示例代码

```js
var ble = api.require('ble');
ble.isScanning(function(ret) {
    if (ret) {
        alert('正在扫描');
    }
});
```

## 可用性

iOS系统，Android系统

可提供的1.0.0及更高版本

<div id="stopScan"></div>

# **stopScan**

停止搜索附近的蓝牙设备，并清空已搜索到的记录在本地的外围设备信息

stopScan()

## 示例代码

```js
var ble = api.require('ble');
ble.stopScan();
```

## 可用性

iOS系统，Android系统

可提供的1.0.0及更高版本

<div id="connect"></div>

# **connect**

连接指定外围设备

connect({params}, callback(ret, err))

## params

peripheralUUID：

- 类型：字符串
- 描述：要连接的外围设备的 UUID 


## callback(ret, err)

ret:

- 类型：JSON 对象
- 内部字段：

```js
{
     status: true      //布尔类型；是否连接成功，true|false
}
```

err:

- 类型：JSON 对象
- 内部字段：

```js
{
     code: 1          //数字类型；连接失败时返回错误码，取值范围如下：
                      //-1：未知错误
                      //1：uuid为空
                      //2：未搜索到该蓝牙设备
                      //3：该设备为已连接状态
}
```

## 示例代码

```js
var ble = api.require('ble');
ble.connect({
    peripheralUUID: ''
}, function(ret, err) {
    if (ret.status) {
        alert("连接成功！");
    } else {
        alert(err.code);
    }
});
```

## 可用性

iOS系统，Android系统

可提供的1.0.0及更高版本

<div id="disconnect"></div>

# **disconnect**

断开与指定外围设备的连接

disconnect({params}, callback(ret))

## params


peripheralUUID：

- 类型：字符串
- 描述：要断开连接的外围设备的 UUID 

## callback(ret)

ret:

- 类型：JSON 对象
- 内部字段：

```js
{
     status: true,       //布尔类型；是否成功断开连接，true|false
     peripheralUUID:''   //字符串类型；断开外围设备的 UUID
}
```

## 示例代码

```js
var ble = api.require('ble');
ble.disconnect({
    peripheralUUID: ''
}, function(ret, err) {
    if (ret.status) {
        alert("断开连接成功！");
    }
});
```

## 可用性

iOS系统，Android系统

可提供的1.0.0及更高版本

<div id="isConnected"></div>

# **isConnected**

判断与指定外围设备是否为连接状态

isConnected({params}, callback(ret))

## params

peripheralUUID：

- 类型：字符串
- 描述：指定外围设备的 UUID 

##callback(ret)

ret：

- 类型：JSON 对象
- 内部字段：

```js
{
    status: true,       //布尔类型；是否连接，true|false
    peripheralUUID:''   //字符串类型；外围设备的 UUID
}
```

## 示例代码

```js
var ble = api.require('ble');
ble.isConnected({
    peripheralUUID: ''
}, function(ret) {
    if (ret) {
        alert('已连接');
    }
});
```

## 可用性

iOS系统，Android系统

可提供的1.0.0及更高版本

<div id="retrievePeripheral"></div>

# **retrievePeripheral**

根据 UUID 找到所有匹配的蓝牙外围设备信息**Andaroid 平台暂不支持本接口**

retrievePeripheral({params}, callback(ret))

## params

peripheralUUIDs：

- 类型：字符串
- 描述：指定的蓝牙外围设备的 UUID 组成的数组


## callback(ret)

ret：

- 类型：JSON 对象
- 描述：若没有则返回空
- 内部字段：

```js
{
    peripherals:[{ //数组类型；获取到的蓝牙外围设备信息
      uuid: '',    //字符串类型；获取到的蓝牙设备的uuid
      name: '',    //字符串类型；获取到的蓝牙设备的名字
      rssi:  ,     //数字类型；获取到的蓝牙设备的信号强度
      services:[]  //数组类型；获取到的蓝牙设备的所有服务 UUID 的集合
    },...]
}
```

## 示例代码

```js
var ble = api.require('ble');
ble.retrievePeripheral({
    peripheralUUIDs: ['', '']
}, function(ret) {
    if (ret) {
        api.alert({ msg: JSON.stringify(ret) });
    }
});
```

## 可用性

iOS系统

可提供的1.0.0及更高版本

<div id="retrieveConnectedPeripheral"></div>

# **retrieveConnectedPeripheral**

根据指定的服务，找到当前系统处于连接状态的蓝牙中包含这个服务的所有蓝牙外围设备信息**Andaroid 平台暂不支持本接口**

retrieveConnectedPeripheral({params}, callback(ret))

## params


serviceUUIDs

- 类型：数组
- 描述：指定的蓝牙4.0设备的服务（service）的 UUID（字符串） 组成的数组


## callback(ret)

ret：

- 类型：JSON 对象
- 描述：若没有则返回空
- 内部字段：

```js
{
    peripherals:[{ //数组类型；获取到的当前处于连接状态的蓝牙外围设备
      uuid: '',    //字符串类型；处于连接状态的蓝牙设备的uuid
      name: '',    //字符串类型；处于连接状态的蓝牙设备的名字
      rssi:   ,    //数字类型；处于连接状态的蓝牙设备的信号强度
      services:[]  //数组类型；处于连接状态的蓝牙设备的所有服务 UUID 的集合
    },...]
}
```

## 示例代码

```js
var ble = api.require('ble');
ble.retrieveConnectedPeripheral({
    serviceUUIDs: ['dsfs', 'sdf']
}, function(ret) {
    if (ret) {
        api.alert({ msg: JSON.stringify(ret) });
    }
});
```

## 可用性

iOS系统

可提供的1.0.0及更高版本

<div id="discoverService"></div>

# **discoverService**

根据指定的外围设备 UUID 获取该外围设备的所有服务

discoverService({params}, callback(ret, err))

## params

peripheralUUID：

- 类型：字符串
- 描述：指定的蓝牙外围设备的 UUID 

## callback(ret, err)

ret:

- 类型：JSON 对象
- 内部字段：

```js
{
     status: true ,   //布尔类型；是否获取成功，true|false
     services:[]      //数组类型；获取的所有服务号集合
}
```

err:

- 类型：JSON 对象
- 内部字段：

```js
{
     code: 1          //数字类型；连接失败时返回错误码，取值范围如下：
                      //-1：未知错误
                      //1：peripheralUUID 为空
                      //2：尚未搜索到该蓝牙设备
}
```

## 示例代码

```js
var ble = api.require('ble');
ble.discoverService({
    peripheralUUID: ''
}, function(ret) {
    if (ret) {
        api.alert({ msg: JSON.stringify(ret) });
    }
});
```

## 可用性

iOS系统，Android系统

可提供的1.0.0及更高版本

<div id="discoverCharacteristics"></div>

# **discoverCharacteristics**

根据指定的外围设备 UUID 及其服务 UUID 获取该外围设备的所有特征（Characteristic）

discoverCharacteristics({params}, callback(ret, err))

## params

serviceUUID

- 类型：字符串
- 描述：指定的服务的 UUID 


peripheralUUID：

- 类型：字符串
- 描述：指定的蓝牙外围设备的 UUID 

## callback(ret, err)

ret:

- 类型：JSON 对象
- 内部字段：

```js
{
     status: true ,      //布尔类型；是否获取成功，true|false
     characteristics:[{  //数组类型；获取的所有特征信息的集合
        uuid: '',        //字符串类型；特征的 UUID 
        serviceUUID: '', //字符串类型；服务的 UUID 
        permissions: '', //字符串类型；特征的权限，取值范围如下：
                         //readable：
                         //writeable：
                         //readEncryptionRequired：
                         //writeEncryptionRequired：
        properties: ''    //字符串类型；特征的属性，取值范围如下：
                         //broadcast：
                         //read：
                         //writeWithoutResponse：
                         //write：
                         //notify：
                         //indicate：
                         //authenticatedSignedWrites：
                         //extendedProperties：
                         //notifyEncryptionRequired：
                         //indicateEncryptionRequired：
     }]   
}
```

err:

- 类型：JSON 对象
- 内部字段：

```js
{
     code: 1          //数字类型；连接失败时返回错误码，取值范围如下：
                      //-1：未知错误
                      //1：peripheralUUID 为空
                      //2：serviceUUID 为空
                      //3：未找到指定服务（service）
                      //4：尚未搜索到该蓝牙设备
}
```

## 示例代码

```js
var ble = api.require('ble');
ble.discoverCharacteristics({
    peripheralUUID: ''
}, function(ret) {
    if (ret) {
        api.alert({ msg: JSON.stringify(ret) });
    }
});
```

## 可用性

iOS系统，Android系统

可提供的1.0.0及更高版本

<div id="discoverDescriptorsForCharacteristic"></div>

# **discoverDescriptorsForCharacteristic**

根据指定的外围设备 UUID 及其服务 UUID 和特征 UUID 获取该外围设备的所有描述符（Descriptor）

discoverDescriptorsForCharacteristic({params}, callback(ret, err))

## params

peripheralUUID：

- 类型：字符串
- 描述：指定的蓝牙外围设备的 UUID 


serviceUUID

- 类型：字符串
- 描述：指定的服务的 UUID 

characteristicUUID

- 类型：字符串
- 描述：指定的特征的 UUID 

## callback(ret, err)

ret:

- 类型：JSON 对象
- 内部字段：

```js
{
     status: true ,      //布尔类型；是否读取成功，true|false
     descriptors:[{      //数组类型；获取的所有描述符信息的集合
        uuid: '',        //字符串类型；描述符的 UUID 
        serviceUUID: '', //字符串类型；服务的 UUID 
        characteristicUUID:'',//字符串类型；特征的 UUID 
        decode: ,        //布尔类型；描述符的值是否是二进制类型数据
        value:           //字符串类型；描述符的值，若 decode 为 true，则该值为转码后的值
     }]      
}
```

err:

- 类型：JSON 对象
- 内部字段：

```js
{
     code: 1          //数字类型；连接失败时返回错误码，取值范围如下：
                      //-1：未知错误
                      //1：peripheralUUID 为空
                      //2：serviceUUID 为空
                      //3：characteristicUUID 为空
                      //4：未找到指定特征（characteristic）
                      //5：未找到指定服务（service）
                      //6：尚未搜索到该蓝牙设备
}
```

## 示例代码

```js
var ble = api.require('ble');
ble.discoverDescriptorsForCharacteristic({
    peripheralUUID: '',
    serviceUUID: '',
    characteristicUUID: ''
}, function(ret) {
    if (ret) {
        api.alert({ msg: JSON.stringify(ret) });
    }
});
```

## 可用性

iOS系统，Android系统

可提供的1.0.0及更高版本

<div id="setNotify"></div>

# **setNotify**

根据指定的外围设备 UUID 及其服务 UUID 和特征 UUID 监听数据回发

setNotify({params}, callback(ret, err))

## params

peripheralUUID：

- 类型：字符串
- 描述：指定的蓝牙外围设备的 UUID 

serviceUUID

- 类型：字符串
- 描述：指定的服务的 UUID 

characteristicUUID

- 类型：字符串
- 描述：指定的特征的 UUID 

## callback(ret, err)

ret:

- 类型：JSON 对象
- 描述：每有数据接收便会触发此回调
- 内部字段：

```js
{
     status: true ,      //布尔类型；是否获取数据，true|false
     characteristic:{    //JSON对象；获取监听的特征的信息
        uuid: '',        //字符串类型；特征的 UUID 
        serviceUUID: '', //字符串类型；服务的 UUID 
        value:  ,        //字符串类型；特征的值
        permissions: '', //字符串类型；特征的权限，取值范围如下：
                         //readable：
                         //writeable：
                         //readEncryptionRequired：
                         //writeEncryptionRequired：
        propertie: ''    //字符串类型；特征的属性，取值范围如下：
                         //broadcast：
                         //read：
                         //writeWithoutResponse：
                         //write：
                         //notify：
                         //indicate：
                         //authenticatedSignedWrites：
                         //extendedProperties：
                         //notifyEncryptionRequired：
                         //indicateEncryptionRequired：
     }      
}
```

err:

- 类型：JSON 对象
- 内部字段：

```js
{
     code: 1          //数字类型；连接失败时返回错误码，取值范围如下：
                      //-1：未知错误
                      //1：peripheralUUID 为空
                      //2：serviceUUID 为空
                      //3：characteristicUUID 为空
                      //4：未找到指定特征（characteristic）
                      //5：未找到指定服务（service）
                      //6：尚未搜索到该蓝牙设备
}
```

## 示例代码

```js
var ble = api.require('ble');
ble.setNotify({
    peripheralUUID: '',
    serviceUUID: '',
    characteristicUUID: ''
}, function(ret) {
    if (ret) {
        api.alert({ msg: JSON.stringify(ret) });
    }
});
```

## 可用性

iOS系统，Android系统

可提供的1.0.0及更高版本

<div id="stopNotify"></div>

# **stopNotify**

停止监听数据。调用setNotify接口后开始监听数据，不需要继续监听时调用disconnect断开链接，在iOS 平台上还需要调用此接口来停止监听。

stopNotify()


## 示例代码

```js
var ble = api.require('ble');
ble.stopNotify();
```

## 可用性

iOS系统

可提供的1.0.4及更高版本

<div id="readValueForCharacteristic"></div>

# **readValueForCharacteristic**

根据指定的外围设备 UUID 及其服务 UUID 和特征 UUID 读取数据

readValueForCharacteristic({params}, callback(ret, err))

## params

peripheralUUID：

- 类型：字符串
- 描述：指定的蓝牙外围设备的 UUID 

serviceUUID

- 类型：字符串
- 描述：指定的服务的 UUID 

characteristicUUID

- 类型：字符串
- 描述：指定的特征的 UUID 

## callback(ret, err)

ret:

- 类型：JSON 对象
- 描述：每有数据接收便会触发此回调
- 内部字段：

```js
{
     status: true ,      //布尔类型；是否读取成功，true|false
     characteristic:{    //JSON对象；获取监听的特征的信息
        uuid: '',        //字符串类型；特征的 UUID 
        serviceUUID: '', //字符串类型；服务的 UUID 
        value:  ,        //字符串类型；特征的值
        permissions: '', //字符串类型；特征的权限，取值范围如下：
                         //readable：
                         //writeable：
                         //readEncryptionRequired：
                         //writeEncryptionRequired：
        propertie: ''    //字符串类型；特征的属性，取值范围如下：
                         //broadcast：
                         //read：
                         //writeWithoutResponse：
                         //write：
                         //notify：
                         //indicate：
                         //authenticatedSignedWrites：
                         //extendedProperties：
                         //notifyEncryptionRequired：
                         //indicateEncryptionRequired：
     }      
}
```

err:

- 类型：JSON 对象
- 内部字段：

```js
{
     code: 1          //数字类型；连接失败时返回错误码，取值范围如下：
                      //-1：未知错误
                      //1：peripheralUUID 为空
                      //2：serviceUUID 为空
                      //3：characteristicUUID 为空
                      //4：未找到指定特征（characteristic）
                      //5：未找到指定服务（service）
                      //6：尚未搜索到该蓝牙设备
}
```

## 示例代码

```js
var ble = api.require('ble');
ble.readValueForCharacteristic({
    peripheralUUID: '',
    serviceUUID: '',
    characteristicUUID: ''
}, function(ret) {
    if (ret) {
        api.alert({ msg: JSON.stringify(ret) });
    }
});
```

## 可用性

iOS系统，Android系统

可提供的1.0.0及更高版本

<div id="readValueForDescriptor"></div>

# **readValueForDescriptor**

根据指定的外围设备 UUID 及其服务 UUID 和特征 UUID 及其描述符获取数据

readValueForDescriptor({params}, callback(ret, err))

## params

peripheralUUID：

- 类型：字符串
- 描述：指定的蓝牙外围设备的 UUID 


serviceUUID

- 类型：字符串
- 描述：指定的服务的 UUID 

characteristicUUID

- 类型：字符串
- 描述：指定的特征的 UUID 

descriptorUUID

- 类型：字符串
- 描述：指定的描述符的 UUID 

## callback(ret, err)

ret:

- 类型：JSON 对象
- 内部字段：

```js
{
     status: true ,      //布尔类型；是否读取成功，true|false
     descriptor:{        //JSON对象；获取的所有描述符信息
        uuid: '',        //字符串类型；描述符的 UUID 
        serviceUUID: '', //字符串类型；服务的 UUID 
        characteristicUUID:'',//字符串类型；特征的 UUID 
        decode: ,        //布尔类型；描述符的值是否是二进制类型数据
        value:           //字符串类型；描述符的值，若 decode 为 true，则该值为转码后的值
     }      
}
```

err:

- 类型：JSON 对象
- 内部字段：

```js
{
     code: 1          //数字类型；连接失败时返回错误码，取值范围如下：
                      //-1：未知错误
                      //1：peripheralUUID 为空
                      //2：serviceUUID 为空
                      //3：characteristicUUID 为空
                      //4：descriptorUUID 为空
                      //5：未找到指定描述符（descriptor）
                      //6：未找到指定特征（characteristic）
                      //7：未找到指定服务（service）
                      //8：尚未搜索到该蓝牙设备
}
```

## 示例代码

```js
var ble = api.require('ble');
ble.readValueForDescriptor({
    peripheralUUID: '',
    serviceUUID: '',
    characteristicUUID: '',
    descriptorUUID: ''
}, function(ret) {
    if (ret) {
        api.alert({ msg: JSON.stringify(ret) });
    }
});
```

## 可用性

iOS系统，Android系统

可提供的1.0.0及更高版本

<div id="writeValueForCharacteristic"></div>

# **writeValueForCharacteristic**

根据指定的外围设备 UUID 及其服务 UUID 和特征 UUID 写数据

writeValueForCharacteristic({params}, callback(ret, err))

## params

peripheralUUID：

- 类型：字符串
- 描述：指定的蓝牙外围设备的 UUID 

serviceUUID

- 类型：字符串
- 描述：指定的服务的 UUID 

characteristicUUID

- 类型：字符串
- 描述：指定的特征的 UUID 

value

- 类型：字符串
- 描述：要写入的数据 ，十六进制的字符串

writeType

- 类型：字符串
- 描述：（可选项）写入数据时的类型
- 默认：auto
- 取值范围：
	- auto：模块自动选择类型
	- response：有回调
	- withoutResponse：无回调

## callback(ret, err)

ret:

- 类型：JSON 对象
- 描述：每有数据接收便会触发此回调
- 内部字段：

```js
{
     status: true ,      //布尔类型；是否发送成功，true|false
     characteristic:{    //JSON对象；获取监听的特征的信息
        uuid: '',        //字符串类型；特征的 UUID 
        serviceUUID: '', //字符串类型；服务的 UUID 
        permissions: '', //字符串类型；特征的权限，取值范围如下：
                         //readable：
                         //writeable：
                         //readEncryptionRequired：
                         //writeEncryptionRequired：
        propertie: ''    //字符串类型；特征的属性，取值范围如下：
                         //broadcast：
                         //read：
                         //writeWithoutResponse：
                         //write：
                         //notify：
                         //indicate：
                         //authenticatedSignedWrites：
                         //extendedProperties：
                         //notifyEncryptionRequired：
                         //indicateEncryptionRequired：
     }      
}
```

err:

- 类型：JSON 对象
- 内部字段：

```js
{
     code: 1          //数字类型；失败时返回错误码，取值范围如下：
                      //-1：未知错误
                      //1：peripheralUUID 为空
                      //2：serviceUUID 为空
                      //3：characteristicUUID 为空
                      //4：value 为空
                      //5：未找到指定特征（characteristic）
                      //6：未找到指定服务（service）
                      //7：尚未搜索到该蓝牙设备
}
```

## 示例代码

```js
var ble = api.require('ble');
ble.writeValueForCharacteristic({
    peripheralUUID: '',
    serviceUUID: '',
    characteristicUUID: '',
    value: ''
}, function(ret) {
    if (ret) {
        api.alert({ msg: JSON.stringify(ret) });
    }
});
```

## 可用性

iOS系统，Android系统

可提供的1.0.0及更高版本

<div id="writeValueForDescriptor"></div>

# **writeValueForDescriptor**

根据指定的外围设备 UUID 及其服务 UUID 和特征 UUID 及其描述符发送数据

writeValueForDescriptor({params}, callback(ret, err))

## params

peripheralUUID：

- 类型：字符串
- 描述：指定的蓝牙外围设备的 UUID 

serviceUUID

- 类型：字符串
- 描述：指定的服务的 UUID 

characteristicUUID

- 类型：字符串
- 描述：指定的特征的 UUID 

descriptorUUID

- 类型：字符串
- 描述：指定的描述符的 UUID 

value

- 类型：字符串
- 描述：要发送的数据，十六进制的字符串

## callback(ret, err)

ret:

- 类型：JSON 对象
- 内部字段：

```js
{
     status: true ,      //布尔类型；是否发送成功，true|false
     characteristic:{    //JSON对象；获取监听的特征的信息
        uuid: '',        //字符串类型；特征的 UUID 
        serviceUUID: '', //字符串类型；服务的 UUID 
        permissions: '', //字符串类型；特征的权限，取值范围如下：
                         //readable：
                         //writeable：
                         //readEncryptionRequired：
                         //writeEncryptionRequired：
        propertie: '' ,  //字符串类型；特征的属性，取值范围如下：
                         //broadcast：
                         //read：
                         //writeWithoutResponse：
                         //write：
                         //notify：
                         //indicate：
                         //authenticatedSignedWrites：
                         //extendedProperties：
                         //notifyEncryptionRequired：
                         //indicateEncryptionRequired：
        descriptors:[{        //数组类型；获取的所有描述符信息的集合
	        uuid: '',         //字符串类型；描述符的 UUID 
	        serviceUUID: '',  //字符串类型；服务的 UUID 
	        characteristicUUID:'',//字符串类型；特征的 UUID 
	        decode: ,         //布尔类型；描述符的值是否是二进制类型数据
	        value:            //字符串类型；描述符的值，若 decode 为 true，则该值为转码后的值
        }] 
     }       
}
```

err:

- 类型：JSON 对象
- 内部字段：

```js
{
     code: 1          //数字类型；失败时返回错误码，取值范围如下：
                      //-1：未知错误
                      //1：peripheralUUID 为空
                      //2：serviceUUID 为空
                      //3：characteristicUUID 为空
                      //4：descriptorUUID 为空
                      //5：value 为空
                      //6：未找到指定描述符（descriptor）
                      //7：未找到指定特征（characteristic）
                      //8：未找到指定服务（service）
                      //9：尚未搜索到该蓝牙设备
}
```

## 示例代码

```js
var ble = api.require('ble');
ble.writeValueForDescriptor({
    peripheralUUID: '',
    serviceUUID: '',
    characteristicUUID: '',
    descriptorUUID: ''
}, function(ret) {
    if (ret) {
        api.alert({ msg: JSON.stringify(ret) });
    }
});
```

## 可用性

iOS系统，Android系统

可提供的1.0.0及更高版本

<div id="connectPeripherals"></div>

# **connectPeripherals**

连接多台外围设备

connectPeripherals({params}, callback(ret))

## params

peripheralUUIDs：

- 类型：数组
- 描述：要连接的外围设备的 UUID 字符串组成的数组


## callback(ret)

ret:

- 类型：JSON 对象
- 描述：peripheralUUIDs 传入多少个 id 则本回调执行多少次
- 内部字段：

```js
{
     status: true      //布尔类型；是否连接成功，true|false
     peripheralUUID:'' //字符串类型；所要链接的外围设备的 id
}
```

## 示例代码

```js
var ble = api.require('ble');
ble.peripheralUUIDs({
    peripheralUUIDs: ['', '', '']
}, function(ret, err) {
    if (ret.status) {
        alert(ret.peripheralUUID + "连接成功！");
    }
});
```

## 可用性

iOS系统，Android系统

可提供的1.0.0及更高版本

<div id="setSimpleNotify"></div>

# **setSimpleNotify**

根据指定的外围设备 UUID 及其服务 UUID 和特征 UUID 监听数据

setSimpleNotify({params}, callback(ret, err))

## params

peripheralUUID：

- 类型：字符串
- 描述：指定的蓝牙外围设备的 UUID 

serviceUUID

- 类型：字符串
- 描述：指定的服务的 UUID 

characteristicUUID

- 类型：字符串
- 描述：指定的特征的 UUID 

## callback(ret, err)

ret:

- 类型：JSON 对象
- 描述：每有数据接收便会触发此回调
- 内部字段：

```js
{
     status: true        //布尔类型；是否获取数据，true|false  
}
```

err:

- 类型：JSON 对象
- 内部字段：

```js
{
     code: 1          //数字类型；连接失败时返回错误码，取值范围如下：
                      //1：peripheralUUID 为空
                      //2：serviceUUID 为空
                      //3：characteristicUUID 为空
                      //4：未找到指定特征（characteristic）
                      //5：未找到指定服务（service）
                      //6：尚未搜索到该蓝牙设备
}
```

## 示例代码

```js
var ble = api.require('ble');
ble.setSimpleNotify({
    peripheralUUID: '',
    serviceUUID: '',
    characteristicUUID: ''
}, function(ret, err) {
    if (!ret.status) {
        api.alert({ msg: JSON.stringify(err) });
    }
});
```

## 可用性

iOS系统，Android系统

<div id="getAllSimpleNotifyData"></div>

# **getAllSimpleNotifyData**

获取模块当前缓存的所监听蓝牙设备的所有数据

getAllSimpleNotifyData(callback(ret))

## callback(ret)

ret:

- 类型：JSON 对象
- 内部字段：

```js
{
     '':                  //模块当前缓存到的外围设备的 UUID，以此为 key 读取取相应的数据信息
     {                    //JSON对象；模块当前缓存到的外围设备发来的数据信息 
        serviceUUID: '',  //字符串类型；服务的 UUID 
        characterUUID: '',//字符串类型；特征的 UUID 
        data:['','','']   //数组类型；特征的值组成的数组，内部元素值为字符串类型
     }       
}
```

## 示例代码

```js
var ble = api.require('ble');
ble.getAllSimpleNotifyData(function(ret) {
    api.alert({ msg: JSON.stringify(ret) });
});
```

## 可用性

iOS系统，Android系统

可提供的1.0.0及更高版本

<div id="clearAllSimpleNotifyData"></div>

# **clearAllSimpleNotifyData**

清空模块当前缓存的所监听蓝牙设备的所有数据

clearAllSimpleNotifyData()

## 示例代码

```js
var ble = api.require('ble');
ble.clearAllSimpleNotifyData();
```

## 可用性

iOS系统，Android系统

可提供的1.0.0及更高版本