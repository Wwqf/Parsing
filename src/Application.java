import io.*;
import io.callback.IOArrayCallback;
import io.write.ArrayWriteContent;
import struct.CFG;
import struct.CFGBuilder;

public class Application {
	public static void main(String[] args) {

		FileUtils utils = FileUtils.getInstance();

		CFG cfg = new CFGBuilder(utils.getProjectPath() + "/src/data/CFG_1").build();
		cfg.eliminateLeftRecursion();
		System.out.println();
	}
}
