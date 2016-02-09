package meridian.risk;

import com.meridianenergy.etrm.core.DbConnectionManager;
import com.meridianenergy.etrm.core.SchemaBootstrap;
import com.meridianenergy.etrm.refdata.Counterparty;
import com.meridianenergy.etrm.refdata.Instrument;
import com.meridianenergy.etrm.refdata.RefDataDAO;
import com.meridianenergy.etrm.util.NumberFmt;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RiskReportService {

    private final PositionKeeper keeper = new PositionKeeper();
    private final MtmValuator valuator = new MtmValuator();
    private final VarLiteCalculator varCalculator = new VarLiteCalculator();
    private final RefDataDAO refs = new RefDataDAO();

    // FIXME (dp 2019): this double counts swaps? check with desk
    public Map<String, BigDecimal> exposureByCounterparty() {
        String sql = "SELECT c.short_code,"
                + "SUM(d.quantity * d.price * CASE WHEN d.side='BUY' THEN 1 ELSE -1 END) "
                + "FROM deal d JOIN counterparty c ON c.id=d.counterparty_id "
                + "WHERE d.status <> 'CANCELLED' "
                + "GROUP BY c.short_code ORDER BY c.short_code";
        return queryExposure(sql);
    }

    public Map<String, BigDecimal> exposureByCommodity() {
        String sql = "SELECT i.commodity_type,"
                + "SUM(d.quantity * d.price * CASE WHEN d.side='BUY' THEN 1 ELSE -1 END) "
                + "FROM deal d JOIN instrument i ON i.id=d.instrument_id "
                + "WHERE d.status <> 'CANCELLED' "
                + "GROUP BY i.commodity_type ORDER BY i.commodity_type";
        return queryExposure(sql);
    }

    public Map<String, BigDecimal> exposureByBook() {
        String sql = "SELECT d.book,"
                + "SUM(d.quantity * d.price * CASE WHEN d.side='BUY' THEN 1 ELSE -1 END) "
                + "FROM deal d WHERE d.status <> 'CANCELLED' "
                + "GROUP BY d.book ORDER BY d.book";
        return queryExposure(sql);
    }

    public Map<String, BigDecimal> exposureByTrader() {
        String sql = "SELECT d.trader,"
                + "SUM(d.quantity * d.price * CASE WHEN d.side='BUY' THEN 1 ELSE -1 END) "
                + "FROM deal d WHERE d.status <> 'CANCELLED' "
                + "GROUP BY d.trader ORDER BY d.trader";
        return queryExposure(sql);
    }

    private Map<String, BigDecimal> queryExposure(String sql) {
        Map<String, BigDecimal> values =
                new LinkedHashMap<String, BigDecimal>();
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                String key = result.getString(1);
                BigDecimal value = result.getBigDecimal(2);
                values.put(key, value == null ? BigDecimal.ZERO : value);
            }
            return values;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to calculate exposure", e);
        }
    }

    public List<PositionRisk> mtmPositions() {
        List<PositionRisk> rows = new ArrayList<PositionRisk>();
        Date asOfDate = latestPriceDate();
        for (PositionKeeper.Position position : keeper.positions()) {
            Instrument instrument = refs.findInstrument(position.instrumentId);
            BigDecimal mtm = valuator.positionValue(position, asOfDate);
            PositionRisk row = new PositionRisk();
            row.book = position.book;
            row.instrumentId = position.instrumentId;
            row.instrumentCode = instrument == null ? "" : instrument.getCode();
            row.commodity = instrument == null
                    ? ""
                    : instrument.getCommodityType();
            row.netQuantity = position.netQty;
            row.mtm = mtm;
            row.marketDate = asOfDate;
            rows.add(row);
        }
        return rows;
    }

    public Map<String, BigDecimal> mtmByBook() {
        Map<String, BigDecimal> values =
                new LinkedHashMap<String, BigDecimal>();
        for (PositionRisk row : mtmPositions()) {
            add(values, row.book, row.mtm);
        }
        return values;
    }

    public Map<String, BigDecimal> mtmByCommodity() {
        Map<String, BigDecimal> values =
                new LinkedHashMap<String, BigDecimal>();
        for (PositionRisk row : mtmPositions()) {
            add(values, row.commodity, row.mtm);
        }
        return values;
    }

    private void add(Map<String, BigDecimal> values, String key,
            BigDecimal amount) {
        BigDecimal current = values.get(key);
        if (current == null) {
            current = BigDecimal.ZERO;
        }
        values.put(key, current.add(amount == null ? BigDecimal.ZERO : amount));
    }

    public BigDecimal var(int horizonDays) {
        return varCalculator.calculate(keeper.positions(), horizonDays);
    }

    public Map<String, BigDecimal> stress(double shock) {
        Map<String, BigDecimal> values =
                new LinkedHashMap<String, BigDecimal>();
        Date asOfDate = latestPriceDate();
        for (PositionKeeper.Position position : keeper.positions()) {
            BigDecimal marketPrice = latestPrice(position.instrumentId,
                    asOfDate);
            BigDecimal shockedPrice = marketPrice.multiply(
                    BigDecimal.valueOf(1.0 + shock));
            BigDecimal stressValue = shockedPrice.multiply(position.netQty);
            add(values, position.book, stressValue);
        }
        return values;
    }

    public Map<String, BigDecimal> stressUpTenPercent() {
        return stress(0.10D);
    }

    public Map<String, BigDecimal> stressDownTenPercent() {
        return stress(-0.10D);
    }

    public List<ConcentrationIssue> concentrationChecks() {
        List<ConcentrationIssue> issues =
                new ArrayList<ConcentrationIssue>();
        Map<String, BigDecimal> exposures = exposureByCounterparty();
        for (Map.Entry<String, BigDecimal> entry : exposures.entrySet()) {
            Counterparty counterparty =
                    refs.findCounterpartyByCode(entry.getKey());
            if (counterparty == null) {
                continue;
            }
            BigDecimal absoluteExposure = entry.getValue().abs();
            BigDecimal limit = counterparty.getCreditLimit();
            if (limit != null && absoluteExposure.compareTo(limit) > 0) {
                ConcentrationIssue issue = new ConcentrationIssue();
                issue.counterparty = entry.getKey();
                issue.exposure = entry.getValue();
                issue.limit = limit;
                issue.excess = absoluteExposure.subtract(limit);
                issues.add(issue);
            }
        }
        return issues;
    }

    public String textReport() {
        StringBuilder report = new StringBuilder();
        report.append("MERIDIAN ENERGY TRADING RISK REPORT\n");
        report.append("As of ").append(asOf()).append('\n');
        report.append("===================================\n");
        report.append(padRight("BOOK", 18));
        report.append(padRight("INSTRUMENT", 18));
        report.append(padLeft("NET QTY", 16));
        report.append(padLeft("MTM", 18));
        report.append('\n');

        for (PositionRisk row : mtmPositions()) {
            report.append(padRight(row.book, 18));
            report.append(padRight(row.instrumentCode, 18));
            report.append(padLeft(NumberFmt.quantity(row.netQuantity), 16));
            report.append(padLeft(NumberFmt.money(row.mtm), 18));
            report.append('\n');
        }

        report.append('\n');
        report.append("VAR-LITE (1D): ");
        report.append(NumberFmt.money(var(1)));
        report.append('\n');
        appendExposureSection(report, "EXPOSURE BY COUNTERPARTY",
                exposureByCounterparty(), 30);
        appendExposureSection(report, "EXPOSURE BY COMMODITY",
                exposureByCommodity(), 20);
        appendExposureSection(report, "EXPOSURE BY BOOK",
                exposureByBook(), 20);
        appendExposureSection(report, "EXPOSURE BY TRADER",
                exposureByTrader(), 20);
        appendStressSection(report);
        appendConcentrationSection(report);
        return report.toString();
    }

    private void appendExposureSection(StringBuilder report, String title,
            Map<String, BigDecimal> values, int width) {
        report.append('\n').append(title).append('\n');
        for (Map.Entry<String, BigDecimal> entry : values.entrySet()) {
            report.append(padRight(entry.getKey(), width));
            report.append(NumberFmt.money(entry.getValue()));
            report.append('\n');
        }
    }

    private void appendStressSection(StringBuilder report) {
        report.append('\n').append("STRESS SCENARIOS\n");
        report.append(padRight("BOOK", 20));
        report.append(padLeft("+10%", 18));
        report.append(padLeft("-10%", 18));
        report.append('\n');
        Map<String, BigDecimal> up = stressUpTenPercent();
        Map<String, BigDecimal> down = stressDownTenPercent();
        for (String book : up.keySet()) {
            report.append(padRight(book, 20));
            report.append(padLeft(NumberFmt.money(up.get(book)), 18));
            report.append(padLeft(NumberFmt.money(down.get(book)), 18));
            report.append('\n');
        }
    }

    private void appendConcentrationSection(StringBuilder report) {
        report.append('\n').append("CREDIT CONCENTRATION\n");
        List<ConcentrationIssue> issues = concentrationChecks();
        if (issues.isEmpty()) {
            report.append("No counterparty limits exceeded\n");
            return;
        }
        for (ConcentrationIssue issue : issues) {
            report.append(padRight(issue.counterparty, 12));
            report.append(" exposure=").append(NumberFmt.money(issue.exposure));
            report.append(" limit=").append(NumberFmt.money(issue.limit));
            report.append(" excess=").append(NumberFmt.money(issue.excess));
            report.append('\n');
        }
    }

    public String csv() {
        return buildCsvReport();
    }

    public String buildCsvReport() {
        StringBuilder csv = new StringBuilder();
        csv.append("book,instrument,commodity,net_quantity,mtm,market_date\n");
        for (PositionRisk row : mtmPositions()) {
            csv.append(csvValue(row.book)).append(',');
            csv.append(csvValue(row.instrumentCode)).append(',');
            csv.append(csvValue(row.commodity)).append(',');
            csv.append(row.netQuantity).append(',');
            csv.append(row.mtm).append(',');
            csv.append(csvValue(new SimpleDateFormat("yyyy-MM-dd")
                    .format(row.marketDate)));
            csv.append('\n');
        }
        csv.append('\n');
        csv.append("counterparty,exposure\n");
        for (Map.Entry<String, BigDecimal> entry
                : exposureByCounterparty().entrySet()) {
            csv.append(csvValue(entry.getKey())).append(',');
            csv.append(entry.getValue()).append('\n');
        }
        return csv.toString();
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    public File writeReportToFile(String path) {
        File output = new File(path);
        File parent = output.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileWriter writer = new FileWriter(output)) {
            writer.write(textReport());
            return output;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write risk report", e);
        }
    }

    public List<Map<String, Object>> exposureRows() {
        List<Map<String, Object>> rows =
                new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, BigDecimal> entry
                : exposureByCounterparty().entrySet()) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("counterparty", entry.getKey());
            row.put("exposure", entry.getValue());
            rows.add(row);
        }
        return rows;
    }

    public BigDecimal totalExposure() {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : exposureByCounterparty().values()) {
            total = total.add(value);
        }
        return total;
    }

    public String asOf() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    private Date latestPriceDate() {
        String sql = "SELECT MAX(price_date) FROM market_price";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getDate(1) : new Date();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to find latest price date",
                    e);
        }
    }

    private BigDecimal latestPrice(int instrumentId, Date date) {
        String sql = "SELECT price FROM market_price "
                + "WHERE instrument_id=? AND price_date=? ";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, instrumentId);
            statement.setDate(2, new java.sql.Date(date.getTime()));
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getBigDecimal(1)
                        : BigDecimal.ZERO;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to find market price", e);
        }
    }

    private String padRight(String value, int width) {
        String text = value == null ? "" : value;
        StringBuilder output = new StringBuilder(text);
        while (output.length() < width) {
            output.append(' ');
        }
        if (output.length() > width) {
            return output.substring(0, width);
        }
        return output.toString();
    }

    private String padLeft(String value, int width) {
        String text = value == null ? "" : value;
        StringBuilder output = new StringBuilder();
        while (output.length() + text.length() < width) {
            output.append(' ');
        }
        output.append(text);
        if (output.length() > width) {
            return output.substring(output.length() - width);
        }
        return output.toString();
    }

    private String formatPosition(PositionRisk row) {
        StringBuilder line = new StringBuilder();
        line.append(padRight(row.book, 18));
        line.append(padRight(row.instrumentCode, 18));
        line.append(padLeft(NumberFmt.quantity(row.netQuantity), 16));
        line.append(padLeft(NumberFmt.money(row.mtm), 18));
        return line.toString();
    }

    private Map<String, BigDecimal> queryByColumn(String column) {
        String sql = "SELECT " + column + ","
                + "SUM(quantity * price * CASE WHEN side='BUY' "
                + "THEN 1 ELSE -1 END) FROM deal "
                + "WHERE status <> 'CANCELLED' GROUP BY " + column
                + " ORDER BY " + column;
        return queryExposure(sql);
    }

    private List<PositionRisk> positionsForBook(String book) {
        List<PositionRisk> result = new ArrayList<PositionRisk>();
        for (PositionRisk row : mtmPositions()) {
            if (book == null || book.equals(row.book)) {
                result.add(row);
            }
        }
        return result;
    }

    private BigDecimal bookMtm(String book) {
        BigDecimal total = BigDecimal.ZERO;
        for (PositionRisk row : positionsForBook(book)) {
            total = total.add(row.mtm);
        }
        return total;
    }

    public String bookSummary(String book) {
        StringBuilder summary = new StringBuilder();
        summary.append("Book: ").append(book).append('\n');
        summary.append("MTM: ").append(NumberFmt.money(bookMtm(book)))
                .append('\n');
        List<PositionRisk> rows = positionsForBook(book);
        summary.append("Positions: ").append(rows.size()).append('\n');
        for (PositionRisk row : rows) {
            summary.append(formatPosition(row)).append('\n');
        }
        return summary.toString();
    }

    public Map<String, BigDecimal> stressByInstrument(double shock) {
        Map<String, BigDecimal> values =
                new LinkedHashMap<String, BigDecimal>();
        Date date = latestPriceDate();
        for (PositionKeeper.Position position : keeper.positions()) {
            Instrument instrument = refs.findInstrument(position.instrumentId);
            String code = instrument == null ? "UNKNOWN"
                    : instrument.getCode();
            BigDecimal price = latestPrice(position.instrumentId, date);
            BigDecimal amount = price.multiply(
                    BigDecimal.valueOf(1.0D + shock))
                    .multiply(position.netQty);
            add(values, code, amount);
        }
        return values;
    }

    public boolean hasLimitBreaches() {
        return !concentrationChecks().isEmpty();
    }

    public String riskHeader() {
        StringBuilder header = new StringBuilder();
        header.append(padRight("BOOK", 18));
        header.append(padRight("INSTRUMENT", 18));
        header.append(padLeft("NET QTY", 16));
        header.append(padLeft("MTM", 18));
        return header.toString();
    }

    private String compactSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("positions=").append(mtmPositions().size());
        summary.append(", exposure=").append(NumberFmt.money(totalExposure()));
        summary.append(", var=").append(NumberFmt.money(var(1)));
        return summary.toString();
    }

    public static void main(String[] args) {
        SchemaBootstrap.initialize();
        RiskReportService service = new RiskReportService();
        System.out.println(service.textReport());
        System.out.println(service.compactSummary());
    }

    public static class PositionRisk {
        private String book;
        private int instrumentId;
        private String instrumentCode;
        private String commodity;
        private BigDecimal netQuantity;
        private BigDecimal mtm;
        private Date marketDate;

        public String getBook() {
            return book;
        }

        public String getInstrumentCode() {
            return instrumentCode;
        }

        public String getCommodity() {
            return commodity;
        }

        public BigDecimal getNetQuantity() {
            return netQuantity;
        }

        public BigDecimal getMtm() {
            return mtm;
        }
    }

    public static class ConcentrationIssue {
        private String counterparty;
        private BigDecimal exposure;
        private BigDecimal limit;
        private BigDecimal excess;

        public String getCounterparty() {
            return counterparty;
        }

        public BigDecimal getExposure() {
            return exposure;
        }

        public BigDecimal getLimit() {
            return limit;
        }

        public BigDecimal getExcess() {
            return excess;
        }
    }
}
