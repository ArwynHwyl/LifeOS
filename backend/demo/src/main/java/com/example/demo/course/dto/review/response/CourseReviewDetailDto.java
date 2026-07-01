package com.example.demo.course.dto.review.response;

import java.util.List;

import com.example.demo.course.dto.management.response.CourseDetailDto;

public record CourseReviewDetailDto(
        CourseDetailDto course,
        List<CourseReviewDto> reviews
) {
}
