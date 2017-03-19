package model;

import java.util.HashMap;
import java.util.Map;

import ilog.concert.IloIntVar;

public class Student {
	private String code;
	private String name;
	private float avgGrade;
	private Map<Integer, StudentPreference> preferences;
	private Map<String, Map<String, IloIntVar>> var_assignedToCourseGroup; // Course code -> (group code -> (boolean variable))
																		   // When "manually" allocating students, this indicates which groups this student is assigned to
	public Student(String code) {
		this.code = code;
		this.avgGrade = -1;
		this.preferences = new HashMap<>();
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
	
	public Map<String, Map<String, IloIntVar>> getVarAssignedToCourseGroup() {
		return var_assignedToCourseGroup;
	}
	
	public void setVarAssignedToCourseGroup(Map<String, Map<String, IloIntVar>> var_assignedToCourseGroup) {
		this.var_assignedToCourseGroup = var_assignedToCourseGroup;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof String) {
			return ((String) obj).equals(code);
		}
		else if (obj instanceof Student) {
			return ((Student) obj).code.equals(code);
		}
		else return false;
	}
	
	@Override
	public int hashCode() {
		return code.hashCode();
	}
}
