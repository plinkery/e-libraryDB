-- ===================================================================
-- E-LIBRARY DATABASE MANAGEMENT SYSTEM (DBMS SCHEMA)
-- Course: 24CS520 DBMS & 24CS561 Java/J2EE
-- Features: 3NF Relational Tables, Branch/Subject Hierarchy, 
--           Ratings, Triggers, Views, and Advanced Lab Queries
-- ===================================================================

DROP DATABASE IF EXISTS elibrary_db;
CREATE DATABASE elibrary_db;
USE elibrary_db;

-- 1. BRANCHES / DEPARTMENTS TABLE (CSE, MECH, ECE, CIVIL, EEE)
CREATE TABLE branches (
    branch_id INT PRIMARY KEY AUTO_INCREMENT,
    branch_code VARCHAR(20) NOT NULL UNIQUE, -- e.g. 'CSE', 'MECH', 'ECE'
    branch_name VARCHAR(100) NOT NULL,
    hod_name VARCHAR(100) NOT NULL
);

-- 2. SUBJECTS / COURSES TABLE (e.g. 24CS520 DBMS, 24CS561 Java)
CREATE TABLE subjects (
    subject_id INT PRIMARY KEY AUTO_INCREMENT,
    subject_code VARCHAR(30) NOT NULL UNIQUE,
    subject_name VARCHAR(100) NOT NULL,
    branch_id INT NOT NULL,
    semester INT NOT NULL,
    FOREIGN KEY (branch_id) REFERENCES branches(branch_id) ON DELETE CASCADE
);

-- 3. GENRES TABLE (Textbook, Reference, Novel, Technology, etc.)
CREATE TABLE genres (
    genre_id INT PRIMARY KEY AUTO_INCREMENT,
    genre_name VARCHAR(100) NOT NULL UNIQUE
);

-- 4. PUBLISHERS TABLE
CREATE TABLE publishers (
    publisher_id INT PRIMARY KEY AUTO_INCREMENT,
    publisher_name VARCHAR(150) NOT NULL
);

-- 5. AUTHORS TABLE
CREATE TABLE authors (
    author_id INT PRIMARY KEY AUTO_INCREMENT,
    author_name VARCHAR(100) NOT NULL,
    country VARCHAR(100) DEFAULT 'International'
);

-- 6. USERS TABLE (Identified by USN: University Serial Number)
CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    usn VARCHAR(30) UNIQUE NOT NULL, -- e.g. '01JST24UCS001', '01JSTLIB001'
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    role ENUM('STUDENT', 'LIBRARIAN') NOT NULL,
    branch_id INT NULL,
    semester INT NULL,
    phone VARCHAR(20),
    reservation_blocked_until DATETIME NULL, -- Applied if 12-hour pickup is missed
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (branch_id) REFERENCES branches(branch_id) ON DELETE SET NULL
);

-- 7. BOOKS TABLE (Catalog / Abstract Book metadata with Ratings)
CREATE TABLE books (
    book_id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    isbn VARCHAR(50) UNIQUE NOT NULL,
    branch_id INT,
    subject_id INT,
    genre_id INT,
    publisher_id INT,
    author_id INT,
    edition VARCHAR(30) DEFAULT '1st Edition',
    pub_year INT DEFAULT 2022,
    rating DECIMAL(2, 1) DEFAULT 4.5, -- 1.0 to 5.0 scale (like Sailors lab rating)
    description TEXT,
    FOREIGN KEY (branch_id) REFERENCES branches(branch_id) ON DELETE SET NULL,
    FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE SET NULL,
    FOREIGN KEY (genre_id) REFERENCES genres(genre_id) ON DELETE SET NULL,
    FOREIGN KEY (publisher_id) REFERENCES publishers(publisher_id) ON DELETE SET NULL,
    FOREIGN KEY (author_id) REFERENCES authors(author_id) ON DELETE SET NULL
);

-- 8. BOOK_COPIES TABLE (Tracks Physical Copies of each Book)
CREATE TABLE book_copies (
    copy_id INT PRIMARY KEY AUTO_INCREMENT,
    book_id INT NOT NULL,
    copy_code VARCHAR(50) UNIQUE NOT NULL, -- Barcode/Accession number
    status ENUM('AVAILABLE', 'RESERVED', 'BORROWED') DEFAULT 'AVAILABLE',
    rack_location VARCHAR(50),
    FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE
);

-- 9. RESERVATIONS TABLE (12-Hour Online Reservation Window)
CREATE TABLE reservations (
    reservation_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    copy_id INT NOT NULL,
    reserved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL, -- reserved_at + 12 Hours
    status ENUM('PENDING', 'FULFILLED', 'EXPIRED', 'CANCELLED') DEFAULT 'PENDING',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (copy_id) REFERENCES book_copies(copy_id) ON DELETE CASCADE
);

-- 10. LOANS TABLE (Book Borrowing Transactions - 1 Month / 30 Days Due)
CREATE TABLE loans (
    loan_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    copy_id INT NOT NULL,
    borrow_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date DATETIME NOT NULL, -- borrow_date + 30 Days
    return_date DATETIME NULL,
    status ENUM('ACTIVE', 'RETURNED') DEFAULT 'ACTIVE',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (copy_id) REFERENCES book_copies(copy_id) ON DELETE CASCADE
);

-- 11. FINES TABLE (Tracks Overdue Charges: Rs. 1 per day overdue)
CREATE TABLE fines (
    fine_id INT PRIMARY KEY AUTO_INCREMENT,
    loan_id INT NOT NULL,
    user_id INT NOT NULL,
    days_overdue INT DEFAULT 0,
    amount DECIMAL(8, 2) DEFAULT 0.00,
    status ENUM('UNPAID', 'PAID') DEFAULT 'UNPAID',
    assessed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    paid_at DATETIME NULL,
    FOREIGN KEY (loan_id) REFERENCES loans(loan_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 12. FINE_COLLECTIONS TABLE (Tracks Institutional Revenue Collected from Fines)
CREATE TABLE fine_collections (
    collection_id INT PRIMARY KEY AUTO_INCREMENT,
    fine_id INT NOT NULL,
    user_id INT NOT NULL,
    amount_paid DECIMAL(8, 2) NOT NULL,
    collected_by INT NOT NULL, -- Librarian user_id
    collection_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (fine_id) REFERENCES fines(fine_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (collected_by) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 13. BOOK_REQUESTS TABLE (Suggestions for Books Not Yet in Library)
CREATE TABLE book_requests (
    request_id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(150),
    branch_id INT NULL,
    genre_id INT NULL,
    suggested_by INT NOT NULL,
    votes_count INT DEFAULT 1,
    status ENUM('PENDING', 'PROCURED', 'REJECTED') DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (suggested_by) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (branch_id) REFERENCES branches(branch_id) ON DELETE SET NULL,
    FOREIGN KEY (genre_id) REFERENCES genres(genre_id) ON DELETE SET NULL
);

-- 14. REQUEST_VOTES TABLE (Supports Voting and Unvoting)
CREATE TABLE request_votes (
    request_id INT NOT NULL,
    user_id INT NOT NULL,
    voted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (request_id, user_id),
    FOREIGN KEY (request_id) REFERENCES book_requests(request_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 15. AUDIT_LOGS TABLE (Accountability log for main transactions)
CREATE TABLE audit_logs (
    log_id INT PRIMARY KEY AUTO_INCREMENT,
    action VARCHAR(100) NOT NULL,
    query_executed TEXT NOT NULL,
    performed_by VARCHAR(100),
    log_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ===================================================================
-- TRIGGERS (DBMS Lab Concept Demonstrations)
-- ===================================================================

-- TRIGGER 1: Prevent borrowing if student already has 5 active loans
DELIMITER //
CREATE TRIGGER PreventBorrowOverLimit
BEFORE INSERT ON loans
FOR EACH ROW
BEGIN
    DECLARE active_count INT;
    SELECT COUNT(*) INTO active_count 
    FROM loans 
    WHERE user_id = NEW.user_id AND status = 'ACTIVE';
    
    IF active_count >= 5 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Borrowing limit exceeded: Maximum 5 books allowed per student';
    END IF;
END;
//
DELIMITER ;

-- TRIGGER 2: Prevent borrowing if student has UNPAID fines or OVERDUE books
DELIMITER //
CREATE TRIGGER PreventBorrowIfOverdue
BEFORE INSERT ON loans
FOR EACH ROW
BEGIN
    DECLARE overdue_count INT;
    DECLARE unpaid_fine_count INT;

    SELECT COUNT(*) INTO overdue_count 
    FROM loans 
    WHERE user_id = NEW.user_id 
      AND status = 'ACTIVE' 
      AND NOW() > due_date;

    SELECT COUNT(*) INTO unpaid_fine_count
    FROM fines
    WHERE user_id = NEW.user_id AND status = 'UNPAID';
      
    IF overdue_count > 0 OR unpaid_fine_count > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Borrowing blocked: Student has overdue books or unpaid fines';
    END IF;
END;
//
DELIMITER ;

-- TRIGGER 3: Automatically mark book copy as BORROWED upon loan creation
DELIMITER //
CREATE TRIGGER UpdateCopyOnLoan
AFTER INSERT ON loans
FOR EACH ROW
BEGIN
    UPDATE book_copies 
    SET status = 'BORROWED' 
    WHERE copy_id = NEW.copy_id;
END;
//
DELIMITER ;

-- TRIGGER 4: Automatically mark book copy as AVAILABLE upon return
DELIMITER //
CREATE TRIGGER UpdateCopyOnReturn
AFTER UPDATE ON loans
FOR EACH ROW
BEGIN
    IF NEW.return_date IS NOT NULL AND OLD.return_date IS NULL THEN
        UPDATE book_copies 
        SET status = 'AVAILABLE' 
        WHERE copy_id = NEW.copy_id;
    END IF;
END;
//
DELIMITER ;

-- ===================================================================
-- VIEWS (DBMS Lab Concept Demonstrations)
-- ===================================================================

-- VIEW 1: Overdue Loans with Fine calculation (Rs. 1 per day)
CREATE OR REPLACE VIEW OverdueLoansView AS
SELECT 
    l.loan_id,
    u.user_id,
    u.usn,
    u.name AS student_name,
    u.phone AS student_phone,
    b.title AS book_title,
    bc.copy_code,
    l.borrow_date,
    l.due_date,
    DATEDIFF(NOW(), l.due_date) AS days_overdue,
    (DATEDIFF(NOW(), l.due_date) * 1.00) AS calculated_fine
FROM loans l
JOIN users u ON l.user_id = u.user_id
JOIN book_copies bc ON l.copy_id = bc.copy_id
JOIN books b ON bc.book_id = b.book_id
WHERE l.status = 'ACTIVE' AND NOW() > l.due_date;

-- VIEW 2: High Demand Books (Borrowed 2 or more times)
CREATE OR REPLACE VIEW HighDemandBooksView AS
SELECT 
    b.book_id,
    b.title,
    b.isbn,
    br.branch_code,
    b.rating,
    COUNT(l.loan_id) AS total_borrows
FROM books b
JOIN book_copies bc ON b.book_id = bc.book_id
JOIN loans l ON bc.copy_id = l.copy_id
LEFT JOIN branches br ON b.branch_id = br.branch_id
GROUP BY b.book_id, b.title, b.isbn, br.branch_code, b.rating
HAVING COUNT(l.loan_id) >= 2;

-- VIEW 3: Branch Revenue & Fine Summary View (Aggregates with SUM & COUNT)
CREATE OR REPLACE VIEW BranchFineSummaryView AS
SELECT 
    br.branch_code,
    br.branch_name,
    COUNT(DISTINCT u.user_id) AS registered_students,
    COUNT(f.fine_id) AS total_fines_levied,
    COALESCE(SUM(CASE WHEN f.status = 'PAID' THEN f.amount ELSE 0 END), 0.00) AS revenue_collected,
    COALESCE(SUM(CASE WHEN f.status = 'UNPAID' THEN f.amount ELSE 0 END), 0.00) AS pending_fines
FROM branches br
LEFT JOIN users u ON br.branch_id = u.branch_id
LEFT JOIN fines f ON u.user_id = f.user_id
GROUP BY br.branch_id, br.branch_code, br.branch_name;

-- ===================================================================
-- SEED DATA (Comprehensive & Categorical)
-- ===================================================================

-- 1. Branches
INSERT INTO branches (branch_id, branch_code, branch_name, hod_name) VALUES
(1, 'CSE', 'Computer Science and Engineering', 'Dr. Ramesh Kumar'),
(2, 'ECE', 'Electronics and Communication Engineering', 'Dr. Sunita Rao'),
(3, 'MECH', 'Mechanical Engineering', 'Dr. B.V. Somesh'),
(4, 'CIVIL', 'Civil Engineering', 'Dr. Arvind Sharma'),
(5, 'EEE', 'Electrical and Electronics Engineering', 'Dr. M.N. Prasad');

-- 2. Subjects
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

-- 3. Genres
INSERT INTO genres (genre_id, genre_name) VALUES
(1, 'Core Syllabus Textbook'),
(2, 'Reference Guide & Manual'),
(3, 'Computer Science & AI'),
(4, 'Engineering Classic & History'),
(5, 'Philosophy & Self-Development');

-- 4. Publishers
INSERT INTO publishers (publisher_id, publisher_name) VALUES
(1, 'Pearson Education'),
(2, 'McGraw-Hill Education'),
(3, 'Oxford University Press'),
(4, 'MIT Press'),
(5, 'John Wiley & Sons'),
(6, 'Delphi Classics');

-- 5. Authors
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

-- 6. Users (Librarian + Students with realistic USNs)
INSERT INTO users (user_id, usn, name, email, password, role, branch_id, semester, phone) VALUES
(1, '01JSTLIB001', 'Mrs. Shwetha Rao (Chief Librarian)', 'librarian@college.edu', 'admin123', 'LIBRARIAN', 1, NULL, '9876543210'),
(2, '01JST24UCS001', 'Rahul Sharma (Student 1)', 'rahul@student.edu', 'pass123', 'STUDENT', 1, 5, '9876543211'),
(3, '01JST24UCS002', 'Priya Patel (Student 2)', 'priya@student.edu', 'pass123', 'STUDENT', 2, 5, '9876543212'),
(4, '01JST24UCS003', 'Amit Kumar (Student 3)', 'amit@student.edu', 'pass123', 'STUDENT', 3, 5, '9876543213');

-- 7. Books (Across Branches, Subjects, Genres, and Ratings)
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

-- 8. Book Copies (Physical Copies with Barcodes & Stacks)
INSERT INTO book_copies (copy_id, book_id, copy_code, status, rack_location) VALUES
(101, 1, 'CSE-JAVA-01', 'AVAILABLE', 'Stack CS-01'),
(102, 1, 'CSE-JAVA-02', 'AVAILABLE', 'Stack CS-01'),
(103, 1, 'CSE-JAVA-03', 'AVAILABLE', 'Stack CS-01'),
(201, 2, 'CSE-DBMS-01', 'AVAILABLE', 'Stack CS-02'),
(202, 2, 'CSE-DBMS-02', 'AVAILABLE', 'Stack CS-02'),
(301, 3, 'CSE-OS-01',   'AVAILABLE', 'Stack CS-03'), -- Available for live borrowing demonstration
(302, 3, 'CSE-OS-02',   'AVAILABLE', 'Stack CS-03'),
(401, 4, 'CSE-ALGO-01', 'AVAILABLE', 'Stack CS-04'),
(501, 5, 'CSE-NET-01',  'AVAILABLE', 'Stack CS-05'),
(601, 6, 'ECE-DSP-01',  'AVAILABLE', 'Stack EC-01'),
(701, 7, 'MECH-THERM-01', 'AVAILABLE', 'Stack ME-01'),
(801, 8, 'MECH-TOM-01', 'AVAILABLE', 'Stack ME-02'),
(901, 9, 'GEN-MYTH-01', 'AVAILABLE', 'Stack GN-01'),
(1001, 10, 'GEN-ZEN-01', 'AVAILABLE', 'Stack GN-02'),

-- Additional copies borrowed by Priya (Student 2) to test 5-book limit (4 active loans):
(104, 1, 'CSE-JAVA-04', 'BORROWED', 'Stack CS-01'),
(203, 2, 'CSE-DBMS-03', 'BORROWED', 'Stack CS-02'),
(402, 4, 'CSE-ALGO-02', 'BORROWED', 'Stack CS-04'),
(602, 6, 'ECE-DSP-02',  'BORROWED', 'Stack EC-01');

-- 9. Loans (Priya's 4 active loans to demonstrate 5-book limit)
-- Amit and Rahul start completely clean (0 loans, 0 reservations, 0 fines) for live demonstration
INSERT INTO loans (loan_id, user_id, copy_id, borrow_date, due_date, status) VALUES
(2, 3, 104, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 25 DAY), 'ACTIVE'),
(3, 3, 203, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_ADD(NOW(), INTERVAL 26 DAY), 'ACTIVE'),
(4, 3, 402, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 27 DAY), 'ACTIVE'),
(5, 3, 602, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 28 DAY), 'ACTIVE');

-- 10. Book Requests (1 pending request with exactly 1 vote by Rahul to demonstrate voting & unvoting)
INSERT INTO book_requests (request_id, title, author, branch_id, genre_id, suggested_by, votes_count, status) VALUES
(1, 'Designing Data-Intensive Applications', 'Martin Kleppmann', 1, 3, 2, 1, 'PENDING');

-- 11. Request Votes (Only Rahul has voted, so Rahul can unvote and Priya/Amit can upvote)
INSERT INTO request_votes (request_id, user_id) VALUES
(1, 2);
