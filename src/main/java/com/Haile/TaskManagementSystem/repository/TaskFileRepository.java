package com.Haile.TaskManagementSystem.repository;

import com.Haile.TaskManagementSystem.model.TaskFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskFileRepository extends JpaRepository<TaskFile, Integer> {
    List<TaskFile> findByTaskId(int taskId);
}
