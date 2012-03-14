package com.meridianenergy.etrm.refdata;

public class Instrument {

    private int id;
    private String code;
    private String name;
    private String commodityType;
    private String unit;
    private String currency;
    private String deliveryLocation;

    public int getId() {
        return id;
    }

    public void setId(int value) {
        id = value;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String value) {
        code = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    public String getCommodityType() {
        return commodityType;
    }

    public void setCommodityType(String value) {
        commodityType = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String value) {
        unit = value;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String value) {
        currency = value;
    }

    public String getDeliveryLocation() {
        return deliveryLocation;
    }

    public void setDeliveryLocation(String value) {
        deliveryLocation = value;
    }
}
