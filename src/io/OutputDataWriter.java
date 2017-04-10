package io;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
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
	private double cplexTolerance;
	private Map<String, Course> courses;
	private Map<String, Student> students;
	private String outputPath;
	
	public OutputDataWriter(IloCplex cplex, double cplexTolerance, Map<String, Course> courses, Map<String, Student> students, String outputPath) {
		this.cplex = cplex;
		this.cplexTolerance = cplexTolerance;
		this.courses = courses;
		this.students = students;
		this.outputPath = outputPath;
	}
	
	public void checkGroupCapacities() throws IOException {
		Map<Course, Integer> courseEnrollments = new HashMap<>(); // Total number of enrolled students per course
		
		for (Student student : students.values()) {
			for (Course enrolledCourse : student.getEnrolledCourses()) {
				Integer courseEnrollment = courseEnrollments.get(enrolledCourse);
				if (courseEnrollment == null) courseEnrollment = new Integer(0);
				
				courseEnrollment += 1;
				courseEnrollments.put(enrolledCourse, courseEnrollment);
			}
		}
		
		String output = "UC;OPTATIVA;VAGAS TOTAIS;ESTUDANTES INSCRITOS;PERCENTAGEM";
		
		for (Course course : courses.values()) {
			Integer enrolledStudents = courseEnrollments.get(course);
			if (enrolledStudents == null) continue;
			
			int totalCapacity = 0;
			
			for (Group group : course.getGroups().values()) {
				totalCapacity += group.getCapacity();
			}
			
			float ratio = (float) totalCapacity / enrolledStudents * 100;
			
			if (ratio <= 115) { // If theres little (or no) surplus in group capacity, output to file
				output += "\r\n" + course.getCode() + ";" + (course.getMandatory() ? "0" : "1") + ";" + totalCapacity + ";" + enrolledStudents + ";" + ratio;
			}
		}
		
		writeToFile(outputPath + "problemas vagas.csv", output);
	}
	
	public void writeOutputData() throws IloException, IOException {
		writeStudentsAssignments();
		writeGroupStats();
	}
	
	private void writeToFile(String filename, String output) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "utf-8"));
		writer.write(output);
		writer.close();
	}
	
	private void writeStudentsAssignments() throws IloException, IOException {
		String output = "ESTUD_NUM_UNICO_INST;NOME;OPCAO;CODIGO;SIGLA";
		
		int courseEnrollments = 0, courseAssignments = 0, completeAssignments = 0, partialAssignments = 0, preferencesFulfilled = 0;
		
		for (Student student : students.values()) {
			int studentEnrollments = 0, studentAssignments = 0;
			
			for (Map.Entry<Course, Map<Group, IloIntVar>> courseEntry : student.getCourseGroupAssignments().entrySet()) {
				++courseEnrollments; ++studentEnrollments;
				Course course = courseEntry.getKey();
				
				for (Map.Entry<Group, IloIntVar> groupEntry : courseEntry.getValue().entrySet()) {
					IloIntVar assignmentVar = groupEntry.getValue();
					
					if (Math.abs(cplex.getValue(assignmentVar) - 1) < cplexTolerance) {
						++courseAssignments; ++studentAssignments;
						Group group = groupEntry.getKey();
						
						output += "\r\n" + student.getCode() + ";" + student.getName() + ";" + "-1" + ";" + course.getCode() + ";" + group.getCode();
						
						break; // A student can't be assigned to more than one group per course, so the loop can be terminated
					}
				}
			}
			
			boolean hasCompleteAssignmentCplex = (Math.abs(cplex.getValue(student.getHasCompleteAssignment()) - 1) < cplexTolerance); // CPLEX variable indicating a complete assignment
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
			
			for (StudentPreference preference : student.getPreferences()) {
				IloIntVar wasFulfilled = preference.getWasFulfilled();
				
				if (Math.abs(cplex.getValue(wasFulfilled) - 1) < cplexTolerance) {
					preferencesFulfilled += 1;
					break;
				}
			}
		}
		
		writeToFile(outputPath + "colocações.csv", output);
		
		writeAssignmentStats(courseEnrollments, courseAssignments, students.size(), completeAssignments, partialAssignments, preferencesFulfilled);
	}
	
	private void writeAssignmentStats(int courseEnrollments, int courseAssignments, int numStudents, int completeAssignments, int partialAssignments, int preferencesFulfilled) throws IOException {
		String output = "";
		
		output += "Inscrições em UCs: " + courseEnrollments;
		output += "\r\n" + "Colocações em UCs: " + courseAssignments;
		output += "\r\n" + "Percentagem de colocações: " + (float) courseAssignments / courseEnrollments * 100 + "%";
		output += "\r\n" + "\r\n" + "Número de estudantes: " + numStudents;
		output += "\r\n" + "Colocações completas: " + completeAssignments;
		output += "\r\n" + "Colocações parciais: " + partialAssignments;
		output += "\r\n" + "Percentagem de colocações completas: " + (float) completeAssignments / numStudents * 100 + "%";
		output += "\r\n" + "\r\n" + "Preferências satisfeitas: " + preferencesFulfilled;
		output += "\r\n" + "Percentagem de alunos c/ preferências satisfeitas: " + (float) preferencesFulfilled / numStudents * 100 + "%";
		output += "\r\n" + "\r\n" + "Inscrições médias por estudante: " + (float) courseEnrollments / numStudents;
		output += "\r\n" + "Colocações médias por estudante: " + (float) courseAssignments / numStudents;
		
		writeToFile(outputPath + "estatísticas.txt", output);
	}
	
	private void writeGroupStats() throws IloException, IOException {
		String output = "UC;TURMA;COLOCADOS;CAPACIDADE";
		
		for (Course course : courses.values()) {
			for (Group group : course.getGroups().values()) {
				IloLinearIntExpr numStudentsAssigned = group.getSumAllAssignedStudents();
				
				output += "\r\n" + course.getCode() + ";" + group.getCode() + ";" + (numStudentsAssigned != null ? (int) cplex.getValue(numStudentsAssigned) : 0) + ";" + group.getCapacity();
			}
		}
		
		writeToFile(outputPath + "turmas.csv", output);
	}
}
