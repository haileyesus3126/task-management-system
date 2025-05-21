package com.Haile.TaskManagementSystem.controller;

import com.Haile.TaskManagementSystem.model.Task;
import com.Haile.TaskManagementSystem.model.TaskAssignment;
import com.Haile.TaskManagementSystem.model.TaskFile;
import com.Haile.TaskManagementSystem.repository.TaskAssignmentRepository;
import com.Haile.TaskManagementSystem.repository.TaskFileRepository;
import com.Haile.TaskManagementSystem.repository.TaskRepository;
import com.Haile.TaskManagementSystem.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    @Autowired private TaskRepository taskRepository;
    @Autowired private TaskAssignmentRepository taskAssignmentRepository;
    @Autowired private TaskFileRepository taskFileRepository;
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
                             @RequestParam("uploadedFiles") MultipartFile[] uploadedFiles,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) throws IOException {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Invalid task data.");
            return "redirect:/tasks/assign";
        }

        task.setStatus("Assigned");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        Task savedTask = taskRepository.save(task);

        if (uploadedFiles != null && uploadedFiles.length > 0) {
            for (MultipartFile file : uploadedFiles) {
                if (file != null && !file.isEmpty()) {
                    String contentType = file.getContentType();
                    if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                        continue;
                    }
                    TaskFile taskFile = new TaskFile();
                    taskFile.setFileName(file.getOriginalFilename());
                    taskFile.setContentType(contentType);
                    taskFile.setData(file.getBytes());
                    taskFile.setUploadedAt(LocalDateTime.now());
                    taskFile.setTask(savedTask);
                    taskFileRepository.save(taskFile);
                }
            }
        }

        if (task.getAssignedTo() != null) {
            for (String username : task.getAssignedTo()) {
                TaskAssignment assignment = new TaskAssignment();
                assignment.setTask(savedTask);
                assignment.setUsername(username);
                assignment.setStatus("Assigned");
                taskAssignmentRepository.save(assignment);
            }
        }

        redirectAttributes.addFlashAttribute("success", "Task assigned with file(s) successfully.");
        return "redirect:/dashboard";
    }

    @GetMapping("/view-files")
    public String viewTaskFiles(Model model, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !(role.equalsIgnoreCase("Administrator") || role.equalsIgnoreCase("Supervisor"))) {
            return "redirect:/login";
        }
        List<TaskFile> files = taskFileRepository.findAll();
        model.addAttribute("files", files);
        return "view-task-files";
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
            redirectAttributes.addFlashAttribute("error", "Assignment not found.");
            return "redirect:/tasks/all-assignments";
        }

        String currentStatus = assignment.getStatus();
        String newStatus = null;
        if (role.equalsIgnoreCase("Supervisor") && currentStatus.equalsIgnoreCase("Assigned")) {
            newStatus = "Supervisor Reviewed";
        } else if (role.equalsIgnoreCase("Administrator") &&
                (currentStatus.equalsIgnoreCase("Assigned") || currentStatus.equalsIgnoreCase("Supervisor Reviewed"))) {
            newStatus = "Admin Reviewed";
        }

        if (newStatus == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid review status.");
            return "redirect:/tasks/all-assignments";
        }

        assignment.setStatus(newStatus);
        taskAssignmentRepository.save(assignment);
        redirectAttributes.addFlashAttribute("success", "Reviewed as: " + newStatus);
        return "redirect:/tasks/all-assignments";
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
            redirectAttributes.addFlashAttribute("error", "Unauthorized update attempt.");
            return "redirect:/tasks/my-tasks";
        }

        assignment.setStatus(status);
        if (file != null && !file.isEmpty()) {
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                redirectAttributes.addFlashAttribute("error", "Only images and PDFs are allowed.");
                return "redirect:/tasks/my-tasks";
            }
            String uniqueName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            assignment.setFileName(uniqueName);
            assignment.setContentType(contentType);
            assignment.setFileContent(file.getBytes());
        }

        assignment.setUpdatedAt(LocalDateTime.now());
        taskAssignmentRepository.save(assignment);
        redirectAttributes.addFlashAttribute("success", "Assignment updated successfully.");
        return "redirect:/tasks/my-tasks";
    }

    @GetMapping("/preview-image/{fileId}")
    @ResponseBody
    public ResponseEntity<byte[]> previewImage(@PathVariable int fileId) {
        Optional<TaskFile> fileOpt = taskFileRepository.findById(fileId);
        if (fileOpt.isEmpty()) return ResponseEntity.notFound().build();

        TaskFile file = fileOpt.get();
        if (!file.getContentType().startsWith("image/")) return ResponseEntity.badRequest().build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, file.getContentType())
                .body(file.getData());
    }

    @GetMapping("/preview-pdf/{fileId}")
    public ResponseEntity<Resource> previewPdf(@PathVariable int fileId) {
        Optional<TaskFile> fileOpt = taskFileRepository.findById(fileId);
        if (fileOpt.isEmpty()) return ResponseEntity.notFound().build();

        TaskFile file = fileOpt.get();
        if (!file.getContentType().equals("application/pdf")) return ResponseEntity.badRequest().build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, file.getContentType())
                .body(new ByteArrayResource(file.getData()));
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
        redirectAttributes.addFlashAttribute("success", "Task updated.");
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
            redirectAttributes.addFlashAttribute("success", "Task deleted.");
        }
        return "redirect:/dashboard";
    }
    @GetMapping("/download-user-file/{assignmentId}")
    public ResponseEntity<Resource> downloadUserFile(@PathVariable int assignmentId) {
        Optional<TaskAssignment> opt = taskAssignmentRepository.findById(assignmentId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        TaskAssignment assignment = opt.get();
        if (assignment.getFileContent() == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + assignment.getFileName() + "\"")
            .header(HttpHeaders.CONTENT_TYPE, assignment.getContentType())
            .body(new ByteArrayResource(assignment.getFileContent()));
    }


    @GetMapping("/download-db/{fileId}")
    public ResponseEntity<Resource> downloadFileFromDb(@PathVariable int fileId) {
        Optional<TaskFile> taskFileOpt = taskFileRepository.findById(fileId);
        if (taskFileOpt.isEmpty()) return ResponseEntity.notFound().build();

        TaskFile file = taskFileOpt.get();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, file.getContentType())
                .body(new ByteArrayResource(file.getData()));
    }
}
