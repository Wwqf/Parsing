package lr;

import cfg.CFG;
import cfg.CFGBuilder;
import cfg.production.Production;
import cfg.production.SubItem;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * 对传入CFG中的产生式进行重新编号，然后放入到productionIds，项与项集都将拥有此类的唯一实例
 */
public class ProductionIdGenerate {

	private static ProductionIdGenerate instance;

	private Map<Integer, Production> productionIds;
	private Map<Integer, String> productionHeads;
	private CFG cfg;


	private ProductionIdGenerate(CFG cfg) {
		this.cfg = cfg;
		this.productionIds = new HashMap<>();
		this.productionHeads = new HashMap<>();
	}

	public static ProductionIdGenerate getInstance(CFG cfg) {
		if (instance == null) {
			synchronized (ProductionIdGenerate.class) {
				if (instance == null) instance = new ProductionIdGenerate(cfg);
			}
		}
		return instance;
	}

	public void resetId() {
		var entry = cfg.getProductionGroupMap().entrySet();

		Production.globalIdCount = -1;
		// 开始符号先设置id
		for (Production production : cfg.getProductionGroupMap().get(cfg.getStartSymbol()).getProductions()) {
			production.setId(++Production.globalIdCount);

			productionIds.put(Production.globalIdCount, production);
			productionHeads.put(Production.globalIdCount, cfg.getStartSymbol());
		}

		for (var item : entry) {
			if (item.getKey().equals(cfg.getStartSymbol())) continue;

			for (var p : item.getValue().getProductions()) {
				p.setId(++Production.globalIdCount);
				productionIds.put(Production.globalIdCount, p);
				productionHeads.put(Production.globalIdCount, item.getKey());
			}
		}
	}

	public Production getProduction(int id) {
		return productionIds.get(id);
	}

	public String getProductionHead(int id) {
		return productionHeads.get(id);
	}

	public LinkedList<SubItem> getSubItems(int id) {
		return productionIds.get(id).getSubItems();
	}

	public SubItem getExpectSubItem(int id, int pointPos) {
		var subItems = productionIds.get(id).getSubItems();
		if (subItems == null) return null;
		return subItems.get(pointPos);
	}

	public CFG getCfg() {
		return cfg;
	}
}
