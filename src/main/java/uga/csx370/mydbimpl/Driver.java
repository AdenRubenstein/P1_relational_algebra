/**
 * Copyright (c) 2025 Sami Menik, PhD. All rights reserved.
 * 
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 * This software is provided "as is," without warranty of any kind.
 */
package uga.csx370.mydbimpl;

import java.util.List;

import uga.csx370.mydb.RA;
import uga.csx370.mydb.Relation;
import uga.csx370.mydb.RelationBuilder;
import uga.csx370.mydb.Type;
import uga.csx370.mydb.Predicate;

public class Driver {
    public static String DIR = System.getProperty("user.dir") + "/tables/";
    public static void main(String[] args) {
        // Following is an example of how to use the relation class.
        // This creates a table with three columns with below mentioned
        // column names and data types.
        // After creating the table, data is loaded from a CSV file.
        // Path should be replaced with a correct file path for a compatible
        // CSV file.
	RA RelationAlg = new RAImpl();
	
	Relation teaches = new RelationBuilder()
		.attributeNames(List.of("t_id", "t_course_id", "t_sec_id", "t_semester", "t_year"))
		.attributeTypes(List.of(Type.INTEGER, Type.INTEGER, Type.STRING, Type.STRING, Type.INTEGER))
				.build();
	teaches.loadData(DIR + "teaches_export.csv");

	Relation course = new RelationBuilder()
		.attributeNames(List.of("c_course_id", "c_title", "c_dept_name", "c_credits"))
		.attributeTypes(List.of(Type.INTEGER, Type.STRING, Type.STRING, Type.INTEGER))
				.build();
	course.loadData(DIR + "course_export.csv");

	Relation instructor = new RelationBuilder()
	    .attributeNames(List.of("i_id", "i_name", "i_dept_name", "i_salary"))
	    .attributeTypes(List.of(Type.INTEGER, Type.STRING, Type.STRING, Type.DOUBLE))
                .build();
	instructor.loadData(DIR + "instructor_export.csv");
	Relation advisor = new RelationBuilder()
	    .attributeNames(List.of("s_id", "inst_id"))
	    .attributeTypes(List.of(Type.STRING, Type.STRING))
                .build();
	advisor.loadData(DIR + "advisor_export.csv");
	Relation takes = new RelationBuilder()
	    .attributeNames(List.of("t_s_id", "course_id", "sec_id", "semester", "year", "grade"))
	    .attributeTypes(List.of(Type.STRING, Type.STRING, Type.STRING, Type.STRING, Type.INTEGER, Type.STRING))
                .build();
	takes.loadData(DIR + "takes_export.csv");

	Relation prereq = new RelationBuilder()
	    .attributeNames(List.of("p_course_id", "p_prereq_id"))
	    .attributeTypes(List.of(Type.INTEGER, Type.INTEGER))
                .build();
	prereq.loadData(DIR + "prereq_export.csv");

	Relation department = new RelationBuilder()
	    .attributeNames(List.of("d_dept_name", "d_building", "d_budget"))
	    .attributeTypes(List.of(Type.STRING, Type.STRING, Type.DOUBLE))
                .build();
	department.loadData(DIR + "department_export.csv");

	Relation student = new RelationBuilder()
	    .attributeNames(List.of("st_id", "st_name", "st_dept_name", "st_tot_cred"))
	    .attributeTypes(List.of(Type.STRING, Type.STRING, Type.STRING, Type.INTEGER))
                .build();
	student.loadData(DIR + "student_export.csv");


	
	// Aden's 
	System.out.printf("\n\nAden Rubenstein - amr00658 \n \n");
	System.out.println("Output: \n Select advisors with a student who has received \n an A+ on an English or Languages course in Fall 2010.");
	System.out.println(" Show the student ID, course & section ID, instructor ID, and instructor name.");
	Predicate p_join1 = row ->
	    row.get(0).getAsString().equals(row.get(6).getAsString())
	    && row.get(4).getAsInt() == 2010;
        Relation join1 = RelationAlg.join(takes, advisor, p_join1);

	Predicate p_select1 = row ->
	    row.get(3).getAsString().equals("Fall") && row.get(5).getAsString().equals("A+");
	Relation select1 = RelationAlg.select(join1, p_select1);

	Predicate p_join2 = row ->
	    row.get(7).getAsString().equals(String.valueOf(row.get(8).getAsInt())) &&
	    (row.get(10).getAsString().equals("English") || row.get(10).getAsString().equals("Languages"));
	Relation join2 = RelationAlg.join(select1, instructor, p_join2);
	Relation output_aden = RelationAlg.project(join2, List.of("t_s_id", "course_id", "sec_id", "i_id", "i_name"));
	output_aden.print();

	//Lior's
	System.out.print("\nLior Akselrad - la87760\n");
	System.out.println("Query: 4-credit courses that has a prereq, owned by department with a budget over $700k, and building of course.\n");

	Relation one = RelationAlg.join(prereq, course,
	    row -> row.get(0).getAsInt() == row.get(2).getAsInt());

	Relation two = RelationAlg.join(one, department,
	    row -> row.get(4).getAsString().equals(row.get(6).getAsString()));

	Relation three = RelationAlg.select(two,
	    row -> row.get(5).getAsInt() == 4 && row.get(8).getAsDouble() > 700000.0);

	three.print();
	

	// Poojitha
	System.out.print("\n Poojitha Kommineni - pk37813 \n");
	System.out.println("Query: Instructors who taught in fall, and taught a course worth 3+ credits.");

	Relation ti = RelationAlg.join(teaches, instructor, row ->
				       row.get(0).getAsInt() == row.get(5).getAsInt());
	Relation tic = RelationAlg.join(ti, course, row ->
					row.get(1).getAsInt() == row.get(9).getAsInt());
	Relation fall = RelationAlg.select(tic, row ->
					   row.get(3).getAsString().equals("Fall"));
	Relation bigCredit = RelationAlg.select(tic, row ->
						row.get(12).getAsInt() >= 3);
	Relation result = RelationAlg.intersect(fall, bigCredit);
	result.print();

	// Liam
	System.out.print("\n Liam Keenan - lkeen \n");
	System.out.println("Query: Seniors (75+ credits) who earned an A in Spring 2007 in a course run "
			   + "by a department housed in Saucon, with the course title and department budget.\n");

	// Narrow takes down first so the joins below stay small.
	Relation topGrade = RelationAlg.select(takes, row ->
					    row.get(5).getAsString().trim().equals("A")
					    && row.get(4).getAsInt() == 2007
					    && row.get(3).getAsString().equals("Spring"));

	// takes(0-5) + course(6-9): takes stores the course id as text, course as a number.
	Relation withCourse = RelationAlg.join(topGrade, course, row ->
					       row.get(1).getAsString().equals(String.valueOf(row.get(6).getAsInt())));

	// + department(10-12): the department that owns the course.
	Relation withDept = RelationAlg.join(withCourse, department, row ->
					     row.get(8).getAsString().equals(row.get(10).getAsString()));

	Relation inSaucon = RelationAlg.select(withDept, row ->
					       row.get(11).getAsString().equals("Saucon"));

	// + student(13-16): the student who took the course.
	Relation withStudent = RelationAlg.join(inSaucon, student, row ->
						row.get(0).getAsString().equals(row.get(13).getAsString()));

	// Restrict to seniors.
	Relation seniors = RelationAlg.select(withStudent, row -> row.get(16).getAsInt() >= 75);

	Relation output_liam = RelationAlg.project(seniors,
						   List.of("st_name", "st_dept_name", "st_tot_cred", "c_title", "d_dept_name", "d_budget"));
	output_liam.print();

	}

}
