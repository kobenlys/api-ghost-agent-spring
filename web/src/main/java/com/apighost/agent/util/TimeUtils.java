package com.apighost.agent.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeUtils {

    public static String getNow(){
        return convertFormat(LocalDateTime.now());
    }

    public static String convertFormat(LocalDateTime localDateTime) {
        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
    }

    public static String convertFormat(Long timeMs) {
        LocalDateTime dateTime = Instant.ofEpochMilli(timeMs)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
    }
}
