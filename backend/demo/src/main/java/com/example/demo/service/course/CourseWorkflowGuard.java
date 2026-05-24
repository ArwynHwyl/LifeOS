package com.example.demo.service.course;

import com.example.demo.entity.course.Course;
import com.example.demo.entity.course.CourseStatus;
import com.example.demo.service.exception.InvalidWorkflowStateException;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
class CourseWorkflowGuard {

    private static final Set<CourseStatus> ADMIN_EDITABLE_STATUSES = Set.of(
            CourseStatus.DRAFT,
            CourseStatus.NEED_REVISION
    );

    private static final Set<CourseStatus> ADMIN_REOPENABLE_STATUSES = Set.of(
            CourseStatus.APPROVED,
            CourseStatus.PUBLISHED
    );

    void requireAdminEditable(Course course) {
        if (!ADMIN_EDITABLE_STATUSES.contains(course.getStatus())) {
            throw new InvalidWorkflowStateException(
                    "Course " + course.getId() + " cannot be edited while status is " + course.getStatus()
            );
        }
    }

    void prepareForAdminEdit(Course course) {
        if (ADMIN_EDITABLE_STATUSES.contains(course.getStatus())) {
            return;
        }
        if (ADMIN_REOPENABLE_STATUSES.contains(course.getStatus())) {
            course.reopenDraftForAdminEdit();
            return;
        }
        throw new InvalidWorkflowStateException(
                "Course " + course.getId() + " cannot be edited while status is " + course.getStatus()
        );
    }

    void requireStatus(Course course, CourseStatus expectedStatus, String action) {
        if (course.getStatus() != expectedStatus) {
            throw new InvalidWorkflowStateException(
                    action + " requires course status " + expectedStatus + " but was " + course.getStatus()
            );
        }
    }
}
