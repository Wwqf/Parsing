package fin.callback;


import fin.FileAttrCode;

import java.io.BufferedReader;

public interface IOBufferCallback {
	void read(BufferedReader reader, FileAttrCode code);
}
