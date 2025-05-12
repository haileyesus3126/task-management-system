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
            session.setAttribute("username", user.getUsername());
            session.setAttribute("role", user.getRole());
            return "redirect:/dashboard";
        } else {
            model.addAttribute("error", true);
            return "login";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session) {
        String role = (String) session.getAttribute("role");
        System.out.println("Logged in role: " + role); // Debug line

        if (role == null) {
            return "redirect:/login";
        }

        if (role.equalsIgnoreCase("Administrator")) {
            return "admin-dashboard";
        } else if (role.equalsIgnoreCase("Supervisor")) {
            return "supervisor-dashboard";
        } else if (role.equalsIgnoreCase("User")) {
            return "user-dashboard";
        } else {
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();  // Destroys the session
        return "redirect:/login"; // Redirect to login page
    }

}
