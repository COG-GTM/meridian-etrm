package com.meridianenergy.etrm.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class DateUtil {

    private static final SimpleDateFormat FORMAT =
            new SimpleDateFormat("yyyy-MM-dd");

    private DateUtil() {
    }

    public static Date parse(String value) {
        try {
            return FORMAT.parse(value);
        } catch (ParseException e) {
            throw new IllegalArgumentException(value, e);
        }
    }

    public static String format(Date value) {
        return value == null ? "" : FORMAT.format(value);
    }

    public static long daysBetween(Date a, Date b) {
        return (b.getTime() - a.getTime()) / 86400000L;
    }
}
