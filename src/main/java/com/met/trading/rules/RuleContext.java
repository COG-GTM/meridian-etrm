package com.met.trading.rules;

import com.meridianenergy.etrm.deal.Deal;
import com.meridianenergy.etrm.refdata.Counterparty;
import com.meridianenergy.etrm.refdata.Instrument;
import com.met.trading.rules.common.RefLookup;
import java.math.BigDecimal;

public class RuleContext {

    private final Deal deal;
    private final RefLookup lookup;
    private BigDecimal priceAdjustment = BigDecimal.ZERO;

    public RuleContext(Deal deal, RefLookup lookup) {
        this.deal = deal;
        this.lookup = lookup;
    }

    public Deal getDeal() {
        return deal;
    }

    public Instrument getInstrument() {
        return deal == null ? null : lookup.instrument(deal.getInstrumentId());
    }

    public Counterparty getCounterparty() {
        return deal == null ? null : lookup.counterparty(deal.getCounterpartyId());
    }

    public BigDecimal getPriceAdjustment() {
        return priceAdjustment;
    }

    public void addPriceAdjustment(BigDecimal value) {
        if (value != null) {
            priceAdjustment = priceAdjustment.add(value);
        }
    }
}
