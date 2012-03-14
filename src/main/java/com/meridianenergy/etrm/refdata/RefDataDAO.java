package com.meridianenergy.etrm.refdata;

import com.meridianenergy.etrm.core.DbConnectionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class RefDataDAO {

    private static final String COUNTERPARTY_COLUMNS =
            "SELECT id,name,short_code,credit_limit,active,country "
                    + "FROM counterparty";
    private static final String INSTRUMENT_COLUMNS =
            "SELECT id,code,name,commodity_type,unit,currency,delivery_location "
                    + "FROM instrument";

    // columns match the old Sybase feed layout, do not reorder
    public Counterparty findCounterparty(int id) {
        String sql = COUNTERPARTY_COLUMNS + " WHERE id=?";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? counterparty(result) : null;
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Counterparty findCounterpartyByCode(String code) {
        String sql = COUNTERPARTY_COLUMNS + " WHERE short_code=?";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? counterparty(result) : null;
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public List<Counterparty> listCounterparties() {
        List<Counterparty> values = new ArrayList<Counterparty>();
        String sql = COUNTERPARTY_COLUMNS + " ORDER BY id";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                values.add(counterparty(result));
            }
            return values;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Instrument findInstrument(int id) {
        String sql = INSTRUMENT_COLUMNS + " WHERE id=?";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? instrument(result) : null;
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Instrument findInstrumentByCode(String code) {
        String sql = INSTRUMENT_COLUMNS + " WHERE code=?";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? instrument(result) : null;
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public List<Instrument> listInstruments() {
        List<Instrument> values = new ArrayList<Instrument>();
        String sql = INSTRUMENT_COLUMNS + " ORDER BY id";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                values.add(instrument(result));
            }
            return values;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Counterparty counterparty(ResultSet result) throws Exception {
        Counterparty value = new Counterparty();
        value.setId(result.getInt(1));
        value.setName(result.getString(2));
        value.setShortCode(result.getString("short_code"));
        value.setCreditLimit(result.getBigDecimal(4));
        value.setActive(result.getBoolean(5));
        value.setCountry(result.getString(6));
        return value;
    }

    private Instrument instrument(ResultSet result) throws Exception {
        Instrument value = new Instrument();
        value.setId(result.getInt(1));
        value.setCode(result.getString(2));
        value.setName(result.getString(3));
        value.setCommodityType(result.getString(4));
        value.setUnit(result.getString(5));
        value.setCurrency(result.getString(6));
        value.setDeliveryLocation(result.getString(7));
        return value;
    }
}
