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
	private String groupsFilename, groupCompositesFilename, preferencesFilename, gradesFilename, procVersion;
	private List<List<Map<String, List<String>>>> schedule;
	private Map<Course, Map<String, Group>> coursesGroups;
	private Map<String, Student> students;
	
	public InputDataReader(String groupsFilename, String groupCompositesFilename, String preferencesFilename, String gradesFilename, String procVersion) throws IOException {
		this.groupsFilename = groupsFilename;
		this.groupCompositesFilename = groupCompositesFilename;
		this.preferencesFilename = preferencesFilename;
		this.gradesFilename = gradesFilename;
		this.procVersion = procVersion;
		
		// Build schedule: list of lists (representing days and timeslots), each containing a map of courseCodes to a list of groupCodes taught in that time period
		schedule = new ArrayList<>();
		for (int weekday = 0; weekday < 6; ++weekday) {
			List<Map<String, List<String>>> dayList = new ArrayList<>();
			
			for (int timeslot = 0; timeslot < 25; ++timeslot) {
				Map<String, List<String>> timeslotClasses = new HashMap<>();
				dayList.add(timeslotClasses);
			}
			
			schedule.add(dayList);
		}
		
		coursesGroups = new HashMap<>();
		students = new HashMap<>();
	}
	
	public void readData() throws IOException {
		readSchedule();
		readStudents();
		readStudentsGrades();
	}
	
	public List<List<Map<String, List<String>>>> getSchedule() {
		return schedule;
	}
	
	public Map<Course, Map<String, Group>> getCoursesGroups() {
		return coursesGroups;
	}
	
	public Map<String, Student> getStudents() {
		return students;
	}
	
	private void readSchedule() throws IOException {
		Map<String, List<String>> groupComposites = readGroupComposites();
		
		BufferedReader reader = new BufferedReader(new FileReader(groupsFilename));
		reader.readLine();
		String fileLine;
		
		while ((fileLine = reader.readLine()) != null) {
			String[] line = fileLine.replace(" ", "").split(";");
			
			String courseCode = line[1];
			String groupCode = line[0];
			int weekday = Integer.parseInt(line[2]) - 2;
			int startTime = (int) ((Float.parseFloat(line[3]) - 8) * 2);
			int duration = (int) (Float.parseFloat(line[4]) * 2);
			int groupCapacity = Integer.parseInt(line[5]);
			boolean mandatory = (Integer.parseInt(line[6]) == 0) ? true : false;
			
			if (!groupCode.startsWith("COMP_")) { // If it's not a group composite, add this group alone to the schedule
				Course thisCourse = new Course(courseCode, mandatory);
				coursesGroups.putIfAbsent(thisCourse, new HashMap<>());
				coursesGroups.get(thisCourse).put(groupCode, new Group(groupCode, groupCapacity));
				
				for (int i = 0; i < duration; ++i) {
					Map<String, List<String>> timeslotClassesMap = schedule.get(weekday).get(startTime + i);
					List<String> timeslotGroups = timeslotClassesMap.get(courseCode);
					if (timeslotGroups == null) timeslotGroups = new ArrayList<>();
					timeslotGroups.add(groupCode);
					timeslotClassesMap.put(courseCode, timeslotGroups);
				}
			} else { // If it is a composite, get all groups associated with it and add them all to the schedule
				List<String> groupsAssociatedWithComposite = groupComposites.get(groupCode);
				
				for (int i = 0; i < duration; ++i) {
					Map<String, List<String>> timeslotClassesMap = schedule.get(weekday).get(startTime + i);
					List<String> timeslotGroups = timeslotClassesMap.get(courseCode);
					if (timeslotGroups == null) timeslotGroups = new ArrayList<>();
					for (String group : groupsAssociatedWithComposite) {
						timeslotGroups.add(group);
					}
					timeslotClassesMap.put(courseCode, timeslotGroups);
				}
			}
		}
		
		reader.close();
	}
	
	private Map<String, List<String>> readGroupComposites() throws IOException {
		Map<String, List<String>> groupComposites = new HashMap<>();
		
		BufferedReader reader = new BufferedReader(new FileReader(groupCompositesFilename));
		reader.readLine();
		String fileLine;
		
		while ((fileLine = reader.readLine()) != null) {
			String[] line = fileLine.replace(" ", "").split(";");
			
			String compositeName = line[0];
			List<String> groupCodes = new ArrayList<>();
			
			for (int i = 1; true; ++i) {
				String groupCode = line[i];
				
				if (groupCode.equals("")) break;
				
				groupCodes.add(groupCode);
			}
			
			groupComposites.put(compositeName, groupCodes);
		}
		
		reader.close();
		
		return groupComposites;
	}
	
	private void readStudents() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(preferencesFilename));
		reader.readLine();
		String fileLine;
		
		Student prevStudent = null;
		
		while ((fileLine = reader.readLine()) != null) {
			String[] line = fileLine.split(";");
			
			String studentCode = line[1];
			String studentName = line[2];
			int preferenceOrder = Integer.parseInt(line[6]);
			String courseCode = line[7];
			String groupCode = line[8];
			
			Student thisStudent = new Student(studentCode);
			if (!thisStudent.equals(prevStudent)) { // If this is a new student...
				if (prevStudent != null) students.put(prevStudent.getCode(), prevStudent); // Save the previous student
				thisStudent.setName(studentName);
			}
			else {
				thisStudent = prevStudent; // If it isn't, retrieve the student
			}
			
			if (!line[0].equals(procVersion)) continue;
			
			StudentPreference thisPreference = new StudentPreference(preferenceOrder);
			Map<Integer, StudentPreference> studentPreferences = thisStudent.getPreferences(); // Get this student's preferences
			studentPreferences.putIfAbsent(preferenceOrder, thisPreference); // If this is a new preference, put it in the map
			thisPreference = studentPreferences.get(preferenceOrder); // If it isn't, retrieve the existing preference
			
			thisPreference.addCourseGroupPair(courseCode, coursesGroups.get(new Course(courseCode, false)).get(groupCode));
			
			// TODO: ADD ENROLLED COURSES TO THIS STUDENT
			
			prevStudent = thisStudent;
		}
		
		reader.close();
	}
	
	private void readStudentsGrades() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(gradesFilename));
		reader.readLine();
		String fileLine;
		
		while ((fileLine = reader.readLine()) != null) {
			String[] line = fileLine.replace(" ", "").split(";");
			
			String studentCode = line[0];
			float studentGrade = line.length == 2 ? Float.parseFloat(line[1]) : 0; // Some students have missing grade information
			
			Student thisStudent = students.get(new Student(studentCode));
			if (thisStudent != null) { // If the student isn't found, it means they're not being assigned to groups in this process version
				thisStudent.setAvgGrade(studentGrade);
			}
		}
		
		reader.close();
	}
}
