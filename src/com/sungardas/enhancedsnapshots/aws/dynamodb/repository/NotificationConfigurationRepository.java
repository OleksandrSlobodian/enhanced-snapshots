package com.sungardas.enhancedsnapshots.aws.dynamodb.repository;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.NotificationConfigurationEntry;
import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;

@EnableScan
public interface NotificationConfigurationRepository extends CrudRepository<NotificationConfigurationEntry, String> {
}
