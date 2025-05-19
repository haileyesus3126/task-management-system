package com.Haile.TaskManagementSystem.repository;

import com.Haile.TaskManagementSystem.model.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Integer> {
    List<TaskComment> findByAssignmentIdOrderByCreatedAtAsc(int assignmentId);
}
