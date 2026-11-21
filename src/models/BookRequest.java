package models;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class BookRequest {
    private int requestId;
    private String title;
    private String author;
    private String branchCode;
    private String genreName;
    private int suggestedBy;
    private String suggestedByName;
    private int votesCount;
    private String status; // "PENDING", "PROCURED"
    private LocalDateTime createdAt;
    private Set<Integer> votedUserIds = new HashSet<>();

    public BookRequest(int requestId, String title, String author, String branchCode, String genreName, 
                       int suggestedBy, String suggestedByName, int initialVotes, String status, LocalDateTime createdAt) {
        this.requestId = requestId;
        this.title = title;
        this.author = author;
        this.branchCode = branchCode != null ? branchCode : "CSE";
        this.genreName = genreName != null ? genreName : "Computer Science & AI";
        this.suggestedBy = suggestedBy;
        this.suggestedByName = suggestedByName;
        this.votesCount = initialVotes;
        this.status = status != null ? status : "PENDING";
        this.createdAt = createdAt;
        this.votedUserIds.add(suggestedBy);
    }

    public int getRequestId() { return requestId; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getBranchCode() { return branchCode; }
    public String getGenreName() { return genreName; }
    public int getSuggestedBy() { return suggestedBy; }
    public String getSuggestedByName() { return suggestedByName; }
    public int getVotesCount() { return votesCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Set<Integer> getVotedUserIds() { return votedUserIds; }

    public boolean hasUserVoted(int userId) {
        return votedUserIds.contains(userId);
    }

    public boolean addVote(int userId) {
        if (votedUserIds.contains(userId)) {
            return false;
        }
        votedUserIds.add(userId);
        votesCount++;
        return true;
    }

    public boolean removeVote(int userId) {
        if (!votedUserIds.contains(userId)) {
            return false;
        }
        votedUserIds.remove(userId);
        if (votesCount > 0) votesCount--;
        return true;
    }
}
