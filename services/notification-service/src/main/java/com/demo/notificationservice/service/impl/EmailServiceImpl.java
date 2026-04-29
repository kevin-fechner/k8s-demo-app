package com.demo.notificationservice.service.impl;

import com.demo.events.order.OrderCreatedEvent;
import com.demo.events.order.OrderStatusChangedEvent;
import com.demo.notificationservice.service.EmailService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final Counter emailsSentCounter;

  @Value("${notification.mail.from}")
  private String fromAddress;

  public EmailServiceImpl(
      JavaMailSender mailSender, TemplateEngine templateEngine, MeterRegistry meterRegistry) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
    this.emailsSentCounter =
        Counter.builder("business.emails.sent")
            .description("Total emails sent")
            .register(meterRegistry);
  }

  @Override
  public void sendOrderConfirmation(OrderCreatedEvent event) {
    Context ctx = new Context();
    ctx.setVariable("customerName", event.customerName());
    ctx.setVariable("orderId", event.orderId());
    ctx.setVariable("items", event.items());
    ctx.setVariable("totalAmount", event.totalAmount());

    sendEmail(
        event.customerEmail(), "Order Confirmation #" + event.orderId(), "order-created", ctx);
  }

  @Override
  public void sendStatusUpdate(OrderStatusChangedEvent event) {
    Context ctx = new Context();
    ctx.setVariable("customerName", event.customerName());
    ctx.setVariable("orderId", event.orderId());
    ctx.setVariable("previousStatus", event.previousStatus());
    ctx.setVariable("newStatus", event.newStatus());

    sendEmail(
        event.customerEmail(),
        "Order #" + event.orderId() + " — Status Update: " + event.newStatus(),
        "order-status-changed",
        ctx);
  }

  private void sendEmail(String to, String subject, String template, Context ctx) {
    try {
      String html = templateEngine.process(template, ctx);
      var message = mailSender.createMimeMessage();
      var helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(fromAddress);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(html, true);
      mailSender.send(message);
      emailsSentCounter.increment();
      log.info("Email sent to={} subject='{}'", to, subject);
    } catch (MessagingException e) {
      log.error("Failed to send email to={}: {}", to, e.getMessage());
    }
  }
}
