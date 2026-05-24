package com.example.demo.repository.course;

import com.example.demo.entity.course.SubTopic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubTopicRepository extends JpaRepository<SubTopic, Long> {

    List<SubTopic> findByModuleIdOrderBySortOrderAscIdAsc(Long moduleId);
}
