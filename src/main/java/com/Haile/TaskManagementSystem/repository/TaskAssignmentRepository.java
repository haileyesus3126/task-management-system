package com.Haile.TaskManagementSystem.repository;

import com.Haile.TaskManagementSystem.model.TaskAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Integer> {

    List<TaskAssignment> findByUsername(String username);
    List<TaskAssignment> findByTaskId(int taskId);

    // ✅ For task review logic
    TaskAssignment findByTaskIdAndUsername(int taskId, String username);

    // ✅ NEW: Paginated fetch for all assignments
    Page<TaskAssignment> findAll(Pageable pageable);
}
