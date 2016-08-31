package com.sungardas.init;

import com.sungardas.enhancedsnapshots.dto.InitConfigurationDto;
import com.sungardas.enhancedsnapshots.dto.converter.BucketNameValidationDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


interface InitConfigurationService {

    InitConfigurationDto getInitConfigurationDto();

    boolean systemIsConfigured();

    boolean checkDefaultUser(String login, String password);

    void configureSystem(ConfigDto configDto);

    BucketNameValidationDTO validateBucketName(String bucketName);

    void saveSamlSPCertificate(MultipartFile file) throws IOException;

    void saveIdpMetadata(MultipartFile file) throws IOException;

    /**
     * Check if it is possible to restore DB from S3 bucket
     *
     * @param bucketName bucket name
     * @return information about DB
     */
    InitConfigurationDto.DB containsMetadata(String bucketName);
}
