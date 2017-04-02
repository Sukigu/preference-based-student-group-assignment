package problem;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloNumExpr;
import ilog.cplex.IloCplex;
import io.InputDataReader;
import io.OutputDataWriter;
import model.Course;
import model.Group;
import model.Schedule;
import model.Student;
import model.StudentPreference;
import model.Timeslot;

public class AssignmentProblem {
	private Schedule schedule;
	private Map<String, Course> courses;
	private Map<String, Student> students;
	private boolean isMandatoryAssignment;
	private String outputPath;
	private IloCplex cplex;
	
	public AssignmentProblem(String coursesFilename, String groupsFilename, String groupCompositesFilename, String preferencesFilename, String gradesFilename, int semester, String procVersion, boolean isMandatoryAssignment, String outputPath) throws IloException, IOException {
		InputDataReader reader = new InputDataReader(coursesFilename, groupsFilename, groupCompositesFilename, preferencesFilename, gradesFilename, semester, procVersion);
		reader.readData();
		
		this.schedule = reader.getSchedule();
		this.courses = reader.getCourses();
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
			
			for (StudentPreference preference : student.getPreferences()) {
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
		
		for (Course course : courses.values()) {
			for (Group group : course.getGroups().values()) {
				if (!isMandatoryAssignment || course.getMandatory()) {
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
			
			for (Course course : student.getEnrolledMandatoryCourses()) { // For each mandatory course this student is enrolled in...
				++numCourseEnrollments;
				Map<String, IloIntVar> groupAssignments = new HashMap<>();
				IloLinearIntExpr sumAllGroupAssignmentsPerCourse = cplex.linearIntExpr(); // Sum of all group assignments for this course for this student
				
				for (Group group : course.getGroups().values()) { // For each group...
					IloIntVar var_assignedToGroup = cplex.boolVar(); // VARIABLE: this student is assigned to this group for this course
					
					// Add this variable to the sum of all group assignments for this course for this student
					sumAllGroupAssignmentsPerCourse.addTerm(1, var_assignedToGroup);
					
					// Add this variable to the sum of all student assignments for this group for this course
					group.addTermToConstrSumAssignedStudents(cplex, var_assignedToGroup);
					
					// Add this variable to the sum of all group assignments for all students in all courses
					sumAllGroupAssignmentsAllStudents.addTerm(1, var_assignedToGroup);
					
					// Add this variable to this student's personal assignments
					groupAssignments.put(group.getCode(), var_assignedToGroup); 
				}
				
				cplex.addLe(sumAllGroupAssignmentsPerCourse, 1); // CONSTRAINT: a student can be assigned to at most 1 group per course
				sumAllGroupAssignments.add(sumAllGroupAssignmentsPerCourse); // Add this course's group assignments to the sum of all group assignments for this student
				courseGroupAssignments.put(course.getCode(), groupAssignments);
			}
			
			student.setCourseGroupAssignments(courseGroupAssignments);
			student.setHasCompleteAssignment(hasCompleteAssignment);
			
			// Add constraint: if sum of all assignments for this student < no. of enrolled courses, then he doesn't have a complete assignment (variable = 0)
			cplex.add(cplex.ifThen(cplex.le(sumAllGroupAssignments, student.getEnrolledMandatoryCourses().size() - 1), cplex.eq(hasCompleteAssignment, 0)));
			
			// Time conflict constraints
			for (Timeslot timeslot : schedule) { // For each timeslot...
				IloLinearIntExpr sumAllPracticalGroupAssignmentsPerTimeslot = cplex.linearIntExpr(); // Sum of all practical group assignments for this timeslot for this student
				
				for (Map.Entry<Course, Set<Group>> timeslotCourse : timeslot.getPracticalClasses().entrySet()) { // For each course that has a practical class in this timeslot...
					if (student.getEnrolledMandatoryCourses().contains(timeslotCourse.getKey())) { // If this student is enrolled in it...
						String courseCode = timeslotCourse.getKey().getCode(); // Get the course code
						Set<Group> groups = timeslotCourse.getValue(); // Get the codes of all taught groups
						
						for (Group group : groups) { // For each group of this course taught in this timeslot...
							IloIntVar temp = courseGroupAssignments.get(courseCode).get(group.getCode());
							if (temp != null) sumAllPracticalGroupAssignmentsPerTimeslot.addTerm(1, temp); // Some groups might have been automatically added to this timeslot since they're part of a composite but in reality this course doesn't have them
						}
					}
				}
				
				cplex.addLe(sumAllPracticalGroupAssignmentsPerTimeslot, 1); // CONSTRAINT: a student can be assigned to at most 1 group with a practical class per timeslot
			}
		}
		
		for (Course course : courses.values()) {
			for (Group group : course.getGroups().values()) {
				if (!isMandatoryAssignment || course.getMandatory()) {
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
			OutputDataWriter writer = new OutputDataWriter(cplex, courses, students, outputPath);
			writer.writeOutputData();
		}
		else {
			System.out.println("Failed to solve problem.");
		}
		
		// Free CPLEX resources
		cplex.end();
	}
}
