package com.example.demo.config;

import com.example.demo.entity.User;
import com.example.demo.entity.UserRole;
import com.example.demo.entity.UserStatus;
import com.example.demo.entity.course.ContentDepth;
import com.example.demo.entity.course.Course;
import com.example.demo.entity.course.CourseModule;
import com.example.demo.entity.course.InteractionType;
import com.example.demo.entity.course.SubTopic;
import com.example.demo.entity.course.SubTopicSourceType;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.course.CourseRepository;
import com.example.demo.service.course.InteractiveConfigService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DevSeedConfig {

    @Bean
    CommandLineRunner seedDevData(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CourseRepository courseRepository,
            InteractiveConfigService interactiveConfigService
    ) {
        return args -> {

            User admin;
            if (!userRepository.existsByEmail("admin@lifeos.local")) {
                admin = userRepository.save(new User(
                        "admin@lifeos.local",
                        "admin",
                        passwordEncoder.encode("Admin12345!"),
                        "Admin",
                        "User",
                        UserRole.ROLE_ADMIN,
                        UserStatus.VERIFY
                ));
            } else {
                admin = userRepository.findByEmail("admin@lifeos.local").orElseThrow();
            }

            if (!userRepository.existsByEmail("teacher@lifeos.local")) {
                userRepository.save(new User(
                        "teacher@lifeos.local",
                        "teacher",
                        passwordEncoder.encode("Teacher12345!"),
                        "Teacher",
                        "User",
                        UserRole.ROLE_TEACHER,
                        UserStatus.VERIFY
                ));
            }

            if (!userRepository.existsByEmail("learner@lifeos.local")) {
                userRepository.save(new User(
                        "learner@lifeos.local",
                        "learner",
                        passwordEncoder.encode("Learner12345!"),
                        "Learner",
                        "User",
                        UserRole.ROLE_LEARNER,
                        UserStatus.VERIFY
                ));
            }

            seedInteractiveReferenceCourse(courseRepository, interactiveConfigService, admin);
        };
    }

    private void seedInteractiveReferenceCourse(
            CourseRepository courseRepository,
            InteractiveConfigService interactiveConfigService,
            User admin
    ) {
        removeLegacyAggregateSeedCourse(courseRepository);

        Course matrixCourse = seedCourse(
                courseRepository,
                "Matrix",
                "Seed course from course_source_for_seed_reference/Matrix.pdf covering matrices, multiplication, determinants, inverses, matrix equations, augmented matrices, and linear systems.",
                admin
        );
        matrixCourse.updateCover("matrix_violet");
        CourseModule matrixModule = new CourseModule(
                "Matrix",
                "Based on Matrix.pdf: matrices, multiplication, determinants, inverses, matrix equations, and linear systems.",
                0,
                ContentDepth.HIGH
        );
        matrixModule.addSubTopic(subTopic(
                "Matrices",
                """
                        <h2>Matrices</h2>
                        <p>A matrix is a rectangular arrangement of numbers. Each number is called an entry, horizontal lines are rows, and vertical lines are columns.</p>
                        <p>The size of a matrix is written as rows by columns, such as <code>3 x 2</code>. An entry is often written as <code>a_ij</code>, where <code>i</code> is the row and <code>j</code> is the column.</p>
                        <p>Two matrices are equal when they have the same size and every matching entry is equal. Addition, subtraction, and scalar multiplication are performed entry by entry.</p>
                        """,
                0,
                1,
                4,
                InteractionType.NONE,
                null,
                null
        ));
        matrixModule.addSubTopic(subTopic(
                "Matrix Multiplication",
                """
                        <h2>Matrix Multiplication</h2>
                        <p>Matrix multiplication is defined when the number of columns in the first matrix equals the number of rows in the second matrix. Each output entry is found by pairing a row from the first matrix with a column from the second matrix and adding the products.</p>
                        <p>If <code>A</code> has size <code>m x n</code> and <code>B</code> has size <code>n x p</code>, then <code>AB</code> has size <code>m x p</code>.</p>
                        """,
                1,
                5,
                9,
                InteractionType.NONE,
                null,
                null
        ));
        matrixModule.addSubTopic(subTopic(
                "Determinants",
                """
                        <h2>Determinants</h2>
                        <p>A determinant is a value computed from a square matrix. It is used to test important properties, such as whether a matrix has an inverse, and to solve some systems of equations.</p>
                        <p>For a <code>2 x 2</code> matrix <code>[[a,b],[c,d]]</code>, the determinant is <code>ad - bc</code>.</p>
                        """,
                2,
                10,
                15,
                InteractionType.FORMULA_EXPLORER,
                "Let learners adjust the entries of a 2x2 matrix until the determinant equals 5.",
                validate(interactiveConfigService, InteractionType.FORMULA_EXPLORER, """
                        {
                          "type": "FORMULA_EXPLORER",
                          "mode": "PRACTICE",
                          "title": "2x2 determinant target",
                          "prompt": "Adjust a, b, c, and d until ad - bc equals 5.",
                          "formula": "a * d - b * c",
                          "variables": [
                            { "name": "a", "label": "a", "min": -5, "max": 5, "step": 1, "initial": 1 },
                            { "name": "b", "label": "b", "min": -5, "max": 5, "step": 1, "initial": 0 },
                            { "name": "c", "label": "c", "min": -5, "max": 5, "step": 1, "initial": 0 },
                            { "name": "d", "label": "d", "min": -5, "max": 5, "step": 1, "initial": 1 }
                          ],
                          "precision": 2,
                          "successCondition": { "kind": "EXPRESSION_EQUALS", "target": 5, "tolerance": 0.01 },
                          "feedback": {
                            "success": "Correct. The determinant is 5.",
                            "failure": "Not yet. Recalculate ad - bc and adjust one entry."
                          }
                        }
                        """)
        ));
        matrixModule.addSubTopic(subTopic(
                "Multiplicative Inverses",
                """
                        <h2>Multiplicative Inverses</h2>
                        <p>The multiplicative inverse of a matrix <code>A</code> is a matrix <code>A^-1</code> such that <code>AA^-1 = I</code> and <code>A^-1A = I</code>.</p>
                        <p>A square matrix has an inverse when its determinant is nonzero. This idea connects directly to matrix equations and systems of linear equations.</p>
                        """,
                3,
                16,
                21,
                InteractionType.NONE,
                null,
                null
        ));
        matrixModule.addSubTopic(subTopic(
                "Matrix Equations",
                """
                        <h2>Matrix Equations</h2>
                        <p>Matrix equations use the rules of matrix addition, subtraction, multiplication, and inverses to rearrange and solve equations. The order of multiplication matters because, in general, <code>AB</code> does not have to equal <code>BA</code>.</p>
                        """,
                4,
                22,
                25,
                InteractionType.NONE,
                null,
                null
        ));
        matrixModule.addSubTopic(subTopic(
                "Augmented Matrices and Linear Systems",
                """
                        <h2>Augmented Matrices and Linear Systems</h2>
                        <p>An augmented matrix places the coefficients and constants of a linear system into one matrix so row operations can be used systematically.</p>
                        <p>Row operations simplify the system and make it easier to see whether it has one solution, no solution, or infinitely many solutions.</p>
                        """,
                5,
                26,
                35,
                InteractionType.GRAPH_2D,
                "Show a line graph that connects linear systems to intersections before the learner tries matching a target point.",
                validate(interactiveConfigService, InteractionType.GRAPH_2D, """
                        {
                          "type": "GRAPH_2D",
                          "mode": "VISUALIZATION",
                          "title": "Linear equation reference",
                          "expression": "2 * x + 1",
                          "xMin": -5,
                          "xMax": 5,
                          "yMin": -10,
                          "yMax": 12,
                          "sampleCount": 120
                        }
                        """)
        ));
        matrixModule.addSubTopic(subTopic(
                "Graph Point Match Practice",
                """
                        <h2>Graph Point Match Practice</h2>
                        <p>After inspecting the reference line, adjust the slope of <code>y = mx + 1</code> so the graph passes through the target point.</p>
                        <p>Use substitution to reason about the target: when <code>x = 2</code>, the graph should produce <code>y = 5</code>.</p>
                        """,
                6,
                26,
                35,
                InteractionType.GRAPH_2D,
                "Challenge learners to adjust a line so it passes through the target point.",
                validate(interactiveConfigService, InteractionType.GRAPH_2D, """
                        {
                          "type": "GRAPH_2D",
                          "mode": "PRACTICE",
                          "title": "Graph point match",
                          "prompt": "Adjust the slope m until the line passes through the target point (2, 5).",
                          "expression": "m * x + 1",
                          "controls": {
                            "m": { "min": -3, "max": 5, "step": 0.5, "initial": 1 }
                          },
                          "xMin": -5,
                          "xMax": 5,
                          "yMin": -10,
                          "yMax": 12,
                          "sampleCount": 120,
                          "successCondition": { "kind": "POINT_ON_GRAPH", "target": { "x": 2, "y": 5 }, "tolerance": 0.1 },
                          "feedback": {
                            "success": "Correct. With m = 2, the line reaches the point (2, 5).",
                            "failure": "Not yet. Substitute x = 2 and solve m * 2 + 1 = 5."
                          }
                        }
                        """)
        ));
        publishSeedCourse(courseRepository, matrixCourse, matrixModule);

        Course probabilityCourse = seedCourse(
                courseRepository,
                "Probability",
                "Seed course from course_source_for_seed_reference/PrbDst.pdf covering random variables, probability distributions, expected value, standard deviation, binomial distribution, and normal distribution.",
                admin
        );
        probabilityCourse.updateCover("prob_rose");
        CourseModule probabilityModule = new CourseModule(
                "Probability",
                "Based on PrbDst.pdf: random variables, probability distributions, expected value, standard deviation, and major distributions.",
                0,
                ContentDepth.HIGH
        );
        probabilityModule.addSubTopic(subTopic(
                "Random Variables",
                """
                        <h2>Random Variables</h2>
                        <p>A random variable represents a quantity of interest from a random experiment, such as the sum of dice rolls, the number of heads in coin flips, or the height of a selected person.</p>
                        <p>Random variables can be discrete, with listable possible values, or continuous, with real-number values over an interval.</p>
                        """,
                0,
                1,
                1,
                InteractionType.QUIZ,
                "Classify the random variable.",
                validate(interactiveConfigService, InteractionType.QUIZ, """
                        {
                          "type": "QUIZ",
                          "mode": "PRACTICE",
                          "title": "Classify variables",
                          "prompt": "Classify the random variable.",
                          "question": "The number of emails you receive tomorrow.",
                          "options": [
                            { "id": "discrete", "label": "Discrete", "correct": true },
                            { "id": "continuous", "label": "Continuous", "correct": false }
                          ],
                          "explanation": "The number of emails is countable (0, 1, 2, ...), so it is a discrete random variable.",
                          "successCondition": { "kind": "QUIZ_CORRECT_OPTION" },
                          "feedback": {
                            "success": "Correct. Since you can count the number of emails, it is discrete.",
                            "failure": "Not quite. Can you receive 1.5 emails, or is it countable?"
                          }
                        }
                        """)
        ));
        probabilityModule.addSubTopic(subTopic(
                "Discrete Probability Distributions",
                """
                        <h2>Discrete Probability Distributions</h2>
                        <p>A probability distribution assigns probabilities to every possible value of a random variable and can be shown in a table or graph.</p>
                        <p>For a discrete random variable, all probabilities must be non-negative and their total must equal 1.</p>
                        """,
                1,
                2,
                3,
                InteractionType.QUIZ,
                "Ask learners to choose the list that forms a valid probability distribution.",
                validate(interactiveConfigService, InteractionType.QUIZ, """
                        {
                          "type": "QUIZ",
                          "mode": "PRACTICE",
                          "title": "Distribution check",
                          "prompt": "Choose the list that can be a valid probability distribution.",
                          "question": "Which list can be a valid probability distribution?",
                          "options": [
                            { "id": "a", "label": "0.2, 0.3, 0.5", "correct": true },
                            { "id": "b", "label": "0.5, 0.7, 0.2", "correct": false },
                            { "id": "c", "label": "-0.1, 0.6, 0.5", "correct": false },
                            { "id": "d", "label": "0.1, 0.2, 0.4", "correct": false }
                          ],
                          "explanation": "The probabilities must be non-negative and add up to exactly 1.",
                          "successCondition": { "kind": "QUIZ_CORRECT_OPTION" },
                          "feedback": {
                            "success": "Correct. The values are non-negative and sum to 1.",
                            "failure": "Not yet. Check both non-negativity and the total probability."
                          }
                        }
                        """)
        ));
        probabilityModule.addSubTopic(subTopic(
                "Expected Value of a Discrete Random Variable",
                """
                        <h2>Expected Value</h2>
                        <p>Expected value is a weighted average of a random variable. It is computed by multiplying each value by its probability and adding the results.</p>
                        <p>This idea summarizes the long-run average behavior of a random experiment repeated many times.</p>
                        """,
                2,
                4,
                6,
                InteractionType.NONE,
                null,
                null
        ));
        probabilityModule.addSubTopic(subTopic(
                "Standard Deviation and Uniform Distributions",
                """
                        <h2>Standard Deviation and Uniform Distributions</h2>
                        <p>Standard deviation measures how spread out the values of a random variable are around the expected value. A larger value means the outcomes tend to be more widely spread.</p>
                        <p>A discrete uniform distribution is a case where every possible value has the same probability.</p>
                        """,
                3,
                7,
                8,
                InteractionType.NONE,
                null,
                null
        ));
        probabilityModule.addSubTopic(subTopic(
                "Binomial Distribution",
                """
                        <h2>Binomial Distribution</h2>
                        <p>The binomial distribution applies to repeated trials where each trial has two outcomes, such as success/failure, and the probability of success stays constant.</p>
                        <p>The random variable usually represents the number of successes out of the total number of trials.</p>
                        """,
                4,
                9,
                11,
                InteractionType.NONE,
                null,
                null
        ));
        probabilityModule.addSubTopic(subTopic(
                "Continuous and Normal Distributions",
                """
                        <h2>Continuous and Normal Distributions</h2>
                        <p>A continuous random variable is described by a probability density. The area under the curve over an interval represents probability.</p>
                        <p>The standard normal distribution and the normal distribution are important models for many data sets, with mean and standard deviation controlling the shape of the curve.</p>
                        """,
                5,
                12,
                26,
                InteractionType.GRAPH_2D,
                "Show a standard bell curve to help learners read area under a curve before trying a peak-matching challenge.",
                validate(interactiveConfigService, InteractionType.GRAPH_2D, """
                        {
                          "type": "GRAPH_2D",
                          "mode": "VISUALIZATION",
                          "title": "Standard normal curve",
                          "expression": "exp(-x^2 / 2)",
                          "xMin": -4,
                          "xMax": 4,
                          "yMin": 0,
                          "yMax": 1.2,
                          "sampleCount": 200
                        }
                        """)
        ));
        probabilityModule.addSubTopic(subTopic(
                "Normal Curve Peak Practice",
                """
                        <h2>Normal Curve Peak Practice</h2>
                        <p>The reference normal curve reaches its maximum at the center. In this challenge, adjust the scale of the curve so the peak reaches the target point.</p>
                        <p>At <code>x = 0</code>, the expression <code>exp(-x^2 / 2)</code> equals 1, so the scale directly controls the peak height.</p>
                        """,
                6,
                12,
                26,
                InteractionType.GRAPH_2D,
                "Challenge learners to adjust the normal curve height so the peak reaches the target.",
                validate(interactiveConfigService, InteractionType.GRAPH_2D, """
                        {
                          "type": "GRAPH_2D",
                          "mode": "PRACTICE",
                          "title": "Normal curve peak match",
                          "prompt": "Adjust the scale a until the curve passes through the target peak at (0, 1).",
                          "expression": "a * exp(-x^2 / 2)",
                          "controls": {
                            "a": { "min": 0.2, "max": 1.8, "step": 0.1, "initial": 0.5 }
                          },
                          "xMin": -4,
                          "xMax": 4,
                          "yMin": 0,
                          "yMax": 1.8,
                          "sampleCount": 200,
                          "successCondition": { "kind": "POINT_ON_GRAPH", "target": { "x": 0, "y": 1 }, "tolerance": 0.02 },
                          "feedback": {
                            "success": "Correct. The curve reaches height 1 at x = 0.",
                            "failure": "Not yet. At x = 0, exp(0) = 1, so the scale controls the peak directly."
                          }
                        }
                        """)
        ));
        publishSeedCourse(courseRepository, probabilityCourse, probabilityModule);

        Course setCourse = seedCourse(
                courseRepository,
                "Set",
                "Seed course from course_source_for_seed_reference/SetAdv.pdf covering set basics, power sets, subset counts, and Venn diagram counting.",
                admin
        );
        setCourse.updateCover("union_green");
        CourseModule setModule = new CourseModule(
                "Set",
                "Based on SetAdv.pdf: set basics, power sets, subset counts, and counting members in Venn regions.",
                0,
                ContentDepth.HIGH
        );
        setModule.addSubTopic(subTopic(
                "Basic Review",
                """
                        <h2>Basic Review</h2>
                        <p>A set is a collection of elements treated as one object. Elements are written inside braces, and order or repetition does not change the set.</p>
                        <p>Important ideas include the empty set, finite sets, subsets, universal sets, Venn diagrams, union, intersection, difference, and complement.</p>
                        """,
                0,
                1,
                3,
                InteractionType.VISUAL_LAYER,
                "Let learners click set regions to connect the set as one object with its elements.",
                validate(interactiveConfigService, InteractionType.VISUAL_LAYER, """
                        {
                          "type": "VISUAL_LAYER",
                          "mode": "VISUALIZATION",
                          "title": "Set as a collection",
                          "canvas": { "width": 900, "height": 520, "backgroundText": "Click the element button to highlight the set region." },
                          "zones": [
                            { "id": "zone_set", "label": "Set A", "shape": "circle", "x": 280, "y": 100, "width": 320, "height": 320, "color": "#8fe0aa", "highlightColor": "#3aa66b", "highlightOpacity": 0.82, "feedback": "Set A is treated as one collection." }
                          ],
                          "elements": [
                            { "id": "element_one", "label": "Element", "kind": "button", "x": 60, "y": 60, "width": 150, "height": 48 }
                          ],
                          "interactions": [
                            { "triggerId": "element_one", "effect": "HIGHLIGHT_ZONE", "targetZoneId": "zone_set", "feedback": "The element belongs to Set A." }
                          ]
                        }
                        """)
        ));
        setModule.addSubTopic(subTopic(
                "2-Set Venn Practice",
                """
                        <h2>2-Set Venn Practice</h2>
                        <p>After reviewing sets as collections, build a two-set Venn diagram for sets A and B.</p>
                        <p><strong>Given:</strong> n(A)=11, n(B)=9, and n(A intersect B)=3. Add both circles, create the overlap, then fill the exact region values.</p>
                        """,
                1,
                1,
                3,
                InteractionType.VISUAL_LAYER,
                "Ask learners to build a two-set Venn diagram and fill each exact region.",
                validate(interactiveConfigService, InteractionType.VISUAL_LAYER, """
                        {
                          "type": "VISUAL_LAYER",
                          "mode": "PRACTICE",
                          "title": "2-set Venn practice",
                          "prompt": "Add circles A and B, arrange the overlap, then enter the exact value for each visible region.",
                          "canvas": { "width": 900, "height": 520, "backgroundText": "" },
                          "zones": [
                            { "id": "zone_a", "label": "A", "shape": "circle", "x": 250, "y": 140, "width": 280, "height": 280, "labelX": 34, "labelY": 28, "color": "#ffd333", "highlightColor": "#ff8f1f", "highlightOpacity": 0.82, "feedback": "" },
                            { "id": "zone_b", "label": "B", "shape": "circle", "x": 390, "y": 140, "width": 280, "height": 280, "labelX": 66, "labelY": 28, "color": "#8fb3ff", "highlightColor": "#4f8cff", "highlightOpacity": 0.82, "feedback": "" }
                          ],
                          "elements": [],
                          "interactions": [],
                          "overlap": {
                            "enabled": true,
                            "sourceZoneIds": ["zone_a", "zone_b"],
                            "inputs": [
                              { "id": "A_ONLY", "label": "n(A)", "zoneIds": ["zone_a"], "value": 11, "kind": "total" },
                              { "id": "B_ONLY", "label": "n(B)", "zoneIds": ["zone_b"], "value": 9, "kind": "total" },
                              { "id": "A_AND_B", "label": "n(A intersect B)", "zoneIds": ["zone_a", "zone_b"], "value": 3, "kind": "intersection" }
                            ],
                            "values": []
                          },
                          "feedback": {
                            "success": "Correct. A only is 8, B only is 6, and the overlap is 3.",
                            "failure": "Not yet. Subtract the overlap from each set total to get the exact outside regions."
                          }
                        }
                        """)
        ));
        setModule.addSubTopic(subTopic(
                "Power Sets",
                """
                        <h2>Power Sets</h2>
                        <p>The power set of a set <code>A</code>, written <code>P(A)</code>, is the set of all subsets of <code>A</code>.</p>
                        <p>For example, if <code>A = {1, 2}</code>, then <code>P(A) = { empty, {1}, {2}, {1,2} }</code>. A power set always contains the empty set and the original set.</p>
                        """,
                2,
                4,
                8,
                InteractionType.QUIZ,
                "Ask learners to check the relationship between elements, subsets, and power sets.",
                validate(interactiveConfigService, InteractionType.QUIZ, """
                        {
                          "type": "QUIZ",
                          "mode": "PRACTICE",
                          "title": "Power set check",
                          "prompt": "Choose the true statement for A = {1, 2}.",
                          "question": "Which statement is true when A = {1, 2}?",
                          "options": [
                            { "id": "a", "label": "{1} is an element of P(A)", "correct": true },
                            { "id": "b", "label": "3 is an element of P(A)", "correct": false },
                            { "id": "c", "label": "P(A) has 2 elements", "correct": false },
                            { "id": "d", "label": "A is not an element of P(A)", "correct": false }
                          ],
                          "explanation": "P(A) contains every subset of A, including {1}, {2}, empty set, and A itself.",
                          "successCondition": { "kind": "QUIZ_CORRECT_OPTION" },
                          "feedback": {
                            "success": "Correct. A power set contains subsets as its elements.",
                            "failure": "Not yet. List every subset of A first."
                          }
                        }
                        """)
        ));
        setModule.addSubTopic(subTopic(
                "Number of Subsets",
                """
                        <h2>Number of Subsets</h2>
                        <p>If a set has <code>n</code> elements, the total number of subsets is <code>2^n</code>. Each element has two choices: it is either included in a subset or not included.</p>
                        """,
                3,
                9,
                13,
                InteractionType.FORMULA_EXPLORER,
                "Let learners adjust the number of set elements and see the total number of subsets.",
                validate(interactiveConfigService, InteractionType.FORMULA_EXPLORER, """
                        {
                          "type": "FORMULA_EXPLORER",
                          "mode": "VISUALIZATION",
                          "title": "Subset count",
                          "formula": "2^n",
                          "variables": [
                            { "name": "n", "label": "Members", "min": 0, "max": 10, "step": 1, "initial": 3 }
                          ],
                          "precision": 0
                        }
                        """)
        ));
        setModule.addSubTopic(subTopic(
                "Counting Members in Regions",
                """
                        <h2>Counting Members in Regions</h2>
                        <p>Counting members in a Venn diagram requires separating overlapping regions clearly, especially when multiple sets overlap.</p>
                        <p>The key rule is inclusion-exclusion, such as <code>n(A union B) = n(A) + n(B) - n(A intersect B)</code>.</p>
                        """,
                4,
                14,
                22,
                InteractionType.FORMULA_EXPLORER,
                "Let learners choose a counting formula and inspect the result and steps from sample values.",
                validate(interactiveConfigService, InteractionType.FORMULA_EXPLORER, """
                        {
                          "type": "FORMULA_EXPLORER",
                          "mode": "VISUALIZATION",
                          "title": "Venn ABC formulas",
                          "formula": "a + b + c - ab - ac - bc + abc",
                          "variables": [
                            { "name": "u", "label": "n(U)", "min": 0, "max": 100, "step": 1, "initial": 60 },
                            { "name": "a", "label": "n(A)", "min": 0, "max": 80, "step": 1, "initial": 24 },
                            { "name": "b", "label": "n(B)", "min": 0, "max": 80, "step": 1, "initial": 22 },
                            { "name": "c", "label": "n(C)", "min": 0, "max": 80, "step": 1, "initial": 18 },
                            { "name": "ab", "label": "n(A intersect B)", "min": 0, "max": 50, "step": 1, "initial": 8 },
                            { "name": "ac", "label": "n(A intersect C)", "min": 0, "max": 50, "step": 1, "initial": 6 },
                            { "name": "bc", "label": "n(B intersect C)", "min": 0, "max": 50, "step": 1, "initial": 5 },
                            { "name": "abc", "label": "n(A intersect B intersect C)", "min": 0, "max": 30, "step": 1, "initial": 2 }
                          ],
                          "precision": 0,
                          "formulaOptions": [
                            {
                              "id": "set-a",
                              "label": "A",
                              "formula": "a",
                              "description": "Shade all members in set A, including regions that overlap with B or C.",
                              "steps": [
                                { "label": "Choose circle A", "expression": "a", "explanation": "n(A) includes every region inside circle A: A only, A intersect B, A intersect C, and A intersect B intersect C." }
                              ]
                            },
                            {
                              "id": "set-b",
                              "label": "B",
                              "formula": "b",
                              "description": "Shade all members in set B, including regions that overlap with A or C.",
                              "steps": [
                                { "label": "Choose circle B", "expression": "b", "explanation": "n(B) includes every region inside circle B: B only, A intersect B, B intersect C, and A intersect B intersect C." }
                              ]
                            },
                            {
                              "id": "set-c",
                              "label": "C",
                              "formula": "c",
                              "description": "Shade all members in set C.",
                              "steps": [
                                { "label": "Choose circle C", "expression": "c", "explanation": "n(C) includes every region inside circle C, including overlaps with A or B." }
                              ]
                            },
                            {
                              "id": "a-only",
                              "label": "A only",
                              "formula": "a - ab - ac + abc",
                              "description": "Shade only the region in A but not in B or C.",
                              "steps": [
                                { "label": "Start with all of A", "expression": "a", "explanation": "Circle A contains both A only and the regions overlapping with B or C." },
                                { "label": "Subtract overlaps with B and C", "expression": "a - ab - ac", "explanation": "Subtract A intersect B and A intersect C, but the middle three-set region has now been subtracted twice." },
                                { "label": "Add the three-set middle back", "expression": "a - ab - ac + abc", "explanation": "Add A intersect B intersect C back once, leaving A only." }
                              ]
                            },
                            {
                              "id": "a-union-b",
                              "label": "A union B",
                              "formula": "a + b - ab",
                              "description": "Shade every region that is in A or B.",
                              "steps": [
                                { "label": "Add A and B", "expression": "a + b", "explanation": "This counts members in A and B together, but A intersect B is counted twice." },
                                { "label": "Subtract A intersect B", "expression": "a + b - ab", "explanation": "Subtract the overlap once to get A union B." }
                              ]
                            },
                            {
                              "id": "a-intersect-b",
                              "label": "A intersect B",
                              "formula": "ab",
                              "description": "Shade members that are in both A and B, including the part that may also be in C.",
                              "steps": [
                                { "label": "Look at the overlap of A and B", "expression": "ab", "explanation": "n(A intersect B) counts every member that is in A and B at the same time." }
                              ]
                            },
                            {
                              "id": "a-union-b-union-c",
                              "label": "A union B union C",
                              "formula": "a + b + c - ab - ac - bc + abc",
                              "description": "Shade every region that is in A, B, or C.",
                              "steps": [
                                { "label": "Add all three sets", "expression": "a + b + c", "explanation": "Start by adding members in A, B, and C, but overlapping regions are overcounted." },
                                { "label": "Subtract pairwise overlaps", "expression": "a + b + c - ab - ac - bc", "explanation": "Subtract A intersect B, A intersect C, and B intersect C to correct double counting." },
                                { "label": "Add the three-set overlap back", "expression": "a + b + c - ab - ac - bc + abc", "explanation": "The middle three-set region was subtracted one time too many, so add it back." }
                              ]
                            },
                            {
                              "id": "a-intersect-b-intersect-c",
                              "label": "A intersect B intersect C",
                              "formula": "abc",
                              "description": "Shade only the middle region that is in A, B, and C at the same time.",
                              "steps": [
                                { "label": "Look at the three-set middle", "expression": "abc", "explanation": "n(A intersect B intersect C) counts members that belong to all three sets." }
                              ]
                            }
                          ]
                        }
                        """)
        ));
        setModule.addSubTopic(subTopic(
                "Three-Set Venn Diagram",
                """
                        <h2>Three-Set Venn Diagram</h2>
                        <p>A Venn diagram separates the regions of sets A, B, and C visually before member counts are computed with inclusion-exclusion.</p>
                        <p>Use the buttons to highlight circles or important regions, then connect the visual regions to the formulas from the previous topic.</p>
                        """,
                5,
                14,
                22,
                InteractionType.VISUAL_LAYER,
                "Let learners press buttons to highlight zones in a three-set Venn diagram and read feedback.",
                validate(interactiveConfigService, InteractionType.VISUAL_LAYER, """
                        {
                          "type": "VISUAL_LAYER",
                          "mode": "VISUALIZATION",
                          "title": "Venn ABC visual layer",
                          "canvas": {
                            "width": 900,
                            "height": 520,
                            "backgroundText": "A, B, and C overlap to show unions and intersections"
                          },
                          "zones": [
                            { "id": "zone_a", "label": "A", "shape": "circle", "x": 260, "y": 130, "width": 260, "height": 260, "labelX": 63, "labelY": 50, "color": "#ffd333", "highlightColor": "#ff8f1f", "highlightOpacity": 0.72, "feedback": "Circle A contains all members in A, including overlaps with B or C." },
                            { "id": "zone_b", "label": "B", "shape": "circle", "x": 380, "y": 130, "width": 260, "height": 260, "labelX": 37, "labelY": 50, "color": "#8fb3ff", "highlightColor": "#4f8cff", "highlightOpacity": 0.72, "feedback": "Circle B contains all members in B, including overlaps with A or C." },
                            { "id": "zone_c", "label": "C", "shape": "circle", "x": 320, "y": 235, "width": 260, "height": 260, "labelX": 50, "labelY": 32, "color": "#8fe0aa", "highlightColor": "#3aa66b", "highlightOpacity": 0.72, "feedback": "Circle C contains all members in C, including overlaps with A or B." }
                          ],
                          "elements": [
                            { "id": "choice_a", "label": "Highlight A", "kind": "button", "x": 40, "y": 40, "width": 170, "height": 48 },
                            { "id": "choice_b", "label": "Highlight B", "kind": "button", "x": 40, "y": 100, "width": 170, "height": 48 },
                            { "id": "choice_c", "label": "Highlight C", "kind": "button", "x": 40, "y": 160, "width": 170, "height": 48 }
                          ],
                          "interactions": [
                            { "triggerId": "choice_a", "effect": "HIGHLIGHT_ZONE", "targetZoneId": "zone_a", "feedback": "Circle A contains every member in A." },
                            { "triggerId": "choice_b", "effect": "HIGHLIGHT_ZONE", "targetZoneId": "zone_b", "feedback": "Circle B contains every member in B." },
                            { "triggerId": "choice_c", "effect": "HIGHLIGHT_ZONE", "targetZoneId": "zone_c", "feedback": "Circle C contains every member in C." }
                          ],
                          "overlap": {
                            "enabled": true,
                            "sourceZoneIds": ["zone_a", "zone_b", "zone_c"],
                            "inputs": [
                              { "id": "A_ONLY", "label": "A", "zoneIds": ["zone_a"], "value": 33, "kind": "total" },
                              { "id": "B_ONLY", "label": "B", "zoneIds": ["zone_b"], "value": 26, "kind": "total" },
                              { "id": "C_ONLY", "label": "C", "zoneIds": ["zone_c"], "value": 22, "kind": "total" },
                              { "id": "A_AND_B", "label": "A ∩ B", "zoneIds": ["zone_a", "zone_b"], "value": 10, "kind": "intersection" },
                              { "id": "A_AND_C", "label": "A ∩ C", "zoneIds": ["zone_a", "zone_c"], "value": 8, "kind": "intersection" },
                              { "id": "B_AND_C", "label": "B ∩ C", "zoneIds": ["zone_b", "zone_c"], "value": 7, "kind": "intersection" },
                              { "id": "A_AND_B_AND_C", "label": "A ∩ B ∩ C", "zoneIds": ["zone_a", "zone_b", "zone_c"], "value": 3, "kind": "intersection" }
                            ],
                            "values": [
                              { "id": "A_ONLY", "label": "A only", "zoneIds": ["zone_a"], "value": 18, "feedback": "Members in A only, not in B or C." },
                              { "id": "B_ONLY", "label": "B only", "zoneIds": ["zone_b"], "value": 12, "feedback": "Members in B only, not in A or C." },
                              { "id": "C_ONLY", "label": "C only", "zoneIds": ["zone_c"], "value": 10, "feedback": "Members in C only, not in A or B." },
                              { "id": "A_AND_B", "label": "A ∩ B only", "zoneIds": ["zone_a", "zone_b"], "value": 7, "feedback": "Members in A and B but not in C." },
                              { "id": "A_AND_C", "label": "A ∩ C only", "zoneIds": ["zone_a", "zone_c"], "value": 5, "feedback": "Members in A and C but not in B." },
                              { "id": "B_AND_C", "label": "B ∩ C only", "zoneIds": ["zone_b", "zone_c"], "value": 4, "feedback": "Members in B and C but not in A." },
                              { "id": "A_AND_B_AND_C", "label": "A ∩ B ∩ C", "zoneIds": ["zone_a", "zone_b", "zone_c"], "value": 3, "feedback": "Members that belong to A, B, and C." }
                            ]
                          }
                        }
                        """)
        ));
        setModule.addSubTopic(subTopic(
                "Build a Venn Diagram Practice",
                """
                        <h2>Build a Venn Diagram Practice</h2>
                        <p>Build a three-set Venn diagram for clubs A, B, and C. You must add all three circles yourself, arrange them so the required overlaps exist, then enter the exact number of members in each visible region.</p>
                        <p><strong>Given:</strong> n(A)=33, n(B)=26, n(C)=22, n(A intersect B)=10, n(A intersect C)=8, n(B intersect C)=7, and n(A intersect B intersect C)=3.</p>
                        <p><strong>Success criteria:</strong> create circles A, B, and C; make all pairwise overlaps and the three-way overlap visible; enter A only=18, B only=12, C only=10, A intersect B only=7, A intersect C only=5, B intersect C only=4, and A intersect B intersect C=3.</p>
                        """,
                6,
                14,
                22,
                InteractionType.VISUAL_LAYER,
                "Ask learners to construct the Venn circles themselves and fill exact region counts.",
                validate(interactiveConfigService, InteractionType.VISUAL_LAYER, """
                        {
                          "type": "VISUAL_LAYER",
                          "mode": "PRACTICE",
                          "title": "Build the clubs Venn diagram",
                          "prompt": "Add circles A, B, and C. Arrange them so every pair overlaps and the middle three-way overlap exists, then enter each exact region count.",
                          "canvas": {
                            "width": 900,
                            "height": 520,
                            "backgroundText": ""
                          },
                          "zones": [
                            { "id": "zone_a", "label": "A", "shape": "circle", "x": 250, "y": 120, "width": 260, "height": 260, "labelX": 35, "labelY": 30, "color": "#ffd333", "highlightColor": "#ff8f1f", "highlightOpacity": 0.72, "feedback": "" },
                            { "id": "zone_b", "label": "B", "shape": "circle", "x": 390, "y": 120, "width": 260, "height": 260, "labelX": 65, "labelY": 30, "color": "#8fb3ff", "highlightColor": "#4f8cff", "highlightOpacity": 0.72, "feedback": "" },
                            { "id": "zone_c", "label": "C", "shape": "circle", "x": 320, "y": 235, "width": 260, "height": 260, "labelX": 50, "labelY": 75, "color": "#8fe0aa", "highlightColor": "#3aa66b", "highlightOpacity": 0.72, "feedback": "" }
                          ],
                          "elements": [],
                          "interactions": [],
                          "overlap": {
                            "enabled": true,
                            "sourceZoneIds": ["zone_a", "zone_b", "zone_c"],
                            "inputs": [
                              { "id": "A_ONLY", "label": "n(A)", "zoneIds": ["zone_a"], "value": 33, "kind": "total" },
                              { "id": "B_ONLY", "label": "n(B)", "zoneIds": ["zone_b"], "value": 26, "kind": "total" },
                              { "id": "C_ONLY", "label": "n(C)", "zoneIds": ["zone_c"], "value": 22, "kind": "total" },
                              { "id": "A_AND_B", "label": "n(A ∩ B)", "zoneIds": ["zone_a", "zone_b"], "value": 10, "kind": "intersection" },
                              { "id": "A_AND_C", "label": "n(A ∩ C)", "zoneIds": ["zone_a", "zone_c"], "value": 8, "kind": "intersection" },
                              { "id": "B_AND_C", "label": "n(B ∩ C)", "zoneIds": ["zone_b", "zone_c"], "value": 7, "kind": "intersection" },
                              { "id": "A_AND_B_AND_C", "label": "n(A ∩ B ∩ C)", "zoneIds": ["zone_a", "zone_b", "zone_c"], "value": 3, "kind": "intersection" }
                            ],
                            "values": []
                          },
                          "feedback": {
                            "success": "Correct. The circles create every required region and the exact counts match the inclusion-exclusion result.",
                            "failure": "Not yet. Check that all required overlaps are visible, then recompute the exact region counts from the given totals and intersections."
                          }
                        }
                        """)
        ));
        publishSeedCourse(courseRepository, setCourse, setModule);

        Course vectorCourse = seedCourse(
                courseRepository,
                "Vector",
                "Seed course from course_source_for_seed_reference/Vector.pdf covering vector quantities, coordinate vectors, unit vectors, dot product, cross product, area, and volume.",
                admin
        );
        vectorCourse.updateCover("nabla_sky");
        CourseModule vectorModule = new CourseModule(
                "Vector",
                "Based on Vector.pdf: vector quantities, coordinate vectors, unit vectors, dot products, cross products, area, and volume.",
                0,
                ContentDepth.HIGH
        );
        vectorModule.addSubTopic(subTopic(
                "Vector Quantities",
                """
                        <h2>Vector Quantities</h2>
                        <p>A vector is a quantity with both magnitude and direction, unlike a scalar, which has magnitude only. Vectors are often represented by arrows or variables marked with an arrow.</p>
                        <p>Vectors with the same magnitude and direction are considered equal even if they are drawn in different positions.</p>
                        """,
                0,
                1,
                7,
                InteractionType.NONE,
                null,
                null
        ));
        vectorModule.addSubTopic(subTopic(
                "Vectors in Rectangular Coordinates",
                """
                        <h2>Vectors in Rectangular Coordinates</h2>
                        <p>A vector in rectangular coordinates is written by its components along each axis, such as <code>(x, y)</code>. The magnitude of a two-dimensional vector is <code>sqrt(x^2 + y^2)</code>.</p>
                        <p>Addition, subtraction, and scalar multiplication are performed component by component.</p>
                        """,
                1,
                8,
                12,
                InteractionType.FORMULA_EXPLORER,
                "Let learners adjust the vector components until the magnitude equals 5.",
                validate(interactiveConfigService, InteractionType.FORMULA_EXPLORER, """
                        {
                          "type": "FORMULA_EXPLORER",
                          "mode": "PRACTICE",
                          "title": "Vector magnitude target",
                          "prompt": "Adjust x and y until the magnitude equals 5.",
                          "formula": "sqrt(x^2 + y^2)",
                          "variables": [
                            { "name": "x", "label": "X component", "min": 0, "max": 10, "step": 1, "initial": 0 },
                            { "name": "y", "label": "Y component", "min": 0, "max": 10, "step": 1, "initial": 0 }
                          ],
                          "precision": 2,
                          "successCondition": { "kind": "EXPRESSION_EQUALS", "target": 5, "tolerance": 0.01 },
                          "feedback": {
                            "success": "Correct. The vector magnitude is 5.",
                            "failure": "Not yet. Try values that form a 3-4-5 triangle."
                          }
                        }
                        """)
        ));
        vectorModule.addSubTopic(subTopic(
                "Unit Vectors",
                """
                        <h2>Unit Vectors</h2>
                        <p>A unit vector is a vector with magnitude 1. To turn a vector into a unit vector, divide the vector by its magnitude.</p>
                        """,
                2,
                13,
                16,
                InteractionType.NONE,
                null,
                null
        ));
        vectorModule.addSubTopic(subTopic(
                "Dot Product",
                """
                        <h2>Dot Product</h2>
                        <p>The dot product returns a scalar. It is used to study the angle between vectors and to test perpendicularity. In rectangular coordinates, multiply matching components and add the products.</p>
                        """,
                3,
                17,
                28,
                InteractionType.NONE,
                null,
                null
        ));
        vectorModule.addSubTopic(subTopic(
                "Cross Product",
                """
                        <h2>Cross Product</h2>
                        <p>The cross product returns a vector perpendicular to the two original vectors. It is used with three-dimensional vectors and connects to the area of a parallelogram.</p>
                        """,
                4,
                29,
                31,
                InteractionType.FORMULA_EXPLORER,
                "Let learners explore the cross product magnitude by adjusting vector length and angle.",
                validate(interactiveConfigService, InteractionType.FORMULA_EXPLORER, """
                        {
                          "type": "FORMULA_EXPLORER",
                          "mode": "PRACTICE",
                          "title": "Cross product magnitude",
                          "prompt": "Adjust the vector lengths and angle until the parallelogram area is 24.",
                          "formula": "a * b * sin(theta)",
                          "variables": [
                            { "name": "a", "label": "Vector a length", "min": 1, "max": 12, "step": 1, "initial": 4 },
                            { "name": "b", "label": "Vector b length", "min": 1, "max": 12, "step": 1, "initial": 6 },
                            { "name": "theta", "label": "Angle radians", "min": 0, "max": 3.14, "step": 0.01, "initial": 1.57 }
                          ],
                          "precision": 2,
                          "successCondition": {
                            "kind": "EXPRESSION_EQUALS",
                            "target": 24,
                            "tolerance": 0.1
                          },
                          "feedback": {
                            "success": "Correct. The cross product magnitude equals the parallelogram area.",
                            "failure": "Not yet. Use |a||b|sin(theta) to reach the target area."
                          }
                        }
                        """)
        ));
        vectorModule.addSubTopic(subTopic(
                "Area and Volume",
                """
                        <h2>Area and Volume</h2>
                        <p>Cross products and scalar triple products help compute the area and volume of geometric shapes built from vectors, such as parallelograms and parallelepipeds.</p>
                        """,
                5,
                32,
                40,
                InteractionType.NONE,
                null,
                null
        ));
        publishSeedCourse(courseRepository, vectorCourse, vectorModule);

        Course logicCourse = seedCourse(
                courseRepository,
                "Logic",
                "Seed course covering propositional logic: statements, connectives, truth tables, valve circuits, and equivalence simplification with logic laws.",
                admin
        );
        logicCourse.updateCover("forall_amber");
        CourseModule logicModule = new CourseModule(
                "Logic",
                "Propositional logic: connectives, truth tables, valve circuits, and simplification with logic laws.",
                0,
                ContentDepth.HIGH
        );
        logicModule.addSubTopic(subTopic(
                "Statements and Connectives",
                """
                        <h2>Statements and Connectives</h2>
                        <p>A proposition is a statement that is either true (T) or false (F). Compound statements are built with connectives: negation <code>¬</code>, conjunction <code>∧</code>, disjunction <code>∨</code>, implication <code>→</code>, and biconditional <code>↔</code>.</p>
                        <p>A truth table lists the output of a compound statement for every combination of truth values of its variables.</p>
                        """,
                0,
                1,
                4,
                InteractionType.NONE,
                null,
                null
        ));
        logicModule.addSubTopic(subTopic(
                "Valve Circuit: Free Explore",
                """
                        <h2>Valve Circuit: Free Explore</h2>
                        <p>Think of each variable as a valve: open means true, closed means false. Water flows through an AND gate only when both inputs flow, and through an OR gate when at least one input flows. A NOT gate inverts the flow.</p>
                        <p>Flip the valves and watch how the output tank responds for <code>(P ∧ Q) ∨ ¬R</code>.</p>
                        """,
                1,
                5,
                8,
                InteractionType.LOGIC_FLOW,
                "Flip the valves freely and observe how the output changes.",
                validate(interactiveConfigService, InteractionType.LOGIC_FLOW, """
                        {
                          "type": "LOGIC_FLOW",
                          "kind": "CIRCUIT",
                          "mode": "VISUALIZATION",
                          "title": "Explore a valve circuit",
                          "prompt": "Flip the valves freely and observe how the output changes.",
                          "expression": "(P ∧ Q) ∨ ¬R",
                          "goal": "EXPLORE"
                        }
                        """)
        ));
        logicModule.addSubTopic(subTopic(
                "Valve Circuit Practice",
                """
                        <h2>Valve Circuit Practice</h2>
                        <p>The statement <code>P ∧ ¬Q</code> is true only when <code>P</code> is true and <code>Q</code> is false.</p>
                        <p>Set each valve so the water reaches the output tank, then check your answer.</p>
                        """,
                2,
                5,
                8,
                InteractionType.LOGIC_FLOW,
                "Flip the valves so the statement evaluates to true and the water flows.",
                validate(interactiveConfigService, InteractionType.LOGIC_FLOW, """
                        {
                          "type": "LOGIC_FLOW",
                          "kind": "CIRCUIT",
                          "mode": "PRACTICE",
                          "title": "Make the water flow",
                          "prompt": "Flip the valves so the statement evaluates to true and the water flows.",
                          "expression": "P ∧ ¬Q",
                          "goal": "TRUE",
                          "feedback": {
                            "success": "Correct. With P true and Q false, the water reaches the tank.",
                            "failure": "Not yet. Recheck each truth value: ¬Q needs Q to be false."
                          }
                        }
                        """)
        ));
        logicModule.addSubTopic(subTopic(
                "Simplify an Implication",
                """
                        <h2>Simplify an Implication</h2>
                        <p>Logical equivalence laws let you rewrite a statement without changing its truth table. The implication law states <code>P → Q ≡ ¬P ∨ Q</code>.</p>
                        <p>Apply the allowed laws step by step to rewrite the starting statement, then enter the final simplified expression.</p>
                        """,
                3,
                9,
                14,
                InteractionType.LOGIC_FLOW,
                "Rewrite P → Q using the implication law and enter the equivalent expression.",
                validate(interactiveConfigService, InteractionType.LOGIC_FLOW, """
                        {
                          "type": "LOGIC_FLOW",
                          "kind": "SIMPLIFY",
                          "mode": "PRACTICE",
                          "title": "Simplify an implication",
                          "prompt": "Rewrite P → Q using the implication law and enter the equivalent expression.",
                          "start": "P → Q",
                          "target": "¬P ∨ Q",
                          "allowedLaws": ["IMPLICATION", "DOUBLE_NEGATION", "DE_MORGAN"],
                          "steps": [
                            { "law": "IMPLICATION", "result": "¬P ∨ Q", "note": "An implication is false only when P is true and Q is false, which matches ¬P ∨ Q." }
                          ],
                          "feedback": {
                            "success": "Correct. The expression is logically equivalent — the truth table never changed.",
                            "failure": "Not yet. Apply the implication law: P → Q ≡ ¬P ∨ Q."
                          }
                        }
                        """)
        ));
        logicModule.addSubTopic(subTopic(
                "Simplify with Distribution",
                """
                        <h2>Simplify with Distribution</h2>
                        <p>Combine the distributive, complement, and identity laws to reduce <code>(P ∧ ¬Q) ∨ (P ∧ Q)</code> to a single variable.</p>
                        <p>Work through the steps, then enter the final simplified expression.</p>
                        """,
                4,
                15,
                20,
                InteractionType.LOGIC_FLOW,
                "Simplify the statement step by step and enter the final expression.",
                validate(interactiveConfigService, InteractionType.LOGIC_FLOW, """
                        {
                          "type": "LOGIC_FLOW",
                          "kind": "SIMPLIFY",
                          "mode": "PRACTICE",
                          "title": "Simplify with distribution",
                          "prompt": "Simplify the statement step by step and enter the final expression.",
                          "start": "(P ∧ ¬Q) ∨ (P ∧ Q)",
                          "target": "P",
                          "allowedLaws": ["DISTRIBUTIVE", "COMPLEMENT", "IDENTITY"],
                          "steps": [
                            { "law": "DISTRIBUTIVE", "result": "P ∧ (¬Q ∨ Q)", "note": "Factor P out of both parts." },
                            { "law": "COMPLEMENT", "result": "P ∧ T", "note": "¬Q ∨ Q is always true." },
                            { "law": "IDENTITY", "result": "P", "note": "P ∧ T is just P." }
                          ],
                          "feedback": {
                            "success": "Correct. Different pipes — same water. The truth table never changed.",
                            "failure": "Not yet. Factor P out first, then use the complement and identity laws."
                          }
                        }
                        """)
        ));
        publishSeedCourse(courseRepository, logicCourse, logicModule);
    }

    private Course seedCourse(CourseRepository courseRepository, String title, String description, User admin) {
        Course course = courseRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(existingCourse -> title.equals(existingCourse.getTitle()))
                .findFirst()
                .orElseGet(() -> new Course(title, description, admin));
        course.updateDetails(title, description);
        course.clearModules();
        return course;
    }

    private void publishSeedCourse(CourseRepository courseRepository, Course course, CourseModule module) {
        course.addModule(module);
        course.publish();
        courseRepository.save(course);
    }

    private void removeLegacyAggregateSeedCourse(CourseRepository courseRepository) {
        courseRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(course -> "Interactive Math Foundations".equals(course.getTitle()))
                .forEach(courseRepository::delete);
    }

    private SubTopic subTopic(
            String title,
            String content,
            int sortOrder,
            Integer pageStart,
            Integer pageEnd,
            InteractionType interactionType,
            String interactionPrompt,
            String interactionConfig
    ) {
        return new SubTopic(
                title,
                content,
                sortOrder,
                SubTopicSourceType.MANUAL,
                pageStart,
                pageEnd,
                interactionType,
                interactionPrompt,
                interactionConfig
        );
    }

    private String validate(
            InteractiveConfigService interactiveConfigService,
            InteractionType interactionType,
            String interactionConfig
    ) {
        return interactiveConfigService.validateAndNormalize(interactionType, interactionConfig);
    }
}
