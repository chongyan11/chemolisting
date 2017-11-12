package chemolisting;

public class Main {
	
	public static void main (String[] args) {
		Listing object = new Listing();
		int[][] result = object.run();
		int[][] seating = object.allocateChairs(result);
		
		for (int j = 0; j < seating[0].length; j++) {
			for (int i = 0; i < seating.length; i++) {
				System.out.print(seating[i][j] + " ");
			}
			System.out.println();
		}
		return;
	}
}
