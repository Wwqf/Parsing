import algorithm.FirstSet;
import algorithm.LeftCommonFactor;
import algorithm.LeftRecursion;
import cfg.CFG;
import cfg.CFGBuilder;
import fin.Fin;
import lalr.ItemCollection;
import lr.LR;
import slr.SLR;

import java.util.HashSet;
import java.util.Set;

public class Application {
	public static void main(String[] args) {


		Fin fin = Fin.getInstance();
		String inputFile = fin.getProjectPath() + "/src/data/input/CFG_1.cfg";
		System.out.println(inputFile);

		long totalStart = System.nanoTime();
		final CFG cfg = new CFGBuilder(inputFile).build();

//		ItemCollection collection = new ItemCollection(cfg);
//		collection.generateItemCollection();
//		collection.printItemCollection();
//		double time = CalculationTime.call(collection::generateItemCollection);
//		System.out.println("Generate ItemCollection -> " + time + "ms");
//
		CFG copyCfg = cfg.copy();
//
//		SLR slr = new SLR(cfg);
//		double time = CalculationTime.call(slr::construct);
//		System.out.println("SLR construct -> " + time + "ms");
//		slr.printActionAndGoto();
//		time = CalculationTime.call(slr::execute);
//		System.out.println("SLR identify -> " + time + "ms");



		copyCfg = new LeftCommonFactor(copyCfg).extract();
		copyCfg = new LeftRecursion(copyCfg).eliminate();
		FirstSet firstSet = new FirstSet(copyCfg);

		lr.ItemCollection itemCollection = new lr.ItemCollection(cfg, firstSet);
		itemCollection.generateItemCollection();
		itemCollection.printItemCollection();

//		LR lr = new LR(copyCfg);
//		double time = CalculationTime.call(lr::construct);
//		System.out.println("LR construct -> " + time + "ms");
//		lr.printActionAndGoto();
//		time = CalculationTime.call(lr::execute);
//		System.out.println("LR identify -> " + time + "ms");

		lalr.ItemCollection collection = new lalr.ItemCollection(itemCollection);
		collection.construct();
		collection.resetId();
		collection.printItemCollection();

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
