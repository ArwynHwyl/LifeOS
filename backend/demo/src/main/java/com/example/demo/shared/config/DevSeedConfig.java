package com.example.demo.shared.config;

import com.example.demo.course.entity.ContentDepth;
import com.example.demo.course.entity.Course;
import com.example.demo.course.entity.CourseModule;
import com.example.demo.course.entity.InteractionType;
import com.example.demo.course.entity.SubTopic;
import com.example.demo.course.entity.SubTopicSourceType;
import com.example.demo.course.repository.CourseRepository;
import com.example.demo.course.repository.LearnerInteractiveProgressRepository;
import com.example.demo.user.entity.User;
import com.example.demo.user.entity.UserRole;
import com.example.demo.user.entity.UserStatus;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.course.service.interactive.InteractiveConfigService;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DevSeedConfig {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.demo.course.repository.SubTopicRepository subTopicRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.demo.course.repository.SubTopicAssetRepository subTopicAssetRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.beans.factory.ObjectProvider<software.amazon.awssdk.services.s3.S3Client> s3Client;

    @org.springframework.beans.factory.annotation.Autowired
    private S3StorageProperties storageProperties;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DevSeedConfig.class);

    @Bean
    CommandLineRunner seedDevData(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CourseRepository courseRepository,
            LearnerInteractiveProgressRepository progressRepository,
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

            seedInteractiveReferenceCourse(courseRepository, progressRepository, interactiveConfigService, admin);
            attachSeedImages();
        };
    }

    private void seedInteractiveReferenceCourse(
            CourseRepository courseRepository,
            LearnerInteractiveProgressRepository progressRepository,
            InteractiveConfigService interactiveConfigService,
            User admin
    ) {
        if (referenceCoursesAlreadySeeded(courseRepository)) {
            return;
        }
        removeLegacyAggregateSeedCourse(courseRepository, progressRepository);

        Course matrixCourse = seedCourse(
                courseRepository,
                progressRepository,
                "Matrix",
                "Learn how matrices store and transform data: from basic operations and multiplication to determinants, inverses, and solving systems of linear equations.",
                admin
        );
        matrixCourse.updateCover("matrix_violet");
        CourseModule matrixModule = new CourseModule(
                "Matrices and Linear Systems",
                "Build fluency with matrix arithmetic, then use determinants and inverses to solve equations and linear systems.",
                0,
                ContentDepth.HIGH
        );
        matrixModule.addSubTopic(subTopic(
                "Matrices",
                lesson("matrix", "Matrices"),
                0,
                1,
                4,
                InteractionType.NONE,
                null,
                null
        ));
        matrixModule.addSubTopic(subTopic(
                "Matrix Multiplication",
                lesson("matrix", "Matrix Multiplication"),
                1,
                5,
                9,
                InteractionType.NONE,
                null,
                null
        ));
        matrixModule.addSubTopic(subTopic(
                "Determinants",
                lesson("matrix", "Determinants"),
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
                lesson("matrix", "Multiplicative Inverses"),
                3,
                16,
                21,
                InteractionType.NONE,
                null,
                null
        ));
        matrixModule.addSubTopic(subTopic(
                "Matrix Equations",
                lesson("matrix", "Matrix Equations"),
                4,
                22,
                25,
                InteractionType.NONE,
                null,
                null
        ));
        matrixModule.addSubTopic(subTopic(
                "Augmented Matrices and Linear Systems",
                lesson("matrix", "Augmented Matrices and Linear Systems"),
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
                lesson("matrix", "Graph Point Match Practice"),
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
                progressRepository,
                "Probability",
                "Model uncertainty with random variables: probability distributions, expected value, standard deviation, the binomial distribution, and the normal curve.",
                admin
        );
        probabilityCourse.updateCover("prob_rose");
        CourseModule probabilityModule = new CourseModule(
                "Discrete and Normal Distributions",
                "Describe random outcomes with distributions, summarise them with mean and spread, and work with binomial and normal models.",
                0,
                ContentDepth.HIGH
        );
        probabilityModule.addSubTopic(subTopic(
                "Discrete Probability Distributions",
                lesson("probability", "Discrete Probability Distributions"),
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
                lesson("probability", "Expected Value of a Discrete Random Variable"),
                2,
                4,
                6,
                InteractionType.NONE,
                null,
                null
        ));
        probabilityModule.addSubTopic(subTopic(
                "Standard Deviation and Uniform Distributions",
                lesson("probability", "Standard Deviation and Uniform Distributions"),
                3,
                7,
                8,
                InteractionType.NONE,
                null,
                null
        ));
        probabilityModule.addSubTopic(subTopic(
                "Binomial Distribution",
                lesson("probability", "Binomial Distribution"),
                4,
                9,
                11,
                InteractionType.NONE,
                null,
                null
        ));
        probabilityModule.addSubTopic(subTopic(
                "Continuous and Normal Distributions",
                lesson("probability", "Continuous and Normal Distributions"),
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
                lesson("probability", "Normal Curve Peak Practice"),
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
                progressRepository,
                "Set",
                "Master set notation and operations, count subsets with power sets, and solve counting problems with Venn diagrams and inclusion-exclusion.",
                admin
        );
        setCourse.updateCover("union_green");
        CourseModule setFoundationsModule = new CourseModule(
                "1. Set Foundations and Notation",
                "Read set notation accurately, distinguish elements from subsets, and connect core operations to Venn regions.",
                0,
                ContentDepth.HIGH
        );
        setFoundationsModule.addSubTopic(subTopic(
                "What Is a Set?",
                lesson("set", "What Is a Set?"),
                0,
                1,
                3,
                InteractionType.VISUAL_LAYER,
                "Let learners click labels to connect a set, its elements, and the empty set idea.",
                validate(interactiveConfigService, InteractionType.VISUAL_LAYER, """
                        {
                          "type": "VISUAL_LAYER",
                          "mode": "VISUALIZATION",
                          "title": "Set, element, and empty set",
                          "canvas": { "width": 900, "height": 520, "backgroundText": "Click each label to connect notation with the diagram." },
                          "zones": [
                            { "id": "zone_a", "label": "A = {1, 2, 3}", "shape": "circle", "x": 285, "y": 105, "width": 330, "height": 330, "labelX": 50, "labelY": 18, "color": "#8fe0aa", "highlightColor": "#3aa66b", "highlightOpacity": 0.82, "feedback": "Set A is the whole collection, not just one member." },
                            { "id": "zone_empty", "label": "empty set", "shape": "circle", "x": 655, "y": 265, "width": 130, "height": 130, "labelX": 50, "labelY": 50, "color": "#f2eadf", "highlightColor": "#d9cec0", "highlightOpacity": 0.9, "feedback": "The empty set has no elements, but it is still a set." }
                          ],
                          "elements": [
                            { "id": "btn_set", "label": "Set A", "kind": "button", "x": 55, "y": 55, "width": 150, "height": 48 },
                            { "id": "btn_element", "label": "Element 2", "kind": "button", "x": 55, "y": 118, "width": 150, "height": 48 },
                            { "id": "btn_empty", "label": "Empty set", "kind": "button", "x": 55, "y": 181, "width": 150, "height": 48 },
                            { "id": "member_1", "label": "1", "kind": "hotspot", "x": 380, "y": 225, "width": 48, "height": 48 },
                            { "id": "member_2", "label": "2", "kind": "hotspot", "x": 470, "y": 185, "width": 48, "height": 48 },
                            { "id": "member_3", "label": "3", "kind": "hotspot", "x": 505, "y": 300, "width": 48, "height": 48 }
                          ],
                          "interactions": [
                            { "triggerId": "btn_set", "effect": "HIGHLIGHT_ZONE", "targetZoneId": "zone_a", "feedback": "A refers to the whole collection {1, 2, 3}." },
                            { "triggerId": "btn_element", "effect": "HIGHLIGHT_ZONE", "targetZoneId": "zone_a", "feedback": "The statement 2 is in A is about one member inside the set." },
                            { "triggerId": "btn_empty", "effect": "HIGHLIGHT_ZONE", "targetZoneId": "zone_empty", "feedback": "The empty set has no members, but it is a subset of every set." },
                            { "triggerId": "member_1", "effect": "SHOW_FEEDBACK", "feedback": "1 is an element of A." },
                            { "triggerId": "member_2", "effect": "SHOW_FEEDBACK", "feedback": "2 is an element of A." },
                            { "triggerId": "member_3", "effect": "SHOW_FEEDBACK", "feedback": "3 is an element of A." }
                          ]
                        }
                        """)
        ));
        setFoundationsModule.addSubTopic(subTopic(
                "Membership, Subsets, and Proper Subsets",
                lesson("set", "Membership, Subsets, and Proper Subsets"),
                1,
                1,
                3,
                InteractionType.QUIZ,
                "Check whether learners can distinguish elements from subsets.",
                validate(interactiveConfigService, InteractionType.QUIZ, """
                        {
                          "type": "QUIZ",
                          "mode": "PRACTICE",
                          "title": "Element or subset?",
                          "prompt": "Choose the true statement for A = {1, 2, 3}.",
                          "question": "Which statement is true when A = {1, 2, 3}?",
                          "options": [
                            { "id": "a", "label": "2 is a subset of A", "correct": false },
                            { "id": "b", "label": "{2} is an element of A", "correct": false },
                            { "id": "c", "label": "{2} is a subset of A", "correct": true },
                            { "id": "d", "label": "{1, 2, 3} is a proper subset of A", "correct": false }
                          ],
                          "successCondition": { "kind": "QUIZ_CORRECT_OPTION", "correctOptionId": "c" },
                          "feedback": {
                            "success": "Correct. {2} is a set whose only element is already in A.",
                            "failure": "Not yet. Elements use membership language; sets use subset language."
                          }
                        }
                        """)
        ));
        setFoundationsModule.addSubTopic(subTopic(
                "Union, Intersection, Difference, and Complement",
                lesson("set", "Union, Intersection, Difference, and Complement"),
                2,
                4,
                8,
                InteractionType.VISUAL_LAYER,
                "Show a two-set Venn diagram where learners can inspect operations and exact regions.",
                validate(interactiveConfigService, InteractionType.VISUAL_LAYER, """
                        {
                          "type": "VISUAL_LAYER",
                          "mode": "VISUALIZATION",
                          "title": "Two-set operations map",
                          "canvas": { "width": 900, "height": 520, "backgroundText": "Click a circle, operation button, or region number." },
                          "zones": [
                            { "id": "zone_a", "label": "A", "shape": "circle", "x": 250, "y": 135, "width": 280, "height": 280, "labelX": 34, "labelY": 25, "color": "#ffd333", "highlightColor": "#ff8f1f", "highlightOpacity": 0.78, "feedback": "Set A contains the A-only region and the overlap." },
                            { "id": "zone_b", "label": "B", "shape": "circle", "x": 390, "y": 135, "width": 280, "height": 280, "labelX": 66, "labelY": 25, "color": "#8fb3ff", "highlightColor": "#4f8cff", "highlightOpacity": 0.78, "feedback": "Set B contains the B-only region and the overlap." }
                          ],
                          "elements": [
                            { "id": "btn_a", "label": "Highlight A", "kind": "button", "x": 40, "y": 40, "width": 170, "height": 48 },
                            { "id": "btn_b", "label": "Highlight B", "kind": "button", "x": 40, "y": 100, "width": 170, "height": 48 },
                            { "id": "btn_union", "label": "A union B", "kind": "button", "x": 40, "y": 160, "width": 170, "height": 48 },
                            { "id": "btn_intersection", "label": "A intersect B", "kind": "button", "x": 40, "y": 220, "width": 170, "height": 48 }
                          ],
                          "interactions": [
                            { "triggerId": "btn_a", "effect": "HIGHLIGHT_ZONE", "targetZoneId": "zone_a", "feedback": "n(A) counts A only plus A intersect B." },
                            { "triggerId": "btn_b", "effect": "HIGHLIGHT_ZONE", "targetZoneId": "zone_b", "feedback": "n(B) counts B only plus A intersect B." },
                            { "triggerId": "btn_union", "effect": "SHOW_FEEDBACK", "feedback": "A union B includes A only, the overlap, and B only." },
                            { "triggerId": "btn_intersection", "effect": "SHOW_FEEDBACK", "feedback": "A intersect B is the overlap shared by both sets." }
                          ],
                          "overlap": {
                            "enabled": true,
                            "sourceZoneIds": ["zone_a", "zone_b"],
                            "inputs": [
                              { "id": "A_ONLY", "label": "n(A)", "zoneIds": ["zone_a"], "value": 20, "kind": "total" },
                              { "id": "B_ONLY", "label": "n(B)", "zoneIds": ["zone_b"], "value": 15, "kind": "total" },
                              { "id": "A_AND_B", "label": "n(A intersect B)", "zoneIds": ["zone_a", "zone_b"], "value": 6, "kind": "intersection" }
                            ],
                            "values": [
                              { "id": "A_ONLY", "label": "A only", "zoneIds": ["zone_a"], "value": 14, "feedback": "A only is n(A) - n(A intersect B) = 20 - 6 = 14." },
                              { "id": "B_ONLY", "label": "B only", "zoneIds": ["zone_b"], "value": 9, "feedback": "B only is n(B) - n(A intersect B) = 15 - 6 = 9." },
                              { "id": "A_AND_B", "label": "A intersect B", "zoneIds": ["zone_a", "zone_b"], "value": 6, "feedback": "The overlap belongs to both A and B." }
                            ]
                          }
                        }
                        """)
        ));
        setFoundationsModule.addSubTopic(subTopic(
                "Operations Checkpoint",
                lesson("set", "Operations Checkpoint"),
                3,
                4,
                8,
                InteractionType.QUIZ,
                "Check operation notation before moving into counting formulas.",
                validate(interactiveConfigService, InteractionType.QUIZ, """
                        {
                          "type": "QUIZ",
                          "mode": "PRACTICE",
                          "title": "Operation notation check",
                          "prompt": "Choose the operation that keeps elements appearing in both sets.",
                          "question": "Which operation keeps only elements that are in both A and B?",
                          "options": [
                            { "id": "a", "label": "A union B", "correct": false },
                            { "id": "b", "label": "A intersect B", "correct": true },
                            { "id": "c", "label": "A - B", "correct": false },
                            { "id": "d", "label": "Complement of A", "correct": false }
                          ],
                          "successCondition": { "kind": "QUIZ_CORRECT_OPTION", "correctOptionId": "b" },
                          "feedback": {
                            "success": "Correct. Intersection keeps the shared region.",
                            "failure": "Not yet. Look for the operation that requires membership in A and B at the same time."
                          }
                        }
                        """)
        ));

        CourseModule setPowerModule = new CourseModule(
                "2. Power Sets and Subset Formulas",
                "Build the power set concept and practice formulas for total subsets and selected subset sizes.",
                1,
                ContentDepth.HIGH
        );
        setPowerModule.addSubTopic(subTopic(
                "Power Sets",
                lesson("set", "Power Sets"),
                0,
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
                          "successCondition": { "kind": "QUIZ_CORRECT_OPTION", "correctOptionId": "a" },
                          "feedback": {
                            "success": "Correct. A power set contains subsets as its elements.",
                            "failure": "Not yet. List every subset of A first."
                          }
                        }
                        """)
        ));
        setPowerModule.addSubTopic(subTopic(
                "Subset Count Formula",
                lesson("set", "Subset Count Formula"),
                1,
                9,
                13,
                InteractionType.FORMULA_EXPLORER,
                "Let learners inspect several subset-count formulas from one value of n.",
                validate(interactiveConfigService, InteractionType.FORMULA_EXPLORER, """
                        {
                          "type": "FORMULA_EXPLORER",
                          "mode": "VISUALIZATION",
                          "title": "Subset formula explorer",
                          "formula": "2^n",
                          "variables": [
                            { "name": "n", "label": "Members n", "min": 0, "max": 10, "step": 1, "initial": 4 }
                          ],
                          "precision": 0,
                          "formulaOptions": [
                            {
                              "id": "all-subsets",
                              "label": "All subsets",
                              "formula": "2^n",
                              "description": "Each member has two choices: included or not included.",
                              "steps": [
                                { "label": "Two choices per member", "expression": "2^n", "explanation": "For n members, multiply 2 choices n times." }
                              ]
                            },
                            {
                              "id": "non-empty-subsets",
                              "label": "Non-empty",
                              "formula": "2^n - 1",
                              "description": "Remove the empty set from the full power set.",
                              "steps": [
                                { "label": "Start with all subsets", "expression": "2^n", "explanation": "The power set includes the empty set." },
                                { "label": "Remove empty set", "expression": "2^n - 1", "explanation": "Subtract one subset: the empty set." }
                              ]
                            },
                            {
                              "id": "two-member-subsets",
                              "label": "2-member subsets",
                              "formula": "n * (n - 1) / 2",
                              "description": "Choose two different members, then divide by two because order does not matter.",
                              "steps": [
                                { "label": "Pick the first member", "expression": "n", "explanation": "There are n choices for the first selected member." },
                                { "label": "Pick a different member", "expression": "n * (n - 1)", "explanation": "After one member is selected, n - 1 choices remain." },
                                { "label": "Remove order duplicates", "expression": "n * (n - 1) / 2", "explanation": "{a, b} and {b, a} are the same subset." }
                              ]
                            },
                            {
                              "id": "three-member-subsets",
                              "label": "3-member subsets",
                              "formula": "n * (n - 1) * (n - 2) / 6",
                              "description": "Choose three different members and divide by 3! because order does not matter.",
                              "steps": [
                                { "label": "Ordered triples", "expression": "n * (n - 1) * (n - 2)", "explanation": "Pick three different members in order." },
                                { "label": "Remove ordering", "expression": "n * (n - 1) * (n - 2) / 6", "explanation": "Six orders describe the same three-member subset." }
                              ]
                            }
                          ]
                        }
                        """)
        ));
        setPowerModule.addSubTopic(subTopic(
                "Subset Count Target Practice",
                lesson("set", "Subset Count Target Practice"),
                2,
                9,
                13,
                InteractionType.FORMULA_EXPLORER,
                "Challenge learners to solve 2^n = 32 with the formula explorer.",
                validate(interactiveConfigService, InteractionType.FORMULA_EXPLORER, """
                        {
                          "type": "FORMULA_EXPLORER",
                          "mode": "PRACTICE",
                          "title": "Power set target",
                          "prompt": "Adjust n until the number of subsets equals 32.",
                          "formula": "2^n",
                          "variables": [
                            { "name": "n", "label": "Members n", "min": 0, "max": 10, "step": 1, "initial": 3 }
                          ],
                          "precision": 0,
                          "successCondition": { "kind": "EXPRESSION_EQUALS", "target": 32, "tolerance": 0.01 },
                          "feedback": {
                            "success": "Correct. 2^5 = 32, so the original set has 5 elements.",
                            "failure": "Not yet. Try powers of two until the result is 32."
                          }
                        }
                        """)
        ));
        setPowerModule.addSubTopic(subTopic(
                "Power Set Size Checkpoint",
                lesson("set", "Power Set Size Checkpoint"),
                3,
                9,
                13,
                InteractionType.QUIZ,
                "Check direct use of the 2^n formula.",
                validate(interactiveConfigService, InteractionType.QUIZ, """
                        {
                          "type": "QUIZ",
                          "mode": "PRACTICE",
                          "title": "Power set size",
                          "prompt": "Choose the size of P(A).",
                          "question": "If A has 4 elements, how many elements does P(A) have?",
                          "options": [
                            { "id": "a", "label": "4", "correct": false },
                            { "id": "b", "label": "8", "correct": false },
                            { "id": "c", "label": "16", "correct": true },
                            { "id": "d", "label": "24", "correct": false }
                          ],
                          "successCondition": { "kind": "QUIZ_CORRECT_OPTION", "correctOptionId": "c" },
                          "feedback": {
                            "success": "Correct. 2^4 = 16.",
                            "failure": "Not yet. Use 2^n with n = 4."
                          }
                        }
                        """)
        ));

        CourseModule setCountingModule = new CourseModule(
                "3. Venn Counting and Inclusion-Exclusion",
                "Turn totals and intersections into exact Venn regions, then practice building two-set and three-set diagrams.",
                2,
                ContentDepth.HIGH
        );
        setCountingModule.addSubTopic(subTopic(
                "Two-Set Venn Counting",
                lesson("set", "Two-Set Venn Counting"),
                0,
                14,
                18,
                InteractionType.FORMULA_EXPLORER,
                "Show two-set Venn formulas for exact regions, union, and outside count.",
                validate(interactiveConfigService, InteractionType.FORMULA_EXPLORER, """
                        {
                          "type": "FORMULA_EXPLORER",
                          "mode": "VISUALIZATION",
                          "title": "Two-set counting formulas",
                          "formula": "a + b - ab",
                          "variables": [
                            { "name": "u", "label": "n(U)", "min": 0, "max": 100, "step": 1, "initial": 30 },
                            { "name": "a", "label": "n(A)", "min": 0, "max": 80, "step": 1, "initial": 14 },
                            { "name": "b", "label": "n(B)", "min": 0, "max": 80, "step": 1, "initial": 12 },
                            { "name": "ab", "label": "n(A intersect B)", "min": 0, "max": 50, "step": 1, "initial": 5 }
                          ],
                          "precision": 0,
                          "formulaOptions": [
                            {
                              "id": "a-only",
                              "label": "A only",
                              "formula": "a - ab",
                              "description": "Members in A but not in B.",
                              "steps": [
                                { "label": "Start with all of A", "expression": "a", "explanation": "A includes the overlap." },
                                { "label": "Remove the overlap", "expression": "a - ab", "explanation": "Subtract A intersect B to leave A only." }
                              ]
                            },
                            {
                              "id": "b-only",
                              "label": "B only",
                              "formula": "b - ab",
                              "description": "Members in B but not in A.",
                              "steps": [
                                { "label": "Start with all of B", "expression": "b", "explanation": "B includes the overlap." },
                                { "label": "Remove the overlap", "expression": "b - ab", "explanation": "Subtract A intersect B to leave B only." }
                              ]
                            },
                            {
                              "id": "a-union-b",
                              "label": "A union B",
                              "formula": "a + b - ab",
                              "description": "Members in A or B.",
                              "steps": [
                                { "label": "Add A and B", "expression": "a + b", "explanation": "The overlap is counted twice." },
                                { "label": "Subtract one overlap", "expression": "a + b - ab", "explanation": "Now each member in the union is counted once." }
                              ]
                            },
                            {
                              "id": "outside",
                              "label": "Outside",
                              "formula": "u - a - b + ab",
                              "description": "Members in the universal set but outside both A and B.",
                              "steps": [
                                { "label": "Find the union", "expression": "a + b - ab", "explanation": "This is every member inside A or B." },
                                { "label": "Subtract from U", "expression": "u - a - b + ab", "explanation": "Everything not in the union is outside both circles." }
                              ]
                            }
                          ]
                        }
                        """)
        ));
        setCountingModule.addSubTopic(subTopic(
                "Build a 2-Set Venn Practice",
                lesson("set", "Build a 2-Set Venn Practice"),
                1,
                14,
                18,
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
                              { "id": "A_ONLY", "label": "n(A)", "zoneIds": ["zone_a"], "value": 14, "kind": "total" },
                              { "id": "B_ONLY", "label": "n(B)", "zoneIds": ["zone_b"], "value": 12, "kind": "total" },
                              { "id": "A_AND_B", "label": "n(A intersect B)", "zoneIds": ["zone_a", "zone_b"], "value": 5, "kind": "intersection" }
                            ],
                            "values": []
                          },
                          "feedback": {
                            "success": "Correct. A only is 9, B only is 7, and the overlap is 5.",
                            "failure": "Not yet. Subtract the overlap from each set total to get the exact outside regions."
                          }
                        }
                        """)
        ));
        setCountingModule.addSubTopic(subTopic(
                "Three-Set Inclusion-Exclusion",
                lesson("set", "Three-Set Inclusion-Exclusion"),
                2,
                14,
                22,
                InteractionType.FORMULA_EXPLORER,
                "Let learners choose a three-set counting formula and inspect the result and steps from sample values.",
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
                              "id": "b-only",
                              "label": "B only",
                              "formula": "b - ab - bc + abc",
                              "description": "Shade only the region in B but not in A or C.",
                              "steps": [
                                { "label": "Start with all of B", "expression": "b", "explanation": "Circle B contains B only and the regions overlapping with A or C." },
                                { "label": "Subtract overlaps with A and C", "expression": "b - ab - bc", "explanation": "The middle three-set region has now been subtracted twice." },
                                { "label": "Add the middle back", "expression": "b - ab - bc + abc", "explanation": "Add A intersect B intersect C back once, leaving B only." }
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
                            },
                            {
                              "id": "outside-all",
                              "label": "Outside all",
                              "formula": "u - a - b - c + ab + ac + bc - abc",
                              "description": "Count members in the universal set but outside A, B, and C.",
                              "steps": [
                                { "label": "Find the union", "expression": "a + b + c - ab - ac - bc + abc", "explanation": "This counts every member inside at least one circle." },
                                { "label": "Subtract from U", "expression": "u - a - b - c + ab + ac + bc - abc", "explanation": "Everything left is outside all three sets." }
                              ]
                            }
                          ]
                        }
                        """)
        ));
        setCountingModule.addSubTopic(subTopic(
                "Three-Set Venn Visual Map",
                lesson("set", "Three-Set Venn Visual Map"),
                3,
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
        setCountingModule.addSubTopic(subTopic(
                "Build the Final Venn Diagram",
                lesson("set", "Build the Final Venn Diagram"),
                4,
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
        setCountingModule.addSubTopic(subTopic(
                "Final Set Counting Checkpoint",
                lesson("set", "Final Set Counting Checkpoint"),
                5,
                14,
                22,
                InteractionType.QUIZ,
                "Ask learners to finish the course by computing the outside region.",
                validate(interactiveConfigService, InteractionType.QUIZ, """
                        {
                          "type": "QUIZ",
                          "mode": "PRACTICE",
                          "title": "Outside-region checkpoint",
                          "prompt": "Choose the number of members outside A, B, and C.",
                          "question": "If n(U)=60 and n(A union B union C)=56, how many members are outside all three sets?",
                          "options": [
                            { "id": "a", "label": "2", "correct": false },
                            { "id": "b", "label": "3", "correct": false },
                            { "id": "c", "label": "4", "correct": true },
                            { "id": "d", "label": "56", "correct": false }
                          ],
                          "successCondition": { "kind": "QUIZ_CORRECT_OPTION", "correctOptionId": "c" },
                          "feedback": {
                            "success": "Correct. Outside all three sets is 60 - 56 = 4.",
                            "failure": "Not yet. Subtract the union count from the universal-set count."
                          }
                        }
                        """)
        ));
        publishSeedCourse(courseRepository, setCourse, setFoundationsModule, setPowerModule, setCountingModule);

        Course vectorCourse = seedCourse(
                courseRepository,
                progressRepository,
                "Vector",
                "Describe quantities that have both size and direction: components, unit vectors, dot and cross products, and the areas and volumes they measure.",
                admin
        );
        vectorCourse.updateCover("nabla_sky");
        CourseModule vectorModule = new CourseModule(
                "Vectors in Two and Three Dimensions",
                "Work with magnitude and direction using components, then measure alignment, area, and volume with dot and cross products.",
                0,
                ContentDepth.HIGH
        );
        vectorModule.addSubTopic(subTopic(
                "Vector Quantities",
                lesson("vector", "Vector Quantities"),
                0,
                1,
                7,
                InteractionType.NONE,
                null,
                null
        ));
        vectorModule.addSubTopic(subTopic(
                "Vectors in Rectangular Coordinates",
                lesson("vector", "Vectors in Rectangular Coordinates"),
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
                lesson("vector", "Unit Vectors"),
                2,
                13,
                16,
                InteractionType.NONE,
                null,
                null
        ));
        vectorModule.addSubTopic(subTopic(
                "Dot Product",
                lesson("vector", "Dot Product"),
                3,
                17,
                28,
                InteractionType.NONE,
                null,
                null
        ));
        vectorModule.addSubTopic(subTopic(
                "Cross Product",
                lesson("vector", "Cross Product"),
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
                lesson("vector", "Area and Volume"),
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
                progressRepository,
                "Logic",
                "Propositional logic for software engineering: truth values, Boolean operators, program conditions, implication, truth tables, and equivalence laws.",
                admin
        );
        logicCourse.updateCover("forall_amber");
        CourseModule logicFoundationsModule = new CourseModule(
                "1. Propositions and Truth Tables",
                "Build compound propositions, predict their truth values, and verify the result with Boolean circuits and truth tables.",
                0,
                ContentDepth.HIGH
        );
        logicFoundationsModule.addSubTopic(subTopic(
                "Propositions and Truth Values",
                lesson("logic", "Propositions and Truth Values"),
                0,
                null,
                null,
                InteractionType.NONE,
                null,
                null
        ));
        logicFoundationsModule.addSubTopic(subTopic(
                "Boolean Connectives",
                lesson("logic", "Boolean Connectives"),
                1,
                null,
                null,
                InteractionType.NONE,
                null,
                null
        ));
        logicFoundationsModule.addSubTopic(subTopic(
                "Explore a Compound Circuit",
                lesson("logic", "Explore a Compound Circuit"),
                2,
                null,
                null,
                InteractionType.LOGIC_FLOW,
                "Toggle the inputs and explain which branch makes the compound expression true.",
                validate(interactiveConfigService, InteractionType.LOGIC_FLOW, """
                        {
                          "type": "LOGIC_FLOW",
                          "kind": "CIRCUIT",
                          "mode": "VISUALIZATION",
                          "title": "Explore (P AND Q) OR NOT R",
                          "expression": "(P & Q) | !R",
                          "variables": ["P", "Q", "R"],
                          "goal": "EXPLORE"
                        }
                        """)
        ));
        logicFoundationsModule.addSubTopic(subTopic(
                "Make the Compound Circuit True",
                lesson("logic", "Make the Compound Circuit True"),
                3,
                null,
                null,
                InteractionType.LOGIC_FLOW,
                "Set P, Q, and R so (P AND Q) OR NOT R evaluates to true.",
                validate(interactiveConfigService, InteractionType.LOGIC_FLOW, """
                        {
                          "type": "LOGIC_FLOW",
                          "kind": "CIRCUIT",
                          "mode": "PRACTICE",
                          "title": "Make the compound circuit true",
                          "prompt": "Set P, Q, and R so (P AND Q) OR NOT R evaluates to true.",
                          "expression": "(P & Q) | !R",
                          "variables": ["P", "Q", "R"],
                          "goal": "TRUE",
                          "feedback": {
                            "success": "Correct. At least one branch is true, so the OR gate produces true.",
                            "failure": "Not yet. Make P and Q both true, or make R false so NOT R becomes true."
                          }
                        }
                        """)
        ));
        logicFoundationsModule.addSubTopic(subTopic(
                "Truth Table Checkpoint",
                lesson("logic", "Truth Table Checkpoint"),
                4,
                null,
                null,
                InteractionType.QUIZ,
                "Choose the row in which (P AND Q) OR NOT R is false.",
                validate(interactiveConfigService, InteractionType.QUIZ, """
                        {
                          "type": "QUIZ",
                          "mode": "PRACTICE",
                          "title": "Truth table checkpoint",
                          "prompt": "Choose the row in which (P AND Q) OR NOT R is false.",
                          "question": "Which assignment makes (P ∧ Q) ∨ ¬R false?",
                          "options": [
                            { "id": "a", "label": "P = T, Q = T, R = T", "correct": false },
                            { "id": "b", "label": "P = F, Q = T, R = T", "correct": true },
                            { "id": "c", "label": "P = F, Q = F, R = F", "correct": false },
                            { "id": "d", "label": "P = T, Q = F, R = F", "correct": false }
                          ],
                          "successCondition": { "kind": "QUIZ_CORRECT_OPTION", "correctOptionId": "b" },
                          "feedback": {
                            "success": "Correct. P AND Q is false and NOT R is also false, so the final OR is false.",
                            "failure": "Not yet. An OR is false only when both of its branches are false."
                          }
                        }
                        """)
        ));

        CourseModule conditionalLogicModule = new CourseModule(
                "2. Conditional Reasoning in Programs",
                "Connect implication to preconditions, postconditions, guard clauses, and counterexamples in software behavior.",
                1,
                ContentDepth.HIGH
        );
        conditionalLogicModule.addSubTopic(subTopic(
                "Implication and Program Contracts",
                lesson("logic", "Implication and Program Contracts"),
                0,
                null,
                null,
                InteractionType.NONE,
                null,
                null
        ));
        conditionalLogicModule.addSubTopic(subTopic(
                "Explore an Implication",
                lesson("logic", "Explore an Implication"),
                1,
                null,
                null,
                InteractionType.LOGIC_FLOW,
                "Explore all four assignments for P implies Q and find the counterexample.",
                validate(interactiveConfigService, InteractionType.LOGIC_FLOW, """
                        {
                          "type": "LOGIC_FLOW",
                          "kind": "CIRCUIT",
                          "mode": "VISUALIZATION",
                          "title": "Explore an implication",
                          "expression": "P -> Q",
                          "variables": ["P", "Q"],
                          "goal": "EXPLORE"
                        }
                        """)
        ));
        conditionalLogicModule.addSubTopic(subTopic(
                "Find the Contract Violation",
                lesson("logic", "Find the Contract Violation"),
                2,
                null,
                null,
                InteractionType.LOGIC_FLOW,
                "Set the inputs to produce the single counterexample that makes P implies Q false.",
                validate(interactiveConfigService, InteractionType.LOGIC_FLOW, """
                        {
                          "type": "LOGIC_FLOW",
                          "kind": "CIRCUIT",
                          "mode": "PRACTICE",
                          "title": "Find the contract violation",
                          "prompt": "Set P and Q to make P implies Q false.",
                          "expression": "P -> Q",
                          "variables": ["P", "Q"],
                          "goal": "FALSE",
                          "feedback": {
                            "success": "Correct. P is true while Q is false, so the contract is violated.",
                            "failure": "Not yet. An implication is false only when its premise is true and its conclusion is false."
                          }
                        }
                        """)
        ));
        conditionalLogicModule.addSubTopic(subTopic(
                "Guard-Clause Checkpoint",
                lesson("logic", "Guard-Clause Checkpoint"),
                3,
                null,
                null,
                InteractionType.QUIZ,
                "Choose the test case that disproves the stated program requirement.",
                validate(interactiveConfigService, InteractionType.QUIZ, """
                        {
                          "type": "QUIZ",
                          "mode": "PRACTICE",
                          "title": "Guard-clause checkpoint",
                          "prompt": "Choose the test case that violates the requirement.",
                          "question": "Requirement: If a user is an admin (A), they may delete the record (D). Which result violates A → D?",
                          "options": [
                            { "id": "a", "label": "A = T and D = T", "correct": false },
                            { "id": "b", "label": "A = T and D = F", "correct": true },
                            { "id": "c", "label": "A = F and D = T", "correct": false },
                            { "id": "d", "label": "A = F and D = F", "correct": false }
                          ],
                          "successCondition": { "kind": "QUIZ_CORRECT_OPTION", "correctOptionId": "b" },
                          "feedback": {
                            "success": "Correct. The required permission is missing even though the premise is true.",
                            "failure": "Not yet. Look for a true premise followed by a false conclusion."
                          }
                        }
                        """)
        ));

        CourseModule equivalenceModule = new CourseModule(
                "3. Logical Equivalence and Simplification",
                "Use equivalence laws to rewrite conditions without changing their truth values, then verify multi-step simplifications.",
                2,
                ContentDepth.HIGH
        );
        equivalenceModule.addSubTopic(subTopic(
                "Why Equivalent Expressions Matter",
                lesson("logic", "Why Equivalent Expressions Matter"),
                0,
                null,
                null,
                InteractionType.NONE,
                null,
                null
        ));
        equivalenceModule.addSubTopic(subTopic(
                "Explore a Negated Conjunction",
                lesson("logic", "Explore a Negated Conjunction"),
                1,
                null,
                null,
                InteractionType.LOGIC_FLOW,
                "Toggle P and Q to inspect the truth pattern of NOT (P AND Q).",
                validate(interactiveConfigService, InteractionType.LOGIC_FLOW, """
                        {
                          "type": "LOGIC_FLOW",
                          "kind": "CIRCUIT",
                          "mode": "VISUALIZATION",
                          "title": "Explore NOT (P AND Q)",
                          "expression": "!(P & Q)",
                          "variables": ["P", "Q"],
                          "goal": "EXPLORE"
                        }
                        """)
        ));
        equivalenceModule.addSubTopic(subTopic(
                "Apply De Morgan's Law",
                lesson("logic", "Apply De Morgan's Law"),
                2,
                null,
                null,
                InteractionType.LOGIC_FLOW,
                "Rewrite NOT (P AND Q) using De Morgan's law.",
                validate(interactiveConfigService, InteractionType.LOGIC_FLOW, """
                        {
                          "type": "LOGIC_FLOW",
                          "kind": "SIMPLIFY",
                          "mode": "PRACTICE",
                          "title": "Apply De Morgan's law",
                          "prompt": "Rewrite NOT (P AND Q) using De Morgan's law.",
                          "start": "!(P & Q)",
                          "target": "!P | !Q",
                          "allowedLaws": ["DE_MORGAN"],
                          "steps": [
                            { "law": "DE_MORGAN", "lawId": "DE_MORGAN", "from": "!(P & Q)", "to": "!P | !Q", "result": "!P | !Q", "note": "Negate each variable and replace AND with OR." }
                          ],
                          "feedback": {
                            "success": "Correct. Both expressions have the same truth value in every row.",
                            "failure": "Not yet. Negate both variables and change AND to OR."
                          }
                        }
                        """)
        ));
        equivalenceModule.addSubTopic(subTopic(
                "Rewrite an Implication",
                lesson("logic", "Rewrite an Implication"),
                3,
                null,
                null,
                InteractionType.LOGIC_FLOW,
                "Rewrite P implies Q using only NOT and OR.",
                validate(interactiveConfigService, InteractionType.LOGIC_FLOW, """
                        {
                          "type": "LOGIC_FLOW",
                          "kind": "SIMPLIFY",
                          "mode": "PRACTICE",
                          "title": "Rewrite an implication",
                          "prompt": "Rewrite P implies Q using only NOT and OR.",
                          "start": "P -> Q",
                          "target": "!P | Q",
                          "allowedLaws": ["IMPLICATION"],
                          "steps": [
                            { "law": "IMPLICATION", "lawId": "IMPLICATION", "from": "P -> Q", "to": "!P | Q", "result": "!P | Q", "note": "An implication is equivalent to NOT P OR Q." }
                          ],
                          "feedback": {
                            "success": "Correct. NOT P OR Q is false in exactly the same row as P implies Q.",
                            "failure": "Not yet. Replace P implies Q with NOT P OR Q."
                          }
                        }
                        """)
        ));
        equivalenceModule.addSubTopic(subTopic(
                "Multi-Law Simplification",
                lesson("logic", "Multi-Law Simplification"),
                4,
                null,
                null,
                InteractionType.LOGIC_FLOW,
                "Simplify the expression to one variable using distribution, complement, and identity.",
                validate(interactiveConfigService, InteractionType.LOGIC_FLOW, """
                        {
                          "type": "LOGIC_FLOW",
                          "kind": "SIMPLIFY",
                          "mode": "PRACTICE",
                          "title": "Multi-law simplification",
                          "prompt": "Simplify the expression to one variable using distribution, complement, and identity.",
                          "start": "(P & !Q) | (P & Q)",
                          "target": "P",
                          "allowedLaws": ["DISTRIBUTIVE", "COMPLEMENT", "IDENTITY"],
                          "steps": [
                            { "law": "DISTRIBUTIVE", "lawId": "DISTRIBUTIVE", "from": "(P & !Q) | (P & Q)", "to": "P & (!Q | Q)", "result": "P & (!Q | Q)", "note": "Factor P out of both terms." },
                            { "law": "COMPLEMENT", "lawId": "COMPLEMENT", "from": "P & (!Q | Q)", "to": "P & T", "result": "P & T", "note": "NOT Q OR Q is always true." },
                            { "law": "IDENTITY", "lawId": "IDENTITY", "from": "P & T", "to": "P", "result": "P", "note": "P AND true equals P." }
                          ],
                          "feedback": {
                            "success": "Correct. The simplified condition P preserves the full truth table.",
                            "failure": "Not yet. Factor P first, then apply complement and identity."
                          }
                        }
                        """)
        ));
        publishSeedCourse(
                courseRepository,
                logicCourse,
                logicFoundationsModule,
                conditionalLogicModule,
                equivalenceModule
        );
    }

    private static final String SEED_CONTENT_MARKER = "Learn how matrices store and transform data";

    private boolean referenceCoursesAlreadySeeded(CourseRepository courseRepository) {
        boolean allPresent = List.of("Matrix", "Probability", "Set", "Vector", "Logic").stream()
                .allMatch(title -> !courseRepository.findByTitleOrderByCreatedAtAsc(title).isEmpty());
        if (!allPresent) {
            return false;
        }
        // Re-seed once when the database still holds earlier seed content: the thinner text-only
        // version, or a version that predates lesson images.
        Long imageLessons = jdbc.queryForObject(
                "select count(*) from sub_topic where content like '%data-seed-image%' or content like '%data-asset-id%'",
                Long.class);
        boolean hasImages = imageLessons != null && imageLessons > 0;
        return hasImages && courseRepository.findByTitleOrderByCreatedAtAsc("Matrix").stream()
                .anyMatch(course -> course.getDescription() != null
                        && course.getDescription().startsWith(SEED_CONTENT_MARKER));
    }

    private static final java.util.regex.Pattern SEED_IMAGE =
            java.util.regex.Pattern.compile("<img data-seed-image=\"([^\"]+)\"");

    /** Uploads the seed images referenced by lesson content and swaps each placeholder for a real asset id. */
    private void attachSeedImages() {
        software.amazon.awssdk.services.s3.S3Client client = s3Client.getIfAvailable();
        if (client == null || storageProperties == null || !storageProperties.isConfigured()) {
            log.warn("Storage is not configured; seed lesson images were not attached");
            return;
        }
        try {
            transactionTemplate.executeWithoutResult(status -> {
                for (SubTopic subTopic : subTopicRepository.findAll()) {
                    String content = subTopic.getContent();
                    if (content == null || !content.contains("data-seed-image")) {
                        continue;
                    }
                    java.util.regex.Matcher matcher = SEED_IMAGE.matcher(content);
                    StringBuilder result = new StringBuilder();
                    while (matcher.find()) {
                        String fileName = matcher.group(1);
                        byte[] bytes = readSeedImage(fileName);
                        Long courseId = subTopic.getModule().getCourse().getId();
                        String storagePath = "courses/" + courseId + "/subtopics/" + subTopic.getId()
                                + "/images/" + java.util.UUID.randomUUID() + "-" + fileName;
                        client.putObject(
                                software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                                        .bucket(storageProperties.bucket().trim())
                                        .key(storagePath)
                                        .contentType("image/png")
                                        .build(),
                                software.amazon.awssdk.core.sync.RequestBody.fromBytes(bytes));
                        com.example.demo.course.entity.SubTopicAsset asset =
                                new com.example.demo.course.entity.SubTopicAsset(
                                        storagePath, fileName, "image/png", (long) bytes.length, null);
                        subTopic.addAsset(asset);
                        asset = subTopicAssetRepository.save(asset);
                        matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(
                                "<img data-asset-id=\"" + asset.getId() + "\""));
                    }
                    matcher.appendTail(result);
                    log.info("Attached images to sub-topic {}", subTopic.getId());
                    subTopic.updateDetails(subTopic.getTitle(), result.toString(), subTopic.getSortOrder(),
                            subTopic.getPageStart(), subTopic.getPageEnd());
                }
            });
        } catch (RuntimeException ex) {
            log.warn("Seed lesson images could not be attached (is storage reachable?): {}", ex.getMessage());
        }
    }

    private byte[] readSeedImage(String fileName) {
        String resource = "/seed/images/" + fileName;
        try (java.io.InputStream in = DevSeedConfig.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing seed image resource: " + resource);
            }
            return in.readAllBytes();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Unable to read seed image resource: " + resource, ex);
        }
    }

    private String lesson(String course, String title) {
        String slug = title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        String resource = "/seed/lessons/" + course + "/" + slug + ".html";
        try (java.io.InputStream in = DevSeedConfig.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing seed lesson resource: " + resource);
            }
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).strip();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Unable to read seed lesson resource: " + resource, ex);
        }
    }

    private void clearAssistantHistory(Long courseId) {
        jdbc.update("delete from assistant_message where conversation_id in (select id from assistant_conversation where course_id = ?)", courseId);
        jdbc.update("delete from assistant_conversation where course_id = ?", courseId);
        jdbc.update("delete from course_review_comment where review_id in (select id from course_review where course_id = ?)", courseId);
    }

    private Course seedCourse(
            CourseRepository courseRepository,
            LearnerInteractiveProgressRepository progressRepository,
            String title,
            String description,
            User admin
    ) {
        Course course = courseRepository.findByTitleOrderByCreatedAtAsc(title).stream()
                .findFirst()
                .orElseGet(() -> new Course(title, description, admin));
        if (course.getId() != null) {
            progressRepository.deleteByCourseId(course.getId());
            clearAssistantHistory(course.getId());
        }
        course.updateDetails(title, description);
        course.clearModules();
        return course;
    }

    private void publishSeedCourse(CourseRepository courseRepository, Course course, CourseModule... modules) {
        for (CourseModule module : modules) {
            course.addModule(module);
        }
        course.publish();
        courseRepository.save(course);
    }

    private void removeLegacyAggregateSeedCourse(
            CourseRepository courseRepository,
            LearnerInteractiveProgressRepository progressRepository
    ) {
        courseRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(course -> "Interactive Math Foundations".equals(course.getTitle()))
                .forEach(course -> {
                    progressRepository.deleteByCourseId(course.getId());
                    courseRepository.delete(course);
                });
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
