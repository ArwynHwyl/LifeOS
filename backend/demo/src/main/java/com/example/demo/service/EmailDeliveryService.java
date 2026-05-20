package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailDeliveryService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String frontendUrl;

    public EmailDeliveryService(
            JavaMailSender mailSender,
            @Value("${app.mail.from:no-reply@lifeos.local}") String fromAddress,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.frontendUrl = frontendUrl;
    }

    public void sendEmailConfirmation(String to, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;
        send(to, "Confirm your LifeOS email", "Confirm your email by opening this link:\n\n" + link);
    }

    public void sendPasswordReset(String to, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        send(to, "Reset your LifeOS password", "Reset your password by opening this link:\n\n" + link);
    }

    private void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
