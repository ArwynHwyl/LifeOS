package com.example.demo.course.service.generation;

import com.example.demo.course.service.generation.model.GeneratedCourseOutlineDraft;
import com.example.demo.course.service.generation.model.GeneratedModuleDraft;

import com.example.demo.course.entity.Course;
import com.example.demo.course.entity.CourseModule;

public interface AiDraftGenerator {

    GeneratedModuleDraft generateDraft(CourseModule module, String prompt);

    GeneratedCourseOutlineDraft generateCourseOutline(Course course, String prompt);
}
