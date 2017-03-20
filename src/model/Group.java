package model;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.cplex.IloCplex;

public class Group {
	private String code;
	private int capacity;
	private IloLinearIntExpr constr_sumAssignedStudents; // Sum of all decision variables indicating whether a student has been assigned to this group
	//private int[][] schedule;
	
	public Group(String code, int capacity) {
		this.code = code;
		this.capacity = capacity;
		//this.schedule = new int[6][25];
	}
	
	public String getCode() {
		return code;
	}
	
	public int getCapacity() {
		return capacity;
	}
	
	public IloLinearIntExpr getConstrSumAssignedStudents() {
		return constr_sumAssignedStudents;
	}
	
	public void addTermToConstrSumAssignedStudents(IloCplex cplex, IloIntVar var_preferenceAssigned) throws IloException {
		if (constr_sumAssignedStudents == null) constr_sumAssignedStudents = cplex.linearIntExpr();
		constr_sumAssignedStudents.addTerm(1, var_preferenceAssigned);
	}
	
	/*public int[][] getSchedule() {
		return schedule;
	}*/
	
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
