package model;

public class Course {
	private String code;
	
	public Course(String code) {
		this.code = code;
	}
	
	public String getCode() {
		return code;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Course) {
			Course otherCourse = (Course) obj;
			if (otherCourse.code.equals(code)) return true;
			else return false;
		}
		else return false;
	}
	
	@Override
	public int hashCode() {
		return code.hashCode();
	}
}
