package com.sungardas.enhancedsnapshots.aws.dynamodb.repository;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.NodeEntry;
import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.socialsignin.spring.data.dynamodb.repository.EnableScanCount;
import org.springframework.data.repository.CrudRepository;

import java.util.List;


@EnableScan
@EnableScanCount
public interface NodeRepository extends CrudRepository<NodeEntry, String> {
    List<NodeEntry> findAll();
    List<NodeEntry> findByMaster(boolean master);
}
