package com.meridianenergy.etrm;

import com.meridianenergy.etrm.batch.EodBatchJob;
import com.meridianenergy.etrm.core.AppContext;
import com.meridianenergy.etrm.core.SchemaBootstrap;
import com.meridianenergy.etrm.deal.Deal;
import com.meridianenergy.etrm.web.RestServer;

public class Main {

    public static void main(String[] args) throws Exception {
        SchemaBootstrap.initialize();

        String command = args.length == 0 ? "server" : args[0];
        if ("server".equals(command)) {
            int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
            RestServer server = new RestServer();
            server.start(port);
            Thread.currentThread().join();
        } else if ("eod".equals(command)) {
            System.out.println(new EodBatchJob().run());
        } else if ("risk-report".equals(command)) {
            System.out.println(AppContext.getRiskReportService().textReport());
        } else if ("invoice".equals(command) && args.length > 1) {
            Deal deal = AppContext.getDealService().findByRef(args[1]);
            if (deal == null) {
                System.out.println("Deal not found");
            } else {
                System.out.println(AppContext.getInvoiceGenerator().generate(deal));
            }
        } else if ("list-trades".equals(command)) {
            for (Deal deal : AppContext.getDealService().listDeals()) {
                System.out.println(AppContext.getDealService().summary(deal));
            }
        } else {
            System.out.println("Usage: server [port] | eod | risk-report | "
                    + "invoice <dealRef> | list-trades");
        }
    }
}
