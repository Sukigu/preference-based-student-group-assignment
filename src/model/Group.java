package model;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.cplex.IloCplex;

public class Group {
	private String code;
	private int capacity;
	private IloLinearIntExpr constr_sumAssignedStudents; // Sum of all decision variables indicating whether a student has been assigned to this group
	
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
	
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}
	
	public IloLinearIntExpr getConstrSumAssignedStudents() {
		return constr_sumAssignedStudents;
	}
	
	public void addTermToConstrSumAssignedStudents(IloCplex cplex, IloIntVar var_preferenceAssigned) throws IloException {
		if (constr_sumAssignedStudents == null) constr_sumAssignedStudents = cplex.linearIntExpr();
		constr_sumAssignedStudents.addTerm(1, var_preferenceAssigned);
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
