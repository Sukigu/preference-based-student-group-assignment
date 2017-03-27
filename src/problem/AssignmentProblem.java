package problem;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloNumExpr;
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
	private boolean isMandatoryAssignment;
	private String outputPath;
	private IloCplex cplex;
	
	public AssignmentProblem(String groupsFilename, String groupCompositesFilename, String preferencesFilename, String gradesFilename, String procVersion, boolean isMandatoryAssignment, String outputPath) throws IloException, IOException {
		InputDataReader reader = new InputDataReader(groupsFilename, groupCompositesFilename, preferencesFilename, gradesFilename, procVersion);
		reader.readData();
		
		this.schedule = reader.getSchedule();
		this.coursesGroups = reader.getCoursesGroups();
		this.students = reader.getStudents();
		this.isMandatoryAssignment = isMandatoryAssignment;
		this.outputPath = outputPath;
		this.cplex = new IloCplex();
	}
	
	public void run() throws IloException, IOException {
		//defineAssignmentProblemWithPreferences();
		defineManualAssignmentProblem();
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
		
		for (Map.Entry<Course, Map<String, Group>> courseEntry : coursesGroups.entrySet()) {
			for (Group group : courseEntry.getValue().values()) {
				if (!isMandatoryAssignment || courseEntry.getKey().getMandatory()) {
					cplex.addLe(group.getConstrSumAssignedStudents(), group.getCapacity()); // CONSTRAINT: the sum of assigned students must not exceed this group's capacity
				}
				// Else (if we're assigning mandatory courses but this course is optional), don't add a constraint for the group capacity, since we know for sure everyone fits  
			}
		}
		
		// Set maximization of objective function
		cplex.addMaximize(objective);
	}
	
	private void defineManualAssignmentProblem() throws IloException {
		IloLinearIntExpr sumAllGroupAssignmentsAllStudents = cplex.linearIntExpr(); // Sum of all group assignments for all students in all courses
		IloLinearIntExpr sumAllCompleteAssignments = cplex.linearIntExpr(); // Number of students assigned to all enrolled courses
		int numCourseEnrollments = 0; // Number of course enrollments (student-course pairs)
		
		for (Student student : students.values()) { // For each student...
			Map<String, Map<String, IloIntVar>> courseGroupAssignments = new HashMap<>();
			
			IloIntVar hasCompleteAssignment = cplex.boolVar(); // Boolean variable indicating if this student was assigned to all courses they enrolled in
			sumAllCompleteAssignments.addTerm(1, hasCompleteAssignment);
			
			IloLinearIntExpr sumAllGroupAssignments = cplex.linearIntExpr(); // Sum of group assignments for this student
			
			for (Map.Entry<Course, Map<String, Group>> courseEntry : coursesGroups.entrySet()) { // For each course...
				if (student.getEnrolledCourses().contains(courseEntry.getKey().getCode())) { // If this student is enrolled in it...
					++numCourseEnrollments;
					Map<String, IloIntVar> groupAssignments = new HashMap<>();
					IloLinearIntExpr sumAllGroupAssignmentsPerCourse = cplex.linearIntExpr(); // Sum of all group assignments for this course for this student
					
					for (Map.Entry<String, Group> groupEntry : courseEntry.getValue().entrySet()) { // For each group...
						IloIntVar var_assignedToGroup = cplex.boolVar(); // VARIABLE: this student is assigned to this group for this course
						
						// Add this variable to the sum of all group assignments for this course for this student
						sumAllGroupAssignmentsPerCourse.addTerm(1, var_assignedToGroup);
						
						// Add this variable to the sum of all student assignments for this group for this course
						groupEntry.getValue().addTermToConstrSumAssignedStudents(cplex, var_assignedToGroup);
						
						// Add this variable to the sum of all group assignments for all students in all courses
						sumAllGroupAssignmentsAllStudents.addTerm(1, var_assignedToGroup);
						
						// Add this variable to this student's personal assignments
						groupAssignments.put(groupEntry.getKey(), var_assignedToGroup); 
					}
					
					cplex.addLe(sumAllGroupAssignmentsPerCourse, 1); // CONSTRAINT: a student can be assigned to at most 1 group per course
					sumAllGroupAssignments.add(sumAllGroupAssignmentsPerCourse); // Add this course's group assignments to the sum of all group assignments for this student
					courseGroupAssignments.put(courseEntry.getKey().getCode(), groupAssignments);
				}
			}
			
			student.setCourseGroupAssignments(courseGroupAssignments);
			student.setHasCompleteAssignment(hasCompleteAssignment);
			
			// CONSTRAINT: the sum of all group assignments for this student must be <= variable indicating a complete assignment * number of course enrollments for this student
			IloLinearIntExpr completeAssignmentTimesNumEnrollments = cplex.linearIntExpr();
			completeAssignmentTimesNumEnrollments.addTerm(hasCompleteAssignment, student.getEnrolledCourses().size());
			cplex.addLe(sumAllGroupAssignments, completeAssignmentTimesNumEnrollments);
			
			// Time conflict constraints
			for (List<Map<String, List<String>>> day : schedule) { // For each day...
				for (Map<String, List<String>> timeslot : day) { // For each timeslot...
					IloLinearIntExpr sumAllGroupAssignmentsPerTimeslot = cplex.linearIntExpr(); // Sum of all group assignments for this timeslot for this student
					
					for (Map.Entry<String, List<String>> timeslotCourse : timeslot.entrySet()) { // For each course taught in this timeslot...
						if (student.getEnrolledCourses().contains(timeslotCourse.getKey())) { // If this student is enrolled in it...
							String courseCode = timeslotCourse.getKey(); // Get the course code
							List<String> groupCodes = timeslotCourse.getValue(); // Get the codes of all taught groups
							
							for (String groupCode : groupCodes) { // For each group of this course taught in this timeslot...
								IloIntVar temp = courseGroupAssignments.get(courseCode).get(groupCode);
								if (temp != null) sumAllGroupAssignmentsPerTimeslot.addTerm(1, temp); // Some groups might have been automatically added to this timeslot since they're part of a composite but in reality this course doesn't have them
							}
						}
					}
					
					cplex.addLe(sumAllGroupAssignmentsPerTimeslot, 1); // CONSTRAINT: a student can be assigned to at most 1 group per timeslot
				}
			}
		}
		
		for (Map.Entry<Course, Map<String, Group>> courseEntry : coursesGroups.entrySet()) {
			for (Group group : courseEntry.getValue().values()) {
				if (!isMandatoryAssignment || courseEntry.getKey().getMandatory()) {
					cplex.addLe(group.getConstrSumAssignedStudents(), group.getCapacity()); // CONSTRAINT: the sum of assigned students must not exceed this group's capacity
				}
				// Else (if we're assigning mandatory courses but this course is optional), don't add a constraint for the group capacity, since we know for sure everyone fits  
			}
		}
		
		IloNumExpr objectiveMaximizeCompleteAssignments = cplex.prod(.8 / students.size(), sumAllCompleteAssignments);
		IloNumExpr objectiveMaximizeSumAllAssignments = cplex.prod(.2 / numCourseEnrollments, sumAllGroupAssignmentsAllStudents);
		IloNumExpr objective = cplex.sum(objectiveMaximizeCompleteAssignments, objectiveMaximizeSumAllAssignments);
		cplex.addMaximize(objective); // OBJECTIVE: maximize (.8 * sum of complete assignments / total students) + (.2 * sum of all group assignments / number of course enrollments)
	}
	
	private void solve() throws IOException, IloException {
		// Solve the problem
		if (cplex.solve()) {
			OutputDataWriter writer = new OutputDataWriter(cplex, coursesGroups, students, outputPath);
			writer.writeOutputData();
		}
		else {
			System.out.println("Failed to solve problem.");
		}
		
		// Free CPLEX resources
		cplex.end();
	}
}
