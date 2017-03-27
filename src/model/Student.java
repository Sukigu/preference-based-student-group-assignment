package model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ilog.concert.IloIntVar;

public class Student {
	private String code;
	private String name;
	private float avgGrade;
	private Map<Integer, StudentPreference> preferences; // Order -> preference
	private Set<String> enrolledCourses; // List of the (mandatory) courses this student enrolled in
	private Map<String, Map<String, IloIntVar>> courseGroupAssignments; // Course code -> (group code -> (boolean variable indicating assignment))
	private IloIntVar hasCompleteAssignment; // Boolean variable indicating if this student was assigned to all courses they enrolled in
	
	public Student(String code) {
		this.code = code;
		this.avgGrade = -1;
		this.preferences = new HashMap<>();
		this.enrolledCourses = new HashSet<>();
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
	
	public Map<Integer, StudentPreference> getPreferences() {
		return preferences;
	}
	
	public Set<String> getEnrolledCourses() {
		return enrolledCourses;
	}
	
	public Map<String, Map<String, IloIntVar>> getCourseGroupAssignments() {
		return courseGroupAssignments;
	}
	
	public void setCourseGroupAssignments(Map<String, Map<String, IloIntVar>> courseGroupAssignments) {
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
}
