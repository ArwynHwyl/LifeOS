package com.example.demo.assessment.config;

import com.example.demo.assessment.entity.Assessment;
import com.example.demo.assessment.entity.AssessmentOption;
import com.example.demo.assessment.entity.AssessmentQuestion;
import com.example.demo.assessment.repository.AssessmentRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class AssessmentSeedConfig {

    @Bean
    CommandLineRunner seedAssessmentData(AssessmentRepository assessmentRepository) {
        return args -> {
            if (assessmentRepository.count() > 0) {
                return;
            }
            assessmentRepository.saveAll(List.of(
                    algebraFundamentals(),
                    geometryEssentials()
            ));
        };
    }

    private Assessment algebraFundamentals() {
        Assessment assessment = new Assessment(
                "Algebra Fundamentals Quiz", "Test your grasp of core algebra concepts.", "ALGEBRA", 1);
        int i = 1;

        AssessmentQuestion q1 = new AssessmentQuestion("Solve for x: 2x + 6 = 14", i++);
        q1.addOption(new AssessmentOption("x = 4", true, 1));
        q1.addOption(new AssessmentOption("x = 10", false, 2));
        q1.addOption(new AssessmentOption("x = 8", false, 3));
        q1.addOption(new AssessmentOption("x = 6", false, 4));
        assessment.addQuestion(q1);

        AssessmentQuestion q2 = new AssessmentQuestion("What is the slope of the line y = 3x − 2?", i++);
        q2.addOption(new AssessmentOption("-2", false, 1));
        q2.addOption(new AssessmentOption("3", true, 2));
        q2.addOption(new AssessmentOption("2", false, 3));
        q2.addOption(new AssessmentOption("-3", false, 4));
        assessment.addQuestion(q2);

        AssessmentQuestion q3 = new AssessmentQuestion("Factor: x² − 9", i++);
        q3.addOption(new AssessmentOption("(x − 3)(x + 3)", true, 1));
        q3.addOption(new AssessmentOption("(x − 9)(x + 1)", false, 2));
        q3.addOption(new AssessmentOption("(x − 3)²", false, 3));
        q3.addOption(new AssessmentOption("(x + 9)(x − 1)", false, 4));
        assessment.addQuestion(q3);

        AssessmentQuestion q4 = new AssessmentQuestion("Using the quadratic formula, how many real solutions does x² + 4 = 0 have?", i++);
        q4.addOption(new AssessmentOption("0", true, 1));
        q4.addOption(new AssessmentOption("1", false, 2));
        q4.addOption(new AssessmentOption("2", false, 3));
        q4.addOption(new AssessmentOption("4", false, 4));
        assessment.addQuestion(q4);

        AssessmentQuestion q5 = new AssessmentQuestion("Simplify: (3x²)(2x³)", i);
        q5.addOption(new AssessmentOption("6x⁵", true, 1));
        q5.addOption(new AssessmentOption("5x⁵", false, 2));
        q5.addOption(new AssessmentOption("6x⁶", false, 3));
        q5.addOption(new AssessmentOption("5x⁶", false, 4));
        assessment.addQuestion(q5);

        return assessment;
    }

    private Assessment geometryEssentials() {
        Assessment assessment = new Assessment(
                "Geometry Essentials Quiz", "Check your understanding of shapes, area and the Pythagorean theorem.", "GEOMETRY", 2);
        int i = 1;

        AssessmentQuestion q1 = new AssessmentQuestion("A right triangle has legs of 6 and 8. What is the hypotenuse?", i++);
        q1.addOption(new AssessmentOption("10", true, 1));
        q1.addOption(new AssessmentOption("14", false, 2));
        q1.addOption(new AssessmentOption("48", false, 3));
        q1.addOption(new AssessmentOption("7", false, 4));
        assessment.addQuestion(q1);

        AssessmentQuestion q2 = new AssessmentQuestion("What is the area of a circle with radius 5? (use π ≈ 3.14)", i++);
        q2.addOption(new AssessmentOption("31.4", false, 1));
        q2.addOption(new AssessmentOption("78.5", true, 2));
        q2.addOption(new AssessmentOption("15.7", false, 3));
        q2.addOption(new AssessmentOption("25", false, 4));
        assessment.addQuestion(q2);

        AssessmentQuestion q3 = new AssessmentQuestion("How many degrees are in the interior angles of a triangle, combined?", i++);
        q3.addOption(new AssessmentOption("360°", false, 1));
        q3.addOption(new AssessmentOption("180°", true, 2));
        q3.addOption(new AssessmentOption("90°", false, 3));
        q3.addOption(new AssessmentOption("270°", false, 4));
        assessment.addQuestion(q3);

        AssessmentQuestion q4 = new AssessmentQuestion("What is the area of a triangle with base 10 and height 6?", i);
        q4.addOption(new AssessmentOption("60", false, 1));
        q4.addOption(new AssessmentOption("16", false, 2));
        q4.addOption(new AssessmentOption("30", true, 3));
        q4.addOption(new AssessmentOption("40", false, 4));
        assessment.addQuestion(q4);

        return assessment;
    }
}
