package algorithm;

import cfg.CFG;
import cfg.production.Production;
import cfg.production.ProductionGroup;
import cfg.production.SubItem;
import cfg.production.SubItemType;
import fout.Fout;
import fout.attr.ColumnAttr;

import java.util.*;

public class FirstSet {

	private CFG cfg;
	// nonTerminal -> {terminal Symbol}
	private Map<String, Set<String>> firstSet;

	// 偷工减料，为求selectSet做准备
	private Map<String, Set<String>> productionFirstSet = new LinkedHashMap<>();
	private boolean isUpdated = false;

	public FirstSet(CFG cfg) {
		this.cfg = cfg;
		firstSet = new LinkedHashMap<>();
	}

	public Map<String, Set<String>> getFirstSet() {
		if (isUpdated) return firstSet;

		Map<String, Set<String>> backup = new LinkedHashMap<>();
		for (String nonTerminal : cfg.getNonTerminals()) {
			Set<String> rec = calculationFirstSet(backup, nonTerminal);
			if (rec != null) {
				firstSet.put(nonTerminal, rec);
			}
		}

		for (var item : firstSet.entrySet()) {
			item.getValue().remove("ε");
		}

		isUpdated = true;
		return this.firstSet;
	}

	/**
	 *
	 * @param nonTerminal
	 * @return
	 */
	private Set<String> calculationFirstSet(Map<String, Set<String>> backup, String nonTerminal) {
		ProductionGroup productionGroup = cfg.getProductionGroupMap().get(nonTerminal);
		if (productionGroup == null) return null;

		Set<String> result = new LinkedHashSet<>();
		for (Production item : productionGroup.getProductions()) {
			Set<String> itemFirstSet = new HashSet<>();

			String firstSubItem = item.getSubItems().getFirst().getValue();
			if (cfg.getNonTerminals().contains(firstSubItem)) {

				ListIterator<SubItem> iterator = item.getSubItems().listIterator(0);
				while (iterator.hasNext()) {
					SubItem subItem = iterator.next();

					// 如果是终结符，添加到result中，并且跳出循环
					if (subItem.getType() == SubItemType.terminal) {
						// 终结符分两种情况，一种是有ε，一种是没有ε
						if (subItem.getValue().equals("ε")) {
							subItem = iterator.next();
						} else {
							result.add(subItem.getValue());
							itemFirstSet.add(subItem.getValue());
						}
						break;
					}

					Set<String> rec = backup.get(subItem.getValue());

					// backup中没有, 计算一遍
					if (rec == null) {
						rec = calculationFirstSet(backup, subItem.getValue());
					}

					assert rec != null;

					backup.put(subItem.getValue(), rec);
					result.addAll(rec);
					itemFirstSet.addAll(rec);

					if (!rec.contains("ε")) {
						// 如果当前子项不能推导出epsilon, 则当前项已经结束
						result.addAll(rec);
						itemFirstSet.addAll(rec);
						break;
					}

				}
			} else {
				result.add(firstSubItem);
				itemFirstSet.add(firstSubItem);
			}

			productionFirstSet.put(item.getProductionStr(), itemFirstSet);
		}
		return result;
	}

	/**
	 * 检查nonTerminal能否经过一步或多步推出ε
	 *
	 * Todo 如果两个产生式成环，可能会栈溢出
	 * @param nonTerminal
	 * @return
	 */
	private boolean canDerivationEpsilon(String nonTerminal) {
		ProductionGroup productionGroup = cfg.getProductionGroupMap().get(nonTerminal);
		if (productionGroup == null) return false;

		boolean result = false;

		for (Production item : productionGroup.getProductions()) {
			String firstSubItem = item.getSubItems().getFirst().getValue();
			if (cfg.getNonTerminals().contains(firstSubItem)) {
				// 是一个非终结符, 则递归检查该终结符能否推导出
				boolean rec = canDerivationEpsilon(firstSubItem);
				if (rec) result = true;
			} else if (firstSubItem.equals("ε")) {
				// 如果是一个ε
				result = true;
			}

			// 只要有一次能推导出，则返回true
			if (result) return true;
		}

		return false;
	}

	public void setUpdated(boolean updated) {
		isUpdated = updated;
	}

	public Map<String, Set<String>> getProductionFirstSet() {
		return productionFirstSet;
	}

	public void printFirstSet() {
		Fout fout = new Fout(ColumnAttr.qCreate("NonTerminal", "FirstSet"));
		firstSet.forEach((key, value) -> fout.insertln(key, value));
		fout.fout();
	}
}

