package lr;

import algorithm.FirstSet;
import cfg.CFG;
import cfg.production.Production;
import cfg.production.ProductionGroup;
import cfg.production.SubItem;
import cfg.production.SubItemType;
import logger.IOColor;
import slr.ProductionIdGenerate;

import java.util.*;

public class ItemCollection {

	private ProductionIdGenerate idGenerate;
	private ItemSet startItemSet;
	private Set<ItemSet> lrItemSets;
	private Set<ItemSet> lrItemSets_remove;
	private FirstSet firstSet;

	public ItemCollection(CFG cfg, FirstSet firstSet) {
		this.idGenerate = ProductionIdGenerate.getInstance(cfg);
		this.firstSet = firstSet;
		this.lrItemSets = new LinkedHashSet<>();
		this.lrItemSets_remove = new HashSet<>();

		initStartItemSet();
	}

	private void initStartItemSet() {
		// 获取开始符号，然后定义增广文法
		String startSym = idGenerate.getCfg().getStartSymbol();
		// 获取新增名
		String newStartSym = idGenerate.getCfg().getAddNonTerminalName(startSym);
		// 生成一条产生式
		Production production = Production.createProduction(new SubItem(startSym, SubItemType.nonTerminal));
		production.resetStr();
		ProductionGroup productionGroup = new ProductionGroup();
		productionGroup.addProduction(production);
		// 添加到cfg
		idGenerate.getCfg().getNonTerminals().add(newStartSym);
		idGenerate.getCfg().getProductionGroupMap().put(newStartSym, productionGroup);
		idGenerate.getCfg().setStartSymbol(newStartSym);
		// idGenerate重新生成id
		idGenerate.resetId();
		// 添加到项集族
		Set<String> set = new HashSet<>();
		set.add("$");
		startItemSet = new ItemSet(idGenerate, firstSet);
		startItemSet.addItem(new Item(idGenerate, production.getId(), 0, set));
		startItemSet.closure();

		lrItemSets.add(startItemSet);
	}

	public void resetId() {
		ItemSet.itemSetIdCount = -1;
		for (ItemSet lrItemSet : lrItemSets) {
			lrItemSet.setId(++ItemSet.itemSetIdCount);
		}
	}

	public void generateItemCollection() {
		Queue<ItemSet> rec = gotoLrItemSet(startItemSet);

		while (!rec.isEmpty()) {
			ItemSet itemSet = rec.poll();
			rec.addAll(gotoLrItemSet(itemSet));
			lrItemSets.removeAll(lrItemSets_remove);
			lrItemSets_remove.clear();
		}
	}

	private Queue<ItemSet> gotoLrItemSet(ItemSet lrItemSet) {
		// 对于lrItemSet中的每个项，进行移入规约查询，
		// 如果可以移入，则创建一个新项集，然后将可以移入的项加入

		Map<String, ItemSet> gotoTables = lrItemSet.getGotoTables();

		Queue<ItemSet> result;
		result = classificationQuery(gotoTables, lrItemSet.getLrItems());

		// 检查合并相同项
		Map<String, ItemSet> update = new HashMap<>();

		gotoTables.forEach((key, value) -> {
			value.closure();

			lrItemSets.forEach(item -> {
				if (item == value) return;

				if (item.contains(value)) {
					update.put(key, item);
					result.remove(value);
					lrItemSets_remove.add(value);
				}
			});
		});
		gotoTables.putAll(update);
		return result;
	}


	/**
	 * 对items进行归类查询，有相同转换的归为一个项集
	 * @param gotoTables goto表
	 * @param items 项集
	 * @return 返回新增项集
	 */
	private Queue<ItemSet> classificationQuery(Map<String, ItemSet> gotoTables, Set<Item> items) {

		Queue<ItemSet> result = new LinkedList<>();

		for (Item lki : items) {
			SubItem subItem = lki.getExpectSubItem();

			// 规约，不可移入，跳过
			if (subItem == null) continue;

			// 查看goto表是否有相同key转换
			ItemSet sets = gotoTables.get(subItem.getValue());

			// 如果没有则新建项集，然后添加
			if (sets == null) {
				sets = new ItemSet(idGenerate, firstSet);
				// 添加到goto表
				gotoTables.put(subItem.getValue(), sets);
				// 添加到项目族
				lrItemSets.add(sets);
				// 添加到返回结果
				result.add(sets);
			}

			// 如果有则添加到项集中, lookhead为继承
			sets.addItem(new Item(idGenerate, lki.getProductionId(), lki.getPointPos() + 1, lki.getLookheads()));
		}
		return result;
	}


	public void printItemCollection() {
		resetId();
		for (ItemSet lrItemSet : lrItemSets) {
			lrItemSet.printItemSet();
		}
	}

	public ProductionIdGenerate getIdGenerate() {
		return idGenerate;
	}

	public ItemSet getStartItemSet() {
		return startItemSet;
	}

	public Set<ItemSet> getLrItemSets() {
		return lrItemSets;
	}

	public FirstSet getFirstSet() {
		return firstSet;
	}
}
