package lalr;

import algorithm.FirstSet;
import fout.Fout;
import fout.attr.ColumnAttr;
import logger.Log;
import lr.Item;
import lr.ItemSet;
import slr.ProductionIdGenerate;

import java.lang.reflect.Array;
import java.util.*;

public class ItemCollection {

	/**
	 * 传入LR（1）文法，构造LALR
	 */
	private Set<lr.ItemSet> lalrItemSets;
	private FirstSet firstSet;
	private ProductionIdGenerate idGenerate;
	public ItemCollection(lr.ItemCollection itemCollection) {
		lalrItemSets = new HashSet<>(itemCollection.getLrItemSets());
		this.firstSet = itemCollection.getFirstSet();
		this.idGenerate = itemCollection.getIdGenerate();
	}

	/**
	 * Todo 在整个构造过程中，遇到几个问题：
	 *  1. 多项集合并的Goto表指向的对象，有可能只存在一个，即:
	 *     * K -α-> M
	 * 	   * L -α-> M
	 * 	 这时，因为newItemSet是该多项集的合并项，只需要简单更改即可:
	 * 	   假设K、L合并为X项，则有：
	 * 	   * X -α-> M
	 *  2. 多项集有指向自身的输入量，在这里，也只是简单的重定向
	 *     * K -α-> K
	 *     * L -α-> L
	 *     假设K、L合并为X项，则有：
	 *     * X -α-> X
	 *  3. Goto表指向的将要被合并的多项集有可能被合并过了（exists存在的意义），
	 *     需要检查有哪些项被合并过，如果有则重定向
	 *     假设K、L合并为X项, O、P合并为Y项
	 *     * K -α-> O (O已被合并为Y项)
	 *     * L -α-> P (P已被合并为Y项)
	 *     存在：
	 *     * X -α-> Y (因为X是K、L的合并项)
	 */
	public void construct() {

		Set<ItemSet> rec;
		while ((rec = findSameCore(lalrItemSets)) != null) {
			// 这几项合并，Goto表转移项集也要合并

			// 找到，合并
			Map<ItemSet, String> redirectSet = findGotoSourceItemSet(lalrItemSets, rec);

			// 合并
			ItemSet newItemSet = mergeItemSet(rec);

			// 合并goto表, 返回受到影响的集合
			Set<ItemSet> affectedSet = mergeGotoTable(new HashMap<>(), newItemSet, rec);

			// 重定向 I -α-> K
			for (var item : redirectSet.entrySet()) {
				item.getKey().getGotoTables().put(item.getValue(), newItemSet);
			}

			// 从backup中删除匹配
			lalrItemSets.removeAll(rec);
			lalrItemSets.removeAll(affectedSet);
			lalrItemSets.add(newItemSet);
		}
		System.out.println();
	}


	private ItemSet mergeItemSet(Set<ItemSet> rec) {
		ItemSet newItemSet = new ItemSet(idGenerate, firstSet);
		for (ItemSet itemSet : rec) {
			// 合并第二分量
			for (Item lrItem : itemSet.getLrItems()) {
				newItemSet.addItemCoreLookhead(lrItem);
			}
		}
		return newItemSet;
	}

	/**
	 * 合并goto表, 返回受到影响的集合
	 * K -α-> L
	 * K -β-> M
	 * @param itemSets
	 * @return
	 */
	private Set<ItemSet> mergeGotoTable(Map<Integer, ItemSet> exists, ItemSet newItemSet, Set<ItemSet> itemSets) {
		Set<ItemSet> affectedSet = new HashSet<>();

		// 有哪些项集可以被合并
		Map<String, Set<ItemSet>> merge = new HashMap<>();
		// 有哪些项集指向自身
		Set<String> pointItself = new HashSet<>();

		/*
		 * 划分，itemSets是一组具有相同核心的项集族，则它们的Goto表一致，
		 * 但是到达的项集不同，在这里提取，将具有相同符号的转移项集放在一起
		 */
		for (ItemSet item : itemSets) {
			var igt = item.getGotoTables().entrySet();

			// 遍历项集Goto表，划分
			for (Map.Entry<String, ItemSet> entry : igt) {
				if (entry.getValue() == item) {
					// 指向自身
					pointItself.add(entry.getKey());
					continue;
				}

				Set<ItemSet> mergeSet = merge.get(entry.getKey());
				if (mergeSet == null) {
					mergeSet = new HashSet<>();
				}
				mergeSet.add(entry.getValue());
				merge.put(entry.getKey(), mergeSet);

				// entry.value是被受影响的项集（因为需要将其合并）
				// 所以加入到返回结果affectedSet中
				affectedSet.add(entry.getValue());
			}
		}

		/*
		 * 解决指向自身的转换
		 */
		for (String pti : pointItself) {
			newItemSet.getGotoTables().put(pti, newItemSet);
		}

		/**
		 * 可能有多个项集指向某一个项集，判断merge某一项的长度是否为1，
		 * 如果为1，则指向相同的项集，该项集不被合并
		 */

		Set<String> removeKey = new HashSet<>();
		for (var item : merge.entrySet()) {
			if (item.getValue().size() == 1) {
				// 增加到Goto表
				for (var v : item.getValue()) {
					newItemSet.getGotoTables().put(item.getKey(), v);
					removeKey.add(item.getKey());
					affectedSet.remove(v);
				}
			}
		}
		for (String s : removeKey) {
			merge.remove(s);
		}

		// 测试输出
//		Fout fout = new Fout(ColumnAttr.qCreate("SourceSet", "ArriveSet"));
//		fout.addSubColumn("ArriveSet", ColumnAttr.qCreate("Sym", "Set"));
//		StringBuilder builder = new StringBuilder();
//		for (var item : itemSets) {
//			builder.append(item.getId()).append(" ");
//		}
//		fout.insert(builder.toString());
//
//		for (var item : merge.entrySet()) {
//			builder = new StringBuilder();
//			for (var s : item.getValue()) {
//				builder.append(s.getId()).append(" ");
//			}
//			fout.insertlnSubColumn("ArriveSet", item.getKey(), builder.toString());
//		}
//		fout.fout();

		// 可能 将要合并的选项已经被合并过了，所以之间重定向即可
		removeKey = new HashSet<>();
		for (var item : merge.entrySet()) {
			// 检查是否存在
			for (ItemSet itemSet : item.getValue()) {
				// 注意，能被合并的多项集，如果检查到存在（即存在一个），则直接更改，然后break
				if (exists.containsKey(itemSet.getId())) {
					newItemSet.getGotoTables().put(item.getKey(), exists.get(itemSet.getId()));
					// 重定向后，还需要删除key-value
					removeKey.add(item.getKey());
					break;
				}
			}
		}
		for (String s : removeKey) {
			merge.remove(s);
		}

		/*
		 * 对每个子项集族进行合并, 这些项集族合并后生成的项集，它的Goto表依然可以进行合并，所以递归执行
		 */
		Map<ItemSet, Set<ItemSet>> sd = new HashMap<>();
		for (Map.Entry<String, Set<ItemSet>> entry : merge.entrySet()) {
			ItemSet nSet = mergeItemSet(entry.getValue());
			newItemSet.getGotoTables().put(entry.getKey(), nSet);
			for (ItemSet is : entry.getValue()) {
				exists.put(is.getId(), nSet);
			}
			sd.put(nSet, entry.getValue());
		}

		for (var entry : sd.entrySet()) {
			affectedSet.addAll(mergeGotoTable(exists, entry.getKey(), entry.getValue()));
			lalrItemSets.add(entry.getKey());
		}

		return affectedSet;
	}

	/**
	 * 在lalrItemSets中找到相同核心的项
	 */
	private Set<ItemSet> findSameCore(Set<lr.ItemSet> backup) {
		Set<ItemSet> result = new HashSet<>();

		for (ItemSet lalrItemSet : backup) {

			boolean isMatch = false;
			for (ItemSet itemSet : backup) {
				if (lalrItemSet == itemSet) continue;

				if (lalrItemSet.containsCore(itemSet)) {
					result.add(lalrItemSet);
					result.add(itemSet);
					isMatch = true;
				}
			}

			if (isMatch) break ;
		}

		if (result.isEmpty()) return null;
		return result;
	}

	/**
	 * I -> J 通过α转换，将J合并为K后，I需要通过α指向K
	 * 获取I和α的映射集合, 然后重定向
	 * @param lalrItemSets
	 * @param mergeSet
	 * @return
	 */
	private Map<ItemSet, String> findGotoSourceItemSet(Set<lr.ItemSet> lalrItemSets, Set<ItemSet> mergeSet) {
		Map<ItemSet, String> result = new HashMap<>();
		for (ItemSet itemSet : lalrItemSets) {
			var entry = itemSet.getGotoTables().entrySet();
			for (Map.Entry<String, ItemSet> stringItemSetEntry : entry) {
				if (itemSet == stringItemSetEntry.getValue()) continue;

				if (mergeSet.contains(stringItemSetEntry.getValue())) {
					result.put(itemSet, stringItemSetEntry.getKey());
				}
			}
		}
		return result;
	}

	public void resetId() {
		ItemSet.itemSetIdCount = -1;
		for (ItemSet lrItemSet : lalrItemSets) {
			lrItemSet.setId(++ItemSet.itemSetIdCount);
		}
	}

	public void printItemCollection() {
		resetId();
		for (ItemSet lrItemSet : lalrItemSets) {
			lrItemSet.printItemSet();
		}
	}
}
