package com.met.trading.rules.products;

import com.met.trading.rules.RuleContext;
import com.met.trading.rules.RuleResult;
import com.met.trading.rules.TradingRule;
import java.math.BigDecimal;

public class RefinedProductsPricingAdjRule implements TradingRule {

    @Override
    public String getRuleName() {
        return "Refined products conversion";
    }

    @Override
    public RuleResult execute(RuleContext context) {
        if (context.getDeal() == null
                || !"PRODUCTS-HOU".equals(context.getDeal().getBook())) {
            return new RuleResult();
        }
        if (context.getInstrument() != null
                && "MT".equals(context.getInstrument().getUnit())) {
            context.addPriceAdjustment(new BigDecimal("0.15"));
        }
        return new RuleResult();
    }
}
