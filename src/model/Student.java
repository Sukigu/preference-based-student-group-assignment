package model;

import java.util.HashMap;
import java.util.Map;

public class Student {
	private String code;
	private String name;
	private float avgGrade;
	private Map<Integer, StudentPreference> preferences;
	
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
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof String) {
			return (String) obj == code;
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
