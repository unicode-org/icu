/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/data/Attic/JapaneseCalendarSymbols.java,v $ 
 * $Date: 2002/02/16 03:05:50 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

/**
 * Default Date Format symbols for the Japanese Calendar
 */
public class JapaneseCalendarSymbols extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1998 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "DateTimePatterns",
            new String[] {
                "h:mm:ss a z", // full time pattern
                "h:mm:ss a z", // long time pattern
                "h:mm:ss a", // medium time pattern
                "h:mm a", // short time pattern
                "EEEE, MMMM d, yy G", // full date pattern
                "MMMM d, yy G", // long date pattern
                "MMM d, yy G", // medium date pattern
                "M/d/yy", // short date pattern
                "{1} {0}" // date-time pattern
            }
        },
        { "Eras", new String[] {
            // Name                         Gregorian Year
            "Taika",                        //  645
            "Hakuchi",                      //  650
            "Hakuh\u014D",                  //  672
            "Shuch\u014D",                  //  686
            "Taih\u014D",                   //  701
            "Keiun",                        //  704
            "Wad\u014D",                    //  708
            "Reiki",                        //  715
            "Y\u014Dr\u014D",               //  717
            "Jinki",                        //  724
            "Tempy\u014D",                  //  729
            "Tempy\u014D-kamp\u014D",       //  749
            "Tempy\u014D-sh\u014Dh\u014D",  //  749
            "Tempy\u014D-h\u014Dji",        //  757
            "Temph\u014D-jingo",            //  765
            "Jingo-keiun",                  //  767
            "H\u014Dki",                    //  770
            "Ten-\u014D",                   //  781
            "Enryaku",                      //  782
            "Daid\u014D",                   //  806
            "K\u014Dnin",                   //  810
            "Tench\u014D",                  //  824
            "Sh\u014Dwa",                   //  834
            "Kaj\u014D",                    //  848
            "Ninju",                        //  851
            "Saiko",                        //  854
            "Tennan",                       //  857
            "J\u014Dgan",                   //  859
            "Genkei",                       //  877
            "Ninna",                        //  885
            "Kampy\u014D",                  //  889
            "Sh\u014Dtai",                  //  898
            "Engi",                         //  901
            "Ench\u014D",                   //  923
            "Sh\u014Dhei",                  //  931
            "Tengy\u014D",                  //  938
            "Tenryaku",                     //  947
            "Tentoku",                      //  957
            "\u014Cwa",                     //  961
            "K\u014Dh\u014D",               //  964
            "Anna",                         //  968
            "Tenroku",                      //  970
            "Ten-en",                       //  973
            "J\u014Dgen",                   //  976
            "Tengen",                       //  978
            "Eikan",                        //  983
            "Kanna",                        //  985
            "Ei-en",                        //  987
            "Eiso",                         //  989
            "Sh\u014Dryaku",                //  990
            "Ch\u014Dtoku",                 //  995
            "Ch\u014Dh\u014D",              //  999
            "Kank\u014D",                   // 1004
            "Ch\u014Dwa",                   // 1012
            "Kannin",                       // 1017
            "Jian",                         // 1021
            "Manju",                        // 1024
            "Ch\u014Dgen",                  // 1028
            "Ch\u014Dryaku",                // 1037
            "Ch\u014Dky\u016B",             // 1040
            "Kantoku",                      // 1044
            "Eish\u014D",                   // 1046
            "Tengi",                        // 1053
            "K\u014Dhei",                   // 1058
            "Jiryaku",                      // 1065
            "Enky\u016B",                   // 1069
            "Sh\u014Dho",                   // 1074
            "Sh\u014Dryaku",                // 1077
            "Eiho",                         // 1081
            "\u014Ctoku",                   // 1084
            "Kanji",                        // 1087
            "Kaho",                         // 1094
            "Eich\u014D",                   // 1096
            "Sh\u014Dtoku",                 // 1097
            "K\u014Dwa",                    // 1099
            "Ch\u014Dji",                   // 1104
            "Kash\u014D",                   // 1106
            "Tennin",                       // 1108
            "Ten-ei",                       // 1110
            "Eiky\u016B",                   // 1113
            "Gen-ei",                       // 1118
            "Hoan",                         // 1120
            "Tenji",                        // 1124
            "Daiji",                        // 1126
            "Tensh\u014D",                  // 1131
            "Ch\u014Dsh\u014D",             // 1132
            "Hoen",                         // 1135
            "Eiji",                         // 1141
            "K\u014Dji",                    // 1142
            "Teny\u014D",                   // 1144
            "Ky\u016Ban",                   // 1145
            "Ninpei",                       // 1151
            "Ky\u016Bju",                   // 1154
            "Hogen",                        // 1156
            "Heiji",                        // 1159
            "Eiryaku",                      // 1160
            "\u014Cho",                     // 1161
            "Ch\u014Dkan",                  // 1163
            "Eiman",                        // 1165
            "Nin-an",                       // 1166
            "Ka\u014D",                     // 1169
            "Sh\u014Dan",                   // 1171
            "Angen",                        // 1175
            "Jish\u014D",                   // 1177
            "Y\u014Dwa",                    // 1181
            "Juei",                         // 1182
            "Genryuku",                     // 1184
            "Bunji",                        // 1185
            "Kenky\u016B",                  // 1190
            "Sh\u014Dji",                   // 1199
            "Kennin",                       // 1201
            "Genky\u016B",                  // 1204
            "Ken-ei",                       // 1206
            "Sh\u014Dgen",                  // 1207
            "Kenryaku",                     // 1211
            "Kenp\u014D",                   // 1213
            "Sh\u014Dky\u016B",             // 1219
            "J\u014D\u014D",                // 1222
            "Gennin",                       // 1224
            "Karoku",                       // 1225
            "Antei",                        // 1227
            "Kanki",                        // 1229
            "J\u014Dei",                    // 1232
            "Tempuku",                      // 1233
            "Bunryaku",                     // 1234
            "Katei",                        // 1235
            "Ryakunin",                     // 1238
            "En-\u014D",                    // 1239
            "Ninji",                        // 1240
            "Kangen",                       // 1243
            "H\u014Dji",                    // 1247
            "Kench\u014D",                  // 1249
            "K\u014Dgen",                   // 1256
            "Sh\u014Dka",                   // 1257
            "Sh\u014Dgen",                  // 1259
            "Bun-\u014D",                   // 1260
            "K\u014Dch\u014D",              // 1261
            "Bun-ei",                       // 1264
            "Kenji",                        // 1275
            "K\u014Dan",                    // 1278
            "Sh\u014D\u014D",               // 1288
            "Einin",                        // 1293
            "Sh\u014Dan",                   // 1299
            "Kengen",                       // 1302
            "Kagen",                        // 1303
            "Tokuji",                       // 1306
            "Enkei",                        // 1308
            "\u014Cch\u014D",               // 1311
            "Sh\u014Dwa",                   // 1312
            "Bunp\u014D",                   // 1317
            "Gen\u014D",                    // 1319
            "Genky\u014D",                  // 1321
            "Sh\u014Dch\u016B",             // 1324
            "Kareki",                       // 1326
            "Gentoku",                      // 1329
            "Genk\u014D",                   // 1331
            "Kemmu",                        // 1334
            "Engen",                        // 1336
            "K\u014Dkoku",                  // 1340
            "Sh\u014Dhei",                  // 1346
            "Kentoku",                      // 1370
            "Bunch\u0169",                  // 1372
            "Tenju",                        // 1375
            "K\u014Dwa",                    // 1381
            "Gench\u0169",                  // 1384
            "Meitoku",                      // 1384
            "K\u014Dryaku",                 // 1379
            "Kakei",                        // 1387
            "K\u014D\u014D",                // 1389
            "Meitoku",                      // 1390
            "\u014Cei",                     // 1394
            "Sh\u014Dch\u014D",             // 1428
            "Eiky\u014D",                   // 1429
            "Kakitsu",                      // 1441
            "Bun-an",                       // 1444
            "H\u014Dtoku",                  // 1449
            "Ky\u014Dtoku",                 // 1452
            "K\u014Dsh\u014D",              // 1455
            "Ch\u014Droku",                 // 1457
            "Kansh\u014D",                  // 1460
            "Bunsh\u014D",                  // 1466
            "\u014Cnin",                    // 1467
            "Bunmei",                       // 1469
            "Ch\u014Dky\u014D",             // 1487
            "Entoku",                       // 1489
            "Mei\u014D",                    // 1492
            "Bunki",                        // 1501
            "Eish\u014D",                   // 1504
            "Taiei",                        // 1521
            "Ky\u014Droku",                 // 1528
            "Tenmon",                       // 1532
            "K\u014Dji",                    // 1555
            "Eiroku",                       // 1558
            "Genki",                        // 1570
            "Tensh\u014D",                  // 1573
            "Bunroku",                      // 1592
            "Keich\u014D",                  // 1596
            "Genwa",                        // 1615
            "Kan-ei",                       // 1624
            "Sh\u014Dho",                   // 1644
            "Keian",                        // 1648
            "Sh\u014D\u014D",               // 1652
            "Meiryaku",                     // 1655
            "Manji",                        // 1658
            "Kanbun",                       // 1661
            "Enp\u014D",                    // 1673
            "Tenwa",                        // 1681
            "J\u014Dky\u014D",              // 1684
            "Genroku",                      // 1688
            "H\u014Dei",                    // 1704
            "Sh\u014Dtoku",                 // 1711
            "Ky\u014Dh\u014D",              // 1716
            "Genbun",                       // 1736
            "Kanp\u014D",                   // 1741
            "Enky\u014D",                   // 1744
            "Kan-en",                       // 1748
            "H\u014Dryaku",                 // 1751
            "Meiwa",                        // 1764
            "An-ei",                        // 1772
            "Tenmei",                       // 1781
            "Kansei",                       // 1789
            "Ky\u014Dwa",                   // 1801
            "Bunka",                        // 1804
            "Bunsei",                       // 1818
            "Tenp\u014D",                   // 1830
            "K\u014Dka",                    // 1844
            "Kaei",                         // 1848
            "Ansei",                        // 1854
            "Man-en",                       // 1860
            "Bunky\u016B",                  // 1861
            "Genji",                        // 1864
            "Kei\u014D",                    // 1865
            "Meiji",                        // 1868
            "Taish\u014D",                  // 1912
            "Sh\u014Dwa",                   // 1926
            "Heisei",                       // 1989
        } },
    };

    public synchronized Object[][] getContents() {
        return fContents;
    }
};
