package model;

public class Group {
	private String code;
	private int capacity;
	
	public Group(String code, int capacity) {
		this.code = code;
		this.capacity = capacity;
	}
	
	public String getCode() {
		return code;
	}
	
	public int getCapacity() {
		return capacity;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Group) {
			Group otherGroup = (Group) obj;
			if (otherGroup.code.equals(code)) return true;
			else return false;
		}
		else return false;
	}
	
	@Override
	public int hashCode() {
		return code.hashCode();
	}
}
