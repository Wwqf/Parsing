package fin.write;

import java.util.List;

public class ArrayWriteContent extends FileWriteContent {
	@Override
	public String construct(Object instance) {
		if (constructStr != null) return constructStr;

		final StringBuilder result = new StringBuilder();
		if (instance instanceof List) {
			List list = (List) instance;
			list.forEach(result::append);
		}
		constructStr = result.toString();
		return constructStr;
	}
}
