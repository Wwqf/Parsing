package fin.callback;


import fin.FileAttrCode;

public interface IOBytesCallback {
	void read(byte[] content, FileAttrCode code);
}
