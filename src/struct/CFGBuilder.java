package struct;

import io.FileAttrCode;
import io.FileUtils;
import io.callback.IOArrayCallback;
import log.Log;
import struct.production.BodySubItem;
import struct.production.BodySubItemAttrType;
import struct.production.ProductionSet;
import struct.production.BodyItem;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class CFGBuilder {
	// Non-terminal Set
	public Set<String> nonTerminals;

	// Terminal Set
	public Set<String> terminals;

	// Start Symbol
	public String startSymbol;

	// Production Rule Mapping Table
	// 每个产生式头部对应一个产生式集合, 即产生式体可能存在多个
	public Map<String, ProductionSet> productions;

	public CFGBuilder(String filename) {
		nonTerminals = new LinkedHashSet<>();
		terminals = new LinkedHashSet<>();
		productions = new LinkedHashMap<>();

		read(filename);
		extract();
	}

	private void read(String filename) {
		FileUtils utils = FileUtils.getInstance();

		// 读取文件
		utils.readFile(filename, (IOArrayCallback) (array, code) -> {
			if (code != FileAttrCode.readSuccess) {
				Log.error("read file error!");
				return ;
			}

			// preHead为上次处理的产生式头部
			String preHead = null;

			// 遍历文件中每行产生式，然后判断分析该产生式
			for (String str : array) {
				if (str.equals("")) continue;

				// 以 -> 分隔产生式，如果分隔后的长度为2，则是一个完整的产生式（head -> body），
				// 如果分隔后的长度为1，则是一个不完整的产生式，可能只包含一个body。
				String[] production = str.trim().split("->");

				if (production.length == 2) {
					completeProductionProcess(production[0].trim(), production[1].trim());
					preHead = production[0].trim();
				} else if (production.length == 1) {
					// 如果在不完整的产生式情况，并且还不存在上一个preHead，则有异常。
					if (preHead == null) {
						Log.warning("production grammar has wrong !");
						return ;
					}
					incompleteProductionProcess(preHead, production[0].trim());
				}
			}
		});
	}

	/**
	 * 完整产生式处理，即有head -> body
	 * @param head
	 * @param body
	 */
	private void completeProductionProcess(String head, String body) {
		if (startSymbol == null) startSymbol = head;

		// 获取prHead对应的产生式集合
		ProductionSet productionSet = productions.get(head);

		// 第一次创建时，可能返回null，则new一个productionSet
		if (productionSet == null) productionSet = new ProductionSet();

		// 分割产生式体
		String[] body_slice = body.split("\\|");

		// 添加到产生式集合中
		for (String s : body_slice) {
			if (s.equals("")) continue;
			productionSet.addBody(s.trim());
		}

		// 添加到 productions中
		productions.put(head, productionSet);
	}

	/**
	 * 不完整产生式处理，即只存在body，而没有head
	 * @param head
	 * @param body
	 */
	private void incompleteProductionProcess(String head, String body) {
		ProductionSet productionSet = productions.get(head);

		/**
		 * 不可能存在productionSet为空的情况，如果为空，肯定包含错误。
		 */
		if (productionSet == null) {
			Log.warning("production grammar has wrong !");
			return ;
		}

		// 分割产生式体
		String[] body_slice = body.split("\\|");

		// 添加到产生式集合中
		for (String s : body_slice) {
			if (s.equals("")) continue;
			productionSet.addBody(s.trim());
		}
	}

	/**
	 * 提取productions中的内容
	 */
	private void extract() {
		// 获取productions的键值对
		Set<Map.Entry<String, ProductionSet>> entry = productions.entrySet();

		// 遍历键值对
		entry.forEach(stringSetEntry -> {
			String keys = stringSetEntry.getKey();
			ProductionSet values = stringSetEntry.getValue();

			// 将key添加到非终结符集合中
			nonTerminals.add(keys);

			// 对产生式体进行处理，包括终结符的提取，以及分离body中的每一个子项
			values.getBodies().forEach(item -> separationBody(values, item));
		});
	}

	/**
	 * 分离body中的子项
	 * @param productionSet
	 * @param body
	 */
	private void separationBody(ProductionSet productionSet, BodyItem body) {
		StringBuilder temporary = new StringBuilder();

		for (char c : body.getBodyStr().toCharArray()) {
			if (c == ' ') {
				// 读到空格，则处理temporary
				insertBodyItem(productionSet, body, temporary);
			} else {
				temporary.append(c);
			}
		}

		if (temporary.length() != 0) {
			insertBodyItem(productionSet, body, temporary);
		}
	}

	/**
	 * 向BodyItem.subItems中添加子项
	 * @param productionSet
	 * @param body
	 * @param temporary 分割的字符串，可能是终结符，可能是非终结符
	 */
	private void insertBodyItem(ProductionSet productionSet, BodyItem body, StringBuilder temporary) {
		boolean hasKey = productions.containsKey(temporary.toString());

		BodySubItem subItem = null;
		if (!hasKey) {
			// 是终结符, 向终结符集合中添加
			terminals.add(temporary.toString());
			subItem = new BodySubItem(temporary.toString(), BodySubItemAttrType.terminal);

			// 如果该终结符为ε，则该productionSet的hasEpsilon属性为true
			if (temporary.toString().equals("ε")) productionSet.hasEpsilon = true;
		} else {
			// 非终结符
			subItem = new BodySubItem(temporary.toString(), BodySubItemAttrType.nonTerminal);
		}
		body.getSubItems().add(subItem);
		// 清空字符串
		temporary.delete(0, temporary.length());
	}

	public CFG build() {
		return new CFG(this);
	}
}
