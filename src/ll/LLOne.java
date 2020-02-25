package ll;

import algorithm.SelectSet;
import global.GlobalMark;
import io.BufferIO;
import io.FileUtils;
import log.Log;
import struct.CFG;
import struct.CFGBuilder;
import struct.production.BodyItem;
import struct.production.BodySubItem;
import struct.production.BodySubItemAttrType;

import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;

/**
 * LL(1)
 */
public class LLOne {

	// input
	private BufferIO buffer;

	private CFG cfg;
	private Map<String, Map<String, BodyItem>> selectSet;

	private Stack<BodySubItem> grammarStack;

	public LLOne(CFG cfg, SelectSet selectSet) {
		this.cfg = cfg;
		this.selectSet = selectSet.getSelectSet();

		FileUtils utils = FileUtils.getInstance();
		this.buffer = new BufferIO.Builder().setFilePath(utils.getProjectPath() + "/src/data/input/input.i").build();

		grammarStack = new Stack<>();
		grammarStack.push(new BodySubItem("$", BodySubItemAttrType.terminal));
		grammarStack.push(new BodySubItem(cfg.startSymbol, BodySubItemAttrType.nonTerminal));
	}

	public void execute() {
		BodySubItem subItem = grammarStack.peek();
		String inputStr = getNextInput();

		while (!subItem.getValue().equals("$")) {
			if (subItem.getValue().equals(inputStr)) {
				System.out.println("匹配: " + subItem.getValue());
				inputStr = getNextInput();
				grammarStack.pop();
			} else if (subItem.getAttr() == BodySubItemAttrType.terminal) {
				Log.error("error!");
			} else if (selectSet.get(subItem.getValue()).get(inputStr) == null) {
				Log.error("error!");
			} else if (selectSet.get(subItem.getValue()).get(inputStr) != null) {
				BodyItem bodyItem = selectSet.get(subItem.getValue()).get(inputStr);
				System.out.println("输出: " + subItem.getValue() + " -> " + bodyItem.getBodyStr());
				grammarStack.pop();

				Object[] iter = bodyItem.getSubItems().toArray();
				for (int i = iter.length - 1; i >= 0; i--) {
					if (!((BodySubItem)iter[i]).getValue().equals("ε"))
						grammarStack.push((BodySubItem)iter[i]);
				}
			}

			subItem = grammarStack.peek();
		}
	}

	private String getNextInput() {
		if (buffer.nextChar() == ' ') {
			while (buffer.nextChar() == ' ');
			buffer.nextMorpheme();
		}

		while (buffer.nextChar() != ' ' && !GlobalMark.stopLexicalAnalysis);
		return buffer.nextMorpheme();
	}
}
