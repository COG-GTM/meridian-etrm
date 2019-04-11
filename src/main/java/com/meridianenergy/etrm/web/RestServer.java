package com.meridianenergy.etrm.web;

import com.meridianenergy.etrm.core.AppContext;
import com.meridianenergy.etrm.deal.Deal;
import com.meridianenergy.etrm.refdata.Instrument;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import meridian.risk.PositionKeeper;

public class RestServer {

    private HttpServer server;

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new Handler());
        server.setExecutor(null);
        server.start();
        System.out.println("Meridian ETRM server listening on " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private static class Handler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String body;
            String contentType = "application/json";
            int status = 200;
            try {
                if ("/health".equals(path)) {
                    body = "OK";
                    contentType = "text/plain";
                } else if ("/positions".equals(path)) {
                    body = positions(false, null);
                // TODO: response format differs from /positions, fix when UI team is ready
                } else if (path.startsWith("/positions/")) {
                    body = positions(true, path.substring(11));
                } else if ("/trades".equals(path)) {
                    body = trades(exchange.getRequestURI().getQuery());
                } else if ("/risk/exposure".equals(path)) {
                    body = exposure();
                } else if (path.startsWith("/deals/")) {
                    body = deal(path.substring(7));
                } else {
                    body = "Not Found";
                    contentType = "text/plain";
                    status = 404;
                }
            } catch (Exception e) {
                body = "{\"error\":\"" + JsonUtil.quote(e.getMessage())
                        + "\"}";
                status = 500;
            }
            byte[] bytes = body.getBytes(Charset.forName("UTF-8"));
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        }

        private String positions(boolean scoped, String book) {
            List<String> rows = new ArrayList<String>();
            List<PositionKeeper.Position> positions =
                    AppContext.getPositionKeeper().positions(
                            scoped ? book : null);
            for (PositionKeeper.Position position : positions) {
                Instrument instrument = AppContext.getRefDataDAO()
                        .findInstrument(position.instrumentId);
                Map<String, Object> values =
                        new LinkedHashMap<String, Object>();
                values.put("book", position.book);
                if (scoped) {
                    values.put("instrument_code",
                            instrument == null ? "" : instrument.getCode());
                    values.put("net_qty", position.netQty);
                } else {
                    values.put("instrumentCode",
                            instrument == null ? "" : instrument.getCode());
                    values.put("netQty", position.netQty);
                }
                rows.add(JsonUtil.object(values));
            }
            return scoped ? JsonUtil.array(rows)
                    : "{\"positions\":" + JsonUtil.array(rows) + "}";
        }

        private String trades(String query) {
            String counterparty = null;
            if (query != null && query.startsWith("counterparty=")) {
                counterparty = query.substring("counterparty=".length());
            }
            List<String> rows = new ArrayList<String>();
            for (Deal deal : AppContext.getDealService()
                    .listDeals(counterparty)) {
                Map<String, Object> values =
                        new LinkedHashMap<String, Object>();
                values.put("dealRef", deal.getDealRef());
                values.put("book", deal.getBook());
                values.put("side", deal.getSide());
                values.put("quantity", deal.getQuantity());
                values.put("price", deal.getPrice());
                rows.add(JsonUtil.object(values));
            }
            return "{\"status\":\"OK\",\"count\":" + rows.size()
                    + ",\"data\":" + JsonUtil.array(rows) + "}";
        }

        private String exposure() {
            List<String> rows = new ArrayList<String>();
            for (Map.Entry<String, java.math.BigDecimal> entry
                    : AppContext.getRiskReportService()
                    .exposureByCounterparty().entrySet()) {
                Map<String, Object> values =
                        new LinkedHashMap<String, Object>();
                values.put("counterparty", entry.getKey());
                values.put("exposure", entry.getValue());
                rows.add(JsonUtil.object(values));
            }
            return "{\"result\":{\"asOf\":"
                    + JsonUtil.quote(
                            AppContext.getRiskReportService().asOf())
                    + ",\"byCounterparty\":" + JsonUtil.array(rows)
                    + "}}";
        }

        private String deal(String ref) {
            Deal deal = AppContext.getDealService().findByRef(ref);
            if (deal == null) {
                return "{\"error\":\"not found\"}";
            }
            Map<String, Object> values =
                    new LinkedHashMap<String, Object>();
            values.put("dealRef", deal.getDealRef());
            values.put("tradeDate", deal.getTradeDate());
            values.put("valueDate", deal.getValueDate());
            values.put("tradeType", deal.getTradeType());
            values.put("side", deal.getSide());
            values.put("quantity", deal.getQuantity());
            values.put("price", deal.getPrice());
            values.put("book", deal.getBook());
            values.put("status", deal.getStatus());
            return JsonUtil.object(values);
        }
    }
}
