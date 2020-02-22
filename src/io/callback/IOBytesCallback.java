package io.callback;

import io.FileAttrCode;

public interface IOBytesCallback {
	void read(byte[] content, FileAttrCode code);
}
