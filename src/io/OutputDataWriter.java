package io;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
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
	private Map<String, Course> courses;
	private Map<String, Student> students;
	private String outputPath;
	
	public OutputDataWriter(IloCplex cplex, Map<String, Course> courses, Map<String, Student> students, String outputPath) {
		this.cplex = cplex;
		this.courses = courses;
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
		
		int courseEnrollments = 0, courseAssignments = 0, completeAssignments = 0, partialAssignments = 0;
		
		for (Student student : students.values()) {
			int studentEnrollments = 0, studentAssignments = 0;
			
			for (Map.Entry<String, Map<String, IloIntVar>> courseEntry : student.getCourseGroupAssignments().entrySet()) {
				++courseEnrollments; ++studentEnrollments;
				String courseCode = courseEntry.getKey();
				
				for (Map.Entry<String, IloIntVar> groupEntry : courseEntry.getValue().entrySet()) {
					IloIntVar assignmentVar = groupEntry.getValue();
					
					if (cplex.getValue(assignmentVar) == 1) {
						++courseAssignments; ++studentAssignments;
						String groupCode = groupEntry.getKey();
						
						writer.newLine();
						writer.write(student.getCode() + ";" + student.getName() + ";" + "-1" + ";" + courseCode + ";" + groupCode);
						
						break; // A student can't be assigned to more than one group per course, so the loop can be terminated
					}
				}
			}
			
			boolean hasCompleteAssignmentCplex = (cplex.getValue(student.getHasCompleteAssignment()) == 1); // CPLEX variable indicating a complete assignment
			boolean hasCompleteAssignmentCheck = (studentEnrollments == studentAssignments); // Manually checking if the student has a complete assignment
			
			if (hasCompleteAssignmentCplex != hasCompleteAssignmentCheck) {
				System.out.println("CPLEX reports incorrect number of complete assignments!");
			}
			else if (hasCompleteAssignmentCheck) {
				++completeAssignments;
			}
			else if (studentAssignments > 0) {
				++partialAssignments;
			}
		}
		
		writer.close();
		
		writeAssignmentStats(courseEnrollments, courseAssignments, students.size(), completeAssignments, partialAssignments);
	}
	
	private void writeStudentsAssignments() throws IloException, IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath + "colocações.csv"), "utf-8"));
		writer.write("ESTUD_NUM_UNICO_INST;NOME;OPCAO;CODIGO;SIGLA");
		
		int numAssignedStudents = 0;
		
		for (Map.Entry<String, Student> studentEntry : students.entrySet()) {
			List<StudentPreference> thisStudentPreferences = studentEntry.getValue().getPreferences();
			
			for (int i = 0; i < thisStudentPreferences.size(); ++i) {
				StudentPreference thisPreference = thisStudentPreferences.get(i);
				
				if (cplex.getValue(thisPreference.getVarPreferenceAssigned()) == 1) {
					++numAssignedStudents;
					Map<Course, Group> courseGroupPairs = thisPreference.getCourseGroupPairs();
					
					for (Map.Entry<Course, Group> groupEntry : courseGroupPairs.entrySet()) {
						writer.newLine();
						writer.write(studentEntry.getValue().getCode() + ";" + studentEntry.getValue().getName() + ";" + thisPreference.getOrder() + ";" + groupEntry.getKey().getCode() + ";" + groupEntry.getValue().getCode());
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
	
	private void writeAssignmentStats(int courseEnrollments, int courseAssignments, int numStudents, int completeAssignments, int partialAssignments) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath + "estatísticas.txt"), "utf-8"));
		
		writer.write("Inscrições em UCs: " + courseEnrollments);
		writer.newLine();
		writer.write("Colocações em UCs: " + courseAssignments);
		writer.newLine();
		writer.write("Percentagem de colocações: " + (float) courseAssignments / courseEnrollments * 100 + "%");
		writer.newLine(); writer.newLine();
		writer.write("Número de estudantes: " + numStudents);
		writer.newLine();
		writer.write("Colocações completas: " + completeAssignments);
		writer.newLine();
		writer.write("Colocações parciais: " + partialAssignments);
		writer.newLine();
		writer.write("Percentagem de colocações completas: " + (float) completeAssignments / numStudents * 100 + "%");
		writer.newLine(); writer.newLine();
		writer.write("Inscrições médias por estudante: " + (float) courseEnrollments / numStudents);
		writer.newLine();
		writer.write("Colocações médias por estudante: " + (float) courseAssignments / numStudents);
		
		writer.close();
	}
	
	private void writeGroupStats() throws IloException, IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath + "turmas.csv"), "utf-8"));
		writer.write("UC;TURMA;COLOCADOS;CAPACIDADE");
		
		for (Course course : courses.values()) {
			for (Group group : course.getGroups().values()) {
				IloLinearIntExpr numStudentsAssigned = group.getConstrSumAssignedStudents();
				
				writer.newLine();
				writer.write(course.getCode() + ";" + group.getCode() + ";" + (numStudentsAssigned != null ? (int) cplex.getValue(numStudentsAssigned) : 0) + ";" + group.getCapacity());
			}
		}
		
		writer.close();
	}
}
