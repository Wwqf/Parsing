package fin.callback;


import fin.FileAttrCode;

import java.util.List;

public interface IOArrayCallback {
	void read(List<String> array, FileAttrCode code);
}
