package meridian.risk;

import com.meridianenergy.etrm.core.DbConnectionManager;
import com.meridianenergy.etrm.deal.Deal;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

public class MtmValuator {

    public BigDecimal value(Deal deal) {
        return value(deal, new java.sql.Date(System.currentTimeMillis()));
    }

    public BigDecimal value(Deal deal, Date date) {
        BigDecimal marketPrice = marketPrice(deal.getInstrumentId(), date);
        BigDecimal difference = marketPrice.subtract(deal.getPrice())
                .multiply(deal.getQuantity());
        if (deal.getSide() == com.meridianenergy.etrm.deal.Side.SELL) {
            return difference.negate();
        }
        return difference;
    }

    public BigDecimal positionValue(PositionKeeper.Position position,
            Date date) {
        BigDecimal price = marketPrice(position.instrumentId, date);
        return price.multiply(position.netQty);
    }

    private BigDecimal marketPrice(int instrumentId, Date date) {
        String sql = "SELECT price FROM market_price WHERE instrument_id=? "
                + "ORDER BY ABS(DATEDIFF('DAY',price_date,?)) LIMIT 1";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, instrumentId);
            statement.setDate(2, new java.sql.Date(date.getTime()));
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getBigDecimal(1)
                        : BigDecimal.ZERO;
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
