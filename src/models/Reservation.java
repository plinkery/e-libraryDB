package models;

import java.time.LocalDateTime;

public class Reservation {
    private int reservationId;
    private int userId;
    private int copyId;
    private int bookId;
    private LocalDateTime reservedAt;
    private LocalDateTime expiresAt; // reservedAt + 12 hours
    private String status; // "PENDING", "FULFILLED", "EXPIRED", "CANCELLED"

    public Reservation(int reservationId, int userId, int copyId, int bookId, LocalDateTime reservedAt, LocalDateTime expiresAt, String status) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.copyId = copyId;
        this.bookId = bookId;
        this.reservedAt = reservedAt;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    public int getReservationId() { return reservationId; }
    public int getUserId() { return userId; }
    public int getCopyId() { return copyId; }
    public int getBookId() { return bookId; }
    public LocalDateTime getReservedAt() { return reservedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isExpired() {
        return "PENDING".equalsIgnoreCase(status) && LocalDateTime.now().isAfter(expiresAt);
    }
}
