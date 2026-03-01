package org.example.fuelmanagement.util;

import org.example.fuelmanagement.model.Trip;
import org.example.fuelmanagement.model.enums.TripType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailParser {
    private static final Logger logger = LoggerFactory.getLogger(EmailParser.class);

    private static final Pattern NAME_PATTERN = Pattern.compile(
            "ПІБ\\s*:\\s*([А-Яа-яІіЇїЄєҐґA-Za-z'\\-\\s]+?)\\s*(?:[\\r\\n]+|Контактна|$)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "Контактна\\s+електронна\\s+адреса\\s*:\\s*([\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,})",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "Мобільний\\s+номер\\s+телефону\\s*:\\s*([+]?[0-9\\s\\-\\(\\)]{10,20})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern START_ADDRESS_PATTERN = Pattern.compile(
            "Початкова\\s+адреса\\s*:\\s*(.+?)\\s*(?:[\\r\\n]+|Тип\\s+прямування|$)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern END_ADDRESS_PATTERN = Pattern.compile(
            "Кінцева\\s+адреса\\s*:\\s*(.+?)\\s*(?:[\\r\\n]+|Початкова\\s+дата|$)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern TRIP_TYPE_PATTERN = Pattern.compile(
            "Тип\\s+прямування\\s*:\\s*(.+?)\\s*(?:[\\r\\n]+|Кінцева|$)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern START_TIME_PATTERN = Pattern.compile(
            "Початкова\\s+дата\\s*,?\\s*час\\s*:\\s*(.+?)\\s*(?:[\\r\\n]+|Кінцева\\s+дата|$)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern END_TIME_PATTERN = Pattern.compile(
            "Кінцева\\s+дата\\s*,?\\s*час\\s*:\\s*(.+?)\\s*(?:[\\r\\n]+|На\\s+кого|$)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern PURPOSE_PATTERN = Pattern.compile(
            "Мета\\s+замовлення\\s*:\\s*(.+?)\\s*(?:[\\r\\n]+|Запит\\s+створено|$)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern POWER_OF_ATTORNEY_PATTERN = Pattern.compile(
            "На\\s+кого\\s+виписана\\s+довіреність\\s*:\\s*(.+?)\\s*(?:[\\r\\n]+|Чи\\s+може|$)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern CAN_DELIVER_PATTERN = Pattern.compile(
            "Чи\\s+може\\s+водій\\s+сам\\s+завезти\\s+вантаж\\s*:\\s*(.+?)\\s*(?:[\\r\\n]+|Час\\s+очікування|$)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Map<String, String> HTML_ENTITIES = new HashMap<>();
    static {
        HTML_ENTITIES.put("&nbsp;", " ");
        HTML_ENTITIES.put("&amp;", "&");
        HTML_ENTITIES.put("&lt;", "<");
        HTML_ENTITIES.put("&gt;", ">");
        HTML_ENTITIES.put("&quot;", "\"");
        HTML_ENTITIES.put("&apos;", "'");
        HTML_ENTITIES.put("&#39;", "'");
        HTML_ENTITIES.put("&#160;", " ");
        HTML_ENTITIES.put("&#34;", "\"");
        HTML_ENTITIES.put("&#38;", "&");
        HTML_ENTITIES.put("&#60;", "<");
        HTML_ENTITIES.put("&#62;", ">");
        HTML_ENTITIES.put("&mdash;", "—");
        HTML_ENTITIES.put("&ndash;", "–");
        HTML_ENTITIES.put("&copy;", "©");
        HTML_ENTITIES.put("&reg;", "®");
        HTML_ENTITIES.put("&trade;", "™");
        HTML_ENTITIES.put("&euro;", "€");
        HTML_ENTITIES.put("&pound;", "£");
        HTML_ENTITIES.put("&yen;", "¥");
        HTML_ENTITIES.put("&cent;", "¢");
        HTML_ENTITIES.put("&laquo;", "«");
        HTML_ENTITIES.put("&raquo;", "»");
        HTML_ENTITIES.put("&hellip;", "…");
        HTML_ENTITIES.put("&bull;", "•");
        HTML_ENTITIES.put("&#8212;", "—");
        HTML_ENTITIES.put("&#8211;", "–");
        HTML_ENTITIES.put("&#8230;", "…");
    }

    private static final Map<String, String> UKRAINIAN_MONTHS = new HashMap<>();
    static {
        UKRAINIAN_MONTHS.put("січня", "January");
        UKRAINIAN_MONTHS.put("лютого", "February");
        UKRAINIAN_MONTHS.put("березня", "March");
        UKRAINIAN_MONTHS.put("квітня", "April");
        UKRAINIAN_MONTHS.put("травня", "May");
        UKRAINIAN_MONTHS.put("червня", "June");
        UKRAINIAN_MONTHS.put("липня", "July");
        UKRAINIAN_MONTHS.put("серпня", "August");
        UKRAINIAN_MONTHS.put("вересня", "September");
        UKRAINIAN_MONTHS.put("жовтня", "October");
        UKRAINIAN_MONTHS.put("листопада", "November");
        UKRAINIAN_MONTHS.put("грудня", "December");
    }

    private static final Map<String, String> UKRAINIAN_DAYS = new HashMap<>();
    static {
        UKRAINIAN_DAYS.put("понеділок", "Monday");
        UKRAINIAN_DAYS.put("вівторок", "Tuesday");
        UKRAINIAN_DAYS.put("середа", "Wednesday");
        UKRAINIAN_DAYS.put("четвер", "Thursday");
        UKRAINIAN_DAYS.put("п'ятниця", "Friday");
        UKRAINIAN_DAYS.put("субота", "Saturday");
        UKRAINIAN_DAYS.put("неділя", "Sunday");
    }

    public static ParsedEmailData parseEmail(String emailContent) {
        logger.info("=== ПОЧАТОК ПАРСИНГУ КОРПОРАТИВНОГО EMAIL ===");

        ParsedEmailData data = new ParsedEmailData();

        try {
            if (emailContent == null || emailContent.trim().isEmpty()) {
                logger.warn("Порожній контент email");
                data.isValid = false;
                data.errorMessage = "Порожній контент";
                return data;
            }

            logger.info("Розмір контенту: {} символів", emailContent.length());
            logger.debug("Перші 500 символів:\n{}",
                    emailContent.length() > 500 ? emailContent.substring(0, 500) + "..." : emailContent);

            String normalizedContent = normalizeEncoding(emailContent);
            String cleanContent = cleanEmailContent(normalizedContent);
            logger.info("Очищений контент (перші 500 симв.):\n{}",
                    cleanContent.length() > 500 ? cleanContent.substring(0, 500) + "..." : cleanContent);

            data.requesterName = extractValue(cleanContent, NAME_PATTERN, "ПІБ");
            data.requesterEmail = extractValue(cleanContent, EMAIL_PATTERN, "Email");
            data.requesterPhone = extractValue(cleanContent, PHONE_PATTERN, "Телефон");
            data.startAddress = extractValue(cleanContent, START_ADDRESS_PATTERN, "Початкова адреса");
            data.endAddress = extractValue(cleanContent, END_ADDRESS_PATTERN, "Кінцева адреса");
            data.purpose = extractValue(cleanContent, PURPOSE_PATTERN, "Мета");
            data.powerOfAttorney = extractValue(cleanContent, POWER_OF_ATTORNEY_PATTERN, "Довіреність");

            String tripTypeStr = extractValue(cleanContent, TRIP_TYPE_PATTERN, "Тип поїздки");
            if (tripTypeStr != null && tripTypeStr.toLowerCase().contains("обидві сторони")) {
                data.tripType = TripType.ROUND_TRIP;
                logger.info("Тип поїздки: В обидві сторони");
            } else {
                data.tripType = TripType.ONE_WAY;
                logger.info("Тип поїздки: В один бік");
            }

            String canDeliverStr = extractValue(cleanContent, CAN_DELIVER_PATTERN, "Може доставити");
            data.canDriverDeliver = canDeliverStr != null &&
                    (canDeliverStr.toLowerCase().contains("так") ||
                            canDeliverStr.toLowerCase().contains("yes"));
            logger.info(" Може водій доставити: {}", data.canDriverDeliver);

            String startTimeStr = extractValue(cleanContent, START_TIME_PATTERN, "Початковий час");
            String endTimeStr = extractValue(cleanContent, END_TIME_PATTERN, "Кінцевий час");

            data.plannedStartTime = parseUkrainianDateTime(startTimeStr);
            data.plannedEndTime = parseUkrainianDateTime(endTimeStr);

            data.requesterName = cleanPersonName(data.requesterName);
            data.requesterEmail = cleanEmail(data.requesterEmail);
            data.requesterPhone = cleanPhone(data.requesterPhone);
            data.startAddress = cleanAddress(data.startAddress);
            data.endAddress = cleanAddress(data.endAddress);
            data.purpose = cleanPurpose(data.purpose);
            data.powerOfAttorney = cleanField(data.powerOfAttorney);

            data.isValid = validateCorporateData(data);

            logParsingResults(data);

        } catch (Exception e) {
            logger.error("❌ Помилка парсингу email: ", e);
            data.isValid = false;
            data.errorMessage = "Помилка парсингу: " + e.getMessage();
        }

        return data;
    }

    private static String normalizeEncoding(String content) {
        if (content == null) return "";
        try {
            if (containsMalformedCharacters(content)) {
                logger.info("Виявлено можливі проблеми з кодуванням, спроба перекодування...");
                byte[] bytes = content.getBytes(StandardCharsets.ISO_8859_1);
                String windows1251 = new String(bytes, Charset.forName("Windows-1251"));
                if (isValidUkrainianText(windows1251)) {
                    logger.info("Успішно перекодовано з Windows-1251");
                    return windows1251;
                }
                String utf8FromIso = new String(bytes, StandardCharsets.UTF_8);
                if (isValidUkrainianText(utf8FromIso)) {
                    logger.info("Успішно перекодовано з UTF-8/ISO-8859-1");
                    return utf8FromIso;
                }
            }
        } catch (Exception e) {
            logger.warn("Помилка нормалізації кодування: {}", e.getMessage());
        }
        return content;
    }
    private static boolean containsMalformedCharacters(String text) {
        return text.contains("�") || 
               text.contains("Ð") || 
               text.contains("Ã") || 
               (text.contains("\\u") && Pattern.compile("\\\\u[0-9a-fA-F]{4}").matcher(text).find());
    }
    private static boolean isValidUkrainianText(String text) {
        return text.contains("ПІБ") || 
               text.contains("адреса") || 
               text.contains("телефон") ||
               text.contains("дата") ||
               Pattern.compile("[А-Яа-яІіЇїЄєҐґ]{3,}").matcher(text).find();
    }

    private static String extractValue(String content, Pattern pattern, String fieldName) {
        try {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String value = matcher.group(1).trim();
                logger.info(" Знайдено {}: '{}'", fieldName, value);
                return value;
            } else {
                logger.warn(" Не знайдено поле: {}", fieldName);
                logger.debug("Шаблон: {}", pattern.pattern());
                return null;
            }
        } catch (Exception e) {
            logger.error("Помилка парсингу поля {}: {}", fieldName, e.getMessage());
            return null;
        }
    }

    private static String cleanEmailContent(String content) {
        if (content == null) return "";

        content = content.replace("\r\n", "\n");
        content = content.replace("\r", "\n");
        content = Pattern.compile("<script[^>]*>.*?</script>", 
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(content).replaceAll(" ");
        content = Pattern.compile("<style[^>]*>.*?</style>", 
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(content).replaceAll(" ");
        content = Pattern.compile("<!--.*?-->", 
                Pattern.DOTALL).matcher(content).replaceAll(" ");
        content = Pattern.compile("<br\\s*/?>", 
                Pattern.CASE_INSENSITIVE).matcher(content).replaceAll("\n");
        content = Pattern.compile("</(?:p|div|tr|li|h[1-6])>", 
                Pattern.CASE_INSENSITIVE).matcher(content).replaceAll("\n");
        content = removeHtmlTags(content);
        content = decodeHtmlEntities(content);
        content = content.replaceAll("\\*+", "");
        content = content.replaceAll("_{2,}", "");
        content = content.replaceAll("-{3,}", "");
        content = content.replaceAll("[ \\t]+", " ");
        content = content.replaceAll("\\n\\s*\\n+", "\n");
        content = Pattern.compile("^[ \\t]+|[ \\t]+$", Pattern.MULTILINE)
                .matcher(content).replaceAll("");

        return content.trim();
    }
    private static String removeHtmlTags(String content) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        int len = content.length();
        while (i < len) {
            char c = content.charAt(i);
            if (c == '<') {
                int tagEnd = findTagEnd(content, i);
                if (tagEnd > i) {
                    result.append(' '); 
                    i = tagEnd + 1;
                    continue;
                }
            }
            result.append(c);
            i++;
        }
        return result.toString();
    }
    private static int findTagEnd(String content, int start) {
        int i = start + 1;
        int len = content.length();
        char inQuote = 0;
        while (i < len) {
            char c = content.charAt(i);
            if (inQuote != 0) {
                if (c == inQuote) {
                    inQuote = 0;
                }
            } else {
                if (c == '"' || c == '\'') {
                    inQuote = c;
                } else if (c == '>') {
                    return i;
                }
            }
            i++;
        }
        return -1; 
    }
    private static String decodeHtmlEntities(String content) {
        for (Map.Entry<String, String> entry : HTML_ENTITIES.entrySet()) {
            content = content.replace(entry.getKey(), entry.getValue());
            String keyWithoutSemicolon = entry.getKey().replace(";", "");
            if (!keyWithoutSemicolon.equals(entry.getKey())) {
                content = content.replace(keyWithoutSemicolon + " ", entry.getValue() + " ");
            }
        }
        Pattern decimalPattern = Pattern.compile("&#(\\d+);?");
        Matcher decimalMatcher = decimalPattern.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (decimalMatcher.find()) {
            try {
                int code = Integer.parseInt(decimalMatcher.group(1));
                if (code > 0 && code < 0x10FFFF) {
                    decimalMatcher.appendReplacement(sb, String.valueOf((char) code));
                }
            } catch (NumberFormatException e) {
            }
        }
        decimalMatcher.appendTail(sb);
        content = sb.toString();
        Pattern hexPattern = Pattern.compile("&#[xX]([0-9a-fA-F]+);?");
        Matcher hexMatcher = hexPattern.matcher(content);
        sb = new StringBuffer();
        while (hexMatcher.find()) {
            try {
                int code = Integer.parseInt(hexMatcher.group(1), 16);
                if (code > 0 && code < 0x10FFFF) {
                    hexMatcher.appendReplacement(sb, String.valueOf((char) code));
                }
            } catch (NumberFormatException e) {
            }
        }
        hexMatcher.appendTail(sb);
        return sb.toString();
    }

    private static String cleanPersonName(String name) {
        if (name == null || name.isEmpty()) return null;

        name = name.replaceAll("(?i)Контактна\\s+електронна\\s+адреса:.*", "").trim();
        name = name.replaceAll("(?i)Мобільний\\s+номер.*", "").trim();
        name = name.replaceAll("\\s+", " ");

        return name.isEmpty() ? null : name;
    }

    private static String cleanEmail(String email) {
        if (email == null || email.isEmpty()) return null;

        Pattern emailPattern = Pattern.compile("([\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,})");
        Matcher matcher = emailPattern.matcher(email);
        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }

        return email.trim().toLowerCase();
    }

    private static String cleanPhone(String phone) {
        if (phone == null || phone.isEmpty()) return null;

        phone = phone.replaceAll("[^+0-9\\(\\)\\-\\s]", "");
        phone = phone.replaceAll("\\s+", " ").trim();

        return phone.isEmpty() ? null : phone;
    }

    private static String cleanAddress(String address) {
        if (address == null || address.isEmpty()) return null;

        address = address.replaceAll("(?i)Тип\\s+прямування:.*", "").trim();
        address = address.replaceAll("(?i)Початкова\\s+дата.*", "").trim();
        address = address.replaceAll("\\s+", " ");

        return address.isEmpty() ? null : address;
    }

    private static String cleanPurpose(String purpose) {
        if (purpose == null || purpose.isEmpty()) return null;

        purpose = purpose.replaceAll("(?i)Запит\\s+створено:.*", "").trim();
        purpose = purpose.replaceAll("\\s+", " ");

        return purpose.isEmpty() ? null : purpose;
    }

    private static String cleanField(String field) {
        if (field == null || field.isEmpty()) return null;

        field = field.trim();
        field = field.replaceAll("\\s+", " ");

        return field.isEmpty() ? null : field;
    }

    private static LocalDateTime parseUkrainianDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }

        try {
            dateTimeStr = dateTimeStr.trim();
            logger.info("Парсинг дати: '{}'", dateTimeStr);

            dateTimeStr = replaceUkrainianDayNames(dateTimeStr);
            dateTimeStr = replaceUkrainianMonthNames(dateTimeStr);

            logger.info("Після заміни: '{}'", dateTimeStr);
            String dateTimeWithYear = addYearIfMissing(dateTimeStr);
            if (!dateTimeWithYear.equals(dateTimeStr)) {
                logger.info("Додано рік: '{}'", dateTimeWithYear);
            }

            String[] patternsWithTime = {
                    "EEEE, d MMMM yyyy HH:mm",
                    "EEEE, d MMMM yyyy, HH:mm",
                    "EEEE d MMMM yyyy HH:mm",
                    "EEE, d MMMM yyyy HH:mm",
                    "EEE d MMMM yyyy HH:mm",
                    "d MMMM yyyy HH:mm",
                    "d MMMM yyyy, HH:mm",
                    "d MMMM yyyy 'о' HH:mm",
                    "d MMMM yyyy 'р.' HH:mm",
                    "d MMMM yyyy 'р.,' HH:mm",
                    "dd.MM.yyyy HH:mm",
                    "d.MM.yyyy HH:mm",
                    "dd.MM.yyyy, HH:mm",
                    "d.MM.yyyy, HH:mm",
                    "dd/MM/yyyy HH:mm",
                    "d/MM/yyyy HH:mm",
                    "yyyy-MM-dd HH:mm",
                    "yyyy-MM-dd'T'HH:mm",
                    "yyyy-MM-dd'T'HH:mm:ss"
            };
            String[] patternsWithoutTime = {
                    "EEEE, d MMMM yyyy",
                    "EEEE d MMMM yyyy",
                    "EEE, d MMMM yyyy",
                    "EEE d MMMM yyyy",
                    "d MMMM yyyy",
                    "d MMMM yyyy 'р.'",
                    "dd.MM.yyyy",
                    "d.MM.yyyy",
                    "dd/MM/yyyy",
                    "d/MM/yyyy",
                    "yyyy-MM-dd"
            };

            for (String pattern : patternsWithTime) {
                LocalDateTime result = tryParseDateTimeWithPattern(dateTimeWithYear, pattern);
                if (result != null) {
                    return result;
                }
                if (!dateTimeWithYear.equals(dateTimeStr)) {
                    result = tryParseDateTimeWithPattern(dateTimeStr, pattern);
                    if (result != null) {
                        return result;
                    }
                }
            }
            for (String pattern : patternsWithoutTime) {
                LocalDateTime result = tryParseDateWithoutTime(dateTimeWithYear, pattern);
                if (result != null) {
                    return result;
                }
                if (!dateTimeWithYear.equals(dateTimeStr)) {
                    result = tryParseDateWithoutTime(dateTimeStr, pattern);
                    if (result != null) {
                        return result;
                    }
                }
            }

            LocalDateTime extracted = extractDateTimeFromText(dateTimeWithYear);
            if (extracted != null) {
                logger.info(" Дата витягнута з тексту: {}", extracted);
                return extracted;
            }

        } catch (Exception e) {
            logger.error("Помилка парсингу дати '{}': {}", dateTimeStr, e.getMessage());
        }

        logger.warn(" Не вдалося парсити дату: {}", dateTimeStr);
        return null;
    }
    private static LocalDateTime tryParseDateTimeWithPattern(String dateTimeStr, String pattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
                    .withResolverStyle(ResolverStyle.SMART);
            LocalDateTime result = LocalDateTime.parse(dateTimeStr, formatter);
            if (isValidDate(result)) {
                logger.info(" Дата парсована: {} з патерном: {}", result, pattern);
                return result;
            }
        } catch (DateTimeParseException e) {
        }
        return null;
    }
    private static LocalDateTime tryParseDateWithoutTime(String dateStr, String pattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
                    .withResolverStyle(ResolverStyle.SMART);
            java.time.LocalDate date = java.time.LocalDate.parse(dateStr, formatter);
            LocalDateTime result = date.atTime(9, 0);
            if (isValidDate(result)) {
                logger.info(" Дата парсована (без часу, встановлено 09:00): {} з патерном: {}", result, pattern);
                return result;
            }
        } catch (DateTimeParseException e) {
        }
        return null;
    }
    private static String addYearIfMissing(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return dateStr;
        }
        if (Pattern.compile("\\b\\d{4}\\b").matcher(dateStr).find()) {
            return dateStr; 
        }
        int currentYear = java.time.LocalDate.now().getYear();
        int currentMonth = java.time.LocalDate.now().getMonthValue();
        int dateMonth = getMonthFromText(dateStr);
        int yearToUse = currentYear;
        if (dateMonth > 0 && dateMonth < currentMonth) {
            yearToUse = currentYear + 1;
        }
        Pattern timePattern = Pattern.compile("(\\d{1,2}:\\d{2}(?::\\d{2})?)\\s*$");
        Matcher timeMatcher = timePattern.matcher(dateStr);
        if (timeMatcher.find()) {
            String time = timeMatcher.group(1);
            String dateWithoutTime = dateStr.substring(0, timeMatcher.start()).trim();
            return dateWithoutTime + " " + yearToUse + " " + time;
        } else {
            return dateStr.trim() + " " + yearToUse;
        }
    }
    private static int getMonthFromText(String text) {
        String[] months = {"January", "February", "March", "April", "May", "June",
                           "July", "August", "September", "October", "November", "December"};
        String lowerText = text.toLowerCase();
        for (int i = 0; i < months.length; i++) {
            if (lowerText.contains(months[i].toLowerCase())) {
                return i + 1;
            }
        }
        return 0; 
    }
    private static LocalDateTime extractDateTimeFromText(String text) {
        Pattern numericDatePattern = Pattern.compile("(\\d{1,2})[./](\\d{1,2})[./](\\d{4})(?:\\s+(\\d{1,2}):(\\d{2}))?");
        Matcher matcher = numericDatePattern.matcher(text);
        if (matcher.find()) {
            try {
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int year = Integer.parseInt(matcher.group(3));
                int hour = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : 9;
                int minute = matcher.group(5) != null ? Integer.parseInt(matcher.group(5)) : 0;
                LocalDateTime result = LocalDateTime.of(year, month, day, hour, minute);
                if (isValidDate(result)) {
                    return result;
                }
            } catch (Exception e) {
                logger.debug("Не вдалося витягти дату з тексту: {}", e.getMessage());
            }
        }
        Pattern textDatePattern = Pattern.compile("(\\d{1,2})\\s+(January|February|March|April|May|June|July|August|September|October|November|December)(?:\\s+(\\d{4}))?(?:\\s+(\\d{1,2}):(\\d{2}))?", Pattern.CASE_INSENSITIVE);
        matcher = textDatePattern.matcher(text);
        if (matcher.find()) {
            try {
                int day = Integer.parseInt(matcher.group(1));
                String monthName = matcher.group(2);
                int month = getMonthFromText(monthName);
                int year = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : java.time.LocalDate.now().getYear();
                int hour = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : 9;
                int minute = matcher.group(5) != null ? Integer.parseInt(matcher.group(5)) : 0;
                if (matcher.group(3) == null && month < java.time.LocalDate.now().getMonthValue()) {
                    year++;
                }
                LocalDateTime result = LocalDateTime.of(year, month, day, hour, minute);
                if (isValidDate(result)) {
                    return result;
                }
            } catch (Exception e) {
                logger.debug("Не вдалося витягти текстову дату: {}", e.getMessage());
            }
        }
        return null;
    }
    private static boolean isValidDate(LocalDateTime dateTime) {
        if (dateTime == null) return false;
        int year = dateTime.getYear();
        if (year < 2000 || year > 2100) {
            return false;
        }
        LocalDateTime fiveYearsAgo = LocalDateTime.now().minusYears(5);
        if (dateTime.isBefore(fiveYearsAgo)) {
            logger.warn("Дата занадто стара: {}", dateTime);
        }
        return true;
    }

    private static String replaceUkrainianDayNames(String text) {
        for (Map.Entry<String, String> entry : UKRAINIAN_DAYS.entrySet()) {
            text = Pattern.compile(Pattern.quote(entry.getKey()), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                    .matcher(text).replaceAll(entry.getValue());
        }
        return text;
    }

    private static String replaceUkrainianMonthNames(String text) {
        for (Map.Entry<String, String> entry : UKRAINIAN_MONTHS.entrySet()) {
            text = Pattern.compile(Pattern.quote(entry.getKey()), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                    .matcher(text).replaceAll(entry.getValue());
        }
        return text;
    }

    private static boolean validateCorporateData(ParsedEmailData data) {
        boolean isValid = true;
        StringBuilder errors = new StringBuilder();

        if (data.requesterName == null || data.requesterName.trim().isEmpty()) {
            errors.append("ПІБ відсутнє; ");
            isValid = false;
        }

        if (data.requesterEmail == null || data.requesterEmail.trim().isEmpty()) {
            errors.append("Email відсутній; ");
            isValid = false;
        }

        if (data.startAddress == null || data.startAddress.trim().isEmpty()) {
            errors.append("Початкова адреса відсутня; ");
            isValid = false;
        }

        if (data.endAddress == null || data.endAddress.trim().isEmpty()) {
            errors.append("Кінцева адреса відсутня; ");
            isValid = false;
        }

        if (data.purpose == null || data.purpose.trim().isEmpty()) {
            errors.append("Мета поїздки відсутня; ");
            isValid = false;
        }

        data.errorMessage = errors.toString();
        return isValid;
    }

    private static void logParsingResults(ParsedEmailData data) {
        logger.info("=== РЕЗУЛЬТАТ ПАРСИНГУ КОРПОРАТИВНОГО EMAIL ===");
        logger.info(" ПІБ: '{}'", data.requesterName);
        logger.info(" Email: '{}'", data.requesterEmail);
        logger.info(" Телефон: '{}'", data.requesterPhone);
        logger.info(" Початкова адреса: '{}'", data.startAddress);
        logger.info(" Кінцева адреса: '{}'", data.endAddress);
        logger.info(" Мета: '{}'", data.purpose);
        logger.info("Тип поїздки: {}", data.tripType);
        logger.info(" Може доставити: {}", data.canDriverDeliver);
        logger.info(" Початковий час: {}", data.plannedStartTime);
        logger.info(" Кінцевий час: {}", data.plannedEndTime);
        logger.info(" Валідний: {}", data.isValid);

        if (!data.isValid) {
            logger.warn("Помилки валідації: {}", data.errorMessage);
        }

        logger.info("===============================================");
    }

    public static class ParsedEmailData {
        public String requesterName;
        public String requesterEmail;
        public String requesterPhone;
        public String startAddress;
        public String endAddress;
        public TripType tripType;
        public LocalDateTime plannedStartTime;
        public LocalDateTime plannedEndTime;
        public String purpose;
        public String powerOfAttorney;
        public boolean canDriverDeliver;
        public boolean isValid;
        public String errorMessage;
        public boolean isUsed;  

        @Override
        public String toString() {
            return String.format("ParsedEmail{name='%s', email='%s', from='%s', to='%s', valid=%s, used=%s}",
                    requesterName, requesterEmail, startAddress, endAddress, isValid, isUsed);
        }
    }
}
