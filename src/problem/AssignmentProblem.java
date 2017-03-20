package problem;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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
	private List<List<Map<String, List<String>>>> schedule;
	private Map<Course, Map<String, Group>> coursesGroups;
	private Map<String, Student> students;
	private String outputPath;
	private IloCplex cplex;
	
	public AssignmentProblem(String scheduleFilename, String groupsFilename, String preferencesFilename, String gradesFilename, String procVersion, String outputPath) throws IloException, IOException {
		InputDataReader reader = new InputDataReader(scheduleFilename, groupsFilename, preferencesFilename, gradesFilename, procVersion);
		reader.readData();
		
		this.schedule = reader.getSchedule();
		this.coursesGroups = reader.getCoursesGroups();
		this.students = reader.getStudents();
		this.outputPath = outputPath;
		this.cplex = new IloCplex();
	}
	
	public void run() throws IloException, IOException {
		defineAssignmentProblemWithPreferences();
		//defineManualAssignmentProblem();
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
			
			cplex.addLe(constr_sumAssignedPreferences, 1); // Constraint: a student can have at most 1 preference assigned
		}
		
		for (Map<String, Group> groupMap : coursesGroups.values()) {
			for (Group group : groupMap.values()) {
				cplex.addLe(group.getConstrSumAssignedStudents(), group.getCapacity()); // Constraint: the sum of assigned students must not exceed this group's capacity
			}
		}
		
		// Set maximization of objective function
		cplex.addMaximize(objective);
	}
	
	private void defineManualAssignmentProblem() throws IloException {
		IloLinearIntExpr obj_maximizeAssignments = cplex.linearIntExpr(); // Sum of all group assignments for all students in all courses
		
		for (Student student : students.values()) { // For each student...
			Map<String, Map<String, IloIntVar>> courseGroupAssignments = new HashMap<>();
			
			for (Map.Entry<Course, Map<String, Group>> courseEntry : coursesGroups.entrySet()) { // For each course...
				Map<String, IloIntVar> groupAssignments = new HashMap<>();
				IloLinearIntExpr constr_maxOneGroupPerCourse = cplex.linearIntExpr(); // Sum of all group assignments for this course for this student
				
				for (Map.Entry<String, Group> groupEntry : courseEntry.getValue().entrySet()) { // For each group...
					IloIntVar var_assignedToGroup = cplex.boolVar(); // VARIABLE: this student is assigned to this group for this course
					
					// Paper constraint 1 (modified to allow partial assignments)
					constr_maxOneGroupPerCourse.addTerm(1, var_assignedToGroup);
					
					// Paper constraint 3
					groupEntry.getValue().addTermToConstrSumAssignedStudents(cplex, var_assignedToGroup);
					
					// Objective function
					obj_maximizeAssignments.addTerm(1, var_assignedToGroup);
					
					groupAssignments.put(groupEntry.getKey(), var_assignedToGroup); 
				}
				
				cplex.addLe(constr_maxOneGroupPerCourse, 1); // CONSTRAINT: a student can be assigned to at most 1 group per course
				courseGroupAssignments.put(courseEntry.getKey().getCode(), groupAssignments);
			}
			
			student.setCourseGroupAssignments(courseGroupAssignments);
			
			// Paper constraint 2
			for (List<Map<String, List<String>>> day : schedule) { // For each day...
				for (Map<String, List<String>> timeslot : day) { // For each timeslot...
					IloLinearIntExpr constr_maxOneGroupPerTimeslot = cplex.linearIntExpr(); // Sum of all group assignments for this timeslot for this student
					
					for (Map.Entry<String, List<String>> timeslotCourse : timeslot.entrySet()) { // For each course taught in this timeslot...
						String courseCode = timeslotCourse.getKey(); // Get the course code
						List<String> groupCodes = timeslotCourse.getValue(); // Get the codes of all taught groups
						
						for (String groupCode : groupCodes) { // For each group of this course taught in this timeslot...
							constr_maxOneGroupPerTimeslot.addTerm(1, courseGroupAssignments.get(courseCode).get(groupCode));
						}
					}
					
					cplex.addLe(constr_maxOneGroupPerTimeslot, 1); // CONSTRAINT: a student can be assigned to at most 1 group per timeslot
				}
			}
		}
		
		for (Map<String, Group> groupMap : coursesGroups.values()) {
			for (Group group : groupMap.values()) {
				cplex.addLe(group.getConstrSumAssignedStudents(), group.getCapacity()); // CONSTRAINT: the sum of assigned students must not exceed this group's capacity
			}
		}
		
		cplex.addMaximize(obj_maximizeAssignments); // OBJECTIVE: maximize the number of students assigned to groups
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
