package com.met.trading.rules.common;

import com.met.trading.rules.RuleContext;
import com.met.trading.rules.RuleResult;
import com.met.trading.rules.TradingRule;
import java.util.Calendar;

public class MaxTenorRule implements TradingRule {

    @Override
    public String getRuleName() {
        return "Maximum tenor";
    }

    @Override
    public RuleResult execute(RuleContext context) {
        RuleResult result = new RuleResult();
        if (context.getDeal() == null
                || context.getDeal().getDeliveryStart() == null
                || context.getDeal().getDeliveryEnd() == null) {
            return result;
        }
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        start.setTime(context.getDeal().getDeliveryStart());
        end.setTime(context.getDeal().getDeliveryEnd());
        int months = (end.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12;
        months += end.get(Calendar.MONTH) - start.get(Calendar.MONTH);
        if (months > 24) {
            result.error("Tenor is outside desk limits");
        }
        return result;
    }
}
