package com.meridianenergy.etrm.settlement;

import com.meridianenergy.etrm.deal.Deal;
import java.util.logging.Logger;

public class ConfirmationDispatcher {

    private static final Logger LOG =
            Logger.getLogger(ConfirmationDispatcher.class.getName());

    public void dispatch(Deal deal) {
        if (deal == null) {
            return;
        }
        sendEmail(deal);
        if (FaxConfirmationExporter.isEnabled()) {
            FaxConfirmationExporter exporter = new FaxConfirmationExporter();
            exporter.export(deal);
        }
    }

    private void sendEmail(Deal deal) {
        LOG.info("Confirmation email queued for " + deal.getDealRef());
    }
}
