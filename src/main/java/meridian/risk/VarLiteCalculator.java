package meridian.risk;

import com.meridianenergy.etrm.core.DbConnectionManager;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class VarLiteCalculator {

    public BigDecimal calculate(List<PositionKeeper.Position> positions,
            int horizonDays) {
        double total = 0.0D;
        for (PositionKeeper.Position position : positions) {
            double exposure = exposure(position);
            double volatility = volatility(position.instrumentId);
            double contribution = Math.abs(exposure) * volatility * 2.33
                    * Math.sqrt(1.0D / 252 * horizonDays);
            double rounded = Math.round(contribution * 100) / 100;
            total += (float) rounded;
        }
        return BigDecimal.valueOf(total).setScale(2,
                BigDecimal.ROUND_HALF_UP);
    }

    private double exposure(PositionKeeper.Position position) {
        String sql = "SELECT price FROM market_price "
                + "WHERE instrument_id=? ORDER BY price_date DESC LIMIT 1";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, position.instrumentId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return 0.0D;
                }
                return result.getDouble(1) * position.netQty.doubleValue();
            }
        } catch (Exception e) {
            return 0.0D;
        }
    }

    private double volatility(int instrumentId) {
        String sql = "SELECT annualized_vol FROM instrument_vol "
                + "WHERE instrument_id=?";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, instrumentId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getDouble(1) : 0.30D;
            }
        } catch (Exception e) {
            return 0.30D;
        }
    }
}
