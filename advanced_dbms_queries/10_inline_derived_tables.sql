-- ===================================================================
-- SCRIPT 10: INLINE DERIVED TABLES (SUBQUERIES IN FROM CLAUSE)
-- Syllabus Topic: Unit 4 (Subqueries in the FROM Clause / Derived Relations)
-- ===================================================================
-- HOW TO RUN IN DBEAVER:
--   Press [Ctrl + Enter] to execute the query.
-- DOES IT CHANGE THE DATABASE?
--   NO (Read-only SELECT query). Safe to run unlimited times.
-- ===================================================================

USE elibrary_db;

-- -------------------------------------------------------------------
-- QUERY 7.1: Deviation from Department Average Rating
-- Concept: Inline temporary tables materialized dynamically in the FROM clause
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "This query showcases an Inline Derived Table (dept_avg). The subquery
--    in the FROM clause computes the average rating for each department first.
--    The outer query then joins this derived dataset back to the physical 'books'
--    relation to compute each book's deviation from its department benchmark."
-- -------------------------------------------------------------------
SELECT 
    b.title,
    br.branch_code,
    b.rating AS book_rating,
    dept_avg.avg_rating AS branch_average,
    ROUND(b.rating - dept_avg.avg_rating, 2) AS rating_deviation
FROM books b
JOIN branches br ON b.branch_id = br.branch_id
JOIN (
    SELECT branch_id, ROUND(AVG(rating), 2) AS avg_rating
    FROM books
    GROUP BY branch_id
) dept_avg ON b.branch_id = dept_avg.branch_id
ORDER BY rating_deviation DESC;
