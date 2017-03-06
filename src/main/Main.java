package main;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ilog.concert.IloException;
import model.Student;
import model.StudentGroup;

public class Main {
	public static void main(String[] args) {
		try {
			// Semester 1 version 1
			String inputPath = "res" + File.separator + "input" + File.separator + "s1" + File.separator;
			InputDataReader reader = new InputDataReader(inputPath + "turmas.csv", inputPath + "escolhas.csv");
			
			Map<String, StudentGroup> groupMap = reader.readStudentGroups();
			List<StudentGroup> groups = new ArrayList<>(groupMap.values());
			List<Student> students = reader.readStudentPreferences("1", groupMap);
			
			AssignmentProblem sem1ver1 = new AssignmentProblem(students, groups);
			sem1ver1.solve("res" + File.separator + "output" + File.separator + "s1v1" + File.separator);
			
			// Semester 1 version 4
			students = reader.readStudentPreferences("4", groupMap);
			AssignmentProblem sem1ver4 = new AssignmentProblem(students, groups);
			sem1ver4.solve("res" + File.separator + "output" + File.separator + "s1v4" + File.separator);
			
			// Semester 2 version 2
			inputPath = "res" + File.separator + "input" + File.separator + "s2" + File.separator;
			reader = new InputDataReader(inputPath + "turmas.csv", inputPath + "escolhas.csv");
			
			groupMap = reader.readStudentGroups();
			groups = new ArrayList<>(groupMap.values());
			students = reader.readStudentPreferences("2", groupMap);
			
			AssignmentProblem sem2ver1 = new AssignmentProblem(students, groups);
			sem2ver1.solve("res" + File.separator + "output" + File.separator + "s2v2" + File.separator);
			
			// Semester 2 version 4
			students = reader.readStudentPreferences("4", groupMap);
			AssignmentProblem sem2ver4 = new AssignmentProblem(students, groups);
			sem2ver4.solve("res" + File.separator + "output" + File.separator + "s2v4" + File.separator);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IloException e) {
			e.printStackTrace();
		}
	}
}
