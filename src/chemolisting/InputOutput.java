package chemolisting;

import java.io.*;

public class InputOutput {
	
	private static final String OUTPUT_FILE_NAME = "output.csv";
	private static final String INPUT_FILE_NAME = "input.csv";
	private static final String NEWLINE = "\n";
	private static final String SEPARATOR = ",";
	
	public static void writeResults(int[][] allocation) { 
		try {
			FileWriter fw = new FileWriter(OUTPUT_FILE_NAME);
			PrintWriter pw = new PrintWriter(fw);
			// print in chair rows and slot columns 
			for (int j = 0; j < allocation[0].length; j++) {
				for (int i = 0; i < allocation.length; i++) {
					pw.print(allocation[i][j] + SEPARATOR);
				}
				pw.print(NEWLINE);
			}
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// write input reading method
	
}
