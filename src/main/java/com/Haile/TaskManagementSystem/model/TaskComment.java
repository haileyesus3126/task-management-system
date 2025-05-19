package com.Haile.TaskManagementSystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class TaskComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // 🔗 Link to TaskAssignment
    @ManyToOne
    @JoinColumn(name = "assignment_id")
    private TaskAssignment assignment;

    // 💬 Comment body
    @Lob
    private String content;

    // 🧑 Commenter username
    private String author;

    // ⏰ Timestamp
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters & Setters

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public TaskAssignment getAssignment() { return assignment; }

    public void setAssignment(TaskAssignment assignment) { this.assignment = assignment; }

    public String getContent() { return content; }

    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }

    public void setAuthor(String author) { this.author = author; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
