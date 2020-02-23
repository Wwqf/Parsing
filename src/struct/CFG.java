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
		Map<String, ProductionSet> appProductions = new LinkedHashMap<>();

		productions.forEach((key, value) -> {
			appProductions.putAll(extractOneProductionSetCommonFactor(key, value));
		});

		productions.putAll(appProductions);
	}

	/**
	 *
	 * @param head
	 * @param productionSet
	 * @return 新增项
	 */
	private Map<String, ProductionSet> extractOneProductionSetCommonFactor(final String head, final ProductionSet productionSet) {
		Map<String, ProductionSet> appProductions = new LinkedHashMap<>();

		// 1. 归类
		final LinkedList<LinkedList<BodyItem>> lists = classify(productionSet);
		// 2. 提取
		lists.forEach(item -> {
			// 获取左公因子，并且剔除了item中的左公因子项
			LinkedList<BodySubItem> commonFactor = extract(item, productionSet);

			// 有左公因子
			if (commonFactor != null && commonFactor.size() != 0) {
				// 获取新增项的名称 名字都为原来的名称 + `, 如果存在，则继续加 `
				String[] appendHead = {head + appendChar};
				while (nonTerminals.contains(appendHead[0])) appendHead[0] += appendChar;

				/* 更新head的产生式集 */
				// 因为产生式集中删除了含左公因子项，所以直接添加一个, 为公共子项 + 新增项
				commonFactor.add(new BodySubItem(appendHead[0], BodySubItemAttrType.nonTerminal));
				BodyItem sourceBodyItem = new BodyItem();
				sourceBodyItem.setSubItems(commonFactor);
				sourceBodyItem.resetBodyStr();
				productionSet.addBody(sourceBodyItem);

				/* 添加新增项 */
				ProductionSet appendProductionSet = new ProductionSet();
				for (BodyItem bodyItem : item) {
					bodyItem.resetBodyStr();

					if (bodyItem.getSubItems().isEmpty()) {
						// 添加一个ε
						BodyItem epsilonItem = new BodyItem();
						epsilonItem.getSubItems().add(new BodySubItem("ε", BodySubItemAttrType.terminal));
						epsilonItem.resetBodyStr();
						appendProductionSet.addBody(epsilonItem);
					} else {
						appendProductionSet.addBody(bodyItem);
					}

				}
				nonTerminals.add(appendHead[0]);
				appProductions.put(appendHead[0], appendProductionSet);
			}
		});

		return appProductions;
	}

	/**
	 * 对productionSet进行归类
	 * @param productionSet
	 * @return
	 */
	private LinkedList<LinkedList<BodyItem>> classify(ProductionSet productionSet) {
		final LinkedList<LinkedList<BodyItem>> lists = new LinkedList<>();

		// 1. 检查当前产生式集合是否有左公因子  O(m)
		productionSet.getBodies().forEach(item -> {
			final boolean[] haveSame = {false};

			// 当前item是否有满足匹配的左公因子
			lists.forEach(list -> {
				// 归类, 只需检查每个集合的第一项（如果和这项不匹配，则和剩下的也都不匹配）
				if (list.getFirst().checkCommonFactor(item)) {
					list.add(item);
					haveSame[0] = true;
				}
			});

			// 因为不可以在遍历的时候做修改，所以如果没有检查到相同的，则新new一个
			if (!haveSame[0]) {
				LinkedList<BodyItem> bodyItems = new LinkedList<>();
				bodyItems.add(item);
				lists.add(bodyItems);
			}
		});

		return lists;
	}

	/**
	 * 对当前类别 list 进行分解提取操作
	 *  即：
	 *      E -> E + T | E - T | E + ( T )
	 *
	 *  1. 提取第一项的第一个子项 E, 当前文法为：
	 *      E -> + T | E - T | E + ( T )
	 *  2. 对剩余中的每一项都进行匹配提取，如果匹配成功则移除子项
	 *      (1). E -> + T | E - T | E + ( T )
	 *      (2). E -> + T | - T | E + ( T )
	 *      (3). E -> + T | - T | + ( T )
	 *  3. 然后再提取第一项的第一个子项 +, 当前文法为：
	 *      E -> T | - T | + ( T )
	 *  4. 对剩余中的每一项都进行匹配提取，如果匹配成功则移除子项, 如果匹配失败则返还之前移除的子项
	 *      (1). E -> T | - T | + ( T )
	 *      (2). E -> T | T | + ( T )       {此时移除的是'-'，匹配失败，返还之前的'+'}
	 *      (3). E -> + T | - T | + ( T )   {所以此时，三项最长的左公因子是 E}
	 *
	 *  Todo question_1.
	 *      在上面进行到第四步时，文法的顺序稍变一下：
	 *          (1). E -> T | + ( T ) | - T
	 *      以这个顺序匹配还是正常的。
	 *
	 * Todo question_2.
	 *      以上面方法匹配时，多个左公因子匹配是否正确呢？
	 *      E -> E + T | E + S | E + ( T )
	 *      1. 提取E成功, 文法为： E -> + T | + S | + ( T )
	 *      2. 提取+成功，文法为： E -> T | S | ( T )
	 *      3. 提取T失败，文法不变。
	 *      4. 所以最后的左公因子是E +
	 *      算法正确.
	 *
	 * Todo question_3.
	 *      还有个算法，以第一个为模板，后面每个都匹配第一个, 以current为标准，最多匹配current个子项
	 *      例如文法： E -> E + T | E + S | E + ( T )
	 *      E + T 为模板   current = 3
	 *      (1). E + S 匹配两个子项   current = 2
	 *      (2). E + ( T ) 匹配两个子项 current = 2
	 *      例如文法： E -> E + T | E - T | E + ( T )
	 *      E + T 为模板   current = 3
	 *      (1). E - T 匹配一个子项, current = 1
	 *      (2). 则 E + ( T ) 匹配一个子项, current = 1
	 *      然后遍历一遍list，提取current个子项，这些子项就是左公因子
	 * @param list 归类后的其中一类别
	 */
	private LinkedList<BodySubItem> extract(LinkedList<BodyItem> list, ProductionSet productionSet) {
		// 当前类别没有左公因子
		if (list.size() == 1 || list.isEmpty()) return null;

		// 左公因子
		LinkedList<BodySubItem> commonFactors = new LinkedList<>();

		// 如果循环后，没有匹配成功(mayExist为false)，则代表没有左公因子了
		final boolean[] mayExist = {true};

		while (mayExist[0]) {
			Set<BodyItem> temp = new HashSet<>();

			// 先添加一个左公因子
			commonFactors.add(list.getFirst().getSubItems().getFirst());

			// 对list遍历，提取左公因子
			list.forEach(item -> {
				// Todo bug 返还完后，应当跳出循环
				if (!mayExist[0]) return ;

				// 从productionSet中移除item, 后面添加
				productionSet.getBodies().remove(item);

				if (item.getSubItems().isEmpty()) {
					// 有可能移除完了，导致某一个为空
					/*
					 * S -> i E t S | i E t S E s 的最长公因子 i E t S
					 *
					 * S -> _ | E s
					 */

					// 移除多余项
					BodySubItem subItem = commonFactors.removeLast();

					temp.forEach(t -> {
						// 匹配失败，返还
						t.getSubItems().addFirst(subItem);
						t.resetBodyStr();
					});

					mayExist[0] = false;
				}

				if (item.getSubItems().getFirst().getValue()
						.equals(commonFactors.getLast().getValue())) {
					// 匹配成功，移除
					item.getSubItems().removeFirst();
					temp.add(item);
				} else {
					mayExist[0] = false;

					// 移除多余项
					BodySubItem subItem = commonFactors.removeLast();

					temp.forEach(t -> {
						// 匹配失败，返还
						t.getSubItems().addFirst(subItem);
					});

				}
			});
		}

		return commonFactors;
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

		final boolean[] isHappenLeftRecursion = {false};

		// 检查如果被替换，是否会发生左递归
		replacedPro.getBodies().forEach(item -> {
			if (item.getSubItems().getFirst().getValue().equals(currentNonTer)) {
				isHappenLeftRecursion[0] = true;
			}
		});

		// 不会发生左递归，直接返回，不需要替换
		if (!isHappenLeftRecursion[0]) return ;

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
			if (item.getSubItems().size() == 1 &&
					item.getSubItems().getFirst().getValue().equals("ε")) {
				// 如果当前item只能推出 ε,则只追加一个apNonTerminal
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
