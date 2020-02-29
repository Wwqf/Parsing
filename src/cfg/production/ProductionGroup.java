package cfg.production;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * 一个产生式头对应的产生式组
 */
public class ProductionGroup implements Cloneable {

	// 该产生式组的产生式头部
	private String productionHead;
	// 该组是否有epsilon产生式
	private boolean hasEpsilon;
	// 为了输出效果，用LinkedHashSet
	private Set<Production> productions = new LinkedHashSet<>();

	public void addProduction(Production production) {
		productions.add(production);
	}

	public void addProduction(String productionStr) {
		addProduction(new Production(productionStr));
	}

	public void addProduction(LinkedList<SubItem> subItems) {
		addProduction(new Production(subItems));
	}

	public Set<Production> getProductions() {
		return productions;
	}

	public void setProductions(Set<Production> productions) {
		this.productions = productions;
	}

	public String getProductionHead() {
		return productionHead;
	}

	public void setProductionHead(String productionHead) {
		this.productionHead = productionHead;
	}

	public boolean isHasEpsilon() {
		return hasEpsilon;
	}

	public void setHasEpsilon(boolean hasEpsilon) {
		this.hasEpsilon = hasEpsilon;
	}

	/**
	 * 对外接口
	 * @return 返回ProductionGroup的clone
	 */
	public ProductionGroup copy() {
		try {
			return clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			System.exit(-2);
		}
		return null;
	}


	@Override
	protected ProductionGroup clone() throws CloneNotSupportedException {
		ProductionGroup cloProductionGroup = (ProductionGroup) super.clone();
		cloProductionGroup.productions = copy(productions);
		return cloProductionGroup;
	}

	private Set<Production> copy(Set<Production> src) {
		Set<Production> dest = new LinkedHashSet<>();
		for (Production t : src) dest.add(t.copy());
		return dest;
	}
}
