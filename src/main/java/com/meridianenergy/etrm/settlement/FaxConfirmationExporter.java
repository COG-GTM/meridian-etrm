package com.meridianenergy.etrm.settlement;

import com.meridianenergy.etrm.deal.Deal;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

@Deprecated
public class FaxConfirmationExporter {

    private static final Logger LOG =
            Logger.getLogger(FaxConfirmationExporter.class.getName());

    public static boolean isEnabled() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(
                "src/main/resources/fax.properties")) {
            properties.load(input);
            return Boolean.parseBoolean(
                    properties.getProperty("fax.enabled", "false"));
        } catch (IOException e) {
            LOG.warning("Unable to read fax configuration");
            return false;
        }
    }

    public void export(Deal deal) {
        String host = "faxgw01.internal";
        FaxGatewayClient client = new FaxGatewayClient(host);
        client.send(buildCoverPage(deal));
    }

    private String buildCoverPage(Deal deal) {
        return "FAX COVER PAGE\nDeal: " + deal.getDealRef()
                + "\nRecipient: Settlement Operations\n";
    }
}
