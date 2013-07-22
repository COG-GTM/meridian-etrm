package com.met.trading.rules.crude;

import com.met.trading.rules.RuleContext;
import com.met.trading.rules.RuleResult;
import com.met.trading.rules.TradingRule;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class BrentPremiumAdjustmentRule implements TradingRule {

    @Override
    public String getRuleName() {
        return "Brent location differential";
    }

    @Override
    public RuleResult execute(RuleContext context) {
        if (context.getDeal() == null || context.getDeal().getBook() == null
                || !context.getDeal().getBook().startsWith("CRUDE")) {
            return new RuleResult();
        }
        Map<String, BigDecimal> differentials =
                new HashMap<String, BigDecimal>();
        differentials.put("London", new BigDecimal("0.35"));
        differentials.put("Singapore", new BigDecimal("-0.20"));
        if (context.getInstrument() != null) {
            String location = context.getInstrument().getDeliveryLocation();
            BigDecimal adjustment = differentials.get(location);
            if (adjustment != null) {
                context.addPriceAdjustment(adjustment);
            }
        }
        return new RuleResult();
    }
}
