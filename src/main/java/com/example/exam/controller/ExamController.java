package com.example.exam.controller;

import com.example.exam.model.*;
import com.example.exam.repository.*;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/exam")
public class ExamController {

    private static final ZoneId INDIA_ZONE =
            ZoneId.of("Asia/Kolkata");

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamResultRepository examResultRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamAnswerRepository examAnswerRepository;

    // Display the exam page and record its start time
    @GetMapping("/{examId}")
    public String getExamPage(
            @PathVariable Long examId,
            Model model,
            HttpSession session) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String currentUsername = authentication.getName();

        User student = userRepository.findByUsername(currentUsername)
                .orElseThrow(() ->
                        new RuntimeException("Student not found"));

        model.addAttribute("student", student);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid exam Id: " + examId));

        model.addAttribute("exam", exam);

        // Load exam questions
        List<Question> questions = exam.getQuestions();
        questions.size();

        model.addAttribute("questions", questions);

        // Get or create the session's exam start-time map
        @SuppressWarnings("unchecked")
        Map<Long, LocalDateTime> startTimes =
                (Map<Long, LocalDateTime>)
                        session.getAttribute("examStartTimes");

        if (startTimes == null) {
            startTimes = new HashMap<>();
            session.setAttribute("examStartTimes", startTimes);
        }

        // Preserve the original start time on page refresh
        startTimes.putIfAbsent(
                examId,
                LocalDateTime.now(INDIA_ZONE)
        );

        return "student/exam_page";
    }

    // Submit the exam and calculate the result
    @PostMapping("/submit")
    public String submitExam(
            @RequestParam Long examId,
            @RequestParam Map<String, String> submittedAnswers,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String currentUsername = authentication.getName();

        User student = userRepository.findByUsername(currentUsername)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid exam Id: " + examId));

        // Record submission time in Indian Standard Time
        LocalDateTime submissionTime =
                LocalDateTime.now(INDIA_ZONE);

        // Retrieve the exam start time
        @SuppressWarnings("unchecked")
        Map<Long, LocalDateTime> startTimes =
                (Map<Long, LocalDateTime>)
                        session.getAttribute("examStartTimes");

        LocalDateTime startTime = null;

        if (startTimes != null) {
            startTime = startTimes.remove(examId);

            // Remove the session attribute when no timers remain
            if (startTimes.isEmpty()) {
                session.removeAttribute("examStartTimes");
            }
        }

        // Calculate duration in seconds
        long durationSeconds = startTime == null
                ? 0L
                : Math.max(
                        0L,
                        Duration.between(
                                startTime,
                                submissionTime
                        ).getSeconds()
                );

        int totalMarks = 0;
        int scoreAchieved = 0;

        // Create the exam result
        ExamResult result = new ExamResult();

        result.setStudent(student);
        result.setExam(exam);
        result.setStartTime(startTime);
        result.setSubmissionTime(submissionTime);
        result.setDurationSeconds(durationSeconds);

        // Save first to generate the result ID
        examResultRepository.save(result);

        // Evaluate answers
        for (Question question : exam.getQuestions()) {

            totalMarks += question.getMarks();

            String answerKey = "q_" + question.getId();
            int selectedOption = 0;

            if (submittedAnswers.containsKey(answerKey)) {
                try {
                    selectedOption = Integer.parseInt(
                            submittedAnswers.get(answerKey)
                    );
                } catch (NumberFormatException e) {
                    selectedOption = 0;
                }

                // Award marks for a correct answer
                if (selectedOption == question.getCorrectAnswer()) {
                    scoreAchieved += question.getMarks();
                }
            }

            // Save the selected answer
            ExamAnswer examAnswer =
                    new ExamAnswer(
                            result,
                            question,
                            selectedOption
                    );

            examAnswerRepository.save(examAnswer);
            result.addAnswer(examAnswer);
        }

        // Update the final score
        result.setScoreAchieved(scoreAchieved);
        result.setTotalMarks(totalMarks);

        examResultRepository.save(result);

        // Send result information to the result page
        redirectAttributes.addFlashAttribute(
                "score", scoreAchieved);

        redirectAttributes.addFlashAttribute(
                "total", totalMarks);

        redirectAttributes.addFlashAttribute(
                "resultId", result.getId());

        return "redirect:/student/result";
    }
}