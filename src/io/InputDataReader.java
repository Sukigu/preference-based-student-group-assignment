package io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import model.Course;
import model.Group;
import model.Schedule;
import model.Student;
import model.StudentPreference;

public class InputDataReader {
	private String coursesFilename, groupsFilename, groupCompositesFilename, preferencesFilename, gradesFilename, procVersion;
	private int semester;
	private Map<String, Course> courses;
	private Schedule schedule;
	private Map<String, Student> students;
	
	public InputDataReader(String coursesFilename, String groupsFilename, String groupCompositesFilename, String preferencesFilename, String gradesFilename, int semester, String procVersion) throws IOException {
		this.coursesFilename = coursesFilename;
		this.groupsFilename = groupsFilename;
		this.groupCompositesFilename = groupCompositesFilename;
		this.preferencesFilename = preferencesFilename;
		this.gradesFilename = gradesFilename;
		this.semester = semester;
		this.procVersion = procVersion;
		this.courses = new HashMap<>();
		this.schedule = new Schedule();
		this.students = new HashMap<>();
	}
	
	public void readData() throws IOException {
		readCourses();
		readGroupComposites();
		readGroups();
		readStudents();
		readStudentsGrades();
	}
	
	public Map<String, Course> getCourses() {
		return courses;
	}
	
	public Schedule getSchedule() {
		return schedule;
	}
	
	public Map<String, Student> getStudents() {
		return students;
	}
	
	private void readCourses() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(coursesFilename));
		reader.readLine();
		String fileLine;
		
		while ((fileLine = reader.readLine()) != null) {
			String[] line = fileLine.split(";");
			
			if (!((semester == 1 && line[3].equals("1S")) || (semester == 2 && line[3].equals("2S")))) continue;
			
			String courseCode = line[0];
			int weeklyTimeslots = Integer.parseInt(line[4]);
			boolean mandatory = (Integer.parseInt(line[5]) == 0) ? true : false;
			
			courses.put(courseCode, new Course(courseCode, mandatory, weeklyTimeslots));
		}
		
		reader.close();
	}
	
	private Map<String, Set<String>> readGroupComposites() throws IOException {
		Map<String, Set<String>> groupComposites = new HashMap<>();
		
		BufferedReader reader = new BufferedReader(new FileReader(groupCompositesFilename));
		reader.readLine();
		String fileLine;
		
		while ((fileLine = reader.readLine()) != null) {
			String[] line = fileLine.split(";");
			
			String compositeName = line[0];
			Set<String> groupCodes = new HashSet<>();
			
			for (int i = 1; i < line.length; ++i) {
				String groupCode = line[i];
				
				if (groupCode.equals("")) break;
				
				groupCodes.add(groupCode);
			}
			
			groupComposites.put(compositeName, groupCodes);
		}
		
		reader.close();
		
		return groupComposites;
	}
	
	private void readGroups() throws IOException {
		Map<String, Set<String>> groupComposites = readGroupComposites();
		
		BufferedReader reader = new BufferedReader(new FileReader(groupsFilename));
		reader.readLine();
		String fileLine;
		
		while ((fileLine = reader.readLine()) != null) {
			String[] line = fileLine.split(";");
			
			String groupCode = line[0];
			String courseCode = line [1];
			int weekDay = Integer.parseInt(line[2]) - 2;
			int startTime = (int) ((Float.parseFloat(line[3]) - 8) * 2);
			int duration = (int) (Float.parseFloat(line[4]) * 2);
			boolean isPracticalClass = line[5].equals("T") ? false : true;
			int groupCapacity = Integer.parseInt(line[6]);
			
			Course thisCourse = courses.get(courseCode);
			Set<String> groupsFromComposite = groupComposites.get(groupCode);
			
			if (groupsFromComposite == null) { // If this is not a group composite, add it to this course and to the schedule
				Group thisGroup = thisCourse.getGroups().get(groupCode);
				
				if (thisGroup == null) { // (Most likely.) If this is a new group, create it
					thisGroup = new Group(groupCode, groupCapacity);
				}
				else { // If we had needed to lazily create this group before as part of a composite, just fill in the missing information
					thisGroup.setCapacity(groupCapacity);
				}
				
				thisCourse.getGroups().put(groupCode, thisGroup);
				schedule.addCourseGroup(thisCourse, thisGroup, isPracticalClass, weekDay, startTime, duration);
			}
			else {
				for (String groupFromComposite : groupsFromComposite) { // If it is, then only add the individual groups to the schedule
					Group thisGroup = thisCourse.getGroups().get(groupFromComposite);
					if (thisGroup == null) thisGroup = new Group(groupFromComposite, -1); // If the individual group hasn't been created yet, lazily create it now
					schedule.addCourseGroup(thisCourse, thisGroup, isPracticalClass, weekDay, startTime, duration);
				}
			}
		}
		
		reader.close();
	}
	
	private void readStudents() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(preferencesFilename));
		reader.readLine();
		String fileLine;
		
		while ((fileLine = reader.readLine()) != null) {
			String[] line = fileLine.split(";");
			
			if (!line[0].equals(procVersion)) continue;
			
			String studentCode = line[1];
			String studentName = line[2];
			int preferenceOrder = Integer.parseInt(line[6]);
			String courseCode = line[7];
			String groupCode = line[8];
			
			Student thisStudent = students.get(studentCode);
			if (thisStudent == null) { // If this is a new student, create them and add them to the students map
				thisStudent = new Student(studentCode);
				thisStudent.setName(studentName);
				students.put(studentCode, thisStudent);
			}
			
			StudentPreference thisPreference;
			
			try {
				thisPreference = thisStudent.getPreferences().get(preferenceOrder - 1);
			} catch (IndexOutOfBoundsException e) { // If this is a new preference, create it and add it to the student's preference list
				thisPreference = new StudentPreference(preferenceOrder);
				thisStudent.getPreferences().add(thisPreference);
			}
			
			Course thisCourse = courses.get(courseCode);
			Group thisGroup = thisCourse.getGroups().get(groupCode);
			
			thisPreference.addCourseGroupPair(thisCourse, thisGroup);
			
			thisStudent.getEnrolledCourses().add(thisCourse); // Add it to the list of this student's enrollments
			thisStudent.addToTargetSumOfTimeslots(thisCourse.getWeeklyTimeslots()); // Add this course's weekly number of timeslots to this student's target sum of timeslots
		}
		
		reader.close();
	}
	
	private void readStudentsGrades() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(gradesFilename));
		reader.readLine();
		String fileLine;
		
		while ((fileLine = reader.readLine()) != null) {
			String[] line = fileLine.split(";");
			
			String studentCode = line[0];
			float studentGrade = line.length == 2 ? Float.parseFloat(line[1]) : 0; // Some students have missing grade information
			
			Student thisStudent = students.get(studentCode);
			if (thisStudent != null) { // If the student isn't found, it means they're not being assigned to groups in this process version
				thisStudent.setAvgGrade(studentGrade);
			}
		}
		
		reader.close();
	}
}
