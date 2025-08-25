package com.voicechat.client.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateHandler {

    public static String transformDate(LocalDateTime localDateTime) {
        LocalDateTime now = LocalDateTime.now();
        if(now.getYear() == localDateTime.getYear()) {
            if (now.minusDays(1).getDayOfYear() == localDateTime.getDayOfYear()){
                return "Yesterday " + localDateTime.getHour() + ":" + localDateTime.getMinute();
            }
            if (now.getDayOfYear() == localDateTime.getDayOfYear()) {
                return "Today " + localDateTime.getHour() + ":" + localDateTime.getMinute();
            }
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return localDateTime.format(formatter);
    }
}
