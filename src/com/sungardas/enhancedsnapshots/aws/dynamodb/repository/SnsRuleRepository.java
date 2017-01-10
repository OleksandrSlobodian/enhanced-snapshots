package com.sungardas.enhancedsnapshots.aws.dynamodb.repository;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.SnsRuleEntry;
import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

@EnableScan
public interface SnsRuleRepository extends CrudRepository<SnsRuleEntry, String> {
    List<SnsRuleEntry> findAll();

    List<SnsRuleEntry> findByOperationAndStatusAndVolumeId(String operation, String status, String volumeId);
    List<SnsRuleEntry> findByOperationAndStatusAndVolumeIdIsNull(String operation, String status);

}
