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
	
	public void run() {
		setUp();
		model();
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
			sum += treatmentLength[i];
		assert (sum == 1.0);
		
		int[] manpower = {10, 15, 15, 15, 15, 18, 13, 13, 13, 13, 13, 13, 15, 15, 18, 18, 18, 18, 8, 3, 3, 3, 3};
		this.manpower = manpower;
		assert (manpower.length == NUM_SLOTS);
		
		int[] numChairs = {44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 22, 11, 11, 11};
		this.maxChairs = numChairs;
		assert (numChairs.length == NUM_SLOTS);
	}
	
	private void model() {
		try {
			IloCplex cplex = new IloCplex();
		
		// ** Decision Variables **
			
			// x: whether a treatment k is started at chair i at time j
			IloIntVar[][][] x = new IloIntVar[NUM_CHAIRS][NUM_SLOTS][];
			for (int i = 0; i < NUM_CHAIRS; i++)
				for (int j = 0; j < NUM_SLOTS; j++)
					x[i][j] = cplex.intVarArray(NUM_TREATMENTS, 0, 1);
			
			// y: whether a treatment k is ongoing at chair i at time j
			IloIntVar[][][] y = new IloIntVar[NUM_CHAIRS][NUM_SLOTS][];
			for (int i = 0; i < NUM_CHAIRS; i++)
				for (int j = 0; j < NUM_SLOTS; j++)
					y[i][j] = cplex.intVarArray(NUM_TREATMENTS, 0, 1);
			
			IloNumVar totalTreatments = cplex.numVar(0.0, Double.MAX_VALUE);
			IloNumVar[] subTotalTreatments = cplex.numVarArray(NUM_TREATMENTS, 0, Double.MAX_VALUE);
		
		// ** Objective Function **
			IloLinearNumExpr objective = cplex.linearNumExpr();
			for (int k = 0; k < NUM_TREATMENTS; k++) 
				for (int i = 0; i < NUM_CHAIRS; i++) 
					for (int j = 0; j < NUM_SLOTS; j++) 
						objective.addTerm(treatmentLength[k], x[i][j][k]);
		
		// ** Constraints **
			
			
		} catch (IloException e) {
			e.printStackTrace();
		}
	}
}
