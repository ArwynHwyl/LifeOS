package com.example.demo.dto.course;

import java.util.List;

public record CourseReviewDetailDto(
        CourseDetailDto course,
        List<CourseReviewDto> reviews
) {
}
