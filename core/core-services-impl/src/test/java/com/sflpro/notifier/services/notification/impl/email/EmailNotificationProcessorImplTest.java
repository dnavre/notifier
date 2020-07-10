package com.sflpro.notifier.services.notification.impl.email;

import com.sflpro.notifier.db.entities.notification.NotificationProviderType;
import com.sflpro.notifier.db.entities.notification.NotificationState;
import com.sflpro.notifier.db.entities.notification.email.EmailNotification;
import com.sflpro.notifier.services.common.exception.ServicesRuntimeException;
import com.sflpro.notifier.services.notification.email.EmailNotificationService;
import com.sflpro.notifier.services.test.AbstractServicesUnitTest;
import com.sflpro.notifier.spi.email.SimpleEmailMessage;
import com.sflpro.notifier.spi.email.SimpleEmailSender;
import com.sflpro.notifier.spi.email.TemplatedEmailMessage;
import com.sflpro.notifier.spi.email.TemplatedEmailSender;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * User: Ruben Dilanyan
 * Company: SFL LLC
 * Date: 1/11/16
 * Time: 2:27 PM
 */
public class EmailNotificationProcessorImplTest extends AbstractServicesUnitTest {

    /* Test subject and mocks */
    private EmailNotificationProcessorImpl emailNotificationProcessor;

    @Mock
    private EmailSenderProvider emailSenderProvider;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private SimpleEmailSender simpleEmailSender;

    @Mock
    private TemplatedEmailSender templatedEmailSender;

    @Before
    public void prepare() {
        emailNotificationProcessor = new EmailNotificationProcessorImpl(emailNotificationService, emailSenderProvider);
    }

    /* Test methods */
    @Test
    public void testProcessNotificationWithInvalidArguments() {
        // Reset
        resetAll();
        // Replay
        replayAll();
        // Run test scenario
        try {
            emailNotificationProcessor.processNotification(null, Collections.emptyMap());
            fail("Exception should be thrown");
        } catch (final IllegalArgumentException ex) {
            // Expected
        }
        // Verify
        verifyAll();
    }

    @Test
    public void testProcessNotificationWithInvalidNotificationState() {
        // Test data
        final Long notificationId = 1L;
        final EmailNotification notification = getServicesImplTestHelper().createEmailNotification();
        notification.setId(notificationId);
        notification.setState(NotificationState.PROCESSING);
        notification.setProviderType(NotificationProviderType.SMTP_SERVER);
        // Reset
        resetAll();
        // Expectations
        expect(emailNotificationService.getEmailNotificationForProcessing(eq(notificationId))).andReturn(notification).once();
        // Replay
        replayAll();
        // Run test scenario
        try {
            emailNotificationProcessor.processNotification(notificationId, Collections.emptyMap());
            fail("Exception should be thrown");
        } catch (final IllegalArgumentException ex) {
            // Expected
        }
        // Verify
        verifyAll();
    }

    @Test
    public void testProcessTemplatedNotificationForWhenProviderTypeIsSmtpAndSuccess() {
        // Test data
        final Long notificationId = 1L;
        final EmailNotification notification = getServicesImplTestHelper().createEmailNotification();
        notification.setId(notificationId);
        notification.setState(NotificationState.CREATED);
        notification.setProviderType(NotificationProviderType.SMTP_SERVER);
        notification.setTemplateName("YoTemplate");
        // Reset
        resetAll();
        // Expectations
        expect(emailNotificationService.getEmailNotificationForProcessing(notificationId)).andReturn(notification).once();
        expect(emailNotificationService.updateNotificationState(notificationId, NotificationState.PROCESSING)).andReturn(notification).once();
        expect(emailSenderProvider.lookupTemplatedEmailSenderFor(notification.getProviderType().name().toLowerCase())).andReturn(Optional.of(templatedEmailSender));
        templatedEmailSender.send(isA(TemplatedEmailMessage.class));
        expectLastCall().andAnswer(() -> {
            final TemplatedEmailMessage message = (TemplatedEmailMessage) getCurrentArguments()[0];
            assertTemplatedEmailMessage(message, notification);
            return null;
        });
        expect(emailNotificationService.updateNotificationState(notificationId, NotificationState.SENT)).andReturn(notification).once();
        // Replay
        replayAll();
        // Run test scenario
        emailNotificationProcessor.processNotification(notificationId, Collections.emptyMap());
        // Verify
        verifyAll();
    }

    @Test
    public void testProcessSimpleNotificationForWhenProviderTypeIsSmtpAndSuccess() {
        // Test data

        final Long notificationId = 1L;
        final EmailNotification notification = getServicesImplTestHelper().createEmailNotification();
        notification.setId(notificationId);
        notification.setState(NotificationState.CREATED);
        notification.setProviderType(NotificationProviderType.SMTP_SERVER);
        // Reset
        resetAll();
        // Expectations
        expect(emailNotificationService.getEmailNotificationForProcessing(notificationId)).andReturn(notification).once();
        expect(emailNotificationService.updateNotificationState(notificationId, NotificationState.PROCESSING)).andReturn(notification).once();
        expect(emailSenderProvider.lookupSimpleEmailSenderFor(notification.getProviderType().name().toLowerCase())).andReturn(Optional.of(simpleEmailSender));
        simpleEmailSender.send(isA(SimpleEmailMessage.class));
        expectLastCall().andAnswer(() -> {
            final SimpleEmailMessage message = (SimpleEmailMessage) getCurrentArguments()[0];
            assertSimpleEmailMessage(message, notification);
            return null;
        });
        expect(emailNotificationService.updateNotificationState(notificationId, NotificationState.SENT)).andReturn(notification).once();
        // Replay
        replayAll();
        // Run test scenario
        emailNotificationProcessor.processNotification(notificationId, Collections.emptyMap());
        // Verify
        verifyAll();
    }

    @Test
    public void testProcessNotificationForWhenProviderTypeIsMandrillAndFail() {
        // Test data
        final Long notificationId = 1L;
        final EmailNotification notification = getServicesImplTestHelper().createEmailNotification();
        notification.setId(notificationId);
        notification.setState(NotificationState.CREATED);
        notification.setProviderType(NotificationProviderType.MANDRILL);
        notification.setTemplateName("YoTemplate");
        // Reset
        resetAll();
        // Expectations
        expect(emailNotificationService.getEmailNotificationForProcessing(notificationId)).andReturn(notification).once();
        expect(emailNotificationService.updateNotificationState(notificationId, NotificationState.PROCESSING)).andReturn(notification).once();
        expect(emailSenderProvider.lookupTemplatedEmailSenderFor(notification.getProviderType().name().toLowerCase())).andReturn(Optional.of(templatedEmailSender));
        templatedEmailSender.send(isA(TemplatedEmailMessage.class));
        expectLastCall().andAnswer(()->{
            final TemplatedEmailMessage message = (TemplatedEmailMessage) getCurrentArguments()[0];
            assertTemplatedEmailMessage(message, notification);
            throw new RuntimeException("Failed to send email");
        });
        expect(emailNotificationService.updateNotificationState(notificationId, NotificationState.FAILED)).andReturn(notification).once();
        expectLastCall().andAnswer(() -> {
            final Runnable runnable = (Runnable) getCurrentArguments()[0];
            runnable.run();
            return null;
        }).anyTimes();
        // Replay
        replayAll();
        // Run test scenario
        try {
            // Run test scenario
            emailNotificationProcessor.processNotification(notificationId, Collections.emptyMap());
            fail("Exception should be thrown");
        } catch (final ServicesRuntimeException ex) {
            // Expected
        }
        // Verify
        verifyAll();
    }

    @Test
    public void testProcessNotificationWhenErrorOccursDuringProcessing() {
        // Test data
        final Long notificationId = 1L;
        final EmailNotification notification = getServicesImplTestHelper().createEmailNotification();
        notification.setId(notificationId);
        notification.setState(NotificationState.CREATED);
        notification.setTemplateName("YoTemplate");
        // Reset
        resetAll();
        // Expectations
        expect(emailNotificationService.getEmailNotificationForProcessing(notificationId)).andReturn(notification).once();
        expect(emailNotificationService.updateNotificationState(notificationId, NotificationState.PROCESSING)).andReturn(notification).once();
        expect(emailSenderProvider.lookupTemplatedEmailSenderFor(notification.getProviderType().name().toLowerCase())).andReturn(Optional.of(templatedEmailSender));
        templatedEmailSender.send(isA(TemplatedEmailMessage.class));
        expectLastCall().andAnswer(()->{
            final TemplatedEmailMessage message = (TemplatedEmailMessage) getCurrentArguments()[0];
            assertTemplatedEmailMessage(message, notification);
            throw new RuntimeException("Failed to send email");
        });
        expect(emailNotificationService.updateNotificationState(notificationId, NotificationState.FAILED)).andReturn(notification).once();
        // Replay
        replayAll();
        try {
            // Run test scenario
            emailNotificationProcessor.processNotification(notificationId, Collections.emptyMap());
            fail("Exception should be thrown");
        } catch (final ServicesRuntimeException ex) {
            // Expected
        }
        // Verify
        verifyAll();
    }

    /* Utility methods */
    private static void assertSimpleEmailMessage(final SimpleEmailMessage message, final EmailNotification notification) {
        assertEquals(notification.getSenderEmail(), message.from());
        assertEquals(notification.getRecipientEmail(), message.to());
        assertEquals(notification.getContent(), message.body());
        assertEquals(notification.getSubject(), message.subject());
    }

    private static void assertTemplatedEmailMessage(final TemplatedEmailMessage message, final EmailNotification notification) {
        assertEquals(notification.getSenderEmail(), message.from());
        assertEquals(notification.getRecipientEmail(), message.to());
        assertEquals(notification.getTemplateName(), message.templateId());
    }
}
