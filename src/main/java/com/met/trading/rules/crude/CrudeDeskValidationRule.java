package com.met.trading.rules.crude;

import com.meridianenergy.etrm.deal.Deal;
import com.meridianenergy.etrm.deal.TradeType;
import com.met.trading.rules.RuleContext;
import com.met.trading.rules.RuleResult;
import com.met.trading.rules.TradingRule;
import java.math.BigDecimal;
import java.util.Calendar;

public class CrudeDeskValidationRule implements TradingRule {

    @Override
    public String getRuleName() {
        return "Crude desk validation";
    }

    @Override
    public RuleResult execute(RuleContext context) {
        RuleResult result = new RuleResult();
        Deal deal = context.getDeal();
        if (deal == null) {
            return result.error("Deal is required");
        }
        if (deal.getBook() == null || !deal.getBook().startsWith("CRUDE")) {
            return result;
        }
        BigDecimal quantity = deal.getQuantity();
        if (quantity == null) {
            result.error("Crude quantity is required");
        } else if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            result.error("Crude quantity must be positive");
        }
        BigDecimal price = deal.getPrice();
        if (price == null) {
            result.error("Crude price is required");
        } else if (price.compareTo(BigDecimal.ZERO) <= 0) {
            if (deal.getTradeType() == TradeType.SWAP) {
                result.warning("Negative swap price accepted");
            } else {
                result.error("Crude price must be positive");
            }
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
            if (months > 24) {
                result.error("Crude tenor exceeds 24 months");
            }
        }
        if (context.getCounterparty() == null
                || !context.getCounterparty().isActive()) {
            result.error("Counterparty is inactive");
        }
        if (context.getCounterparty() != null
                && quantity != null && price != null) {
            BigDecimal notional = quantity.multiply(price).abs();
            if (notional.compareTo(
                    context.getCounterparty().getCreditLimit()) > 0) {
                result.error("Crude notional exceeds counterparty limit");
            }
        }
        return result;
    }
}
