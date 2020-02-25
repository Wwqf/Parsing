package algorithm;

import javafx.beans.binding.SetExpression;
import log.Log;
import struct.CFG;
import struct.production.BodyItem;
import struct.production.BodySubItem;
import struct.production.BodySubItemAttrType;
import struct.production.ProductionSet;

import java.util.*;

public class FollowSet {

	private static final String InputRightEndSym = "$";

	private CFG cfg;
	// nonTerminal -> {terminal Symbol}
	private Map<String, Set<String>> followSet;
	private Map<String, Set<String>> firstSet;

	private boolean isUpdated = false;

	public FollowSet(CFG cfg, FirstSet firstSet) {
		this.cfg = cfg;
		followSet = new HashMap<>();
		this.firstSet = firstSet.getFirstSet();

		// 添加输入右端结束标记
		Set<String> startSymSet = new HashSet<>();
		startSymSet.add(InputRightEndSym);
		followSet.put(cfg.getStartSymbol(), startSymSet);
	}

	public Map<String, Set<String>> getFollowSet() {
		if (isUpdated) return followSet;

		Map<String, DependencyTree> dependencyTrees = new HashMap<>();
		DependencyTree startSymDepTree = registerDependencyTree(dependencyTrees, cfg.startSymbol);
		startSymDepTree.addFollowSym(InputRightEndSym);

		for (String nonTerminal : cfg.getNonTerminals()) {
			calculationFollowSet(dependencyTrees, nonTerminal);
		}

		isUpdated = true;
		return this.followSet;
	}

	private void calculationFollowSet(Map<String, DependencyTree> dependencyTrees, String nonTerminal) {
		ProductionSet productionSet = cfg.getProductions().get(nonTerminal);

		DependencyTree nonTerDepTree = dependencyTrees.get(nonTerminal);
		if (nonTerDepTree == null) {
			nonTerDepTree = new DependencyTree(nonTerminal, dependencyTrees);
			dependencyTrees.put(nonTerminal, nonTerDepTree);
		}

		// 1. 外层循环控制一个产生式体
		for (BodyItem bodyItem : productionSet.getBodies()) {
			ListIterator<BodySubItem> iterator = bodyItem.getSubItems().listIterator(0);

			// 2. 内层循环遍历当前产生式体的子项
			while (iterator.hasNext()) {
				ListIterator<BodySubItem> it = bodyItem.getSubItems().listIterator(iterator.nextIndex());
				recursiveQuery(dependencyTrees, nonTerminal, it);
				iterator.next();
			}

		}

		// 到这里，已经检查完了
		dependencyTrees.forEach((key, value) -> followSet.put(key, value.curFollowSet));
	}

	private void recursiveQuery(Map<String, DependencyTree> dependencyTrees, String nonTerminal, ListIterator<BodySubItem> iterator) {
		// 如果迭代器没有下一个选项，退出递归
		if (!iterator.hasNext()) return ;

		BodySubItem subItem = iterator.next();

		// 如果当前子项是非终结符
		if (subItem.getAttr() == BodySubItemAttrType.nonTerminal) {

			// 获取当前子项的follow集
			DependencyTree curSubItemDepTree = registerDependencyTree(dependencyTrees, subItem.getValue());
			DependencyTree nonTerDepTree = registerDependencyTree(dependencyTrees, nonTerminal);

			/*
			 * 有几种情况：
			 * 1. 下一个子项的first集不包含epsilon    2. 下一个子项的first集包含epsilon
			 * 3. 下一个子项是终结符 4. 没有下一个项了，当前子项是末尾
			 */

			if (!iterator.hasNext()) {
				// 当前子项是末尾, 则将nonTerminal的follow集加入到当前子项的Follow集，
				curSubItemDepTree.addFollowSym(nonTerDepTree.curFollowSet);

				// 并把当前子项添加到nonTerminal的依赖项中
				nonTerDepTree.addDependencyNonTerminal(subItem.getValue());
			} else {

				boolean exit_abnormally = false;
				while (iterator.hasNext()) {

					// 判断下一子项是非终结符还是终结符
					BodySubItem nextSubItem = iterator.next();
					if (nextSubItem.getAttr() == BodySubItemAttrType.terminal) {
						// 如果是终结符，则将该终结符加入到Follow集
						curSubItemDepTree.addFollowSym(nextSubItem.getValue());
						exit_abnormally = true;
						break;
					} else {
						// 如果是非终结符, 获取该非终结符的first集，然后判断是否有epsilon
						Set<String> nextSubItemFirst = firstSet.get(nextSubItem.getValue());
						assert nextSubItemFirst != null;

						if (nextSubItemFirst.contains("ε")) {
							// 先去掉集合中的epsilon，然后加入到当前子项的follow集
							nextSubItemFirst.remove("ε");
							curSubItemDepTree.addFollowSym(nextSubItemFirst);
							// 然后再检查之后的每项
						} else {
							curSubItemDepTree.addFollowSym(nextSubItemFirst);
							exit_abnormally = true;
							break;
						}
					}
				}

				if (exit_abnormally) return ;

				// 当前子项是末尾, 则将nonTerminal的follow集加入到当前子项的Follow集，
				curSubItemDepTree.addFollowSym(nonTerDepTree.curFollowSet);
				// 并把当前子项添加到nonTerminal的依赖项中
				nonTerDepTree.addDependencyNonTerminal(subItem.getValue());

			}
		}
	}

	/**
	 * 注册一个节点
	 * @param dependencyTrees
	 * @param nonTer
	 * @return
	 */
	private DependencyTree registerDependencyTree(Map<String, DependencyTree> dependencyTrees, String nonTer) {
		DependencyTree curSubItemDepTree = dependencyTrees.get(nonTer);
		if (curSubItemDepTree == null) {
			curSubItemDepTree = new DependencyTree(nonTer, dependencyTrees);
			dependencyTrees.put(nonTer, curSubItemDepTree);
		}
		return curSubItemDepTree;
	}


	public void setUpdated(boolean updated) {
		isUpdated = updated;
	}

	/**
	 * 依赖树， 包含当前非终结符以及该终结符的follow集
	 * 还包含更新该非终结符时同样要更新的 被依赖非终结符集
	 *
	 * Todo 依赖树中有依赖关系的节点都是被动更新的。例如文法E：
	 *  E -> T E`
	 *  E`的Follow集依赖于E，如果E被更新，则E`也要更新
	 *  则E存储一个E`的指针，当E更新时，也更新E`的Follow集
	 */
	static final class DependencyTree {
		String curNonTerminal;
		Map<String, DependencyTree> fatherMaps;
		Set<String> curFollowSet;

		Set<String> dependencyNonTerminal;

		/**
		 *  更新者，在进行递归更新时，可能存在环，设置更新者可以消除对立环，而不能消除三级以上的环。
		 *  如: Ds' -> Ds         Ds -> Ds' 可以被消除 {在这里 -> 是指更新的非终结符}
		 *  而如：E -> T -> T' -> E 时，则不能被消除。
		 */
//		String updater = "";

		/**
		 * Todo 所以，以集合的方式，来消除多级环.
		 *  以 E -> T -> S -> V -> E' 举例:
		 *  E 对应的updater，只有 {}, depTree {T}
		 *  T 对应的updater，只有 {E}, depTree {S}
		 *  S 对应的updater，只有 {E, T}, depTree {V}
		 *  V 对应的updater，只有 {E, T, S}, depTree {E'}
		 *  ....
		 *  而 E -> T -> S -> E :
		 *  E 对应的updater，只有 {T, S}, depTree {T} - 检测到成环，取消更新
		 *  T 对应的updater，只有 {E}, depTree {S}
		 * 	S 对应的updater，只有 {E, T}, depTree {E} - 检测到成环，取消更新
		 *
		 * 	Todo but!
		 * 	 这样很消耗空间，如果假定只存在对立环，还是只用String updater好了。
		 * 	 时间复杂度应当差不了太多，毕竟Set以hash值判断.
		 *
		 * 	 对立环存在是因为文法的二义性
		 */
		Set<String> updater = new HashSet<>();

		public DependencyTree(String curNonTerminal, Map<String, DependencyTree> fatherMaps) {
			this.curNonTerminal = curNonTerminal;
			this.fatherMaps = fatherMaps;
			curFollowSet = new HashSet<>();
			dependencyNonTerminal = new HashSet<>();
		}

		public void addDependencyNonTerminal(String nonTer) {
			if (nonTer.equals(curNonTerminal)) return ;

			this.dependencyNonTerminal.add(nonTer);
		}

		/*
		 * 每当 当前非终结符的follow集更新时，也要更新依赖项的非终结符
		 */

		public void addFollowSym(String followSym) {
			curFollowSet.add(followSym);
			updateDependencyNonTerminal();
		}

		public void addFollowSym(Set<String> followSymSet) {
			curFollowSet.addAll(followSymSet);
			updateDependencyNonTerminal();
		}

		private void updateDependencyNonTerminal() {
			dependencyNonTerminal.forEach(nonTer -> {

				DependencyTree depItem = fatherMaps.get(nonTer);
				// 检查是否有依赖成环
				if (updater.contains(nonTer)) {
					// 如果更新 '我'的，将会被我更新，则取消这次更新
					return ;
				}

				depItem.updater.add(curNonTerminal);
				depItem.updater.addAll(this.updater);

				depItem.addFollowSym(this.curFollowSet);
			});

		}
	}

	public void printFollowSet() {
		followSet.forEach((key, value) -> {
			System.out.println(key + " -> " + value);
		});
	}
}
