package problem;

import java.io.IOException;
import java.util.Map;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.cplex.IloCplex;
import io.InputDataReader;
import io.OutputDataWriter;
import model.Course;
import model.Group;
import model.Student;
import model.StudentPreference;

public class AssignmentProblem {
	private Map<Course, Map<String, Group>> coursesGroups;
	private Map<String, Student> students;
	private String outputPath;
	private IloCplex cplex;
	
	public AssignmentProblem(String scheduleFilename, String groupsFilename, String preferencesFilename, String gradesFilename, String procVersion, String outputPath) throws IloException, IOException {
		InputDataReader reader = new InputDataReader(scheduleFilename, groupsFilename, preferencesFilename, gradesFilename, procVersion);
		reader.readData();
		
		this.coursesGroups = reader.getCoursesGroups();
		this.students = reader.getStudents();
		this.outputPath = outputPath;
		this.cplex = new IloCplex();
	}
	
	public void run() throws IloException, IOException {
		defineAssignmentProblemWithPreferences();
		solve();
	}
	
	private void defineAssignmentProblemWithPreferences() throws IloException {
		IloLinearIntExpr objective = cplex.linearIntExpr();
		
		for (Student student : students.values()) {
			IloLinearIntExpr constr_sumAssignedPreferences = cplex.linearIntExpr(); // Sum of all boolean variables indicating an assigned preference
			
			for (StudentPreference preference : student.getPreferences().values()) {
				IloIntVar var_preferenceAssigned = cplex.boolVar(); // 1 if student gets this preference assigned, 0 otherwise
				
				preference.setVarPreferenceAssigned(var_preferenceAssigned);
				objective.addTerm(var_preferenceAssigned, preference.getWeight()); // Build objective function: preferenceAssigned * preferenceWeight
				constr_sumAssignedPreferences.addTerm(1, var_preferenceAssigned); // Build constraint: sum of all preference assignments for this student
				
				for (Group group : preference.getCourseGroupPairs().values()) {
					group.addTermToConstrSumAssignedStudents(cplex, var_preferenceAssigned); // Build constraint: sum of all students assigned to this group
				}
			}
			
			cplex.addLe(constr_sumAssignedPreferences, 1); // Constraint: A student can have at most 1 preference assigned
		}
		
		for (Map<String, Group> groupMap : coursesGroups.values()) {
			for (Group group : groupMap.values()) {
				cplex.addLe(group.getConstrSumAssignedStudents(), group.getCapacity()); // Constraint: The sum of assigned students must not exceed this group's capacity
			}
		}
		
		// Set maximization of objective function
		cplex.addMaximize(objective);
	}
	
	private void solve() throws IOException, IloException {
		// Solve the problem
		if (cplex.solve()) {
			OutputDataWriter writer = new OutputDataWriter(cplex, coursesGroups, students);
			writer.writeOutputData(outputPath);
		}
		else {
			System.out.println("Failed to solve problem.");
		}
		
		// Free CPLEX resources
		cplex.end();
	}
}
