package com.example.demo.course.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.course.entity.LearnerInteractiveProgress;

public interface LearnerInteractiveProgressRepository extends JpaRepository<LearnerInteractiveProgress, Long> {

    List<LearnerInteractiveProgress> findByUserUserIdAndSubTopicIdIn(UUID userId, Collection<Long> subTopicIds);

    Optional<LearnerInteractiveProgress> findByUserUserIdAndSubTopicId(UUID userId, Long subTopicId);

    @Modifying
    @Transactional
    @Query("delete from LearnerInteractiveProgress progress where progress.course.id = :courseId")
    void deleteByCourseId(Long courseId);
}
