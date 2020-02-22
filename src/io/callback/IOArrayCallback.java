package io.callback;

import io.FileAttrCode;

import java.util.List;

public interface IOArrayCallback {
	void read(List<String> array, FileAttrCode code);
}
