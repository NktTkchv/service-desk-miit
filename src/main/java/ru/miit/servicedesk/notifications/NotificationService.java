package ru.miit.servicedesk.notifications;

public interface NotificationService {

    void notifyUser(Long userId, Long requestId, String type, String title, String body);

    void sendEmail(Long userId, String subject, String body);

    void sendChatMessage(Long userId, String message);
}
