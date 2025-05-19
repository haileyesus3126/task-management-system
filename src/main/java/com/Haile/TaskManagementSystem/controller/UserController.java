package com.Haile.TaskManagementSystem.controller;

import com.Haile.TaskManagementSystem.model.User;
import com.Haile.TaskManagementSystem.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // ✅ Show User Management Page (Admins Only)
    @GetMapping
    public String listUsers(Model model, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !role.equalsIgnoreCase("Administrator")) {
            return "redirect:/login";
        }

        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("user", new User());
        return "user-management";
    }

    // ✅ Handle New User Creation (NO encryption)
    @PostMapping("/create")
    public String createUser(@Valid @ModelAttribute("user") User user,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Validation failed. Please fill in all fields.");
            return "redirect:/users";
        }

        if (userRepository.existsByUsername(user.getUsername())) {
            redirectAttributes.addFlashAttribute("error", "Username already exists!");
        } else {
            userRepository.save(user); // ❗ Saving password as-is (not encrypted)
            redirectAttributes.addFlashAttribute("success", "User created successfully!");
        }

        return "redirect:/users";
    }

    // ✅ Delete User
    @PostMapping("/delete")
    public String deleteUser(@RequestParam int id, RedirectAttributes redirectAttributes) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully.");
        } else {
            redirectAttributes.addFlashAttribute("error", "User not found.");
        }
        return "redirect:/users";
    }

    // ✅ Safe fallback for direct GET request to /delete
    @GetMapping("/delete")
    public String preventDeleteViaGet() {
        return "redirect:/users";
    }
}
