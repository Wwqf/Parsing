package io.callback;

import com.alibaba.fastjson.JSONObject;
import io.FileAttrCode;

public interface IOJsonCallback {
	void read(JSONObject content, FileAttrCode code);
}
