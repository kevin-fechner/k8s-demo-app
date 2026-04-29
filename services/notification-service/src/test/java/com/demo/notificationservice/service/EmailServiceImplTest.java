package com.demo.notificationservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.demo.events.order.OrderCreatedEvent;
import com.demo.events.order.OrderStatusChangedEvent;
import com.demo.notificationservice.service.impl.EmailServiceImpl;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

  @Mock private JavaMailSender mailSender;
  @Mock private TemplateEngine templateEngine;
  @Spy private MeterRegistry meterRegistry = new SimpleMeterRegistry();
  @InjectMocks private EmailServiceImpl emailServiceImpl;

  private OrderCreatedEvent orderCreatedEvent;
  private OrderStatusChangedEvent orderStatusChangedEvent;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(emailServiceImpl, "fromAddress", "noreply@demo-app.local");
    when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>test</html>");

    orderCreatedEvent =
        new OrderCreatedEvent(
            1L,
            "John Doe",
            "john@example.com",
            List.of(new OrderCreatedEvent.OrderItem(1L, "Widget", 2, new BigDecimal("9.99"))),
            new BigDecimal("19.98"),
            LocalDateTime.of(2024, 1, 15, 10, 0, 0));

    orderStatusChangedEvent =
        new OrderStatusChangedEvent(
            1L,
            "john@example.com",
            "John Doe",
            "PENDING",
            "CONFIRMED",
            LocalDateTime.of(2024, 1, 15, 11, 0, 0));
  }

  @Test
  @DisplayName("Should process order-created template with correct variables")
  void sendOrderConfirmation_ProcessesOrderCreatedTemplate() {
    emailServiceImpl.sendOrderConfirmation(orderCreatedEvent);

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine).process(eq("order-created"), contextCaptor.capture());
    Context ctx = contextCaptor.getValue();
    assertThat(ctx.getVariable("customerName")).isEqualTo("John Doe");
    assertThat(ctx.getVariable("orderId")).isEqualTo(1L);
    assertThat(ctx.getVariable("totalAmount")).isEqualTo(new BigDecimal("19.98"));
    assertThat(ctx.getVariable("items")).isEqualTo(orderCreatedEvent.items());
  }

  @Test
  @DisplayName("Should send confirmation email to customer with correct subject")
  void sendOrderConfirmation_SendsEmailWithCorrectRecipientAndSubject() throws Exception {
    emailServiceImpl.sendOrderConfirmation(orderCreatedEvent);

    ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(messageCaptor.capture());
    MimeMessage sent = messageCaptor.getValue();
    assertThat(((InternetAddress) sent.getRecipients(Message.RecipientType.TO)[0]).getAddress())
        .isEqualTo("john@example.com");
    assertThat(sent.getSubject()).isEqualTo("Order Confirmation #1");
  }

  @Test
  @DisplayName("Should process order-status-changed template with correct variables")
  void sendStatusUpdate_ProcessesOrderStatusChangedTemplate() {
    emailServiceImpl.sendStatusUpdate(orderStatusChangedEvent);

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine).process(eq("order-status-changed"), contextCaptor.capture());
    Context ctx = contextCaptor.getValue();
    assertThat(ctx.getVariable("customerName")).isEqualTo("John Doe");
    assertThat(ctx.getVariable("orderId")).isEqualTo(1L);
    assertThat(ctx.getVariable("previousStatus")).isEqualTo("PENDING");
    assertThat(ctx.getVariable("newStatus")).isEqualTo("CONFIRMED");
  }

  @Test
  @DisplayName("Should send status update email to customer with correct subject")
  void sendStatusUpdate_SendsEmailWithCorrectRecipientAndSubject() throws Exception {
    emailServiceImpl.sendStatusUpdate(orderStatusChangedEvent);

    ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(messageCaptor.capture());
    MimeMessage sent = messageCaptor.getValue();
    assertThat(((InternetAddress) sent.getRecipients(Message.RecipientType.TO)[0]).getAddress())
        .isEqualTo("john@example.com");
    assertThat(sent.getSubject()).isEqualTo("Order #1 \u2014 Status Update: CONFIRMED");
  }

  @Test
  @DisplayName("Should not throw when MessagingException occurs while building message")
  void sendOrderConfirmation_WhenMessagingExceptionOccurs_DoesNotThrow() throws MessagingException {
    MimeMessage brokenMessage = mock(MimeMessage.class);
    doThrow(new MessagingException("connection refused"))
        .when(brokenMessage)
        .setContent(any(Multipart.class));
    when(mailSender.createMimeMessage()).thenReturn(brokenMessage);

    assertThatNoException()
        .isThrownBy(() -> emailServiceImpl.sendOrderConfirmation(orderCreatedEvent));
    verify(mailSender, never()).send(any(MimeMessage.class));
  }
}
