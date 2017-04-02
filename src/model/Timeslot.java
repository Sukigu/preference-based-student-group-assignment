package model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Timeslot {
	private Map<Course, Set<Group>> lectureClasses;
	private Map<Course, Set<Group>> practicalClasses;
	
	public Timeslot() {
		this.lectureClasses = new HashMap<>();
		this.practicalClasses = new HashMap<>();
	}
	
	public Map<Course, Set<Group>> getLectureClasses() {
		return lectureClasses;
	}
	
	public void addLectureClass(Course course, Group group) {
		Set<Group> courseLectureClasses = lectureClasses.get(course);
		if (courseLectureClasses == null) courseLectureClasses = new HashSet<>();
		courseLectureClasses.add(group);
		
		lectureClasses.put(course, courseLectureClasses);
	}
	
	public Map<Course, Set<Group>> getPracticalClasses() {
		return practicalClasses;
	}
	
	public void addPracticalClass(Course course, Group group) {
		Set<Group> coursePracticalClasses = practicalClasses.get(course);
		if (coursePracticalClasses == null) coursePracticalClasses = new HashSet<>();
		coursePracticalClasses.add(group);
		
		practicalClasses.put(course, coursePracticalClasses);
	}
}
