package algorithm;

import cfg.CFG;
import cfg.production.Production;
import cfg.production.ProductionGroup;
import cfg.production.SubItem;
import cfg.production.SubItemType;

import java.util.*;

/**
 * 提取左公因子
 */
public class LeftCommonFactor {

	private CFG cfg;
	public LeftCommonFactor(CFG cfg) {
		this.cfg = cfg;
	}

	public CFG extract() {
		Map<String, ProductionGroup> appProductions = new LinkedHashMap<>();

		cfg.getProductionGroupMap().forEach((key, value) -> {
			appProductions.putAll(extractCommonFactor(key, value));
		});

		cfg.getProductionGroupMap().putAll(appProductions);
		return this.cfg;
	}

	/**
	 *
	 * @param head
	 * @param productionGroup
	 * @return 新增项
	 */
	private Map<String, ProductionGroup> extractCommonFactor(final String head, final ProductionGroup productionGroup) {
		Map<String, ProductionGroup> appProductions = new LinkedHashMap<>();

		// 1. 归类
		final LinkedList<LinkedList<Production>> lists = classify(productionGroup);

		// 2. 提取
		lists.forEach(item -> {
			// 获取左公因子，并且剔除了item中的左公因子项
			LinkedList<SubItem> commonFactor = decompositionExtract(item, productionGroup);

			// 有左公因子
			if (commonFactor != null && commonFactor.size() != 0) {
				// 获取新增项的名称
				String appendHead = cfg.getAddNonTerminalName(head);

				/* 更新head的产生式集 */
				// 因为产生式集中删除了含左公因子项，所以直接添加一个, 为公共子项 + 新增项
				commonFactor.add(new SubItem(appendHead, SubItemType.nonTerminal));

				// 新增产生式 -> 公共子项 + 新增项
				Production sourceProduction = Production.createProduction(commonFactor);
				sourceProduction.resetStr();
				productionGroup.addProduction(sourceProduction);

				/* 添加新增项 */
				ProductionGroup appendProductionGroup = new ProductionGroup();
				for (Production production : item) {
					production.resetStr();

					if (production.getSubItems().isEmpty()) {
						// 添加一个ε
						Production epsilonProduction = Production.createProduction("ε", SubItemType.terminal);
						epsilonProduction.resetStr();

						appendProductionGroup.addProduction(epsilonProduction);
					} else {
						appendProductionGroup.addProduction(production);
					}

				}
				cfg.getNonTerminals().add(appendHead);
				appProductions.put(appendHead, appendProductionGroup);
			}
		});

		return appProductions;
	}

	/**
	 * 对产生式组进行归类
	 * @param productionGroup 产生式组
	 * @return 归类的结果每个LinkedList<Production>为一类
	 */
	private LinkedList<LinkedList<Production>> classify(ProductionGroup productionGroup) {
		final LinkedList<LinkedList<Production>> lists = new LinkedList<>();

		// 1. 检查当前产生式集合是否有左公因子  O(m)
		productionGroup.getProductions().forEach(item -> {
			boolean haveSame = false;

			// 当前item是否有满足匹配的左公因子
			for (LinkedList<Production> productions : lists) {
				// 归类, 只需检查每个集合的第一项（如果和这项不匹配，则和剩下的也都不匹配）
				if (productions.getFirst().commonFactor(item)) {
					productions.add(item);
					haveSame = true;
				}
			}

			// 因为不可以在遍历的时候做修改，所以如果没有检查到相同的，则新new一个种类
			if (!haveSame) {
				LinkedList<Production> productions = new LinkedList<>();
				productions.add(item);
				lists.add(productions);
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
	private LinkedList<SubItem> decompositionExtract(LinkedList<Production> list, ProductionGroup productionGroup) {
		// 当前类别没有左公因子
		if (list.size() == 1 || list.isEmpty()) return null;

		// 左公因子
		LinkedList<SubItem> commonFactors = new LinkedList<>();

		// 如果循环后，没有匹配成功(mayExist为false)，则代表没有左公因子了
		final boolean[] mayExist = {true};

		while (mayExist[0]) {
			Set<Production> temp = new HashSet<>();

			// 先添加一个左公因子
			commonFactors.add(list.getFirst().getSubItems().getFirst());

			// 对list遍历，提取左公因子
			list.forEach(item -> {
				// Todo bug 返还完后，应当跳出循环
				if (!mayExist[0]) return ;

				// 从productionSet中移除item, 后面添加
				productionGroup.getProductions().remove(item);

				if (item.getSubItems().isEmpty()) {
					// 有可能移除完了，导致某一个为空
					/*
					 * S -> i E t S | i E t S E s 的最长公因子 i E t S
					 *
					 * S -> _ | E s
					 */

					// 移除多余项
					SubItem subItem = commonFactors.removeLast();

					temp.forEach(t -> {
						// 匹配失败，返还
						t.getSubItems().addFirst(subItem);
						t.resetStr();
					});

					mayExist[0] = false;
					return ;
				}

				if (item.getSubItems().getFirst().getValue()
						.equals(commonFactors.getLast().getValue())) {
					// 匹配成功，移除
					item.getSubItems().removeFirst();
					temp.add(item);
				} else {
					mayExist[0] = false;

					// 移除多余项
					SubItem subItem = commonFactors.removeLast();

					temp.forEach(t -> {
						// 匹配失败，返还
						t.getSubItems().addFirst(subItem);
					});

				}
			});
		}

		return commonFactors;
	}
}
