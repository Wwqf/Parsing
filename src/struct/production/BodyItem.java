package struct.production;

import java.util.LinkedList;

/**
 * 存放一个产生式体，包含此产生式体的字符串和产生式体的组成形式序列
 * 为了匹配方便!
 */
public class BodyItem {
	private String bodyStr;
	private LinkedList<BodySubItem> subItems = new LinkedList<>();

	public BodyItem() { }

	public BodyItem(String bodyStr) {
		this.bodyStr = bodyStr;
	}

	public void resetBodyStr() {
		if (subItems.isEmpty()) {
			bodyStr = "";
			return ;
		}
		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < subItems.size() - 1; i++) {
			builder.append(subItems.get(i).getValue()).append(' ');
		}
		builder.append(subItems.get(subItems.size() - 1).getValue());
		bodyStr = builder.toString();
	}


	public void setBodyStr(String bodyStr) {
		this.bodyStr = bodyStr;
	}

	public String getBodyStr() {
		return bodyStr;
	}

	public void setSubItems(LinkedList<BodySubItem> subItems) {
		this.subItems = subItems;
	}

	public LinkedList<BodySubItem> getSubItems() {
		return subItems;
	}

	public boolean checkCommonFactor(BodyItem other) {
		return other.getSubItems().getFirst().getValue().equals(this.getSubItems().getFirst().getValue());
	}
}
