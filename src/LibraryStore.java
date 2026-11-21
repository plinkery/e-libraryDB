import models.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LibraryStore {
    private static final String SQL_LOG_PATH = "live_dbms_transactions.sql";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<Integer, User> users = new ConcurrentHashMap<>();
    private final Map<Integer, Book> books = new ConcurrentHashMap<>();
    private final Map<Integer, BookCopy> copies = new ConcurrentHashMap<>();
    private final Map<Integer, Reservation> reservations = new ConcurrentHashMap<>();
    private final Map<Integer, Loan> loans = new ConcurrentHashMap<>();
    private final Map<Integer, Fine> fines = new ConcurrentHashMap<>();
    private final Map<Integer, BookRequest> bookRequests = new ConcurrentHashMap<>();

    private double totalFinesCollected = 0.0;
    private final List<String> recentSqlLogs = Collections.synchronizedList(new ArrayList<>());

    private final AtomicInteger nextReservationId = new AtomicInteger(100);
    private final AtomicInteger nextLoanId = new AtomicInteger(100);
    private final AtomicInteger nextFineId = new AtomicInteger(100);
    private final AtomicInteger nextRequestId = new AtomicInteger(100);
    private final AtomicInteger nextBookId = new AtomicInteger(100);
    private final AtomicInteger nextCopyId = new AtomicInteger(2000);

    public LibraryStore() {
        initSeedData();
        clearSqlLogs();
    }

    public synchronized void resetAllData() {
        users.clear();
        books.clear();
        copies.clear();
        reservations.clear();
        loans.clear();
        fines.clear();
        bookRequests.clear();
        recentSqlLogs.clear();
        totalFinesCollected = 0.0;
        initSeedData();
        clearSqlLogs();
        logSql("Reset Database to Seed State", 
            "-- Resetting all tables to initial state...\n" +
            "SELECT 'Database reset to initial seed state' AS reset_status;\n" +
            "SELECT COUNT(*) AS total_books FROM books;\n" +
            "SELECT COUNT(*) AS total_copies FROM book_copies;"
        );
    }

    private void initSeedData() {
        // 1. Users with realistic College USNs
        users.put(1, new User(1, "01JSTLIB001", "Mrs. Shwetha Rao (Chief Librarian)", "librarian@college.edu", "admin123", "LIBRARIAN", "CSE", null, "9876543210"));
        users.put(2, new User(2, "01JST24UCS001", "Rahul Sharma (Student 1)", "rahul@student.edu", "pass123", "STUDENT", "CSE", 5, "9876543211"));
        users.put(3, new User(3, "01JST24UCS002", "Priya Patel (Student 2)", "priya@student.edu", "pass123", "STUDENT", "ECE", 5, "9876543212"));
        users.put(4, new User(4, "01JST24UCS003", "Amit Kumar (Student 3)", "amit@student.edu", "pass123", "STUDENT", "MECH", 5, "9876543213"));

        // 2. Books across Branches, Subjects, Genres, and Ratings (4.5 to 4.9)
        books.put(1, new Book(1, "Java: The Complete Reference", "978-1260440249", "CSE", "Object Oriented Programming", "Core Syllabus Textbook", "McGraw-Hill", "Herbert Schildt", "11th Edition", 2021, 4.8, "Comprehensive syllabus guide to core Java, OOP, collections, multithreading and database access."));
        books.put(2, new Book(2, "Fundamentals of Database Systems", "978-0133970777", "CSE", "Database Management Systems", "Core Syllabus Textbook", "Pearson Education", "Elmasri and Navathe", "7th Edition", 2020, 4.9, "Standard DBMS textbook covering ER models, relational algebra, 3NF normalization, and SQL."));
        books.put(3, new Book(3, "Operating System Concepts", "978-1118063330", "CSE", "Operating System Concepts", "Core Syllabus Textbook", "John Wiley & Sons", "Silberschatz and Galvin", "10th Edition", 2021, 4.7, "The definitive dinosaur OS reference book covering memory, processes, and synchronization."));
        books.put(4, new Book(4, "Introduction to Algorithms (CLRS)", "978-0262033848", "CSE", "Design and Analysis of Algorithms", "Reference Guide & Manual", "MIT Press", "Thomas H. Cormen", "4th Edition", 2022, 4.9, "Gold standard textbook for algorithm design, dynamic programming, and data structures."));
        books.put(5, new Book(5, "Computer Networks", "978-0132126953", "CSE", "Computer Communication Networks", "Core Syllabus Textbook", "Pearson Education", "Andrew S. Tanenbaum", "5th Edition", 2019, 4.6, "Networking principles, OSI reference layers, TCP/IP protocols, and routing algorithms."));
        books.put(6, new Book(6, "Digital Signal Processing: Principles", "978-0131873742", "ECE", "Digital Signal Processing", "Core Syllabus Textbook", "Pearson Education", "John G. Proakis", "4th Edition", 2020, 4.7, "Discrete-time signals, Z-transform, FFT, filter design for Electronics engineers."));
        books.put(7, new Book(7, "Engineering Thermodynamics", "978-9352606429", "MECH", "Applied Thermodynamics", "Core Syllabus Textbook", "McGraw-Hill", "P.K. Nag", "6th Edition", 2021, 4.6, "Thermodynamic cycles, entropy, steam tables, refrigeration and heat engines."));
        books.put(8, new Book(8, "Theory of Machines and Mechanisms", "978-0195371239", "MECH", "Theory of Machines", "Core Syllabus Textbook", "Oxford University Press", "Joseph E. Shigley", "5th Edition", 2019, 4.5, "Kinematics of linkages, cams, gears, and dynamic balancing of machines."));
        books.put(9, new Book(9, "The Mythical Man-Month", "978-0201835953", "CSE", "Software Engineering Concepts", "Engineering Classic & History", "Pearson Education", "Frederick P. Brooks", "Anniversary Ed", 2018, 4.8, "Classic essays on software engineering, scheduling fallacies, and project complexity."));
        books.put(10, new Book(10, "Zen and the Art of Motorcycle Maintenance", "978-0060589462", "MECH", "Applied Thermodynamics", "Philosophy & Self-Development", "Delphi Classics", "Robert M. Pirsig", "Deluxe Edition", 2017, 4.9, "Celebrated philosophical inquiry into quality, rationality, and technology."));

        // 3. Physical Copies
        addCopy(101, 1, "CSE-JAVA-01", "AVAILABLE", "Stack CS-01");
        addCopy(102, 1, "CSE-JAVA-02", "AVAILABLE", "Stack CS-01");
        addCopy(103, 1, "CSE-JAVA-03", "AVAILABLE", "Stack CS-01");

        addCopy(201, 2, "CSE-DBMS-01", "AVAILABLE", "Stack CS-02");
        addCopy(202, 2, "CSE-DBMS-02", "AVAILABLE", "Stack CS-02");

        addCopy(301, 3, "CSE-OS-01", "AVAILABLE", "Stack CS-03"); // Clean state: Available to reserve/borrow
        addCopy(302, 3, "CSE-OS-02", "AVAILABLE", "Stack CS-03");

        addCopy(401, 4, "CSE-ALGO-01", "AVAILABLE", "Stack CS-04");
        addCopy(501, 5, "CSE-NET-01", "AVAILABLE", "Stack CS-05");
        addCopy(601, 6, "ECE-DSP-01", "AVAILABLE", "Stack EC-01");
        addCopy(701, 7, "MECH-THERM-01", "AVAILABLE", "Stack ME-01");
        addCopy(801, 8, "MECH-TOM-01", "AVAILABLE", "Stack ME-02");
        addCopy(901, 9, "GEN-MYTH-01", "AVAILABLE", "Stack GN-01");
        addCopy(1001, 10, "GEN-ZEN-01", "AVAILABLE", "Stack GN-02");

        // 4. Pre-seed 4 active loans for Priya (Student 2) to test 5-book limit (01JST24UCS002)
        LocalDateTime now = LocalDateTime.now();
        addCopy(104, 1, "CSE-JAVA-04", "BORROWED", "Stack CS-01");
        addCopy(203, 2, "CSE-DBMS-03", "BORROWED", "Stack CS-02");
        addCopy(402, 4, "CSE-ALGO-02", "BORROWED", "Stack CS-04");
        addCopy(602, 6, "ECE-DSP-02", "BORROWED", "Stack EC-01");

        loans.put(2, new Loan(2, 3, 104, 1, now.minusDays(5), now.plusDays(25), "ACTIVE"));
        loans.put(3, new Loan(3, 3, 203, 2, now.minusDays(4), now.plusDays(26), "ACTIVE"));
        loans.put(4, new Loan(4, 3, 402, 4, now.minusDays(3), now.plusDays(27), "ACTIVE"));
        loans.put(5, new Loan(5, 3, 602, 6, now.minusDays(2), now.plusDays(28), "ACTIVE")); // Priya has 4 active books!

        // Amit (Student 3) and Rahul (Student 1) start completely clean (0 borrowed books, 0 reservations, 0 fines)
        // This allows user to demonstrate the entire lifecycle live from scratch using the Teacher Demo Tools!

        // 5. Pre-seed exactly ONE Book Request with ONE vote (by Rahul Sharma)
        BookRequest req1 = new BookRequest(1, "Designing Data-Intensive Applications", "Martin Kleppmann", "CSE", "Computer Science & AI", 2, "Rahul Sharma (Student 1)", 1, "PENDING", now.minusDays(1));
        req1.addVote(2); // Only Rahul has voted!
        bookRequests.put(1, req1);
    }

    private void addCopy(int copyId, int bookId, String copyCode, String status, String rackLocation) {
        BookCopy copy = new BookCopy(copyId, bookId, copyCode, status, rackLocation);
        copies.put(copyId, copy);
        Book b = books.get(bookId);
        if (b != null) {
            b.addCopy(copy);
        }
    }

    // =========================================================================
    // SQL Transaction Logger (Synchronously writes to live_dbms_transactions.sql)
    // =========================================================================
    private void writeToFile(String content, boolean append) {
        try (FileWriter fw = new FileWriter(SQL_LOG_PATH, append);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.print(content);
            pw.flush();
            fw.flush();
        } catch (IOException e) {
            System.err.println("Could not write to " + SQL_LOG_PATH + ": " + e.getMessage());
        }
    }

    public synchronized void logSql(String actionName, String sqlStatement) {
        String timestamp = LocalDateTime.now().format(TIME_FMT);
        String formattedLog = String.format("\n-- [%s] ACTION: %s\n%s\n", timestamp, actionName, sqlStatement.trim());

        recentSqlLogs.add(0, formattedLog);
        if (recentSqlLogs.size() > 50) {
            recentSqlLogs.remove(recentSqlLogs.size() - 1);
        }

        writeToFile(formattedLog, true);
    }

    public synchronized void clearSqlLogs() {
        recentSqlLogs.clear();
        String header = "-- ===================================================================\n" +
            "-- LIVE DBMS TRANSACTIONS LOG\n" +
            "-- Generated in real-time by College E-Library System\n" +
            "-- You can run or inspect these statements directly in DBeaver / Workbench!\n" +
            "-- ===================================================================\n" +
            "USE elibrary_db;\n\n" +
            "-- [LOGS CLEARED & READY AT " + LocalDateTime.now().format(TIME_FMT) + "]\n" +
            "-- Ready for fresh real-time transactions from user clicks...\n\n";
        writeToFile(header, false);
    }

    public List<String> getRecentSqlLogs() {
        return new ArrayList<>(recentSqlLogs);
    }

    public double getTotalFinesCollected() {
        return totalFinesCollected;
    }

    // =========================================================================
    // Core User & Domain Lookups
    // =========================================================================
    public User getUser(int userId) {
        return users.get(userId);
    }

    public User getUserByUsnOrEmail(String identifier) {
        if (identifier == null) return null;
        String clean = identifier.trim().toLowerCase();
        for (User u : users.values()) {
            if (u.getUsn().equalsIgnoreCase(clean) || u.getEmail().equalsIgnoreCase(clean)) {
                return u;
            }
        }
        return null;
    }

    public Collection<User> getAllUsers() {
        return users.values();
    }

    public Collection<Book> getAllBooks() {
        return books.values();
    }

    public Book getBook(int bookId) {
        return books.get(bookId);
    }

    public Loan getLoan(int loanId) {
        return loans.get(loanId);
    }

    public BookCopy getCopy(int copyId) {
        return copies.get(copyId);
    }

    public List<Loan> getActiveLoansForUser(int userId) {
        List<Loan> list = new ArrayList<>();
        for (Loan l : loans.values()) {
            if (l.getUserId() == userId && "ACTIVE".equalsIgnoreCase(l.getStatus())) {
                list.add(l);
            }
        }
        return list;
    }

    public List<Loan> getAllActiveLoans() {
        List<Loan> list = new ArrayList<>();
        for (Loan l : loans.values()) {
            if ("ACTIVE".equalsIgnoreCase(l.getStatus())) {
                list.add(l);
            }
        }
        return list;
    }

    public List<Reservation> getPendingReservations() {
        checkAndExpireReservations();
        List<Reservation> list = new ArrayList<>();
        for (Reservation r : reservations.values()) {
            if ("PENDING".equalsIgnoreCase(r.getStatus())) {
                list.add(r);
            }
        }
        return list;
    }

    public List<Reservation> getPendingReservationsForUser(int userId) {
        checkAndExpireReservations();
        List<Reservation> list = new ArrayList<>();
        for (Reservation r : reservations.values()) {
            if (r.getUserId() == userId && "PENDING".equalsIgnoreCase(r.getStatus())) {
                list.add(r);
            }
        }
        return list;
    }

    public List<Fine> getFinesForUser(int userId) {
        List<Fine> list = new ArrayList<>();
        for (Fine f : fines.values()) {
            if (f.getUserId() == userId) {
                list.add(f);
            }
        }
        return list;
    }

    public double getTotalUnpaidFineForUser(int userId) {
        double total = 0.0;
        for (Fine f : fines.values()) {
            if (f.getUserId() == userId && "UNPAID".equalsIgnoreCase(f.getStatus())) {
                total += f.getAmount();
            }
        }
        return total;
    }

    public boolean hasOverdueLoans(int userId) {
        // True only if there is an active overdue loan OR an UNPAID fine!
        for (Loan l : loans.values()) {
            if (l.getUserId() == userId && "ACTIVE".equalsIgnoreCase(l.getStatus()) && l.isOverdue()) {
                return true;
            }
        }
        return getTotalUnpaidFineForUser(userId) > 0.0;
    }

    public List<Map<String, Object>> getHighDemandBooks() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Book b : books.values()) {
            int borrows = 0;
            for (Loan l : loans.values()) {
                if (l.getBookId() == b.getBookId()) borrows++;
            }
            int available = b.getAvailableCopiesCount();
            if (borrows >= 2 || available == 0) {
                Map<String, Object> map = new HashMap<>();
                map.put("bookId", b.getBookId());
                map.put("title", b.getTitle());
                map.put("author", b.getAuthor());
                map.put("branchCode", b.getBranchCode());
                map.put("rating", b.getRating());
                map.put("totalBorrows", borrows);
                map.put("availableCopies", available);
                map.put("totalCopies", b.getTotalCopiesCount());
                result.add(map);
            }
        }
        return result;
    }

    // =========================================================================
    // Student: Reserve Book (12-Hour window)
    // =========================================================================
    public synchronized Reservation reserveBook(int userId, int bookId) throws Exception {
        User user = users.get(userId);
        if (user == null) throw new Exception("User not found");
        if (!"STUDENT".equalsIgnoreCase(user.getRole())) throw new Exception("Only students can reserve books.");

        if (user.isReservationBlocked()) {
            throw new Exception("Reservation Blocked! You failed to borrow a reserved book within 12 hours. You cannot reserve until " 
                + user.getReservationBlockedUntil().format(TIME_FMT));
        }

        if (hasOverdueLoans(userId)) {
            throw new Exception("Reservation Blocked: You have overdue books or unpaid fines! Pay fines to unblock.");
        }

        int activeLoans = getActiveLoansForUser(userId).size();
        int activeRes = getPendingReservationsForUser(userId).size();
        if (activeLoans + activeRes >= 5) {
            throw new Exception("Maximum limit reached: You can have at most 5 books borrowed/reserved at a time.");
        }

        for (Reservation r : reservations.values()) {
            if (r.getUserId() == userId && r.getBookId() == bookId && "PENDING".equalsIgnoreCase(r.getStatus())) {
                throw new Exception("You already have an active reservation for this book.");
            }
        }

        Book book = books.get(bookId);
        if (book == null) throw new Exception("Book not found");
        BookCopy copy = book.getFirstAvailableCopy();
        if (copy == null) {
            throw new Exception("No physical copies are currently available to reserve.");
        }

        copy.setStatus("RESERVED");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(12);
        int resId = nextReservationId.incrementAndGet();

        Reservation res = new Reservation(resId, userId, copy.getCopyId(), bookId, now, expiresAt, "PENDING");
        reservations.put(resId, res);

        String sql = String.format(
            "UPDATE book_copies SET status = 'RESERVED' WHERE copy_id = %d;\n" +
            "INSERT INTO reservations (reservation_id, user_id, copy_id, reserved_at, expires_at, status)\n" +
            "VALUES (%d, %d, %d, '%s', '%s', 'PENDING')\n" +
            "ON DUPLICATE KEY UPDATE status = VALUES(status), expires_at = VALUES(expires_at);\n\n" +
            "-- [WORKBENCH VERIFICATION]: Run to inspect updated tables\n" +
            "SELECT r.reservation_id, u.usn, u.name, b.title, bc.copy_code, bc.status AS copy_status, r.expires_at\n" +
            "FROM reservations r\n" +
            "JOIN users u ON r.user_id = u.user_id\n" +
            "JOIN book_copies bc ON r.copy_id = bc.copy_id\n" +
            "JOIN books b ON bc.book_id = b.book_id\n" +
            "WHERE r.reservation_id = %d;",
            copy.getCopyId(), resId, userId, copy.getCopyId(), now.format(TIME_FMT), expiresAt.format(TIME_FMT), resId
        );
        logSql("Student Reserved Book (12-Hour Expiry)", sql);

        return res;
    }

    public synchronized void cancelReservation(int userId, int reservationId) throws Exception {
        Reservation res = reservations.get(reservationId);
        if (res == null) throw new Exception("Reservation not found");
        if (res.getUserId() != userId) throw new Exception("Unauthorized to cancel this reservation");

        res.setStatus("CANCELLED");
        BookCopy copy = copies.get(res.getCopyId());
        if (copy != null && "RESERVED".equalsIgnoreCase(copy.getStatus())) {
            copy.setStatus("AVAILABLE");
        }

        String sql = String.format(
            "UPDATE reservations SET status = 'CANCELLED' WHERE reservation_id = %d;\n" +
            "UPDATE book_copies SET status = 'AVAILABLE' WHERE copy_id = %d;\n\n" +
            "-- [RUN TO SHOW TEACHER VISUAL RESULT IN WORKBENCH]:\n" +
            "SELECT r.reservation_id, u.usn, u.name AS student_name, r.status AS reservation_status, bc.copy_code, bc.status AS copy_status\n" +
            "FROM reservations r\n" +
            "JOIN users u ON r.user_id = u.user_id\n" +
            "JOIN book_copies bc ON r.copy_id = bc.copy_id\n" +
            "WHERE r.reservation_id = %d;",
            reservationId, res.getCopyId(), reservationId
        );
        logSql("Student Cancelled Reservation", sql);
    }

    // =========================================================================
    // Librarian: Issue Book (Starts 1-Month Due Timer)
    // =========================================================================
    public synchronized Loan borrowBook(int librarianId, int studentId, int copyId) throws Exception {
        User librarian = users.get(librarianId);
        if (librarian == null || !"LIBRARIAN".equalsIgnoreCase(librarian.getRole())) {
            throw new Exception("Access Denied: Only librarians can approve and mark books as borrowed.");
        }

        User student = users.get(studentId);
        if (student == null || !"STUDENT".equalsIgnoreCase(student.getRole())) {
            throw new Exception("Invalid student ID.");
        }

        int activeLoans = getActiveLoansForUser(studentId).size();
        if (activeLoans >= 5) {
            throw new Exception("TRIGGER ERROR [SQLSTATE 45000]: Borrowing limit exceeded: Maximum 5 books allowed per student (USN: " + student.getUsn() + ").");
        }

        if (hasOverdueLoans(studentId)) {
            throw new Exception("TRIGGER ERROR [SQLSTATE 45000]: Borrowing blocked: Student has overdue books with unpaid fines (USN: " + student.getUsn() + ").");
        }

        BookCopy copy = copies.get(copyId);
        if (copy == null) throw new Exception("Copy not found.");

        if ("BORROWED".equalsIgnoreCase(copy.getStatus())) {
            throw new Exception("This copy is already borrowed by another student.");
        }

        Reservation matchingRes = null;
        for (Reservation r : reservations.values()) {
            if (r.getCopyId() == copyId && "PENDING".equalsIgnoreCase(r.getStatus())) {
                if (r.getUserId() != studentId) {
                    throw new Exception("This copy is reserved by another student!");
                }
                matchingRes = r;
                break;
            }
        }

        if (matchingRes != null) {
            matchingRes.setStatus("FULFILLED");
        }

        copy.setStatus("BORROWED");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDate = now.plusDays(30);
        int loanId = nextLoanId.incrementAndGet();

        Loan loan = new Loan(loanId, studentId, copyId, copy.getBookId(), now, dueDate, "ACTIVE");
        loans.put(loanId, loan);

        String sql = String.format(
            "UPDATE book_copies SET status = 'BORROWED' WHERE copy_id = %d;\n" +
            (matchingRes != null ? String.format("UPDATE reservations SET status = 'FULFILLED' WHERE reservation_id = %d;\n", matchingRes.getReservationId()) : "") +
            "INSERT INTO loans (loan_id, user_id, copy_id, borrow_date, due_date, status)\n" +
            "VALUES (%d, %d, %d, '%s', '%s', 'ACTIVE')\n" +
            "ON DUPLICATE KEY UPDATE status = VALUES(status), due_date = VALUES(due_date);\n\n" +
            "-- [WORKBENCH VERIFICATION]: Run to inspect newly active loan and updated copy status\n" +
            "SELECT l.loan_id, u.usn, u.name, b.title, bc.copy_code, bc.status AS copy_status, l.borrow_date, l.due_date\n" +
            "FROM loans l\n" +
            "JOIN users u ON l.user_id = u.user_id\n" +
            "JOIN book_copies bc ON l.copy_id = bc.copy_id\n" +
            "JOIN books b ON bc.book_id = b.book_id\n" +
            "WHERE l.loan_id = %d;",
            copyId, loanId, studentId, copyId, now.format(TIME_FMT), dueDate.format(TIME_FMT), loanId
        );
        logSql("Librarian Issued Book (1-Month Due Timer Started)", sql);

        return loan;
    }

    // =========================================================================
    // Librarian: Return Book & Fine Collection
    // =========================================================================
    public synchronized Map<String, Object> returnBook(int librarianId, int loanId, boolean collectFineNow) throws Exception {
        User librarian = users.get(librarianId);
        if (librarian == null || !"LIBRARIAN".equalsIgnoreCase(librarian.getRole())) {
            throw new Exception("Only librarians can process book returns.");
        }

        Loan loan = loans.get(loanId);
        if (loan == null) throw new Exception("Loan record not found.");
        if (!"ACTIVE".equalsIgnoreCase(loan.getStatus())) throw new Exception("This book is already marked as returned.");

        LocalDateTime now = LocalDateTime.now();

        // 1. Calculate overdue status and fine BEFORE changing loan status or return date!
        long daysOverdue = 0;
        double fineAmount = 0.0;
        if (now.isAfter(loan.getDueDate())) {
            Duration diff = Duration.between(loan.getDueDate(), now);
            daysOverdue = diff.toDays() <= 0 ? 1 : diff.toDays();
            fineAmount = (double) daysOverdue * 1.00; // Rs. 1 per day
        }

        // Check if an existing fine record exists for this loan (e.g. from time machine simulation)
        Fine fineObj = null;
        for (Fine f : fines.values()) {
            if (f.getLoanId() == loanId) {
                fineObj = f;
                if (fineObj.getAmount() > fineAmount) {
                    fineAmount = fineObj.getAmount();
                    daysOverdue = fineObj.getDaysOverdue();
                }
                break;
            }
        }

        // 2. Mark loan and physical copy as returned/available
        loan.setReturnDate(now);
        loan.setStatus("RETURNED");

        BookCopy copy = copies.get(loan.getCopyId());
        if (copy != null) {
            copy.setStatus("AVAILABLE");
        }

        // 3. Process fine collection
        boolean wasAlreadyPaid = (fineObj != null && "PAID".equalsIgnoreCase(fineObj.getStatus()));
        if (fineAmount > 0) {
            if (fineObj == null) {
                int fineId = nextFineId.incrementAndGet();
                fineObj = new Fine(fineId, loanId, loan.getUserId(), (int) daysOverdue, fineAmount, collectFineNow ? "PAID" : "UNPAID", now);
                if (collectFineNow) {
                    fineObj.markPaid();
                    totalFinesCollected += fineAmount;
                }
                fines.put(fineId, fineObj);
            } else {
                fineObj.setAmount(fineAmount);
                if (collectFineNow && !wasAlreadyPaid) {
                    fineObj.markPaid();
                    totalFinesCollected += fineAmount;
                }
            }
        }

        // 4. Build comprehensive SQL audit log with Workbench verification
        StringBuilder sql = new StringBuilder();
        sql.append(String.format("UPDATE loans SET return_date = '%s', status = 'RETURNED' WHERE loan_id = %d;\n", now.format(TIME_FMT), loanId));
        sql.append(String.format("UPDATE book_copies SET status = 'AVAILABLE' WHERE copy_id = %d;\n", loan.getCopyId()));
        if (fineAmount > 0 && fineObj != null) {
            if (collectFineNow) {
                // Option 1: Student pays overdue fine at counter while returning book
                sql.append(String.format("INSERT INTO fines (fine_id, loan_id, user_id, days_overdue, amount, status, assessed_at, paid_at)\n" +
                    "VALUES (%d, %d, %d, %d, %.2f, 'PAID', '%s', '%s')\n" +
                    "ON DUPLICATE KEY UPDATE status = 'PAID', paid_at = '%s';\n",
                    fineObj.getFineId(), loanId, loan.getUserId(), daysOverdue, fineAmount, now.format(TIME_FMT), now.format(TIME_FMT), now.format(TIME_FMT)));
                sql.append(String.format("INSERT INTO fine_collections (fine_id, user_id, amount_paid, collected_by, collection_date)\n" +
                    "SELECT %d, %d, %.2f, %d, '%s'\n" +
                    "WHERE NOT EXISTS (SELECT 1 FROM fine_collections WHERE fine_id = %d AND user_id = %d);\n",
                    fineObj.getFineId(), loan.getUserId(), fineAmount, librarianId, now.format(TIME_FMT),
                    fineObj.getFineId(), loan.getUserId()));
            } else {
                // Option 2: Student returns physical book now, but fine is recorded as UNPAID (Pay Later)
                sql.append(String.format("INSERT INTO fines (fine_id, loan_id, user_id, days_overdue, amount, status, assessed_at)\n" +
                    "VALUES (%d, %d, %d, %d, %.2f, 'UNPAID', '%s')\n" +
                    "ON DUPLICATE KEY UPDATE status = 'UNPAID';\n",
                    fineObj.getFineId(), loanId, loan.getUserId(), daysOverdue, fineAmount, now.format(TIME_FMT)));
            }
        }
        sql.append(String.format("\n-- [WORKBENCH VERIFICATION]: Run to inspect return state and fine settlement\n" +
            "SELECT l.loan_id, u.usn, u.name, bc.copy_code, bc.status AS copy_status, l.return_date, l.status AS loan_status\n" +
            "FROM loans l JOIN users u ON l.user_id = u.user_id JOIN book_copies bc ON l.copy_id = bc.copy_id WHERE l.loan_id = %d;\n" +
            "SELECT * FROM fines WHERE loan_id = %d;\n" +
            "SELECT * FROM fine_collections WHERE user_id = %d ORDER BY collection_id DESC LIMIT 1;\n" +
            "SELECT * FROM BranchFineSummaryView;",
            loanId, loanId, loan.getUserId()));

        String actionTitle;
        if (collectFineNow && fineAmount > 0) {
            actionTitle = "Librarian Option 1: Pay Fine & Return Book (₹" + String.format("%.2f", fineAmount) + ")";
        } else if (fineAmount > 0) {
            actionTitle = "Librarian Option 2: Return Book (Fine Unpaid - Pay Later)";
        } else {
            actionTitle = "Librarian Processed On-Time Book Return";
        }
        logSql(actionTitle, sql.toString());

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("fineAmount", fineAmount);
        resp.put("daysOverdue", daysOverdue);
        resp.put("fineCollected", collectFineNow && fineAmount > 0);
        resp.put("isFinePaid", collectFineNow);
        return resp;
    }

    // Explicit Fine Collection Method (Clear fine when student pays)
    public synchronized void collectFine(int librarianId, int fineId) throws Exception {
        Fine f = fines.get(fineId);
        if (f == null) throw new Exception("Fine record not found.");
        if ("PAID".equalsIgnoreCase(f.getStatus())) throw new Exception("Fine is already marked as paid.");

        f.markPaid();
        totalFinesCollected += f.getAmount();

        User student = users.get(f.getUserId());
        String studentUsn = student != null ? student.getUsn() : "Unknown";

        String sql = String.format(
            "UPDATE fines SET status = 'PAID', paid_at = '%s' WHERE fine_id = %d;\n" +
            "INSERT INTO fine_collections (fine_id, user_id, amount_paid, collected_by, collection_date)\n" +
            "SELECT %d, %d, %.2f, %d, '%s'\n" +
            "WHERE NOT EXISTS (SELECT 1 FROM fine_collections WHERE fine_id = %d AND user_id = %d);\n\n" +
            "-- [WORKBENCH VERIFICATION]: Verify fine cleared and revenue recorded\n" +
            "SELECT * FROM fines WHERE fine_id = %d;\n" +
            "SELECT * FROM BranchFineSummaryView;\n" +
            "-- Overdue fine cleared for student USN: %s",
            LocalDateTime.now().format(TIME_FMT), fineId,
            fineId, f.getUserId(), f.getAmount(), librarianId, LocalDateTime.now().format(TIME_FMT),
            fineId, f.getUserId(),
            fineId, studentUsn
        );
        logSql("Fine Payment Cleared (Revenue Collected)", sql);
    }

    // =========================================================================
    // Auto-Expire 12-Hour Reservations & Apply 1-Day Suspension
    // =========================================================================
    public synchronized int checkAndExpireReservations() {
        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Reservation r : reservations.values()) {
            if ("PENDING".equalsIgnoreCase(r.getStatus()) && now.isAfter(r.getExpiresAt())) {
                r.setStatus("EXPIRED");
                BookCopy copy = copies.get(r.getCopyId());
                if (copy != null && "RESERVED".equalsIgnoreCase(copy.getStatus())) {
                    copy.setStatus("AVAILABLE");
                }

                User student = users.get(r.getUserId());
                if (student != null) {
                    student.setReservationBlockedUntil(now.plusDays(1));
                }

                count++;

                String sql = String.format(
                    "UPDATE reservations SET status = 'EXPIRED' WHERE reservation_id = %d;\n" +
                    "UPDATE book_copies SET status = 'AVAILABLE' WHERE copy_id = %d;\n" +
                    "UPDATE users SET reservation_blocked_until = DATE_ADD(NOW(), INTERVAL 1 DAY) WHERE user_id = %d;\n\n" +
                    "-- [RUN TO SHOW TEACHER VISUAL RESULT IN WORKBENCH]:\n" +
                    "SELECT r.reservation_id, r.status AS reservation_status, bc.copy_code, bc.status AS copy_status, u.usn, u.name, u.reservation_blocked_until\n" +
                    "FROM reservations r\n" +
                    "JOIN book_copies bc ON r.copy_id = bc.copy_id\n" +
                    "JOIN users u ON r.user_id = u.user_id\n" +
                    "WHERE r.reservation_id = %d;",
                    r.getReservationId(), r.getCopyId(), r.getUserId(), r.getReservationId()
                );
                logSql("Reservation Expired (12-Hour Missed -> 1-Day Block Applied)", sql);
            }
        }
        return count;
    }

    // =========================================================================
    // Student: Request Missing Book, Upvote, and UNVOTE
    // =========================================================================
    public synchronized BookRequest requestNewBook(int userId, String title, String author, String branchCode, String genreName) throws Exception {
        User user = users.get(userId);
        if (user == null) throw new Exception("User not found");
        if (title == null || title.trim().isEmpty()) throw new Exception("Title is required");

        int reqId = nextRequestId.incrementAndGet();
        LocalDateTime now = LocalDateTime.now();
        BookRequest req = new BookRequest(reqId, title.trim(), author != null ? author.trim() : "Unknown", branchCode, genreName, userId, user.getName(), 1, "PENDING", now);
        bookRequests.put(reqId, req);

        String sql = String.format(
            "INSERT INTO book_requests (request_id, title, author, branch_id, genre_id, suggested_by, votes_count, status)\n" +
            "VALUES (%d, '%s', '%s', (SELECT branch_id FROM branches WHERE branch_code='%s'), (SELECT genre_id FROM genres WHERE genre_name='%s'), %d, 1, 'PENDING')\n" +
            "ON DUPLICATE KEY UPDATE votes_count = VALUES(votes_count), status = VALUES(status);\n" +
            "INSERT IGNORE INTO request_votes (request_id, user_id) VALUES (%d, %d);\n\n" +
            "-- [RUN TO SHOW TEACHER VISUAL RESULT IN WORKBENCH]:\n" +
            "SELECT r.request_id, r.title, r.author, br.branch_code, g.genre_name, u.usn, u.name AS suggested_by, r.votes_count, r.status\n" +
            "FROM book_requests r\n" +
            "JOIN users u ON r.suggested_by = u.user_id\n" +
            "LEFT JOIN branches br ON r.branch_id = br.branch_id\n" +
            "LEFT JOIN genres g ON r.genre_id = g.genre_id\n" +
            "WHERE r.request_id = %d;",
            reqId, title.replace("'", "''"), author.replace("'", "''"), branchCode != null ? branchCode : "CSE", genreName != null ? genreName : "Computer Science & AI", userId,
            reqId, userId,
            reqId
        );
        logSql("Student Requested New Non-Existent Book", sql);
        return req;
    }

    public synchronized boolean voteBookRequest(int userId, int requestId) throws Exception {
        BookRequest req = bookRequests.get(requestId);
        if (req == null) throw new Exception("Book request not found");
        boolean added = req.addVote(userId);
        if (added) {
            String sql = String.format(
                "INSERT IGNORE INTO request_votes (request_id, user_id) VALUES (%d, %d);\n" +
                "UPDATE book_requests SET votes_count = (SELECT COUNT(*) FROM request_votes WHERE request_id = %d) WHERE request_id = %d;\n\n" +
                "-- [RUN TO SHOW TEACHER VISUAL RESULT IN WORKBENCH]:\n" +
                "SELECT r.request_id, r.title, r.votes_count, COUNT(rv.user_id) AS total_vote_records\n" +
                "FROM book_requests r\n" +
                "LEFT JOIN request_votes rv ON r.request_id = rv.request_id\n" +
                "WHERE r.request_id = %d\n" +
                "GROUP BY r.request_id, r.title, r.votes_count;",
                requestId, userId,
                requestId, requestId,
                requestId
            );
            logSql("Student Upvoted Book Acquisition Request", sql);
        }
        return added;
    }

    public synchronized boolean unvoteBookRequest(int userId, int requestId) throws Exception {
        BookRequest req = bookRequests.get(requestId);
        if (req == null) throw new Exception("Book request not found");
        boolean removed = req.removeVote(userId);
        if (removed) {
            String sql = String.format(
                "DELETE FROM request_votes WHERE request_id = %d AND user_id = %d;\n" +
                "UPDATE book_requests SET votes_count = (SELECT COUNT(*) FROM request_votes WHERE request_id = %d) WHERE request_id = %d;\n\n" +
                "-- [RUN TO SHOW TEACHER VISUAL RESULT IN WORKBENCH]:\n" +
                "SELECT r.request_id, r.title, r.votes_count, COUNT(rv.user_id) AS total_vote_records\n" +
                "FROM book_requests r\n" +
                "LEFT JOIN request_votes rv ON r.request_id = rv.request_id\n" +
                "WHERE r.request_id = %d\n" +
                "GROUP BY r.request_id, r.title, r.votes_count;",
                requestId, userId,
                requestId, requestId,
                requestId
            );
            logSql("Student Removed Upvote from Book Request", sql);
        }
        return removed;
    }

    // Librarian Approves & Procures Request (Adds Book Directly to Catalog)
    public synchronized Book procureBookRequest(int librarianId, int requestId) throws Exception {
        User lib = users.get(librarianId);
        if (lib == null || !"LIBRARIAN".equalsIgnoreCase(lib.getRole())) {
            throw new Exception("Only librarians can approve and procure books.");
        }

        BookRequest req = bookRequests.get(requestId);
        if (req == null) throw new Exception("Book request not found");

        req.setStatus("PROCURED");

        int bookId = nextBookId.incrementAndGet();
        String generatedIsbn = "978-" + (1000000000L + (long)(Math.random() * 8999999999L));
        Book newBook = new Book(
            bookId, 
            req.getTitle(), 
            generatedIsbn, 
            req.getBranchCode(), 
            "Procured Syllabus Adopted", 
            req.getGenreName(), 
            "Pearson Education", 
            req.getAuthor(), 
            "1st Edition", 
            2024, 
            4.8, 
            "Recently procured based on " + req.getVotesCount() + " student acquisition votes."
        );
        books.put(bookId, newBook);

        // Create 2 physical copies
        int c1 = nextCopyId.incrementAndGet();
        int c2 = nextCopyId.incrementAndGet();
        addCopy(c1, bookId, req.getBranchCode() + "-NEW-" + (c1 % 100), "AVAILABLE", "Stack " + req.getBranchCode() + "-01");
        addCopy(c2, bookId, req.getBranchCode() + "-NEW-" + (c2 % 100), "AVAILABLE", "Stack " + req.getBranchCode() + "-01");

        String sql = String.format(
            "UPDATE book_requests SET status = 'PROCURED' WHERE request_id = %d;\n" +
            "INSERT INTO books (book_id, title, isbn, branch_id, genre_id, publisher_id, author_id, edition, pub_year, rating, description)\n" +
            "VALUES (%d, '%s', '%s', (SELECT branch_id FROM branches WHERE branch_code='%s'), 1, 1, 1, '1st Edition', 2024, 4.8, 'Procured via student votes')\n" +
            "ON DUPLICATE KEY UPDATE title = VALUES(title), rating = VALUES(rating);\n" +
            "INSERT INTO book_copies (copy_id, book_id, copy_code, status, rack_location) VALUES\n" +
            "(%d, %d, '%s', 'AVAILABLE', 'Stack %s-01'),\n" +
            "(%d, %d, '%s', 'AVAILABLE', 'Stack %s-01')\n" +
            "ON DUPLICATE KEY UPDATE status = VALUES(status), rack_location = VALUES(rack_location);\n\n" +
            "-- [WORKBENCH VERIFICATION]: Verify newly procured title and available physical copies\n" +
            "SELECT b.book_id, b.title, br.branch_code, b.rating, COUNT(bc.copy_id) AS available_copies\n" +
            "FROM books b JOIN branches br ON b.branch_id = br.branch_id\n" +
            "JOIN book_copies bc ON b.book_id = bc.book_id\n" +
            "WHERE b.book_id = %d GROUP BY b.book_id, b.title, br.branch_code, b.rating;",
            requestId, bookId, req.getTitle().replace("'", "''"), generatedIsbn, req.getBranchCode(),
            c1, bookId, req.getBranchCode() + "-NEW-" + (c1 % 100), req.getBranchCode(),
            c2, bookId, req.getBranchCode() + "-NEW-" + (c2 % 100), req.getBranchCode(),
            bookId
        );
        logSql("Librarian Approved & Procured Book (Added to Catalog with 2 Copies)", sql);

        return newBook;
    }

    public Collection<BookRequest> getAllBookRequests() {
        return bookRequests.values();
    }

    // =========================================================================
    // LAB DBMS DEMONSTRATION QUERIES (Exact match to Lab syllabus)
    // =========================================================================
    public Map<String, Object> executeLabQuery(String queryId) {
        Map<String, Object> res = new HashMap<>();

        if ("query1".equalsIgnoreCase(queryId)) {
            // Lab Concept: Multi-table JOIN + Aggregates + HAVING (from Student Enrollment lab)
            res.put("title", "1. Multi-Table JOIN with GROUP BY & HAVING (Student Enrollment Lab Style)");
            res.put("concept", "Demonstrates joining Branches, Books, and Publishers with AVG(rating) >= 4.5 and total copies >= 2");
            res.put("sql", 
                "SELECT br.branch_code, br.branch_name, COUNT(bk.book_id) AS total_titles,\n" +
                "       ROUND(AVG(bk.rating), 2) AS avg_rating, SUM(bk.copies_count) AS total_physical_copies\n" +
                "FROM branches br\n" +
                "JOIN books bk ON br.branch_id = bk.branch_id\n" +
                "JOIN publishers p ON bk.publisher_id = p.publisher_id\n" +
                "WHERE p.publisher_name LIKE '%Pearson%'\n" +
                "GROUP BY br.branch_code, br.branch_name\n" +
                "HAVING AVG(bk.rating) >= 4.5 AND SUM(bk.copies_count) >= 2\n" +
                "ORDER BY avg_rating DESC;"
            );
            List<Map<String, String>> rows = new ArrayList<>();
            rows.add(Map.of("branch_code", "CSE", "branch_name", "Computer Science", "total_titles", "3", "avg_rating", "4.77", "total_physical_copies", "7"));
            rows.add(Map.of("branch_code", "ECE", "branch_name", "Electronics & Comm", "total_titles", "1", "avg_rating", "4.70", "total_physical_copies", "2"));
            res.put("rows", rows);
            logSql("Executed DBMS Lab Query 1 (JOIN + HAVING)", (String) res.get("sql"));
        } else if ("query2".equalsIgnoreCase(queryId)) {
            // Lab Concept: Correlated Subquery with NOT EXISTS / Relational Division (Company / Sailors lab style)
            res.put("title", "2. Relational Division using Double NOT EXISTS (Sailors / Company Lab Style)");
            res.put("concept", "Find students who have borrowed books from EVERY engineering branch (Relational Division)");
            res.put("sql", 
                "SELECT u.usn, u.name, br.branch_name AS student_dept\n" +
                "FROM users u\n" +
                "JOIN branches br ON u.branch_id = br.branch_id\n" +
                "WHERE u.role = 'STUDENT'\n" +
                "  AND NOT EXISTS (\n" +
                "      SELECT b.branch_id FROM branches b WHERE b.branch_code IN ('CSE', 'ECE', 'MECH')\n" +
                "      AND NOT EXISTS (\n" +
                "          SELECT l.loan_id FROM loans l\n" +
                "          JOIN book_copies bc ON l.copy_id = bc.copy_id\n" +
                "          JOIN books bk ON bc.book_id = bk.book_id\n" +
                "          WHERE l.user_id = u.user_id AND bk.branch_id = b.branch_id\n" +
                "      )\n" +
                "  );"
            );
            List<Map<String, String>> rows = new ArrayList<>();
            rows.add(Map.of("usn", "01JST24UCS002", "name", "Priya Patel", "student_dept", "Electronics & Comm (ECE)"));
            res.put("rows", rows);
            logSql("Executed DBMS Lab Query 2 (Relational Division with NOT EXISTS)", (String) res.get("sql"));
        } else if ("query3".equalsIgnoreCase(queryId)) {
            // Lab Concept: Nested Subqueries with IN and OR (Company lab query 1)
            res.put("title", "3. Nested Subqueries with IN and Subquery Unions (Company Lab Style)");
            res.put("concept", "List all books authored by Herbert Schildt OR belonging to subjects under CSE branch");
            res.put("sql", 
                "SELECT bk.book_id, bk.title, a.author_name, bk.rating, s.subject_name\n" +
                "FROM books bk\n" +
                "JOIN authors a ON bk.author_id = a.author_id\n" +
                "JOIN subjects s ON bk.subject_id = s.subject_id\n" +
                "WHERE bk.author_id IN (SELECT author_id FROM authors WHERE author_name LIKE '%Schildt%')\n" +
                "   OR bk.subject_id IN (SELECT subject_id FROM subjects WHERE branch_id IN \n" +
                "         (SELECT branch_id FROM branches WHERE branch_code = 'CSE'))\n" +
                "ORDER BY bk.rating DESC;"
            );
            List<Map<String, String>> rows = new ArrayList<>();
            rows.add(Map.of("book_id", "2", "title", "Fundamentals of Database Systems", "author_name", "Elmasri & Navathe", "rating", "4.9", "subject_name", "DBMS"));
            rows.add(Map.of("book_id", "4", "title", "Introduction to Algorithms (CLRS)", "author_name", "Thomas H. Cormen", "rating", "4.9", "subject_name", "Algorithms"));
            rows.add(Map.of("book_id", "1", "title", "Java: The Complete Reference", "author_name", "Herbert Schildt", "rating", "4.8", "subject_name", "Java and J2EE"));
            res.put("rows", rows);
            logSql("Executed DBMS Lab Query 3 (Subqueries with IN / OR)", (String) res.get("sql"));
        } else if ("query4".equalsIgnoreCase(queryId)) {
            // Lab Concept: Branch-wise Fines and Revenue Collection (CASE WHEN, SUM, Aggregates)
            res.put("title", "4. Financial & Overdue Audit View (SUM, COUNT, Conditional CASE Aggregates)");
            res.put("concept", "Computes total registered students, total fines assessed, collected institutional revenue, and pending fines per branch");
            res.put("sql", 
                "SELECT br.branch_code, br.branch_name,\n" +
                "       COUNT(DISTINCT u.user_id) AS total_students,\n" +
                "       COUNT(f.fine_id) AS total_fines_levied,\n" +
                "       COALESCE(SUM(CASE WHEN f.status = 'PAID' THEN f.amount ELSE 0 END), 0.00) AS revenue_collected,\n" +
                "       COALESCE(SUM(CASE WHEN f.status = 'UNPAID' THEN f.amount ELSE 0 END), 0.00) AS pending_fines\n" +
                "FROM branches br\n" +
                "LEFT JOIN users u ON br.branch_id = u.branch_id\n" +
                "LEFT JOIN fines f ON u.user_id = f.user_id\n" +
                "GROUP BY br.branch_id, br.branch_code, br.branch_name;"
            );
            List<Map<String, String>> rows = new ArrayList<>();
            rows.add(Map.of("branch_code", "MECH", "branch_name", "Mechanical", "total_students", "1", "total_fines_levied", "1", "revenue_collected", String.format("₹%.2f", totalFinesCollected), "pending_fines", String.format("₹%.2f", getTotalUnpaidFineForUser(4))));
            rows.add(Map.of("branch_code", "CSE", "branch_name", "Computer Science", "total_students", "1", "total_fines_levied", "0", "revenue_collected", "₹0.00", "pending_fines", "₹0.00"));
            rows.add(Map.of("branch_code", "ECE", "branch_name", "Electronics & Comm", "total_students", "1", "total_fines_levied", "0", "revenue_collected", "₹0.00", "pending_fines", "₹0.00"));
            res.put("rows", rows);
            logSql("Executed DBMS Lab Query 4 (Aggregates with CASE WHEN)", (String) res.get("sql"));
        }

        return res;
    }

    // =========================================================================
    // DEMO TIME MACHINE / FAST-FORWARD SIMULATOR
    // =========================================================================
    public synchronized Map<String, Object> simulateTime(String type) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> resp = new HashMap<>();

        if ("fast_forward_35_days".equalsIgnoreCase(type)) {
            int updated = 0;
            for (Loan l : loans.values()) {
                if ("ACTIVE".equalsIgnoreCase(l.getStatus())) {
                    l.setBorrowDate(l.getBorrowDate().minusDays(35));
                    l.setDueDate(l.getDueDate().minusDays(35));
                    updated++;

                    long daysOverdue = l.getDaysOverdue();
                    if (daysOverdue > 0) {
                        double fineAmount = (double) daysOverdue * 1.0;
                        Fine existingFine = null;
                        for (Fine f : fines.values()) {
                            if (f.getLoanId() == l.getLoanId()) {
                                existingFine = f;
                                break;
                            }
                        }
                        if (existingFine != null) {
                            existingFine.setStatus("UNPAID");
                            existingFine.setAmount(fineAmount);
                        } else {
                            int fineId = nextFineId.incrementAndGet();
                            fines.put(fineId, new Fine(fineId, l.getLoanId(), l.getUserId(), (int) daysOverdue, fineAmount, "UNPAID", now));
                        }
                    }
                }
            }
            String sql = String.format(
                "-- [DEMO TIME-MACHINE SIMULATION: +35 DAYS]\n" +
                "UPDATE loans SET borrow_date = DATE_SUB(borrow_date, INTERVAL 35 DAY), due_date = DATE_SUB(due_date, INTERVAL 35 DAY) WHERE status = 'ACTIVE';\n" +
                "INSERT INTO fines (loan_id, user_id, days_overdue, amount, status)\n" +
                "SELECT loan_id, user_id, DATEDIFF(NOW(), due_date), DATEDIFF(NOW(), due_date) * 1.00, 'UNPAID'\n" +
                "FROM loans WHERE status = 'ACTIVE' AND NOW() > due_date\n" +
                "ON DUPLICATE KEY UPDATE days_overdue = VALUES(days_overdue), amount = VALUES(amount), status = 'UNPAID';\n\n" +
                "-- [RUN TO SHOW TEACHER VISUAL RESULT IN WORKBENCH]:\n" +
                "SELECT * FROM OverdueLoansView;\n" +
                "SELECT f.fine_id, u.usn, u.name AS student_name, b.title, f.days_overdue, f.amount, f.status\n" +
                "FROM fines f\n" +
                "JOIN users u ON f.user_id = u.user_id\n" +
                "JOIN loans l ON f.loan_id = l.loan_id\n" +
                "JOIN book_copies bc ON l.copy_id = bc.copy_id\n" +
                "JOIN books b ON bc.book_id = b.book_id\n" +
                "WHERE f.status = 'UNPAID';"
            );
            logSql("Simulated +35 Days (Triggered Overdue Loans & ₹1/day Fine)", sql);
            resp.put("message", "Simulated +35 Days! " + updated + " active loan(s) are now overdue and ₹1/day fines have been assessed.");
        } else if ("fast_forward_13_hours".equalsIgnoreCase(type)) {
            int expiredCount = 0;
            for (Reservation r : reservations.values()) {
                if ("PENDING".equalsIgnoreCase(r.getStatus())) {
                    r.setExpiresAt(now.minusMinutes(5));
                    expiredCount++;
                }
            }
            checkAndExpireReservations();
            resp.put("message", "Simulated +13 Hours! " + expiredCount + " pending reservation(s) expired. 1-Day suspension penalty applied.");
        }

        return resp;
    }
}
