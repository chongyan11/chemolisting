package chemolisting;

import ilog.concert.*;
import ilog.cplex.*;

public class Listing {
// Temporary data declarations
	private int NUM_TREATMENTS;
	private int NUM_SLOTS;
	private int NUM_CHAIRS;
	private double MANPOWER_FACTOR;
	private double ALPHA;
	
	private int[] treatmentLength;
	private double[] treatmentRatio;
	private int[] manpower;
	private int[] maxChairs;
	
	public int[][] run() throws ListingException {
		setUp();
		return model();
	}
	
	private void setUp() throws ListingException {
	// TODO: add file reading
		InputOutput.readInputFile();
		
		NUM_TREATMENTS = InputOutput.getNumTreatments();
		NUM_SLOTS = InputOutput.getNumSlots();
		NUM_CHAIRS = InputOutput.getMaxChairs();
		MANPOWER_FACTOR = InputOutput.getMaxNurseRatio();
		ALPHA = InputOutput.getAlpha();
		
		int[] treatmentLength = InputOutput.getTreatmentLengths();
		this.treatmentLength = treatmentLength;
		if (!(treatmentLength.length == NUM_TREATMENTS))
			throw new ListingException("Number of treatments do not match (1)");
		
		double[] treatmentRatio = InputOutput.getTreatmentRatios();
		this.treatmentRatio = treatmentRatio;
		if (!(treatmentRatio.length == NUM_TREATMENTS))
			throw new ListingException("Number of treatments do not match (2)");
		double sum = 0;
		for (int i = 0; i < treatmentRatio.length; i++)
			sum += treatmentRatio[i];
		if (sum > 1.0)
			throw new ListingException("Sum of ratios exceed 1");
		else if (sum < 1.0)
			throw new ListingException("Sum of ratios below 1");
		
		int[] manpower = InputOutput.getManpower();
		this.manpower = manpower;
		if (!(manpower.length == NUM_SLOTS)) 
			throw new ListingException("Number of slots do not match (1)");
		
		int[] numChairs = InputOutput.getChairs();
		this.maxChairs = numChairs;
		if (!(numChairs.length == NUM_SLOTS))
			throw new ListingException("Number of slots do not match (2)");
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
