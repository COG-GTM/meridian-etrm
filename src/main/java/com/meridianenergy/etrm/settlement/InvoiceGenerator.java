package com.meridianenergy.etrm.settlement;

import com.meridianenergy.etrm.core.DbConnectionManager;
import com.meridianenergy.etrm.deal.Deal;
import com.meridianenergy.etrm.util.DateUtil;
import com.meridianenergy.etrm.util.NumberFmt;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;

public class InvoiceGenerator {

    private final SettlementEngine settlements = new SettlementEngine();

    public String generate(Deal deal) {
        SettlementEngine.Settlement settlement =
                settlements.calculate(deal);
        String text = "MERIDIAN ENERGY TRADING INVOICE\n"
                + "Deal: " + deal.getDealRef() + "\n"
                + "Invoice date: " + DateUtil.format(new Date()) + "\n"
                + "Amount: " + NumberFmt.money(settlement.amount) + "\n"
                + "Accrued interest: "
                + NumberFmt.money(settlement.accruedInterest) + "\n"
                + "Total: "
                + NumberFmt.money(settlement.amount
                        .add(settlement.accruedInterest)) + "\n";
        insertInvoice(deal, settlement, text);
        writeTextFile(deal, text);
        return text;
    }

    private void insertInvoice(Deal deal,
            SettlementEngine.Settlement settlement, String text) {
        String sql = "INSERT INTO invoice(deal_id,invoice_date,amount,"
                + "accrued_interest,details) VALUES(?,?,?,?,?)";
        try (Connection connection = DbConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, deal.getId());
            statement.setDate(2, new java.sql.Date(System.currentTimeMillis()));
            statement.setBigDecimal(3, settlement.amount);
            statement.setBigDecimal(4, settlement.accruedInterest);
            statement.setString(5, text);
            statement.executeUpdate();
        } catch (java.sql.SQLException e) {
        }
    }

    private void writeTextFile(Deal deal, String text) {
        try {
            File directory = new File("./out");
            directory.mkdirs();
            File file = new File(directory, deal.getDealRef() + ".txt");
            FileWriter writer = new FileWriter(file);
            writer.write(text);
            writer.close();
        } catch (IOException ignored) {
        } catch (Exception e) {
            return;
        }
    }
}
