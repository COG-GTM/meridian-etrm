package com.meridianenergy.etrm.core;

import com.meridianenergy.etrm.deal.DealCaptureService;
import com.meridianenergy.etrm.refdata.RefDataDAO;
import com.meridianenergy.etrm.settlement.InvoiceGenerator;
import com.meridianenergy.etrm.settlement.SettlementEngine;
import meridian.risk.MtmValuator;
import meridian.risk.PositionKeeper;
import meridian.risk.RiskReportService;
import meridian.risk.VarLiteCalculator;

public final class AppContext {

    private static DealCaptureService dealService;
    private static RefDataDAO refDataDAO;
    private static RiskReportService riskReportService;
    private static PositionKeeper positionKeeper;
    private static MtmValuator mtmValuator;
    private static VarLiteCalculator varCalculator;
    private static SettlementEngine settlementEngine;
    private static InvoiceGenerator invoiceGenerator;

    private AppContext() {
    }

    public static synchronized RefDataDAO getRefDataDAO() {
        if (refDataDAO == null) {
            refDataDAO = new RefDataDAO();
        }
        return refDataDAO;
    }

    public static synchronized DealCaptureService getDealService() {
        if (dealService == null) {
            dealService = new DealCaptureService();
        }
        return dealService;
    }

    public static synchronized PositionKeeper getPositionKeeper() {
        if (positionKeeper == null) {
            positionKeeper = new PositionKeeper();
        }
        return positionKeeper;
    }

    public static synchronized MtmValuator getMtmValuator() {
        if (mtmValuator == null) {
            mtmValuator = new MtmValuator();
        }
        return mtmValuator;
    }

    public static synchronized VarLiteCalculator getVarReportCalculator() {
        if (varCalculator == null) {
            varCalculator = new VarLiteCalculator();
        }
        return varCalculator;
    }

    public static synchronized RiskReportService getRiskReportService() {
        if (riskReportService == null) {
            riskReportService = new RiskReportService();
        }
        return riskReportService;
    }

    public static synchronized SettlementEngine getSettlementEngine() {
        if (settlementEngine == null) {
            settlementEngine = new SettlementEngine();
        }
        return settlementEngine;
    }

    public static synchronized InvoiceGenerator getInvoiceGenerator() {
        if (invoiceGenerator == null) {
            invoiceGenerator = new InvoiceGenerator();
        }
        return invoiceGenerator;
    }
}
