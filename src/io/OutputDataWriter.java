package io;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

import ilog.concert.IloException;
import ilog.concert.IloLinearIntExpr;
import ilog.cplex.IloCplex;
import model.Student;
import model.StudentGroup;
import model.StudentPreference;

public class OutputDataWriter {
	private IloCplex cplex;
	private List<StudentGroup> groups;
	private List<Student> students;
	
	public OutputDataWriter(IloCplex cplex, List<StudentGroup> groups, List<Student> students) {
		this.cplex = cplex;
		this.groups = groups;
		this.students = students;
	}
	
	public void writeOutputData(String outputPath) throws IloException, IOException {
		writeStudentsAssignments(outputPath);
		writeGroupStats(outputPath);
	}
	
	private void writeStudentsAssignments(String outputPath) throws IloException, IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath + "colocações.csv"), "utf-8"));
		writer.write("ESTUD_NUM_UNICO_INST;NOME;OPCAO;CODIGO;SIGLA");
		
		int numAssignedStudents = 0;
		
		for (Student student : students) {
			for (StudentPreference preference : student.getPreferences()) {
				if (cplex.getValue(preference.getVarPreferenceAssigned()) == 1) {
					++numAssignedStudents;
					Map<String, StudentGroup> courseGroupPairs = preference.getCourseGroupPairs();
					
					for (StudentGroup assignedGroup : courseGroupPairs.values()) {
						writer.newLine();
						writer.write(student.getCode() + ";" + student.getName() + ";" + preference.getOrder() + ";" + assignedGroup.getCourseCode() + ";" + assignedGroup.getGroupCode());
					}
				}
			}
		}
		
		writer.close();
		
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath + "estatísticas.txt"), "utf-8"));
		writer.write("Assigned students: " + numAssignedStudents);
		writer.newLine();
		writer.write("Total students: " + students.size());
		writer.newLine();
		writer.write("Assignment rate: " + (float) (numAssignedStudents) / students.size() * 100 + "%");
		writer.close();
	}
	
	private void writeGroupStats(String outputPath) throws IloException, IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath + "turmas.csv"), "utf-8"));
		writer.write("CODIGO;SIGLA;COLOCADOS;CAPACIDADE");
		
		for (StudentGroup group : groups) {
			IloLinearIntExpr numStudentsAssigned = group.getConstrSumAssignedStudents();
			
			writer.newLine();
			writer.write(group.getCourseCode() + ";" + group.getGroupCode() + ";" + (numStudentsAssigned != null ? (int) cplex.getValue(numStudentsAssigned) : 0) + ";" + group.getCapacity());
		}
		
		writer.close();
	}
}
