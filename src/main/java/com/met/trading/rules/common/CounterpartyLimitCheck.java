package com.met.trading.rules.common;

import com.met.trading.rules.RuleContext;
import com.met.trading.rules.RuleResult;
import com.met.trading.rules.TradingRule;
import java.math.BigDecimal;

public class CounterpartyLimitCheck implements TradingRule {

    @Override
    public String getRuleName() {
        return "Counterparty limit";
    }

    @Override
    public RuleResult execute(RuleContext context) {
        RuleResult result = new RuleResult();
        if (context.getDeal() == null || context.getCounterparty() == null) {
            return result;
        }
        BigDecimal quantity = context.getDeal().getQuantity();
        BigDecimal price = context.getDeal().getPrice();
        BigDecimal limit = context.getCounterparty().getCreditLimit();
        if (quantity == null || price == null || limit == null) {
            return result;
        }
        BigDecimal notional = quantity.multiply(price).abs();
        if (notional.compareTo(limit) > 0) {
            result.error("Credit limit exceeded");
        }
        return result;
    }
}
