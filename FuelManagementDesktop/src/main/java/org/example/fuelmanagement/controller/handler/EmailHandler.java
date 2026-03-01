package org.example.fuelmanagement.controller.handler;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import org.example.fuelmanagement.controller.helper.AlertHelper;
import org.example.fuelmanagement.controller.helper.FormValidationHelper;
import org.example.fuelmanagement.dao.TripDAO;
import org.example.fuelmanagement.service.EmailService;
import org.example.fuelmanagement.util.EmailParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

public class EmailHandler {
    private static final Logger logger = LoggerFactory.getLogger(EmailHandler.class);

    private final EmailService emailService;
    private final TripDAO tripDAO;

    public EmailHandler(EmailService emailService, TripDAO tripDAO) {
        this.emailService = emailService;
        this.tripDAO = tripDAO;
    }

    public boolean isConfigured() {
        return emailService != null;
    }

    public void checkEmails(
            ObservableList<EmailParser.ParsedEmailData> parsedEmailsData,
            Consumer<Integer> onSuccess,
            Consumer<Exception> onError,
            Runnable onStart,
            Runnable onComplete
    ) {
        if (emailService == null) {
            AlertHelper.showError("Помилка", "Email сервіс не налаштовано");
            return;
        }

        Platform.runLater(onStart);

        Task<List<EmailParser.ParsedEmailData>> checkEmailTask = new Task<>() {
            @Override
            protected List<EmailParser.ParsedEmailData> call() throws Exception {
                return emailService.checkForNewEmails();
            }

            @Override
            protected void succeeded() {
                List<EmailParser.ParsedEmailData> emails = getValue();
                Platform.runLater(() -> {
                    onComplete.run();

                    if (emails.isEmpty()) {
                        onSuccess.accept(0);
                    } else {
                        int addedCount = 0;
                        for (EmailParser.ParsedEmailData newEmail : emails) {
                            if (!isDuplicateEmail(newEmail, parsedEmailsData)) {
                                parsedEmailsData.add(newEmail);
                                addedCount++;
                            }
                        }
                        checkUsedEmails(parsedEmailsData);
                        onSuccess.accept(addedCount);
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    onComplete.run();
                    onError.accept((Exception) getException());
                    logger.error("Помилка перевірки email: ", getException());
                });
            }
        };

        new Thread(checkEmailTask).start();
    }

    public boolean isDuplicateEmail(
            EmailParser.ParsedEmailData newEmail,
            ObservableList<EmailParser.ParsedEmailData> existingEmails
    ) {
        if (newEmail == null || existingEmails == null || existingEmails.isEmpty()) {
            return false;
        }

        String newEmailAddr = newEmail.requesterEmail != null 
                ? newEmail.requesterEmail.toLowerCase().trim() : "";
        String newStartAddr = FormValidationHelper.normalizeAddress(newEmail.startAddress);
        String newEndAddr = FormValidationHelper.normalizeAddress(newEmail.endAddress);

        for (EmailParser.ParsedEmailData existing : existingEmails) {
            String existingEmailAddr = existing.requesterEmail != null 
                    ? existing.requesterEmail.toLowerCase().trim() : "";
            String existingStartAddr = FormValidationHelper.normalizeAddress(existing.startAddress);
            String existingEndAddr = FormValidationHelper.normalizeAddress(existing.endAddress);

            boolean emailMatch = !newEmailAddr.isEmpty() && newEmailAddr.equals(existingEmailAddr);
            boolean startAddrMatch = !newStartAddr.isEmpty() && newStartAddr.equals(existingStartAddr);
            boolean endAddrMatch = !newEndAddr.isEmpty() && newEndAddr.equals(existingEndAddr);

            boolean timeMatch = false;
            if (newEmail.plannedStartTime != null && existing.plannedStartTime != null) {
                timeMatch = newEmail.plannedStartTime.equals(existing.plannedStartTime);
            }

            if (emailMatch && startAddrMatch && endAddrMatch) {
                if (newEmail.plannedStartTime == null || existing.plannedStartTime == null || timeMatch) {
                    logger.debug("Знайдено дублікат заявки: {} -> {}", newStartAddr, newEndAddr);
                    return true;
                }
            }
        }
        return false;
    }

    public void checkUsedEmails(ObservableList<EmailParser.ParsedEmailData> parsedEmailsData) {
        if (parsedEmailsData == null || parsedEmailsData.isEmpty()) {
            return;
        }

        logger.info("Перевірка {} заявок на використання...", parsedEmailsData.size());
        int usedCount = 0;

        for (EmailParser.ParsedEmailData emailData : parsedEmailsData) {
            if (emailData.isValid &&
                    emailData.requesterEmail != null && !emailData.requesterEmail.isEmpty() &&
                    emailData.startAddress != null && !emailData.startAddress.isEmpty() &&
                    emailData.endAddress != null && !emailData.endAddress.isEmpty()) {

                boolean wasUsed = tripDAO.existsSimilarTrip(
                        emailData.requesterEmail,
                        emailData.startAddress,
                        emailData.endAddress,
                        emailData.plannedStartTime
                );

                if (wasUsed && !emailData.isUsed) {
                    emailData.isUsed = true;
                    usedCount++;
                    logger.debug("Заявка від {} вже використана", emailData.requesterName);
                }
            }
        }

        if (usedCount > 0) {
            logger.info("Знайдено {} використаних заявок", usedCount);
        }
    }

    public String getErrorHint(String errorMessage) {
        if (errorMessage.contains("authentication") || errorMessage.contains("password")) {
            return "Перевірте логін/пароль в application.properties";
        } else if (errorMessage.contains("connection")) {
            return "Перевірте інтернет з'єднання та налаштування сервера";
        }
        return null;
    }
}
