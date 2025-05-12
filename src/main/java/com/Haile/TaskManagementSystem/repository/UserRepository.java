package com.Haile.TaskManagementSystem.repository;

import com.Haile.TaskManagementSystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
    User findByUsernameAndPassword(String username, String password);
}
