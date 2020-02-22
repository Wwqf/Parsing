import io.*;
import io.callback.IOArrayCallback;
import io.write.ArrayWriteContent;
import struct.CFG;
import struct.CFGBuilder;

public class Application {
	public static void main(String[] args) {

		FileUtils utils = FileUtils.getInstance();
		System.out.println(utils.getProjectPath());

		CFG cfg = new CFGBuilder(utils.getProjectPath() + "/src/data/CFG_4").build();
		cfg.eliminateLeftRecursion();
	}
}
