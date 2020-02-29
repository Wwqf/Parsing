package cfg.production;

/**
 * 子项属性类型
 *
 * 例如：E -> T * E
 * 其中，T、*、E 分别是产生式体中的三个子项;
 *  T 和 E 是nonTerminal，* 是terminal
 */
public enum SubItemType {
	nonTerminal,
	terminal
}
