package model;

import java.util.HashMap;
import java.util.Map;

public class Course {
	private String code;
	private boolean mandatory; // True if mandatory; false if optional
	private int weeklyTimeslots;
	private Map<String, Group> groups;
	private int numEnrollments;
	
	public Course(String code, boolean mandatory, int weeklyTimeslots) {
		this.code = code;
		this.mandatory = mandatory;
		this.weeklyTimeslots = weeklyTimeslots;
		this.groups = new HashMap<>();
		this.numEnrollments = 0;
	}
	
	public String getCode() {
		return code;
	}
	
	public boolean getMandatory() {
		return mandatory;
	}
	
	public int getWeeklyTimeslots() {
		return weeklyTimeslots;
	}
	
	public Map<String, Group> getGroups() {
		return groups;
	}
	
	public int getNumEnrollments() {
		return numEnrollments;
	}
	
	public void incNumEnrollments() {
		numEnrollments += 1;
	}
	
	public int calculateSumGroupCapacities() {
		int sumGroupCapacities = 0;
		
		for (Group group : groups.values()) {
			sumGroupCapacities += group.getCapacity();
		}
		
		return sumGroupCapacities;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Course) {
			return ((Course) obj).code.equals(code);
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
