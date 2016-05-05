package com.sungardas.enhancedsnapshots.components;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class RetryInterceptor implements MethodInterceptor {

    @Autowired
    private AmazonS3 amazonS3;

    @Value("${enhancedsnapshots.amazon.retry.count}")
    private int count;

    @Value("${enhancedsnapshots.amazon.retry.sleep}")
    private int sleep;

    @Value("${amazon.s3.default.region}")
    private String defaultS3Region;

    private static final Logger LOG = LogManager.getLogger(RetryInterceptor.class);

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Throwable throwable = null;
        for (int i = 0; i < count; i++) {
            try {
                return methodInvocation.proceed();
            } catch (AmazonS3Exception e) {
                amazonS3.setRegion(com.amazonaws.regions.Region.getRegion(Regions.fromName(defaultS3Region)));
                methodInvocation.proceed();
            } catch (AmazonServiceException e) {
                if (e.getErrorType() == AmazonServiceException.ErrorType.Client) {
                    throw e;
                }
                LOG.debug("Exception while method invocation: ", methodInvocation.getMethod());
                LOG.debug("Amazon service exception, try: " + i, e);
                throwable = e;
                Thread.sleep(sleep);
            }
        }
        throw throwable;
    }
}
