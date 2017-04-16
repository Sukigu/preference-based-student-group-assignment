package problem;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearIntExprIterator;
import ilog.concert.IloLinearNumExpr;
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
	private Map<String, Course> courses;
	private Schedule schedule;
	private Map<String, Student> students;
	private boolean isMandatoryAssignment;
	private IloCplex cplex;
	private OutputDataWriter writer;
	
	private int targetNumOccupiedTimeslots;
	
	public AssignmentProblem(String coursesFilename, String groupsFilename, String scheduleFilename, String groupCompositesFilename, String preferencesFilename, String gradesFilename, int semester, String procVersion, boolean isMandatoryAssignment, String outputPath) throws IloException, IOException {
		InputDataReader reader = new InputDataReader(coursesFilename, groupsFilename, scheduleFilename, groupCompositesFilename, preferencesFilename, gradesFilename, semester, procVersion);
		reader.readData();

		this.courses = reader.getCourses();
		this.schedule = reader.getSchedule();
		this.students = reader.getStudents();
		this.isMandatoryAssignment = isMandatoryAssignment;
		this.cplex = new IloCplex();
		this.writer = new OutputDataWriter(cplex, cplex.getParam(IloCplex.DoubleParam.EpRHS), courses, students, outputPath);
		
		this.targetNumOccupiedTimeslots = 0;
	}
	
	public void run() throws IloException, IOException {
		writer.checkGroupCapacities();
		defineManualAssignmentProblem();
		solve();
	}
	
	private void defineManualAssignmentProblem() throws IloException {
		IloLinearNumExpr weightedSumAllAssignments = cplex.linearNumExpr(); // Summation of each student's assignments multiplied by their grade
		IloLinearNumExpr weightedSumAllCompleteStudents = cplex.linearNumExpr(); // Summation of all variables indicating a student assigned to all of their courses multiplied by their grade
		IloLinearNumExpr weightedSumFulfilledPreferences = cplex.linearNumExpr(); // Summation of all variables indicating a student preference fulfilled multiplied by their grade
		IloLinearIntExpr sumAllOccupiedTimeslots = cplex.linearIntExpr(); // Sum of all timeslots occupied individually by all students
		
		float sumEnrollmentsTimesAvgGrade = 0; // Summation of each student's number of course enrollments multiplied by their grade
		float sumAvgGrades = 0; // Sum of every student's grade
		
		for (Student student : students.values()) {
			float avgGrade = student.getAvgGrade();
			IloLinearIntExpr sumAllAssignmentsPerStudent = processStudentAssignments(student); // Sum of all assignments for this student
			
			IloLinearIntExprIterator studentAssignmentsIterator = sumAllAssignmentsPerStudent.linearIterator();
			while (studentAssignmentsIterator.hasNext()) { // Iterating over this student's assignment variables to add them to the objective function multiplied by their grade
				IloIntVar studentAssignment = studentAssignmentsIterator.nextIntVar();
				weightedSumAllAssignments.addTerm(avgGrade, studentAssignment);
			}
			
			IloIntVar completeStudent = cplex.boolVar("(Complete assignment for " + student.getCode() + ")"); // VARIABLE: student was assigned to all of their courses?
			cplex.add(cplex.ifThen(cplex.le(sumAllAssignmentsPerStudent, student.getEnrolledCourses().size() - 1), cplex.eq(completeStudent, 0))); // CONSTRAINT: if sum of all assignments < number of enrolled courses, then it's not a complete assignment
			
			weightedSumAllCompleteStudents.addTerm(avgGrade, completeStudent);
			student.setHasCompleteAssignment(completeStudent); // Set this student's complete status variable
			
			for (StudentPreference preference : student.getPreferences()) {
				processStudentPreferences(student, preference, weightedSumFulfilledPreferences);
			}
			
			for (Timeslot timeslot : schedule) {
				processStudentTimeslots(student, timeslot, sumAllOccupiedTimeslots);
			}
			
			sumEnrollmentsTimesAvgGrade += student.getEnrolledCourses().size() * avgGrade;
			sumAvgGrades += avgGrade;
		}
		
		for (Course course : courses.values()) {
			for (Group group : course.getGroups().values()) {
				if (!isMandatoryAssignment || course.getMandatory()) {
					cplex.addLe(group.getSumAllAssignedStudents(), group.getCapacity()); // CONSTRAINT: sum of all assigned students <= group's capacity
				}
				// Else (if we're assigning mandatory courses but this course is optional), don't add a constraint for the group capacity, since we know for sure everyone fits
			}
		}
		
		IloNumExpr objMaximizeSumAllAssignments = cplex.prod(1. / sumEnrollmentsTimesAvgGrade, weightedSumAllAssignments);
		IloNumExpr objMaximizeCompleteStudents = cplex.prod(1. / sumAvgGrades, weightedSumAllCompleteStudents);
		IloNumExpr objMaximizeOccupiedTimeslots = cplex.prod(1. / targetNumOccupiedTimeslots, sumAllOccupiedTimeslots);
		IloNumExpr objMaximizeFulfilledPreferences = cplex.prod(1. / sumAvgGrades, weightedSumFulfilledPreferences);
		cplex.addMaximize(cplex.sum(cplex.prod(.1, objMaximizeSumAllAssignments), cplex.prod(.4, objMaximizeCompleteStudents), cplex.prod(.4, objMaximizeOccupiedTimeslots), cplex.prod(.1, objMaximizeFulfilledPreferences)));
	}
	
	private IloLinearIntExpr processStudentAssignments(Student student) throws IloException {
		IloLinearIntExpr sumAllAssignmentsPerStudent = cplex.linearIntExpr();
		
		for (Course course : student.getEnrolledCourses()) {
			targetNumOccupiedTimeslots += course.getWeeklyTimeslots();
			
			IloLinearIntExpr sumAllAssignmentsPerStudentPerCourse = processStudentAssignmentsPerCourse(student, course); // Sum of all assignments for this student and this course
			sumAllAssignmentsPerStudent.add(sumAllAssignmentsPerStudentPerCourse);
		}
		
		return sumAllAssignmentsPerStudent;
	}
	
	private IloLinearIntExpr processStudentAssignmentsPerCourse(Student student, Course course) throws IloException {
		IloLinearIntExpr sumAllAssignmentsPerStudentPerCourse = cplex.linearIntExpr();
		
		Map<Group, IloIntVar> groupAssignments = new HashMap<>();
		
		for (Group group : course.getGroups().values()) {
			IloIntVar studentAssigned = cplex.boolVar("(" + student.getCode() + ": " + course.getCode() + "-" + group.getCode() + ")"); // VARIABLE: student assigned to this course-group pair?
			sumAllAssignmentsPerStudentPerCourse.addTerm(1, studentAssigned);
			
			group.addTermToSumAllAssignedStudents(cplex, studentAssigned);
			
			groupAssignments.put(group, studentAssigned);
		}
		
		cplex.addLe(sumAllAssignmentsPerStudentPerCourse, 1); // CONSTRAINT: a student can be assigned to at most 1 group per course
		
		student.getCourseGroupAssignments().put(course, groupAssignments);
		
		return sumAllAssignmentsPerStudentPerCourse;
	}
	
	private void processStudentPreferences(Student student, StudentPreference preference, IloLinearNumExpr weightedSumFulfilledPreferences) throws IloException {
		IloLinearIntExpr sumIndividualGroupAssignments = cplex.linearIntExpr();
		
		for (Map.Entry<Course, Group> preferenceCourseGroup : preference.getCourseGroupPairs().entrySet()) { // Get the course-group pair
			Course preferenceCourse = preferenceCourseGroup.getKey();
			Group preferenceGroup = preferenceCourseGroup.getValue();
			
			IloIntVar groupAssignment = student.getCourseGroupAssignments().get(preferenceCourse).get(preferenceGroup);
			sumIndividualGroupAssignments.addTerm(1, groupAssignment);
		}
		
		IloIntVar fulfilledPreference = cplex.boolVar("(Complete preference order " + preference.getOrder() + " for " + student.getCode() + ")");
		preference.setWasFulfilled(fulfilledPreference);
		weightedSumFulfilledPreferences.addTerm(student.getAvgGrade() - (preference.getOrder() - 1) / 9., fulfilledPreference);
		
		// CONSTRAINT: if sum of all group assignments in this preference < number of course-group pairs in it, then it's not completely fulfilled
		cplex.add(cplex.ifThen(cplex.le(sumIndividualGroupAssignments, preference.getSize() - 1), cplex.eq(fulfilledPreference, 0)));
	}
	
	private void processStudentTimeslots(Student student, Timeslot timeslot, IloLinearIntExpr sumAllOccupiedTimeslots) throws IloException {
		IloIntVar timeslotOccupied = cplex.boolVar(); // VARIABLE: student has this timeslot occupied?
		IloLinearIntExpr sumAllPracticalClasses = cplex.linearIntExpr(); // Sum of all practical classes for this student in this timeslot
		IloLinearIntExpr sumAllClasses = cplex.linearIntExpr(); // Sum of all classes for this student in this timeslot
		
		sumAllOccupiedTimeslots.addTerm(1, timeslotOccupied);
		
		for (Map.Entry<Course, Set<Group>> practicalClass : timeslot.getPracticalClasses().entrySet()) {
			Course course = practicalClass.getKey();
			
			if (student.getEnrolledCourses().contains(course)) {
				for (Group group : practicalClass.getValue()) {
					IloIntVar assignmentVariable = student.getCourseGroupAssignments().get(course).get(group);
					sumAllPracticalClasses.addTerm(1, assignmentVariable);
				}
			}
		}
		
		cplex.addLe(sumAllPracticalClasses, 1); // CONSTRAINT: a student can have at most 1 concurrent practical class
		sumAllClasses.add(sumAllPracticalClasses);
		
		for (Map.Entry<Course, Set<Group>> lectureClass : timeslot.getLectureClasses().entrySet()) {
			Course course = lectureClass.getKey();
			
			if (student.getEnrolledCourses().contains(course)) {
				for (Group group : lectureClass.getValue()) {
					IloIntVar assignmentVariable = student.getCourseGroupAssignments().get(course).get(group);
					if (assignmentVariable != null) sumAllClasses.addTerm(1, assignmentVariable); // Some groups might have been automatically added to this timeslot since they're part of a composite but in reality this course doesn't have them
				}
			}
		}
		
		cplex.add(cplex.ifThen(cplex.eq(sumAllClasses, 0), cplex.eq(timeslotOccupied, 0))); // CONSTRAINT: if sum of all classes in this timeslot = 0, the timeslot isn't occupied
	}
	
	private void solve() throws IOException, IloException {
		// Solve the problem
		if (cplex.solve()) {
			System.out.println("Solution found by CPLEX is " + cplex.getStatus() + ".");
			writer.writeOutputData();
		}
		else {
			System.out.println("Failed to solve problem.");
		}
		
		// Free CPLEX resources
		cplex.end();
	}
}
