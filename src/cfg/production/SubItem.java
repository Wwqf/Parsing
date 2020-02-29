package cfg.production;

import java.io.Serializable;

/**
 * 一个子项，可以是终结符，可以是非终结符
 */
public class SubItem implements Cloneable, Serializable {
	/**
	 * 每个子项包含自身的值和属性类型
	 */
	private String value;
	private SubItemType type;

	public SubItem(String value, SubItemType type) {
		this.value = value;
		this.type = type;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setType(SubItemType type) {
		this.type = type;
	}

	public SubItemType getType() {
		return type;
	}



	public SubItem copy() {
		try {
			return clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			System.exit(-2);
		}
		return null;
	}

	@Override
	protected SubItem clone() throws CloneNotSupportedException {
		return (SubItem) super.clone();
	}
}
