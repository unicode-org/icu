// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2012-2013, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.currency;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * Container of ISO 4217 alpha3 - numeric3 code mapping data
 */
public class NumericCodeData {
    private SortedMap<String, String> _codeMap;

    private NumericCodeData(SortedMap<String, String> codeMap) {
        _codeMap = codeMap;
    }

    public static NumericCodeData getDefaultInstance() {
        SortedMap<String, String> map = new TreeMap<String, String>();
        for (String[] dataEntry : CODE_MAP_DATA) {
            map.put(dataEntry[0], dataEntry[1]);
        }
        return new NumericCodeData(map);
    }

    public static NumericCodeData createInstance(Collection<CurrencyDataEntry> dataEntries) {
        SortedMap<String, String> map = new TreeMap<String, String>();
        for (CurrencyDataEntry dataEntry : dataEntries) {
            String alphaCode = dataEntry.alphabeticCode();
            Integer numCode = dataEntry.numericCode();
            if (alphaCode == null || numCode == null) {
                continue;
            }
            map.put(alphaCode, String.format("%03d", numCode));
        }
        return new NumericCodeData(map);
    }

    public NumericCodeData merge(NumericCodeData anotherData) {
        SortedMap<String, String> codeMap = anotherData.getAlphaNumericCodeMap();
        for (Entry<String, String> codeMapEntry : codeMap.entrySet()) {
            String alphaCode = codeMapEntry.getKey();
            String numCode = codeMapEntry.getValue();
            if (!_codeMap.containsKey(alphaCode)) {
                _codeMap.put(alphaCode, numCode);
            } else {
                String existingValue = _codeMap.get(alphaCode);
                if (!existingValue.equals(numCode)) {
                    throw new RuntimeException("Duplicated definition for " + alphaCode + ": value=" + existingValue + "/another value=" + numCode);
                }
            }
        }
        return this;
    }

    public SortedMap<String, String> getAlphaNumericCodeMap() {
        return Collections.unmodifiableSortedMap(_codeMap);
    }

    public void printArray() {
        for (Entry<String, String> entry : getAlphaNumericCodeMap().entrySet()) {
            System.out.println("        {\"" + entry.getKey() + "\", \"" + entry.getValue() + "\"},");
        }
    }

    public void writeResource(OutputStreamWriter osw, String resName) throws IOException {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));

        PrintWriter pw = new PrintWriter(osw, true);

        // Header
        pw.println("//---------------------------------------------------------");
        pw.println("// Copyright (C) 2016 and later: Unicode, Inc. and others.");
        pw.println("// License & terms of use: http://www.unicode.org/copyright.html");
        pw.println("//---------------------------------------------------------");
        pw.println("// Copyright (C) 2013, International Business Machines");
        pw.println("// Corporation and others.  All Rights Reserved.");
        pw.println("//---------------------------------------------------------");
        pw.println("// Build tool: com.ibm.icu.dev.tool.currency.NumericCodeData");
        pw.println(String.format("// Build date: %1$tFT%1$tTZ", cal));
        pw.println("//---------------------------------------------------------");
        pw.println("// >> !!! >>   THIS IS A MACHINE-GENERATED FILE   << !!! <<");
        pw.println("// >> !!! >>>            DO NOT EDIT             <<< !!! <<");
        pw.println("//---------------------------------------------------------");
        pw.println("");

        // Resource root
        pw.println(resName + ":table(nofallback){");

        // Map data
        pw.println("    codeMap{");

        for (Entry<String, String> mapEntry : _codeMap.entrySet()) {
            String alphaCode = mapEntry.getKey();
            int numericCodeVal = Integer.parseInt(mapEntry.getValue());
            pw.println(String.format("        %1$s:int{%2$d}", alphaCode, numericCodeVal));
        }

        pw.println("    }");

        pw.println("}");
    }

    private static final String[][] CODE_MAP_DATA = {
        {"ADP", "020"},
        {"AED", "784"},
        {"AFA", "004"},
        {"AFN", "971"},
        {"ALL", "008"},
        {"ALK", "008"},
        {"AMD", "051"},
        {"ANG", "532"},
        {"AOA", "973"},
        {"AOK", "024"},
        {"AON", "024"},
        {"AOR", "982"},
        {"ARA", "032"},
        {"ARP", "032"},
        {"ARS", "032"},
        {"ARY", "032"},
        {"ATS", "040"},
        {"AUD", "036"},
        {"AWG", "533"},
        {"AYM", "945"},
        {"AZM", "031"},
        {"AZN", "944"},
        {"BAD", "070"},
        {"BAM", "977"},
        {"BBD", "052"},
        {"BDT", "050"},
        {"BEC", "993"},
        {"BEF", "056"},
        {"BEL", "992"},
        {"BGJ", "100"},
        {"BGK", "100"},
        {"BGL", "100"},
        {"BGN", "975"},
        {"BHD", "048"},
        {"BIF", "108"},
        {"BMD", "060"},
        {"BND", "096"},
        {"BOB", "068"},
        {"BOP", "068"},
        {"BOV", "984"},
        {"BRB", "076"},
        {"BRC", "076"},
        {"BRE", "076"},
        {"BRL", "986"},
        {"BRN", "076"},
        {"BRR", "987"},
        {"BSD", "044"},
        {"BTN", "064"},
        {"BUK", "104"},
        {"BWP", "072"},
        {"BYB", "112"},
        {"BYN", "933"},
        {"BYR", "974"},
        {"BZD", "084"},
        {"CAD", "124"},
        {"CDF", "976"},
        {"CHC", "948"},
        {"CHE", "947"},
        {"CHF", "756"},
        {"CHW", "948"},
        {"CLF", "990"},
        {"CLP", "152"},
        {"CNY", "156"},
        {"COP", "170"},
        {"COU", "970"},
        {"CRC", "188"},
        {"CSD", "891"},
        {"CSJ", "203"},
        {"CSK", "200"},
        {"CUC", "931"},
        {"CUP", "192"},
        {"CVE", "132"},
        {"CYP", "196"},
        {"CZK", "203"},
        {"DDM", "278"},
        {"DEM", "276"},
        {"DJF", "262"},
        {"DKK", "208"},
        {"DOP", "214"},
        {"DZD", "012"},
        {"ECS", "218"},
        {"ECV", "983"},
        {"EEK", "233"},
        {"EGP", "818"},
        {"ERN", "232"},
        {"ESA", "996"},
        {"ESB", "995"},
        {"ESP", "724"},
        {"ETB", "230"},
        {"EUR", "978"},
        {"FIM", "246"},
        {"FJD", "242"},
        {"FKP", "238"},
        {"FRF", "250"},
        {"GBP", "826"},
        {"GEK", "268"},
        {"GEL", "981"},
        {"GHC", "288"},
        {"GHP", "939"},
        {"GHS", "936"},
        {"GIP", "292"},
        {"GMD", "270"},
        {"GNE", "324"},
        {"GNF", "324"},
        {"GNS", "324"},
        {"GQE", "226"},
        {"GRD", "300"},
        {"GTQ", "320"},
        {"GWE", "624"},
        {"GWP", "624"},
        {"GYD", "328"},
        {"HKD", "344"},
        {"HNL", "340"},
        {"HRD", "191"},
        {"HRK", "191"},
        {"HTG", "332"},
        {"HUF", "348"},
        {"IDR", "360"},
        {"IEP", "372"},
        {"ILP", "376"},
        {"ILR", "376"},
        {"ILS", "376"},
        {"INR", "356"},
        {"IQD", "368"},
        {"IRR", "364"},
        {"ISJ", "352"},
        {"ISK", "352"},
        {"ITL", "380"},
        {"JMD", "388"},
        {"JOD", "400"},
        {"JPY", "392"},
        {"KES", "404"},
        {"KGS", "417"},
        {"KHR", "116"},
        {"KMF", "174"},
        {"KPW", "408"},
        {"KRW", "410"},
        {"KWD", "414"},
        {"KYD", "136"},
        {"KZT", "398"},
        {"LAJ", "418"},
        {"LAK", "418"},
        {"LBP", "422"},
        {"LKR", "144"},
        {"LRD", "430"},
        {"LSL", "426"},
        {"LSM", "426"},
        {"LTL", "440"},
        {"LTT", "440"},
        {"LUC", "989"},
        {"LUF", "442"},
        {"LUL", "988"},
        {"LVL", "428"},
        {"LVR", "428"},
        {"LYD", "434"},
        {"MAD", "504"},
        {"MDL", "498"},
        {"MGA", "969"},
        {"MGF", "450"},
        {"MKD", "807"},
        {"MLF", "466"},
        {"MMK", "104"},
        {"MNT", "496"},
        {"MOP", "446"},
        {"MRO", "478"},
        {"MRU", "929"},
        {"MTL", "470"},
        {"MTP", "470"},
        {"MUR", "480"},
        {"MVQ", "462"},
        {"MVR", "462"},
        {"MWK", "454"},
        {"MXN", "484"},
        {"MXP", "484"},
        {"MXV", "979"},
        {"MYR", "458"},
        {"MZE", "508"},
        {"MZM", "508"},
        {"MZN", "943"},
        {"NAD", "516"},
        {"NGN", "566"},
        {"NIC", "558"},
        {"NIO", "558"},
        {"NLG", "528"},
        {"NOK", "578"},
        {"NPR", "524"},
        {"NZD", "554"},
        {"OMR", "512"},
        {"PAB", "590"},
        {"PEH", "604"},
        {"PEI", "604"},
        {"PEN", "604"},
        {"PES", "604"},
        {"PGK", "598"},
        {"PHP", "608"},
        {"PKR", "586"},
        {"PLN", "985"},
        {"PLZ", "616"},
        {"PTE", "620"},
        {"PYG", "600"},
        {"QAR", "634"},
        {"RHD", "716"},
        {"ROK", "642"},
        {"ROL", "642"},
        {"RON", "946"},
        {"RSD", "941"},
        {"RUB", "643"},
        {"RUR", "810"},
        {"RWF", "646"},
        {"SAR", "682"},
        {"SBD", "090"},
        {"SCR", "690"},
        {"SDD", "736"},
        {"SDG", "938"},
        {"SDP", "736"},
        {"SEK", "752"},
        {"SGD", "702"},
        {"SHP", "654"},
        {"SIT", "705"},
        {"SKK", "703"},
        {"SLL", "694"},
        {"SOS", "706"},
        {"SRD", "968"},
        {"SRG", "740"},
        {"SSP", "728"},
        {"STD", "678"},
        {"STN", "930"},
        {"SUR", "810"},
        {"SVC", "222"},
        {"SYP", "760"},
        {"SZL", "748"},
        {"THB", "764"},
        {"TJR", "762"},
        {"TJS", "972"},
        {"TMM", "795"},
        {"TMT", "934"},
        {"TND", "788"},
        {"TOP", "776"},
        {"TPE", "626"},
        {"TRL", "792"},
        {"TRY", "949"},
        {"TTD", "780"},
        {"TWD", "901"},
        {"TZS", "834"},
        {"UAH", "980"},
        {"UAK", "804"},
        {"UGS", "800"},
        {"UGW", "800"},
        {"UGX", "800"},
        {"USD", "840"},
        {"USN", "997"},
        {"USS", "998"},
        {"UYI", "940"},
        {"UYN", "858"},
        {"UYP", "858"},
        {"UYU", "858"},
        {"UYW", "927"},
        {"UZS", "860"},
        {"VEB", "862"},
        {"VEF", "937"},
        {"VES", "928"},
        {"VNC", "704"},
        {"VND", "704"},
        {"VUV", "548"},
        {"WST", "882"},
        {"XAF", "950"},
        {"XAG", "961"},
        {"XAU", "959"},
        {"XBA", "955"},
        {"XBB", "956"},
        {"XBC", "957"},
        {"XBD", "958"},
        {"XCD", "951"},
        {"XDR", "960"},
        {"XEU", "954"},
        {"XOF", "952"},
        {"XPD", "964"},
        {"XPF", "953"},
        {"XPT", "962"},
        {"XSU", "994"},
        {"XTS", "963"},
        {"XUA", "965"},
        {"XXX", "999"},
        {"YDD", "720"},
        {"YER", "886"},
        {"YUD", "890"},
        {"YUM", "891"},
        {"YUN", "890"},
        {"ZAL", "991"},
        {"ZAR", "710"},
        {"ZMK", "894"},
        {"ZMW", "967"},
        {"ZRN", "180"},
        {"ZRZ", "180"},
        {"ZWC", "716"},
        {"ZWD", "716"},
        {"ZWL", "932"},
        {"ZWN", "942"},
        {"ZWR", "935"},
    };
}
