package models;

import java.time.LocalDateTime;

public class Fine {
    private int fineId;
    private int loanId;
    private int userId;
    private int daysOverdue;
    private double amount;
    private String status; // "UNPAID", "PAID"
    private LocalDateTime assessedAt;
    private LocalDateTime paidAt;

    public Fine(int fineId, int loanId, int userId, int daysOverdue, double amount, String status, LocalDateTime assessedAt) {
        this.fineId = fineId;
        this.loanId = loanId;
        this.userId = userId;
        this.daysOverdue = daysOverdue;
        this.amount = amount;
        this.status = status;
        this.assessedAt = assessedAt;
    }

    public int getFineId() { return fineId; }
    public int getLoanId() { return loanId; }
    public int getUserId() { return userId; }
    public int getDaysOverdue() { return daysOverdue; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getAssessedAt() { return assessedAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public void markPaid() {
        this.status = "PAID";
        this.paidAt = LocalDateTime.now();
    }
}
