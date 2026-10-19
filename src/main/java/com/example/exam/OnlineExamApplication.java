package com.example.exam;

import com.example.exam.model.User;
import com.example.exam.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class OnlineExamApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineExamApplication.class, args);
    }

    @Bean
    CommandLineRunner createAdminUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            String adminUsername = "admin";

            if (userRepository.findByUsername(adminUsername).isEmpty()) {

                User adminUser = new User();
                adminUser.setUsername("admin");
                adminUser.setPassword(
                        passwordEncoder.encode("adminpass"));
                adminUser.setRole("ROLE_ADMIN");
                adminUser.setFullName("Admin");

                userRepository.save(adminUser);

                System.out.println(
                    ">>> Admin user created successfully! <<<");
            } else {
                System.out.println(
                    ">>> Admin user already exists. <<<");
            }
        };
    }
}