package cfg.production;

import java.io.*;
import java.util.Collections;
import java.util.LinkedList;

/**
 * 一条产生式, 由子项序列组成
 * 每个产生式都有标记
 */
public class Production implements Cloneable {
	public static int globalIdCount = -1;

	private int id;
	private String productionStr;
	private LinkedList<SubItem> subItems = new LinkedList<>();

	public Production(String productionStr) {
		this.productionStr = productionStr;
		id = ++globalIdCount;
	}

	public Production(LinkedList<SubItem> subItems) {
		this.subItems = subItems;
		id = ++globalIdCount;
	}

	/**
	 * 根据子项序列，重置产生式字符串
	 * @return 重置后的字符串
	 */
	public String resetStr() {
		if (subItems.isEmpty()) {
			productionStr = "";
		} else {
			StringBuilder builder = new StringBuilder();

			for (int i = 0; i < subItems.size() - 1; i++) {
				builder.append(subItems.get(i).getValue()).append(' ');
			}
			builder.append(subItems.get(subItems.size() - 1).getValue());
			productionStr = builder.toString();
		}
		return productionStr;
	}

	/**
	 * 两个产生式有没有左公因子
	 * @param other 另外一个产生式
	 * @return true为有，反之没有
	 */
	public boolean commonFactor(Production other) {
		return other.getSubItems().getFirst().getValue().equals(this.getSubItems().getFirst().getValue());
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getProductionStr() {
		return productionStr;
	}

	public void setProductionStr(String productionStr) {
		this.productionStr = productionStr;
	}

	public LinkedList<SubItem> getSubItems() {
		return subItems;
	}

	public void setSubItems(LinkedList<SubItem> subItems) {
		this.subItems = subItems;
	}

	/**
	 * 对外接口
	 * @return 返回production的clone
	 */
	public Production copy() {
		try {
			return clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			System.exit(-2);
		}
		return null;
	}


	/**
	 * 经过几轮测试，序列化版深度拷贝几乎是遍历拷贝的三倍耗时，
	 * 所以最后决定还是用遍历版；Collections.copy属于浅拷贝，则不考虑
	 * @return 返回production的clone
	 * @throws CloneNotSupportedException 该类没有实现Cloneable
	 */
	@Override
	protected Production clone() throws CloneNotSupportedException {
		Production cloProduction = (Production) super.clone();
		cloProduction.subItems = copy(subItems);
		return cloProduction;
	}

	private LinkedList<SubItem> copy(LinkedList<SubItem> src) {
		LinkedList<SubItem> dest = new LinkedList<>();
		for (SubItem t : src) dest.add(t.copy());
		return dest;
	}


	public static Production createProduction(SubItem... subItems) {
		LinkedList<SubItem> apSubItems = new LinkedList<>();
		Collections.addAll(apSubItems, subItems);
		return new Production(apSubItems);
	}

	public static Production createProduction(LinkedList<SubItem> subItems) {
		return new Production(subItems);
	}

	public static Production createProduction(String subItemStr, SubItemType type) {
		LinkedList<SubItem> apSubItems = new LinkedList<>();
		apSubItems.add(new SubItem(subItemStr, type));
		return new Production(apSubItems);
	}
}
