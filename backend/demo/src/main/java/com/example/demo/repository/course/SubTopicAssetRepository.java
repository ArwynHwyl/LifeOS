package com.example.demo.repository.course;

import com.example.demo.entity.course.SubTopicAsset;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubTopicAssetRepository extends JpaRepository<SubTopicAsset, Long> {

    List<SubTopicAsset> findBySubTopicIdOrderByCreatedAtAscIdAsc(Long subTopicId);
}
