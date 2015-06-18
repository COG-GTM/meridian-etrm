package com.meridianenergy.etrm.settlement;

import com.meridianenergy.etrm.core.DbConnectionManager;
import com.meridianenergy.etrm.deal.Deal;
import com.meridianenergy.etrm.util.DateUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SettlementEngine {

    public Settlement calculate(Deal deal) {
        BigDecimal notional = deal.getQuantity().multiply(deal.getPrice());
        if (deal.getValueDate() == null
                || !deal.getValueDate().before(deal.getTradeDate())) {
            return new Settlement(notional, BigDecimal.ZERO, 0L);
        }
        long days = DateUtil.daysBetween(
                deal.getValueDate(), deal.getTradeDate());
        BigDecimal interest = notional.multiply(rate())
                .multiply(BigDecimal.valueOf(days))
                .divide(new BigDecimal("360"), 8,
                        BigDecimal.ROUND_HALF_UP);
        return new Settlement(notional, interest, days);
    }

    private BigDecimal rate() {
        String sql = "SELECT config_value FROM settlement_config "
                + "WHERE config_key='accrued_interest_rate'";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getBigDecimal(1)
                    : new BigDecimal("0.045");
        } catch (Exception e) {
            return new BigDecimal("0.045");
        }
    }

    public BigDecimal settlementAmount(Deal deal) {
        Settlement settlement = calculate(deal);
        return settlement.amount.add(settlement.accruedInterest);
    }

    public static class Settlement {
        public BigDecimal amount;
        public BigDecimal accruedInterest;
        public long accruedDays;

        public Settlement(BigDecimal amount, BigDecimal accruedInterest,
                long accruedDays) {
            this.amount = amount;
            this.accruedInterest = accruedInterest;
            this.accruedDays = accruedDays;
        }
    }
}
