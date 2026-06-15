package com.example.demo.service.course;

import com.example.demo.entity.course.Course;
import com.example.demo.entity.course.CourseModule;
import com.example.demo.entity.course.InteractionType;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class MockAiDraftGenerator implements AiDraftGenerator {

    @Override
    public GeneratedModuleDraft generateDraft(CourseModule module, String prompt) {
        List<GeneratedSubTopicDraft> subTopics = List.of(
                new GeneratedSubTopicDraft(
                        module.getTitle() + " overview",
                        "Draft overview generated from the selected source pages. Admin review is required before submission."
                ),
                new GeneratedSubTopicDraft(
                        "Core concepts",
                        "Draft explanation of the key concepts for this module, tailored to the requested content depth.",
                        InteractionType.QUIZ,
                        "Check whether learners can identify the key concept from the generated explanation.",
                        quizConfig("Core concepts check", "Which option best matches the module's core concept?")
                ),
                new GeneratedSubTopicDraft(
                        "Applied example",
                        "Draft example connecting the module topic to computational programming or AI development.",
                        InteractionType.GRAPH_2D,
                        "Show a simple linear relationship learners can inspect before applying the idea in code.",
                        graphConfig("Applied relationship", "2 * x + 1")
                )
        );
        return new GeneratedModuleDraft(prompt, subTopics);
    }

    @Override
    public GeneratedCourseOutlineDraft generateCourseOutline(Course course, String prompt) {
        List<GeneratedCourseModuleDraft> modules = List.of(
                new GeneratedCourseModuleDraft(
                        "Mathematical Foundations for Software Engineering",
                        "Core notation, logic, functions, and proof habits used to reason about software systems.",
                        InteractionType.FORMULA_EXPLORER,
                        "Create an interactive notation explorer that lets learners map symbols to software examples.",
                        List.of(
                                new GeneratedSubTopicDraft(
                                        "Logic and predicates",
                                        "Introduce propositions, predicates, quantifiers, and how they model program invariants.",
                                        InteractionType.QUIZ,
                                        "Ask learners to classify predicates and choose correct quantifiers for code assertions.",
                                        quizConfig("Predicate check", "Which statement is a predicate?")
                                ),
                                new GeneratedSubTopicDraft(
                                        "Functions and relations",
                                        "Connect mathematical functions, relations, domains, and codomains to APIs and data transformations.",
                                        InteractionType.GRAPH_2D,
                                        "Plot simple mappings and highlight domain/range changes as learners edit examples.",
                                        graphConfig("Function mapping", "2 * x + 1")
                                )
                        )
                ),
                new GeneratedCourseModuleDraft(
                        "Linear Algebra for Data and Graphics",
                        "Vectors, matrices, transformations, and spaces with practical links to machine learning and rendering.",
                        InteractionType.VISUAL_LAYER,
                        "Show vector and matrix transformations as connected input, transform, and output regions.",
                        List.of(
                                new GeneratedSubTopicDraft(
                                        "Vectors and matrices",
                                        "Explain vector operations and matrix multiplication as structured transformations.",
                                        InteractionType.FORMULA_EXPLORER,
                                        "Step through each multiplication cell and show how row-column products are computed.",
                                        formulaConfig("Dot product cell", "a * b + c * d")
                                ),
                                new GeneratedSubTopicDraft(
                                        "Transformations in software",
                                        "Apply linear transforms to coordinates, graphics, embeddings, and feature spaces.",
                                        InteractionType.VISUAL_LAYER,
                                        "Let learners highlight input, transform, and output regions in a software pipeline.",
                                        visualLayerConfig("Transformation pipeline")
                                )
                        )
                ),
                new GeneratedCourseModuleDraft(
                        "Discrete Structures and Algorithms",
                        "Sets, graphs, recurrence relations, and complexity tools used in everyday algorithm design.",
                        InteractionType.GRAPH_2D,
                        "Create a graph visualizer for traversals, shortest paths, and adjacency representations.",
                        List.of(
                                new GeneratedSubTopicDraft(
                                        "Sets, graphs, and trees",
                                        "Use discrete structures to represent permissions, dependencies, networks, and parse trees.",
                                        InteractionType.VISUAL_LAYER,
                                        "Let learners compare overlapping permission sets and inspect computed regions.",
                                        visualLayerConfig("Permission sets")
                                ),
                                new GeneratedSubTopicDraft(
                                        "Recurrences and complexity",
                                        "Model recursive algorithms with recurrences and estimate growth with asymptotic notation.",
                                        InteractionType.FORMULA_EXPLORER,
                                        "Compare recurrence expansions and plot approximate growth curves.",
                                        formulaConfig("Recurrence growth", "a * n + b")
                                )
                        )
                )
        );
        return new GeneratedCourseOutlineDraft(prompt, modules);
    }

    private String quizConfig(String title, String question) {
        return """
                {"type":"QUIZ","title":"%s","question":"%s","options":[{"id":"a","label":"A precise mathematical statement with variables.","correct":true},{"id":"b","label":"A paragraph with no testable condition.","correct":false}]}
                """.formatted(title, question).trim();
    }

    private String graphConfig(String title, String expression) {
        return """
                {"type":"GRAPH_2D","title":"%s","expression":"%s","xMin":-5,"xMax":5,"yMin":-10,"yMax":10,"sampleCount":100}
                """.formatted(title, expression).trim();
    }

    private String formulaConfig(String title, String formula) {
        return """
                {"type":"FORMULA_EXPLORER","title":"%s","formula":"%s","variables":[{"name":"a","label":"A","min":0,"max":10,"step":1,"initial":2},{"name":"b","label":"B","min":0,"max":10,"step":1,"initial":3},{"name":"c","label":"C","min":0,"max":10,"step":1,"initial":4},{"name":"d","label":"D","min":0,"max":10,"step":1,"initial":5},{"name":"n","label":"N","min":1,"max":20,"step":1,"initial":4}],"precision":2}
                """.formatted(title, formula).trim();
    }

    private String visualLayerConfig(String title) {
        return """
                {"type":"VISUAL_LAYER","title":"%s","canvas":{"width":900,"height":520,"backgroundText":"Generated visual"},"zones":[{"id":"zone_a","label":"Set A","shape":"circle","x":260,"y":130,"width":260,"height":260,"color":"#ffd333","highlightColor":"#ff8f1f","highlightOpacity":0.8},{"id":"zone_b","label":"Set B","shape":"circle","x":380,"y":130,"width":260,"height":260,"color":"#8fb3ff","highlightColor":"#3f7cff","highlightOpacity":0.8}],"elements":[{"id":"choice_overlap","label":"Overlap","kind":"button","x":40,"y":40,"width":150,"height":48}],"interactions":[{"triggerId":"choice_overlap","effect":"HIGHLIGHT_ZONE","targetZoneId":"zone_b","feedback":"The overlap is computed from the totals and intersection."}],"overlap":{"enabled":true,"sourceZoneIds":["zone_a","zone_b"],"inputs":[{"id":"A_ONLY","label":"A","zoneIds":["zone_a"],"value":11,"kind":"total"},{"id":"B_ONLY","label":"B","zoneIds":["zone_b"],"value":9,"kind":"total"},{"id":"A_AND_B","label":"A intersect B","zoneIds":["zone_a","zone_b"],"value":3,"kind":"intersection"}]}}
                """.formatted(title).trim();
    }
}
