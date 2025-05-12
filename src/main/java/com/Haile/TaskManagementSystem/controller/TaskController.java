package com.Haile.TaskManagementSystem.controller;

import com.Haile.TaskManagementSystem.model.Task;
import com.Haile.TaskManagementSystem.repository.TaskRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    // Show Assign Task Form
    @GetMapping("/assign")
    public String showAssignTaskForm(Model model, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || (!role.equalsIgnoreCase("Administrator") && !role.equalsIgnoreCase("Supervisor"))) {
            return "redirect:/login";
        }
        model.addAttribute("task", new Task());
        return "assign-task";
    }

    // Handle Task Form Submission
    @PostMapping("/assign")
    public String assignTask(@ModelAttribute Task task) {
        task.setStatus("Assigned");
        taskRepository.save(task);
        return "redirect:/dashboard";
    }

    // View My Tasks (User)
    @GetMapping("/my-tasks")
    public String viewMyTasks(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }
        model.addAttribute("tasks", taskRepository.findByAssignedTo(username));
        return "user-tasks";
    }

    // ✅ Updated file upload logic
    @PostMapping("/update-status")
    public String updateTaskStatus(@RequestParam int taskId,
                                   @RequestParam String status,
                                   @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {

        Task task = taskRepository.findById(taskId).orElse(null);

        if (task != null) {
            task.setStatus(status);

            if (file != null && !file.isEmpty()) {
                String uploadPath = System.getProperty("user.dir") + File.separator + "uploads";
                File uploadDir = new File(uploadPath);
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }

                File saveFile = new File(uploadDir, file.getOriginalFilename());
                file.transferTo(saveFile);

                task.setFileName(file.getOriginalFilename()); // ✅ Save filename
            }

            taskRepository.save(task);
        }

        return "redirect:/tasks/my-tasks";
    }

    // View All Tasks (Admin + Supervisor)
    @GetMapping("/all-tasks")
    public String viewAllTasks(HttpSession session, Model model) {
        String role = (String) session.getAttribute("role");
        if (role == null || (!role.equalsIgnoreCase("Administrator") && !role.equalsIgnoreCase("Supervisor"))) {
            return "redirect:/login";
        }
        model.addAttribute("tasks", taskRepository.findAll());
        model.addAttribute("role", role);
        return "all-tasks";
    }

    // Review Logic
    @PostMapping("/review-task")
    public String reviewTask(@RequestParam int taskId, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null) return "redirect:/login";

        Task task = taskRepository.findById(taskId).orElse(null);
        if (task != null) {
            if (role.equalsIgnoreCase("Supervisor") && task.getStatus().equalsIgnoreCase("Assigned")) {
                task.setStatus("Supervisor Reviewed");
            } else if (role.equalsIgnoreCase("Administrator") &&
                       (task.getStatus().equalsIgnoreCase("Assigned") || task.getStatus().equalsIgnoreCase("Supervisor Reviewed"))) {
                task.setStatus("Admin Reviewed");
            }
            taskRepository.save(task);
        }

        return "redirect:/tasks/all-tasks";
    }
}
