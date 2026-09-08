package uga.csx370.mydbimpl;

import java.util.List;
import java.util.ArrayList;

import uga.csx370.mydb.Cell;
import uga.csx370.mydb.Predicate;
import uga.csx370.mydb.RA;
import uga.csx370.mydb.Relation;
import uga.csx370.mydb.RelationBuilder;
import uga.csx370.mydb.Type;

public class RAImpl implements RA {

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
    public Relation diff(Relation rel1, Relation rel2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'diff'");
    }

    @Override
    public Relation rename(Relation rel, List<String> origAttr, List<String> renamedAttr) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'rename'");
    }

    @Override
    public Relation cartesianProduct(Relation rel1, Relation rel2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'cartesianProduct'");
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
