package com.Haile.TaskManagementSystem.controller;

import com.Haile.TaskManagementSystem.model.Task;
import com.Haile.TaskManagementSystem.model.TaskAssignment;
import com.Haile.TaskManagementSystem.repository.TaskAssignmentRepository;
import com.Haile.TaskManagementSystem.repository.TaskRepository;
import com.Haile.TaskManagementSystem.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    @Autowired private TaskRepository taskRepository;
    @Autowired private TaskAssignmentRepository taskAssignmentRepository;
    @Autowired private UserRepository userRepository;

    @GetMapping("/assign")
    public String showAssignTaskForm(Model model, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !(role.equalsIgnoreCase("Administrator") || role.equalsIgnoreCase("Supervisor"))) {
            return "redirect:/login";
        }
        model.addAttribute("task", new Task());
        model.addAttribute("users", userRepository.findAll());
        return "assign-task";
    }

    @PostMapping("/assign")
    public String assignTask(@ModelAttribute Task task,
                             @RequestParam("files") MultipartFile[] files,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) throws IOException {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Invalid task data.");
            return "redirect:/tasks/assign";
        }

        List<String> uploadedFileNames = new ArrayList<>();
        if (files != null && files.length > 0) {
            String uploadPath = System.getProperty("user.dir") + File.separator + "uploads";
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    String contentType = file.getContentType();
                    if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                        redirectAttributes.addFlashAttribute("error", "Invalid file type. Only images and PDFs allowed.");
                        return "redirect:/tasks/assign";
                    }
                    String uniqueFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                    file.transferTo(new File(uploadDir, uniqueFileName));
                    uploadedFileNames.add(uniqueFileName);
                }
            }
        }

        task.setFileNames(uploadedFileNames);
        task.setStatus("Assigned");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        Task savedTask = taskRepository.save(task);

        if (task.getAssignedTo() != null) {
            for (String username : task.getAssignedTo()) {
                TaskAssignment assignment = new TaskAssignment();
                assignment.setTask(savedTask);
                assignment.setUsername(username);
                assignment.setStatus("Assigned");
                taskAssignmentRepository.save(assignment);
            }
        }
        redirectAttributes.addFlashAttribute("success", "Task assigned successfully.");
        return "redirect:/dashboard";
    }

    @GetMapping("/my-tasks")
    public String viewMyAssignments(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        if (username == null) return "redirect:/login";

        List<TaskAssignment> assignments = taskAssignmentRepository.findByUsername(username);
        model.addAttribute("assignments", assignments);
        return "user-tasks";
    }

    @PostMapping("/update-assignment-status")
    public String updateAssignmentStatus(@RequestParam int assignmentId,
                                         @RequestParam String status,
                                         @RequestParam(value = "file", required = false) MultipartFile file,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) throws IOException {
        TaskAssignment assignment = taskAssignmentRepository.findById(assignmentId).orElse(null);
        String username = (String) session.getAttribute("username");

        if (assignment == null || !assignment.getUsername().equals(username)) {
            redirectAttributes.addFlashAttribute("error", "Invalid assignment or permission denied.");
            return "redirect:/tasks/my-tasks";
        }

        assignment.setStatus(status);
        if (file != null && !file.isEmpty()) {
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                redirectAttributes.addFlashAttribute("error", "Invalid file type. Only images and PDFs allowed.");
                return "redirect:/tasks/my-tasks";
            }
            String uploadPath = System.getProperty("user.dir") + File.separator + "uploads";
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) uploadDir.mkdirs();
            String uniqueFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            file.transferTo(new File(uploadDir, uniqueFileName));
            assignment.setFileName(uniqueFileName);
        }
        taskAssignmentRepository.save(assignment);
        redirectAttributes.addFlashAttribute("success", "Assignment updated successfully.");
        return "redirect:/tasks/my-tasks";
    }

    @GetMapping("/all-assignments")
    public String viewAllAssignments(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     HttpSession session,
                                     Model model) {
        String role = (String) session.getAttribute("role");
        if (role == null || !(role.equalsIgnoreCase("Administrator") || role.equalsIgnoreCase("Supervisor"))) {
            return "redirect:/login";
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<TaskAssignment> assignmentsPage = taskAssignmentRepository.findAll(pageable);

        model.addAttribute("assignments", assignmentsPage);
        model.addAttribute("role", role);
        return "admin-assignments";
    }

    @PostMapping("/review-task")
    public String reviewTask(@RequestParam int taskId,
                             @RequestParam String username,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (role == null) return "redirect:/login";

        TaskAssignment assignment = taskAssignmentRepository.findByTaskIdAndUsername(taskId, username);
        if (assignment == null) {
            redirectAttributes.addFlashAttribute("error", "Assignment not found for the user.");
            return "redirect:/tasks/all-assignments";
        }

        String currentStatus = assignment.getStatus();
        if (currentStatus != null) currentStatus = currentStatus.trim().toLowerCase();
        else {
            redirectAttributes.addFlashAttribute("error", "Assignment has no status.");
            return "redirect:/tasks/all-assignments";
        }

        String newStatus = null;
        if (role.equalsIgnoreCase("Supervisor") && currentStatus.equals("assigned")) {
            newStatus = "Supervisor Reviewed";
        } else if (role.equalsIgnoreCase("Administrator") &&
                (currentStatus.equals("assigned") || currentStatus.equals("supervisor reviewed"))) {
            newStatus = "Admin Reviewed";
        }

        if (newStatus == null) {
            redirectAttributes.addFlashAttribute("error", "You cannot review this task in its current state.");
            return "redirect:/tasks/all-assignments";
        }

        assignment.setStatus(newStatus);
        taskAssignmentRepository.save(assignment);

        redirectAttributes.addFlashAttribute("success", "Task reviewed successfully as " + newStatus + ".");
        return "redirect:/tasks/all-assignments";
    }

    @GetMapping("/edit/{id}")
    public String editTaskForm(@PathVariable int id, Model model, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !(role.equalsIgnoreCase("Administrator") || role.equalsIgnoreCase("Supervisor"))) {
            return "redirect:/login";
        }
        Task task = taskRepository.findById(id).orElse(null);
        if (task == null) {
            model.addAttribute("error", "Task not found");
            return "redirect:/dashboard";
        }
        model.addAttribute("task", task);
        model.addAttribute("users", userRepository.findAll());
        return "edit-task";
    }

    @PostMapping("/update")
    public String updateTask(@ModelAttribute Task task, RedirectAttributes redirectAttributes) {
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        redirectAttributes.addFlashAttribute("success", "Task updated successfully.");
        return "redirect:/dashboard";
    }

    @GetMapping("/delete/{id}")
    public String deleteTask(@PathVariable int id, RedirectAttributes redirectAttributes, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !(role.equalsIgnoreCase("Administrator") || role.equalsIgnoreCase("Supervisor"))) {
            return "redirect:/login";
        }
        if (!taskRepository.existsById(id)) {
            redirectAttributes.addFlashAttribute("error", "Task not found.");
        } else {
            taskRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Task deleted successfully.");
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) throws MalformedURLException {
        Path path = Paths.get(System.getProperty("user.dir"), "uploads").resolve(filename);
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists() || !resource.isReadable()) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
