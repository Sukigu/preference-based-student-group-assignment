package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ilog.concert.IloIntVar;

public class Student {
	private String code;
	private String name;
	private float avgGrade;
	private List<StudentPreference> preferences;
	private Set<Course> enrolledCourses; // List of the mandatory courses this student enrolled in
	
	private Map<Course, Map<Group, IloIntVar>> courseGroupAssignments; // Course code -> (group code -> (boolean variable indicating assignment))
	private IloIntVar hasCompleteAssignment; // Boolean variable indicating if this student was assigned to all courses they enrolled in
	private List<IloIntVar> occupiedPeriods; // Boolean variables indicating if this student was assigned to classes in each time period (mornings/afternoons)
	
	public Student(String code, String name) {
		this.code = code;
		this.name = name;
		this.avgGrade = -1;
		this.preferences = new ArrayList<>();
		this.enrolledCourses = new HashSet<>();
		this.courseGroupAssignments = new HashMap<>();
		this.occupiedPeriods = new ArrayList<>();
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
	
	public void setPreferences(List<StudentPreference> preferences) {
		this.preferences = preferences;
	}
	
	public Set<Course> getEnrolledCourses() {
		return enrolledCourses;
	}
	
	public Map<Course, Map<Group, IloIntVar>> getCourseGroupAssignments() {
		return courseGroupAssignments;
	}
	
	public IloIntVar getHasCompleteAssignment() {
		return hasCompleteAssignment;
	}
	
	public void setHasCompleteAssignment(IloIntVar hasCompleteAssignment) {
		this.hasCompleteAssignment = hasCompleteAssignment;
	}
	
	public List<IloIntVar> getOccupiedPeriods() {
		return occupiedPeriods;
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
