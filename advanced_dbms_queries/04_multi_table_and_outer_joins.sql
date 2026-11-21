-- ===================================================================
-- SCRIPT 04: MULTI-TABLE JOINS & OUTER JOINS (INNER, LEFT, RIGHT)
-- Syllabus Topics: Unit 3 (Relational Algebra Joins) & Unit 4 (SQL Joins)
-- ===================================================================
-- HOW TO RUN IN DBEAVER:
--   Press [Ctrl + Enter] on each query to inspect results individually,
--   or press [Alt + X] to execute all queries in sequence.
-- DOES IT CHANGE THE DATABASE?
--   NO (Read-only SELECT queries). Safe to run unlimited times.
-- ===================================================================

USE elibrary_db;

-- -------------------------------------------------------------------
-- QUERY 1.1: 5-Table Master Circulation Inner Join
-- Relational Algebra:
--   π[loan_id, usn, student_name, title, copy_code, branch_code, due_date](
--       loans ⋈ users ⋈ book_copies ⋈ books ⋈ branches
--   )
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "This query demonstrates a 5-table relational equi-join. It links the
--    circulation transaction fact relation 'loans' with 'users', 'book_copies',
--    'books', and 'branches'. This illustrates Third Normal Form: attributes are
--    decoupled across normalized relations, and SQL joins reconstruct complete
--    patron checkout profiles dynamically."
-- -------------------------------------------------------------------
SELECT 
    l.loan_id,
    u.usn,
    u.name AS student_name,
    bk.title AS book_title,
    bc.copy_code,
    br.branch_code AS book_dept,
    l.borrow_date,
    l.due_date,
    l.status AS loan_status
FROM loans l
JOIN users u ON l.user_id = u.user_id
JOIN book_copies bc ON l.copy_id = bc.copy_id
JOIN books bk ON bc.book_id = bk.book_id
JOIN branches br ON bk.branch_id = br.branch_id
WHERE l.status = 'ACTIVE'
ORDER BY l.due_date ASC;


-- -------------------------------------------------------------------
-- QUERY 1.2: Left Outer Join for Comprehensive Department Audit (R ⟕ S)
-- Relational Algebra:
--   branches ⟕[branches.branch_id = books.branch_id] books
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "An INNER JOIN would eliminate departments like Civil Engineering (CIVIL)
--    if they had zero registered books. A LEFT OUTER JOIN guarantees that all
--    tuples from 'branches' are preserved. Unmatched attributes from 'books' are
--    padded with NULL, allowing COUNT(bk.book_id) to return 0 instead of dropping
--    the department from the institutional audit."
-- -------------------------------------------------------------------
SELECT 
    br.branch_code,
    br.branch_name,
    br.hod_name,
    COUNT(bk.book_id) AS total_catalog_titles,
    COALESCE(ROUND(AVG(bk.rating), 2), 0.0) AS avg_rating
FROM branches br
LEFT JOIN books bk ON br.branch_id = bk.branch_id
GROUP BY br.branch_id, br.branch_code, br.branch_name, br.hod_name
ORDER BY total_catalog_titles DESC;


-- -------------------------------------------------------------------
-- QUERY 1.3: Right Outer Join Showing Courses Lacking Library Books (R ⟖ S)
-- Concept: Anti-Join Pattern for Curriculum Gap Analysis
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "Here we use a RIGHT OUTER JOIN combined with a WHERE bk.book_id IS NULL filter.
--    This preserves all curriculum subjects from the right table 'subjects' and
--    filters out any courses that lack a designated reference textbook in our catalog.
--    This is the classic SQL Anti-Join pattern for academic resource auditing."
-- -------------------------------------------------------------------
SELECT 
    s.subject_code,
    s.subject_name,
    s.semester,
    bk.title AS mapped_book_title
FROM books bk
RIGHT JOIN subjects s ON bk.subject_id = s.subject_id
WHERE bk.book_id IS NULL;
