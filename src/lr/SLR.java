package lr;

import algorithm.FirstSet;
import algorithm.FollowSet;
import algorithm.LeftCommonFactor;
import algorithm.LeftRecursion;
import cfg.CFG;
import cfg.production.Production;
import cfg.production.SubItem;
import cfg.production.SubItemType;
import fin.BufferIO;
import fin.Fin;
import fout.Fout;
import fout.attr.ColumnAttr;
import fout.attr.FoutGravity;
import logger.Log;

import java.util.*;

public class SLR {

	private CFG cfg;
	private ProductionIdGenerate idGenerate;
	private FirstSet firstSet;
	private FollowSet followSet;
	private ItemCollection lrItemCollection;

	// key是项集符号，value是<终结符或非终结符，动作>
	private Map<Integer, Map<String, Action>> actionTables;
	// key是项集编号，value是<终结符或非终结符，产生式编号>
	private Map<Integer, Map<String, Integer>> gotoTables;

	private BufferIO buffer;
	private Stack<Integer> itemSetIdStack;

	public SLR(CFG cfg) {
		this.cfg = cfg.copy();

		// 获取项集族
		lrItemCollection = new ItemCollection(this.cfg);
		lrItemCollection.generateItemCollection();
		lrItemCollection.resetId();
		idGenerate = lrItemCollection.getIdGenerate();

		// 提取左公因子，消除左递归
		cfg = new LeftCommonFactor(cfg).extract();
	    cfg = new LeftRecursion(cfg).eliminate();

	    // 求First集和Follow集
		this.firstSet = new FirstSet(cfg);
		firstSet.getFirstSet();
		// !!! 对源cfg的拷贝获取First集，但是求Follow集的时候使用源cfg，而不是拷贝
		this.followSet = new FollowSet(this.cfg, firstSet);
		followSet.getFollowSet();

		this.actionTables = new LinkedHashMap<>();
		this.gotoTables = new LinkedHashMap<>();

		cfg.printProduction();
		firstSet.printFirstSet();
		followSet.printFollowSet();
		lrItemCollection.printItemCollection();

		Fin utils = Fin.getInstance();
		this.buffer = new BufferIO.Builder().setFilePath(utils.getProjectPath() + "/src/data/input/input.i").build();
		this.itemSetIdStack = new Stack<>();
		// 将开始项集的编号加入到栈中
		this.itemSetIdStack.push(lrItemCollection.getStartItemSet().getId());
	}

	public void execute() {
		Fout fout = new Fout();
		fout.addColumn(new ColumnAttr("Stack", FoutGravity.LEFT));
		fout.addColumn(new ColumnAttr("Symbol", FoutGravity.LEFT));
		fout.addColumn(new ColumnAttr("Input", FoutGravity.RIGHT));
		fout.addColumn(new ColumnAttr("Action", FoutGravity.LEFT));

		String inputStr = getNextInput();

		String actionStr = "";
		List<String> symbolList = new ArrayList<>();
		StringBuilder symbolStr;
		String morphemeStr = "";
		StringBuilder stackStr;
		while (true) {
			Integer peekId = itemSetIdStack.peek();
			Action action = actionTables.get(peekId).get(inputStr);

			stackStr = new StringBuilder();
			symbolStr = new StringBuilder();

			if (action.type == ActionType.shift) {
				for (int item : itemSetIdStack) {
					stackStr.append(item).append(" ");
				}

				for (String str : symbolList) {
					symbolStr.append(str).append(" ");
				}

				actionStr = "Shift -> " + action.id;
				symbolList.add(inputStr);

				itemSetIdStack.push(action.id);
				morphemeStr = inputStr + buffer.getCurrentBufferString();
				inputStr = getNextInput();

			} else if (action.type == ActionType.reduce) {
				for (int item : itemSetIdStack) {
					stackStr.append(item).append(" ");
				}

				for (String str : symbolList) {
					symbolStr.append(str).append(" ");
				}

				morphemeStr = inputStr + buffer.getCurrentBufferString();

				// 弹出归约个数个符号
				Production p = idGenerate.getProduction(action.id);
				int num = p.getSubItems().size(); //产生式编号
				for (int i = num; i > 0; i--) {
					itemSetIdStack.pop();
				}

				// 将Goto[t, A]压入
				peekId = itemSetIdStack.peek(); // 项集编号
				String head = idGenerate.getProductionHead(action.id); // 产生式编号
				var singleGoto = gotoTables.get(peekId);
				if (singleGoto.containsKey(head)) {
					int itemSetId = singleGoto.get(head);
					itemSetIdStack.push(itemSetId);
				} else {
					Log.error("error!");
				}

				actionStr = "According to [" +
						idGenerate.getProductionHead(action.id) + " -> " + p.getProductionStr() +
						"] reduce.";
				for (int i = num, j = symbolList.size() - 1; i > 0; i--, j--) {
					symbolList.remove(j);
				}
				symbolList.add(head);
			} else if (action.type == ActionType.accept) {
				for (int item : itemSetIdStack) {
					stackStr.append(item).append(" ");
				}

				for (String str : symbolList) {
					symbolStr.append(str).append(" ");
				}

				Log.debug("success!");
				fout.insertln(stackStr.toString(), symbolStr.toString(), morphemeStr, "Accept!");
				break;
			} else if (action.type == ActionType.error) {
				Log.error("error!");
			}

			fout.insertln(stackStr.toString(), symbolStr.toString(), morphemeStr, actionStr);
		}
		fout.fout();
	}

	private String getNextInput() {
		char c = buffer.nextChar();
		if (c == ' ' || c == '\t'  || c == '\n') {
			while (c == ' ' || c == '\t'  || c == '\n') c = buffer.nextChar();
			buffer.nextMorpheme();
		}

		while (true) {
			if (BufferIO.stopLexicalAnalysis) break;

			c = buffer.nextChar();
			if (c == ' ' || c == '\t'  || c == '\n') break;
		}
		return buffer.nextMorpheme();
	}

	public void construct() {
		// 对项集族遍历
		var collection = lrItemCollection.getLrItemSets();
		for (ItemSet itemSet : collection) {
			Map<String, Action> singleAction = new HashMap<>();
			Map<String, Integer> singleGoto = new HashMap<>();

			actionTables.put(itemSet.getId(), singleAction);
			gotoTables.put(itemSet.getId(), singleGoto);

			// 项，也是产生式
			for (Item item : itemSet.getLrKernelItems()) {
				analyze(itemSet, singleAction, singleGoto, item);
			}


			for (Item item : itemSet.getLrNonKernelItems()) {
				analyze(itemSet, singleAction, singleGoto, item);
			}

		}
	}

	private void analyze(ItemSet itemSet, Map<String, Action> singleAction, Map<String, Integer> singleGoto, Item item) {
		SubItem subItem = item.getExpectSubItem();
		if (subItem != null) {
			var itemSetGotoTable = itemSet.getGotoTables();

			if (subItem.getType() == SubItemType.nonTerminal) {
				// add gotoTables
				int jmpItemSetId = itemSetGotoTable.get(subItem.getValue()).getId();
				singleGoto.put(subItem.getValue(), jmpItemSetId);

			} else {
				// add actionTables
				int jmpItemSetId = itemSetGotoTable.get(subItem.getValue()).getId();
				Action action = new Action(ActionType.shift, jmpItemSetId);
				singleAction.put(subItem.getValue(), action);
			}

		} else {
			// 获取产生式头部，然后找到Follow(head)
			String head = item.getProductionHead();

			if (head.equals(this.cfg.getStartSymbol())) {
				Action action = new Action(ActionType.accept, -1);
				singleAction.put("$", action);
				return;
			}

			var follows = followSet.getFollowSet().get(head);
			for (String follow : follows) {
				// 对其中的每个终结符，都加入到action中 归约为产生式编号
				Action action = new Action(ActionType.reduce, item.getProductionId());
				singleAction.put(follow, action);
			}
		}
	}

	/**
	 * 一个项集对应一个Action集
	 */
	class Action {
		// 一个Action包含动作类型和项集编号
		ActionType type;
		int id;

		public Action(ActionType type, int id) {
			this.type = type;
			this.id = id;
		}
	}

	enum ActionType {
		shift, // 移入
		reduce, // 归约
		accept, // 接受
		error; // 报错

		public static String convert(ActionType type) {
			switch (type) {
				case shift: return "s";
				case reduce: return "r";
				case accept: return "acc";
				case error: return "error";
			}
			return "";
		}
	}

	public void printActionAndGoto() {
		Fout fout = new Fout(ColumnAttr.qCreate("State", "Action", "Goto"));
		String[] terminals = null;

		if (cfg.getTerminals().contains("ε")) {
			terminals = new String[cfg.getTerminals().size()];
		} else terminals = new String[cfg.getTerminals().size() + 1];
		cfg.getTerminals().toArray(terminals);

		boolean hasDollar = false;
		for (int i = 0; i < terminals.length; i++) {
			if (String.valueOf(terminals[i]).equals("ε")) {
				terminals[i] = "$";
				hasDollar = true;
			}
		}
		if (!hasDollar) terminals[terminals.length - 1] = "$";
		fout.addSubColumn("Action", ColumnAttr.qCreate(terminals));
		Object[] nonTerminals = cfg.getNonTerminals().toArray();
		fout.addSubColumn("Goto", ColumnAttr.qCreate(nonTerminals));

		for (int i = 0; i <= ItemSet.itemSetIdCount; i++) {
			Map<String, Action> singleAction = actionTables.get(i);
			Map<String, Integer> singleGoto = gotoTables.get(i);

			fout.insert(i);
			String str = "";
			for (Object terminal : terminals) {
				Action action = singleAction.get(String.valueOf(terminal));
				if (action == null) str = "";
				else {
					if (action.type == ActionType.accept || action.type == ActionType.error) {
						str = ActionType.convert(action.type);
					} else {
						str = ActionType.convert(action.type) + action.id;
					}
				}

				fout.insert(str);
			}

			for (Object nonTerminal : nonTerminals) {
				if (singleGoto.containsKey(String.valueOf(nonTerminal))) {
					str = String.valueOf(singleGoto.get(String.valueOf(nonTerminal)));
				} else {
					str = "";
				}


				fout.insert(str);
			}
		}

		fout.fout();
	}
}
