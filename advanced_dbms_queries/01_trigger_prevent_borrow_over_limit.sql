-- ===================================================================
-- SCRIPT 01: TRIGGER 1 DEMONSTRATION — PreventBorrowOverLimit
-- Syllabus Topic: Unit 4 (Active Database Concepts & Stored Triggers)
-- Rule Enforced : Maximum 5 active book loans allowed per student.
-- ===================================================================
-- HOW TO RUN IN DBEAVER:
--   Run Step 1, Step 2, Step 3, and Step 4 one by one using [Ctrl + Enter].
-- DOES IT CHANGE THE DATABASE?
--   Step 2 inserts a temporary 5th loan, Step 3 tests the trigger block,
--   and Step 4 IMMEDIATELY cleans up and restores Priya to 4 loans.
--   Result: Database returns to original state; other scripts are NOT affected.
--
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "Sir/Ma'am, this is an Active Database Trigger (PreventBorrowOverLimit)
--    fired BEFORE INSERT ON loans. Priya Patel already holds 4 books. When we
--    issue a 5th book, it succeeds. But when we attempt to issue a 6th book,
--    the trigger counts active loans, evaluates active_count >= 5, and raises
--    SIGNAL SQLSTATE '45000' with our custom message. Even raw SQL in DBeaver
--    cannot violate this business constraint."
-- ===================================================================

USE elibrary_db;

-- -------------------------------------------------------------------
-- STEP 1: Verify Priya Patel (user_id = 3) currently has 4 active loans
-- -------------------------------------------------------------------
SELECT 
    u.user_id,
    u.usn,
    u.name AS student_name,
    COUNT(l.loan_id) AS active_loans_count
FROM users u
JOIN loans l ON u.user_id = l.user_id
WHERE u.user_id = 3 AND l.status = 'ACTIVE'
GROUP BY u.user_id, u.usn, u.name;

-- -------------------------------------------------------------------
-- STEP 2: Insert a 5th VALID loan for Priya (Copy 701 - Applied Thermodynamics)
-- Expected Result: Query OK, 1 row affected (5/5 quota reached)
-- -------------------------------------------------------------------
INSERT INTO loans (user_id, copy_id, borrow_date, due_date, status)
VALUES (3, 701, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'ACTIVE');

-- Verify Priya now has exactly 5 active loans:
SELECT 
    u.usn, u.name, COUNT(l.loan_id) AS current_active_loans
FROM users u
JOIN loans l ON u.user_id = l.user_id
WHERE u.user_id = 3 AND l.status = 'ACTIVE'
GROUP BY u.usn, u.name;

-- -------------------------------------------------------------------
-- STEP 3: ATTEMPT TO FORCE A 6TH LOAN FOR PRIYA (Copy 801 - Theory of Machines)
-- Expected Result: 💥 TRIGGER ABORTS INSERT WITH ERROR CODE 1644:
-- "Borrowing limit exceeded: Maximum 5 books allowed per student"
-- -------------------------------------------------------------------
INSERT INTO loans (user_id, copy_id, borrow_date, due_date, status)
VALUES (3, 801, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'ACTIVE');

-- -------------------------------------------------------------------
-- STEP 4: SAFE CLEANUP (Restores database to pristine initial state)
-- Removes the 5th test loan so Priya returns to exactly 4 loans.
-- -------------------------------------------------------------------
DELETE FROM loans WHERE user_id = 3 AND copy_id = 701;
UPDATE book_copies SET status = 'AVAILABLE' WHERE copy_id = 701;

-- Verification: Priya is back to 4 active loans
SELECT 
    u.usn, u.name, COUNT(l.loan_id) AS active_loans_after_cleanup
FROM users u
JOIN loans l ON u.user_id = l.user_id
WHERE u.user_id = 3 AND l.status = 'ACTIVE'
GROUP BY u.usn, u.name;
