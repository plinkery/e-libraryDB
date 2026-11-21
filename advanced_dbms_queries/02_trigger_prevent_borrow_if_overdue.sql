-- ===================================================================
-- SCRIPT 02: TRIGGER 2 DEMONSTRATION — PreventBorrowIfOverdue
-- Syllabus Topic: Unit 4 (Active Database Integrity Rules)
-- Rule Enforced : Borrowing blocked if student has overdue books or unpaid fines.
-- ===================================================================
-- HOW TO RUN IN DBEAVER:
--   Run Step 1, Step 2, Step 3, and Step 4 one by one using [Ctrl + Enter].
-- DOES IT CHANGE THE DATABASE?
--   Step 2 inserts a temporary test fine, Step 3 verifies borrowing is blocked,
--   and Step 4 immediately cleans up the test fine.
--   Result: Database returns to original state; other scripts are NOT affected.
--
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "This trigger (PreventBorrowIfOverdue) enforces academic accountability.
--    If a student has a fine with status = 'UNPAID', the trigger intercepts
--    any incoming checkout on table 'loans' and raises SQLSTATE '45000'.
--    This prevents students with unpaid dues from checking out more assets."
-- ===================================================================

USE elibrary_db;

-- -------------------------------------------------------------------
-- STEP 1: Verify Rahul Sharma (user_id = 2) currently has 0 unpaid fines
-- -------------------------------------------------------------------
SELECT 
    u.user_id,
    u.usn,
    u.name AS student_name,
    COUNT(f.fine_id) AS current_unpaid_fines
FROM users u
LEFT JOIN fines f ON u.user_id = f.user_id AND f.status = 'UNPAID'
WHERE u.user_id = 2
GROUP BY u.user_id, u.usn, u.name;

-- -------------------------------------------------------------------
-- STEP 2: Create a temporary unpaid fine of ₹10.00 for Rahul (user_id = 2)
-- Note: Loan ID 2 exists in seed data
-- -------------------------------------------------------------------
INSERT INTO fines (loan_id, user_id, days_overdue, amount, status)
VALUES (2, 2, 10, 10.00, 'UNPAID');

-- Verify the unpaid fine record is registered:
SELECT * FROM fines WHERE user_id = 2 AND status = 'UNPAID';

-- -------------------------------------------------------------------
-- STEP 3: ATTEMPT TO ISSUE A BOOK TO RAHUL (Copy 501 - Computer Networks)
-- Expected Result: 💥 TRIGGER ABORTS INSERT WITH ERROR CODE 1644:
-- "Borrowing blocked: Student has overdue books or unpaid fines"
-- -------------------------------------------------------------------
INSERT INTO loans (user_id, copy_id, borrow_date, due_date, status)
VALUES (2, 501, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'ACTIVE');

-- -------------------------------------------------------------------
-- STEP 4: SAFE CLEANUP (Removes test fine, restores Rahul's clean record)
-- -------------------------------------------------------------------
DELETE FROM fines WHERE user_id = 2 AND amount = 10.00;

-- Verification: Rahul is back to 0 unpaid fines
SELECT 
    u.usn, u.name, COUNT(f.fine_id) AS unpaid_fines_after_cleanup
FROM users u
LEFT JOIN fines f ON u.user_id = f.user_id AND f.status = 'UNPAID'
WHERE u.user_id = 2
GROUP BY u.usn, u.name;
