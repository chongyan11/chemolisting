package chemolisting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

class ExcelConvert {

	static void convert(String inputPath, String outputPath) {
		// For storing data into CSV files
		StringBuffer data = new StringBuffer();
		File inputFile = new File(inputPath);
		File outputFile = new File(outputPath);

		try {
			FileOutputStream fos = new FileOutputStream(outputFile);

			// Get the workbook object for XLSX file
			XSSFWorkbook wBook = new XSSFWorkbook(new FileInputStream(inputFile));

			// Get first sheet from the workbook
			XSSFSheet sheet = wBook.getSheetAt(0);
			Row row;
			Cell cell;

			// Iterate through each rows from first sheet
			Iterator<Row> rowIterator = sheet.iterator();
			int i = 1;
			while (rowIterator.hasNext() && i < 19) {
				row = rowIterator.next();

				// For each row, iterate through each columns
				Iterator<Cell> cellIterator = row.cellIterator();
				int j = 1;
				while (cellIterator.hasNext() && j < 53) {
					cell = cellIterator.next();
					switch (cell.getCellTypeEnum()) {
					case NUMERIC:
						data.append(cell.getNumericCellValue() + ",");

						break;
					case STRING:
						data.append(cell.getStringCellValue() + ",");

						break;
					default:
						data.append(cell + ",");
					}
					j++;
				}
				data.append("\n");
				i++;
			}

			fos.write(data.toString().getBytes());
			fos.close();
		} catch (Exception ioe) {
			ioe.printStackTrace();
		}
	}
}