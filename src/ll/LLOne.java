package ll;

import algorithm.SelectSet;
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

import javax.swing.*;
import java.util.Map;
import java.util.Stack;

/**
 * LL(1)
 */
public class LLOne {

	// input
	private BufferIO buffer;

	private CFG cfg;
	private Map<String, Map<String, Production>> selectSet;

	private Stack<SubItem> grammarStack;

	public LLOne(CFG cfg, SelectSet selectSet) {
		this.cfg = cfg;
		this.selectSet = selectSet.getSelectSet();

		Fin utils = Fin.getInstance();
		this.buffer = new BufferIO.Builder().setFilePath(utils.getProjectPath() + "/src/data/input/input.i").build();

		grammarStack = new Stack<>();
		grammarStack.push(new SubItem("$", SubItemType.terminal));
		grammarStack.push(new SubItem(cfg.getStartSymbol(), SubItemType.nonTerminal));
	}

	public void execute() {
		Fout fout = new Fout();
		fout.addColumn(new ColumnAttr("Matched", FoutGravity.LEFT));
		fout.addColumn(new ColumnAttr("Stack", FoutGravity.RIGHT));
		fout.addColumn(new ColumnAttr("Input", FoutGravity.RIGHT));
		fout.addColumn(new ColumnAttr("Action"));


		SubItem subItem = grammarStack.peek();
		String inputStr = getNextInput();

		String matched = "";
		String action = "";
		boolean isAccept = true;
		while (!subItem.getValue().equals("$")) {
			if (subItem.getValue().equals(inputStr)) {
				action = "match: " + subItem.getValue();
				matched += subItem.getValue() + " ";
				inputStr = getNextInput();
				grammarStack.pop();
			} else if (subItem.getType() == SubItemType.terminal) {
//				Log.error("error!");
				isAccept = false;
				break;
			} else if (selectSet.get(subItem.getValue()).get(inputStr) == null) {
//				Log.error("error!");
				isAccept = false;
				break;
			} else if (selectSet.get(subItem.getValue()).get(inputStr) != null) {
				Production production = selectSet.get(subItem.getValue()).get(inputStr);
				action = "output: " + subItem.getValue() + " -> " + production.getProductionStr();
				grammarStack.pop();

				Object[] iter = production.getSubItems().toArray();
				for (int i = iter.length - 1; i >= 0; i--) {
					if (!((SubItem)iter[i]).getValue().equals("ε"))
						grammarStack.push((SubItem)iter[i]);
				}
			}
			recordProcess(fout, matched, inputStr, action);
			subItem = grammarStack.peek();
		}

		if (isAccept) {
			fout.insertln("success", "success!", "success!", "success!");
		} else {
			fout.insertln("failed.", "failed.", "failed.", "failed.");
		}
		fout.fout();
	}

	public void recordProcess(Fout fout, String matched, String morpheme, String action) {
		StringBuilder stack = new StringBuilder();
		for (SubItem subItem : grammarStack) {
			stack.insert(0, subItem.getValue() + " ");
		}
		String bufStr = buffer.getCurrentBufferString();
		fout.insertln(matched, stack.toString(), morpheme + bufStr, action);
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
}
