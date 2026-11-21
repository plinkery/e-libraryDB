-- ===================================================================
-- SCRIPT 12: DATABASE RESET & PRISTINE SEED RESTORATION
-- Course Code: 24CS520 - Database Management Systems (JSSSTU CSE)
-- Schema: elibrary_db
-- ===================================================================
-- HOW TO RUN IN DBEAVER:
--   Press [Alt + X] to execute all queries in this file.
-- DOES IT CHANGE THE DATABASE?
--   YES! It resets all transactional and master tables to the exact
--   pristine seed state defined in the project syllabus. Use this if you
--   experimented manually and want a clean slate for your viva demo.
-- ===================================================================

USE elibrary_db;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Wipe all existing transactional and master data
TRUNCATE TABLE fine_collections;
TRUNCATE TABLE fines;
TRUNCATE TABLE loans;
TRUNCATE TABLE reservations;
TRUNCATE TABLE request_votes;
TRUNCATE TABLE book_requests;
TRUNCATE TABLE book_copies;
TRUNCATE TABLE books;
TRUNCATE TABLE subjects;
TRUNCATE TABLE users;
TRUNCATE TABLE authors;
TRUNCATE TABLE publishers;
TRUNCATE TABLE genres;
TRUNCATE TABLE branches;

-- 2. Restore Master Data: Branches
INSERT INTO branches (branch_id, branch_code, branch_name, hod_name) VALUES
(1, 'CSE', 'Computer Science and Engineering', 'Dr. Ramesh Kumar'),
(2, 'ECE', 'Electronics and Communication Engineering', 'Dr. Sunita Rao'),
(3, 'MECH', 'Mechanical Engineering', 'Dr. B.V. Somesh'),
(4, 'CIVIL', 'Civil Engineering', 'Dr. Arvind Sharma'),
(5, 'EEE', 'Electrical and Electronics Engineering', 'Dr. M.N. Prasad');

-- 3. Restore Master Data: Subjects
INSERT INTO subjects (subject_id, subject_code, subject_name, branch_id, semester) VALUES
(1, '24CS520', 'Database Management Systems', 1, 5),
(2, '24CS561', 'Java and J2EE Programming', 1, 5),
(3, '24CS530', 'Operating System Concepts', 1, 5),
(4, '24CS420', 'Design and Analysis of Algorithms', 1, 4),
(5, '24CS610', 'Computer Communication Networks', 1, 6),
(6, '24EC520', 'Digital Signal Processing', 2, 5),
(7, '24EC540', 'Electronic Devices and Circuits', 2, 5),
(8, '24ME510', 'Applied Thermodynamics', 3, 5),
(9, '24ME530', 'Theory of Machines', 3, 5),
(10, '24CV520', 'Structural Analysis and Mechanics', 4, 5);

-- 4. Restore Master Data: Genres
INSERT INTO genres (genre_id, genre_name) VALUES
(1, 'Core Syllabus Textbook'),
(2, 'Reference Guide & Manual'),
(3, 'Computer Science & AI'),
(4, 'Engineering Classic & History'),
(5, 'Philosophy & Self-Development');

-- 5. Restore Master Data: Publishers
INSERT INTO publishers (publisher_id, publisher_name) VALUES
(1, 'Pearson Education'),
(2, 'McGraw-Hill Education'),
(3, 'Oxford University Press'),
(4, 'MIT Press'),
(5, 'John Wiley & Sons'),
(6, 'Delphi Classics');

-- 6. Restore Master Data: Authors
INSERT INTO authors (author_id, author_name, country) VALUES
(1, 'Herbert Schildt', 'USA'),
(2, 'Elmasri and Navathe', 'USA'),
(3, 'Silberschatz and Galvin', 'USA'),
(4, 'Thomas H. Cormen', 'USA'),
(5, 'Andrew S. Tanenbaum', 'Netherlands'),
(6, 'John G. Proakis', 'USA'),
(7, 'P.K. Nag', 'India'),
(8, 'Joseph E. Shigley', 'USA'),
(9, 'Frederick P. Brooks', 'USA'),
(10, 'Robert M. Pirsig', 'USA');

-- 7. Restore Users (Librarian + 3 Students)
INSERT INTO users (user_id, usn, name, email, password, role, branch_id, semester, phone) VALUES
(1, '01JSTLIB001', 'Mrs. Shwetha Rao (Chief Librarian)', 'librarian@college.edu', 'admin123', 'LIBRARIAN', 1, NULL, '9876543210'),
(2, '01JST24UCS001', 'Rahul Sharma (Student 1)', 'rahul@student.edu', 'pass123', 'STUDENT', 1, 5, '9876543211'),
(3, '01JST24UCS002', 'Priya Patel (Student 2)', 'priya@student.edu', 'pass123', 'STUDENT', 2, 5, '9876543212'),
(4, '01JST24UCS003', 'Amit Kumar (Student 3)', 'amit@student.edu', 'pass123', 'STUDENT', 3, 5, '9876543213');

-- 8. Restore Books Catalog
INSERT INTO books (book_id, title, isbn, branch_id, subject_id, genre_id, publisher_id, author_id, edition, pub_year, rating, description) VALUES
(1, 'Java: The Complete Reference', '978-1260440249', 1, 2, 1, 2, 1, '11th Edition', 2021, 4.8, 'Comprehensive syllabus guide to core Java, OOP, collections, multithreading and database access.'),
(2, 'Fundamentals of Database Systems', '978-0133970777', 1, 1, 1, 1, 2, '7th Edition', 2020, 4.9, 'Standard DBMS textbook covering ER models, relational algebra, 3NF normalization, and SQL.'),
(3, 'Operating System Concepts', '978-1118063330', 1, 3, 1, 5, 3, '10th Edition', 2021, 4.7, 'The definitive dinosaur OS reference book covering memory, processes, synchronization.'),
(4, 'Introduction to Algorithms (CLRS)', '978-0262033848', 1, 4, 2, 4, 4, '4th Edition', 2022, 4.9, 'Gold standard textbook for algorithm design, dynamic programming, and data structures.'),
(5, 'Computer Networks', '978-0132126953', 1, 5, 1, 1, 5, '5th Edition', 2019, 4.6, 'Networking principles, OSI reference layers, TCP/IP protocols, and routing algorithms.'),
(6, 'Digital Signal Processing: Principles', '978-0131873742', 2, 6, 1, 1, 6, '4th Edition', 2020, 4.7, 'Discrete-time signals, Z-transform, FFT, filter design for Electronics engineers.'),
(7, 'Engineering Thermodynamics', '978-9352606429', 3, 8, 1, 2, 7, '6th Edition', 2021, 4.6, 'Thermodynamic cycles, entropy, steam tables, refrigeration and heat engines.'),
(8, 'Theory of Machines and Mechanisms', '978-0195371239', 3, 9, 1, 3, 8, '5th Edition', 2019, 4.5, 'Kinematics of linkages, cams, gears, and dynamic balancing of machines.'),
(9, 'The Mythical Man-Month', '978-0201835953', 1, 2, 4, 1, 9, 'Anniversary Ed', 2018, 4.8, 'Classic essays on software engineering, scheduling fallacies, and project complexity.'),
(10, 'Zen and the Art of Motorcycle Maintenance', '978-0060589462', 3, 8, 5, 6, 10, 'Deluxe Edition', 2017, 4.9, 'Celebrated philosophical inquiry into quality, rationality, and technology.');

-- 9. Restore Physical Book Copies
INSERT INTO book_copies (copy_id, book_id, copy_code, status, rack_location) VALUES
(101, 1, 'CSE-JAVA-01', 'AVAILABLE', 'Stack CS-01'),
(102, 1, 'CSE-JAVA-02', 'AVAILABLE', 'Stack CS-01'),
(103, 1, 'CSE-JAVA-03', 'AVAILABLE', 'Stack CS-01'),
(201, 2, 'CSE-DBMS-01', 'AVAILABLE', 'Stack CS-02'),
(202, 2, 'CSE-DBMS-02', 'AVAILABLE', 'Stack CS-02'),
(301, 3, 'CSE-OS-01',   'AVAILABLE', 'Stack CS-03'),
(302, 3, 'CSE-OS-02',   'AVAILABLE', 'Stack CS-03'),
(401, 4, 'CSE-ALGO-01', 'AVAILABLE', 'Stack CS-04'),
(501, 5, 'CSE-NET-01',  'AVAILABLE', 'Stack CS-05'),
(601, 6, 'ECE-DSP-01',  'AVAILABLE', 'Stack EC-01'),
(701, 7, 'MECH-THERM-01', 'AVAILABLE', 'Stack ME-01'),
(801, 8, 'MECH-TOM-01', 'AVAILABLE', 'Stack ME-02'),
(901, 9, 'GEN-MYTH-01', 'AVAILABLE', 'Stack GN-01'),
(1001, 10, 'GEN-ZEN-01', 'AVAILABLE', 'Stack GN-02'),
(104, 1, 'CSE-JAVA-04', 'BORROWED', 'Stack CS-01'),
(203, 2, 'CSE-DBMS-03', 'BORROWED', 'Stack CS-02'),
(402, 4, 'CSE-ALGO-02', 'BORROWED', 'Stack CS-04'),
(602, 6, 'ECE-DSP-02',  'BORROWED', 'Stack EC-01');

-- 10. Restore Circulation Loans (Priya holds 4 loans across CSE and ECE)
INSERT INTO loans (loan_id, user_id, copy_id, borrow_date, due_date, status) VALUES
(2, 3, 104, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 25 DAY), 'ACTIVE'),
(3, 3, 203, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_ADD(NOW(), INTERVAL 26 DAY), 'ACTIVE'),
(4, 3, 402, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 27 DAY), 'ACTIVE'),
(5, 3, 602, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 28 DAY), 'ACTIVE');

-- 11. Restore Student Book Requests & Votes
INSERT INTO book_requests (request_id, title, author, branch_id, genre_id, suggested_by, votes_count, status) VALUES
(1, 'Designing Data-Intensive Applications', 'Martin Kleppmann', 1, 3, 2, 1, 'PENDING');

INSERT INTO request_votes (request_id, user_id) VALUES
(1, 2);

SET FOREIGN_KEY_CHECKS = 1;

-- Final Verification: Verify database is restored
SELECT 'Database successfully restored to pristine seed state!' AS status_message;
SELECT COUNT(*) AS total_books FROM books;
SELECT COUNT(*) AS total_copies FROM book_copies;
SELECT COUNT(*) AS total_active_loans FROM loans WHERE status = 'ACTIVE';
