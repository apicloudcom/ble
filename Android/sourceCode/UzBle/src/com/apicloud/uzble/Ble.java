package com.apicloud.uzble;

import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;
/**
 * ble bean 类；
 * @author 邓宝成
 *
 */
public class Ble {
	private String peripheralUUID;
	private String serviceId;
	private String characteristicUUID;
	private UZModuleContext moduleContext;

	public Ble(String peripheralUUID, String serviceId,
			String characteristicUUID, UZModuleContext moduleContext) {
		this.peripheralUUID = peripheralUUID;
		this.serviceId = serviceId;
		this.characteristicUUID = characteristicUUID;
		this.moduleContext = moduleContext;
	}

	public String getPeripheralUUID() {
		return peripheralUUID;
	}

	public void setPeripheralUUID(String peripheralUUID) {
		this.peripheralUUID = peripheralUUID;
	}

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public String getCharacteristicUUID() {
		return characteristicUUID;
	}

	public void setCharacteristicUUID(String characteristicUUID) {
		this.characteristicUUID = characteristicUUID;
	}

	public UZModuleContext getModuleContext() {
		return moduleContext;
	}

	public void setModuleContext(UZModuleContext moduleContext) {
		this.moduleContext = moduleContext;
	}
}
