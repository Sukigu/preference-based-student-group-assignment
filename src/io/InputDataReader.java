package io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import model.Course;
import model.Group;
import model.Student;
import model.StudentPreference;

public class InputDataReader {
	private String scheduleFilename,
		groupsFilename,
		preferencesFilename,
		gradesFilename,
		procVersion;
	
	private Map<Course, Map<String, Group>> coursesGroups;
	private Map<String, Student> students;
	
	public InputDataReader(String scheduleFilename, String groupsFilename, String preferencesFilename, String gradesFilename, String procVersion) throws IOException {
		this.scheduleFilename = scheduleFilename;
		this.groupsFilename = groupsFilename;
		this.preferencesFilename = preferencesFilename;
		this.gradesFilename = gradesFilename;
		this.procVersion = procVersion;
	}
	
	public void readData() throws IOException {
		coursesGroups = readCoursesGroups();
		students = readStudents(coursesGroups);
		readStudentsGrades(students);
		readSchedule(); // TODO: Read schedule first, reading info about courses/groups for the first time
	}
	
	public Map<Course, Map<String, Group>> getCoursesGroups() {
		return coursesGroups;
	}
	
	public Map<String, Student> getStudents() {
		return students;
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
	
	private Map<String, Student> readStudents(Map<Course, Map<String, Group>> coursesGroups) throws IOException {
		Map<String, Student> students = new HashMap<>();
		
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
				students.put(prevStudent.getCode(), prevStudent);
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
	
	private Map<String, Student> readStudentsGrades(Map<String, Student> students) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(gradesFilename));
		reader.readLine();
		String fileLine;
		
		while ((fileLine = reader.readLine()) != null) {
			String[] line = fileLine.split(";");
			
			String studentCode = line[0];
			float studentGrade = Float.parseFloat(line[1]);
			
			students.get(studentCode).setAvgGrade(studentGrade);
		}
		
		reader.close();
		
		return students;
	}
	
	private void readSchedule() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(scheduleFilename));
		reader.readLine();
		String fileLine;
		
		while ((fileLine = reader.readLine()) != null) {
			String[] line = fileLine.split(";");
			
			String courseCode = line[1];
			String groupCode = line[0];
			int weekday = Integer.parseInt(line[2]) - 2;
			int startTime = (int) ((Float.parseFloat(line[3]) - 8) * 2);
			int duration = (int) (Float.parseFloat(line[4]) * 2);
			
			int[][] groupSchedule = coursesGroups.get(courseCode).get(groupCode).getSchedule();
			for (int i = 0; i < duration; ++i) {
				groupSchedule[weekday][startTime + i] = 1;
			}
		}
		
		reader.close();
	}
}
