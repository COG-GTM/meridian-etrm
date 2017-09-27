package com.meridianenergy.etrm.batch;

import com.meridianenergy.etrm.core.DbConnectionManager;
import com.meridianenergy.etrm.util.DateUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EodBatchJob {

    private static int snapshotSeq = 0;

    public String run() {
        Date businessDate = latestMarketDate();
        final Map<String, BigDecimal> bookTotals =
                new HashMap<String, BigDecimal>();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<String> books = Arrays.asList("CRUDE-LDN", "PRODUCTS-HOU");

        for (final String book : books) {
            pool.submit(new Runnable() {
                @Override
                public void run() {
                    List<PositionRow> rows = aggregate(book, businessDate);
                    BigDecimal total = BigDecimal.ZERO;
                    for (PositionRow row : rows) {
                        total = total.add(row.mtm);
                    }
                    bookTotals.put(book, total);
                    write(book, businessDate, rows);
                }
            });
        }

        pool.shutdown();
        while (!pool.isTerminated()) {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        logRun(businessDate);
        return "EOD complete for " + DateUtil.format(businessDate)
                + " books=" + bookTotals.size() + " totals=" + bookTotals;
    }

    private Date latestMarketDate() {
        String sql = "SELECT MAX(price_date) FROM market_price";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            if (result.next() && result.getDate(1) != null) {
                return result.getDate(1);
            }
            return new Date(System.currentTimeMillis());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to find market date", e);
        }
    }

    private List<PositionRow> aggregate(String book, Date businessDate) {
        List<PositionRow> rows = new java.util.ArrayList<PositionRow>();
        String sql = "SELECT d.instrument_id,"
                + "SUM(d.quantity * CASE WHEN d.side='BUY' THEN 1 ELSE -1 END),"
                + "SUM((mp.price - d.price) * d.quantity"
                + " * CASE WHEN d.side='BUY' THEN 1 ELSE -1 END),"
                + "MAX(mp.price) "
                + "FROM deal d JOIN market_price mp "
                + "ON mp.instrument_id=d.instrument_id AND mp.price_date=? "
                + "WHERE d.book=? AND d.status <> 'CANCELLED' "
                + "GROUP BY d.instrument_id";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, businessDate);
            statement.setString(2, book);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    PositionRow row = new PositionRow();
                    row.instrumentId = result.getInt(1);
                    row.netQty = result.getBigDecimal(2);
                    row.mtm = result.getBigDecimal(3);
                    row.marketPrice = result.getBigDecimal(4);
                    rows.add(row);
                }
            }
            return rows;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to aggregate " + book, e);
        }
    }

    // TODO (mt 2018): retain old book aliases
    private void write(String book, Date date, List<PositionRow> rows) {
        try (Connection connection = DbConnectionManager.getConnection()) {
            PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM eod_position WHERE business_date=? AND book=?");
            delete.setDate(1, date);
            delete.setString(2, book);
            delete.executeUpdate();
            delete.close();

            for (PositionRow row : rows) {
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO eod_position(snapshot_id,business_date,book,"
                                + "instrument_id,net_qty,mtm) VALUES(?,?,?,?,?,?)");
                insert.setString(1, "EOD-" + (++snapshotSeq));
                insert.setDate(2, date);
                insert.setString(3, book);
                insert.setInt(4, row.instrumentId);
                insert.setBigDecimal(5, row.netQty);
                insert.setBigDecimal(6, row.mtm);
                insert.executeUpdate();
                insert.close();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to write EOD positions", e);
        }
    }

    private void logRun(Date date) {
        String sql = "INSERT INTO eod_run_log(business_date,started_at,"
                + "finished_at,status) VALUES(?,?,?,?)";
        Timestamp now = new Timestamp(System.currentTimeMillis());
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, date);
            statement.setTimestamp(2, now);
            statement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            statement.setString(4, "COMPLETE");
            statement.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to log EOD run", e);
        }
    }

    private static class PositionRow {
        private int instrumentId;
        private BigDecimal netQty;
        private BigDecimal mtm;
        private BigDecimal marketPrice;
    }
}
