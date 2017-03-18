package io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Course;
import model.Group;
import model.Student;
import model.StudentPreference;

public class InputDataReader {
	private String groupsFilename;
	private String scheduleFilename;
	private String preferencesFilename;
	private String gradesFilename;
	private String procVersion;
	
	public InputDataReader(String groupsFilename, String scheduleFilename, String preferencesFilename, String gradesFilename, String procVersion) throws IOException {
		this.groupsFilename = groupsFilename;
		this.scheduleFilename = scheduleFilename;
		this.preferencesFilename = preferencesFilename;
		this.gradesFilename = gradesFilename;
		this.procVersion = procVersion;
	}
	
	public void readStuff() throws IOException {
		Map<Course, Map<String, Group>> coursesGroups = readCoursesGroups();
		List<Student> students = readStudents(coursesGroups);
		readStudentsGrades(students);
		//readSchedule();
	}
	
	private Map<Course, Map<String, Group>> readCoursesGroups() throws IOException {
		Map<Course, Map<String, Group>> coursesGroups = new HashMap<>();
		
		BufferedReader reader = new BufferedReader(new FileReader(groupsFilename));
		reader.readLine();
		String fileLine;
		
		while ((fileLine = reader.readLine()) != null) {
			String[] line = fileLine.split(";");
			
			String courseCode = line[0];
			String groupCode = line[1];
			int groupCapacity = Integer.parseInt(line[2]);
			
			Course thisCourse = new Course(courseCode);
			coursesGroups.putIfAbsent(thisCourse, new HashMap<>());
			coursesGroups.get(thisCourse).put(groupCode, new Group(groupCode, groupCapacity));
		}
		
		reader.close();
		
		return coursesGroups;
	}
	
	private List<Student> readStudents( Map<Course, Map<String, Group>> coursesGroups) throws IOException {
		List<Student> students = new ArrayList<>();
		
		BufferedReader reader = new BufferedReader(new FileReader(preferencesFilename));
		reader.readLine();
		String fileLine;
		
		Student prevStudent = null;
		
		while ((fileLine = reader.readLine()) != null) {
			String[] line = fileLine.split(";");
			
			if (!line[0].equals(procVersion)) continue;
			
			String studentCode = line[1];
			String studentName = line[2];
			int preferenceOrder = Integer.parseInt(line[6]);
			String courseCode = line[7];
			String groupCode = line[8];
			
			Student thisStudent = new Student(studentCode);
			if (!thisStudent.equals(prevStudent)) { // If this is a new student, save the previous student
				students.add(prevStudent);
				thisStudent.setName(studentName);
			}
			else {
				thisStudent = prevStudent; // If it isn't, retrieve the student
			}
			
			StudentPreference thisPreference = new StudentPreference(preferenceOrder);
			Map<Integer, StudentPreference> studentPreferences = thisStudent.getPreferences(); // Get this student's preferences
			studentPreferences.putIfAbsent(preferenceOrder, thisPreference); // If this is a new preference, put it in the map
			thisPreference = studentPreferences.get(preferenceOrder); // If it isn't, retrieve the existing preference
			
			thisPreference.addCourseGroupPair(courseCode, coursesGroups.get(courseCode).get(groupCode));
			
			prevStudent = thisStudent;
		}
		
		reader.close();
		
		return students;
	}
	
	private List<Student> readStudentsGrades(List<Student> students) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(gradesFilename));
		reader.readLine();
		String fileLine;
		
		while ((fileLine = reader.readLine()) != null) {
			// TODO: Read grades from file
		}
		
		reader.close();
		
		return students;
	}
}
