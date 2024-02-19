// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2012, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.currency;

/**
 * Currency data entry corresponding to the XML data distributed by SIX Interbank Clearing
 * (The ISO 4217 maintenance agency).
 */
public class CurrencyDataEntry {
    private String entity;
    private String currency;
    private String alphabeticCode;
    private Integer numericCode;
    private Integer minorUnit;
    private String withdrawalDate;
    private String remark;
    private boolean historic;

    private CurrencyDataEntry(String entity, String currency, String alphabeticCode, Integer numericCode, Integer minorUnit, String withdrawalDate, String remark, boolean historic) {
        this.entity = entity;
        this.currency = currency;
        this.alphabeticCode = alphabeticCode;
        this.numericCode = numericCode;
        this.minorUnit = minorUnit;
        this.withdrawalDate = withdrawalDate;
        this.remark = remark;
        this.historic = historic;
    }

    public String entity() {
        return entity;
    }

    public String currency() {
        return currency;
    }

    public String alphabeticCode() {
        return alphabeticCode;
    }

    public Integer numericCode() {
        return numericCode;
    }

    public Integer minorUnit() {
        return minorUnit;
    }

    public String withdrawalDate() {
        return withdrawalDate;
    }

    public String remark() {
        return remark;
    }

    public boolean historic() {
        return historic;
    }

    public static class Builder {
        private String entity_;
        private String currency_;
        private String alphabeticCode_;
        private Integer numericCode_;
        private Integer minorUnit_;

        private String withdrawalDate_;
        private String remark_;
        private boolean historic_ = false;

        public Builder setEntity(String entity) {
            entity_ = entity;
            return this;
        }

        public Builder setCurrency(String currency) {
            currency_ = currency;
            return this;
        }

        public Builder setAlphabeticCode(String alphabeticCode) {
            alphabeticCode_ = alphabeticCode;
            return this;
        }

        public Builder setNumericCode(String numericCode) {
            try {
                numericCode_ = Integer.parseInt(numericCode);
            } catch (NumberFormatException e) {
                // ignore
            }
            return this;
        }

        public Builder setMinorUnit(String minorUnit) {
            try {
                minorUnit_ = Integer.parseInt(minorUnit);
            } catch (NumberFormatException e) {
                // ignore
            }
            return this;
        }

        public Builder setWithdrawalDate(String withdrawalDate) {
            withdrawalDate_ = withdrawalDate;
            return this;
        }

        public Builder setRemark(String remark) {
            remark_ = remark;
            return this;
        }

        public Builder setHistoric() {
            historic_ = true;
            return this;
        }

        public CurrencyDataEntry build() {
            return new CurrencyDataEntry(entity_, currency_, alphabeticCode_, numericCode_, minorUnit_, withdrawalDate_, remark_, historic_);
        }

        public Builder reset() {
            entity_ = null;
            currency_ = null;
            alphabeticCode_ = null;
            numericCode_ = null;
            minorUnit_ = null;
            withdrawalDate_ = null;
            remark_ = null;
            historic_ = false;
            return this;
        }
    }
}
