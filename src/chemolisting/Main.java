package chemolisting;

public class Main {

	public static void main(String[] args) {
		try {
			Listing object = new Listing();
			int[][] result = object.run();
			int[] subtotals = object.getSubtotal();
			int[][] seating = object.allocateChairs(result);
			InputOutput.writeResults(seating, subtotals);
			InputOutput.executeMacro();
			return;
		} catch (ListingException e) {
			e.printStackTrace();
		}
	}
}