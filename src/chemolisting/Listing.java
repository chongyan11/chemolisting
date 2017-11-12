package chemolisting;

import ilog.concert.*;
import ilog.cplex.*;

public class Listing {
// Temporary data declarations
	private final int NUM_TREATMENTS = 5;
	private final int NUM_SLOTS = 23;
	private final int NUM_CHAIRS = 44;
	private final int MANPOWER_FACTOR = 4;
	private final double ALPHA = 0.02;
	
	private int[] treatmentLength;
	private double[] treatmentRatio;
	private int[] manpower;
	private int[] maxChairs;
	
	public int[][] run() {
		setUp();
		return model();
	}
	
	private void setUp() {
	// TODO: add file reading
		int[] treatmentLength = {12, 8, 5, 2, 1};
		this.treatmentLength = treatmentLength;
		assert (treatmentLength.length == NUM_TREATMENTS);
		
		double[] treatmentRatio = {0.125, 0.25, 0.25, 0.125, 0.25};
		this.treatmentRatio = treatmentRatio;
		assert (treatmentRatio.length == NUM_TREATMENTS);
		double sum = 0;
		for (int i = 0; i < treatmentRatio.length; i++)
			sum += treatmentRatio[i];
		assert (sum == 1.0);
		
		int[] manpower = {10, 15, 15, 15, 15, 18, 13, 13, 13, 13, 13, 13, 15, 15, 18, 18, 18, 18, 8, 3, 3, 3, 3};
		this.manpower = manpower;
		assert (manpower.length == NUM_SLOTS);
		
		int[] numChairs = {44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 22, 11, 11, 11};
		this.maxChairs = numChairs;
		assert (numChairs.length == NUM_SLOTS);
	}
	
	private int[][] model() {
		try {
			IloCplex cplex = new IloCplex();
		
		// ** Decision Variables **
			
			// x: whether a treatment k is started at chair i at time j
			IloIntVar[][] x = new IloIntVar[NUM_SLOTS][];
			for (int i = 0; i < NUM_SLOTS; i++)
				x[i] = cplex.intVarArray(NUM_TREATMENTS, 0, NUM_CHAIRS);
			
			// y: whether a treatment k is ongoing at chair i at time j
			IloIntVar[][] y = new IloIntVar[NUM_SLOTS][];
			for (int i = 0; i < NUM_SLOTS; i++)
				y[i] = cplex.intVarArray(NUM_TREATMENTS, 0, NUM_CHAIRS);
			
			IloNumVar totalTreatments = cplex.numVar(0.0, Double.MAX_VALUE);
			IloNumVar[] subTotalTreatments = cplex.numVarArray(NUM_TREATMENTS, 0, Double.MAX_VALUE);
		
		// ** Objective Function **
			IloLinearNumExpr objective = cplex.linearNumExpr();
			for (int i = 0; i < NUM_SLOTS; i++)
				for (int j = 0; j < NUM_TREATMENTS; j++)
					objective.addTerm(treatmentLength[j], x[i][j]);
			cplex.addMaximize(objective);
			
		// ** Constraints **
			// Number of treatments of each type should sum to total number of treatments
			IloLinearNumExpr checkTotal = cplex.linearNumExpr();
			for (int j = 0; j < NUM_TREATMENTS; j++) {
				checkTotal.addTerm(1, subTotalTreatments[j]);
			}
			cplex.addEq(checkTotal, totalTreatments);
			
			// Sum of each treatment type's cases started per chair must equal subtotalTreatments[k]

			IloLinearNumExpr[] checkSubtotal = new IloLinearNumExpr[NUM_TREATMENTS];
			for (int j = 0; j < NUM_TREATMENTS; j++) {
				checkSubtotal[j] = cplex.linearNumExpr();
				for (int i = 0; i < NUM_SLOTS; i++) {
					checkSubtotal[j].addTerm(1, x[i][j]);;
				}
				cplex.addEq(checkSubtotal[j], subTotalTreatments[j]);
			}
			
			// Treatment ratios should be obeyed for each type of treatment
			for (int j = 0; j < NUM_TREATMENTS; j++) {
				IloNumExpr upperBound = cplex.prod((treatmentRatio[j] + ALPHA), totalTreatments);
				IloNumExpr lowerBound = cplex.prod((treatmentRatio[j] - ALPHA), totalTreatments);
				cplex.addLe(subTotalTreatments[j], upperBound);
				cplex.addGe(subTotalTreatments[j], lowerBound);
			}
			
			// Creating equivalency between x and y
			IloLinearIntExpr[] xsumLower = new IloLinearIntExpr[NUM_TREATMENTS];
			IloLinearIntExpr[] xsumUpper = new IloLinearIntExpr[NUM_TREATMENTS];
			for (int j = 0; j < NUM_TREATMENTS; j++) {
				xsumLower[j] = cplex.linearIntExpr();
				for (int i = treatmentLength[j] - 1; i < NUM_SLOTS; i++) {
					IloLinearIntExpr temp = cplex.linearIntExpr();
					for (int q = 0; q < treatmentLength[j]; q++)
						temp.addTerm(1, x[i-q][j]);
					xsumLower[j] = temp;
					cplex.addEq(xsumLower[j], y[i][j]);
				}
				xsumUpper[j] = cplex.linearIntExpr();
				for (int i = 0; i < treatmentLength[j] - 1; i++) {
					IloLinearIntExpr temp = cplex.linearIntExpr();
					for (int q = 0; q <= i; q++)
						temp.addTerm(1, x[q][j]);
					xsumUpper[j] = temp;
					cplex.addEq(xsumUpper[j], y[i][j]);
				}
			}
			
			// No treatment can be scheduled to end after closing time
			for (int j = 0; j < NUM_TREATMENTS; j++) {
				if (treatmentLength[j] > 1) {
					for (int i = (NUM_SLOTS - treatmentLength[j]); i < NUM_SLOTS; i++) {
						cplex.addEq(x[i][j], 0);
					}
				}
			}
			
			// Chairs occupied cannot exceed total chairs, cannot exceed a certain manpower factor
			// Number of treatments started cannot exceed manpower available at that time
			for (int i = 0; i < NUM_SLOTS; i++) {
				IloLinearIntExpr sumChairs = cplex.linearIntExpr();
				for (int j = 0; j < NUM_TREATMENTS; j++)
					sumChairs.addTerm(1, y[i][j]);
				cplex.addLe(sumChairs, (maxChairs[i] + 1));
				cplex.addLe(sumChairs, (MANPOWER_FACTOR * manpower[i]));
				
				IloLinearIntExpr sumStarts = cplex.linearIntExpr();
				for (int j = 0; j < NUM_TREATMENTS; j++)
					sumStarts.addTerm(1, x[i][j]);
				cplex.addLe(sumStarts, manpower[i]);
			}
			
		// ** Solving and printing **
			if (cplex.solve()) {
				System.out.println(cplex.getObjValue());
				for (int i = 0; i < NUM_SLOTS; i++) {
					for (int j = 0; j < NUM_TREATMENTS; j++) {
						System.out.print(cplex.getValue(x[i][j]) + " ");
					}
					System.out.println();
				}
				System.out.println();
				for (int i = 0; i < NUM_SLOTS; i++) {
					for (int j = 0; j < NUM_TREATMENTS; j++) {
						System.out.print(cplex.getValue(y[i][j]) + " ");
					}
					System.out.println();
				}
				System.out.println(cplex.getValue(totalTreatments));
				for (int j = 0; j < NUM_TREATMENTS; j++)
					System.out.print(cplex.getValue(subTotalTreatments[j]) + " ");
				System.out.println();
			}
			
		// ** Setting up return variable **
			int[][] result = new int[NUM_SLOTS][NUM_TREATMENTS];
			for (int i = 0; i < NUM_SLOTS; i++) {
				for (int j = 0; j < NUM_TREATMENTS; j++) {
					int num = (int) cplex.getValue(x[i][j]);
					result[i][j] = num;
				}
			}
			
			return result;
			
		} catch (IloException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public int[][] allocateChairs(int[][] result) {
		int[][] finalAllocation = new int[NUM_SLOTS][NUM_CHAIRS];
		int[][] allocation = new int[NUM_SLOTS][NUM_CHAIRS];
		for (int i = 0; i < NUM_SLOTS; i++) {
			for (int j = 0; j < NUM_TREATMENTS; j++) {
				int count = 0;
				int chairNum = 0;
				while (count < result[i][j] && chairNum < NUM_CHAIRS) {
					if (allocation[i][chairNum] == 0) {
						allocation[i][chairNum] = j+1;
						count++;
						for (int k = 1; k < treatmentLength[j]; k++) {
							allocation[i+k][chairNum] = -1;
						}
					}
					chairNum++;
				}
			}
		}
		int num = 0;
		for (int j = 0; j < NUM_CHAIRS; j++) {
			// System.out.println("Chair " + j);
			for (int i = 0; i < NUM_SLOTS; i++) {
				if (allocation[i][j] > -1)
					num = allocation[i][j];
				// System.out.print(num + " ");
				finalAllocation[i][j] = num;
			}
			// System.out.println();
		}
		
		return finalAllocation;
	}
}
