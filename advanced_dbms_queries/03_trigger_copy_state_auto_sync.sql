-- ===================================================================
-- SCRIPT 03: TRIGGERS 3 & 4 DEMONSTRATION — State Auto-Synchronization
-- Triggers: UpdateCopyOnLoan (AFTER INSERT) & UpdateCopyOnReturn (AFTER UPDATE)
-- Rule Enforced : Automated physical book copy status synchronization.
-- ===================================================================
-- HOW TO RUN IN DBEAVER:
--   Run Step 1 through Step 6 sequentially using [Ctrl + Enter].
-- DOES IT CHANGE THE DATABASE?
--   Step 2 borrows copy 501 (turns to BORROWED), Step 4 returns it (turns to AVAILABLE),
--   and Step 6 cleans up the temporary demo loan.
--   Result: Copy 501 returns to AVAILABLE; database is left 100% clean.
--
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "These two triggers demonstrate automated side-effects and state synchronization.
--    In normalized databases, redundant updates invite anomalies. Instead of requiring
--    the application to execute two separate UPDATE queries, Trigger 3 automatically
--    marks the physical copy 'BORROWED' upon loan insertion, and Trigger 4
--    automatically releases the copy back to 'AVAILABLE' upon return date update."
-- ===================================================================

USE elibrary_db;

-- -------------------------------------------------------------------
-- STEP 1: Verify physical copy 501 (Computer Networks) is currently AVAILABLE
-- -------------------------------------------------------------------
SELECT copy_id, book_id, copy_code, status, rack_location 
FROM book_copies 
WHERE copy_id = 501;

-- -------------------------------------------------------------------
-- STEP 2: Issue a loan to Amit Kumar (user_id = 4) for copy 501
-- Notice: We do NOT write an UPDATE query for book_copies!
-- -------------------------------------------------------------------
INSERT INTO loans (user_id, copy_id, borrow_date, due_date, status)
VALUES (4, 501, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'ACTIVE');

-- -------------------------------------------------------------------
-- STEP 3: Verify Trigger 3 (UpdateCopyOnLoan) automatically marked copy 501 as BORROWED!
-- -------------------------------------------------------------------
SELECT copy_id, copy_code, status AS copy_status_after_borrow, rack_location
FROM book_copies 
WHERE copy_id = 501;

-- Also verify the loan is active:
SELECT loan_id, user_id, copy_id, borrow_date, due_date, status 
FROM loans 
WHERE user_id = 4 AND copy_id = 501 AND status = 'ACTIVE';

-- -------------------------------------------------------------------
-- STEP 4: Process Book Return (Librarian stamps return_date)
-- Notice: Again, we do NOT touch book_copies!
-- -------------------------------------------------------------------
UPDATE loans 
SET return_date = NOW(), status = 'RETURNED' 
WHERE user_id = 4 AND copy_id = 501 AND status = 'ACTIVE';

-- -------------------------------------------------------------------
-- STEP 5: Verify Trigger 4 (UpdateCopyOnReturn) automatically reverted copy 501 to AVAILABLE!
-- -------------------------------------------------------------------
SELECT copy_id, copy_code, status AS copy_status_after_return, rack_location
FROM book_copies 
WHERE copy_id = 501;

-- -------------------------------------------------------------------
-- STEP 6: SAFE CLEANUP (Removes temporary demo loan for Amit)
-- -------------------------------------------------------------------
DELETE FROM loans WHERE user_id = 4 AND copy_id = 501;

-- Final Verification: Copy 501 is AVAILABLE and Amit has clean loan record
SELECT copy_id, copy_code, status FROM book_copies WHERE copy_id = 501;
SELECT COUNT(*) AS amits_loans FROM loans WHERE user_id = 4;
