package com.example.demo.course.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.course.entity.SubTopic;

public interface SubTopicRepository extends JpaRepository<SubTopic, Long> {

    List<SubTopic> findByModuleIdOrderBySortOrderAscIdAsc(Long moduleId);
}
