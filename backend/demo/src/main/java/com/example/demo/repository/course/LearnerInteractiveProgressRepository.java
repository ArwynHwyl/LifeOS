package com.example.demo.repository.course;

import com.example.demo.entity.course.LearnerInteractiveProgress;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearnerInteractiveProgressRepository extends JpaRepository<LearnerInteractiveProgress, Long> {

    List<LearnerInteractiveProgress> findByUserUserIdAndSubTopicIdIn(UUID userId, Collection<Long> subTopicIds);

    Optional<LearnerInteractiveProgress> findByUserUserIdAndSubTopicId(UUID userId, Long subTopicId);
}
