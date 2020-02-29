package algorithm;

import cfg.CFG;
import cfg.production.Production;
import cfg.production.ProductionGroup;
import cfg.production.SubItem;
import cfg.production.SubItemType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class LeftRecursion {

	private CFG cfg;
	public LeftRecursion(CFG cfg) {
		this.cfg = cfg;
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
	public CFG eliminate() {
		Object[] list = cfg.getNonTerminals().toArray();
		for (int i = 0; i < list.length; i++) {
			for (int j = 0; j < i; j++) {
				replaceProduction(String.valueOf(list[i]), String.valueOf(list[j]));
			}
			eliminateImmediateLeftRecursion(String.valueOf(list[i]));
		}
		return this.cfg;
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
		ProductionGroup currentPro = cfg.getProductionGroupMap().get(currentNonTer);
		ProductionGroup replacedPro = cfg.getProductionGroupMap().get(replacedNonTer);

		// 不能在遍历时删除对象，所以创建一个需要删除的set
		Set<Production> removeProductions = new LinkedHashSet<>();

		final boolean[] isHappenLeftRecursion = {false};

		// 检查如果被替换，是否会发生左递归
		replacedPro.getProductions().forEach(item -> {
			if (item.getSubItems().getFirst().getValue().equals(currentNonTer)) {
				isHappenLeftRecursion[0] = true;
			}
		});

		// 不会发生左递归，直接返回，不需要替换
		if (!isHappenLeftRecursion[0]) return ;

		currentPro.getProductions().forEach(item -> {
			// 当前产生式的首项可以被替换
			if (item.getSubItems().getFirst().getValue().equals(replacedNonTer)) {
				// 1. 从列表中删除此产生式
				removeProductions.add(item);
			}
		});

		// 2. 然后替换产生式
		removeProductions.forEach(item -> {
			// 先移除
			currentPro.getProductions().remove(item);
			// 删除首项
			item.getSubItems().removeFirst();
			// 替换产生式
			replacedPro.getProductions().forEach(rpItem -> {
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

				Production production = Production.createProduction(rpItem.getSubItems());
				production.getSubItems().addAll(item.getSubItems());
				production.resetStr();
				currentPro.getProductions().add(production);
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
		ProductionGroup ProductionGroup = cfg.getProductionGroupMap().get(nonTer);

		// 分类
		List<Production> startWithNonTerminal = new ArrayList<>();
		List<Production> startWithOther = new ArrayList<>();

		ProductionGroup.getProductions().forEach(item -> {
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
		String appendNonTerminal = cfg.getAddNonTerminalName(nonTer);
		/*
		 * 新增 nonTer`
		 */
		Set<Production> newProductionSets = new LinkedHashSet<>();
		startWithNonTerminal.forEach(item -> {
			// 移除第一项（即该左递归的非终结符）
			item.getSubItems().removeFirst();
			// 合并之后的每项 + 新增的appendNonTerminal项
			Production production = Production.createProduction(item.getSubItems());
			production.getSubItems().add(new SubItem(appendNonTerminal, SubItemType.nonTerminal));
			// 重置Production字符串对应的字符串（因为之前是以subItems的方式追加）
			production.resetStr();
			// 添加到新的集合中
			newProductionSets.add(production);
		});

		// 添加一个ε项
		Production production = Production.createProduction("ε", SubItemType.terminal);
		production.resetStr();
		newProductionSets.add(production);

		// 更新nonTerminals
		cfg.getNonTerminals().add(appendNonTerminal);

		// 更新productions
		ProductionGroup newProductionGroup = new ProductionGroup();
		newProductionGroup.setProductions(newProductionSets);
		newProductionGroup.setHasEpsilon(true);
		cfg.getProductionGroupMap().put(appendNonTerminal, newProductionGroup);


		/*
		 * 替换
		 */
		Set<Production> newBodySets = new LinkedHashSet<>();
		startWithOther.forEach(item -> {
			Production _production = null;
			if (!(item.getSubItems().size() == 1 &&
					item.getSubItems().getFirst().getValue().equals("ε"))) {
				// 如果当前item只能推出 ε,则只追加一个apNonTerminal
				// 否则，还需追加item的所有项
				_production = Production.createProduction(item.getSubItems());
			}

			if (_production == null) {
				_production = Production.createProduction(appendNonTerminal, SubItemType.nonTerminal);
			} else {
				_production.getSubItems().add(new SubItem(appendNonTerminal, SubItemType.nonTerminal));
			}
			_production.resetStr();
			newBodySets.add(_production);
		});

		// 更新productions
		ProductionGroup.setHasEpsilon(false);
		ProductionGroup.setProductions(newBodySets);
		cfg.getProductionGroupMap().put(nonTer, ProductionGroup);
	}

}
