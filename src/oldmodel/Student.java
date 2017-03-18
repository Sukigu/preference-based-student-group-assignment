package oldmodel;

import java.util.ArrayList;
import java.util.List;

import model.StudentPreference;

public class Student {
	private String code;
	private String name;
	private List<StudentPreference> preferences;
	
	public Student(String code, String name) {
		this.code = code;
		this.name = name;
		this.preferences = new ArrayList<StudentPreference>();
	}
	
	public String getCode() {
		return code;
	}
	
	public String getName() {
		return name;
	}
	
	public List<StudentPreference> getPreferences() {
		return preferences;
	}
	
	public void addPreference(StudentPreference preference) {
		preferences.add(preference);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Student) {
			Student otherStudent = (Student) obj;
			if (otherStudent.getCode().equals(code)) return true;
			else return false;
		}
		else return false;
	}
	
	@Override
	public int hashCode() {
		return code.hashCode();
	}
}
