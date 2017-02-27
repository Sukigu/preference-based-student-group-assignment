package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

import ilog.concert.IloException;
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
	
	public void writeOutputData() throws IloException, IOException {
		System.out.println();
		writeStudentsAssignments();
		writeGroupStats();
	}
	
	public void writeStudentsAssignments() throws IloException, IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("res" + File.separator + "output" + File.separator + "colocações.csv"), "utf-8"));
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
		
		System.out.println("Assigned students: " + numAssignedStudents);
	}
	
	public void writeGroupStats() throws IloException, IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("res" + File.separator + "output" + File.separator + "turmas.csv"), "utf-8"));
		writer.write("CODIGO;SIGLA;COLOCADOS;CAPACIDADE");
		
		for (StudentGroup group : groups) {
			writer.newLine();
			writer.write(group.getCourseCode() + ";" + group.getGroupCode() + ";" + (int) cplex.getValue(group.getConstrSumAssignedStudents()) + ";" + group.getCapacity());
		}
		
		writer.close();
	}
}
