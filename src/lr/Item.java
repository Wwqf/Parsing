package lr;

import cfg.production.SubItem;
import slr.ProductionIdGenerate;

import java.util.HashSet;
import java.util.Set;

public class Item {
	public static final char Point = '·';

	// 产生式的编号
	private int productionId;
	// 每个项 · 的位置
	private int pointPos;
	//
	private ProductionIdGenerate productionIdGenerate;

	private Set<String> lookheads;

	public Item(ProductionIdGenerate productionIdGenerate, int productionId, int pointPos, Set<String> lookheads) {
		this.productionIdGenerate = productionIdGenerate;
		this.productionId = productionId;
		this.pointPos = pointPos;
		this.lookheads = lookheads;
	}

	public int getPointPos() {
		return pointPos;
	}

	public int getProductionId() {
		return productionId;
	}

	public String getProductionHead() {
		return productionIdGenerate.getProductionHead(this.productionId);
	}

	public Set<String> getLookheads() {
		return lookheads;
	}

	// 获取期望子项，即下一个需要匹配的符号
	// 当要进行归约时（即没有期望子项）返回null，否则返回子项
	public SubItem getExpectSubItem() {
		if (pointPos >= productionIdGenerate.getProduction(productionId).getSubItems().size()) return null;

		SubItem subItem = productionIdGenerate.getExpectSubItem(productionId, pointPos);
		if (subItem.getValue().equals("ε")) return null;
		return subItem;
	}

	/**
	 * 获取向前看符号的子项描述
	 * @return
	 */
	public SubItem getLookheadSubItem() {
		if (pointPos + 1 >= productionIdGenerate.getProduction(productionId).getSubItems().size()) return null;
		return productionIdGenerate.getExpectSubItem(productionId, pointPos + 1);
	}

	public SubItem getLookheadSubItem(int offset) {
		if (pointPos + 1 + offset >= productionIdGenerate.getProduction(productionId).getSubItems().size()) return null;
		return productionIdGenerate.getExpectSubItem(productionId, pointPos + 1 + offset);
	}

	public boolean equalsCore(Item other) {
		return productionId == other.productionId &&
				pointPos == other.pointPos;
	}

	public boolean include(ItemSet itemSet) {
		for (Item lrItem : itemSet.getLrItems()) {
			if (lrItem.equals(this)) return true;
		}
		return false;
	}

	public boolean includeCore(ItemSet itemSet) {
		for (Item lrItem : itemSet.getLrItems()) {
			if (lrItem.equalsCore(this)) return true;
		}
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Item) {
			return (productionId == ((Item) obj).productionId &&
					pointPos == ((Item) obj).pointPos &&
					lookheads.containsAll(((Item) obj).lookheads) &&
					((Item) obj).getLookheads().containsAll(lookheads));
		}
		return false;
	}

}
