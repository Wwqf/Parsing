package lr;

import algorithm.FirstSet;
import cfg.production.Production;
import cfg.production.ProductionGroup;
import cfg.production.SubItem;
import cfg.production.SubItemType;
import fout.Fout;
import fout.attr.ColumnAttr;
import lr.Item;
import slr.ProductionIdGenerate;

import java.util.*;

public class ItemSet {
	public static int itemSetIdCount = -1;

	// 项集的唯一标号
	private int id;
	// 项集
	private Set<Item> lrItems;

	private ProductionIdGenerate idGenerate;
	private FirstSet firstSet;

	private boolean isClosure = false;

	// goto表，key为一个非终结符或终结符，value为通过key能到达哪些项集
	private Map<String, ItemSet> gotoTables = new HashMap<>();

	public ItemSet(ProductionIdGenerate idGenerate, FirstSet firstSet) {
		this.id = ++itemSetIdCount;
		this.idGenerate = idGenerate;
		this.firstSet = firstSet;
		this.lrItems = new LinkedHashSet<>();
	}

	public ItemSet addItem(Item item) {
		lrItems.add(item);
		return this;
	}

	/**
	 * 为相同核心的项合并lookhead，此方法为LALR构造时使用
	 * @param item
	 * @return
	 */
	public ItemSet addItemCoreLookhead(Item item) {
		boolean has = false;
		for (Item lrItem : lrItems) {
			if (lrItem.equalsCore(item)) {
				lrItem.getLookheads().addAll(item.getLookheads());
				has = true;
				break;
			}
		}

		if (!has) {
			lrItems.add(item);
		}
		return this;
	}

	/**
	 * [A -> α·Bβ, a]
	 * B -> expectSubItem
	 * β -> lookheadSubItem
	 *
	 * 如果β为非终结符，获取β的first集，然后设置新的item的第二分量是first成员；（自生lookhead）
	 * 如果β为终结符，当此终结符不为ε时，不产生新的item，如果为ε，则继承a（继承lookhead）
	 */
	public void closure() {
		if (isClosure) return ;

		Queue<Item> queue = new LinkedList<>(lrItems);

		while (!queue.isEmpty()) {
			Item item = queue.poll();

			SubItem expect = item.getExpectSubItem();
			if (expect != null) {
				// 如果是终结符，跳过
				if (expect.getType() == SubItemType.terminal) continue;

				// 非终结符，获取该非终结符对应的产生式集，断言产生式不可能为null
				ProductionGroup group = idGenerate.getCfg().getProductionGroupMap().get(
						expect.getValue()
				);
				assert group != null;

				// 对每个产生式遍历
				SubItem lookhead = item.getLookheadSubItem();
				Set<String> extend_lookhead = item.getLookheads();
				for (Production production : group.getProductions()) {
					Item newItem = null;

					if (lookhead == null || lookhead.getValue().equals("ε")) {
						// 继承lookhead
						Set<String> new_extend = new HashSet<>(extend_lookhead);
						newItem = new Item(idGenerate, production.getId(), 0, new_extend);
					} else if (lookhead.getType() == SubItemType.nonTerminal) {
						// 自生lookhead
						// 获取first集
						Set<String> first_lk = firstSet.getFirstSet().get(lookhead.getValue());
						Set<String> new_first = new HashSet<>(first_lk);
						newItem = new Item(idGenerate, production.getId(), 0, new_first);
					} else if (lookhead.getType() == SubItemType.terminal) {
						Set<String> set = new HashSet<>();
						set.add(lookhead.getValue());
						newItem = new Item(idGenerate, production.getId(), 0, set);
					}

					// 找一个相同核心的项，如果lookhead不同，则将lookhead加入
					if (newItem != null) {
						boolean newAdd = true;
						for (Item lrItem : lrItems) {
							if (lrItem.equalsCore(newItem)) {
								lrItem.getLookheads().addAll(newItem.getLookheads());
								newAdd = false;
								break;
							}
						}
						if (newAdd) {
							queue.add(newItem);
							lrItems.add(newItem);
						}
					}
				}
			}
		}

		isClosure = true;
	}

	/**
	 * 传入一个项集，判断当前项集是否可能包含该项集
	 * @param other 另外一个项集
	 * @return 如果包含返回true
	 */
	public boolean contains(ItemSet other) {

		for (Item lrItem : other.getLrItems()) {
			if (!lrItem.include(this)) return false;
		}
		return true;
	}

	/**
	 * 传入一个项集，判断当前项集和另一个项集具有相同的核心
	 * @param other 另外一个项集
	 * @return 如果包含返回true
	 */
	public boolean containsCore(ItemSet other) {
		if (other.getLrItems().size() != this.getLrItems().size()) return false;

		for (Item lrItem : other.getLrItems()) {
			if (!lrItem.includeCore(this)) return false;
		}
		return true;
	}

	public Set<Item> getLrItems() {
		return lrItems;
	}

	public Map<String, ItemSet> getGotoTables() {
		return gotoTables;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void printItemSet() {
		Fout fout = new Fout(ColumnAttr.qCreate("ItemSet", "Lookhead", "Goto"));
		fout.setTableName("I" + id);

		for (Item lrItem : lrItems) {
			fout.insert(getProductionStr(lrItem.getProductionHead(), idGenerate.getSubItems(lrItem.getProductionId()), lrItem.getPointPos()));
			fout.insert(lrItem.getLookheads());

			if (lrItem.getExpectSubItem() == null) fout.insert("");
			else fout.insert("Goto -> " + gotoTables.get(lrItem.getExpectSubItem().getValue()).getId());
		}

		fout.fout();
	}

	private String getProductionStr(String head, LinkedList<SubItem> subItems, int pointPos) {
		StringBuilder builder = new StringBuilder();

		builder.append(head).append(" -> ");
		subItems.add(pointPos, new SubItem("·", SubItemType.terminal));
		subItems.forEach(item -> {
			if (!item.getValue().equals("ε"))
				builder.append(item.getValue()).append(' ');
		});
		subItems.remove(pointPos);
		return builder.toString();
	}
}
