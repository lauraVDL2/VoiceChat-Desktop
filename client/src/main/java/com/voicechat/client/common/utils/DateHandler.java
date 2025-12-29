package com.voicechat.client.common.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DateHandler {

    public static String transformDate(LocalDateTime localDateTime) {
        LocalDateTime now = LocalDateTime.now();
        if(now.getYear() == localDateTime.getYear()) {
            if (now.minusDays(1).getDayOfYear() == localDateTime.getDayOfYear()){
                return "Yesterday " + formattedTime(localDateTime.getHour()) + ":" + formattedTime(localDateTime.getMinute());
            }
            if (now.getDayOfYear() == localDateTime.getDayOfYear()) {
                return "Today " + formattedTime(localDateTime.getHour()) + ":" + formattedTime(localDateTime.getMinute());
            }
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return localDateTime.format(formatter);
    }

    public static String formattedTime(int data) {
        String result = "";
        if (data < 10) {
            result = "0" + data;
        }
        else {
            result = data + "";
        }
        return result;
    }

    public static LocalDateTime toLocalDateTime(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS");

        // Parse the string into a LocalDateTime
        return LocalDateTime.parse(date, formatter);
    }

    public static LocalDate getBeginningOfTheWeek(LocalDate localDate) {
        return localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static LocalDate getEndOfTheWeek(LocalDate localDate) {
        return localDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
    }

    public static List<LocalDate> getDaysOfWeek(LocalDateTime dateTime, int weekLength) {
        LocalDate localDate = dateTime.toLocalDate();

        // Get the first day of the week
        LocalDate startOfWeek = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // Generate all days of the week
        List<LocalDate> daysOfWeek = new ArrayList<>();
        for (int i = 0; i < weekLength; i++) {
            daysOfWeek.add(startOfWeek.plusDays(i));
        }

        return daysOfWeek;
    }
}
