package com.example.exam.controller;

import com.example.exam.model.Exam;
import com.example.exam.model.ExamAnswer;
import com.example.exam.model.ExamResult;
import com.example.exam.model.Question;
import com.example.exam.model.User;

import com.example.exam.repository.ExamAnswerRepository;
import com.example.exam.repository.ExamRepository;
import com.example.exam.repository.ExamResultRepository;
import com.example.exam.repository.QuestionRepository;
import com.example.exam.repository.UserRepository;
import com.example.exam.service.ExportService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamResultRepository examResultRepository;

    @Autowired
    private ExamAnswerRepository examAnswerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ExportService exportService;

    // ==================== ADMIN DASHBOARD ====================

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {

        model.addAttribute(
                "totalStudents",
                userRepository.countByRole("ROLE_STUDENT"));

        model.addAttribute(
                "totalExams",
                examRepository.count());

        model.addAttribute(
                "totalQuestions",
                questionRepository.count());

        model.addAttribute(
                "totalSubmissions",
                examResultRepository.count());

        List<Exam> allExams = examRepository.findAll();

        model.addAttribute(
                "chartLabels",
                allExams.stream()
                        .map(Exam::getTitle)
                        .collect(Collectors.toList()));

        model.addAttribute(
                "chartData",
                allExams.stream()
                        .map(exam -> exam.getResults().size())
                        .collect(Collectors.toList()));

        // Latest five submissions first
        List<ExamResult> recentResults =
                examResultRepository.findTop5ByOrderBySubmissionTimeDesc();

        model.addAttribute("recentResults", recentResults);

        return "admin/dashboard";
    }

    // ==================== MANAGE EXAMS ====================

    @GetMapping("/manage-exams")
    public String getManageExamsPage(Model model) {

        model.addAttribute("exams", examRepository.findAll());

        return "admin/manage_exams";
    }

    @GetMapping("/exam/add")
    public String showAddExamForm(Model model) {

        model.addAttribute("exam", new Exam());

        return "admin/add_exam";
    }

    @PostMapping("/exam/add")
    public String addExam(
            @ModelAttribute Exam exam,
            RedirectAttributes redirectAttributes) {

        examRepository.save(exam);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Exam added successfully.");

        return "redirect:/admin/manage-exams";
    }

    // ==================== ADD QUESTIONS ====================

    @GetMapping("/exam/{examId}/question/add")
    public String showAddQuestionForm(
            @PathVariable Long examId,
            Model model) {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid exam ID: " + examId));

        Question question = new Question();
        question.setExam(exam);

        model.addAttribute("question", question);
        model.addAttribute("examId", examId);

        return "admin/add_question";
    }

    @PostMapping("/exam/question/add")
    public String addQuestion(
            @ModelAttribute Question question,
            @RequestParam Long examId,
            RedirectAttributes redirectAttributes) {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid exam ID: " + examId));

        question.setExam(exam);
        questionRepository.save(question);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Question added successfully!");

        return "redirect:/admin/exam/" + examId + "/question/add";
    }

    // ==================== EXAM RESULTS ====================

    @GetMapping("/results/{examId}")
    public String viewResults(
            @PathVariable Long examId,
            Model model) {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid exam ID: " + examId));

        // Sort by submission timestamp: newest first
        List<ExamResult> results =
                examResultRepository
                        .findByExamOrderBySubmissionTimeDesc(exam);

        model.addAttribute("exam", exam);
        model.addAttribute("results", results);

        return "admin/view_results";
    }

    // ==================== EXPORT MARKS TO EXCEL ====================

    @GetMapping("/results/{examId}/export")
    public ResponseEntity<byte[]> exportResultsToExcel(
            @PathVariable Long examId) throws IOException {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid exam ID: " + examId));

        List<ExamResult> results =
                examResultRepository
                        .findByExamOrderBySubmissionTimeDesc(exam);

        byte[] file = exportService.buildMarksWorkbook(exam, results);

        String fileName = ExportService.safeFileName(exam.getTitle())
                + "_marks.xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(file.length)
                .body(file);
    }

    // ==================== DELETE EXAM ====================

    @PostMapping("/exam/delete/{examId}")
    @Transactional
    public String deleteExam(
            @PathVariable Long examId,
            RedirectAttributes redirectAttributes) {

        try {
            Exam exam = examRepository.findById(examId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Exam not found: " + examId));

            // 1. Delete answers belonging to this exam's results
            List<ExamResult> results =
                    examResultRepository.findByExam(exam);

            for (ExamResult result : results) {
                examAnswerRepository.deleteAll(result.getAnswers());
            }

            examAnswerRepository.flush();

            // 2. Delete the exam results
            examResultRepository.deleteAll(results);
            examResultRepository.flush();

            // 3. Delete the exam questions
            List<Question> questions = exam.getQuestions();

            questionRepository.deleteAll(questions);
            questionRepository.flush();

            // 4. Delete the exam
            examRepository.delete(exam);
            examRepository.flush();

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Exam and its associated questions, answers, "
                            + "and results deleted successfully.");

        } catch (DataIntegrityViolationException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Cannot delete this exam because related database "
                            + "records still exist. Check the foreign-key "
                            + "relationships and server logs.");

            throw e;

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to delete exam: " + e.getMessage());

            throw e;
        }

        return "redirect:/admin/manage-exams";
    }

    // ==================== EDIT EXAM ====================

    @GetMapping("/exam/edit/{examId}")
    public String showEditExamForm(
            @PathVariable Long examId,
            Model model) {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid exam ID: " + examId));

        model.addAttribute("exam", exam);

        return "admin/edit_exam";
    }

    @PostMapping("/exam/update/{examId}")
    public String updateExam(
            @PathVariable Long examId,
            @ModelAttribute Exam updatedExam) {

        Exam existingExam = examRepository.findById(examId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid exam ID: " + examId));

        existingExam.setTitle(updatedExam.getTitle());

        existingExam.setDurationInMinutes(
                updatedExam.getDurationInMinutes());

        examRepository.save(existingExam);

        return "redirect:/admin/manage-exams";
    }

    // ==================== MANAGE QUESTIONS ====================

    @GetMapping("/exam/manage-questions/{examId}")
    public String showManageQuestionsPage(
            @PathVariable Long examId,
            Model model) {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid exam ID: " + examId));

        model.addAttribute("exam", exam);
        model.addAttribute("questions", exam.getQuestions());

        return "admin/manage_questions";
    }

    @GetMapping("/question/edit/{questionId}")
    public String showEditQuestionForm(
            @PathVariable Long questionId,
            Model model) {

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid question ID: " + questionId));

        model.addAttribute("question", question);

        return "admin/edit_question";
    }

    @PostMapping("/question/update/{questionId}")
    public String updateQuestion(
            @PathVariable Long questionId,
            @ModelAttribute Question updatedQuestion) {

        Question existingQuestion =
                questionRepository.findById(questionId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid question ID: " + questionId));

        existingQuestion.setText(updatedQuestion.getText());
        existingQuestion.setOption1(updatedQuestion.getOption1());
        existingQuestion.setOption2(updatedQuestion.getOption2());
        existingQuestion.setOption3(updatedQuestion.getOption3());
        existingQuestion.setOption4(updatedQuestion.getOption4());

        existingQuestion.setCorrectAnswer(
                updatedQuestion.getCorrectAnswer());

        existingQuestion.setMarks(updatedQuestion.getMarks());

        questionRepository.save(existingQuestion);

        return "redirect:/admin/exam/manage-questions/"
                + existingQuestion.getExam().getId();
    }

    @GetMapping("/exam/{examId}/question/delete/{questionId}")
    @Transactional
    public String deleteQuestion(
            @PathVariable Long examId,
            @PathVariable Long questionId,
            RedirectAttributes redirectAttributes) {

        try {
            Question question =
                    questionRepository.findById(questionId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Question not found: "
                                                    + questionId));

            if (question.getExam() == null
                    || !question.getExam().getId().equals(examId)) {

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "The question does not belong to this exam.");

                return "redirect:/admin/exam/manage-questions/"
                        + examId;
            }

            // Protect historical student answers
            boolean hasAnswers =
                    examAnswerRepository.existsByQuestion(question);

            if (hasAnswers) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Cannot delete this question because student "
                                + "answers reference it.");

                return "redirect:/admin/exam/manage-questions/"
                        + examId;
            }

            questionRepository.delete(question);
            questionRepository.flush();

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Question deleted successfully.");

        } catch (DataIntegrityViolationException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Cannot delete this question because related "
                            + "records exist.");
        }

        return "redirect:/admin/exam/manage-questions/" + examId;
    }

    // ==================== MANAGE STUDENTS ====================

    @GetMapping("/students")
    public String getManageStudentsPage(Model model) {

        List<User> students =
                userRepository.findByRole("ROLE_STUDENT");

        model.addAttribute("students", students);

        return "admin/manage_students";
    }

    @GetMapping("/student/delete/{id}")
    @Transactional
    public String deleteStudent(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            User student = userRepository.findById(id)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Invalid student ID: " + id));

            examResultRepository.deleteByStudent(student);
            examResultRepository.flush();

            userRepository.delete(student);
            userRepository.flush();

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Student account and associated results "
                            + "deleted successfully.");

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to delete student: " + e.getMessage());

            throw e;
        }

        return "redirect:/admin/students";
    }

    // ==================== RESET STUDENT PASSWORD ====================

    @PostMapping("/student/reset-password")
    public String resetStudentPassword(
            @RequestParam("userId") Long userId,
            @RequestParam("newPassword") String newPassword,
            RedirectAttributes redirectAttributes) {

        if (newPassword == null || newPassword.trim().isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Password cannot be empty.");

            return "redirect:/admin/students";
        }

        try {
            User student = userRepository.findById(userId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Invalid student ID: " + userId));

            student.setPassword(
                    passwordEncoder.encode(newPassword));

            userRepository.save(student);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Password for " + student.getUsername()
                            + " has been reset.");

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to reset the student password.");
        }

        return "redirect:/admin/students";
    }
}