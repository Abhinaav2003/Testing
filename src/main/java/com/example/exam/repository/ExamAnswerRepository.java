package com.example.exam.repository;

import com.example.exam.model.ExamAnswer;
import com.example.exam.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamAnswerRepository
        extends JpaRepository<ExamAnswer, Long> {

    boolean existsByQuestion(Question question);
}