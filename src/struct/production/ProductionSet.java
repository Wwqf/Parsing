package struct.production;

import java.util.HashSet;
import java.util.Set;

/**
 * 一个产生式头部对应的产生式体集合
 */
public class ProductionSet {
	public boolean hasEpsilon = false;
	private Set<BodyItem> bodies = new HashSet<>();

	public void addBody(BodyItem bodyItem) {
		bodies.add(bodyItem);
	}

	public void addBody(String bodyStr) {
		BodyItem body = new BodyItem(bodyStr);
		bodies.add(body);
	}

	public Set<BodyItem> getBodies() {
		return bodies;
	}

	public void setBodies(Set<BodyItem> bodies) {
		this.bodies = bodies;
	}
}

