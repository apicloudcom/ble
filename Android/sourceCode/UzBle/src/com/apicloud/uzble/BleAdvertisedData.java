package com.apicloud.uzble;

import java.util.List;
import java.util.UUID;

public class BleAdvertisedData {
	private List<UUID> mUuids;
	private String mName;

	public BleAdvertisedData(List<UUID> uuids, String name) {
		mUuids = uuids;
		mName = name;
	}

	public List<UUID> getUuids() {
		return mUuids;
	}

	public String getName() {
		return mName;
	}
}