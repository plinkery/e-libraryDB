-- ===================================================================
-- SCRIPT 05: SELF-JOINS & CARTESIAN CROSS JOINS
-- Syllabus Topics: Unit 3 (Cartesian Product R x S) & Unit 4 (Self-Joins)
-- ===================================================================
-- HOW TO RUN IN DBEAVER:
--   Press [Ctrl + Enter] on each query to inspect results individually,
--   or press [Alt + X] to execute all queries in sequence.
-- DOES IT CHANGE THE DATABASE?
--   NO (Read-only SELECT queries). Safe to run unlimited times.
-- ===================================================================

USE elibrary_db;

-- -------------------------------------------------------------------
-- QUERY 2.1: Self-Join Comparing Book Ratings Within the Same Department
-- Concept: Joining a relation with itself using multiple tuple variables
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "This is a Self-Join. We alias the 'books' relation twice (b1 and b2)
--    as two separate tuple variables. The join condition binds them to the
--    same academic branch (b1.branch_id = b2.branch_id) while asserting
--    b1.rating > b2.rating. This allows us to compare books within the same
--    department partition without requiring nested subqueries."
-- -------------------------------------------------------------------
SELECT 
    b1.title AS higher_rated_book,
    b1.rating AS higher_rating,
    b2.title AS lower_rated_book,
    b2.rating AS lower_rating,
    br.branch_code
FROM books b1
JOIN books b2 ON b1.branch_id = b2.branch_id AND b1.rating > b2.rating
JOIN branches br ON b1.branch_id = br.branch_id
ORDER BY br.branch_code, (b1.rating - b2.rating) DESC;


-- -------------------------------------------------------------------
-- QUERY 2.2: Cartesian Cross Join (R x S) for Complete Department-Genre Matrix
-- Relational Algebra:
--   π[branch_code](branches) x π[genre_name](genres)
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "This demonstrates the fundamental Cartesian Product operation (CROSS JOIN).
--    It computes the complete combinatorial cross-product between branches and genres
--    (5 branches x 5 genres = 25 pairs). Joining this matrix to 'books' using a LEFT JOIN
--    reveals gaps where specific departments have 0 books in specific genres (e.g. MECH
--    having 0 books in Computer Science & AI)."
-- -------------------------------------------------------------------
SELECT 
    br.branch_code,
    g.genre_name,
    COUNT(bk.book_id) AS books_available
FROM branches br
CROSS JOIN genres g
LEFT JOIN books bk ON bk.branch_id = br.branch_id AND bk.genre_id = g.genre_id
GROUP BY br.branch_code, g.genre_name
ORDER BY br.branch_code, books_available ASC;
