import algorithm.*;
import cfg.CFG;
import cfg.CFGBuilder;
import cfg.production.Production;
import cfg.production.ProductionGroup;
import cfg.production.SubItem;
import cfg.production.SubItemType;
import fin.FileAttrCode;
import fin.Fin;
import fin.callback.IOJsonCallback;
import ll.LLOne;
import lr.*;

import java.util.LinkedList;

public class Application {
	public static void main(String[] args) {


		Fin fin = Fin.getInstance();
		String inputFile = fin.getProjectPath() + "/src/data/input/CFG_6.cfg";
		System.out.println(inputFile);

		long totalStart = System.nanoTime();
		final CFG cfg = new CFGBuilder(inputFile).build();

//		ItemCollection collection = new ItemCollection(cfg);
//		collection.generateItemCollection();
//		collection.printItemCollection();
//		double time = CalculationTime.call(collection::generateItemCollection);
//		System.out.println("Generate ItemCollection -> " + time + "ms");

		SLR slr = new SLR(cfg);
		double time = CalculationTime.call(slr::construct);
		System.out.println("SLR construct -> " + time + "ms");
		slr.printActionAndGoto();
		slr.execute();
		long totalEnd = System.nanoTime();
		System.out.println("Total time -> " + (1.0 * (totalEnd - totalStart) / 1000 / 1000) + "ms");
	}

	interface CalTimeCallBack {
		void method();
	}

	private static final class CalculationTime {

		static double call(CalTimeCallBack callBack) {
			long startTime = System.nanoTime();
			callBack.method();
			long endTime = System.nanoTime();
			return (1.0 * (endTime - startTime) / 1000 / 1000);
		}
	}
}
