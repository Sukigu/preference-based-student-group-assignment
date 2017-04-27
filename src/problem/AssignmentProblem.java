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
import ilog.concert.IloNumVar;
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
	private IloLinearNumExpr weightedSumAllAssignments, weightedSumAllCompleteStudents, weightedSumFulfilledPreferences, sumAllGroupUtilizationSlacks;
	private IloLinearIntExpr sumAllOccupiedTimeslots;
	
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
		weightedSumAllAssignments = cplex.linearNumExpr(); // Summation of each student's assignments multiplied by their grade
		weightedSumAllCompleteStudents = cplex.linearNumExpr(); // Summation of all variables indicating a student assigned to all of their courses multiplied by their grade
		weightedSumFulfilledPreferences = cplex.linearNumExpr(); // Summation of all variables indicating a student preference fulfilled multiplied by their grade
		sumAllOccupiedTimeslots = cplex.linearIntExpr(); // Sum of all timeslots occupied individually by all students
		sumAllGroupUtilizationSlacks = cplex.linearNumExpr(); // Sum of all group utilization slack variables for the group balance soft constraint
		
		float sumEnrollmentsTimesAvgGrade = 0; // Summation of each student's number of course enrollments multiplied by their grade
		float sumAvgGrades = 0; // Sum of every student's grade
		
		for (Student student : students.values()) {
			processStudent(student);
			
			sumEnrollmentsTimesAvgGrade += student.getEnrolledCourses().size() * student.getAvgGrade();
			sumAvgGrades += student.getAvgGrade();
		}
		
		float sumTargetNumStudentsAssigned = 0;
		
		if (isMandatoryAssignment) {
			for (Course course : courses.values()) {
				sumTargetNumStudentsAssigned += processCourse(course);
			}
		}
		else {
			for (Course course : courses.values()) {
				if (!course.getMandatory()) {
					processCourseOptional(course);
				}
			}
		}
		
		IloNumExpr objMaximizeSumAllAssignments = (sumEnrollmentsTimesAvgGrade != 0) ? cplex.prod(1. / sumEnrollmentsTimesAvgGrade, weightedSumAllAssignments) : cplex.constant(1);
		IloNumExpr objMaximizeCompleteStudents = (sumAvgGrades != 0) ? cplex.prod(1. / sumAvgGrades, weightedSumAllCompleteStudents) : cplex.constant(1);
		IloNumExpr objMaximizeOccupiedTimeslots = (targetNumOccupiedTimeslots != 0) ? cplex.prod(1. / targetNumOccupiedTimeslots, sumAllOccupiedTimeslots) : cplex.constant(1);
		IloNumExpr objMaximizeFulfilledPreferences = (sumAvgGrades != 0) ? cplex.prod(1. / (sumAvgGrades * 10), weightedSumFulfilledPreferences) : cplex.constant(1);
		IloNumExpr objMinimizeGroupUtilizationSlacks = (sumTargetNumStudentsAssigned != 0) ? cplex.sum(1, cplex.prod(-1. / sumTargetNumStudentsAssigned, sumAllGroupUtilizationSlacks)) : cplex.constant(1);
		
		if (isMandatoryAssignment) {
			cplex.addMaximize(cplex.sum(cplex.prod(.175, objMaximizeSumAllAssignments), cplex.prod(.175, objMaximizeCompleteStudents), cplex.prod(.175, objMaximizeOccupiedTimeslots), cplex.prod(.3, objMaximizeFulfilledPreferences),  cplex.prod(.175, objMinimizeGroupUtilizationSlacks)));
		}
		else {
			cplex.addMaximize(objMaximizeFulfilledPreferences);
		}
		
		// TODO: DEBUG
		System.out.println("sumEnrollmentsTimesAvgGrade = " + sumEnrollmentsTimesAvgGrade);
		System.out.println("sumAvgGrades = " + sumAvgGrades);
		System.out.println("targetNumOccupiedTimeslots = " + targetNumOccupiedTimeslots);
		System.out.println("sumAvgGrades * 10 = " + sumAvgGrades * 10);
		System.out.println("sumTargetNumStudentsAssignedToGroups = " + sumTargetNumStudentsAssigned);
		System.out.println();
	}
	
	private void processStudent(Student student) throws IloException {
		float avgGrade = student.getAvgGrade();
		IloLinearIntExpr sumAllAssignmentsPerStudent = processAssignmentsPerStudent(student); // Sum of all assignments for this student
		
		IloLinearIntExprIterator studentAssignmentsIterator = sumAllAssignmentsPerStudent.linearIterator();
		while (studentAssignmentsIterator.hasNext()) { // Iterating over this student's assignment variables to add them to the objective function multiplied by their grade
			IloIntVar studentAssignment = studentAssignmentsIterator.nextIntVar();
			weightedSumAllAssignments.addTerm(avgGrade, studentAssignment);
		}
		
		IloIntVar completeStudent = cplex.boolVar("(Complete assignment for " + student.getCode() + ")"); // VARIABLE: student was assigned to all of their courses?
		cplex.add(cplex.ifThen(cplex.le(sumAllAssignmentsPerStudent, student.getEnrolledCourses().size() - 1), cplex.eq(completeStudent, 0))); // CONSTRAINT: if sum of all assignments < number of enrolled courses, then it's not a complete assignment
		cplex.add(cplex.ifThen(cplex.eq(sumAllAssignmentsPerStudent, student.getEnrolledCourses().size()), cplex.eq(completeStudent, 1))); // CONSTRAINT: if sum of all assignments = number of enrolled courses, then it is a complete assignment
		
		weightedSumAllCompleteStudents.addTerm(avgGrade, completeStudent);
		student.setHasCompleteAssignment(completeStudent); // Set this student's complete status variable
		
		for (StudentPreference preference : student.getPreferences()) {
			processStudentPreferences(student, preference, sumAllAssignmentsPerStudent);
		}
		
		for (Timeslot timeslot : schedule) {
			processStudentTimeslots(student, timeslot);
		}
	}
	
	private IloLinearIntExpr processAssignmentsPerStudent(Student student) throws IloException {
		IloLinearIntExpr sumAllAssignmentsPerStudent = cplex.linearIntExpr();
		
		for (Course course : student.getEnrolledCourses()) {
			targetNumOccupiedTimeslots += course.getWeeklyTimeslots();
			
			IloLinearIntExpr sumAllAssignmentsPerStudentPerCourse = processAssignmentsPerStudentPerCourse(student, course); // Sum of all assignments for this student and this course
			sumAllAssignmentsPerStudent.add(sumAllAssignmentsPerStudentPerCourse);
		}
		
		return sumAllAssignmentsPerStudent;
	}
	
	private IloLinearIntExpr processAssignmentsPerStudentPerCourse(Student student, Course course) throws IloException {
		IloLinearIntExpr sumAllAssignmentsPerStudentPerCourse = cplex.linearIntExpr();
		
		Map<Group, IloIntVar> groupAssignments = new HashMap<>();
		
		for (Group group : course.getGroups().values()) {
			IloIntVar studentGroupAssignment = processAssignmentsPerStudentPerCoursePerGroup(student, course, group);
			sumAllAssignmentsPerStudentPerCourse.addTerm(1, studentGroupAssignment);
			
			groupAssignments.put(group, studentGroupAssignment);
		}
		
		cplex.addLe(sumAllAssignmentsPerStudentPerCourse, 1); // CONSTRAINT: a student can be assigned to at most 1 group per course
		
		student.getCourseGroupAssignments().put(course, groupAssignments);
		
		return sumAllAssignmentsPerStudentPerCourse;
	}
	
	private IloIntVar processAssignmentsPerStudentPerCoursePerGroup(Student student, Course course, Group group) throws IloException {
		IloIntVar studentGroupAssignment = cplex.boolVar("(" + student.getCode() + ": " + course.getCode() + "-" + group.getCode() + ")"); // VARIABLE: student assigned to this course-group pair?
		
		group.addTermToSumAllAssignedStudents(cplex, studentGroupAssignment);
		
		return studentGroupAssignment;
	}
	
	private void processStudentPreferences(Student student, StudentPreference preference, IloLinearIntExpr sumAllAssignmentsPerStudent) throws IloException {
		int preferenceOrder = preference.getOrder();
		int preferenceSize = preference.getSize();
		IloLinearIntExpr sumIndividualGroupAssignments = cplex.linearIntExpr();
		
		for (Map.Entry<Course, Group> preferenceCourseGroup : preference.getCourseGroupPairs().entrySet()) { // Get the course-group pair
			Course preferenceCourse = preferenceCourseGroup.getKey();
			Group preferenceGroup = preferenceCourseGroup.getValue();
			
			IloIntVar groupAssignment = student.getCourseGroupAssignments().get(preferenceCourse).get(preferenceGroup);
			sumIndividualGroupAssignments.addTerm(1, groupAssignment);
		}
		
		IloIntVar fulfilledPreference = cplex.boolVar("(Complete preference order " + preferenceOrder + " for " + student.getCode() + ")");
		preference.setWasFulfilled(fulfilledPreference);
		/*weightedSumFulfilledPreferences.addTerm(student.getAvgGrade() - (preferenceOrder - 1) / 9., fulfilledPreference);*/
		weightedSumFulfilledPreferences.addTerm(student.getAvgGrade() * (10 - (preferenceOrder - 1)), fulfilledPreference);
		
		// CONSTRAINT: if sum of all assignments in this preference < number of course-group pairs in it
		// or sum of all assignments in this preference < sum of the student's total assignments,
		// then it's not completely fulfilled
		cplex.add(cplex.ifThen(cplex.or(
				cplex.le(sumIndividualGroupAssignments, preferenceSize - 1),
				cplex.le(sumIndividualGroupAssignments, cplex.sum(sumAllAssignmentsPerStudent, -1))),
				cplex.eq(fulfilledPreference, 0)));
		
		/*// CONSTRAINT: if sum of all assignments in this preference = number of course-group pairs in it
		// and sum of all assignments in this preference = sum of the student's total assignments,
		// then it is completely fulfilled
		cplex.add(cplex.ifThen(cplex.and(
				cplex.eq(sumIndividualGroupAssignments, preferenceSize),
				cplex.eq(sumIndividualGroupAssignments, sumAllAssignmentsPerStudent)),
				cplex.eq(fulfilledPreference, 1)));*/
	}
	
	private void processStudentTimeslots(Student student, Timeslot timeslot) throws IloException {
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
					sumAllClasses.addTerm(1, assignmentVariable);
				}
			}
		}
		
		cplex.add(cplex.ifThen(cplex.eq(sumAllClasses, 0), cplex.eq(timeslotOccupied, 0))); // CONSTRAINT: if sum of all classes in this timeslot = 0, the timeslot isn't occupied
		cplex.add(cplex.ifThen(cplex.not(cplex.eq(sumAllClasses, 0)), cplex.eq(timeslotOccupied, 1))); // CONSTRAINT: if sum of all classes in this timeslot != 0, the timeslot is occupied
	}
	
	private float processCourse(Course course) throws IloException {
		int numStudentsEnrolledThisCourse = course.getNumEnrollments();
		int sumGroupCapacitiesThisCourse = course.calculateSumGroupCapacities();
		
		float targetNumStudentsAssignedToCourse = 0;
		
		for (Group group : course.getGroups().values()) {
			targetNumStudentsAssignedToCourse += processCourseGroup(course, group, numStudentsEnrolledThisCourse, sumGroupCapacitiesThisCourse);
		}
		
		return targetNumStudentsAssignedToCourse;
	}
	
	private float processCourseGroup(Course course, Group group, int numStudentsEnrolledThisCourse, int sumGroupCapacitiesThisCourse) throws IloException {
		IloLinearIntExpr sumAllAssignedStudents = group.getSumAllAssignedStudents();
		int groupCapacity = group.getCapacity();
		
		if (sumAllAssignedStudents == null) return 0; // Some courses might not have enrolled students
		
		if (!isMandatoryAssignment || course.getMandatory()) {
			cplex.addLe(sumAllAssignedStudents, groupCapacity); // CONSTRAINT: sum of all assigned students <= group's capacity
		}
		// Else (if we're assigning mandatory courses but this course is optional), don't add a constraint for the group capacity, since we know for sure everyone fits
		
		float groupMinUtilization = group.getMinUtilization();
		float targetNumStudentsAssigned = groupMinUtilization * groupCapacity / sumGroupCapacitiesThisCourse * numStudentsEnrolledThisCourse;
		
		IloNumVar groupUtilizationSlack = cplex.numVar(0, targetNumStudentsAssigned);
		sumAllGroupUtilizationSlacks.addTerm(1, groupUtilizationSlack);
		
		// SOFT CONSTRAINT: try to balance students assigned to groups according to each group's target minimum utilization of the total group capacities for the same course
		cplex.addGe(cplex.sum(sumAllAssignedStudents, groupUtilizationSlack), targetNumStudentsAssigned);
		
		//		// CONSTRAINT: balance students assigned to groups according to each group's target minimum utilization of the total group capacities for the same course
		//		cplex.addGe(sumAllAssignedStudents, targetNumStudentsAssigned);
		
		return targetNumStudentsAssigned;
	}
	
	private void processCourseOptional(Course course) throws IloException {
		for (Group group : course.getGroups().values()) {
			processCourseGroupOptional(course, group);
		}
	}
	
	private void processCourseGroupOptional(Course course, Group group) throws IloException {
		IloLinearIntExpr sumAllAssignedStudents = group.getSumAllAssignedStudents();
		int groupCapacity = group.getCapacity();
		
		if (sumAllAssignedStudents == null) return; // Some courses might not have enrolled students
		
		cplex.addLe(sumAllAssignedStudents, groupCapacity); // CONSTRAINT: sum of all assigned students <= group's capacity
		
		float groupMinUtilization = .1f;
		
		cplex.addGe(sumAllAssignedStudents, groupMinUtilization * groupCapacity);
	}
	
	private void solve() throws IOException, IloException {
		cplex.setParam(IloCplex.DoubleParam.TiLim, 300 /*1500*/); // Set timeout in seconds
		
		// Solve the problem
		if (cplex.solve()) {
			System.out.println();
			System.out.println("Solution found by CPLEX is " + cplex.getStatus() + ".");
			
			// TODO: DEBUG
			System.out.println();
			System.out.println("weightedSumAllAssignments = " + cplex.getValue(weightedSumAllAssignments));
			System.out.println("weightedSumAllCompleteStudents = " + cplex.getValue(weightedSumAllCompleteStudents));
			System.out.println("sumAllOccupiedTimeslots = " + cplex.getValue(sumAllOccupiedTimeslots));
			System.out.println("weightedSumFulfilledPreferences = " + cplex.getValue(weightedSumFulfilledPreferences));
			System.out.println("sumAllGroupUtilizationSlacks = " + cplex.getValue(sumAllGroupUtilizationSlacks));
			
			writer.writeOutputData();
		}
		else {
			System.out.println("Failed to solve problem.");
		}
		
		// Free CPLEX resources
		cplex.end();
	}
}
