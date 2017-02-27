package model;

import java.util.HashMap;
import java.util.Map;

import ilog.concert.IloIntVar;

public class StudentPreference {
	private int order;
	private Map<String, StudentGroup> courseGroupPairs;
	private IloIntVar var_preferenceAssigned; // Decision variable to indicate if this preference has been assigned to the student
	private int weight; // Importance of this specific student preference regarding all other preferences
	
	public StudentPreference(int order) {
		this.order = order;
		this.courseGroupPairs = new HashMap<String, StudentGroup>();
		this.weight = 1;
	}
	
	public int getOrder() {
		return order;
	}
	
	public Map<String, StudentGroup> getCourseGroupPairs() {
		return courseGroupPairs;
	}
	
	public void addCourseGroupPair(String course, StudentGroup group) {
		courseGroupPairs.put(course, group);
	}
	
	public IloIntVar getVarPreferenceAssigned() {
		return var_preferenceAssigned;
	}
	
	public void setVarPreferenceAssigned(IloIntVar var_preferenceAssigned) {
		this.var_preferenceAssigned = var_preferenceAssigned;
	}
	
	public int getWeight() {
		return weight;
	}
	
	public void setWeight(int weight) {
		this.weight = weight;
	}
}
