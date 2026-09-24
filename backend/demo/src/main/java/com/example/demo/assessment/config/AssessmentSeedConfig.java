package com.example.demo.assessment.config;

import com.example.demo.assessment.entity.Assessment;
import com.example.demo.assessment.entity.AssessmentOption;
import com.example.demo.assessment.entity.AssessmentQuestion;
import com.example.demo.assessment.repository.AssessmentRepository;
import com.example.demo.assessment.repository.LearnerAssessmentAttemptRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class AssessmentSeedConfig {

    /** Present only in the current seed; used to detect the earlier, smaller seed and replace it once. */
    private static final String SEED_MARKER_TITLE = "Propositional Logic Basics";

    @Bean
    CommandLineRunner seedAssessmentData(
            AssessmentRepository assessmentRepository,
            LearnerAssessmentAttemptRepository attemptRepository
    ) {
        return args -> {
            boolean alreadyCurrent = assessmentRepository.findAll().stream()
                    .anyMatch(a -> SEED_MARKER_TITLE.equals(a.getTitle()));
            if (alreadyCurrent) {
                return;
            }
            attemptRepository.deleteAll();
            assessmentRepository.deleteAll();
            assessmentRepository.saveAll(List.of(
                    propositionalLogicBasics(),
                    conditionalsAndEquivalence(),
                    setNotationAndOperations(),
                    powerSetsAndVennCounting(),
                    vectorFundamentals(),
                    matrixBasics(),
                    determinantsInversesAndSystems(),
                    distributionsAndExpectedValue(),
                    binomialAndNormal(),
                    algebraFundamentals(),
                    geometryEssentials()
            ));
        };
    }

    // ---------------------------------------------------------------- helpers

    private record Q(String text, String[] options) {
    }

    /** The FIRST option is the correct one; options are rotated deterministically so answers vary in position. */
    private static Q q(String text, String... optionsCorrectFirst) {
        return new Q(text, optionsCorrectFirst);
    }

    private Assessment build(String title, String description, String tag, int sortOrder, Q... questions) {
        Assessment assessment = new Assessment(title, description, tag, sortOrder);
        int number = 1;
        for (Q question : questions) {
            AssessmentQuestion aq = new AssessmentQuestion(question.text(), number);
            int size = question.options().length;
            int shift = (number * 3 + sortOrder) % size;
            for (int i = 0; i < size; i++) {
                int source = (i - shift + size) % size;
                aq.addOption(new AssessmentOption(question.options()[source], source == 0, i + 1));
            }
            assessment.addQuestion(aq);
            number++;
        }
        return assessment;
    }

    // ------------------------------------------------------------------ Logic

    private Assessment propositionalLogicBasics() {
        return build("Propositional Logic Basics",
                "Propositions, truth values, and the NOT, AND, and OR connectives.", "LOGIC", 1,
                q("Which of these is a proposition?",
                        "7 is greater than 10.", "Close the door.", "Is it raining?", "x + 2 = 9"),
                q("If P is true and Q is false, what is the value of P ∧ Q?",
                        "False", "True", "It cannot be determined", "Both true and false"),
                q("When is P ∨ Q false?",
                        "Only when both P and Q are false", "Whenever P is false", "Whenever Q is false", "Whenever P and Q differ"),
                q("How many rows does a truth table for three variables P, Q, and R have?",
                        "8", "4", "6", "9"),
                q("¬(¬P) is equivalent to:",
                        "P", "¬P", "True", "False"),
                q("Which statement is the negation of “the server is running”?",
                        "The server is not running.", "The server is running slowly.", "The server might be running.", "The server was running."),
                q("With P = F, Q = F, and R = T, what is the value of (P ∨ Q) ∧ R?",
                        "False", "True", "It depends on the order of evaluation", "Undefined"));
    }

    private Assessment conditionalsAndEquivalence() {
        return build("Conditionals and Equivalence",
                "Implication, contrapositives, De Morgan's laws, and simplifying conditions.", "LOGIC", 2,
                q("For which values of P and Q is P → Q false?",
                        "P is true and Q is false", "P is false and Q is true", "P is false and Q is false", "P is true and Q is true"),
                q("P → Q is logically equivalent to:",
                        "¬P ∨ Q", "P ∨ ¬Q", "¬P ∧ Q", "Q → P"),
                q("By De Morgan's law, ¬(P ∨ Q) is equivalent to:",
                        "¬P ∧ ¬Q", "¬P ∨ ¬Q", "P ∧ Q", "¬P ∧ Q"),
                q("What is the contrapositive of “If it rains, the match is cancelled”?",
                        "If the match is not cancelled, it does not rain.", "If the match is cancelled, it rains.",
                        "If it does not rain, the match is not cancelled.", "If it rains, the match is not cancelled."),
                q("Requirement: if an order is paid, it must be shipped. Which case violates it?",
                        "The order is paid and not shipped.", "The order is paid and shipped.",
                        "The order is not paid and shipped.", "The order is not paid and not shipped."),
                q("(P ∧ Q) ∨ (P ∧ ¬Q) simplifies to:",
                        "P", "Q", "P ∧ Q", "True"),
                q("Which pair of statements is NOT logically equivalent?",
                        "P → Q and Q → P", "P → Q and ¬Q → ¬P", "¬(P ∧ Q) and ¬P ∨ ¬Q", "¬¬P and P"));
    }

    // -------------------------------------------------------------------- Sets

    private Assessment setNotationAndOperations() {
        return build("Set Notation and Operations",
                "Elements, subsets, union, intersection, difference, and complement.", "SETS", 3,
                q("If A = {1, 2, 3}, which statement is true?",
                        "2 ∈ A", "{2} ∈ A", "2 ⊆ A", "4 ∈ A"),
                q("Let A = {1, 2, 3, 4} and B = {3, 4, 5}. What is A ∩ B?",
                        "{3, 4}", "{1, 2, 3, 4, 5}", "{1, 2}", "{5}"),
                q("For the same A and B, what is A ∪ B?",
                        "{1, 2, 3, 4, 5}", "{3, 4}", "{1, 2}", "{1, 2, 5}"),
                q("For the same A and B, what is A − B?",
                        "{1, 2}", "{5}", "{3, 4}", "{1, 2, 5}"),
                q("Which statement is true for every set A?",
                        "∅ ⊆ A", "∅ ∈ A", "A ⊂ A", "A = ∅"),
                q("If U = {1, 2, 3, 4, 5, 6} and A = {2, 4, 6}, what is A′ (the complement of A)?",
                        "{1, 3, 5}", "{2, 4, 6}", "{1, 2, 3}", "{ }"),
                q("Which notation describes the even numbers less than 10?",
                        "{x | x is even and x < 10}", "{x | x is odd and x < 10}", "{x | x is even and x > 10}", "{x | x < 10}"));
    }

    private Assessment powerSetsAndVennCounting() {
        return build("Power Sets and Venn Counting",
                "Counting subsets and using Venn diagrams with inclusion-exclusion.", "SETS", 4,
                q("How many subsets does a set with 5 elements have?",
                        "32", "10", "25", "64"),
                q("If A = {a, b}, what is the power set P(A)?",
                        "{∅, {a}, {b}, {a, b}}", "{{a}, {b}}", "{a, b}", "{∅, a, b}"),
                q("How many non-empty subsets does a 4-element set have?",
                        "15", "16", "8", "4"),
                q("If n(A) = 14, n(B) = 12, and n(A ∩ B) = 5, what is n(A ∪ B)?",
                        "21", "26", "31", "17"),
                q("With the same values, how many members are in A only?",
                        "9", "14", "5", "7"),
                q("If n(U) = 30 and n(A ∪ B) = 21, how many members are outside both sets?",
                        "9", "21", "51", "12"),
                q("In n(A ∪ B ∪ C) = n(A) + n(B) + n(C) − n(A ∩ B) − n(A ∩ C) − n(B ∩ C) + ?, what completes the formula?",
                        "n(A ∩ B ∩ C)", "n(A ∪ B ∪ C)", "n(U)", "0"),
                q("Let n(A) = 24, n(B) = 22, n(C) = 18, n(A ∩ B) = 8, n(A ∩ C) = 6, n(B ∩ C) = 5, and n(A ∩ B ∩ C) = 2. What is n(A ∪ B ∪ C)?",
                        "47", "58", "44", "49"));
    }

    // ------------------------------------------------------------------ Vectors

    private Assessment vectorFundamentals() {
        return build("Vector Fundamentals",
                "Magnitude, unit vectors, dot product, cross product, and area.", "VECTOR", 5,
                q("Which quantity is a vector?",
                        "Velocity", "Speed", "Mass", "Temperature"),
                q("What is the magnitude of the vector ⟨3, 4⟩?",
                        "5", "7", "12", "25"),
                q("What is the unit vector in the direction of ⟨3, 4⟩?",
                        "⟨0.6, 0.8⟩", "⟨3, 4⟩", "⟨1, 1⟩", "⟨0.3, 0.4⟩"),
                q("For a = ⟨2, 3⟩ and b = ⟨4, −1⟩, what is a · b?",
                        "5", "11", "8", "−3"),
                q("If the dot product of two nonzero vectors is zero, the vectors are:",
                        "perpendicular", "parallel", "equal", "opposite"),
                q("If |a| = 4, |b| = 6, and the angle between them is 90°, what is |a × b|?",
                        "24", "10", "0", "12"),
                q("The area of the triangle formed by vectors a and b is:",
                        "½ |a × b|", "|a × b|", "|a · b|", "2 |a × b|"),
                q("What is ⟨1, 2⟩ + ⟨3, −5⟩?",
                        "⟨4, −3⟩", "⟨2, 7⟩", "⟨3, −10⟩", "⟨4, 3⟩"));
    }

    // ----------------------------------------------------------------- Matrices

    private Assessment matrixBasics() {
        return build("Matrix Basics",
                "Matrix size, entries, addition, scalar multiplication, and multiplication rules.", "MATRIX", 6,
                q("What is the size of a matrix with 2 rows and 3 columns?",
                        "2 × 3", "3 × 2", "2 × 2", "6"),
                q("For A = [[1, 2], [3, 4]], what is the entry a₂₁ (row 2, column 1)?",
                        "3", "2", "1", "4"),
                q("If A is a 2 × 3 matrix, with which matrix B is the product AB defined?",
                        "B is 3 × 4", "B is 2 × 3", "B is 4 × 3", "B is 4 × 2"),
                q("A 2 × 3 matrix multiplied by a 3 × 4 matrix gives a matrix of size:",
                        "2 × 4", "3 × 3", "2 × 3", "4 × 2"),
                q("What is [[1, 2], [3, 4]] + [[5, 0], [1, 7]]?",
                        "[[6, 2], [4, 11]]", "[[6, 2], [4, 10]]", "[[5, 0], [3, 28]]", "[[6, 2], [3, 11]]"),
                q("What is 3 × [[1, 2], [3, 4]]?",
                        "[[3, 6], [9, 12]]", "[[4, 5], [6, 7]]", "[[3, 2], [3, 4]]", "[[1, 6], [3, 12]]"),
                q("For square matrices A and B, which statement is true in general?",
                        "AB and BA may be different.", "AB always equals BA.", "AB is never defined.", "AB is always the zero matrix."),
                q("The identity matrix I satisfies:",
                        "AI = IA = A", "AI = 0", "AI = I", "AI = −A"));
    }

    private Assessment determinantsInversesAndSystems() {
        return build("Determinants, Inverses, and Systems",
                "2 × 2 determinants, inverses, matrix equations, and augmented matrices.", "MATRIX", 7,
                q("What is the determinant of [[3, 1], [2, 4]]?",
                        "10", "14", "12", "2"),
                q("A square matrix has an inverse exactly when its determinant is:",
                        "not zero", "zero", "positive", "negative"),
                q("The matrix [[2, 4], [1, 2]] has determinant 0. This means the matrix is:",
                        "singular (it has no inverse)", "invertible", "the identity matrix", "not square"),
                q("For A = [[3, 1], [2, 4]], A⁻¹ = (1/10)[[4, −1], [−2, 3]]. What is the entry in row 1, column 1 of A⁻¹?",
                        "0.4", "0.3", "4", "−0.1"),
                q("To solve the matrix equation AX = B for X (when A is invertible), use:",
                        "X = A⁻¹B", "X = BA⁻¹", "X = B / A", "X = A + B"),
                q("Which augmented matrix represents x + y = 3 and x − y = 1?",
                        "[1 1 | 3] and [1 −1 | 1]", "[1 1 | 1] and [1 −1 | 3]", "[3 1 | 1] and [1 1 | 3]", "[1 3 | 1] and [−1 1 | 1]"),
                q("What is the solution of x + y = 3 and x − y = 1?",
                        "x = 2, y = 1", "x = 1, y = 2", "x = 3, y = 0", "x = 0, y = 3"),
                q("A system made of two parallel lines has:",
                        "no solution", "one solution", "infinitely many solutions", "exactly two solutions"));
    }

    // -------------------------------------------------------------- Probability

    private Assessment distributionsAndExpectedValue() {
        return build("Distributions and Expected Value",
                "Valid distributions, expected value, standard deviation, and uniform distributions.", "PROBABILITY", 8,
                q("Which list can be a valid probability distribution?",
                        "0.2, 0.3, 0.5", "0.5, 0.7, 0.2", "−0.1, 0.6, 0.5", "0.1, 0.2, 0.4"),
                q("What is the expected value of one roll of a fair six-sided die?",
                        "3.5", "3", "4", "21"),
                q("A 20-baht ticket wins 100 baht with probability 0.1. What is the expected net result per play?",
                        "−10 baht", "+10 baht", "−20 baht", "0 baht"),
                q("A game is called fair when its expected value is:",
                        "0", "1", "less than 0", "greater than 1"),
                q("The standard deviation is:",
                        "the square root of the variance", "the variance squared", "the mean divided by n", "the largest value minus the smallest"),
                q("In a uniform distribution with 4 equally likely outcomes, each outcome has probability:",
                        "1/4", "1/2", "4", "1/16"),
                q("The probabilities in a discrete distribution always add up to:",
                        "1", "0", "100", "a value that depends on the distribution"));
    }

    private Assessment binomialAndNormal() {
        return build("Binomial and Normal Distributions",
                "Binomial probabilities, the 68–95–99.7 rule, and z-scores.", "PROBABILITY", 9,
                q("Which condition is NOT required for a binomial distribution?",
                        "Each trial depends on the earlier results.", "There is a fixed number of trials.",
                        "Each trial has two outcomes.", "The success probability is constant."),
                q("What is the probability of exactly 3 heads in 5 flips of a fair coin?",
                        "0.3125", "0.5", "0.6", "0.125"),
                q("What is the mean of a binomial distribution with n = 20 and p = 0.3?",
                        "6", "14", "0.3", "20"),
                q("In a normal distribution, about what percentage of values lie within 1 standard deviation of the mean?",
                        "68%", "50%", "95%", "99.7%"),
                q("About what percentage lie within 2 standard deviations of the mean?",
                        "95%", "68%", "99.7%", "75%"),
                q("If μ = 70 and σ = 10, what is the z-score of a mark of 90?",
                        "2", "20", "−2", "0.2"),
                q("The standard normal distribution has:",
                        "mean 0 and standard deviation 1", "mean 1 and standard deviation 0", "mean 0 and variance 0", "mean 1 and standard deviation 1"),
                q("For a continuous random variable, the probability of an interval equals:",
                        "the area under the curve over that interval", "the height of the curve", "the slope of the curve", "the mean of the curve"));
    }

    // ------------------------------------------------------- Earlier assessments

    private Assessment algebraFundamentals() {
        Assessment assessment = new Assessment(
                "Algebra Fundamentals Quiz", "Test your grasp of core algebra concepts.", "ALGEBRA", 10);
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
                "Geometry Essentials Quiz", "Check your understanding of shapes, area and the Pythagorean theorem.", "GEOMETRY", 11);
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
