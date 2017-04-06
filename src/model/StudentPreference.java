package model;

import java.util.HashMap;
import java.util.Map;

import ilog.concert.IloLinearIntExpr;

public class StudentPreference {
	private int order;
	private int size;
	private Map<Course, Group> courseGroupPairs;
	private IloLinearIntExpr sumIndividualGroupAssignments;
	
	public StudentPreference(int order) {
		this.order = order;
		this.size = 0;
		this.courseGroupPairs = new HashMap<>();
	}
	
	public int getOrder() {
		return order;
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
	
	public IloLinearIntExpr getSumIndividualGroupAssignments() {
		return sumIndividualGroupAssignments;
	}
	
	public void setSumIndividualGroupAssignments(IloLinearIntExpr sumIndividualGroupAssignments) {
		this.sumIndividualGroupAssignments = sumIndividualGroupAssignments;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StudentPreference) {
			return ((StudentPreference) obj).order == order;
		}
		else return false;
	}
}
