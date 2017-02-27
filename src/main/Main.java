package main;
import java.io.IOException;
import java.util.List;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.cplex.IloCplex;
import model.Student;
import model.StudentGroup;
import model.StudentPreference;

public class Main {
	public static void main(String[] args) {
		try {
			InputDataReader reader = new InputDataReader("turmas.csv", "preferências.csv");
			reader.readInputData();
			
			List<StudentGroup> groups = reader.getGroups();
			List<Student> students = reader.getStudents();
			
			IloCplex cplex = new IloCplex();
			IloLinearIntExpr objective = cplex.linearIntExpr();
						
			for (Student student : students) {
				IloLinearIntExpr constr_sumAssignedPreferences = cplex.linearIntExpr(); // Sum of all boolean variables indicating an assigned preference
				
				for (StudentPreference preference : student.getPreferences()) {
					IloIntVar var_preferenceAssigned = cplex.boolVar(); // 1 if student gets this preference assigned, 0 otherwise
					
					preference.setVarPreferenceAssigned(var_preferenceAssigned);
					objective.addTerm(var_preferenceAssigned, preference.getWeight()); // Build objective function: preferenceAssigned * preferenceWeight
					constr_sumAssignedPreferences.addTerm(1, var_preferenceAssigned); // Build constraint: sum of all preference assignments for this student
					
					for (StudentGroup group : preference.getCourseGroupPairs().values()) {
						group.addTermToConstrSumAssignedStudents(cplex, var_preferenceAssigned); // Build constraint: sum of all students assigned to this group
					}
				}
				
				cplex.addLe(constr_sumAssignedPreferences, 1); // Constraint: A student can have at most 1 preference assigned
			}
			
			for (StudentGroup group : groups) {
				cplex.addLe(group.getConstrSumAssignedStudents(), group.getCapacity()); // Constraint: The sum of assigned students must not exceed this group's capacity
			}
			
			// Set maximization of objective function
			cplex.addMaximize(objective);
			
			// Solve the problem
			if (cplex.solve()) {
				OutputDataWriter writer = new OutputDataWriter(cplex, groups, students);
				writer.writeOutputData();
			}
			else {
				System.out.println("Failed to solve problem.");
			}
			
			// Free CPLEX resources
			cplex.end();
			
			/*int numStudents = students.size();
			
			// Define variables
			IloIntVar[][] preferenceAssigned = new IloIntVar[numStudents][]; // [student index][option index]
			
			// Define weights
			int[][] preferenceWeights = new int[numStudents][];
			
			// Define objective function
			IloLinearIntExpr objective = cplex.linearIntExpr();
			
			for (int i = 0; i < numStudents; ++i) {
				int currentStudentNumPreferences = students.get(i).getPreferences().size();
				
				// Create variables
				preferenceAssigned[i] = cplex.boolVarArray(currentStudentNumPreferences);
				
				for (int j = 0; j < currentStudentNumPreferences; ++j) {
					// Create weights
					preferenceWeights[i][j] = 1; // Each preference of each student has same weight
					
					// Create objective function
					objective.addTerm(preferenceAssigned[i][j], preferenceWeights[i][j]);
				}
			}
			
			cplex.addMaximize(objective);*/
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IloException e) {
			e.printStackTrace();
		}
	}
}
