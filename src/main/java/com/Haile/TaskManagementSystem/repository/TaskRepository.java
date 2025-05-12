package com.Haile.TaskManagementSystem.repository;

import com.Haile.TaskManagementSystem.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Integer> {
    List<Task> findByAssignedTo(String username);
}
