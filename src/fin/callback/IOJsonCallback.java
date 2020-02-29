package fin.callback;

import fin.FileAttrCode;
import com.alibaba.fastjson.JSONObject;

public interface IOJsonCallback {
	void read(JSONObject content, FileAttrCode code);
}
