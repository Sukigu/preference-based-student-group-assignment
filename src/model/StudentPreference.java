package model;

import java.util.HashMap;
import java.util.Map;

import ilog.concert.IloIntVar;

public class StudentPreference {
	private int order;
	private int size;
	private Map<Course, Group> courseGroupPairs;
	private IloIntVar wasFulfilled; // Boolean variable indicating if this preference was fulfilled in its entirety
	
	public StudentPreference(int order) {
		this.order = order;
		this.size = 0;
		this.courseGroupPairs = new HashMap<>();
	}
	
	public int getOrder() {
		return order;
	}
	
	public void setOrder(int order) {
		this.order = order;
	}
	
	public int getSize() {
		return size;
	}
	
	public Map<Course, Group> getCourseGroupPairs() {
		return courseGroupPairs;
	}
	
	public void addCourseGroupPair(Course course, Group group) {
		courseGroupPairs.put(course, group);
		size += 1;
	}
	
	public IloIntVar getWasFulfilled() {
		return wasFulfilled;
	}
	
	public void setWasFulfilled(IloIntVar wasFulfilled) {
		this.wasFulfilled = wasFulfilled;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StudentPreference) {
			StudentPreference otherPreference = (StudentPreference) obj;
			
			return otherPreference.courseGroupPairs.equals(courseGroupPairs);
		}
		else return false;
	}
}
