package algorithm;

import struct.CFG;
import struct.production.BodyItem;
import struct.production.ProductionSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SelectSet {

	private CFG cfg;
	private Map<String, Set<String>> firstSet;
	private Map<BodyItem, Set<String>> bodyItemFirstSet;
	private Map<String, Set<String>> followSet;
	// 因为有的文法存在二义性（但是包含二义性的文法不是LL文法），所以用Set存储，而不是单个BodyItem
//	private Map<String, Map<String, Set<BodyItem>>> selectSet;

	private Map<String, Map<String, BodyItem>> selectSet;

	private boolean isUpdated = false;

	public SelectSet(CFG cfg, FirstSet firstSet, FollowSet followSet) {
		this.cfg = cfg;
		this.firstSet = firstSet.getFirstSet();
		this.bodyItemFirstSet = firstSet.getBodyItemFirstSet();
		this.followSet = followSet.getFollowSet();
		this.selectSet = new HashMap<>();
	}

	public Map<String, Map<String, BodyItem>> getSelectSet() {
		if (isUpdated) return selectSet;

		for (String nonTerminal : cfg.getNonTerminals()) {
			traversalProductionSet(nonTerminal);
		}

		isUpdated = true;
		return this.selectSet;
	}

	private void traversalProductionSet(String nonTerminal) {
		ProductionSet productionSet = cfg.getProductions().get(nonTerminal);
		if (productionSet == null) return ;

		productionSet.getBodies().forEach(bodyItem -> {
			Set<String> itemFirstSet = bodyItemFirstSet.get(bodyItem);

			Map<String, BodyItem> selectItem = selectSet.get(nonTerminal);
			if (selectItem == null) {
				selectItem = new HashMap<>();
			}

			for (String terminal : itemFirstSet) {
				if (terminal.equals("ε")) continue;
				selectItem.put(terminal, bodyItem);
			}

			if (itemFirstSet.contains("ε")) {
				Set<String> nonTerFollow = followSet.get(nonTerminal);
				for (String terminal : nonTerFollow) {
					if (terminal.equals("ε")) continue;
					selectItem.put(terminal, bodyItem);
				}

				if (itemFirstSet.contains("$")) {
					selectItem.put("$", bodyItem);
				}
			} else {

			}

			selectSet.put(nonTerminal, selectItem);
		});
	}

	public void printSelectSet() {
		selectSet.forEach((key, value) -> {
			System.out.println("\t" + key + ":");

			// Map<String, BodyItem>
			value.forEach((ter, bodyItems) -> {
				System.out.print("{" + ter + ", " + key + " -> " + bodyItems.getBodyStr() + "}\t");
			});
			System.out.println();
		});


	}
}
