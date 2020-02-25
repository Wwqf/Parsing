import algorithm.FirstSet;
import algorithm.FollowSet;
import algorithm.SelectSet;
import io.*;
import io.callback.IOArrayCallback;
import io.write.ArrayWriteContent;
import ll.LLOne;
import struct.CFG;
import struct.CFGBuilder;

public class Application {
	public static void main(String[] args) {

		FileUtils utils = FileUtils.getInstance();
		System.out.println(utils.getProjectPath());

		CFG cfg = new CFGBuilder(utils.getProjectPath() + "/src/data/input/CFG_5").build();
		cfg.extractLeftCommonFactor();
		System.out.println(cfg.getProductionsString());
		cfg.eliminateLeftRecursion();
		System.out.println(cfg.getProductionsString());

		System.out.println("\n\nFirstSet:-");
		FirstSet firstSet = new FirstSet(cfg);
		firstSet.getFirstSet();
		firstSet.printFirstSet();
		System.out.println("\n\nFollowSet:-");
		FollowSet followSet = new FollowSet(cfg, firstSet);
		followSet.getFollowSet();
		followSet.printFollowSet();
		System.out.println("\n\nSelectSet:-");
		SelectSet selectSet = new SelectSet(cfg, firstSet, followSet);
		selectSet.getSelectSet();
		selectSet.printSelectSet();

		LLOne llOne = new LLOne(cfg, selectSet);
		llOne.execute();
	}
}
