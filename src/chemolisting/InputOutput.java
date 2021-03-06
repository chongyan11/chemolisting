package chemolisting;

import java.io.*;
import java.util.ArrayList;
import com.jacob.com.*;
import com.jacob.activeX.*;

public class InputOutput {

	private static final String OUTPUT_FILE_NAME = "output.csv";
	private static final String INPUT_FILE_NAME = "system/input.csv";
	private static final String TOCONVERT_FILE_NAME = "input.xlsm";
	private static final String MACRO_FILE_NAME = "listing.xlsm";
	private static final String MACRO_NAME = "Sheet1.Main";
	private static final String NEWLINE = "\n";
	private static final String SEPARATOR = ",";

	private static int numTreatments;
	private static ArrayList<Integer> lengthTreatments = new ArrayList<Integer>();
	private static ArrayList<Double> ratioTreatments = new ArrayList<Double>();
	private static double alpha;
	private static int maxChairs;
	private static double maxNurseRatio;
	private static int numSlots;
	private static ArrayList<Double> manpower = new ArrayList<Double>();
	private static ArrayList<Integer> chairs = new ArrayList<Integer>();
	private static ArrayList<Integer> noShow = new ArrayList<Integer>();
	private static ArrayList<Integer> prevBookings = new ArrayList<Integer>();
	private static double noShowFactor;
	private static double startedCasesToNursesRatio;
	private static double startingCasesToNursesRatio;
	private static double maxCasesPerTimeSlot;

	public static void writeResults(int[][] allocation, int[] subtotal) {
		try {
			FileWriter fw = new FileWriter(OUTPUT_FILE_NAME);
			PrintWriter pw = new PrintWriter(fw);
			// print out number of chairs and number of slots
			pw.print(allocation[0].length + SEPARATOR + allocation.length + SEPARATOR);
			pw.println();
			// print out number of treatments
			pw.println(numTreatments);
			// print out treatment lengths
			for (int i = 0; i < numTreatments; i++) {
				pw.print(lengthTreatments.get(i) + SEPARATOR);
			}
			pw.println();
			// print out subtotal number of treatments
			for (int i = 0; i < numTreatments; i++) {
				pw.print(subtotal[i] + SEPARATOR);
			}
			pw.println();
			// print in chair rows and slot columns
			for (int j = 0; j < allocation[0].length; j++) {
				for (int i = 0; i < allocation.length; i++) {
					pw.print(allocation[i][j] + SEPARATOR);
				}
				pw.println();
			}
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void executeMacro() {
		File file = new File(MACRO_FILE_NAME);
		callExcelMacro(file, MACRO_NAME);
	}

	private static void callExcelMacro(File file, String macroName) {
		ComThread.InitSTA();
		final ActiveXComponent excel = new ActiveXComponent("Excel.Application");
		try {
			// Opens Excel if property is set to true
			excel.setProperty("Visible", new Variant(true));
			final Dispatch workbooks = excel.getProperty("Workbooks").toDispatch();
			final Dispatch workBook = Dispatch.call(workbooks, "Open", file.getAbsolutePath()).toDispatch();

			// Calls the macro
			final Variant result = Dispatch.call(excel, "Run", new Variant(macroName));

			// Saves file
			Dispatch.call(workBook, "Save");

			// Closes the excel file
			// com.jacob.com.Variant f = new com.jacob.com.Variant(true);
			// Dispatch.call(workBook, "Close", f);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Closes Microsoft Excel itself
			// excel.invoke("Quit", new Variant[0]);
			ComThread.Release();
		}
	}

	// write input reading method
	public static void readInputFile() {
		try {
			ExcelConvert.convert(TOCONVERT_FILE_NAME, INPUT_FILE_NAME);
			FileReader fr = new FileReader(INPUT_FILE_NAME);
			BufferedReader br = new BufferedReader(fr);
			String line = new String();
			String[] string;

			// HARDCODE: First readline for number of treatments
			line = br.readLine();
			string = line.split(SEPARATOR, 3);
			numTreatments = (int) Double.parseDouble(string[1]);

			// HARDCODE: Discard Second readline and use Third readline for
			// number of treatments
			line = br.readLine();
			line = br.readLine();
			string = line.split(SEPARATOR);
			for (int i = 1; i < string.length; i++) {
				int x = (int) Double.parseDouble(string[i]);
				lengthTreatments.add(x);
			}

			// HARDCODE: Fourth readline for ratio of treatments
			line = br.readLine();
			string = line.split(SEPARATOR);
			for (int i = 1; i < string.length; i++) {
				double x = Double.parseDouble(string[i]);
				ratioTreatments.add(x);
			}

			// HARDCODE: Fifth readline for number of bookings last month
			line = br.readLine();
			string = line.split(SEPARATOR);
			for (int i = 1; i < string.length; i++) {
				int x = (int) Double.parseDouble(string[i]);
				prevBookings.add(x);
			}

			// HARDCODE: Sixth readline for number of noshows last month
			line = br.readLine();
			string = line.split(SEPARATOR);
			for (int i = 1; i < string.length; i++) {
				int x = (int) Double.parseDouble(string[i]);
				noShow.add(x);
			}

			// HARDCODE: Seventh readline for no show factor
			line = br.readLine();
			string = line.split(SEPARATOR, 3);
			noShowFactor = Double.parseDouble(string[1]);

			// HARDCODE: Eight readline for allowable error (alpha)
			line = br.readLine();
			string = line.split(SEPARATOR, 3);
			alpha = Double.parseDouble(string[1]);

			// HARDCODE: Ninth readline for Max Number of chairs
			line = br.readLine();
			string = line.split(SEPARATOR, 3);
			maxChairs = (int) Double.parseDouble(string[1]);

			// HARDCODE: Tenth readline for Max ongoing cases to nurses ratio
			line = br.readLine();
			string = line.split(SEPARATOR, 3);
			maxNurseRatio = Double.parseDouble(string[1]);

			// HARDCODE: Eleventh readline for Started Cases to Nurses Ratio
			line = br.readLine();
			string = line.split(SEPARATOR, 3);
			startedCasesToNursesRatio = Double.parseDouble(string[1]);

			// HARDCODE: Twelfth readline for Starting Cases to Nurses Ratio
			line = br.readLine();
			string = line.split(SEPARATOR, 3);
			startingCasesToNursesRatio = Double.parseDouble(string[1]);

			// HARDCODE: Thirteenth readline for Max Cases Per Time Slot
			line = br.readLine();
			string = line.split(SEPARATOR, 3);
			maxCasesPerTimeSlot = Double.parseDouble(string[1]);

			// HARDCODE: Fourteenth readline for Number of Timeslots
			line = br.readLine();
			string = line.split(SEPARATOR, 3);
			numSlots = (int) Double.parseDouble(string[1]);

			// HARDCODE: Discard Fifteenth readline and Sixteenth readline for
			// manpower at each timeslot
			line = br.readLine();
			line = br.readLine();
			string = line.split(SEPARATOR);
			for (int i = 1; i < string.length; i++) {
				Double x = Double.parseDouble(string[i]);
				manpower.add(x);
			}

			// HARDCODE: Seventeenth readline for number of operational chairs
			// at each timeslot
			line = br.readLine();
			string = line.split(SEPARATOR);
			for (int i = 1; i < string.length; i++) {
				int x = (int) Double.parseDouble(string[1]);
				chairs.add(x);
			}

			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static int getNumTreatments() {
		return numTreatments;
	}

	public static int[] getTreatmentLengths() {
		int[] lengths = new int[lengthTreatments.size()];
		for (int i = 0; i < lengthTreatments.size(); i++) {
			lengths[i] = lengthTreatments.get(i);
		}
		return lengths;
	}

	public static double[] getTreatmentRatios() {
		double[] ratios = new double[ratioTreatments.size()];
		for (int i = 0; i < ratioTreatments.size(); i++) {
			ratios[i] = ratioTreatments.get(i);
		}
		return ratios;
	}

	public static double getAlpha() {
		return alpha;
	}

	public static int getMaxChairs() {
		return maxChairs;
	}

	public static double getMaxNurseRatio() {
		return maxNurseRatio;
	}

	public static int getNumSlots() {
		return numSlots;
	}

	public static double[] getManpower() {
		double[] mp = new double[manpower.size()];
		for (int i = 0; i < manpower.size(); i++) {
			mp[i] = manpower.get(i);
		}
		return mp;
	}

	public static int[] getChairs() {
		int[] c = new int[chairs.size()];
		for (int i = 0; i < chairs.size(); i++) {
			c[i] = chairs.get(i);
		}
		return c;
	}

	public static int[] getPrevBookings() {
		int[] pb = new int[prevBookings.size()];
		for (int i = 0; i < prevBookings.size(); i++) {
			pb[i] = prevBookings.get(i);
		}
		return pb;
	}

	public static int[] getPrevNoShows() {
		int[] ns = new int[noShow.size()];
		for (int i = 0; i < noShow.size(); i++) {
			ns[i] = noShow.get(i);
		}
		return ns;
	}

	public static double getNoShowFactor() {
		return noShowFactor;
	}

	public static double getMaxStartedCasesPerNurse() {
		return startedCasesToNursesRatio;
	}

	public static double getMaxStartingCasesPerNurse() {
		return startingCasesToNursesRatio;
	}

	public static double getMaxStartingCasesPerTimeSlot() {
		return maxCasesPerTimeSlot;
	}
}
