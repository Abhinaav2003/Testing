package com.example.exam.controller;

import com.example.exam.model.User;
import com.example.exam.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    private static final Set<String> ALLOWED_AVATARS = Set.of(
        "avatar1.png",
        "avatar2.png",
        "avatar3.png",
        "avatar4.png"
    );

    @GetMapping("/")
    public String home() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {

            boolean isAdmin = authentication.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_ADMIN"::equals);

            if (isAdmin) {
                return "redirect:/admin/dashboard";
            }

            return "redirect:/student/dashboard";
        }

        return "index";
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerStudent(
            @ModelAttribute("user") User user,
            BindingResult bindingResult,
            @RequestParam(name = "avatar", required = false)
            String avatar,
            Model model) {

        // Check for an existing account.
        if (user.getUsername() != null
                && !user.getUsername().isBlank()
                && userService.findByUsername(user.getUsername()) != null) {

            bindingResult.rejectValue(
                "username",
                "error.user",
                "An account already exists with that email."
            );
        }

        // Validate the selected predefined avatar.
        if (avatar == null || !ALLOWED_AVATARS.contains(avatar)) {
            model.addAttribute(
                "avatarError",
                "Please select one of the four available avatars."
            );
        }

        if (bindingResult.hasErrors()
                || avatar == null
                || !ALLOWED_AVATARS.contains(avatar)) {

            model.addAttribute("user", user);
            return "register";
        }

        // Store only the image path, not the image itself.
        user.setProfilePicUrl("/images/avatars/" + avatar);

        // New registrations must be students.
        user.setRole("ROLE_STUDENT");

        // Save using the existing service.
        userService.saveStudent(user);

        return "redirect:/register?success";
    }
}