package com.meridianenergy.etrm.refdata;

import java.math.BigDecimal;

public class Counterparty {

    private int id;
    private String name;
    private String shortCode;
    private BigDecimal creditLimit;
    private boolean active;
    private String country;

    public int getId() {
        return id;
    }

    public void setId(int value) {
        id = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String value) {
        shortCode = value;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal value) {
        creditLimit = value;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean value) {
        active = value;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String value) {
        country = value;
    }
}
