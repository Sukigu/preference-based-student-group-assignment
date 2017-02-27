package model;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.cplex.IloCplex;

public class StudentGroup {
	private String courseCode;
	private String groupCode;
	private int capacity;
	private IloLinearIntExpr constr_sumAssignedStudents; // Sum of all decision variables indicating whether a student has been assigned to this group
	
	public StudentGroup(String courseCode, String groupCode, int capacity) {
		this.courseCode = courseCode;
		this.groupCode = groupCode;
		this.capacity = capacity;
	}
	
	public String getCourseCode() {
		return courseCode;
	}
	
	public String getGroupCode() {
		return groupCode;
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
}
