package com.meridianenergy.etrm.settlement;

import java.util.logging.Logger;

public class FaxGatewayClient {

    private static final Logger LOG =
            Logger.getLogger(FaxGatewayClient.class.getName());

    private final String host;

    public FaxGatewayClient(String host) {
        this.host = host;
    }

    public void send(String tiffCoverPage) {
        LOG.info("Fax payload prepared for " + host + ": "
                + tiffCoverPage.length() + " bytes");
    }
}
