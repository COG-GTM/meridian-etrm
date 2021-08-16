package com.meridianenergy.etrm.deal;

import com.meridianenergy.etrm.TestDb;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DealCaptureServiceTest {

    private final DealCaptureService service = new DealCaptureService();

    @BeforeClass
    public static void beforeClass() {
        TestDb.initialize();
    }

    @Test
    public void listsSeedDeals() {
        assertEquals(25, service.listDeals().size());
    }

    @Test
    public void findsSeedDealByReference() {
        Deal deal = service.findByRef("MET-2024-00007");
        assertNotNull(deal);
        assertEquals("CRUDE-LDN", deal.getBook());
    }

    @Test
    public void ticketContainsTradeReference() {
        Deal deal = service.findByRef("MET-2024-00007");
        assertTrue(service.formatTicket(deal).contains("MET-2024-00007"));
    }

    @Test
    public void nullDealFailsValidation() {
        assertFalse(service.validate(null).isOk());
    }

    @Test
    public void searchCanFilterByBook() {
        Map<String, Object> criteria = new HashMap<String, Object>();
        criteria.put("book", "CRUDE-LDN");
        List<Deal> values = service.search(criteria);
        assertTrue(values.size() > 0);
        for (Deal value : values) {
            assertEquals("CRUDE-LDN", value.getBook());
        }
    }

    @Test
    public void notionalUsesQuantityAndPrice() {
        Deal deal = service.findByRef("MET-2024-00007");
        assertEquals(new BigDecimal("4704000.00000000"),
                service.notional(deal));
    }
}
