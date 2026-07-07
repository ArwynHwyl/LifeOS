package com.example.demo.course.service.management;

import com.example.demo.course.entity.Course;
import com.example.demo.course.entity.CourseStatus;
import com.example.demo.shared.exception.InvalidWorkflowStateException;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CourseWorkflowGuard {

    private static final Set<CourseStatus> ADMIN_EDITABLE_STATUSES = Set.of(
            CourseStatus.DRAFT,
            CourseStatus.NEED_REVISION
    );

    private static final Set<CourseStatus> ADMIN_REOPENABLE_STATUSES = Set.of(
            CourseStatus.PUBLISHED
    );

    public void requireAdminEditable(Course course) {
        if (!ADMIN_EDITABLE_STATUSES.contains(course.getStatus())) {
            throw new InvalidWorkflowStateException(
                    "Course " + course.getId() + " cannot be edited while status is " + course.getStatus()
            );
        }
    }

    public void prepareForAdminEdit(Course course) {
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

    public void requireStatus(Course course, CourseStatus expectedStatus, String action) {
        if (course.getStatus() != expectedStatus) {
            throw new InvalidWorkflowStateException(
                    action + " requires course status " + expectedStatus + " but was " + course.getStatus()
            );
        }
    }
}
