package com.Haile.TaskManagementSystem.controller;

import com.Haile.TaskManagementSystem.model.User;
import com.Haile.TaskManagementSystem.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {

        User user = userRepository.findByUsernameAndPassword(username, password);

        if (user != null) {
            // Set session attributes
            session.setAttribute("username", user.getUsername());
            session.setAttribute("role", user.getRole());

            // Debug: log role
            System.out.println("Logged in as: " + user.getRole());

            // ✅ Role-based redirect
            String role = user.getRole().toLowerCase();

            switch (role) {
                case "administrator":
                    return "redirect:/tasks/dashboard";
                case "supervisor":
                    return "redirect:/tasks/supervisor-dashboard";
                case "user":
                    return "redirect:/tasks/user-dashboard";
                default:
                    return "redirect:/login";
            }

        } else {
            model.addAttribute("error", true);
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();  // Destroy the session
        return "redirect:/login";
    }
}
