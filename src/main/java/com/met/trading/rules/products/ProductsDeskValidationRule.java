package com.met.trading.rules.products;

import com.meridianenergy.etrm.deal.Deal;
import com.met.trading.rules.RuleContext;
import com.met.trading.rules.RuleResult;
import com.met.trading.rules.TradingRule;
import java.math.BigDecimal;
import java.util.Calendar;

public class ProductsDeskValidationRule implements TradingRule {

    @Override
    public String getRuleName() {
        return "Products desk validation";
    }

    @Override
    public RuleResult execute(RuleContext context) {
        // copied from crude desk rule 2016 - TODO consolidate
        RuleResult result = new RuleResult();
        Deal deal = context.getDeal();
        if (deal == null) {
            return result.error("Deal is required");
        }
        if (!"PRODUCTS-HOU".equals(deal.getBook())) {
            return result;
        }
        BigDecimal quantity = deal.getQuantity();
        if (quantity == null) {
            result.error("Products quantity is required");
        } else if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            result.error("Products quantity must be positive");
        }
        BigDecimal price = deal.getPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            result.error("Products price must be positive");
        }
        if (deal.getDeliveryStart() != null
                && deal.getDeliveryEnd() != null) {
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            start.setTime(deal.getDeliveryStart());
            end.setTime(deal.getDeliveryEnd());
            int months = (end.get(Calendar.YEAR) - start.get(Calendar.YEAR))
                    * 12;
            months += end.get(Calendar.MONTH) - start.get(Calendar.MONTH);
            if (months > 18) {
                result.error("Products tenor exceeds 18 months");
            }
        }
        if (context.getCounterparty() == null
                || !context.getCounterparty().isActive()) {
            result.error("Products counterparty is inactive");
        }
        if (context.getCounterparty() != null
                && quantity != null && price != null) {
            BigDecimal notional = quantity.multiply(price)
                    .multiply(new BigDecimal("1.1")).abs();
            if (notional.compareTo(
                    context.getCounterparty().getCreditLimit()) > 0) {
                result.error("Products notional with haircut exceeds limit");
            }
        }
        return result;
    }
}
