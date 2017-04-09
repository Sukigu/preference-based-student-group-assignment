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
	private IloCplex cplex;
	private OutputDataWriter writer;
	
	private int numEnrollments;
	private int targetNumOccupiedTimeslots;
	
	public AssignmentProblem(String coursesFilename, String groupsFilename, String scheduleFilename, String groupCompositesFilename, String preferencesFilename, String gradesFilename, int semester, String procVersion, boolean isMandatoryAssignment, String outputPath) throws IloException, IOException {
		InputDataReader reader = new InputDataReader(coursesFilename, groupsFilename, scheduleFilename, groupCompositesFilename, preferencesFilename, gradesFilename, semester, procVersion);
		reader.readData();
		
		this.schedule = reader.getSchedule();
		this.courses = reader.getCourses();
		this.students = reader.getStudents();
		this.isMandatoryAssignment = isMandatoryAssignment;
		this.cplex = new IloCplex();
		this.writer = new OutputDataWriter(cplex, cplex.getParam(IloCplex.DoubleParam.EpRHS), courses, students, outputPath);
		
		this.numEnrollments = 0;
		this.targetNumOccupiedTimeslots = 0;
	}
	
	public void run() throws IloException, IOException {
		writer.checkGroupCapacities();
		defineManualAssignmentProblem();
		solve();
	}
	
	private void defineManualAssignmentProblem() throws IloException {
		IloLinearIntExpr sumAllAssignments = cplex.linearIntExpr(); // Sum of all assignments of all students
		IloLinearIntExpr sumAllCompleteStudents = cplex.linearIntExpr(); // Number of students who were assigned to all of their courses
		IloLinearIntExpr sumAllOccupiedTimeslots = cplex.linearIntExpr(); // Number of total accumulated timeslots occupied
		
		for (Student student : students.values()) {
			IloLinearIntExpr sumAllAssignmentsPerStudent = assignmentPerStudent(student); // Sum of all assignments for this student
			IloIntVar completeStudent = cplex.boolVar("(Complete assignment for " + student.getCode() + ")"); // VARIABLE: student was assigned to all of their courses? 
			
			// CONSTRAINT: if sum of all assignments < number of enrolled courses, then it's not a complete assignment
			cplex.add(cplex.ifThen(cplex.le(sumAllAssignmentsPerStudent, student.getEnrolledCourses().size() - 1), cplex.eq(completeStudent, 0)));
			
			sumAllAssignments.add(sumAllAssignmentsPerStudent);
			sumAllCompleteStudents.addTerm(1, completeStudent);
			
			student.setHasCompleteAssignment(completeStudent);
			
			for (Timeslot timeslot : schedule) {
				assignmentPerStudentTimeslot(student, timeslot, sumAllOccupiedTimeslots);
			}
		}
		
		for (Course course : courses.values()) {
			for (Group group : course.getGroups().values()) {
				if (!isMandatoryAssignment || course.getMandatory()) {
					//if (course.getCode().equals("EIC0105") || course.getCode().equals("EIC0012")) continue; TODO: DEBUG
					cplex.addLe(group.getSumAllAssignedStudents(), group.getCapacity()); // CONSTRAINT: sum of all assigned students <= group's capacity
				}
				// Else (if we're assigning mandatory courses but this course is optional), don't add a constraint for the group capacity, since we know for sure everyone fits
			}
		}
		
		IloNumExpr objMaximizeSumAllAssignments = cplex.prod(1. / numEnrollments, sumAllAssignments);
		IloNumExpr objMaximizeCompleteStudents = cplex.prod(1. / students.size(), sumAllCompleteStudents);
		IloNumExpr objMaximizeOccupiedTimeslots = cplex.prod(1. / targetNumOccupiedTimeslots, sumAllOccupiedTimeslots);
		/*IloNumExpr objMaximizeFulfilledPreferences = processStudentPreferences();*/
		cplex.addMaximize(cplex.sum(cplex.prod(.1, objMaximizeSumAllAssignments), cplex.prod(.5, objMaximizeCompleteStudents), cplex.prod(.4, objMaximizeOccupiedTimeslots)/*, cplex.prod(.1, objMaximizeFulfilledPreferences)*/));
	}
	
	private IloNumExpr processStudentPreferences() throws IloException {
		IloLinearIntExpr sumFulfilledPreferences = cplex.linearIntExpr();
		
		for (Student student : students.values()) { // For each student...
			for (StudentPreference preference : student.getPreferences()) { // For each of their preferences...
				IloLinearIntExpr sumIndividualGroupAssignments = cplex.linearIntExpr();
				
				for (Map.Entry<Course, Group> preferenceCourseGroup : preference.getCourseGroupPairs().entrySet()) { // Get the course-group pair
					Course preferenceCourse = preferenceCourseGroup.getKey();
					Group preferenceGroup = preferenceCourseGroup.getValue();
					
					IloIntVar groupAssignment = student.getCourseGroupAssignments().get(preferenceCourse).get(preferenceGroup);
					sumIndividualGroupAssignments.addTerm(1, groupAssignment);
				}
				
				IloIntVar fulfilledPreference = cplex.boolVar("(Complete preference order " + preference.getOrder() + " for " + student.getCode() + ")");
				sumFulfilledPreferences.addTerm(1, fulfilledPreference);
				
				// CONSTRAINT: if sum of all group assignments in this preference < number of course-group pairs in it, then it's not completely fulfilled
				cplex.add(cplex.ifThen(cplex.le(sumIndividualGroupAssignments, preference.getSize() - 1), cplex.eq(fulfilledPreference, 0)));
			}
		}
		
		return cplex.prod(1. / students.size(), sumFulfilledPreferences);
	}
	
	private IloLinearIntExpr assignmentPerStudent(Student student) throws IloException {
		IloLinearIntExpr sumAllAssignmentsPerStudent = cplex.linearIntExpr();
		
		for (Course course : student.getEnrolledCourses()) {
			numEnrollments += 1;
			targetNumOccupiedTimeslots += course.getWeeklyTimeslots();
			
			IloLinearIntExpr sumAllAssignmentsPerStudentPerCourse = assignmentPerStudentCourse(student, course); // Sum of all assignments for this student and this course
			sumAllAssignmentsPerStudent.add(sumAllAssignmentsPerStudentPerCourse);
		}
		
		return sumAllAssignmentsPerStudent;
	}
	
	private IloLinearIntExpr assignmentPerStudentCourse(Student student, Course course) throws IloException {
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
	
	private void assignmentPerStudentTimeslot(Student student, Timeslot timeslot, IloLinearIntExpr sumAllOccupiedTimeslots) throws IloException {
		IloIntVar timeslotOccupied = cplex.boolVar(); // VARIABLE: student has this timeslot occupied?
		IloLinearIntExpr sumAllPracticalClasses = cplex.linearIntExpr(); // Sum of all practical classes for this student in this timeslot
		IloLinearIntExpr sumAllClasses = cplex.linearIntExpr(); // Sum of all classes for this student in this timeslot
		
		sumAllOccupiedTimeslots.addTerm(1, timeslotOccupied);
		
		for (Map.Entry<Course, Set<Group>> practicalClass : timeslot.getPracticalClasses().entrySet()) {
			Course course = practicalClass.getKey();
			
			if (student.getEnrolledCourses().contains(course)) {
				for (Group group : practicalClass.getValue()) {
					IloIntVar assignmentVariable = student.getCourseGroupAssignments().get(course).get(group);
					if (assignmentVariable != null) sumAllPracticalClasses.addTerm(1, assignmentVariable); // Some groups might have been automatically added to this timeslot since they're part of a composite but in reality this course doesn't have them
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
