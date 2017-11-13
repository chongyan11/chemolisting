package chemolisting;

public class Main {
	
	public static void main (String[] args) {
		Listing object = new Listing();
		int[][] result = object.run();
		int[][] seating = object.allocateChairs(result);
		InputOutput.writeResults(seating);
		return;
	}
}
