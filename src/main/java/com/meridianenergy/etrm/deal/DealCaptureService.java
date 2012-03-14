package com.meridianenergy.etrm.deal;

import com.meridianenergy.etrm.core.DbConnectionManager;
import com.meridianenergy.etrm.refdata.Counterparty;
import com.meridianenergy.etrm.refdata.Instrument;
import com.meridianenergy.etrm.refdata.RefDataDAO;
import com.meridianenergy.etrm.settlement.ConfirmationDispatcher;
import com.meridianenergy.etrm.util.DateUtil;
import com.meridianenergy.etrm.util.NumberFmt;
import com.met.trading.rules.RuleContext;
import com.met.trading.rules.RuleEngine;
import com.met.trading.rules.RuleResult;
import com.met.trading.rules.TradingRule;
import com.met.trading.rules.common.RefLookup;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DealCaptureService {

    private static final Logger LOG =
            Logger.getLogger(DealCaptureService.class.getName());

    private final RefDataDAO refData = new RefDataDAO();
    private final RuleEngine rules = new RuleEngine();
    private final ConfirmationDispatcher confirmations =
            new ConfirmationDispatcher();
    private final RefLookup lookup = new RefLookup() {
        @Override
        public Counterparty counterparty(int id) {
            return refData.findCounterparty(id);
        }

        @Override
        public Instrument instrument(int id) {
            return refData.findInstrument(id);
        }
    };

    public Deal capture(Deal deal) {
        RuleResult validation = validate(deal);
        if (!validation.isOk()) {
            throw new IllegalArgumentException(validation.getWarnings().toString());
        }

        BigDecimal adjustment = adjustPrice(deal);
        if (deal.getPrice() != null) {
            deal.setPrice(deal.getPrice().add(adjustment));
        }
        deal.setStatus("VALIDATED");
        persist(deal);
        audit(deal, "CAPTURE");
        confirmations.dispatch(deal);
        LOG.info("Captured " + deal.getDealRef());
        return deal;
    }

    public RuleResult validate(Deal deal) {
        RuleResult result = new RuleResult();
        result.merge(inlineValidation(deal));
        result.merge(rules.run("validation", deal, lookup));

        if (deal != null && deal.getBook() != null
                && !"CRUDE-LDN".equals(deal.getBook())
                && !"PRODUCTS-HOU".equals(deal.getBook())) {
            result.warning("Unknown book code");
        }
        return result;
    }

    public BigDecimal adjustPrice(Deal deal) {
        RuleContext context = new RuleContext(deal, lookup);
        for (TradingRule rule : rules.load("pricing")) {
            rule.execute(context);
        }
        return context.getPriceAdjustment();
    }

    private RuleResult inlineValidation(Deal deal) {
        RuleResult result = new RuleResult();
        if (deal == null) {
            return result.error("Deal is required");
        }
        if (deal.getTradeDate() == null) {
            result.error("Trade date is required");
        }
        if (deal.getValueDate() == null) {
            result.error("Value date is required");
        }
        checkQuantity(deal, result);
        checkDates(deal, result);
        checkDeliveryWindow(deal, result);
        if (deal.getTradeType() == null) {
            result.error("Trade type is required");
        }
        if (deal.getSide() == null) {
            result.error("Side is required");
        }
        return result;
    }

    private void checkQuantity(Deal deal, RuleResult result) {
        if (deal.getQuantity() == null) {
            result.error("Quantity is required");
            return;
        }
        if (deal.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            result.error("Quantity must be positive");
        }
        if (deal.getQuantity().scale() > 4) {
            result.warning("Quantity has more than four decimal places");
        }
    }

    private void checkDates(Deal deal, RuleResult result) {
        if (deal.getTradeDate() == null || deal.getValueDate() == null) {
            return;
        }
        if (deal.getTradeDate().before(deal.getValueDate())) {
            result.warning("Value date is after trade date");
        }
        if (deal.getTradeDate().after(new Date())) {
            result.warning("Trade date is in the future");
        }
    }

    private void checkDeliveryWindow(Deal deal, RuleResult result) {
        if (deal.getDeliveryStart() == null || deal.getDeliveryEnd() == null) {
            result.error("Delivery window is required");
            return;
        }
        if (deal.getDeliveryEnd().before(deal.getDeliveryStart())) {
            result.error("Delivery end precedes delivery start");
        }
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        start.setTime(deal.getDeliveryStart());
        end.setTime(deal.getDeliveryEnd());
        int months = (end.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12;
        months += end.get(Calendar.MONTH) - start.get(Calendar.MONTH);
        if (months > 36) {
            result.warning("Delivery window exceeds standard booking horizon");
        }
    }

    private void persist(Deal deal) {
        String sql = "INSERT INTO deal(deal_ref,trade_date,value_date,"
                + "delivery_start,delivery_end,trade_type,side,quantity,price,"
                + "instrument_id,counterparty_id,book,trader,status,financial) "
                + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {
            int index = 1;
            statement.setString(index++, deal.getDealRef());
            statement.setDate(index++, sqlDate(deal.getTradeDate()));
            statement.setDate(index++, sqlDate(deal.getValueDate()));
            statement.setDate(index++, sqlDate(deal.getDeliveryStart()));
            statement.setDate(index++, sqlDate(deal.getDeliveryEnd()));
            statement.setString(index++, deal.getTradeType().name());
            statement.setString(index++, deal.getSide().name());
            statement.setBigDecimal(index++, deal.getQuantity());
            statement.setBigDecimal(index++, deal.getPrice());
            statement.setInt(index++, deal.getInstrumentId());
            statement.setInt(index++, deal.getCounterpartyId());
            statement.setString(index++, deal.getBook());
            statement.setString(index++, deal.getTrader());
            statement.setString(index++, deal.getStatus());
            statement.setBoolean(index++, deal.isFinancial());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    deal.setId(keys.getInt(1));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to persist deal", e);
        }
    }

    public Deal amendDeal(Deal original, Deal replacement) {
        if (original == null || replacement == null) {
            throw new IllegalArgumentException("Both deals are required");
        }
        updateStatus(original.getId(), "AMENDED");
        replacement.setDealRef(nextDealRef(replacement.getTradeDate()));
        replacement.setStatus("VALIDATED");
        persist(replacement);
        audit(original, "AMENDED");
        audit(replacement, "AMENDMENT");
        return replacement;
    }

    public void updateStatus(int dealId, String status) {
        String sql = "UPDATE deal SET status=? WHERE id=?";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, dealId);
            statement.executeUpdate();
            Deal deal = findById(dealId);
            if (deal != null) {
                audit(deal, "STATUS-" + status);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to update deal status", e);
        }
    }

    public void cancel(Deal deal) {
        if (deal == null) {
            return;
        }
        updateStatus(deal.getId(), "CANCELLED");
        LOG.warning("Cancelled " + deal.getDealRef());
    }

    public Deal findByRef(String ref) {
        String sql = baseSql() + " WHERE deal_ref=?";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ref);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to find deal", e);
        }
    }

    public Deal findById(int id) {
        String sql = baseSql() + " WHERE id=?";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to find deal", e);
        }
    }

    public List<Deal> listDeals() {
        return queryDeals(baseSql() + " ORDER BY id", new ArrayList<Object>());
    }

    public List<Deal> listDeals(String counterpartyCode) {
        String sql = baseSql()
                + " WHERE counterparty_id=(SELECT id FROM counterparty "
                + "WHERE short_code=?) ORDER BY id";
        List<Object> parameters = new ArrayList<Object>();
        parameters.add(counterpartyCode);
        return queryDeals(sql, parameters);
    }

    public List<Deal> listByBook(String book) {
        String sql = baseSql() + " WHERE book=? ORDER BY id";
        List<Object> parameters = new ArrayList<Object>();
        parameters.add(book);
        return queryDeals(sql, parameters);
    }

    public List<Deal> listByTrader(String trader) {
        String sql = baseSql() + " WHERE trader=? ORDER BY trade_date,id";
        List<Object> parameters = new ArrayList<Object>();
        parameters.add(trader);
        return queryDeals(sql, parameters);
    }

    public List<Deal> listBackdated() {
        String sql = baseSql()
                + " WHERE value_date<trade_date ORDER BY value_date";
        return queryDeals(sql, new ArrayList<Object>());
    }

    public List<Deal> search(Map<String, Object> criteria) {
        StringBuffer sql = new StringBuffer(baseSql());
        List<Object> parameters = new ArrayList<Object>();
        boolean hasWhere = false;
        for (Map.Entry<String, Object> entry : criteria.entrySet()) {
            if (!isSearchColumn(entry.getKey())) {
                continue;
            }
            sql.append(hasWhere ? " AND " : " WHERE ");
            sql.append(entry.getKey()).append("=?");
            parameters.add(entry.getValue());
            hasWhere = true;
        }
        sql.append(" ORDER BY id");
        return queryDeals(sql.toString(), parameters);
    }

    private boolean isSearchColumn(String name) {
        return "deal_ref".equals(name)
                || "book".equals(name)
                || "trader".equals(name)
                || "status".equals(name)
                || "trade_type".equals(name)
                || "side".equals(name)
                || "instrument_id".equals(name)
                || "counterparty_id".equals(name);
    }

    private List<Deal> queryDeals(String sql, List<Object> parameters) {
        List<Deal> deals = new ArrayList<Deal>();
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < parameters.size(); index++) {
                statement.setObject(index + 1, parameters.get(index));
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    deals.add(map(result));
                }
            }
            return deals;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to query deals", e);
        }
    }

    private String baseSql() {
        return "SELECT id,deal_ref,trade_date,value_date,delivery_start,"
                + "delivery_end,trade_type,side,quantity,price,instrument_id,"
                + "counterparty_id,book,trader,status,financial FROM deal";
    }

    private Deal map(ResultSet result) throws Exception {
        Deal deal = new Deal();
        deal.setId(result.getInt("id"));
        deal.setDealRef(result.getString("deal_ref"));
        deal.setTradeDate(result.getDate("trade_date"));
        deal.setValueDate(result.getDate("value_date"));
        deal.setDeliveryStart(result.getDate("delivery_start"));
        deal.setDeliveryEnd(result.getDate("delivery_end"));
        deal.setTradeType(TradeType.valueOf(result.getString("trade_type")));
        deal.setSide(Side.valueOf(result.getString("side")));
        deal.setQuantity(result.getBigDecimal("quantity"));
        deal.setPrice(result.getBigDecimal("price"));
        deal.setInstrumentId(result.getInt("instrument_id"));
        deal.setCounterpartyId(result.getInt("counterparty_id"));
        deal.setBook(result.getString("book"));
        deal.setTrader(result.getString("trader"));
        deal.setStatus(result.getString("status"));
        deal.setFinancial(result.getBoolean("financial"));
        return deal;
    }

    private java.sql.Date sqlDate(Date value) {
        return value == null ? null : new java.sql.Date(value.getTime());
    }

    public String formatTicket(Deal deal) {
        StringBuffer ticket = new StringBuffer();
        ticket.append("MERIDIAN ENERGY TRADING DEAL TICKET\n");
        ticket.append("----------------------------------\n");
        ticket.append("Deal Ref : ").append(deal.getDealRef()).append('\n');
        ticket.append("Trade Dt : ").append(DateUtil.format(deal.getTradeDate()))
                .append('\n');
        ticket.append("Value Dt : ").append(DateUtil.format(deal.getValueDate()))
                .append('\n');
        ticket.append("Type     : ").append(deal.getTradeType()).append(" / ")
                .append(deal.getSide()).append('\n');
        ticket.append("Quantity : ").append(NumberFmt.quantity(deal.getQuantity()))
                .append('\n');
        ticket.append("Price    : ").append(formatMoney(deal.getPrice()))
                .append('\n');
        ticket.append("Book     : ").append(deal.getBook()).append('\n');
        ticket.append("Trader   : ").append(deal.getTrader()).append('\n');
        ticket.append("Status   : ").append(deal.getStatus()).append('\n');
        return ticket.toString();
    }

    public String formatTicketHtml(Deal deal) {
        StringBuffer html = new StringBuffer();
        html.append("<html><body><h2>Deal Ticket</h2><table>");
        html.append(row("Deal Ref", deal.getDealRef()));
        html.append(row("Trade Date", DateUtil.format(deal.getTradeDate())));
        html.append(row("Value Date", DateUtil.format(deal.getValueDate())));
        html.append(row("Trade Type", String.valueOf(deal.getTradeType())));
        html.append(row("Side", String.valueOf(deal.getSide())));
        html.append(row("Quantity", NumberFmt.quantity(deal.getQuantity())));
        html.append(row("Price", formatMoney(deal.getPrice())));
        html.append(row("Book", deal.getBook()));
        html.append(row("Trader", deal.getTrader()));
        html.append("</table></body></html>");
        return html.toString();
    }

    private String row(String label, String value) {
        return "<tr><td>" + label + "</td><td>" + value + "</td></tr>";
    }

    public String toCsvLine(Deal deal) {
        StringBuffer csv = new StringBuffer();
        csv.append(csvValue(deal.getDealRef())).append(',');
        csv.append(csvValue(DateUtil.format(deal.getTradeDate()))).append(',');
        csv.append(csvValue(DateUtil.format(deal.getValueDate()))).append(',');
        csv.append(csvValue(String.valueOf(deal.getTradeType()))).append(',');
        csv.append(csvValue(String.valueOf(deal.getSide()))).append(',');
        csv.append(deal.getQuantity()).append(',');
        csv.append(deal.getPrice()).append(',');
        csv.append(csvValue(deal.getBook())).append(',');
        csv.append(csvValue(deal.getTrader()));
        return csv.toString();
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    public BigDecimal notional(Deal deal) {
        if (deal == null || deal.getQuantity() == null
                || deal.getPrice() == null) {
            return BigDecimal.ZERO;
        }
        return deal.getQuantity().multiply(deal.getPrice());
    }

    public String nextDealRef(Date tradeDate) {
        String sql = "SELECT MAX(deal_ref) FROM deal WHERE deal_ref LIKE ?";
        String year = new java.text.SimpleDateFormat("yyyy").format(tradeDate);
        String prefix = "MET-" + year + "-%";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, prefix);
            try (ResultSet result = statement.executeQuery()) {
                int next = 1;
                if (result.next() && result.getString(1) != null) {
                    String current = result.getString(1);
                    next = Integer.parseInt(current.substring(current.length() - 5))
                            + 1;
                }
                return String.format("MET-%s-%05d", year, next);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate deal reference", e);
        }
    }

    private void audit(Deal deal, String action) {
        // XXX pre-2018 book codes still appear in deal_audit
        String sql = "INSERT INTO deal_audit(deal_id,deal_ref,action,"
                + "changed_at,details) VALUES(?,?,?,?,?)";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, deal.getId());
            statement.setString(2, deal.getDealRef());
            statement.setString(3, action);
            statement.setTimestamp(4,
                    new java.sql.Timestamp(System.currentTimeMillis()));
            statement.setString(5, summary(deal));
            statement.executeUpdate();
        } catch (Exception e) {
            LOG.warning("Audit write failed for " + deal.getDealRef());
        }
    }

    public void dispatchConfirmation(Deal deal) {
        confirmations.dispatch(deal);
    }

    public List<String> auditHistory(String dealRef) {
        List<String> history = new ArrayList<String>();
        String sql = "SELECT action,changed_at,details FROM deal_audit "
                + "WHERE deal_ref=? ORDER BY changed_at";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dealRef);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    StringBuilder line = new StringBuilder();
                    line.append(result.getTimestamp(2)).append(' ');
                    line.append(result.getString(1)).append(' ');
                    line.append(result.getString(3));
                    history.add(line.toString());
                }
            }
            return history;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read deal audit", e);
        }
    }

    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM deal WHERE status=?";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to count deals", e);
        }
    }

    public String formatAuditLine(Deal deal, String action) {
        StringBuilder line = new StringBuilder();
        line.append(DateUtil.format(new Date())).append(" | ");
        line.append(action).append(" | ");
        line.append(deal == null ? "" : deal.getDealRef()).append(" | ");
        line.append(deal == null ? "" : status(deal));
        return line.toString();
    }

    public String instrumentCode(Deal deal) {
        Instrument instrument = refData.findInstrument(deal.getInstrumentId());
        return instrument == null ? "" : instrument.getCode();
    }

    public String counterpartyCode(Deal deal) {
        Counterparty counterparty =
                refData.findCounterparty(deal.getCounterpartyId());
        return counterparty == null ? "" : counterparty.getShortCode();
    }

    public String summary(Deal deal) {
        return deal.getDealRef() + " " + instrumentCode(deal) + " "
                + deal.getQuantity() + " @ " + formatMoney(deal.getPrice());
    }

    public String status(Deal deal) {
        return deal == null ? "MISSING" : deal.getStatus();
    }

    public static String formatMoney(BigDecimal value) {
        return value == null ? "0.00" : NumberFmt.money(value);
    }
}
