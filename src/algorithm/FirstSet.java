package algorithm;

import log.IOColor;
import struct.CFG;
import struct.production.BodyItem;
import struct.production.BodySubItem;
import struct.production.BodySubItemAttrType;
import struct.production.ProductionSet;

import java.util.*;

public class FirstSet {

	private CFG cfg;
	// nonTerminal -> {terminal Symbol}
	private Map<String, Set<String>> firstSet;

	// 偷工减料，为求selectSet做准备
	private Map<BodyItem, Set<String>> bodyItemFirstSet = new HashMap<>();
	private boolean isUpdated = false;

	public FirstSet(CFG cfg) {
		this.cfg = cfg;
		firstSet = new HashMap<>();
	}

	public Map<String, Set<String>> getFirstSet() {
		if (isUpdated) return firstSet;

		Map<String, Set<String>> backup = new HashMap<>();
		for (String nonTerminal : cfg.getNonTerminals()) {
			Set<String> rec = calculationFirstSet(backup, nonTerminal);
			if (rec != null) {
				firstSet.put(nonTerminal, rec);
			}
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
		ProductionSet productionSet = cfg.getProductions().get(nonTerminal);
		if (productionSet == null) return null;

		Set<String> result = new HashSet<>();
		for (BodyItem item : productionSet.getBodies()) {
			Set<String> itemFirstSet = new HashSet<>();

			String firstSubItem = item.getSubItems().getFirst().getValue();
			if (cfg.getNonTerminals().contains(firstSubItem)) {

				ListIterator<BodySubItem> iterator = item.getSubItems().listIterator(0);
				while (iterator.hasNext()) {
					BodySubItem subItem = iterator.next();

					// 如果是终结符，添加到result中，并且跳出循环
					if (subItem.getAttr() == BodySubItemAttrType.terminal) {
						result.add(subItem.getValue());
						itemFirstSet.add(subItem.getValue());
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

			bodyItemFirstSet.put(item, itemFirstSet);
		}
		return result;
	}

	/**
	 *检查nonTerminal能否经过一步或多步推出ε
	 *
	 * Todo 如果两个产生式成环，可能会栈溢出
	 * @param nonTerminal
	 * @return
	 */
	private boolean canDerivationEpsilon(String nonTerminal) {
		ProductionSet productionSet = cfg.getProductions().get(nonTerminal);
		if (productionSet == null) return false;

		boolean result = false;

		for (BodyItem item : productionSet.getBodies()) {
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

	public Map<BodyItem, Set<String>> getBodyItemFirstSet() {
		return bodyItemFirstSet;
	}

	public void printFirstSet() {
		firstSet.forEach((key, value) -> {
			System.out.println(key + " -> " + value);
		});
	}
}

