package com.example.fuelmanagementapp.utils;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {

    private static final String TAG = "DateUtils";

    private static final String[] API_DATE_FORMATS = {
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",   
            "yyyy-MM-dd'T'HH:mm:ss.SSS",      
            "yyyy-MM-dd'T'HH:mm:ss",           
            "yyyy-MM-dd HH:mm:ss.SSSSSS",     
            "yyyy-MM-dd HH:mm:ss.SSS",         
            "yyyy-MM-dd HH:mm:ss",             
            "yyyy-MM-dd"                      
    };

    private static final String DISPLAY_DATE_FORMAT = "dd.MM.yyyy";
    private static final String DISPLAY_TIME_FORMAT = "HH:mm";
    private static final String DISPLAY_DATETIME_FORMAT = "dd.MM HH:mm";
    private static final String DISPLAY_FULL_DATETIME_FORMAT = "dd.MM.yyyy HH:mm";

    private static final Locale DEFAULT_LOCALE = new Locale("uk", "UA"); 

    private static final SimpleDateFormat displayDateFormatter =
            new SimpleDateFormat(DISPLAY_DATE_FORMAT, DEFAULT_LOCALE);
    private static final SimpleDateFormat displayTimeFormatter =
            new SimpleDateFormat(DISPLAY_TIME_FORMAT, DEFAULT_LOCALE);
    private static final SimpleDateFormat displayDateTimeFormatter =
            new SimpleDateFormat(DISPLAY_DATETIME_FORMAT, DEFAULT_LOCALE);
    private static final SimpleDateFormat displayFullDateTimeFormatter =
            new SimpleDateFormat(DISPLAY_FULL_DATETIME_FORMAT, DEFAULT_LOCALE);

    public static Date parseApiDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        String cleanedDateString = dateString.trim();

        if (cleanedDateString.endsWith("Z")) {
            cleanedDateString = cleanedDateString.substring(0, cleanedDateString.length() - 1);
        } else if (cleanedDateString.matches(".*[+-]\\d{2}:?\\d{2}$")) {
            cleanedDateString = cleanedDateString.replaceAll("[+-]\\d{2}:?\\d{2}$", "");
        }

        for (String format : API_DATE_FORMATS) {
            try {
                SimpleDateFormat formatter = new SimpleDateFormat(format, DEFAULT_LOCALE);
                formatter.setTimeZone(TimeZone.getDefault()); 
                Date date = formatter.parse(cleanedDateString);
                Log.d(TAG, "Successfully parsed date: " + dateString + "with format: " + format);
                return date;
            } catch (ParseException e) {
                Log.v(TAG, "Failed to parse with format " + format + ": " + e.getMessage());
            }
        }

        Log.w(TAG, "Failed to parse date string: " + dateString);
        return null;
    }

    public static String formatDisplayDate(String apiDateString) {
        Date date = parseApiDate(apiDateString);
        if (date == null) {
            Log.w(TAG, "Cannot format display date, parsing failed for: " + apiDateString);
            return "";
        }
        return displayDateFormatter.format(date);
    }

    public static String formatDisplayTime(String apiDateString) {
        Date date = parseApiDate(apiDateString);
        if (date == null) {
            Log.w(TAG, "Cannot format display time, parsing failed for: " + apiDateString);
            return "";
        }
        return displayTimeFormatter.format(date);
    }

    public static String formatDisplayDateTime(String apiDateString) {
        Date date = parseApiDate(apiDateString);
        if (date == null) {
            Log.w(TAG, "Cannot format display datetime, parsing failed for: " + apiDateString);
            return "";
        }
        try {
            return displayDateTimeFormatter.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting datetime", e);
            return displayDateFormatter.format(date); 
        }
    }

    public static String formatFullDisplayDateTime(String apiDateString) {
        Date date = parseApiDate(apiDateString);
        if (date == null) {
            Log.w(TAG, "Cannot format full display datetime, parsing failed for: " + apiDateString);
            return "";
        }
        return displayFullDateTimeFormatter.format(date);
    }

    public static boolean isDatePast(String apiDateString) {
        Date date = parseApiDate(apiDateString);
        if (date == null) {
            Log.w(TAG, "Cannot check if date is past, parsing failed for: " + apiDateString);
            return false;
        }
        return date.before(new Date());
    }

    public static boolean isDateFuture(String apiDateString) {
        Date date = parseApiDate(apiDateString);
        if (date == null) {
            return false;
        }
        return date.after(new Date());
    }

    public static String getCurrentApiDateTime() {
        SimpleDateFormat apiFormatter = new SimpleDateFormat(API_DATE_FORMATS[2], DEFAULT_LOCALE);
        return apiFormatter.format(new Date());
    }

    public static long getMinutesDifference(String startDateString, String endDateString) {
        Date startDate = parseApiDate(startDateString);
        Date endDate = parseApiDate(endDateString);

        if (startDate == null || endDate == null) {
            Log.w(TAG, "Cannot calculate minutes difference, parsing failed");
            return 0;
        }

        long diffInMillis = endDate.getTime() - startDate.getTime();
        return diffInMillis / (60 * 1000); 
    }

    public static long getHoursDifference(String startDateString, String endDateString) {
        return getMinutesDifference(startDateString, endDateString) / 60;
    }

    public static String formatDuration(String startDateString, String endDateString) {
        long minutes = getMinutesDifference(startDateString, endDateString);
        if (minutes <= 0) {
            return "0 мин";
        }

        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;

        if (hours > 0) {
            return hours + "ч " + remainingMinutes + "мин";
        } else {
            return remainingMinutes + "мин";
        }
    }

    public static boolean isValidDateString(String dateString) {
        return parseApiDate(dateString) != null;
    }

    public static String getRelativeDateString(String apiDateString) {
        Date date = parseApiDate(apiDateString);
        if (date == null) {
            return "";
        }

        Date now = new Date();
        long diffInMillis = date.getTime() - now.getTime();
        long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);

        if (diffInDays == 0) {
            return "Сегодня " + formatDisplayTime(apiDateString);
        } else if (diffInDays == 1) {
            return "Завтра " + formatDisplayTime(apiDateString);
        } else if (diffInDays == -1) {
            return "Вчера " + formatDisplayTime(apiDateString);
        } else {
            return formatDisplayDateTime(apiDateString);
        }
    }
}
