-- ===================================================================
-- SCRIPT 00: ENVIRONMENT & DATABASE HEALTH CHECK
-- Course Code: 24CS520 - Database Management Systems (JSSSTU CSE)
-- Schema: elibrary_db
-- ===================================================================
-- HOW TO RUN IN DBEAVER:
--   Press [Ctrl + Enter] on each query, OR [Alt + X] to execute all.
-- DOES IT CHANGE THE DATABASE?
--   NO (Read-only SELECT queries). Safe to run unlimited times.
-- ===================================================================

USE elibrary_db;

-- 1. Check current database and connection status
SELECT 
    DATABASE() AS current_database,
    USER() AS connected_user,
    VERSION() AS mysql_version,
    NOW() AS current_system_time;

-- 2. Master Table Cardinality Audit (Row counts across all 14 tables)
SELECT 'branches' AS table_name, COUNT(*) AS row_count FROM branches
UNION ALL
SELECT 'subjects', COUNT(*) FROM subjects
UNION ALL
SELECT 'genres', COUNT(*) FROM genres
UNION ALL
SELECT 'publishers', COUNT(*) FROM publishers
UNION ALL
SELECT 'authors', COUNT(*) FROM authors
UNION ALL
SELECT 'users', COUNT(*) FROM users
UNION ALL
SELECT 'books', COUNT(*) FROM books
UNION ALL
SELECT 'book_copies', COUNT(*) FROM book_copies
UNION ALL
SELECT 'reservations', COUNT(*) FROM reservations
UNION ALL
SELECT 'loans', COUNT(*) FROM loans
UNION ALL
SELECT 'fines', COUNT(*) FROM fines
UNION ALL
SELECT 'fine_collections', COUNT(*) FROM fine_collections
UNION ALL
SELECT 'book_requests', COUNT(*) FROM book_requests
UNION ALL
SELECT 'request_votes', COUNT(*) FROM request_votes;

-- 3. Verify Active Stored Triggers compiled in MySQL
SELECT 
    TRIGGER_NAME,
    EVENT_MANIPULATION AS event_type,
    EVENT_OBJECT_TABLE AS target_table,
    ACTION_TIMING AS timing,
    CREATED
FROM information_schema.TRIGGERS
WHERE TRIGGER_SCHEMA = 'elibrary_db'
ORDER BY EVENT_OBJECT_TABLE, TRIGGER_NAME;

-- 4. Verify Compiled Database Views
SELECT 
    TABLE_NAME AS view_name,
    IS_UPDATABLE,
    CHECK_OPTION
FROM information_schema.VIEWS
WHERE TABLE_SCHEMA = 'elibrary_db';
