package com.indeed.alliances.code.aws.aggregator;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

/**
 * Utility class for sending emails. Note that AWS's Simple Email Service is region-dependent,
 * and is not available in all regions. Both the FROM and TO email addresses must be confirmed
 * via the SES Identity Management console before the emails can be sent.
 *
 *
 * Written by George Ludwig, Solutions Architect, Global Alliances at Indeed
 * June 2018
 */

public class Utils {

    /**
     * Sends a single email
     * @param from
     * @param to
     * @param subject
     * @param message
     */
    public static void sendEmail(String from, String to, String subject, String message) {
        try {
            AmazonSimpleEmailService client =
                    AmazonSimpleEmailServiceClientBuilder.standard().withRegion("us-east-1").build();
            SendEmailRequest request = new SendEmailRequest()
                    .withDestination(
                            new Destination().withToAddresses(to))
                    .withMessage(new Message()
                            .withBody(new Body()
                                    .withText(new Content()
                                            .withCharset("UTF-8").withData(message)))
                            .withSubject(new Content()
                                    .withCharset("UTF-8").withData(subject)))
                    .withSource(from);
            client.sendEmail(request);
            System.out.println("Email sent!");
        } catch (Exception ex) {
            System.out.println("The email was not sent. Error message: "
                    + ex.getMessage());
        }
    }

    /**
     * Sends an email to every address in toList, which is a comma-separated list of email addresses.
     * @param from
     * @param toList
     * @param subject
     * @param message
     */
    public static void sendEmails(String from, String toList, String subject, String message) {
        String[] emails = toList.split(",");
        for(int i =0; i<emails.length; i++) {
            Utils.sendEmail(from, emails[i], subject, message);
        }
    }
}
