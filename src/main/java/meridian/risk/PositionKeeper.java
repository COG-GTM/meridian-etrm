package meridian.risk;

import com.meridianenergy.etrm.core.DbConnectionManager;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PositionKeeper {

    public List<Position> positions() {
        return positions(null);
    }

    public List<Position> positions(String book) {
        List<Position> values = new ArrayList<Position>();
        String sql = "SELECT book,instrument_id,"
                + "SUM(quantity * CASE WHEN side='BUY' THEN 1 ELSE -1 END) "
                + "FROM deal WHERE status<>'CANCELLED'";
        if (book != null) {
            sql += " AND book=?";
        }
        sql += " GROUP BY book,instrument_id ORDER BY book,instrument_id";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (book != null) {
                statement.setString(1, book);
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    values.add(new Position(result.getString(1),
                            result.getInt(2), result.getBigDecimal(3)));
                }
            }
            return values;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static class Position {
        public String book;
        public int instrumentId;
        public BigDecimal netQty;

        public Position(String book, int instrumentId, BigDecimal netQty) {
            this.book = book;
            this.instrumentId = instrumentId;
            this.netQty = netQty;
        }
    }
}
