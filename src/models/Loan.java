package models;

import java.time.Duration;
import java.time.LocalDateTime;

public class Loan {
    private int loanId;
    private int userId;
    private int copyId;
    private int bookId;
    private LocalDateTime borrowDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private String status; // "ACTIVE", "RETURNED"

    public Loan(int loanId, int userId, int copyId, int bookId, LocalDateTime borrowDate, LocalDateTime dueDate, String status) {
        this.loanId = loanId;
        this.userId = userId;
        this.copyId = copyId;
        this.bookId = bookId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.status = status;
    }

    public int getLoanId() { return loanId; }
    public int getUserId() { return userId; }
    public int getCopyId() { return copyId; }
    public int getBookId() { return bookId; }
    public LocalDateTime getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDateTime borrowDate) { this.borrowDate = borrowDate; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDateTime returnDate) { this.returnDate = returnDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isOverdue() {
        LocalDateTime effectiveEnd = returnDate != null ? returnDate : LocalDateTime.now();
        return effectiveEnd.isAfter(dueDate);
    }

    public long getDaysOverdue() {
        LocalDateTime effectiveEnd = returnDate != null ? returnDate : LocalDateTime.now();
        if (!effectiveEnd.isAfter(dueDate)) return 0;
        Duration diff = Duration.between(dueDate, effectiveEnd);
        long days = diff.toDays();
        return days <= 0 ? 1 : days;
    }

    public double getCalculatedFine() {
        return (double) getDaysOverdue() * 1.00; // Rs. 1 per day
    }
}
