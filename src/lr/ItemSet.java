package lr;

import cfg.CFG;
import cfg.production.Production;
import cfg.production.ProductionGroup;
import cfg.production.SubItem;
import cfg.production.SubItemType;
import fout.Fout;
import fout.attr.ColumnAttr;

import java.util.*;

/**
 * 项集
 */
public class ItemSet {
	public static int itemSetIdCount = -1;

	// 项集的唯一标号
	private int id;
	// 非内核项
	private Set<Item> lrNonKernelItems;
	// 内核项
	private Set<Item> lrKernelItems;

	// goto表，key为一个非终结符或终结符，value为通过key能到达哪些项集
	private Map<String, ItemSet> gotoTables = new HashMap<>();
	// key为产生式编号（productionId），value为此编号对应的pointPos位置
	// 即，在项集中可能存在同一个产生式，但是·的位置不一样
	private Map<Integer, Set<Integer>> productionPointPosSet;

	private ProductionIdGenerate idGenerate;

	public ItemSet(ProductionIdGenerate idGenerate) {
		this.id = ++itemSetIdCount;
		this.idGenerate = idGenerate;
		lrNonKernelItems = new LinkedHashSet<>();
		lrKernelItems = new LinkedHashSet<>();
		productionPointPosSet = new HashMap<>();
	}

	/**
	 *
	 * @param item
	 * @return
	 */
	public ItemSet addItem(Item item) {
		if (item.getPointPos() == 0) lrNonKernelItems.add(item);
		else lrKernelItems.add(item);
		addProductionPointPosSet(item);
		return this;
	}

	/**
	 * 开始符号调用添加到内核项
	 * @param item
	 * @return
	 */
	public ItemSet addKernelLRItem(Item item) {
		lrKernelItems.add(item);
		addProductionPointPosSet(item);
		return this;
	}

	public void closure() {

		Queue<Item> queue = new LinkedList<>();
		queue.addAll(lrKernelItems);
		queue.addAll(lrNonKernelItems);
		// 标志位，某个非终结符是否已经添加过了
		Set<String> alreadyAdds = new HashSet<>();

		while (!queue.isEmpty()) {
			Item item = queue.poll();
			SubItem expect = item.getExpectSubItem();

			// 如果可以进行 ‘移入’ & 是非终结符 & 没有添加过
			if (expect != null &&
					expect.getType() == SubItemType.nonTerminal &&
					!alreadyAdds.contains(expect.getValue())) {
				// 添加flag
				alreadyAdds.add(expect.getValue());

				// 从cfg中查找，将非终结符对应产生式组的每个产生式都加入到非内核
				ProductionGroup productionGroup = idGenerate.getCfg().getProductionGroupMap().get(expect.getValue());
				assert productionGroup != null;

				// 每个产生式都加入到非内核项
				for (Production production : productionGroup.getProductions()) {
					Item lrItem = new Item(idGenerate, production.getId(), 0);
					lrNonKernelItems.add(lrItem);
					addProductionPointPosSet(lrItem);
					// 并且添加到队列中
					queue.add(lrItem);
				}
			}
		}
	}

	/**
	 * 传入一个项集，判断当前项集是否可能包含该项集
	 * @param other 另外一个项集
	 * @return 如果包含返回true
	 */
	public boolean contains(ItemSet other) {
		if (other == this) return false;
		return contains(other.productionPointPosSet);
	}

	private boolean contains(Map<Integer, Set<Integer>> otherProductionPointPosSet) {
		var entry = otherProductionPointPosSet.entrySet();
		for (var item : entry) {
			int keys = item.getKey();
			Set<Integer> values = item.getValue();

			Set<Integer> current = productionPointPosSet.get(keys);
			// 当前项集不包含
			if (current == null) return false;

			// 如果匹配成功，检查·的位置是否一致
			boolean status = current.containsAll(values);
			if (!status) return false;
		}

		return true;
	}

	private void addProductionPointPosSet(Item item) {
		Set<Integer> set = productionPointPosSet.computeIfAbsent(item.getProductionId(), k -> new HashSet<>());
		set.add(item.getPointPos());
	}

	public Map<String, ItemSet> getGotoTables() {
		return gotoTables;
	}

	public Set<Item> getLrKernelItems() {
		return lrKernelItems;
	}

	public Set<Item> getLrNonKernelItems() {
		return lrNonKernelItems;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void printItemSet() {
		Fout fout = new Fout(ColumnAttr.qCreate("KernelItemSet", "NonKernelItemSet"));
		fout.setTableName("I" + id);

		Object[] ko = lrKernelItems.toArray();
		Object[] kno = lrNonKernelItems.toArray();

		int len = Math.max(ko.length, kno.length);
		String strKO = "";
		String strKNO = "";
		for (int i = 0; i < len; i++) {
			if (i < ko.length) {
				String head = String.valueOf(idGenerate.getProductionHead(((Item)ko[i]).getProductionId()));
				var subItems = idGenerate.getSubItems(((Item) ko[i]).getProductionId());
				strKO = getProductionStr(head, subItems, ((Item) ko[i]).getPointPos());

			}
			else strKO = "";

			if (i < kno.length) {
				String head = String.valueOf(idGenerate.getProductionHead(((Item)kno[i]).getProductionId()));
				var subItems = idGenerate.getSubItems(((Item) kno[i]).getProductionId());
				strKNO = getProductionStr(head, subItems, ((Item) kno[i]).getPointPos());
			}
			else strKNO = "";

			fout.insertln(strKO, strKNO);
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
