package struct.production;

/**
 * 产生式体的某个子项是非终结符还是终结符
 * 例如：E -> T * E
 * 其中，T、*、E 分别是产生式体中的三个子项
 */
public enum BodySubItemAttrType {
	nonTerminal,
	terminal
}
