-- ===================================================================
-- SCRIPT 09: SET OPERATIONS (UNION & UNION ALL)
-- Syllabus Topics: Unit 3 (Set Operations R ∪ S) & Unit 4 (UNION in SQL)
-- ===================================================================
-- HOW TO RUN IN DBEAVER:
--   Press [Ctrl + Enter] to execute the query.
-- DOES IT CHANGE THE DATABASE?
--   NO (Read-only SELECT query). Safe to run unlimited times.
-- ===================================================================

USE elibrary_db;

-- -------------------------------------------------------------------
-- QUERY 6.1: UNION ALL — Unified Patron Hold & Circulation List
-- Concept: Set Union of two relations with compatible schemas
-- 🗣️ WHAT TO SAY TO THE TEACHER:
--   "This query demonstrates the Set Union operation (UNION ALL).
--    It combines two heterogeneous patron commitment workflows—12-hour online
--    reservations and 30-day active loans—into a single unified dispatch stream.
--    UNION ALL is chosen over UNION because both relations track distinct IDs,
--    eliminating the computational overhead of deduplication sorting."
-- -------------------------------------------------------------------
SELECT 
    '12-Hour Reservation' AS commitment_type,
    r.user_id,
    u.usn,
    u.name AS student_name,
    bc.copy_code,
    r.expires_at AS deadline
FROM reservations r
JOIN users u ON r.user_id = u.user_id
JOIN book_copies bc ON r.copy_id = bc.copy_id
WHERE r.status = 'PENDING'

UNION ALL

SELECT 
    '30-Day Active Loan' AS commitment_type,
    l.user_id,
    u.usn,
    u.name AS student_name,
    bc.copy_code,
    l.due_date AS deadline
FROM loans l
JOIN users u ON l.user_id = u.user_id
JOIN book_copies bc ON l.copy_id = bc.copy_id
WHERE l.status = 'ACTIVE'

ORDER BY deadline ASC;
