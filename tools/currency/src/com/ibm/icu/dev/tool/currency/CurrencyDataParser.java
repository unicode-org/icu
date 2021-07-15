// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2012-2013, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.currency;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.icu.dev.tool.currency.CurrencyDataEntry.Builder;

/**
 * Parser for ISO 4217 data file in XML format distributed by SIX Interbank Clearing
 * (The ISO 4217 maintenance agency).
 */
public class CurrencyDataParser {

    public static Collection<CurrencyDataEntry> parse(File dataFile, boolean historic) throws IOException {
        Collection<CurrencyDataEntry> result = null;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser;
            parser = factory.newSAXParser();
            Handler handler = new Handler(historic);
            parser.parse(new FileInputStream(dataFile), handler);
            result = handler.getParsedISOCurrencies();
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private static class Handler extends DefaultHandler {
        private enum ElementType {
            ENTITY("CtryNm"),
            CURRENCY("CcyNm"),
            ALPHABETIC_CODE("Ccy"),
            NUMERIC_CODE("CcyNbr"),
            MINOR_UNIT("CcyMnrUnts"),
            WITHDRAWAL_DATE("WthdrwlDt"),
            REMARK("Remark"),   // obsolete
            OTHER("Other");     // place holder

            private String elemName;

            ElementType(String elemName) {
                this.elemName = elemName;
            }

            public static ElementType forName(String name) {
                for (ElementType type : values()) {
                    if (type.elemName.equals(name)) {
                        return type;
                    }
                }
                return OTHER;
            }
        };

        Collection<CurrencyDataEntry> isoCurrencies = new LinkedList<CurrencyDataEntry>();
        ElementType elem = ElementType.OTHER;
        Builder currBld = new Builder();

        private final boolean historic;
        private final String currElemName;

        public Handler(boolean historic) {
            this.historic = historic;
            currElemName = historic ? "HstrcCcyNtry" : "CcyNtry";
        }

        public Collection<CurrencyDataEntry> getParsedISOCurrencies() {
            return isoCurrencies;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if (qName.equals(currElemName)) {
                currBld.reset();
                elem = ElementType.OTHER;
            } else {
                try {
                    elem = ElementType.forName(qName);
                } catch (IllegalArgumentException e) {
                    elem = ElementType.OTHER;
                }
            }
        }

        public void endElement(String uri, String localName, String qName) {
            // emit a currency data
            if (qName.equals(currElemName)) {
                if (historic) {
                    currBld.setHistoric();
                }
                isoCurrencies.add(currBld.build());
                currBld.reset();
            }
            elem = ElementType.OTHER;
        }

        public void characters(char[] ch, int start, int length) {
            switch (elem) {
            case ENTITY:
                currBld.setEntity(new String(ch, start, length).trim());
                break;
            case CURRENCY:
                currBld.setCurrency(new String(ch, start, length).trim());
                break;
            case ALPHABETIC_CODE:
                currBld.setAlphabeticCode(new String(ch, start, length).trim());
                break;
            case NUMERIC_CODE:
                currBld.setNumericCode(new String(ch, start, length).trim());
                break;
            case MINOR_UNIT:
                currBld.setMinorUnit(new String(ch, start, length).trim());
                break;
            case WITHDRAWAL_DATE:
                currBld.setWithdrawalDate(new String(ch, start, length).trim());
                break;
            case REMARK:
                currBld.setRemark(new String(ch, start, length).trim());
                break;
            default:
                // NOOP
                break;
            }
        }
    }
}
