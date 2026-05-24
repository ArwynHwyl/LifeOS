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
                        "Draft explanation of the key concepts for this module, tailored to the requested content depth."
                ),
                new GeneratedSubTopicDraft(
                        "Applied example",
                        "Draft example connecting the module topic to computational programming or AI development."
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
                                        "Ask learners to classify predicates and choose correct quantifiers for code assertions."
                                ),
                                new GeneratedSubTopicDraft(
                                        "Functions and relations",
                                        "Connect mathematical functions, relations, domains, and codomains to APIs and data transformations.",
                                        InteractionType.GRAPH_2D,
                                        "Plot simple mappings and highlight domain/range changes as learners edit examples."
                                )
                        )
                ),
                new GeneratedCourseModuleDraft(
                        "Linear Algebra for Data and Graphics",
                        "Vectors, matrices, transformations, and spaces with practical links to machine learning and rendering.",
                        InteractionType.THREE_JS,
                        "Show a vector and matrix transform scene where learners rotate, scale, and compose transformations.",
                        List.of(
                                new GeneratedSubTopicDraft(
                                        "Vectors and matrices",
                                        "Explain vector operations and matrix multiplication as structured transformations.",
                                        InteractionType.FORMULA_EXPLORER,
                                        "Step through each multiplication cell and show how row-column products are computed."
                                ),
                                new GeneratedSubTopicDraft(
                                        "Transformations in software",
                                        "Apply linear transforms to coordinates, graphics, embeddings, and feature spaces.",
                                        InteractionType.THREE_JS,
                                        "Let learners manipulate a 3D object and inspect the transformation matrix."
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
                                        InteractionType.GRAPH_2D,
                                        "Let learners add nodes and edges, then view adjacency lists and traversal order."
                                ),
                                new GeneratedSubTopicDraft(
                                        "Recurrences and complexity",
                                        "Model recursive algorithms with recurrences and estimate growth with asymptotic notation.",
                                        InteractionType.FORMULA_EXPLORER,
                                        "Compare recurrence expansions and plot approximate growth curves."
                                )
                        )
                )
        );
        return new GeneratedCourseOutlineDraft(prompt, modules);
    }
}
