/*
 **********************************************************************
 * Copyright (c) 2011, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: John Emmons
 * Created: April 8 - 2011
 * Since: ICU 4.8
 **********************************************************************
 */

package com.ibm.icu.dev.test.util;

import java.util.Set;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.Region;
import com.ibm.icu.util.Region.RegionType;

/**
 * @test
 * @summary General test of Regions
 */

public class RegionTest extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new RegionTest().run(args);
    }
   /**
     * Test for known regions.
     */
    public void TestKnownRegions() {
        String[][] knownRegions = {
         //   Code  , Numeric , Parent, Type
            { "001", "001", null , "WORLD" },
            { "002", "002", "001", "CONTINENT" },
            { "003", "003", null,  "GROUPING" },
            { "005", "005", "019", "SUBCONTINENT" },
            { "009", "009", "001", "CONTINENT" },
            { "011", "011", "002", "SUBCONTINENT" },
            { "013", "013", "019", "SUBCONTINENT" },
            { "014", "014", "002", "SUBCONTINENT" },
            { "015", "015", "002", "SUBCONTINENT" },
            { "017", "017", "002", "SUBCONTINENT" },
            { "018", "018", "002", "SUBCONTINENT" },
            { "019", "019", "001", "CONTINENT" },
            { "021", "021", "019", "SUBCONTINENT" },
            { "029", "029", "019", "SUBCONTINENT" },
            { "030", "030", "142", "SUBCONTINENT" },
            { "034", "034", "142", "SUBCONTINENT" },
            { "035", "035", "142", "SUBCONTINENT" },
            { "039", "039", "150", "SUBCONTINENT" },
            { "053", "053", "009", "SUBCONTINENT" },
            { "054", "054", "009", "SUBCONTINENT" },
            { "057", "057", "009", "SUBCONTINENT" },
            { "061", "061", "009", "SUBCONTINENT" },
            { "062", "062", null,  "DEPRECATED"  },
            { "142", "142", "001", "CONTINENT" },
            { "143", "143", "142", "SUBCONTINENT" },
            { "145", "145", "142", "SUBCONTINENT" },
            { "150", "150", "001", "CONTINENT" },
            { "151", "151", "150", "SUBCONTINENT" },
            { "154", "154", "150", "SUBCONTINENT" },
            { "155", "155", "150", "SUBCONTINENT" },
            { "419", "419", null,  "GROUPING" },
            { "172", "172", null,  "DEPRECATED" },
            { "200", "200", null,  "DEPRECATED" },
            { "830", "830", null,  "DEPRECATED" },
            { "AC" , "-1" , "QO" , "TERRITORY" },
            { "AD" , "020", "039", "TERRITORY" },
            { "AE" , "784", "145", "TERRITORY" },
            { "AF" , "004", "034", "TERRITORY" },
            { "AG" , "028", "029", "TERRITORY" },
            { "AI" , "660", "029", "TERRITORY" },
            { "AL" , "008", "039", "TERRITORY" },
            { "AM" , "051", "145", "TERRITORY" },
            { "AN" , "530", null,  "DEPRECATED" },
            { "AO" , "024", "017", "TERRITORY" },
            { "AQ" , "010", "QO" , "TERRITORY" },
            { "AR" , "032", "005", "TERRITORY" },
            { "AS" , "016", "061", "TERRITORY" },
            { "AT" , "040", "155", "TERRITORY" },
            { "AU" , "036", "053", "TERRITORY" },
            { "AW" , "533", "029", "TERRITORY" },
            { "AX" , "248", "154", "TERRITORY" },
            { "AZ" , "031", "145", "TERRITORY" },
            { "BA" , "070", "039", "TERRITORY" },
            { "BB" , "052", "029", "TERRITORY" },
            { "BD" , "050", "034", "TERRITORY" },
            { "BE" , "056", "155", "TERRITORY" },
            { "BF" , "854", "011", "TERRITORY" },
            { "BG" , "100", "151", "TERRITORY" },
            { "BH" , "048", "145", "TERRITORY" },
            { "BI" , "108", "014", "TERRITORY" },
            { "BJ" , "204", "011", "TERRITORY" },
            { "BL" , "652", "029", "TERRITORY" },
            { "BM" , "060", "021", "TERRITORY" },
            { "BN" , "096", "035", "TERRITORY" },
            { "BO" , "068", "005", "TERRITORY" },
            { "BQ" , "535", "029", "TERRITORY" },
            { "BR" , "076", "005", "TERRITORY" },
            { "BS" , "044", "029", "TERRITORY" },
            { "BT" , "064", "034", "TERRITORY" },
            { "BU" , "104", "035", "TERRITORY" },
            { "BV" , "074", "QO" , "TERRITORY" },
            { "BW" , "072", "018", "TERRITORY" },
            { "BY" , "112", "151", "TERRITORY" },
            { "BZ" , "084", "013", "TERRITORY" },
            { "CA" , "124", "021", "TERRITORY" },
            { "CC" , "166", "QO" , "TERRITORY" },
            { "CD" , "180", "017", "TERRITORY" },
            { "CF" , "140", "017", "TERRITORY" },
            { "CG" , "178", "017", "TERRITORY" },
            { "CH" , "756", "155", "TERRITORY" },
            { "CI" , "384", "011", "TERRITORY" },
            { "CK" , "184", "061", "TERRITORY" },
            { "CL" , "152", "005", "TERRITORY" },
            { "CM" , "120", "017", "TERRITORY" },
            { "CN" , "156", "030", "TERRITORY" },
            { "CO" , "170", "005", "TERRITORY" },
            { "CP" , "-1" , "QO" , "TERRITORY" },
            { "CR" , "188", "013", "TERRITORY" },
            { "CU" , "192", "029", "TERRITORY" },
            { "CV" , "132", "011", "TERRITORY" },
            { "CW" , "531", "029", "TERRITORY" },
            { "CX" , "162", "QO" , "TERRITORY" },
            { "CY" , "196", "145", "TERRITORY" },
            { "CZ" , "203", "151", "TERRITORY" },
            { "DD" , "276", "155", "TERRITORY" },
            { "DE" , "276", "155", "TERRITORY" },
            { "DG" , "-1" , "QO" , "TERRITORY" },
            { "DJ" , "262", "014", "TERRITORY" },
            { "DK" , "208", "154", "TERRITORY" },
            { "DM" , "212", "029", "TERRITORY" },
            { "DO" , "214", "029", "TERRITORY" },
            { "DY" , "204", "011", "TERRITORY" },
            { "DZ" , "012", "015", "TERRITORY" },
            { "EA" , "-1" , "015", "TERRITORY" },
            { "EC" , "218", "005", "TERRITORY" },
            { "EE" , "233", "154", "TERRITORY" },
            { "EG" , "818", "015", "TERRITORY" },
            { "EH" , "732", "015", "TERRITORY" },
            { "ER" , "232", "014", "TERRITORY" },
            { "ES" , "724", "039", "TERRITORY" },
            { "ET" , "231", "014", "TERRITORY" },
            { "EU" , "967", null,  "GROUPING" },
            { "FI" , "246", "154", "TERRITORY" },
            { "FJ" , "242", "054", "TERRITORY" },
            { "FK" , "238", "005", "TERRITORY" },
            { "FM" , "583", "057", "TERRITORY" },
            { "FO" , "234", "154", "TERRITORY" },
            { "FQ" , "-1",  null , "DEPRECATED" },
            { "FR" , "250", "155", "TERRITORY" },
            { "FX" , "250", "155", "TERRITORY" },
            { "GA" , "266", "017", "TERRITORY" },
            { "GB" , "826", "154", "TERRITORY" },
            { "GD" , "308", "029", "TERRITORY" },
            { "GE" , "268", "145", "TERRITORY" },
            { "GF" , "254", "005", "TERRITORY" },
            { "GG" , "831", "154", "TERRITORY" },
            { "GH" , "288", "011", "TERRITORY" },
            { "GI" , "292", "039", "TERRITORY" },
            { "GL" , "304", "021", "TERRITORY" },
            { "GM" , "270", "011", "TERRITORY" },
            { "GN" , "324", "011", "TERRITORY" },
            { "GP" , "312", "029", "TERRITORY" },
            { "GQ" , "226", "017", "TERRITORY" },
            { "GR" , "300", "039", "TERRITORY" },
            { "GS" , "239", "QO" , "TERRITORY" },
            { "GT" , "320", "013", "TERRITORY" },
            { "GU" , "316", "057", "TERRITORY" },
            { "GW" , "624", "011", "TERRITORY" },
            { "GY" , "328", "005", "TERRITORY" },
            { "HK" , "344", "030", "TERRITORY" },
            { "HM" , "334", "QO" , "TERRITORY" },
            { "HN" , "340", "013", "TERRITORY" },
            { "HR" , "191", "039", "TERRITORY" },
            { "HT" , "332", "029", "TERRITORY" },
            { "HU" , "348", "151", "TERRITORY" },
            { "HV" , "854", "011", "TERRITORY" },
            { "IC" , "-1" , "015", "TERRITORY" },
            { "ID" , "360", "035", "TERRITORY" },
            { "IE" , "372", "154", "TERRITORY" },
            { "IL" , "376", "145", "TERRITORY" },
            { "IM" , "833", "154", "TERRITORY" },
            { "IN" , "356", "034", "TERRITORY" },
            { "IO" , "086", "QO" , "TERRITORY" },
            { "IQ" , "368", "145", "TERRITORY" },
            { "IR" , "364", "034", "TERRITORY" },
            { "IS" , "352", "154", "TERRITORY" },
            { "IT" , "380", "039", "TERRITORY" },
            { "JE" , "832", "154", "TERRITORY" },
            { "JM" , "388", "029", "TERRITORY" },
            { "JO" , "400", "145", "TERRITORY" },
            { "JP" , "392", "030", "TERRITORY" },
            { "JT" , "581", "QO" , "TERRITORY" },
            { "KE" , "404", "014", "TERRITORY" },
            { "KG" , "417", "143", "TERRITORY" },
            { "KH" , "116", "035", "TERRITORY" },
            { "KI" , "296", "057", "TERRITORY" },
            { "KM" , "174", "014", "TERRITORY" },
            { "KN" , "659", "029", "TERRITORY" },
            { "KP" , "408", "030", "TERRITORY" },
            { "KR" , "410", "030", "TERRITORY" },
            { "KW" , "414", "145", "TERRITORY" },
            { "KY" , "136", "029", "TERRITORY" },
            { "KZ" , "398", "143", "TERRITORY" },
            { "LA" , "418", "035", "TERRITORY" },
            { "LB" , "422", "145", "TERRITORY" },
            { "LC" , "662", "029", "TERRITORY" },
            { "LI" , "438", "155", "TERRITORY" },
            { "LK" , "144", "034", "TERRITORY" },
            { "LR" , "430", "011", "TERRITORY" },
            { "LS" , "426", "018", "TERRITORY" },
            { "LT" , "440", "154", "TERRITORY" },
            { "LU" , "442", "155", "TERRITORY" },
            { "LV" , "428", "154", "TERRITORY" },
            { "LY" , "434", "015", "TERRITORY" },
            { "MA" , "504", "015", "TERRITORY" },
            { "MC" , "492", "155", "TERRITORY" },
            { "MD" , "498", "151", "TERRITORY" },
            { "ME" , "499", "039", "TERRITORY" },
            { "MF" , "663", "029", "TERRITORY" },
            { "MG" , "450", "014", "TERRITORY" },
            { "MH" , "584", "057", "TERRITORY" },
            { "MI" , "581", "QO" , "TERRITORY" },
            { "MK" , "807", "039", "TERRITORY" },
            { "ML" , "466", "011", "TERRITORY" },
            { "MM" , "104", "035", "TERRITORY" },
            { "MN" , "496", "030", "TERRITORY" },
            { "MO" , "446", "030", "TERRITORY" },
            { "MP" , "580", "057", "TERRITORY" },
            { "MQ" , "474", "029", "TERRITORY" },
            { "MR" , "478", "011", "TERRITORY" },
            { "MS" , "500", "029", "TERRITORY" },
            { "MT" , "470", "039", "TERRITORY" },
            { "MU" , "480", "014", "TERRITORY" },
            { "MV" , "462", "034", "TERRITORY" },
            { "MW" , "454", "014", "TERRITORY" },
            { "MX" , "484", "013", "TERRITORY" },
            { "MY" , "458", "035", "TERRITORY" },
            { "MZ" , "508", "014", "TERRITORY" },
            { "NA" , "516", "018", "TERRITORY" },
            { "NC" , "540", "054", "TERRITORY" },
            { "NE" , "562", "011", "TERRITORY" },
            { "NF" , "574", "053", "TERRITORY" },
            { "NG" , "566", "011", "TERRITORY" },
            { "NH" , "548", "054", "TERRITORY" },
            { "NI" , "558", "013", "TERRITORY" },
            { "NL" , "528", "155", "TERRITORY" },
            { "NO" , "578", "154", "TERRITORY" },
            { "NP" , "524", "034", "TERRITORY" },
            { "NQ" , "010", "QO" , "TERRITORY" },
            { "NR" , "520", "057", "TERRITORY" },
            { "NT" , "536", null , "DEPRECATED" },
            { "NU" , "570", "061", "TERRITORY" },
            { "NZ" , "554", "053", "TERRITORY" },
            { "OM" , "512", "145", "TERRITORY" },
            { "PA" , "591", "013", "TERRITORY" },
            { "PC" , "-1",  null,  "DEPRECATED" },
            { "PE" , "604", "005", "TERRITORY" },
            { "PF" , "258", "061", "TERRITORY" },
            { "PG" , "598", "054", "TERRITORY" },
            { "PH" , "608", "035", "TERRITORY" },
            { "PK" , "586", "034", "TERRITORY" },
            { "PL" , "616", "151", "TERRITORY" },
            { "PM" , "666", "021", "TERRITORY" },
            { "PN" , "612", "061", "TERRITORY" },
            { "PR" , "630", "029", "TERRITORY" },
            { "PS" , "275", "145", "TERRITORY" },
            { "PT" , "620", "039", "TERRITORY" },
            { "PU" , "581", "QO" , "TERRITORY" },
            { "PW" , "585", "057", "TERRITORY" },
            { "PY" , "600", "005", "TERRITORY" },
            { "PZ" , "591", "013", "TERRITORY" },
            { "QA" , "634", "145", "TERRITORY" },
            { "QO" , "961", "009", "SUBCONTINENT" },
            { "QU" , "967", null,  "GROUPING" },
            { "RE" , "638", "014", "TERRITORY" },
            { "RO" , "642", "151", "TERRITORY" },
            { "RS" , "688", "039", "TERRITORY" },
            { "RU" , "643", "151", "TERRITORY" },
            { "RW" , "646", "014", "TERRITORY" },
            { "SA" , "682", "145", "TERRITORY" },
            { "SB" , "090", "054", "TERRITORY" },
            { "SC" , "690", "014", "TERRITORY" },
            { "SD" , "736", "015", "TERRITORY" },
            { "SE" , "752", "154", "TERRITORY" },
            { "SG" , "702", "035", "TERRITORY" },
            { "SH" , "654", "011", "TERRITORY" },
            { "SI" , "705", "039", "TERRITORY" },
            { "SJ" , "744", "154", "TERRITORY" },
            { "SK" , "703", "151", "TERRITORY" },
            { "SL" , "694", "011", "TERRITORY" },
            { "SM" , "674", "039", "TERRITORY" },
            { "SN" , "686", "011", "TERRITORY" },
            { "SO" , "706", "014", "TERRITORY" },
            { "SR" , "740", "005", "TERRITORY" },
            { "ST" , "678", "017", "TERRITORY" },
            { "SU" , "810", null , "DEPRECATED" },
            { "SV" , "222", "013", "TERRITORY" },
            { "SX" , "534", "029", "TERRITORY" },
            { "SY" , "760", "145", "TERRITORY" },
            { "SZ" , "748", "018", "TERRITORY" },
            { "TA" , "-1" , "QO", "TERRITORY" },
            { "TC" , "796", "029", "TERRITORY" },
            { "TD" , "148", "017", "TERRITORY" },
            { "TF" , "260", "QO" , "TERRITORY" },
            { "TG" , "768", "011", "TERRITORY" },
            { "TH" , "764", "035", "TERRITORY" },
            { "TJ" , "762", "143", "TERRITORY" },
            { "TK" , "772", "061", "TERRITORY" },
            { "TL" , "626", "035", "TERRITORY" },
            { "TM" , "795", "143", "TERRITORY" },
            { "TN" , "788", "015", "TERRITORY" },
            { "TO" , "776", "061", "TERRITORY" },
            { "TP" , "626", "035", "TERRITORY" },
            { "TR" , "792", "145", "TERRITORY" },
            { "TT" , "780", "029", "TERRITORY" },
            { "TV" , "798", "061", "TERRITORY" },
            { "TW" , "158", "030", "TERRITORY" },
            { "TZ" , "834", "014", "TERRITORY" },
            { "UA" , "804", "151", "TERRITORY" },
            { "UG" , "800", "014", "TERRITORY" },
            { "UM" , "581", "QO" , "TERRITORY" },
            { "US" , "840", "021", "TERRITORY" },
            { "UY" , "858", "005", "TERRITORY" },
            { "UZ" , "860", "143", "TERRITORY" },
            { "VA" , "336", "039", "TERRITORY" },
            { "VC" , "670", "029", "TERRITORY" },
            { "VE" , "862", "005", "TERRITORY" },
            { "VG" , "092", "029", "TERRITORY" },
            { "VI" , "850", "029", "TERRITORY" },
            { "VN" , "704", "035", "TERRITORY" },
            { "VU" , "548", "054", "TERRITORY" },
            { "WF" , "876", "061", "TERRITORY" },
            { "WS" , "882", "061", "TERRITORY" },
            { "YD" , "887", "145", "TERRITORY" },
            { "YE" , "887", "145", "TERRITORY" },
            { "YT" , "175", "014", "TERRITORY" },
            { "ZA" , "710", "018", "TERRITORY" },
            { "ZM" , "894", "014", "TERRITORY" },
            { "ZR" , "180", "017", "TERRITORY" },
            { "ZW" , "716", "014", "TERRITORY" },
            { "ZZ" , "999", null , "UNKNOWN" }
            };
        
        for (String [] rd : knownRegions ) {
            try {
               Region r = Region.get(rd[0]);
               int n = r.getNumericCode();
               int e = Integer.valueOf(rd[1]).intValue();
               if ( n != e ) {
                   errln("Numeric code mismatch for region " + r.toString() + ". Expected: " + e + " Got:" + n);
               }
               Region c = r.getContainingRegion();
               if (rd[2] == null) {                   
                   if ( c != null) {
                       errln("Parent for " + r.toString() + " should have been NULL.  Got: " + c.toString());
                   }
               } else {
                   Region p = Region.get(rd[2]);                   
                   if ( !p.equals(c)) {
                       errln("Expected parent of region " + r.toString() + " was " + p.toString() + ". Got: " + ( c == null ? "NULL" : c.toString()) );
                   }
               }
               if (!r.isOfType(Region.RegionType.valueOf(rd[3]))) {
                   errln("Expected region " + r.toString() + " to be of type " + rd[3] + ". Got:" + r.getType().toString());
               }
               int nc = Integer.valueOf(rd[1]).intValue();
               if ( nc > 0 ) {
                   Region ncRegion = Region.get(nc);
                   if ( !ncRegion.equals(r) && nc != 891 ) { // 891 is special case - CS and YU both deprecated codes for region 891
                       errln("Creating region " + r.toString() + " by its numeric code returned a different region. Got: " + ncRegion.toString());
                   }
               }
            } catch (IllegalArgumentException ex ) {
               errln("Known region " + rd[0] + "was not recognized.");
            }
        }
    }
    
    public void TestBadArguments() {
        try {
            Region.get(null);
            errln("Calling Region.get(null) should have thrown a NullPointerException, but didn't.");
        } catch ( NullPointerException ex ) {
            // Do nothing - we're supposed to get here.
        }
        try {
            Region.get("BOGUS");
            errln("Calling Region.get(BOGUS) should have thrown a IllegalArgumentException, but didn't.");
        } catch ( IllegalArgumentException ex ) {
            // Do nothing - we're supposed to get here.
        }
        try {
            Region.get(-123);
            errln("Calling Region.get(-123) should have thrown a IllegalArgumentException, but didn't.");
        } catch ( IllegalArgumentException ex ) {
            // Do nothing - we're supposed to get here.
        }
    }
    public void TestAvailableRegions() {
        // Test to make sure that the set of territories contained in World and the set of all available
        // territories are one and the same.
        Set<Region> availableTerritories = Region.getAvailable(RegionType.TERRITORY);
        Region world = Region.get("001");
        Set<Region> containedInWorld = world.getContainedTerritories();
        if ( !availableTerritories.equals(containedInWorld) ) {
            errln("Available territories and all territories contained in world should be the same set." +
            	  "Available = " + availableTerritories.toString() + 
            	  " : Contained in World = " + containedInWorld.toString());
        }
    }
}
