package com.careconnect.notifications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

@Service
public class SesService {

    private final SesClient sesClient;
    private final String fromAddress;

    public SesService(@Value("${aws.region:us-east-1}") String awsRegion,
                      @Value("${aws.ses.from:no-reply@localhost}") String fromAddress) {
        Region region = Region.of(awsRegion);
        this.sesClient = SesClient.builder().region(region).build();
        this.fromAddress = fromAddress;
    }

    public String sendEmail(String toAddress, String subject, String htmlBody, String textBody) {
        Destination destination = Destination.builder().toAddresses(toAddress).build();

        Content subj = Content.builder().data(subject).build();
        Body body = Body.builder()
                .html(Content.builder().data(htmlBody == null ? "" : htmlBody).build())
                .text(Content.builder().data(textBody == null ? "" : textBody).build())
                .build();
        Message message = Message.builder().subject(subj).body(body).build();

        SendEmailRequest request = SendEmailRequest.builder()
                .destination(destination)
                .message(message)
                .source(fromAddress)
                .build();

        SendEmailResponse resp = sesClient.sendEmail(request);
        return resp.messageId();
    }
}
