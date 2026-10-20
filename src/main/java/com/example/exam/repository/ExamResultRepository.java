package com.example.exam.repository;

import com.example.exam.model.Exam;
import com.example.exam.model.ExamResult;
import com.example.exam.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamResultRepository
        extends JpaRepository<ExamResult, Long> {

    // Find results for a particular exam
    List<ExamResult> findByExam(Exam exam);

    // Find results for a particular exam, newest submission first
    List<ExamResult> findByExamOrderBySubmissionTimeDesc(Exam exam);

    // Find the five most recent results
    List<ExamResult> findTop5ByOrderBySubmissionTimeDesc();

    // Delete all results belonging to a student
    void deleteByStudent(User student);

    // Find all results belonging to a student
    List<ExamResult> findByStudent(User student);

    // Find a student's results, newest first
    List<ExamResult> findByStudentOrderBySubmissionTimeDesc(User student);
}