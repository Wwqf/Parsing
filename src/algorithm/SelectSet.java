package algorithm;


import cfg.CFG;
import cfg.production.Production;
import cfg.production.ProductionGroup;
import fout.Fout;
import fout.attr.ColumnAttr;

import java.util.*;

public class SelectSet {

	private CFG cfg;
	private Map<String, Set<String>> firstSet;
	private Map<String, Set<String>> productionFirstSet;
	private Map<String, Set<String>> followSet;
	// 因为有的文法存在二义性（但是包含二义性的文法不是LL文法），所以用Set存储，而不是单个BodyItem
//	private Map<String, Map<String, Set<BodyItem>>> selectSet;

	private Map<String, Map<String, Production>> selectSet;

	private boolean isUpdated = false;

	public SelectSet(CFG cfg, FirstSet firstSet, FollowSet followSet) {
		this.cfg = cfg;
		this.firstSet = firstSet.getFirstSet();
		this.productionFirstSet = firstSet.getProductionFirstSet();
		this.followSet = followSet.getFollowSet();
		this.selectSet = new LinkedHashMap<>();
	}

	public Map<String, Map<String, Production>> getSelectSet() {
		if (isUpdated) return selectSet;

		for (String nonTerminal : cfg.getNonTerminals()) {
			traversalProductionSet(nonTerminal);
		}

		isUpdated = true;
		return this.selectSet;
	}

	private void traversalProductionSet(String nonTerminal) {
		ProductionGroup productionGroup = cfg.getProductionGroupMap().get(nonTerminal);
		if (productionGroup == null) return;

		for (Production production : productionGroup.getProductions()) {
			Set<String> itemFirstSet = productionFirstSet.get(production.getProductionStr());

			assert itemFirstSet != null;

			Map<String, Production> selectItem = selectSet.get(nonTerminal);
			if (selectItem == null) {
				selectItem = new LinkedHashMap<>();
			}

			for (String terminal : itemFirstSet) {
				if (terminal.equals("ε")) continue;
				selectItem.put(terminal, production);
			}

			if (itemFirstSet.contains("ε")) {
				Set<String> nonTerFollow = followSet.get(nonTerminal);
				for (String terminal : nonTerFollow) {
					if (terminal.equals("ε")) continue;
					selectItem.put(terminal, production);
				}

				if (itemFirstSet.contains("$")) {
					selectItem.put("$", production);
				}
			}
			selectSet.put(nonTerminal, selectItem);
		}
	}

	public void printSelectSet() {
		Fout fout = new Fout(ColumnAttr.qCreate("NonTerminal", "SelectSet"));

		Object[] terminals = cfg.getTerminals().toArray();
		for (int i = 0; i < terminals.length; i++) {
			if (String.valueOf(terminals[i]).equals("ε")) {
				terminals[i] = "$";
			}
		}
		fout.addSubColumn("SelectSet", ColumnAttr.qCreate(terminals));

		var entry = selectSet.entrySet();
		for (var item : entry) {
			fout.insert(item.getKey());

			var values = item.getValue();
			for (Object str : terminals) {
				Production p = values.get(String.valueOf(str));
				if (p != null) fout.insert(p.getProductionStr());
				else fout.insert("");
			}
		}

		fout.fout();
	}
}
