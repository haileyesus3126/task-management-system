package com.Haile.TaskManagementSystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
public class Task {

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ElementCollection
    private List<String> assignedTo = new ArrayList<>();

    private String title;
    private String description;
    private String status;

    @ElementCollection
    private List<String> fileNames = new ArrayList<>(); // Optional legacy field

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    private boolean deleted = false;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    private LocalDate dueDate;

    private int progress;

    // ✅ Remove orphanRemoval to prevent deletion during updates
    @JsonIgnore
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    private List<TaskFile> files = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    private List<TaskAssignment> assignments = new ArrayList<>();

    // --- Getters and Setters ---

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public List<String> getAssignedTo() {
        return assignedTo;
    }
    public void setAssignedTo(List<String> assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getFileNames() {
        return fileNames;
    }
    public void setFileNames(List<String> fileNames) {
        this.fileNames = fileNames;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Priority getPriority() {
        return priority;
    }
    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public int getProgress() {
        return progress;
    }
    public void setProgress(int progress) {
        this.progress = progress;
    }

    public boolean isDeleted() {
        return deleted;
    }
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public List<TaskFile> getFiles() {
        return files;
    }
    public void setFiles(List<TaskFile> files) {
        this.files = files;
    }

    public List<TaskAssignment> getAssignments() {
        return assignments;
    }
    public void setAssignments(List<TaskAssignment> assignments) {
        this.assignments = assignments;
    }
}
