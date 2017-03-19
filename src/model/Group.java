package model;

public class Group {
	private String code;
	private int capacity;
	private int[][] schedule;
	
	public Group(String code, int capacity) {
		this.code = code;
		this.capacity = capacity;
		this.schedule = new int[6][25];
	}
	
	public String getCode() {
		return code;
	}
	
	public int getCapacity() {
		return capacity;
	}
	
	public int[][] getSchedule() {
		return schedule;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof String) {
			return ((String) obj).equals(code);
		}
		else if (obj instanceof Group) {
			return ((Group) obj).code.equals(code);
		}
		else return false;
	}
	
	@Override
	public int hashCode() {
		return code.hashCode();
	}
}
