package com.met.trading.rules.common;

import com.meridianenergy.etrm.refdata.Counterparty;
import com.meridianenergy.etrm.refdata.Instrument;

public interface RefLookup {

    Counterparty counterparty(int id);

    Instrument instrument(int id);
}
