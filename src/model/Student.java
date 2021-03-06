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
	private List<Boolean> wantedPeriods; // True if student selected period of index N in one of their preferences, false otherwise 
	private Map<Course, Set<Group>> wantedCourseGroups; // List of the course-group pairs this student selected over all their preferences
	
	private Map<Course, Map<Group, IloIntVar>> courseGroupAssignments; // Course code -> (group code -> (boolean variable indicating assignment))
	private IloIntVar hasCompleteAssignment; // Boolean variable indicating if this student was assigned to all courses they enrolled in
	
	public Student(String code, String name) {
		this.code = code;
		this.name = name;
		this.avgGrade = -1;
		this.preferences = new ArrayList<>();
		this.enrolledCourses = new HashSet<>();
		this.wantedPeriods = new ArrayList<>();
		this.wantedCourseGroups = new HashMap<>();
		this.courseGroupAssignments = new HashMap<>();
		
		for (int i = 0; i < 12; ++i) {
			this.wantedPeriods.add(false);
		}
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
		
		// Add all course-group pairs from all preferences to the set of wanted course-group pairs
		for (StudentPreference preference : preferences) {
			for (Course course : preference.getCourseGroupPairs().keySet()) {
				Group group = preference.getCourseGroupPairs().get(course);
				
				wantedCourseGroups.putIfAbsent(course, new HashSet<>());
				wantedCourseGroups.get(course).add(group);
			}
		}
	}
	
	public Set<Course> getEnrolledCourses() {
		return enrolledCourses;
	}
	
	public boolean getWantedPeriod(int period) {
		return wantedPeriods.get(period);
	}
	
	public void setWantedPeriodsTrue(Set<Integer> periods) {
		for (int period : periods) {
			wantedPeriods.set(period, true);
		}
	}
	
	public boolean getWantedCourseGroup(Course course, Group group) {
		try {
			return wantedCourseGroups.get(course).contains(group);
		}
		catch (NullPointerException e) {
			return false;
		}
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
