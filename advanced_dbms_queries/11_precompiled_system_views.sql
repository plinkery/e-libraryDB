-- ===================================================================
-- SCRIPT 11: DATABASE VIEWS & VIRTUAL RELATIONS
-- Syllabus Topics: Unit 4 (Database Views, Virtual Tables & Data Security)
-- ===================================================================
-- HOW TO RUN IN DBEAVER:
--   Press [Ctrl + Enter] on each query to inspect results individually,
--   or press [Alt + X] to execute all queries in sequence.
-- DOES IT CHANGE THE DATABASE?
--   NO (Read-only SELECT queries). Safe to run unlimited times.
-- ===================================================================

USE elibrary_db;

-- -------------------------------------------------------------------
-- VIEW 1: OverdueLoansView
-- Purpose: Dynamically computes overdue durations and daily fine amounts (₹1.00/day)
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "This view is a virtual relation that executes dynamic date arithmetic
--    (DATEDIFF(NOW(), due_date) * 1.00) whenever queried, ensuring librarians
--    always see up-to-the-minute overdue fine balances without stale stored columns."
-- -------------------------------------------------------------------
SELECT 
    loan_id,
    usn,
    student_name,
    student_phone,
    book_title,
    copy_code,
    borrow_date,
    due_date,
    days_overdue,
    calculated_fine
FROM OverdueLoansView;


-- -------------------------------------------------------------------
-- VIEW 2: HighDemandBooksView
-- Purpose: Identifies most circulated titles (borrowed 2 or more times)
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "HighDemandBooksView encapsulates the complex 3-table join between books,
--    book_copies, and loans with a post-aggregation HAVING clause (COUNT >= 2).
--    This provides logical data independence: reporting tools query the view directly
--    without needing to know the underlying table architecture."
-- -------------------------------------------------------------------
SELECT 
    book_id,
    title,
    isbn,
    branch_code,
    rating,
    total_borrows
FROM HighDemandBooksView
ORDER BY total_borrows DESC;


-- -------------------------------------------------------------------
-- VIEW 3: BranchFineSummaryView
-- Purpose: Financial Ledger auditing fine revenue collection by department
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "BranchFineSummaryView aggregates student accounts and fines per branch.
--    It provides role-based data security: finance auditors can inspect departmental
--    revenue without accessing sensitive personal student phone numbers or credentials."
-- -------------------------------------------------------------------
SELECT 
    branch_code,
    branch_name,
    registered_students,
    total_fines_levied,
    revenue_collected,
    pending_fines
FROM BranchFineSummaryView
ORDER BY branch_code ASC;
