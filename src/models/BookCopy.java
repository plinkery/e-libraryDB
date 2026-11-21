package models;

public class BookCopy {
    private int copyId;
    private int bookId;
    private String copyCode;
    private String status; // "AVAILABLE", "RESERVED", "BORROWED"
    private String rackLocation;

    public BookCopy(int copyId, int bookId, String copyCode, String status, String rackLocation) {
        this.copyId = copyId;
        this.bookId = bookId;
        this.copyCode = copyCode;
        this.status = status;
        this.rackLocation = rackLocation;
    }

    public int getCopyId() { return copyId; }
    public int getBookId() { return bookId; }
    public String getCopyCode() { return copyCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRackLocation() { return rackLocation; }
}
