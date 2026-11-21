-- ===================================================================
-- SCRIPT 06: AGGREGATES, GROUP BY, HAVING & CONDITIONAL CASE AGGREGATION
-- Syllabus Topics: Unit 3 (Aggregate γ) & Unit 4 (HAVING vs WHERE, CASE WHEN)
-- ===================================================================
-- HOW TO RUN IN DBEAVER:
--   Press [Ctrl + Enter] on each query to inspect results individually,
--   or press [Alt + X] to execute all queries in sequence.
-- DOES IT CHANGE THE DATABASE?
--   NO (Read-only SELECT queries). Safe to run unlimited times.
-- ===================================================================

USE elibrary_db;

-- -------------------------------------------------------------------
-- QUERY 3.1: Catalog Statistics with Multiple Aggregates
-- Operations: COUNT(), ROUND(AVG()), MIN(), MAX() over grouped partitions
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "This query demonstrates aggregate functions in relational algebra (gamma).
--    The GROUP BY clause partitions the relation into equivalence classes based
--    on engineering department, and applies statistical reduction functions:
--    COUNT for cardinality, AVG for mean score, and MIN/MAX for edition years."
-- -------------------------------------------------------------------
SELECT 
    br.branch_code,
    COUNT(bk.book_id) AS total_titles,
    ROUND(AVG(bk.rating), 2) AS average_rating,
    MIN(bk.pub_year) AS oldest_edition_year,
    MAX(bk.pub_year) AS newest_edition_year
FROM branches br
JOIN books bk ON br.branch_id = bk.branch_id
GROUP BY br.branch_code;


-- -------------------------------------------------------------------
-- QUERY 3.2: High-Demand Books with HAVING vs WHERE
-- Concept: Pre-aggregation tuple filtering (WHERE) vs post-aggregation group filtering (HAVING)
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "Examiners frequently ask for the difference between WHERE and HAVING.
--    In this query, WHERE filters individual loan records BEFORE aggregation
--    (only loans within the past 1 year). The HAVING clause evaluates the
--    aggregated group statistic AFTER GROUP BY, retaining only books whose
--    total borrow count is greater than or equal to 2."
-- -------------------------------------------------------------------
SELECT 
    b.book_id,
    b.title,
    b.isbn,
    COUNT(l.loan_id) AS times_borrowed
FROM books b
JOIN book_copies bc ON b.book_id = bc.book_id
JOIN loans l ON bc.copy_id = l.copy_id
WHERE l.borrow_date >= DATE_SUB(NOW(), INTERVAL 1 YEAR)
GROUP BY b.book_id, b.title, b.isbn
HAVING COUNT(l.loan_id) >= 2;


-- -------------------------------------------------------------------
-- QUERY 3.3: Department Revenue Ledger via Conditional Aggregation (CASE WHEN)
-- Concept: Inline pivot / multi-condition aggregation inside a single query pass
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "Rather than executing multiple queries to count registered students,
--    total fines, collected revenue, and outstanding pending fines, we use
--    conditional CASE expressions inside SUM(). This evaluates conditional
--    totals in a single sequential scan of the table."
-- -------------------------------------------------------------------
SELECT 
    br.branch_code,
    COUNT(DISTINCT u.user_id) AS registered_students,
    COUNT(f.fine_id) AS total_fines_levied,
    COALESCE(SUM(CASE WHEN f.status = 'PAID' THEN f.amount ELSE 0 END), 0.00) AS collected_revenue,
    COALESCE(SUM(CASE WHEN f.status = 'UNPAID' THEN f.amount ELSE 0 END), 0.00) AS pending_revenue
FROM branches br
LEFT JOIN users u ON br.branch_id = u.branch_id
LEFT JOIN fines f ON u.user_id = f.user_id
GROUP BY br.branch_code;
