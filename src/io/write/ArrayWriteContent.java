package io.write;

import java.util.List;
import java.util.function.Consumer;

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
