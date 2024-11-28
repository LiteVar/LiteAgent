package com.litevar.agent.core.util;

import jakarta.annotation.Resource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author uncle
 * @since 2024/8/2 14:25
 */
@Component
public class MailSendUtil {

    @Resource
    private JavaMailSenderImpl mailSender;

    @Async
    public void send(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        message.setFrom(mailSender.getUsername());
        mailSender.send(message);
    }
}
