package main;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Student;
import model.StudentGroup;
import model.StudentPreference;

public class InputDataReader {
	private String groupsFilename;
	private String preferencesFilename;
	
	private Map<String, StudentGroup> groups;
	private List<Student> students;
	
	public InputDataReader(String groupsFilename, String preferencesFilename) throws IOException {
		this.groupsFilename = groupsFilename;
		this.preferencesFilename = preferencesFilename;
		this.groups = new HashMap<String, StudentGroup>();
		this.students = new ArrayList<Student>();
	}
	
	public List<StudentGroup> getGroups() {
		return new ArrayList<StudentGroup>(groups.values());
	}
	
	public List<Student> getStudents() {
		return students;
	}
	
	public void readInputData() throws IOException {
		readStudentGroups();
		readStudentPreferences();
	}
	
	private void readStudentGroups() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader("res" + File.separator + "input" + File.separator + groupsFilename));
		reader.readLine();
		String fileLine;
		
		while ((fileLine = reader.readLine()) != null) {
			String[] line = fileLine.split(";");
			
			String courseCode = line[0];
			String groupCode = line[1];
			int groupCapacity = Integer.parseInt(line[2]);
			
			groups.put(courseCode + "_" + groupCode, new StudentGroup(courseCode, groupCode, groupCapacity));
		}
		
		reader.close();
	}
	
	private void readStudentPreferences() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader("res" + File.separator + "input" + File.separator + preferencesFilename));
		reader.readLine();
		String fileLine;
		
		Student currentStudent = null;
		StudentPreference currentPreference = null;
		
		do {
			if ((fileLine = reader.readLine()) == null) {
				if (currentStudent != null) { // If there's still a student left to add to the students list...
					if (currentPreference != null) { // If there's still a preference left to add to this student...
						currentStudent.addPreference(currentPreference);
					}
					students.add(currentStudent);
				}
				
				break;
			}
			
			String[] line = fileLine.split(";");
			
			String studentCode = line[1];
			String studentName = line[2];
			int order = Integer.parseInt(line[6]);
			String courseCode = line[7];
			String groupCode = line[8];
			
			if (currentStudent == null || !studentCode.equals(currentStudent.getCode())) { // If this is a new student...
				if (currentStudent != null) {
					currentStudent.addPreference(currentPreference);
					students.add(currentStudent);
				}
				
				currentStudent = new Student(studentCode, studentName);
				currentPreference = null;
			}
			
			if (currentPreference == null || order != currentPreference.getOrder()) { // If this is a new preference for this student...
				if (currentPreference != null) {
					currentStudent.addPreference(currentPreference);
				}
				
				currentPreference = new StudentPreference(order);
			}
			
			currentPreference.addCourseGroupPair(courseCode, groups.get(courseCode + "_" + groupCode));
		} while (true);
		
		reader.close();
	}
}
