package com.Haile.TaskManagementSystem.repository;

import com.Haile.TaskManagementSystem.model.LogoImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LogoImageRepository extends JpaRepository<LogoImage, Long> {
    Optional<LogoImage> findTopByOrderByUploadedAtDesc();
}
