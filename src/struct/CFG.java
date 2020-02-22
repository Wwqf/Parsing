package struct;

import struct.production.BodyItem;
import struct.production.BodySubItem;
import struct.production.BodySubItemAttrType;
import struct.production.ProductionSet;

import java.util.*;

/**
 * Context-free Grammar
 */
public class CFG {

	private static final char appendChar = '`';

	// Non-terminal Set
	public Set<String> nonTerminals;

	// Terminal Set
	public Set<String> terminals;

	// Start Symbol
	public String startSymbol;

	// Production Rule Mapping Table
	// 每个产生式头部对应一个产生式集合, 即产生式体可能存在多个
	public Map<String, ProductionSet> productions;

	public CFG(CFGBuilder builder) {
		this.nonTerminals = builder.nonTerminals;
		this.terminals = builder.terminals;
		this.startSymbol = builder.startSymbol;
		this.productions = builder.productions;

	}

	/**
	 * 提取左公因子
	 */
	public void extractLeftCommonFactor() {

	}

	/**
	 * 消除左递归
	 */
	public void eliminateLeftRecursion() {
		Object[] list = nonTerminals.toArray();
		for (int i = 0; i < list.length; i++) {
			for (int j = 0; j < i; j++) {

			}
			eliminateImmediateLeftRecursion(String.valueOf(list[i]));
			System.out.println();
		}
	}

	/**
	 * 消除立即左递归
	 *  S -> S a | b | c d | S S c
	 *
	 *  1. 先分组
	 *      S -> S a | S S c | b | c d
	 *  2. 替换S
	 *      S -> b S` | c d S`
	 *  3. 新增S`
	 *      S` -> a S` | S c S` | ε
	 *
	 *  4. 即，最后的产生式为：
	 *      S -> b S` | c d S`
	 *      S` -> a S` | S c S` | ε
	 *
	 * Todo bug - 不能消除隐式ε(替换后存在的ε)
	 */
	private void eliminateImmediateLeftRecursion(String nonTer) {
		ProductionSet productionSet = productions.get(nonTer);

		// 分类
		List<BodyItem> startWithNonTerminal = new ArrayList<>();
		List<BodyItem> startWithOther = new ArrayList<>();

		productionSet.getBodies().forEach(item -> {
			if (item.getSubItems().getFirst().getValue().equals(nonTer)) {
				// 以该非终结符开头，认定为立即左递归
				startWithNonTerminal.add(item);
			} else {
				startWithOther.add(item);
			}
		});

		// 没有立即左递归
		if (startWithNonTerminal.isEmpty()) return ;

		// 名字都为原来的名称 + `, 如果存在，则继续加 `
		String[] appendNonTerminal = {nonTer + appendChar};
		while (nonTerminals.contains(appendNonTerminal[0])) appendNonTerminal[0] += appendChar;

		/*
		 * 新增 nonTer`
		 */
		Set<BodyItem> newBodyItemSets = new HashSet<>();
		startWithNonTerminal.forEach(item -> {
			BodyItem bodyItem = new BodyItem();
			// 移除第一项（即该左递归的非终结符）
			item.getSubItems().removeFirst();
			// 合并之后的每项 + 新增的appendNonTerminal项
			bodyItem.getSubItems().addAll(item.getSubItems());
			bodyItem.getSubItems().add(new BodySubItem(appendNonTerminal[0], BodySubItemAttrType.nonTerminal));
			// 重置bodyItem字符串对应的字符串（因为之前是以subItems的方式追加）
			bodyItem.resetBodyStr();
			// 添加到新的集合中
			newBodyItemSets.add(bodyItem);
		});

		// 添加一个ε项
		BodyItem bodyItem = new BodyItem();
		bodyItem.getSubItems().add(new BodySubItem("ε", BodySubItemAttrType.terminal));
		bodyItem.resetBodyStr();
		newBodyItemSets.add(bodyItem);

		// 更新nonTerminals
		nonTerminals.add(appendNonTerminal[0]);

		// 更新productions
		ProductionSet newProductionSet = new ProductionSet();
		newProductionSet.setBodies(newBodyItemSets);
		newProductionSet.hasEpsilon = true;
		productions.put(appendNonTerminal[0], newProductionSet);


		/*
		 * 替换
		 */
		Set<BodyItem> newBodySets = new HashSet<>();
		startWithOther.forEach(item -> {
			BodyItem _bodyItem = new BodyItem();
			if (_bodyItem.getSubItems().size() == 1 &&
					_bodyItem.getSubItems().getFirst().getValue().equals("ε")) {
				// 如果当前bodyItem只能推出 ε,则只追加一个apNonTerminal
				// 否则，还需追加item的所有项
			} else _bodyItem.getSubItems().addAll(item.getSubItems());

			_bodyItem.getSubItems().add(new BodySubItem(appendNonTerminal[0], BodySubItemAttrType.nonTerminal));
			_bodyItem.resetBodyStr();
			newBodySets.add(_bodyItem);
		});

		// 更新productions
		productionSet.hasEpsilon = false;
		productionSet.setBodies(newBodySets);
		productions.put(nonTer, productionSet);
	}
}
