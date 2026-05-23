package com.example.demo.service.course;

import com.example.demo.entity.course.CourseModule;
import com.example.demo.entity.course.Course;

public interface AiDraftGenerator {

    GeneratedModuleDraft generateDraft(CourseModule module, String prompt);

    GeneratedCourseOutlineDraft generateCourseOutline(Course course, String prompt);
}
