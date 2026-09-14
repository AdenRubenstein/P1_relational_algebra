package uga.csx370.mydbimpl;

import java.util.List;
import java.util.ArrayList;

import uga.csx370.mydb.Cell;
import uga.csx370.mydb.Predicate;
import uga.csx370.mydb.RA;
import uga.csx370.mydb.Relation;
import uga.csx370.mydb.RelationBuilder;
import uga.csx370.mydb.Type;
import java.util.HashSet;
import java.util.Set;

public class RAImpl implements RA {



    @Override
    public Relation project(Relation rel, List<String> attrs) {
        List<Type> relTypes = rel.getTypes();
        List<Integer> keep = new ArrayList<>();
        List<Type> outTypes = new ArrayList<>();

        // Record where each requested attribute lives in the original row.
        for (String attr : attrs) {
            if (!rel.hasAttr(attr)) {
                throw new IllegalArgumentException("Attribute does not exist: " + attr);
            }
            int idx = rel.getAttrIndex(attr);
            keep.add(idx);
            outTypes.add(relTypes.get(idx));
        }

        Relation result = new RelationBuilder()
                .attributeNames(new ArrayList<>(attrs))
                .attributeTypes(outTypes)
                .build();

        Set<List<Cell>> seen = new HashSet<>();

        // Copy only the kept columns; projection is a set operation, so drop duplicates.
        for (int i = 0; i < rel.getSize(); ++i) {
            List<Cell> row = rel.getRow(i);
            List<Cell> projected = new ArrayList<>();

            for (int idx : keep) {
                projected.add(row.get(idx));
            }
            if (seen.add(projected)) {
                result.insert(projected);
            }
        }
        return result;
    }

    @Override
    public Relation union(Relation rel1, Relation rel2) {
		//union needs matching schemas
        if (!rel1.getAttrs().equals(rel2.getAttrs()) || !rel1.getTypes().equals(rel2.getTypes())) {
			throw new IllegalArgumentException("Relations are not compatible.");
   		}

    	Relation result = new RelationBuilder()
            	.attributeNames(rel1.getAttrs())
            	.attributeTypes(rel1.getTypes())
            	.build();

    	Set<List<Cell>> seen = new HashSet<>();

    	// add all unique rows from rel1.
    	for (int i = 0; i < rel1.getSize(); ++i) {
        	List<Cell> row = rel1.getRow(i);
        	if (seen.add(row)) {
            	result.insert(row);
        	}
    	}

		//add all unique row from rel2 if it's not already here
		for (int i = 0; i < rel2.getSize(); ++i) {
        	List<Cell> row = rel2.getRow(i);
        	if (seen.add(row)) {
            	result.insert(row);
        	}
    	}
    	return result;
	}

    @Override
    public Relation intersect(Relation rel1, Relation rel2) {
        //intersection needs matching schemas too
		if (!rel1.getAttrs().equals(rel2.getAttrs()) || !rel1.getTypes().equals(rel2.getTypes())) {
        	throw new IllegalArgumentException("Relations are not compatible.");
   		}

		Relation result = new RelationBuilder()
           	 	.attributeNames(rel1.getAttrs())
            	.attributeTypes(rel1.getTypes())
            	.build();

    	Set<List<Cell>> rightRows = new HashSet<>();
    	for (int i = 0; i < rel2.getSize(); ++i) {
        	rightRows.add(rel2.getRow(i));
    	}

    	Set<List<Cell>> seen = new HashSet<>();

		//making sure there's no duplication
		for (int i = 0; i < rel1.getSize(); ++i) {
        	List<Cell> row = rel1.getRow(i);
        	if (rightRows.contains(row) && seen.add(row)) {
            	result.insert(row);
        	}
    	}
    	return result;
	}
    
    @Override
    public Relation cartesianProduct(Relation rel1, Relation rel2) {
        // Copy of predicate-join, but with tautlogy.
        for (String attr : rel2.getAttrs()) {
            if (rel1.hasAttr(attr)) {
                throw new IllegalArgumentException("Relations share attribute: " + attr);
            }
        }

        List<String> outAttrs = new ArrayList<>(rel1.getAttrs());
        outAttrs.addAll(rel2.getAttrs());
        List<Type> outTypes = new ArrayList<>(rel1.getTypes());
        outTypes.addAll(rel2.getTypes());

        Relation result = new RelationBuilder()
                .attributeNames(outAttrs)
                .attributeTypes(outTypes)
                .build();

        for (int i = 0; i < rel1.getSize(); i++) {
            List<Cell> left = rel1.getRow(i);
            for (int j = 0; j < rel2.getSize(); j++) {
                List<Cell> joined = new ArrayList<>(left);
                joined.addAll(rel2.getRow(j));
                result.insert(joined);
     
            }
        }
        return result;
    }
    
    @Override
    public Relation select(Relation rel, Predicate p) {
	
        // Output Prpearation 
        List<String> outAttrs = new ArrayList<>(rel .getAttrs());
        List<Type> outTypes = new ArrayList<>(rel .getTypes());
        Relation result = new RelationBuilder()
                .attributeNames(outAttrs)
                .attributeTypes(outTypes)
                .build();

	for (int i=0; i < rel.getSize(); i++) {
	    List<Cell> insertion = rel.getRow(i);
	    if (p.check(insertion)) {
		result.insert(insertion);
	    }
	}
	return result;
	
    }   

    @Override
    public Relation diff(Relation rel1, Relation rel2) {
        // Difference requires matching schemas.
        if (!rel1.getAttrs().equals(rel2.getAttrs()) || !rel1.getTypes().equals(rel2.getTypes())) {
            throw new IllegalArgumentException("Relations are not compatible.");
        }

        Relation result = new RelationBuilder()
                .attributeNames(rel1.getAttrs())
                .attributeTypes(rel1.getTypes())
                .build();
        Set<List<Cell>> rightRows = new HashSet<>();

        // Store rows from rel2 for quick membership checks.
        for (int i = 0; i < rel2.getSize(); ++i) {
            rightRows.add(rel2.getRow(i));
        }

        Set<List<Cell>> seen = new HashSet<>();

        // Add unique rows from rel1 that do not appear in rel2.
        for (int i = 0; i < rel1.getSize(); ++i) {
            List<Cell> row = rel1.getRow(i);

            if (!rightRows.contains(row) && seen.add(row)) {
                result.insert(row);
            }
        }
        return result;
    }

    @Override
    public Relation rename(Relation rel, List<String> origAttr, List<String> renamedAttr) {
        // Each original attribute must have one corresponding replacement name.
        if (origAttr.size() != renamedAttr.size()) {
            throw new IllegalArgumentException("Attribute lists must have equal lengths.");
        }

        List<String> attrs = rel.getAttrs();

        // Replace the requested names while preserving column order and types.
        for (int i = 0; i < origAttr.size(); ++i) {
            if (!rel.hasAttr(origAttr.get(i))) {
                throw new IllegalArgumentException("Attribute does not exist: " + origAttr.get(i));
            }
            attrs.set(rel.getAttrIndex(origAttr.get(i)), renamedAttr.get(i));
        }

        Relation result = new RelationBuilder()
                .attributeNames(attrs)
                .attributeTypes(rel.getTypes())
                .build();

        // Copy the original rows into the relation with the renamed schema.
        for (int i = 0; i < rel.getSize(); ++i) {
            result.insert(rel.getRow(i));
        }
        return result;
    }


    @Override
    public Relation join(Relation rel1, Relation rel2) {
        //Attributes that appear in both relations
        List<String> attributeboth = new ArrayList<>();
        for (String attr : rel1.getAttrs()) {
            if (rel2.hasAttr(attr)) {
                attributeboth.add(attr);
            }
        }

        //Output schema is all of rel1, then rel2's columns except the ones with matching values
        List<String> outputAttribute = new ArrayList<>(rel1.getAttrs());
        List<Type> outputTypes = new ArrayList<>(rel1.getTypes());
        List<Integer> keep = new ArrayList<>();
        for (int i = 0; i < rel2.getAttrs().size(); i++) {
            String attr = rel2.getAttrs().get(i);
            if (!attributeboth.contains(attr)) {
                outputAttribute.add(attr);
                outputTypes.add(rel2.getTypes().get(i));
                keep.add(i);
            }
        }

        Relation result = new RelationBuilder()
                .attributeNames(outputAttribute)
                .attributeTypes(outputTypes)
                .build();

        //Keep every pair of rows that has the same values.
        for (int i = 0; i < rel1.getSize(); i++) {
            List<Cell> left = rel1.getRow(i);
            for (int j = 0; j < rel2.getSize(); j++) {
                List<Cell> right = rel2.getRow(j);
                boolean match = true;
                for (String attribute : attributeboth) {
                    if (!left.get(rel1.getAttrIndex(attribute))
                            .equals(right.get(rel2.getAttrIndex(attribute)))) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    List<Cell> joined = new ArrayList<>(left);
                    for (int idx : keep) {
                        joined.add(right.get(idx));
                    }
                    result.insert(joined);
                }
            }
        }
        return result;
    }

    @Override
    public Relation join(Relation rel1, Relation rel2, Predicate p) {
        //Ensures we dont have the same attribute names
        for (String attribute : rel2.getAttrs()) {
            if (rel1.hasAttr(attribute)) {
                throw new IllegalArgumentException("Relations share attribute: " + attribute);
            }
        }

        //output is rel1 columns and then rel2 columns but in order
        List<String> outputAttributes = new ArrayList<>(rel1.getAttrs());
        outputAttributes.addAll(rel2.getAttrs());
        List<Type> outputTypes = new ArrayList<>(rel1.getTypes());
        outputTypes.addAll(rel2.getTypes());

        Relation result = new RelationBuilder()
                .attributeNames(outputAttributes)
                .attributeTypes(outputTypes)
                .build();

        //Check every row from rel 1 and 2 row-by-row. Joins left and right rows if they comply with predicate
        for (int i = 0; i < rel1.getSize(); i++) {
            List<Cell> left = rel1.getRow(i);
            for (int j = 0; j < rel2.getSize(); j++) {
                List<Cell> joined = new ArrayList<>(left);
                joined.addAll(rel2.getRow(j));
                if (p.check(joined)) {
                    result.insert(joined);
                }
            }
        }
        return result;
    }

}
