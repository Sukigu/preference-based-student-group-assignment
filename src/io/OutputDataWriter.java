package io;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
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
	private String outputPath;
	
	public OutputDataWriter(IloCplex cplex, Map<Course, Map<String, Group>> coursesGroups, Map<String, Student> students, String outputPath) {
		this.cplex = cplex;
		this.coursesGroups = coursesGroups;
		this.students = students;
		this.outputPath = outputPath;
	}
	
	public void writeOutputData() throws IloException, IOException {
		//writeStudentsAssignments();
		writeStudentsManualAssignments();
		writeGroupStats();
	}
	
	private void writeStudentsManualAssignments() throws IloException, IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath + "colocações.csv"), "utf-8"));
		writer.write("ESTUD_NUM_UNICO_INST;NOME;OPCAO;CODIGO;SIGLA");
		
		int courseEnrollments = 0, courseAssignments = 0;
		
		for (Student student : students.values()) {
			for (Map.Entry<String, Map<String, IloIntVar>> courseEntry : student.getCourseGroupAssignments().entrySet()) {
				++courseEnrollments;
				String courseCode = courseEntry.getKey();
				
				for (Map.Entry<String, IloIntVar> groupEntry : courseEntry.getValue().entrySet()) {
					IloIntVar assignmentVar = groupEntry.getValue();
					
					if (cplex.getValue(assignmentVar) == 1) {
						++courseAssignments;
						String groupCode = groupEntry.getKey();
						
						writer.newLine();
						writer.write(student.getCode() + ";" + student.getName() + ";" + "-1" + ";" + courseCode + ";" + groupCode);
					}
				}
			}
		}
		
		writer.close();
		
		writeAssignmentStats(courseEnrollments, courseAssignments);
	}
	
	private void writeStudentsAssignments() throws IloException, IOException {
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
	
	private void writeAssignmentStats(int courseEnrollments, int courseAssignments) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath + "estatísticas.txt"), "utf-8"));
		
		writer.write("Inscrições em UCs: " + courseEnrollments);
		writer.newLine();
		writer.write("Colocações em UCs: " + courseAssignments);
		writer.newLine();
		writer.write("Percentagem de colocações: " + (float) courseAssignments / courseEnrollments * 100 + "%");
		
		writer.close();
	}
	
	private void writeGroupStats() throws IloException, IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath + "turmas.csv"), "utf-8"));
		writer.write("UC;TURMA;COLOCADOS;CAPACIDADE");
		
		for (Map.Entry<Course, Map<String, Group>> coursesGroupsEntry : coursesGroups.entrySet()) {
			String courseCode = coursesGroupsEntry.getKey().getCode();
			
			for (Group group : coursesGroupsEntry.getValue().values()) {
				IloLinearIntExpr numStudentsAssigned = group.getConstrSumAssignedStudents();
				
				writer.newLine();
				writer.write(courseCode + ";" + group.getCode() + ";" + (numStudentsAssigned != null ? (int) cplex.getValue(numStudentsAssigned) : 0) + ";" + group.getCapacity());
			}
		}
		
		writer.close();
	}
}
