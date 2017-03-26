package model;

public class Course {
	private String code;
	private boolean mandatory; // True if mandatory; false if optional
	
	public Course(String code, boolean mandatory) {
		this.code = code;
		this.mandatory = mandatory;
	}
	
	public String getCode() {
		return code;
	}
	
	public boolean getMandatory() {
		return mandatory;
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
}
