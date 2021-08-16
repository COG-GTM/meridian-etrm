package com.meridianenergy.etrm.settlement;

import com.meridianenergy.etrm.TestDb;
import com.meridianenergy.etrm.deal.Deal;
import com.meridianenergy.etrm.deal.Side;
import com.meridianenergy.etrm.deal.TradeType;
import com.meridianenergy.etrm.util.DateUtil;
import java.math.BigDecimal;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SettlementEngineTest {

    @BeforeClass
    public static void beforeClass() {
        TestDb.initialize();
    }

    @Test
    public void currentValueDateHasNoAccruedInterest() {
        Deal deal = new Deal();
        deal.setTradeDate(DateUtil.parse("2024-06-28"));
        deal.setValueDate(DateUtil.parse("2024-06-28"));
        deal.setQuantity(new BigDecimal("100"));
        deal.setPrice(new BigDecimal("80"));
        deal.setTradeType(TradeType.SPOT);
        deal.setSide(Side.BUY);
        SettlementEngine.Settlement settlement =
                new SettlementEngine().calculate(deal);
        assertEquals(BigDecimal.ZERO, settlement.accruedInterest);
        assertEquals(0L, settlement.accruedDays);
    }
}
