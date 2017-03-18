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
		preferences = new HashMap<>();
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
		if (obj instanceof Student) {
			Student otherStudent = (Student) obj;
			if (otherStudent.code.equals(code)) return true;
			else return false;
		}
		else return false;
	}
	
	@Override
	public int hashCode() {
		return code.hashCode();
	}
}
