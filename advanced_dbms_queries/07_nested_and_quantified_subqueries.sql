-- ===================================================================
-- SCRIPT 07: NESTED & QUANTIFIED SUBQUERIES (IN, NOT IN, ALL, ANY)
-- Syllabus Topics: Unit 4 (Subqueries, Set Membership & Quantified Comparisons)
-- ===================================================================
-- HOW TO RUN IN DBEAVER:
--   Press [Ctrl + Enter] on each query to inspect results individually,
--   or press [Alt + X] to execute all queries in sequence.
-- DOES IT CHANGE THE DATABASE?
--   NO (Read-only SELECT queries). Safe to run unlimited times.
-- ===================================================================

USE elibrary_db;

-- -------------------------------------------------------------------
-- QUERY 4.1: Uncorrelated Nested Subquery with IN
-- Concept: Set Membership Test
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "This query uses the IN set-membership operator. The inner subquery
--    executes independently once, generating a scalar set of branch IDs
--    that offer Database or Java subjects. The outer query matches students
--    enrolled in any department belonging to that candidate set."
-- -------------------------------------------------------------------
SELECT usn, name, email, semester
FROM users
WHERE role = 'STUDENT'
  AND branch_id IN (
      SELECT branch_id 
      FROM subjects 
      WHERE subject_name LIKE '%Database%' OR subject_name LIKE '%Java%'
  );


-- -------------------------------------------------------------------
-- QUERY 4.2: Subquery with NOT IN (Relational Set Difference R - S)
-- Concept: Identifying unreferenced / dormant records
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "This implements Relational Set Difference (R - S). The inner query
--    produces the projection of all book IDs that have ever been borrowed.
--    The outer query selects books whose IDs do NOT appear in that set.
--    This isolates dormant catalog titles that have never circulated."
-- -------------------------------------------------------------------
SELECT book_id, title, isbn, edition
FROM books
WHERE book_id NOT IN (
    SELECT DISTINCT bc.book_id
    FROM book_copies bc
    JOIN loans l ON bc.copy_id = l.copy_id
);


-- -------------------------------------------------------------------
-- QUERY 4.3: Quantified Subquery using > ALL
-- Concept: Strict Universal Comparison against an entire set
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "The > ALL operator compares an outer attribute against every element
--    in the subquery set. Here, the subquery retrieves all ratings of Mechanical
--    Engineering books. The condition evaluates to TRUE only if a book's rating
--    is strictly greater than the maximum rating in the MECH department."
-- -------------------------------------------------------------------
SELECT title, rating, pub_year
FROM books
WHERE rating > ALL (
    SELECT rating 
    FROM books 
    WHERE branch_id = (SELECT branch_id FROM branches WHERE branch_code = 'MECH')
);


-- -------------------------------------------------------------------
-- QUERY 4.4: Quantified Subquery using > ANY (or > SOME)
-- Concept: Existential Comparison against at least one element
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "The > ANY (or > SOME) operator evaluates to TRUE if the value is greater
--    than at least one value returned by the subquery. Here, it matches any book
--    whose rating exceeds the rating of at least one book in Computer Science."
-- -------------------------------------------------------------------
SELECT title, rating, pub_year
FROM books
WHERE rating > ANY (
    SELECT rating 
    FROM books 
    WHERE branch_id = (SELECT branch_id FROM branches WHERE branch_code = 'CSE')
);
