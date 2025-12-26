package com.voicechat.client.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
}
