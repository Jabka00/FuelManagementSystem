package org.example.fuelmanagement.service;

import org.example.fuelmanagement.util.EmailParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.AndTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private static final String TARGET_SUBJECT_PREFIX = "✉ Запит до інших служб";
    private static final int MAX_EMAILS_TO_PROCESS = 50;
    private static final int DAYS_TO_SEARCH = 10;

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private Store store;

    public EmailService(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        logger.info(" EmailService ініціалізовано для {}", username);
    }

    public List<EmailParser.ParsedEmailData> checkForNewEmails() throws Exception {
        List<EmailParser.ParsedEmailData> parsedEmails = new ArrayList<>();

        try {
            logger.info(" Початок перевірки пошти (оптимізований режим)...");
            long startTime = System.currentTimeMillis();

            connect();

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            logger.info("📫 Загальна кількість листів в INBOX: {}", inbox.getMessageCount());

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -DAYS_TO_SEARCH);
            Date sinceDate = cal.getTime();
            SearchTerm unseenTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            SearchTerm dateTerm = new ReceivedDateTerm(ComparisonTerm.GE, sinceDate);
            SearchTerm combinedTerm = new AndTerm(unseenTerm, dateTerm);
            Message[] unreadMessages = inbox.search(combinedTerm);
            logger.info("📬 Знайдено {} непрочитаних листів за останні {} днів", unreadMessages.length, DAYS_TO_SEARCH);

            if (unreadMessages.length == 0) {
                inbox.close(false);
                logger.info(" Немає непрочитаних листів для обробки");
                return parsedEmails;
            }

            int messagesToProcess = Math.min(unreadMessages.length, MAX_EMAILS_TO_PROCESS);
            Message[] messagesToCheck = new Message[messagesToProcess];
            for (int i = 0; i < messagesToProcess; i++) {
                messagesToCheck[i] = unreadMessages[unreadMessages.length - 1 - i];
            }

            FetchProfile fetchProfile = new FetchProfile();
            fetchProfile.add(FetchProfile.Item.ENVELOPE);
            fetchProfile.add(FetchProfile.Item.FLAGS);
            fetchProfile.add(FetchProfile.Item.CONTENT_INFO);
            inbox.fetch(messagesToCheck, fetchProfile);
            logger.info("⚡ Метадані {} листів завантажено пакетом", messagesToProcess);

            List<Message> targetMessages = new ArrayList<>();
            for (Message message : messagesToCheck) {
                try {
                    String subject = message.getSubject();
                    if (isTargetEmail(subject)) {
                        targetMessages.add(message);
                        logger.debug(" Знайдено цільовий лист: '{}'", subject);
                    }
                } catch (Exception e) {
                    logger.warn(" Помилка читання теми листа: {}", e.getMessage());
                }
            }

            logger.info(" Знайдено {} листів з потрібною темою", targetMessages.size());

            if (targetMessages.isEmpty()) {
                inbox.close(false);
                long duration = System.currentTimeMillis() - startTime;
                logger.info(" Перевірка завершена за {} мс, цільових листів не знайдено", duration);
                return parsedEmails;
            }

            int targetEmailsParsed = 0;
            for (Message message : targetMessages) {
                try {
                    String emailContent = getEmailContent(message);

                    if (emailContent != null && !emailContent.trim().isEmpty()) {
                        logger.debug("📄 Контент листа отримано ({} символів)", emailContent.length());

                        EmailParser.ParsedEmailData parsedData = EmailParser.parseEmail(emailContent);

                        if (parsedData.isValid) {
                            parsedEmails.add(parsedData);
                            targetEmailsParsed++;
                            logger.info(" Лист успішно парсований: {}", parsedData.requesterName);
                        } else {
                            logger.warn(" Лист не валідний: {}", parsedData.errorMessage);
                        }
                    } else {
                        logger.warn(" Порожній контент листа");
                    }
                } catch (Exception e) {
                    logger.error(" Помилка обробки листа: {}", e.getMessage());
                }
            }

            inbox.close(false);

            long duration = System.currentTimeMillis() - startTime;
            logger.info(" Результат перевірки пошти (за {} мс):", duration);
            logger.info("  • Перевірено непрочитаних: {}", messagesToProcess);
            logger.info("  • Знайдено цільових листів: {}", targetMessages.size());
            logger.info("  • Успішно парсовано: {}", targetEmailsParsed);

        } finally {
            disconnect();
        }

        return parsedEmails;
    }

    private boolean isTargetEmail(String subject) {
        if (subject == null || subject.trim().isEmpty()) {
            return false;
        }

        String trimmedSubject = subject.trim();
        if (trimmedSubject.startsWith(TARGET_SUBJECT_PREFIX)) {
            logger.debug(" Тема починається з '{}': '{}'", TARGET_SUBJECT_PREFIX, subject);
            return true;
        }

        String lowerSubject = trimmedSubject.toLowerCase();
        String lowerPrefix = TARGET_SUBJECT_PREFIX.toLowerCase();
        if (lowerSubject.startsWith(lowerPrefix)) {
            logger.debug(" Тема починається з '{}' (ігнор регістру): '{}'", TARGET_SUBJECT_PREFIX, subject);
            return true;
        }

        return false;
    }

    private String getEmailContent(Message message) throws MessagingException, IOException {
        try {
            Object content = message.getContent();

            if (content instanceof String) {

                return (String) content;
            } else if (content instanceof MimeMultipart) {

                return getTextFromMimeMultipart((MimeMultipart) content);
            } else {
                logger.warn(" Невідомий тип контенту: {}", content.getClass().getName());
                return content.toString();
            }
        } catch (Exception e) {
            logger.error(" Помилка отримання контенту листа: {}", e.getMessage());
            return null;
        }
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        StringBuilder result = new StringBuilder();
        int count = mimeMultipart.getCount();

        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);

            if (bodyPart.isMimeType("text/plain")) {

                result.append(bodyPart.getContent().toString());
            } else if (bodyPart.isMimeType("text/html")) {

                String htmlContent = bodyPart.getContent().toString();
                String plainText = htmlToPlainText(htmlContent);
                result.append(plainText);
            } else if (bodyPart.isMimeType("multipart/*")) {

                MimeMultipart nestedMultipart = (MimeMultipart) bodyPart.getContent();
                result.append(getTextFromMimeMultipart(nestedMultipart));
            }
        }

        return result.toString();
    }

    private String htmlToPlainText(String html) {
        if (html == null) return "";

        String text = html.replaceAll("<[^>]*>", " ");

        text = text.replace("&nbsp;", " ");
        text = text.replace("&amp;", "&");
        text = text.replace("&lt;", "<");
        text = text.replace("&gt;", ">");
        text = text.replace("&quot;", "\"");
        text = text.replace("&#39;", "'");

        text = text.replaceAll("\\s+", " ");

        return text.trim();
    }

    private void connect() throws MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", host);
        props.put("mail.imaps.port", port);
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.ssl.trust", "*");
        props.put("mail.imaps.connectiontimeout", "10000"); 
        props.put("mail.imaps.timeout", "15000"); 
        props.put("mail.imaps.writetimeout", "10000"); 
        props.put("mail.imaps.partialfetch", "true");
        props.put("mail.imaps.fetchsize", "65536"); 
        props.put("mail.imaps.compress.enable", "true");
        props.put("mail.imaps.connectionpoolsize", "2");
        props.put("mail.imaps.connectionpooltimeout", "30000");

        Session session = Session.getInstance(props);
        store = session.getStore("imaps");
        store.connect(host, username, password);

        logger.info(" Підключено до поштового сервера {}", host);
    }

    private void disconnect() {
        try {
            if (store != null && store.isConnected()) {
                store.close();
                logger.info("🔌 Відключено від поштового сервера");
            }
        } catch (MessagingException e) {
            logger.error(" Помилка відключення: {}", e.getMessage());
        }
    }

    public boolean testConnection() {
        try {
            connect();
            logger.info(" Тест підключення до email успішний");
            return true;
        } catch (Exception e) {
            logger.error(" Тест підключення до email невдалий: {}", e.getMessage());
            return false;
        } finally {
            disconnect();
        }
    }

    public String getConfigurationInfo() {
        return String.format("Email: %s, Сервер: %s:%d, Цільова тема (починається з): '%s'",
                username, host, port, TARGET_SUBJECT_PREFIX);
    }
}