package cfg;

import cfg.production.Production;
import cfg.production.ProductionGroup;
import fout.Fout;
import fout.attr.ColumnAttr;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class CFG implements Cloneable {
	public static final char appendChar = '`';

	// Non-terminal Set
	private java.util.Set<String> nonTerminals;

	// Terminal Set
	private Set<String> terminals;

	// Start Symbol
	private String startSymbol;

	// Production Rule Mapping Table
	private Map<String, ProductionGroup> productionGroupMap;

	public CFG(CFGBuilder builder) {
		this.nonTerminals = builder.nonTerminals;
		this.terminals = builder.terminals;
		this.startSymbol = builder.startSymbol;
		this.productionGroupMap = builder.productionGroupMap;
	}

	public void setNonTerminals(Set<String> nonTerminals) {
		this.nonTerminals = nonTerminals;
	}

	public Set<String> getNonTerminals() {
		return nonTerminals;
	}

	public void setTerminals(Set<String> terminals) {
		this.terminals = terminals;
	}

	public Set<String> getTerminals() {
		return terminals;
	}

	public void setStartSymbol(String startSymbol) {
		this.startSymbol = startSymbol;
	}

	public String getStartSymbol() {
		return startSymbol;
	}

	public void setProductionGroupMap(Map<String, ProductionGroup> productionGroupMap) {
		this.productionGroupMap = productionGroupMap;
	}

	public Map<String, ProductionGroup> getProductionGroupMap() {
		return productionGroupMap;
	}

	public void printCFG() {
		printStartSymbol();
		printNonTerminal();
		printTerminal();
		printProduction();
	}

	public void printStartSymbol() {
		Fout fout = new Fout();
		fout.addColumn(new ColumnAttr("StartSymbol"));
		fout.insert(startSymbol);
		fout.fout();
	}

	public void printNonTerminal() {
		Fout fout = new Fout();
		fout.addColumn(new ColumnAttr("NonTerminal"));
		for (String s : nonTerminals) {
			fout.insertln(s);
		}
		fout.fout();
	}

	public void printTerminal() {
		Fout fout = new Fout();
		fout.addColumn(new ColumnAttr("Terminal"));
		for (String s : terminals) {
			fout.insertln(s);
		}
		fout.fout();
	}

	public void printProduction() {
		Fout fout = new Fout();
		fout.addColumn(new ColumnAttr("Production"));

		Set<Map.Entry<String, ProductionGroup>> entry = productionGroupMap.entrySet();
		entry.forEach(stringSetEntry -> {
			String keys = stringSetEntry.getKey();
			ProductionGroup values = stringSetEntry.getValue();

			StringBuilder builder = new StringBuilder();
			builder.append(keys).append(" -> ");

			values.getProductions().forEach(item -> {
				builder.append(item.getProductionStr()).append(" | ");
			});
			builder.delete(builder.length() - 3, builder.length());
			fout.insertln(builder.toString());
		});
		fout.fout();
	}

	/**
	 * 对外接口
	 * @return 返回CFG的clone
	 */
	public CFG copy() {
		try {
			return clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			System.exit(-2);
		}
		return null;
	}


	@Override
	protected CFG clone() throws CloneNotSupportedException {
		CFG cfg = (CFG) super.clone();
		copy(cfg);
		return cfg;
	}

	private void copy(CFG dest) {
		Map<String, ProductionGroup> productionGroupMap = new LinkedHashMap<>();

		Set<String> nonTerminals = new LinkedHashSet<>(this.nonTerminals);
		Set<String> terminals = new LinkedHashSet<>(this.terminals);
		this.productionGroupMap.forEach((key, value) -> {
			productionGroupMap.put(key, value.copy());
		});

		dest.setNonTerminals(nonTerminals);
		dest.setTerminals(terminals);
		dest.setProductionGroupMap(productionGroupMap);
	}

	public String getAddNonTerminalName(String nonTer) {
		// 获取新增项的名称 名字都为原来的名称 + `, 如果存在，则继续加 `
		String appendNonTer = nonTer + CFG.appendChar;
		while (nonTerminals.contains(appendNonTer)) appendNonTer += CFG.appendChar;
		return appendNonTer;
	}
}
