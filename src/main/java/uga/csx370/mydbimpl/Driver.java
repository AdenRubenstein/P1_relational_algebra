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

public class Driver {
    
    public static void main(String[] args) {
        // Following is an example of how to use the relation class.
        // This creates a table with three columns with below mentioned
        // column names and data types.
        // After creating the table, data is loaded from a CSV file.
        // Path should be replaced with a correct file path for a compatible
        // CSV file.
        Relation instructor = new RelationBuilder()
	    .attributeNames(List.of("ID", "name", "dept_name", "salary"))
	    .attributeTypes(List.of(Type.INTEGER, Type.STRING, Type.STRING, Type.DOUBLE))
                .build();
	instructor.loadData("/home/adenr290/mysql-files/P1_relational_algebra/tables/instructor_export.csv");
	 Relation advisor = new RelationBuilder()
	    .attributeNames(List.of("s_id", "i_id"))
	    .attributeTypes(List.of(Type.STRING, Type.STRING))
                .build();
	advisor.loadData("/home/adenr290/mysql-files/P1_relational_algebra/tables/advisor_export.csv");
	Relation takes = new RelationBuilder()
	    .attributeNames(List.of("ID", "course_id", "sec_id", "semester", "year", "grade"))
	    .attributeTypes(List.of(Type.STRING, Type.STRING, Type.STRING, Type.STRING, Type.INTEGER, Type.STRING))
                .build();
	takes.loadData("/home/adenr290/mysql-files/P1_relational_algebra/tables/takes_export.csv");
	
	System.out.printf("\n\nAden Rubenstein - amr00658 \n \n");
        takes.print();

    }

}
