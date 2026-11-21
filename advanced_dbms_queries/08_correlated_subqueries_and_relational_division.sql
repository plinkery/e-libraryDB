-- ===================================================================
-- SCRIPT 08: CORRELATED SUBQUERIES & RELATIONAL DIVISION (A ÷ B)
-- Syllabus Topics: Unit 3 (Relational Division A ÷ B) & Unit 4 (EXISTS / NOT EXISTS)
-- ===================================================================
-- HOW TO RUN IN DBEAVER:
--   Press [Ctrl + Enter] on each query to inspect results individually,
--   or press [Alt + X] to execute all queries in sequence.
-- DOES IT CHANGE THE DATABASE?
--   NO (Read-only SELECT queries). Safe to run unlimited times.
-- ===================================================================

USE elibrary_db;

-- -------------------------------------------------------------------
-- QUERY 5.1: Correlated Subquery with EXISTS
-- Concept: Row-by-row correlation with boolean short-circuit evaluation
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "Unlike an uncorrelated subquery that runs once, this correlated subquery
--    executes for each outer tuple in authors, binding a.author_id into the inner query.
--    The EXISTS operator evaluates whether at least one active loan exists for that author,
--    short-circuiting as soon as the first match is found for optimum query execution."
-- -------------------------------------------------------------------
SELECT a.author_id, a.author_name, a.country
FROM authors a
WHERE EXISTS (
    SELECT 1 
    FROM books b
    JOIN book_copies bc ON b.book_id = bc.book_id
    JOIN loans l ON bc.copy_id = l.copy_id
    WHERE b.author_id = a.author_id AND l.status = 'ACTIVE'
);


-- -------------------------------------------------------------------
-- QUERY 5.2: Relational Division (A ÷ B) Using Double NOT EXISTS
-- Syllabus Topic: Unit 3 & Unit 4 (Universal Quantification: "FOR ALL")
-- Question: "Find all students who have borrowed books from EVERY engineering branch!"
--
-- Relational Algebra:
--   Result = π[user_id, usn, name](users) ÷ π[branch_id](σ[branch_code ∈ {'CSE','ECE','MECH'}](branches))
--
-- Expected Output:
--   usn            | name        | enrolled_department
--   01JST24UCS002  | Priya Patel | Electronics and Communication Engineering
--
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "This is Relational Division (A ÷ B). Because SQL lacks a native 'DIVIDE BY'
--    keyword, relational division is implemented via double NOT EXISTS, expressing
--    universal quantification: 'Find students such that there does NOT exist a
--    required branch for which there does NOT exist a loan by this student.'
--    Student Priya Patel has borrowed books from CSE, ECE, and MECH, satisfying
--    the universal division condition!"
-- -------------------------------------------------------------------
SELECT u.usn, u.name, br.branch_name AS enrolled_department
FROM users u
JOIN branches br ON u.branch_id = br.branch_id
WHERE u.role = 'STUDENT'
  AND NOT EXISTS (
      -- Set B: All required engineering branches
      SELECT b.branch_id 
      FROM branches b 
      WHERE b.branch_code IN ('CSE', 'ECE', 'MECH')
      AND NOT EXISTS (
          -- Set A: Branches from which this student has borrowed
          SELECT l.loan_id 
          FROM loans l
          JOIN book_copies bc ON l.copy_id = bc.copy_id
          JOIN books bk ON bc.book_id = bk.book_id
          WHERE l.user_id = u.user_id 
            AND bk.branch_id = b.branch_id
      )
  );
