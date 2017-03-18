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
import model.Course;
import model.Group;
import model.Student;
import model.StudentPreference;

public class OutputDataWriter {
	private IloCplex cplex;
	private Map<Course, Map<String, Group>> coursesGroups;
	private Map<String, Student> students;
	
	public OutputDataWriter(IloCplex cplex, Map<Course, Map<String, Group>> coursesGroups, Map<String, Student> students) {
		this.cplex = cplex;
		this.coursesGroups = coursesGroups;
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
		
		for (Map.Entry<String, Student> studentEntry : students.entrySet()) {
			for (Map.Entry<Integer, StudentPreference> preferenceEntry : studentEntry.getValue().getPreferences().entrySet()) {
				if (cplex.getValue(preferenceEntry.getValue().getVarPreferenceAssigned()) == 1) {
					++numAssignedStudents;
					Map<String, Group> courseGroupPairs = preferenceEntry.getValue().getCourseGroupPairs();
					
					for (Map.Entry<String, Group> groupEntry : courseGroupPairs.entrySet()) {
						writer.newLine();
						writer.write(studentEntry.getValue().getCode() + ";" + studentEntry.getValue().getName() + ";" + preferenceEntry.getValue().getOrder() + ";" + groupEntry.getKey() + ";" + groupEntry.getValue().getCode());
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
		
		coursesGroups.entrySet().forEach((entry) -> {
			entry.getValue().forEach((groupCode, group) -> {
				IloLinearIntExpr numStudentsAssigned = group.getConstrSumAssignedStudents();
				
				writer.newLine();
				writer.write(entry.getKey() + ";" + groupCode + ";" + (numStudentsAssigned != null ? (int) cplex.getValue(numStudentsAssigned) : 0) + ";" + group.getCapacity());
			});
		});
		
		writer.close();
	}
}
