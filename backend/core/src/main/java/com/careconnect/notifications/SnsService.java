package com.careconnect.notifications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

@Service
public class SnsService {

    private final SnsClient snsClient;

    public SnsService(@Value("${aws.region:us-east-1}") String awsRegion) {
        Region region = Region.of(awsRegion);
        this.snsClient = SnsClient.builder().region(region).build();
    }

    // Package-visible constructor for tests or alternate client injection
    SnsService(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    public String publishToTopic(String topicArn, String subject, String message) {
        PublishRequest req = PublishRequest.builder()
                .topicArn(topicArn)
                .subject(subject)
                .message(message)
                .build();
        PublishResponse resp = snsClient.publish(req);
        return resp.messageId();
    }

    public String publishSms(String phoneNumber, String message) {
        PublishRequest req = PublishRequest.builder()
                .phoneNumber(phoneNumber)
                .message(message)
                .build();
        PublishResponse resp = snsClient.publish(req);
        return resp.messageId();
    }
}
