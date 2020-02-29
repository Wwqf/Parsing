package algorithm;

import cfg.CFG;
import cfg.production.Production;

import java.util.Map;
import java.util.Set;

public class SyncSet {
	public static final String SyncUnitStr = "sync";

	private CFG cfg;
	private Map<String, Set<String>> followSet;
	private SelectSet selectSet;

	public SyncSet(CFG cfg, FollowSet followSet, SelectSet selectSet) {
		this.cfg = cfg;
		this.followSet = followSet.getFollowSet();
		this.selectSet = selectSet;
	}

}
