/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

/**
 * Japanese Kanji version of the Date Format symbols for the Japanese calendar
 */
public class JapaneseCalendarSymbols_ja extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1998 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "DateTimePatterns",
            new String[] {
                "H'\u6642'mm'\u5206'ss'\u79d2'z", // full time pattern
                "H:mm:ss:z", // long time pattern
                "H:mm:ss", // medium time pattern
                "H:mm", // short time pattern
                "G yy'\u5e74'M'\u6708'd'\u65e5'", // full date pattern
                "G yy/MM/dd", // long date pattern
                "G yy/MM/dd", // medium date pattern
                "G yy/MM/dd", // short date pattern
                "{1} {0}" // date-time pattern
            }
        },
        { "Eras", new String[] {
            "\u5927\u5316",             //  645 Taika
            "\u767D\u96C9",             //  650 Hakuchi
            "\u767D\u9CEF",             //  672 Hakuho
            "\u6731\u9CE5",             //  686 Shucho
            "\u5927\u5B9D",             //  701 Taiho
            "\u6176\u96F2",             //  704 Keiun
            "\u548C\u9285",             //  708 Wado
            "\u970A\u4E80",             //  715 Reiki
            "\u990A\u8001",             //  717 Yoro
            "\u795E\u4E80",             //  724 Jinki
            "\u5929\u5E73",             //  729 Tempyo
            "\u5929\u5E73\u611F\u5B9D", //  749 Tempyo-kampo
            "\u5929\u5E73\u52DD\u5B9D", //  749 Tempyo-shoho
            "\u5929\u5E73\u5B9D\u5B57", //  757 Tempyo-hoji
            "\u5929\u5E73\u795E\u8B77", //  765 Tempho-jingo
            "\u795E\u8B77\u666F\u96F2", //  767 Jingo-keiun
            "\u5B9D\u4E80",             //  770 Hoki
            "\u5929\u5FDC",             //  781 Ten-o
            "\u5EF6\u66A6",             //  782 Enryaku
            "\u5927\u540C",             //  806 Daido
            "\u5F18\u4EC1",             //  810 Konin
            "\u5929\u9577",             //  824 Tencho
            "\u627F\u548C",             //  834 Showa
            "\u5609\u7965",             //  848 Kajo
            "\u4EC1\u5BFF",             //  851 Ninju
            "\u6589\u8861",             //  854 Saiko
            "\u5929\u5B89",             //  857 Tennan
            "\u8C9E\u89B3",             //  859 Jogan
            "\u5143\u6176",             //  877 Genkei
            "\u4EC1\u548C",             //  885 Ninna
            "\u5BDB\u5E73",             //  889 Kampyo
            "\u660C\u6CF0",             //  898 Shotai
            "\u5EF6\u559C",             //  901 Engi
            "\u5EF6\u9577",             //  923 Encho
            "\u627F\u5E73",             //  931 Shohei
            "\u5929\u6176",             //  938 Tengyo
            "\u5929\u66A6",             //  947 Tenryaku
            "\u5929\u5FB3",             //  957 Tentoku
            "\u5FDC\u548C",             //  961 Owa
            "\u5EB7\u4FDD",             //  964 Koho
            "\u5B89\u548C",             //  968 Anna
            "\u5929\u7984",             //  970 Tenroku
            "\u5929\u5EF6",             //  973 Ten-en
            "\u8C9E\u5143",             //  976 Jogen
            "\u5929\u5143",             //  978 Tengen
            "\u6C38\u89B3",             //  983 Eikan
            "\u5BDB\u548C",             //  985 Kanna
            "\u6C38\u5EF6",             //  987 Ei-en
            "\u6C38\u795A",             //  989 Eiso
            "\u6B63\u66A6",             //  990 Shoryaku
            "\u9577\u5FB3",             //  995 Chotoku
            "\u9577\u4FDD",             //  999 Choho
            "\u5BDB\u5F18",             // 1004 Kanko
            "\u9577\u548C",             // 1012 Chowa
            "\u5BDB\u4EC1",             // 1017 Kannin
            "\u6CBB\u5B89",             // 1021 Jian
            "\u4E07\u5BFF",             // 1024 Manju
            "\u9577\u5143",             // 1028 Chogen
            "\u9577\u66A6",             // 1037 Choryaku
            "\u9577\u4E45",             // 1040 Chokyu
            "\u5BDB\u5FB3",             // 1044 Kantoku
            "\u6C38\u627F",             // 1046 Eisho
            "\u5929\u559C",             // 1053 Tengi
            "\u5EB7\u5E73",             // 1058 Kohei
            "\u6CBB\u66A6",             // 1065 Jiryaku
            "\u5EF6\u4E45",             // 1069 Enkyu
            "\u627F\u4FDD",             // 1074 Shoho
            "\u627F\u66A6",             // 1077 Shoryaku
            "\u6C38\u4FDD",             // 1081 Eiho
            "\u5FDC\u5FB3",             // 1084 Otoku
            "\u5BDB\u6CBB",             // 1087 Kanji
            "\u5609\u4FDD",             // 1094 Kaho
            "\u6C38\u9577",             // 1096 Eicho
            "\u627F\u5FB3",             // 1097 Shotoku
            "\u5EB7\u548C",             // 1099 Kowa
            "\u9577\u6CBB",             // 1104 Choji
            "\u5609\u627F",             // 1106 Kasho
            "\u5929\u4EC1",             // 1108 Tennin
            "\u5929\u6C38",             // 1110 Ten-ei
            "\u6C38\u4E45",             // 1113 Eikyu
            "\u5143\u6C38",             // 1118 Gen-ei
            "\u4FDD\u5B89",             // 1120 Hoan
            "\u5929\u6CBB",             // 1124 Tenji
            "\u5927\u6CBB",             // 1126 Daiji
            "\u5929\u627F",             // 1131 Tensho
            "\u9577\u627F",             // 1132 Chosho
            "\u4FDD\u5EF6",             // 1135 Hoen
            "\u6C38\u6CBB",             // 1141 Eiji
            "\u5EB7\u6CBB",             // 1142 Koji
            "\u5929\u990A",             // 1144 Tenyo
            "\u4E45\u5B89",             // 1145 Kyuan
            "\u4EC1\u5E73",             // 1151 Ninpei
            "\u4E45\u5BFF",             // 1154 Kyuju
            "\u4FDD\u5143",             // 1156 Hogen
            "\u5E73\u6CBB",             // 1159 Heiji
            "\u6C38\u66A6",             // 1160 Eiryaku
            "\u5FDC\u4FDD",             // 1161 Oho
            "\u9577\u5BDB",             // 1163 Chokan
            "\u6C38\u4E07",             // 1165 Eiman
            "\u4EC1\u5B89",             // 1166 Nin-an
            "\u5609\u5FDC",             // 1169 Kao
            "\u627F\u5B89",             // 1171 Shoan
            "\u5B89\u5143",             // 1175 Angen
            "\u6CBB\u627F",             // 1177 Jisho
            "\u990A\u548C",             // 1181 Yowa
            "\u5BFF\u6C38",             // 1182 Juei
            "\u5143\u66A6",             // 1184 Genryuku
            "\u6587\u6CBB",             // 1185 Bunji
            "\u5EFA\u4E45",             // 1190 Kenkyu
            "\u6B63\u6CBB",             // 1199 Shoji
            "\u5EFA\u4EC1",             // 1201 Kennin
            "\u5143\u4E45",             // 1204 Genkyu
            "\u5EFA\u6C38",             // 1206 Ken-ei
            "\u627F\u5143",             // 1207 Shogen
            "\u5EFA\u66A6",             // 1211 Kenryaku
            "\u5EFA\u4FDD",             // 1213 Kenpo
            "\u627F\u4E45",             // 1219 Shokyu
            "\u8C9E\u5FDC",             // 1222 Joo
            "\u5143\u4EC1",             // 1224 Gennin
            "\u5609\u7984",             // 1225 Karoku
            "\u5B89\u8C9E",             // 1227 Antei
            "\u5BDB\u559C",             // 1229 Kanki
            "\u8C9E\u6C38",             // 1232 Joei
            "\u5929\u798F",             // 1233 Tempuku
            "\u6587\u66A6",             // 1234 Bunryaku
            "\u5609\u798E",             // 1235 Katei
            "\u66A6\u4EC1",             // 1238 Ryakunin
            "\u5EF6\u5FDC",             // 1239 En-o
            "\u4EC1\u6CBB",             // 1240 Ninji
            "\u5BDB\u5143",             // 1243 Kangen
            "\u5B9D\u6CBB",             // 1247 Hoji
            "\u5EFA\u9577",             // 1249 Kencho
            "\u5EB7\u5143",             // 1256 Kogen
            "\u6B63\u5609",             // 1257 Shoka
            "\u6B63\u5143",             // 1259 Shogen
            "\u6587\u5FDC",             // 1260 Bun-o
            "\u5F18\u9577",             // 1261 Kocho
            "\u6587\u6C38",             // 1264 Bun-ei
            "\u5EFA\u6CBB",             // 1275 Kenji
            "\u5F18\u5B89",             // 1278 Koan
            "\u6B63\u5FDC",             // 1288 Shoo
            "\u6C38\u4EC1",             // 1293 Einin
            "\u6B63\u5B89",             // 1299 Shoan
            "\u4E7E\u5143",             // 1302 Kengen
            "\u5609\u5143",             // 1303 Kagen
            "\u5FB3\u6CBB",             // 1306 Tokuji
            "\u5EF6\u6176",             // 1308 Enkei
            "\u5FDC\u9577",             // 1311 Ocho
            "\u6B63\u548C",             // 1312 Showa
            "\u6587\u4FDD",             // 1317 Bunpo
            "\u5143\u5FDC",             // 1319 Geno
            "\u5143\u4EA8",             // 1321 Genkyo
            "\u6B63\u4E2D",             // 1324 Shochu
            "\u5609\u66A6",             // 1326 Kareki
            "\u5143\u5FB3",             // 1329 Gentoku
            "\u5143\u5F18",             // 1331 Genko
            "\u5EFA\u6B66",             // 1334 Kemmu
            "\u5EF6\u5143",             // 1336 Engen
            "\u8208\u56FD",             // 1340 Kokoku
            "\u6B63\u5E73",             // 1346 Shohei
            "\u5EFA\u5FB3",             // 1370 Kentoku
            "\u6587\u4E2D",             // 1372 Bunchu
            "\u5929\u6388",             // 1375 Tenju
            "\u5F18\u548C",             // 1381 Kowa
            "\u5143\u4E2D",             // 1384 Genchu
            "\u81F3\u5FB3",             // 1384 Meitoku
            "\u5EB7\u66A6",             // 1379 Koryaku
            "\u5609\u6176",             // 1387 Kakei
            "\u5EB7\u5FDC",             // 1389 Koo
            "\u660E\u5FB3",             // 1390 Meitoku
            "\u5FDC\u6C38",             // 1394 Oei
            "\u6B63\u9577",             // 1428 Shocho
            "\u6C38\u4EAB",             // 1429 Eikyo
            "\u5609\u5409",             // 1441 Kakitsu
            "\u6587\u5B89",             // 1444 Bun-an
            "\u5B9D\u5FB3",             // 1449 Hotoku
            "\u4EAB\u5FB3",             // 1452 Kyotoku
            "\u5EB7\u6B63",             // 1455 Kosho
            "\u9577\u7984",             // 1457 Choroku
            "\u5BDB\u6B63",             // 1460 Kansho
            "\u6587\u6B63",             // 1466 Bunsho
            "\u5FDC\u4EC1",             // 1467 Onin
            "\u6587\u660E",             // 1469 Bunmei
            "\u9577\u4EAB",             // 1487 Chokyo
            "\u5EF6\u5FB3",             // 1489 Entoku
            "\u660E\u5FDC",             // 1492 Meio
            "\u6587\u4E80",             // 1501 Bunki
            "\u6C38\u6B63",             // 1504 Eisho
            "\u5927\u6C38",             // 1521 Taiei
            "\u4EAB\u7984",             // 1528 Kyoroku
            "\u5929\u6587",             // 1532 Tenmon
            "\u5F18\u6CBB",             // 1555 Koji
            "\u6C38\u7984",             // 1558 Eiroku
            "\u5143\u4E80",             // 1570 Genki
            "\u5929\u6B63",             // 1573 Tensho
            "\u6587\u7984",             // 1592 Bunroku
            "\u6176\u9577",             // 1596 Keicho
            "\u5143\u548C",             // 1615 Genwa
            "\u5BDB\u6C38",             // 1624 Kan-ei
            "\u6B63\u4FDD",             // 1644 Shoho
            "\u6176\u5B89",             // 1648 Keian
            "\u627F\u5FDC",             // 1652 Shoo
            "\u660E\u66A6",             // 1655 Meiryaku
            "\u4E07\u6CBB",             // 1658 Manji
            "\u5BDB\u6587",             // 1661 Kanbun
            "\u5EF6\u5B9D",             // 1673 Enpo
            "\u5929\u548C",             // 1681 Tenwa
            "\u8C9E\u4EAB",             // 1684 Jokyo
            "\u5143\u7984",             // 1688 Genroku
            "\u5B9D\u6C38",             // 1704 Hoei
            "\u6B63\u5FB3",             // 1711 Shotoku
            "\u4EAB\u4FDD",             // 1716 Kyoho
            "\u5143\u6587",             // 1736 Genbun
            "\u5BDB\u4FDD",             // 1741 Kanpo
            "\u5EF6\u4EAB",             // 1744 Enkyo
            "\u5BDB\u5EF6",             // 1748 Kan-en
            "\u5B9D\u66A6",             // 1751 Horyaku
            "\u660E\u548C",             // 1764 Meiwa
            "\u5B89\u6C38",             // 1772 An-ei
            "\u5929\u660E",             // 1781 Tenmei
            "\u5BDB\u653F",             // 1789 Kansei
            "\u4EAB\u548C",             // 1801 Kyowa
            "\u6587\u5316",             // 1804 Bunka
            "\u6587\u653F",             // 1818 Bunsei
            "\u5929\u4FDD",             // 1830 Tenpo
            "\u5F18\u5316",             // 1844 Koka
            "\u5609\u6C38",             // 1848 Kaei
            "\u5B89\u653F",             // 1854 Ansei
            "\u4E07\u5EF6",             // 1860 Man-en
            "\u6587\u4E45",             // 1861 Bunkyu
            "\u5143\u6CBB",             // 1864 Genji
            "\u6176\u5FDC",             // 1865 Keio
            "\u660E\u6CBB",             // 1868 Meiji
            "\u5927\u6B63",             // 1912 Taisho
            "\u662D\u548C",             // 1926 Showa
            "\u5E73\u6210",             // 1989 Heisei
        } },
    };

    public synchronized Object[][] getContents() {
        return fContents;
    }
};
