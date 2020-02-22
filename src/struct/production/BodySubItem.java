package struct.production;

/**
 * 产生式体的子项
 * 例如：E -> T * E
 * 其中，T、*、E 分别是产生式体中的三个子项
 */
public class BodySubItem {
	/**
	 * 每个子项包含自身的值和属性类型
	 */
	private String value;
	private BodySubItemAttrType attr;

	public BodySubItem(String value, BodySubItemAttrType attr) {
		this.value = value;
		this.attr = attr;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setAttr(BodySubItemAttrType attr) {
		this.attr = attr;
	}

	public BodySubItemAttrType getAttr() {
		return attr;
	}
}
