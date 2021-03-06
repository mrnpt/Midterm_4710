package RModel;


import java.util.*;
import java.util.stream.Collectors;

public class Query {
    private Relation relation;

    /*
    group by a relation r by the attribute groupbyAttr and then apply aggregate function aggrFunction on the attribute selectAttr
    return result as a relation
     */
    public Relation select_groupby(String aggrFunction, Attribute selectAttr, Relation r, Attribute groupbyAttr) {
        Relation tmp = this.project(new ArrayList<Attribute>(Collections.singletonList(groupbyAttr)), r);
        ArrayList<Tuple> result = new ArrayList<>();
        Relation finalRelation = null;
        for(Tuple t1 : tmp.getTuples()) {
            HashMap<String, Object> aTuplpe = new HashMap<>();
            ArrayList<Tuple> tuplesOfOneGroup = new ArrayList<>();
            for(Tuple t2 : r.getTuples()) {
                if(t1.getAttribute(groupbyAttr.getName()).equals(t2.getAttribute(groupbyAttr.getName()))) {
                    tuplesOfOneGroup.add(t2);
                }
            }
            Relation OneGroup = this.select(selectAttr, new Relation(new ArrayList<Attribute>(Collections.singletonList(groupbyAttr)), tuplesOfOneGroup), aggrFunction);
            aTuplpe.put(groupbyAttr.getName(), t1.getAttribute(groupbyAttr.getName()));
            aTuplpe.put(OneGroup.getAttributeNames().get(0), OneGroup.getTuples().get(0).getAttribute(OneGroup.getAttributeNames().get(0)));
            result.add(new Tuple(aTuplpe));
            ArrayList<Attribute> attr_4_union = new ArrayList<>();
            attr_4_union.add(groupbyAttr);
            attr_4_union.add(OneGroup.getAttributes().get(0));
            Relation relation_4_union = new Relation(r.getName(), attr_4_union, result);
            if(finalRelation == null) {
                finalRelation = relation_4_union;
            } else {
                finalRelation = this.union(finalRelation, relation_4_union);
            }
        }
        return finalRelation;
    }

    /*
    select aggrFunction(attr) from r
    return result as a relation
     */
    public Relation select(Attribute attr, Relation r, String aggrFunction) {
        if(!attr.getType().equals(Integer.class) && !aggrFunction.equalsIgnoreCase("count")) throw new IllegalArgumentException("Attribute type must be Integer");
        ArrayList<Tuple> result = new ArrayList<>();
        ArrayList<Attribute> attributes = new ArrayList<>();
        HashMap<String, Object> value = new HashMap<>();
        attributes.add(new Attribute(aggrFunction+"_"+attr.getName(), attr.getType()));
        ArrayList<Integer> intArr = new ArrayList<>();
        if(aggrFunction.equalsIgnoreCase("count"))
            value.put(aggrFunction+"_"+attr.getName(), r.getValuesOfColumn(r.getTuples(), attr).size());
        else {
            r.getValuesOfColumn(r.getTuples(), attr).forEach((n) -> intArr.add((Integer) n));
            switch (aggrFunction.toLowerCase()) {
                case "min":
                    value.put(aggrFunction + "_" + attr.getName(), Collections.min(intArr));
                    break;
                case "max":
                    value.put(aggrFunction + "_" + attr.getName(), Collections.max(intArr));
                    break;
                case "avg":
                case "average":
                    value.put(aggrFunction + "_" + attr.getName(), intArr.stream().mapToDouble(a -> a).sum() / intArr.size());
                    break;
                case "sum":
                    value.put(aggrFunction + "_" + attr.getName(), intArr.stream().mapToInt(a -> a).sum());
                    break;
                default:
                    throw new IllegalArgumentException("Aggregate function is illegal");
            }
        }
        result.add(new Tuple(value));

        return new Relation(r.getName(), attributes, result);
    }

    /*
    select a set of attributes from relation r
    return result as a relation
     */
    public Relation select(ArrayList<Attribute> attributes, Relation r) {
        Map<String, Object> value = new HashMap<>();
        ArrayList<Tuple> tuples = new ArrayList<>();
        if(attributes.size() < 1) throw new IllegalArgumentException("Set of attribute must not be empty");
        for(Attribute attr : attributes) {
            if(!r.getAttributeNames().contains(attr.getName()))
                throw new IllegalArgumentException("Set of attribute must be a subset of relation " + r.getName());
        }
        int no_Of_Attrs = 0;
        int no_Of_Rows = 0;


        while(no_Of_Rows < r.getValuesOfColumn(r.getTuples(), attributes.get(0)).size()) {
            while (no_Of_Attrs < attributes.size()) {
                value.put(attributes.get(no_Of_Attrs).getName(), r.getValuesOfColumn(r.getTuples(), attributes.get(no_Of_Attrs)).get(no_Of_Rows));
                no_Of_Attrs++;
            }
            tuples.add(new Tuple(value));
            no_Of_Rows++;
            no_Of_Attrs = 0;
        }
        relation = new Relation(r.getName(), attributes, tuples);
        return relation;
    }


    /*
    select a set of attributes from relation r with a condition
    return result as a relation
     */
    public Relation select(ArrayList<Attribute> attributes, Relation r, Attribute whereAttrs, String condition, Object operand) {
        Relation tmp = new Relation(r.getName(), r.getAttributes(), r.getTuples());
        if(condition.equals("=")) {
            tmp.getTuples().removeIf(t->!t.getAttribute(whereAttrs.getName()).equals(operand));
        } else {
            tmp.getTuples().removeIf(t -> t.getAttribute(whereAttrs.getName()).equals(operand));
            if(condition.equals("<")) {
                if(operand instanceof Integer) {
                    tmp.getTuples().removeIf(t -> (Integer)t.getAttribute(whereAttrs.getName()) > (Integer)operand);
                } else if(operand instanceof String) {
                    tmp.getTuples().removeIf(t -> t.getAttribute(whereAttrs.getName()).toString().compareTo(operand.toString()) > 0);
                }
            } else if (condition.equals(">")) {
                if(operand instanceof Integer) {
                    tmp.getTuples().removeIf(t -> (Integer)t.getAttribute(whereAttrs.getName()) < (Integer)operand);
                } else if(operand instanceof String) {
                    tmp.getTuples().removeIf(t -> t.getAttribute(whereAttrs.getName()).toString().compareTo(operand.toString()) < 0);
                }
            }
        }
        Relation result = new Relation(r.getName(), attributes, tmp.getTuples());
        return result;
    }

    /*
    project a set of attributes on relation r. Only print distinct tuples
    return result as a relation
     */
    public Relation project(ArrayList<Attribute> attributes, Relation r) {
        Map<String, Object> value = new HashMap<>();
        ArrayList<Tuple> tuples = new ArrayList<>();
        if(attributes.size() < 1) throw new IllegalArgumentException("Set of attribute must not be empty");
         for(Attribute attr : attributes) {
            if(!r.getAttributeNames().contains(attr.getName()))
                throw new IllegalArgumentException("Set of attribute must be a subset of relation " + r.getName());
        }
        int no_Of_Attrs = 0;
        int no_Of_Rows = 0;


        while(no_Of_Rows < r.getValuesOfColumn(r.getTuples(), attributes.get(0)).size()) {
            while (no_Of_Attrs < attributes.size()) {
                value.put(attributes.get(no_Of_Attrs).getName(), r.getValuesOfColumn(r.getTuples(), attributes.get(no_Of_Attrs)).get(no_Of_Rows));
                no_Of_Attrs++;
            }
            if(tuples.size() == 0)
                tuples.add(new Tuple(value));
            else {
                int l = tuples.size();
                boolean isDuplicate = false;
                for(int i = 0; i < l; i++) {
                    if(tuples.get(i).equals(new Tuple(value)))
                        isDuplicate = true;
                }
                if(!isDuplicate)
                    tuples.add(new Tuple(value));
            }
            no_Of_Rows++;
            no_Of_Attrs = 0;
        }
        relation = new Relation(r.getName(), attributes, tuples);
        return relation;
    }

    /*
    Union two relations r1 and r2
    return result as a relation
     */
    public Relation union(Relation r1, Relation r2) {
        if(r1.getAttributes().size() != r2.getAttributes().size()) throw new IllegalArgumentException("Numbers of attributes do not match.");
        else {
            for(int i = 0; i < r1.getAttributes().size(); i++) {
                if(!r1.getAttributes().get(i).equals(r2.getAttributes().get(i))) throw new IllegalArgumentException("Attribute types do not match.");
            }
        }
        r1.getTuples().addAll(r2.getTuples());

        return this.project(r1.getAttributes(), r1);
    }

    /*
    Intersect two relation r1 and r2
    return result as a relation
     */
    public Relation intersect(Relation r1, Relation r2) {
        if(r1.getAttributes().size() != r2.getAttributes().size()) throw new IllegalArgumentException("Numbers of attributes do not match.");
        else {
            for(int i = 0; i < r1.getAttributes().size(); i++) {
                if(!r1.getAttributes().get(i).equals(r2.getAttributes().get(i))) throw new IllegalArgumentException("Attribute types do not match.");
            }
        }

        ArrayList<Tuple> smallerTuples = r1.getTuples().size() < r2.getTuples().size() ? r1.getTuples() : r2.getTuples();
        ArrayList<Tuple> biggerTuples = r1.getTuples().size() < r2.getTuples().size() ? r2.getTuples() : r1.getTuples();
        ArrayList<Tuple> result = new ArrayList<>();

        for (Tuple smallerTuple : smallerTuples) {
            for (Tuple biggerTuple : biggerTuples) {
                if (biggerTuple.equals(smallerTuple)) {
                    result.add(smallerTuple);
                    break;
                }
            }
        }

        return new Relation(r1.getAttributes(), result);
    }

    //Return new relation containing tuples which are in relation r1 and not in relation r2
    public Relation differ(Relation r1, Relation r2) {
        if(r1.getAttributes().size() != r2.getAttributes().size()) throw new IllegalArgumentException("Numbers of attributes do not match.");
        else {
            for(int i = 0; i < r1.getAttributes().size(); i++) {
                if(!r1.getAttributes().get(i).equals(r2.getAttributes().get(i))) throw new IllegalArgumentException("Attribute types do not match.");
            }
        }

        ArrayList<Tuple> result = new ArrayList<>();

        for(int i = 0; i < r1.getTuples().size(); i++) {
            boolean isInBoth = false;
            for(int j = 0; j < r2.getTuples().size(); j++) {
                if(r1.getTuples().get(i).equals(r2.getTuples().get(j))) {
                    isInBoth = true;
                    break;
                }
            }
            if(!isInBoth)
                result.add(r1.getTuples().get(i));
        }

        return new Relation(r1.getAttributes(), result);
    }

    /*
    cross join two relation r1 and r2
    return result as a relation
     */
    public Relation crossJoin (Relation r1, Relation r2) {
        ArrayList<Tuple> result = new ArrayList<>();
        ArrayList<Attribute> newAttrs = new ArrayList<>(r1.getAttributes());

        for (Attribute attr : r2.getAttributes()) {
            if (r1.getAttributeNames().contains(attr.getName())) {
                newAttrs.add(new Attribute( attr.getName()+ "_" + r2.getName(), attr.getType()));
            }
            else
                newAttrs.add(attr);
        }

        ArrayList<ArrayList<Object>> tuplesOfr2 = new ArrayList<>();
        for(Attribute attr : r2.getAttributes())
            tuplesOfr2.add(r2.getValuesOfColumn(r2.getTuples(), attr));

        for(Tuple t1 : r1.getTuples()) {
            ArrayList<Object> aTuple = new ArrayList<>();
            for (Attribute attr : r1.getAttributes()) {
                aTuple.add(t1.getAttribute(attr.getName()));
            }
            for(int i = 0; i < tuplesOfr2.get(0).size(); i++) {
                HashMap<String, Object> newTuple = new HashMap<>();
                ArrayList<Object> tmp = new ArrayList<>(aTuple);
                for(int j = 0; j < tuplesOfr2.size(); j++) {
                    tmp.add(tuplesOfr2.get(j).get(i));
                }
                for(int k = 0; k < tmp.size(); k++) {
                    newTuple.put(newAttrs.get(k).getName(), tmp.get(k));
                }
                result.add(new Tuple(newTuple));
            }
        }
        ArrayList<String> relationName = new ArrayList<>(Arrays.asList(r1.getName().split("_")));
        relationName.addAll(Arrays.asList(r2.getName().split("_")));
        relationName = (ArrayList<String>) relationName.stream().distinct().collect(Collectors.toList());

        return new Relation(String.join("_", relationName),newAttrs, result);
    }

    /*
    equi-join two relation r1 and r2 on attribute onAttr
    return result as a relation
     */
    public Relation equiJoin (Relation r1, Relation r2, Attribute onAttr) {
        if(!r1.getAttributeNames().contains(onAttr.getName()) || !r2.getAttributeNames().contains(onAttr.getName()))
            throw new IllegalArgumentException(String.format("The attribute %s does not exist in either or both relations.", onAttr.getName()));

        Relation tmp = this.crossJoin(r1, r2);
        ArrayList<Tuple> result = new ArrayList<>();

        for(Tuple t : tmp.getTuples()) {
            if(t.getAttribute(onAttr.getName()).equals(t.getAttribute(onAttr.getName()+"_"+r2.getName())))
                result.add(t);
        }

        ArrayList<String> relationName = new ArrayList<>(Arrays.asList(r1.getName().split("_")));
        relationName.addAll(Arrays.asList(r2.getName().split("_")));
        relationName = (ArrayList<String>) relationName.stream().distinct().collect(Collectors.toList());

        return new Relation(String.join("_", relationName), tmp.getAttributes(), result);
    }

    /*
    natural join two relation r1 and r2
    return result as a relation
     */
    public Relation naturalJoin (Relation r1, Relation r2) {
        ArrayList<Attribute> onAttrs = new ArrayList<>();
        for(Attribute attr : r2.getAttributes()) {
            if(r1.getAttributeNames().contains(attr.getName()))
                onAttrs.add(attr);
        }
        if(onAttrs.size() == 0) throw new IllegalArgumentException("There is no attribute that has same name in both relations");

        Relation tmp1 = this.equiJoin(r1, r2, onAttrs.get(0));
        ArrayList<Attribute> tmp = new ArrayList<>(tmp1.getAttributes());
        for(int i = 1; i < onAttrs.size(); i++) {
            tmp1 = equiJoin(tmp1, r2, onAttrs.get(i));
        }

        ArrayList<Tuple> result = new ArrayList<>();
        for(Attribute attr : onAttrs) {
            for(Tuple t : tmp1.getTuples()) {
                if(t.getAttribute(attr.getName()).equals(t.getAttribute(attr.getName()+"_"+r2.getName()))) {
                    t.deleteAnAttribute(attr.getName()+"_"+r2.getName());
                    result.add(t);
                }
            }
            tmp.removeIf(s->s.getName().equals(attr.getName()+"_"+r2.getName()));
        }
        ArrayList<String> relationName = new ArrayList<>(Arrays.asList(r1.getName().split("_")));
        relationName.addAll(Arrays.asList(r2.getName().split("_")));
        relationName = (ArrayList<String>) relationName.stream().distinct().collect(Collectors.toList());

        return new Relation(String.join("_", relationName),tmp, result);
    }
}
