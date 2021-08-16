package com.meridianenergy.etrm.refdata;

import com.meridianenergy.etrm.TestDb;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RefDataDAOTest {

    private final RefDataDAO dao = new RefDataDAO();

    @BeforeClass
    public static void beforeClass() {
        TestDb.initialize();
    }

    @Test
    public void listsSixCounterparties() {
        assertEquals(6, dao.listCounterparties().size());
    }

    @Test
    public void findsCounterpartyByCode() {
        Counterparty value = dao.findCounterpartyByCode("NORD");
        assertNotNull(value);
        assertEquals("NORD", value.getShortCode());
    }

    @Test
    public void inactiveCounterpartyIsRetained() {
        List<Counterparty> values = dao.listCounterparties();
        boolean foundInactive = false;
        for (Counterparty value : values) {
            if (!value.isActive()) {
                foundInactive = true;
            }
        }
        assertTrue(foundInactive);
    }

    @Test
    public void listsEightInstruments() {
        assertEquals(8, dao.listInstruments().size());
    }

    @Test
    public void findsInstrumentByCode() {
        Instrument value = dao.findInstrumentByCode("BRENT_PHYS");
        assertNotNull(value);
        assertEquals("CRUDE", value.getCommodityType());
    }

    @Test
    public void missingInstrumentReturnsNull() {
        assertEquals(null, dao.findInstrumentByCode("MISSING"));
    }
}
