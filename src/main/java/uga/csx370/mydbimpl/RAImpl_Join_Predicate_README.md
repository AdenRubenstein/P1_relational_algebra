# Using the `Predicate` with `RAImpl.join(rel1, rel2, p)`

A working reference for the theta-join predicate in `uga.csx370.mydbimpl.RAImpl`.

> **Note:** This is a developer reference, **not** the `README.txt` deliverable the
> assignment asks for. That one needs group number, team member names, and each
> member's contributions, and lives inside the project folder.

---

## Contents

1. [The core rule: index offsets](#1-the-core-rule-index-offsets)
2. [Case 1 - Equi-join](#case-1---equi-join)
3. [Case 2 - Inequality / self-join](#case-2---inequality--self-join)
4. [Case 3 - Three-table composition](#case-3---three-table-composition)
5. [Case 4 - Range and multi-condition](#case-4---range-and-multi-condition)
6. [Gotchas](#gotchas)
7. [Status of `RAImpl`](#status-of-raimpl)

---

## 1. The core rule: index offsets

`Predicate` is a functional interface with one method, so a lambda works:

```java
Predicate p = row -> /* boolean */;
```

`RAImpl.join(rel1, rel2, p)` builds the **concatenated** row before testing it —
all of `rel1`'s cells, then all of `rel2`'s cells — and passes that whole list to
`p.check(...)`. So inside the predicate:

| To reach a column of... | Use this index |
| --- | --- |
| `rel1` | `rel1.getAttrIndex("X")` |
| `rel2` | `rel1.getAttrs().size() + rel2.getAttrIndex("Y")` |

**The `rel1.getAttrs().size()` offset on `rel2` is the part that is easy to miss.**
Forgetting it silently reads the wrong column, or throws a type-conversion error.

`select(rel, p)` is different: its predicate receives a **single-relation** row, so
there is no offset. Only theta join needs the arithmetic.

### The pattern to follow

Compute every index **once, outside the lambda**, into `final` locals. This keeps
the predicate readable and survives schema reordering.

```java
final int off     = instructor.getAttrs().size();          // where rel2 begins
final int iDept   = instructor.getAttrIndex("dept_name");
final int iSalary = instructor.getAttrIndex("salary");
final int dName   = off + dept.getAttrIndex("d_name");
final int dBudget = off + dept.getAttrIndex("budget");

Predicate p = row ->
        row.get(iDept).getAsString().equals(row.get(dName).getAsString())
     && row.get(iSalary).getAsDouble() * 2 > row.get(dBudget).getAsDouble();

Relation res = ra.join(instructor, dept, p);
res.print();
```

Output:

```
+-------+------------+------------+----------+------------+----------+-----------+
| ID    | name       | dept_name  | salary   | d_name     | building | budget    |
+-------+------------+------------+----------+------------+----------+-----------+
| 10101 | Srinivasan | Comp. Sci. | 65000.00 | Comp. Sci. | Taylor   | 100000.00 |
| 12121 | Wu         | Finance    | 90000.00 | Finance    | Painter  | 120000.00 |
+-------+------------+------------+----------+------------+----------+-----------+
```

Note the rename to `d_name`. **Theta join throws `IllegalArgumentException` if the
two relations share any attribute name** — that is specified behavior, so rename
one side first. Natural join, `join(rel1, rel2)`, is the operator that merges
common attributes instead.

---

## Case 1 - Equi-join

The workhorse. Use it when you renamed a key column and now need to match on it.

```java
Relation adv = ra.rename(advisor, List.of("i_id"), List.of("adv_id"));

final int off = instructor.getAttrs().size();
final int a   = instructor.getAttrIndex("ID");
final int b   = off + adv.getAttrIndex("adv_id");

Predicate eq = row -> row.get(a).getAsString().equals(row.get(b).getAsString());

Relation r = ra.join(instructor, adv, eq);
```

**Use `.equals()`, never `==`.** `Cell` overrides `equals` and `hashCode`; `==`
compares object references and will be false even for identical values.

---

## Case 2 - Inequality / self-join

The case a natural join genuinely cannot express. Here: every pair of instructors
in the same department where the first out-earns the second.

```java
Relation i2 = ra.rename(instructor,
        List.of("ID",  "name",  "dept_name",  "salary"),
        List.of("ID2", "name2", "dept_name2", "salary2"));

final int off = instructor.getAttrs().size();
final int d1  = instructor.getAttrIndex("dept_name");
final int s1  = instructor.getAttrIndex("salary");
final int d2  = off + i2.getAttrIndex("dept_name2");
final int s2  = off + i2.getAttrIndex("salary2");

Predicate outEarns = row ->
        row.get(d1).getAsString().equals(row.get(d2).getAsString())
     && row.get(s1).getAsDouble() > row.get(s2).getAsDouble();

ra.join(instructor, i2, outEarns).print();
```

Output:

```
+-------+--------+------------+----------+-------+------------+------------+----------+
| ID    | name   | dept_name  | salary   | ID2   | name2      | dept_name2 | salary2  |
+-------+--------+------------+----------+-------+------------+------------+----------+
| 45565 | Katz   | Comp. Sci. | 75000.00 | 10101 | Srinivasan | Comp. Sci. | 65000.00 |
| 83821 | Brandt | Comp. Sci. | 92000.00 | 10101 | Srinivasan | Comp. Sci. | 65000.00 |
| 83821 | Brandt | Comp. Sci. | 92000.00 | 45565 | Katz       | Comp. Sci. | 75000.00 |
+-------+--------+------------+----------+-------+------------+------------+----------+
```

A self-join **must** go through `rename` — joining a relation with itself otherwise
trips the shared-attribute check on every column.

---

## Case 3 - Three-table composition

This is the shape the assignment requires: *"Each query must have operation
compositions and should use more than two tables."*

Query: **each student, their advisor, and that advisor's department.**

The key insight is that after the first join the intermediate relation has a
**new, wider schema** — so recompute the offset from the *intermediate*, not from
the original left-hand relation.

```java
Relation adv  = ra.rename(advisor,
        List.of("s_id",     "i_id"),
        List.of("adv_s_id", "adv_i_id"));
Relation inst = ra.rename(instructor,
        List.of("ID",   "name",   "dept_name", "salary"),
        List.of("i_ID", "i_name", "i_dept",    "i_salary"));

// Step 1: student JOIN advisor  on  student.ID = advisor.s_id
final int off1 = student.getAttrs().size();          // 4
final int sID  = student.getAttrIndex("ID");
final int aS   = off1 + adv.getAttrIndex("adv_s_id");
Predicate onAdvisee = row ->
        row.get(sID).getAsString().equals(row.get(aS).getAsString());
Relation sa = ra.join(student, adv, onAdvisee);

// Step 2: (result) JOIN instructor  on  advisor.i_id = instructor.ID
final int off2 = sa.getAttrs().size();               // 6 - recomputed!
final int aI   = sa.getAttrIndex("adv_i_id");        // index within sa
final int iID  = off2 + inst.getAttrIndex("i_ID");
Predicate onInstructor = row ->
        row.get(aI).getAsString().equals(row.get(iID).getAsString());
Relation full = ra.join(sa, inst, onInstructor);

// Step 3: narrow it down
Relation cs = ra.select(full, row ->
        row.get(full.getAttrIndex("dept_name")).getAsString().equals("Comp. Sci."));
ra.project(cs, List.of("name", "i_name", "i_dept")).print();
```

Output of the two joins:

```
+-------+---------+------------+----------+----------+----------+-------+------------+------------+----------+
| ID    | name    | dept_name  | tot_cred | adv_s_id | adv_i_id | i_ID  | i_name     | i_dept     | i_salary |
+-------+---------+------------+----------+----------+----------+-------+------------+------------+----------+
| 00128 | Zhang   | Comp. Sci. |      102 | 00128    | 45565    | 45565 | Katz       | Comp. Sci. | 75000.00 |
| 12345 | Shankar | Comp. Sci. |       32 | 12345    | 10101    | 10101 | Srinivasan | Comp. Sci. | 65000.00 |
| 19991 | Brandt  | History    |       80 | 19991    | 22222    | 22222 | Einstein   | Physics    | 95000.00 |
+-------+---------+------------+----------+----------+----------+-------+------------+------------+----------+
```

**Push `select` down where you can.** Theta join is a nested loop — it tests
`rel1.getSize() * rel2.getSize()` pairs. Filtering a table *before* joining it
keeps the intermediate relations small, which also helps you hit the assignment's
"not empty, not more than ~50 rows" target.

---

## Case 4 - Range and multi-condition

Predicates are ordinary Java booleans — combine them freely with `&&`, `||`, `!`.

```java
final int yr = takes.getAttrIndex("year");
Predicate recent = row -> row.get(yr).getAsInt() >= 2018
                       && row.get(yr).getAsInt() <= 2019;
Relation r = ra.select(takes, recent);              // no offset - select!
```

Inside a join, with a `Set` lookup and a string method:

```java
final Set<String> passing = Set.of("A", "A-", "B+", "B");

final int off   = takes.getAttrs().size();
final int tYear = takes.getAttrIndex("year");
final int tGr   = takes.getAttrIndex("grade");
final int cTtl  = off + course.getAttrIndex("title");

Predicate p = row ->
        row.get(tYear).getAsInt() >= 2018
     && passing.contains(row.get(tGr).getAsString())
     && row.get(cTtl).getAsString().startsWith("Intro");
```

Predicates are also reusable — assign one to a variable and pass it to several
calls, rather than rewriting the lambda each time.

---

## Gotchas

### Type mismatches

`Cell.getAsInt()` throws `RuntimeException("Illegal cell type conversion.")` if the
cell is not `Type.INTEGER`; same for `getAsDouble()` and `getAsString()`. And
`Cell.equals` compares **type as well as value**, so:

```java
Cell.val(10101).equals(Cell.val("10101"))   // false - INTEGER vs STRING
```

In the current `Driver`, `instructor.ID` is declared `INTEGER` while `takes.ID` and
`advisor.i_id` are `STRING`. Joining across those will either throw or silently
match nothing. **Declare ID consistently as `STRING` in all schemas**, or convert
explicitly inside the predicate.

### Use interfaces as types

An explicit grading criterion: *"Your implementation must use the interfaces as
types whenever it is possible."*

```java
RA ra = new RAImpl();          // correct
Relation r = ra.join(a, b, p); // correct
Predicate p = row -> ...;      // correct

RAImpl ra = new RAImpl();      // costs points
RelationImpl r = ...;          // costs points
```

### Effectively final

Locals captured by a lambda must be `final` or effectively final. Declaring the
index variables `final` makes this explicit and catches accidental reassignment.

### Cost

Theta join is `O(|rel1| * |rel2|)` — it materializes and tests every pair. Chained
joins over unfiltered tables blow up fast. Filter early (see Case 3).

---

## Status of `RAImpl`

| Operator | Status |
| --- | --- |
| `join(rel1, rel2)` — natural | Implemented |
| `join(rel1, rel2, p)` — theta | Implemented |
| `select` | `UnsupportedOperationException` |
| `project` | `UnsupportedOperationException` |
| `union` | `UnsupportedOperationException` |
| `intersect` | `UnsupportedOperationException` |
| `diff` | `UnsupportedOperationException` |
| `rename` | `UnsupportedOperationException` |
| `cartesianProduct` | `UnsupportedOperationException` |

Cases 1, 2, and 3 above all depend on `rename`, and Cases 3 and 4 on `select` and
`project`. **`rename` is the blocker** — without it no theta join between two real
tables from `uni_in_class` can run, because they share attribute names.

---

*Examples in Cases 1-4 were compiled and executed against this `RAImpl`; the
printed tables are real output.*
