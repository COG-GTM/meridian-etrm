package com.meridianenergy.etrm.deal;

import java.math.BigDecimal;
import java.util.Date;

public class Deal {

    private int id;
    private String dealRef;
    private Date tradeDate;
    private Date valueDate;
    private Date deliveryStart;
    private Date deliveryEnd;
    private TradeType tradeType;
    private Side side;
    private BigDecimal quantity;
    private BigDecimal price;
    private int instrumentId;
    private int counterpartyId;
    private String book;
    private String trader;
    private String status;
    private boolean financial;

    public int getId() {
        return id;
    }

    public void setId(int value) {
        id = value;
    }

    public String getDealRef() {
        return dealRef;
    }

    public void setDealRef(String value) {
        dealRef = value;
    }

    public Date getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(Date value) {
        tradeDate = value;
    }

    public Date getValueDate() {
        return valueDate;
    }

    public void setValueDate(Date value) {
        valueDate = value;
    }

    public Date getDeliveryStart() {
        return deliveryStart;
    }

    public void setDeliveryStart(Date value) {
        deliveryStart = value;
    }

    public Date getDeliveryEnd() {
        return deliveryEnd;
    }

    public void setDeliveryEnd(Date value) {
        deliveryEnd = value;
    }

    public TradeType getTradeType() {
        return tradeType;
    }

    public void setTradeType(TradeType value) {
        tradeType = value;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side value) {
        side = value;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal value) {
        quantity = value;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal value) {
        price = value;
    }

    public int getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(int value) {
        instrumentId = value;
    }

    public int getCounterpartyId() {
        return counterpartyId;
    }

    public void setCounterpartyId(int value) {
        counterpartyId = value;
    }

    public String getBook() {
        return book;
    }

    public void setBook(String value) {
        book = value;
    }

    public String getTrader() {
        return trader;
    }

    public void setTrader(String value) {
        trader = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        status = value;
    }

    public boolean isFinancial() {
        return financial;
    }

    public void setFinancial(boolean value) {
        financial = value;
    }
}
