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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'project'");
    }

    @Override
    public Relation union(Relation rel1, Relation rel2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'union'");
    }

    @Override
    public Relation intersect(Relation rel1, Relation rel2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'intersect'");
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
        // Attributes that appear in both relations, in rel1's order.
        List<String> common = new ArrayList<>();
        for (String attr : rel1.getAttrs()) {
            if (rel2.hasAttr(attr)) {
                common.add(attr);
            }
        }

        // Output schema: all of rel1, then rel2's columns except the common ones.
        List<String> outAttrs = new ArrayList<>(rel1.getAttrs());
        List<Type> outTypes = new ArrayList<>(rel1.getTypes());
        List<Integer> keep = new ArrayList<>();
        for (int i = 0; i < rel2.getAttrs().size(); i++) {
            String attr = rel2.getAttrs().get(i);
            if (!common.contains(attr)) {
                outAttrs.add(attr);
                outTypes.add(rel2.getTypes().get(i));
                keep.add(i);
            }
        }

        Relation result = new RelationBuilder()
                .attributeNames(outAttrs)
                .attributeTypes(outTypes)
                .build();

        // Keep every pair of rows that agrees on all the common attributes.
        for (int i = 0; i < rel1.getSize(); i++) {
            List<Cell> left = rel1.getRow(i);
            for (int j = 0; j < rel2.getSize(); j++) {
                List<Cell> right = rel2.getRow(j);
                boolean match = true;
                for (String attr : common) {
                    if (!left.get(rel1.getAttrIndex(attr))
                            .equals(right.get(rel2.getAttrIndex(attr)))) {
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
                if (p.check(joined)) {
                    result.insert(joined);
                }
            }
        }
        return result;
    }

}
