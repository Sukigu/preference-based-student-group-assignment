package main;
import java.io.File;
import java.io.IOException;

import ilog.concert.IloException;
import problem.AssignmentProblem;

public class Main {
	public static void main(String[] args) {
		try {
//			String s1InputPath = "res" + File.separator + "input" + File.separator + "s1" + File.separator;
//			
//			String s1v1OutputPath = "res" + File.separator + "output" + File.separator + "s1v1" + File.separator;
//			AssignmentProblem s1v1 = new AssignmentProblem(s1InputPath + "turmas.csv", s1InputPath + "escolhas.csv", "1", s1v1OutputPath);
//			s1v1.solve();
//			
//			String s1v4OutputPath = "res" + File.separator + "output" + File.separator + "s1v4" + File.separator;
//			AssignmentProblem s1v4 = new AssignmentProblem(s1InputPath + "turmas.csv", s1InputPath + "escolhas.csv", "4", s1v4OutputPath);
//			s1v4.solve();
//			
			String s2InputPath = "res" + File.separator + "input" + File.separator + "s2" + File.separator;
			
			String s2v2OutputPath = "res" + File.separator + "output" + File.separator + "s2v2" + File.separator;
			AssignmentProblem s2v2 = new AssignmentProblem(s2InputPath + "turmas.csv", s2InputPath + "escolhas.csv", "2", s2v2OutputPath);
			s2v2.run();
			
			String s2v4OutputPath = "res" + File.separator + "output" + File.separator + "s2v4" + File.separator;
			AssignmentProblem s2v4 = new AssignmentProblem(s2InputPath + "turmas.csv", s2InputPath + "escolhas.csv", "4", s2v4OutputPath);
			s2v4.run();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IloException e) {
			e.printStackTrace();
		}
	}
}
