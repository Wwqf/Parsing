package io.callback;

import io.FileAttrCode;

import java.io.BufferedReader;

public interface IOBufferCallback {
	void read(BufferedReader reader, FileAttrCode code);
}
