package com.example.demo.service.course;

import com.example.demo.dto.course.PublishedCourseDetailDto;
import com.example.demo.dto.course.PublishedCourseSummaryDto;
import com.example.demo.entity.course.Course;
import com.example.demo.entity.course.CourseStatus;
import com.example.demo.repository.course.CourseRepository;
import com.example.demo.service.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearnerCourseService {

    private final CourseRepository courseRepository;
    private final CourseDtoMapper mapper;
    private final CourseInputValidator validator;

    public LearnerCourseService(CourseRepository courseRepository, CourseDtoMapper mapper, CourseInputValidator validator) {
        this.courseRepository = courseRepository;
        this.mapper = mapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public List<PublishedCourseSummaryDto> listPublishedCourses() {
        return mapper.toPublishedSummaryDtos(courseRepository.findByStatusOrderByUpdatedAtDesc(CourseStatus.PUBLISHED));
    }

    @Transactional(readOnly = true)
    public PublishedCourseDetailDto getPublishedCourse(Long courseId) {
        Course course = courseRepository.findById(validator.requiredId(courseId, "courseId"))
                .filter(foundCourse -> foundCourse.getStatus() == CourseStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published course not found: " + courseId));
        return mapper.toPublishedDetailDto(course);
    }
}
