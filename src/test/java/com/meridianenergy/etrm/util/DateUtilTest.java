package com.meridianenergy.etrm.util;

import java.util.Date;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DateUtilTest {

    @Test
    public void formatsDate() {
        assertEquals("2024-06-28",
                DateUtil.format(DateUtil.parse("2024-06-28")));
    }

    @Test
    public void countsWholeDays() {
        Date start = DateUtil.parse("2024-06-01");
        Date end = DateUtil.parse("2024-06-04");
        assertEquals(3L, DateUtil.daysBetween(start, end));
    }

    @Test
    public void nullFormatsAsEmpty() {
        assertEquals("", DateUtil.format(null));
        assertTrue(DateUtil.parse("2024-01-01").before(
                DateUtil.parse("2024-01-02")));
    }
}
