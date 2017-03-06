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

public class AssignmentProblem {
	private List<Student> students;
	List<StudentGroup> groups;
	
	public AssignmentProblem(List<Student> students, List<StudentGroup> groups) {
		this.students = students;
		this.groups = groups;
	}
	
	public void solve(String outputPath) throws IloException, IOException {
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
			writer.writeOutputData(outputPath);
		}
		else {
			System.out.println("Failed to solve problem.");
		}
		
		// Free CPLEX resources
		cplex.end();
	}
}
