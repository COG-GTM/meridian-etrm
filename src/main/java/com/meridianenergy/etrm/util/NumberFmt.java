package com.meridianenergy.etrm.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public final class NumberFmt {

    private static final DecimalFormat MONEY =
            new DecimalFormat("#,##0.00");
    private static final DecimalFormat QUANTITY =
            new DecimalFormat("#,##0.0000");

    private NumberFmt() {
    }

    public static String money(BigDecimal value) {
        return value == null ? "0.00" : MONEY.format(value);
    }

    public static String quantity(BigDecimal value) {
        return value == null ? "0.0000" : QUANTITY.format(value);
    }
}
