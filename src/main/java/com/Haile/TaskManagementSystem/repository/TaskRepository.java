package com.Haile.TaskManagementSystem.repository;

import com.Haile.TaskManagementSystem.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Integer> {

    // Find tasks assigned to a specific user (partial match in the list)
    List<Task> findByAssignedToContaining(String username);

    // Count tasks by status, case-insensitive (e.g., "Completed", "Pending")
    long countByStatusIgnoreCase(String status);
}
