package com.example.demo.course.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.course.entity.SubTopicAsset;

public interface SubTopicAssetRepository extends JpaRepository<SubTopicAsset, Long> {

    List<SubTopicAsset> findBySubTopicIdOrderByCreatedAtAscIdAsc(Long subTopicId);
}
