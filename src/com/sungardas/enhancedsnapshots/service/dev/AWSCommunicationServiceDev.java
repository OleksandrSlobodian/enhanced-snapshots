package com.sungardas.enhancedsnapshots.service.dev;

import com.amazonaws.services.ec2.model.*;
import com.sungardas.enhancedsnapshots.service.impl.AWSCommunicationServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@Profile("dev")
public class AWSCommunicationServiceDev extends AWSCommunicationServiceImpl {

    @Value("${enhancedsnapshots.dev.hostname:localhost}")
    private String hostname;

}
