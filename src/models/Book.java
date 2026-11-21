package models;

import java.util.ArrayList;
import java.util.List;

public class Book {
    private int bookId;
    private String title;
    private String isbn;
    private String branchCode; // "CSE", "MECH", "ECE", "CIVIL", "EEE"
    private String subjectName; // "Database Management Systems", etc.
    private String genreName; // "Core Syllabus Textbook", etc.
    private String publisher;
    private String author;
    private String edition;
    private int pubYear;
    private double rating; // 1.0 - 5.0 (like sailors rating)
    private String description;
    private List<BookCopy> copies = new ArrayList<>();

    public Book(int bookId, String title, String isbn, String branchCode, String subjectName, String genreName, 
                String publisher, String author, String edition, int pubYear, double rating, String description) {
        this.bookId = bookId;
        this.title = title;
        this.isbn = isbn;
        this.branchCode = branchCode;
        this.subjectName = subjectName;
        this.genreName = genreName;
        this.publisher = publisher;
        this.author = author;
        this.edition = edition;
        this.pubYear = pubYear;
        this.rating = rating;
        this.description = description;
    }

    public int getBookId() { return bookId; }
    public String getTitle() { return title; }
    public String getIsbn() { return isbn; }
    public String getBranchCode() { return branchCode; }
    public String getSubjectName() { return subjectName; }
    public String getGenreName() { return genreName; }
    public String getPublisher() { return publisher; }
    public String getAuthor() { return author; }
    public String getEdition() { return edition; }
    public int getPubYear() { return pubYear; }
    public double getRating() { return rating; }
    public String getDescription() { return description; }
    public List<BookCopy> getCopies() { return copies; }

    public void addCopy(BookCopy copy) {
        this.copies.add(copy);
    }

    public int getTotalCopiesCount() {
        return copies.size();
    }

    public int getAvailableCopiesCount() {
        int count = 0;
        for (BookCopy c : copies) {
            if ("AVAILABLE".equalsIgnoreCase(c.getStatus())) {
                count++;
            }
        }
        return count;
    }

    public BookCopy getFirstAvailableCopy() {
        for (BookCopy c : copies) {
            if ("AVAILABLE".equalsIgnoreCase(c.getStatus())) {
                return c;
            }
        }
        return null;
    }
}
