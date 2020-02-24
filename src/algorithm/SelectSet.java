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
	private Map<String, Map<String, Set<BodyItem>>> selectSet;

	private boolean isUpdated = false;

	public SelectSet(CFG cfg, FirstSet firstSet, FollowSet followSet) {
		this.cfg = cfg;
		this.firstSet = firstSet.getFirstSet();
		this.bodyItemFirstSet = firstSet.getBodyItemFirstSet();
		this.followSet = followSet.getFollowSet();
		this.selectSet = new HashMap<>();
	}

	public Map<String, Map<String, Set<BodyItem>>> getSelectSet() {
		if (isUpdated) return selectSet;

		for (String nonTerminal : cfg.getNonTerminals()) {
			traversalProductionSet(nonTerminal);
			System.out.println("\t" + nonTerminal + ":");
			Map<String, Set<BodyItem>> item = selectSet.get(nonTerminal);
			item.forEach((key, value) -> {
				System.out.print("{" + key + ", " + value + "}\t");
			});
			System.out.println();
		}

		isUpdated = true;
		return this.selectSet;
	}

	private void traversalProductionSet(String nonTerminal) {
		ProductionSet productionSet = cfg.getProductions().get(nonTerminal);
		if (productionSet == null) return ;

		productionSet.getBodies().forEach(bodyItem -> {
			Set<String> itemFirstSet = bodyItemFirstSet.get(bodyItem);

			Map<String, Set<BodyItem>> selectItem = selectSet.get(nonTerminal);
			if (selectItem == null) {
				selectItem = new HashMap<>();
			}

			if (itemFirstSet.contains("ε")) {
				Set<String> nonTerFollow = followSet.get(nonTerminal);
				for (String terminal : nonTerFollow) {
					Set<BodyItem> sbi = selectItem.get(terminal);
					if (sbi == null) sbi = new HashSet<>();

					sbi.add(bodyItem);
					selectItem.put(terminal, sbi);
				}

				if (itemFirstSet.contains("$")) {
					Set<BodyItem> sbi = selectItem.get("$");
					if (sbi == null) sbi = new HashSet<>();

					sbi.add(bodyItem);
					selectItem.put("$", sbi);
				}
			} else {
				for (String terminal : itemFirstSet) {
					Set<BodyItem> sbi = selectItem.get(terminal);
					if (sbi == null) sbi = new HashSet<>();

					sbi.add(bodyItem);
					selectItem.put(terminal, sbi);
				}
			}

			selectSet.put(nonTerminal, selectItem);
		});
	}
}
