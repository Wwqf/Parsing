package slr;

import cfg.production.SubItem;

/**
 * LR自动机 项
 */
public class Item {
	public static final char Point = '·';

	// 产生式的编号
	private int productionId;
	// 每个项 · 的位置
	private int pointPos;
	//
	private ProductionIdGenerate productionIdGenerate;

	public Item(ProductionIdGenerate productionIdGenerate, int productionId, int pointPos) {
		this.productionIdGenerate = productionIdGenerate;
		this.productionId = productionId;
		this.pointPos = pointPos;
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

	// 获取期望子项，即下一个需要匹配的符号
	// 当要进行归约时（即没有期望子项）返回null，否则返回子项
	public SubItem getExpectSubItem() {
		if (pointPos >= productionIdGenerate.getProduction(productionId).getSubItems().size()) return null;

		SubItem subItem = productionIdGenerate.getExpectSubItem(productionId, pointPos);
		if (subItem.getValue().equals("ε")) return null;
		return subItem;
	}

}
