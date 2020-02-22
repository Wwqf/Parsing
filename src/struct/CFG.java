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
	 *      S -> A a | b
	 *      A -> A c | S d | c
	 *
	 * 1. 替换产生式体中的下界
	 *      A -> A c | A a d | b d | c
	 * 2. 消除直接左递归
	 *     A -> b d A` | c A`
	 *     A` -> c A` | a d A` | ε
	 *
	 * 变化产生式的顺序，导致消除左递归的结果不同，是否正常？
	 */
	public void eliminateLeftRecursion() {
		Object[] list = nonTerminals.toArray();
		for (int i = 0; i < list.length; i++) {
			for (int j = 0; j < i; j++) {
				replaceProduction(String.valueOf(list[i]), String.valueOf(list[j]));
			}
			eliminateImmediateLeftRecursion(String.valueOf(list[i]));
		}

		System.out.println(this.toString());
	}

	/**
	 * 将Ai -> Ajγ 的产生式替换为产生式组 Ai -> δ1γ | δ2γ ...
	 * 其中 Aj -> δ1 | δ2 ...
	 *
	 * 有两种情况，一种是替换后没有立即左递归，一种是替换后有立即左递归。
	 * 如果没有，则不更新productions
	 *
	 * 考虑一件事：需不需要在源Set中操作，即如果在源Set中发生改变，会不会影响之后的结果？
	 * @param currentNonTer
	 * @param replacedNonTer
	 */
	private void replaceProduction(String currentNonTer, String replacedNonTer) {
		ProductionSet currentPro = productions.get(currentNonTer);
		ProductionSet replacedPro = productions.get(replacedNonTer);

		// 不能在遍历时删除对象，所以创建一个需要删除的set
		Set<BodyItem> removeBodyItems = new LinkedHashSet<>();

		currentPro.getBodies().forEach(item -> {
			// 当前产生式的首项可以被替换
			if (item.getSubItems().getFirst().getValue().equals(replacedNonTer)) {
				// 1. 从列表中删除此产生式
				removeBodyItems.add(item);
			}
		});

		// 2. 然后替换产生式
		removeBodyItems.forEach(item -> {
			// 先移除
			currentPro.getBodies().remove(item);
			// 删除首项
			item.getSubItems().removeFirst();
			// 替换产生式
			replacedPro.getBodies().forEach(rpItem -> {
				/*
				 * Todo e.g.
				 *  currentNonTer >> A
				 *  replacedNonTer >> S
				 *
				 * 	 S -> A a | b
				 * 	 A -> A c | S d | c
				 *
				 *  1. 先移除A中的S d
				 *      A -> A c | c
				 *  2. 将S d中的S移除（需要替换），只剩下d (其实就是步骤3)
				 *
				 *  3. 替换S d中的S (对于S中每一项都要替换)
				 *      A a d | b d
				 *  4. 然后添加到A的产生式集中
				 *      A -> A c | A a d | b d | c
				 *  5. 然后消除立即左递归
				 */

				BodyItem bodyItem = new BodyItem();
				bodyItem.getSubItems().addAll(rpItem.getSubItems());
				bodyItem.getSubItems().addAll(item.getSubItems());
				bodyItem.resetBodyStr();
				currentPro.getBodies().add(bodyItem);
			});
		});
	}

	/**
	 * 消除立即左递归
	 *      S -> S a | b | c d | S S c
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
		Set<BodyItem> newBodyItemSets = new LinkedHashSet<>();
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
		Set<BodyItem> newBodySets = new LinkedHashSet<>();
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

	public void printCFG() {

	}

	@Override
	public String toString() {
		return getNonTerminalsString() + '\n' +
				getTerminalsString() + '\n' +
				getStartSymbolString() + '\n' +
				getProductionsString();
	}


	public String getNonTerminalsString() {
		StringBuilder builder = new StringBuilder();
		builder.append("nonTerminals --------------------------------------\n");
		final int[] len = {0};
		nonTerminals.forEach(item -> {
			builder.append('[').append(item).append(']').append(' ');
			if (++len[0] % 5 == 0) builder.append('\n');
		});
		builder.append('\n');
		return builder.toString();
	}

	public String getTerminalsString() {
		StringBuilder builder = new StringBuilder();
		builder.append("terminals --------------------------------------\n");
		terminals.forEach(item -> {
			builder.append(item).append(' ');
		});

		builder.append('\n');
		return builder.toString();
	}

	public String getStartSymbolString() {
		return "Start Symbol --------------------------------------\n" +
				startSymbol + '\n';
	}

	public String getProductionsString() {
		StringBuilder builder = new StringBuilder();
		builder.append("productions --------------------------------------\n");
		Set<Map.Entry<String, ProductionSet>> entry = productions.entrySet();

		entry.forEach(stringSetEntry -> {
			String keys = stringSetEntry.getKey();
			ProductionSet values = stringSetEntry.getValue();

			builder.append(keys).append(" -> ");

			values.getBodies().forEach(item -> {
				builder.append(item.getBodyStr()).append(" | ");
			});
			builder.delete(builder.length() - 3, builder.length());
			builder.append('\n');
		});
		return builder.toString();
	}
}
