/**
 * Copyright (c) 2025 Sami Menik, PhD. All rights reserved.
 * 
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 * This software is provided "as is," without warranty of any kind.
 */
package uga.csx370.mydbimpl;

import java.util.List;

import uga.csx370.mydb.Relation;
import uga.csx370.mydb.RelationBuilder;
import uga.csx370.mydb.Type;
import uga.csx370.mydb.Predicate;

public class Driver {
    
    public static void main(String[] args) {
        // Following is an example of how to use the relation class.
        // This creates a table with three columns with below mentioned
        // column names and data types.
        // After creating the table, data is loaded from a CSV file.
        // Path should be replaced with a correct file path for a compatible
        // CSV file.
	RAImpl RelationAlg = new RAImpl();
	
        Relation instructor = new RelationBuilder()
	    .attributeNames(List.of("i_id", "i_name", "i_dept_name", "i_salary"))
	    .attributeTypes(List.of(Type.INTEGER, Type.STRING, Type.STRING, Type.DOUBLE))
                .build();
	instructor.loadData("/home/adenr290/mysql-files/P1_relational_algebra/tables/instructor_export.csv");
	 Relation advisor = new RelationBuilder()
	    .attributeNames(List.of("s_id", "inst_id"))
	    .attributeTypes(List.of(Type.STRING, Type.STRING))
                .build();
	advisor.loadData("/home/adenr290/mysql-files/P1_relational_algebra/tables/advisor_export.csv");
	Relation takes = new RelationBuilder()
	    .attributeNames(List.of("t_s_id", "course_id", "sec_id", "semester", "year", "grade"))
	    .attributeTypes(List.of(Type.STRING, Type.STRING, Type.STRING, Type.STRING, Type.INTEGER, Type.STRING))
                .build();
	takes.loadData("/home/adenr290/mysql-files/P1_relational_algebra/tables/takes_export.csv");

	// Aden's 
	System.out.printf("\n\nAden Rubenstein - amr00658 \n \n");
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
	join2.print();

    }

}
