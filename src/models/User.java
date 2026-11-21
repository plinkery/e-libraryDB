package models;

import java.time.LocalDateTime;

public class User {
    private int userId;
    private String usn; // e.g. "01JST24UCS001", "01JSTLIB001"
    private String name;
    private String email;
    private String password;
    private String role; // "STUDENT" or "LIBRARIAN"
    private String branchCode; // "CSE", "ECE", "MECH"
    private Integer semester;
    private String phone;
    private LocalDateTime reservationBlockedUntil;

    public User(int userId, String usn, String name, String email, String password, String role, String branchCode, Integer semester, String phone) {
        this.userId = userId;
        this.usn = usn;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.branchCode = branchCode;
        this.semester = semester;
        this.phone = phone;
    }

    public int getUserId() { return userId; }
    public String getUsn() { return usn; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getBranchCode() { return branchCode; }
    public Integer getSemester() { return semester; }
    public String getPhone() { return phone; }
    public LocalDateTime getReservationBlockedUntil() { return reservationBlockedUntil; }
    public void setReservationBlockedUntil(LocalDateTime until) { this.reservationBlockedUntil = until; }

    public boolean isReservationBlocked() {
        return reservationBlockedUntil != null && LocalDateTime.now().isBefore(reservationBlockedUntil);
    }
}
