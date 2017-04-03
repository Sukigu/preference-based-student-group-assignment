package model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ilog.concert.IloIntVar;

public class Student {
	private String code;
	private String name;
	private float avgGrade;
	private List<StudentPreference> preferences; // Order -> preference
	private Set<Course> enrolledCourses; // List of the mandatory courses this student enrolled in
	private int targetSumOfTimeslots; // If this student were to be assigned to all courses they enrolled in, they'd be expected to occupy this number of weekly timeslots
	private Map<Course, Map<Group, IloIntVar>> courseGroupAssignments; // Course code -> (group code -> (boolean variable indicating assignment))
	private IloIntVar hasCompleteAssignment; // Boolean variable indicating if this student was assigned to all courses they enrolled in
	
	public Student(String code) {
		this.code = code;
		this.avgGrade = -1;
		this.preferences = new ArrayList<>();
		this.enrolledCourses = new HashSet<>();
		this.targetSumOfTimeslots = 0;
	}
	
	public String getCode() {
		return code;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public float getAvgGrade() {
		return avgGrade;
	}
	
	public void setAvgGrade(float avgGrade) {
		this.avgGrade = avgGrade;
	}
	
	public List<StudentPreference> getPreferences() {
		return preferences;
	}
	
	public Set<Course> getEnrolledCourses() {
		return enrolledCourses;
	}
	
	public int getTargetSumOfTimeslots() {
		return targetSumOfTimeslots;
	}
	
	public void addToTargetSumOfTimeslots(int delta) {
		this.targetSumOfTimeslots += delta;
	}
	
	public Map<Course, Map<Group, IloIntVar>> getCourseGroupAssignments() {
		return courseGroupAssignments;
	}
	
	public void setCourseGroupAssignments(Map<Course, Map<Group, IloIntVar>> courseGroupAssignments) {
		this.courseGroupAssignments = courseGroupAssignments;
	}
	
	public IloIntVar getHasCompleteAssignment() {
		return hasCompleteAssignment;
	}
	
	public void setHasCompleteAssignment(IloIntVar hasCompleteAssignment) {
		this.hasCompleteAssignment = hasCompleteAssignment;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Student) {
			return ((Student) obj).code.equals(code);
		}
		else return false;
	}
	
	@Override
	public int hashCode() {
		return code.hashCode();
	}
	
	@Override
	public String toString() {
		return code;
	}
}
