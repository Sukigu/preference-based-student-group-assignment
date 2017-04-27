package model;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.cplex.IloCplex;

public class Group {
	private String code;
	private int capacity;
	private float minUtilization; // Minimum percentage of students assigned to this group, relative to the theoretical number of students who should be assigned to it
	private IloLinearIntExpr sumAllAssignedStudents; // Sum of all decision variables indicating whether a student has been assigned to this group
	
	public Group(String code, int capacity, float minUtilization) {
		this.code = code;
		this.capacity = capacity;
		this.minUtilization = minUtilization;
	}
	
	public String getCode() {
		return code;
	}
	
	public int getCapacity() {
		return capacity;
	}
	
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}
	
	public float getMinUtilization() {
		return minUtilization;
	}
	
	public IloLinearIntExpr getSumAllAssignedStudents() {
		return sumAllAssignedStudents;
	}
	
	public void addTermToSumAllAssignedStudents(IloCplex cplex, IloIntVar var_preferenceAssigned) throws IloException {
		if (sumAllAssignedStudents == null) sumAllAssignedStudents = cplex.linearIntExpr();
		sumAllAssignedStudents.addTerm(1, var_preferenceAssigned);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Group) {
			return ((Group) obj).code.equals(code);
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
