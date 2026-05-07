// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2012-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.test.calendar;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.PersianCalendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import java.util.Date;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PersianTest extends CalendarTestFmwk {
    /** Test basic mapping to and from Gregorian. */
    @Test
    public void TestMapping() {
        final int[] DATA = {
            // (Note: months are 1-based)
            2011, 1, 11, 1389, 10, 21,
            1986, 2, 25, 1364, 12, 6,
            1934, 3, 14, 1312, 12, 23,
            2090, 3, 19, 1468, 12, 29,
            2007, 2, 22, 1385, 12, 3,
            1969, 12, 31, 1348, 10, 10,
            1945, 11, 12, 1324, 8, 21,
            1925, 3, 31, 1304, 1, 11,
            1996, 3, 19, 1374, 12, 29,
            1996, 3, 20, 1375, 1, 1,
            1997, 3, 20, 1375, 12, 30,
            1997, 3, 21, 1376, 1, 1,
            2008, 3, 19, 1386, 12, 29,
            2008, 3, 20, 1387, 1, 1,
            2004, 3, 19, 1382, 12, 29,
            2004, 3, 20, 1383, 1, 1,
            2006, 3, 20, 1384, 12, 29,
            2006, 3, 21, 1385, 1, 1,
            2005, 4, 20, 1384, 1, 31,
            2005, 4, 21, 1384, 2, 1,
            2005, 5, 21, 1384, 2, 31,
            2005, 5, 22, 1384, 3, 1,
            2005, 6, 21, 1384, 3, 31,
            2005, 6, 22, 1384, 4, 1,
            2005, 7, 22, 1384, 4, 31,
            2005, 7, 23, 1384, 5, 1,
            2005, 8, 22, 1384, 5, 31,
            2005, 8, 23, 1384, 6, 1,
            2005, 9, 22, 1384, 6, 31,
            2005, 9, 23, 1384, 7, 1,
            2005, 10, 22, 1384, 7, 30,
            2005, 10, 23, 1384, 8, 1,
            2005, 11, 21, 1384, 8, 30,
            2005, 11, 22, 1384, 9, 1,
            2005, 12, 21, 1384, 9, 30,
            2005, 12, 22, 1384, 10, 1,
            2006, 1, 20, 1384, 10, 30,
            2006, 1, 21, 1384, 11, 1,
            2006, 2, 19, 1384, 11, 30,
            2006, 2, 20, 1384, 12, 1,
            2006, 3, 20, 1384, 12, 29,
            2006, 3, 21, 1385, 1, 1,

            // The 2820-year cycle arithmetical algorithm would fail this one.
            2025, 3, 21, 1404, 1, 1,
        };

        Calendar cal = Calendar.getInstance(new ULocale("fa_IR@calendar=persian"));
        StringBuilder buf = new StringBuilder();

        logln("Gregorian -> Persian");

        Calendar grego = Calendar.getInstance();
        grego.clear();
        for (int i = 0; i < DATA.length; ) {
            grego.set(DATA[i++], DATA[i++] - 1, DATA[i++]);
            Date date = grego.getTime();
            cal.setTime(date);
            int y = cal.get(Calendar.YEAR);
            int m = cal.get(Calendar.MONTH) + 1; // 0-based -> 1-based
            int d = cal.get(Calendar.DAY_OF_MONTH);
            int yE = DATA[i++]; // Expected y, m, d
            int mE = DATA[i++]; // 1-based
            int dE = DATA[i++];
            buf.setLength(0);
            buf.append(date + " -> ");
            buf.append(y + "/" + m + "/" + d);
            if (y == yE && m == mE && d == dE) {
                logln("OK: " + buf.toString());
            } else {
                errln("Fail: " + buf.toString() + ", expected " + yE + "/" + mE + "/" + dE);
            }
        }

        logln("Persian -> Gregorian");
        for (int i = 0; i < DATA.length; ) {
            grego.set(DATA[i++], DATA[i++] - 1, DATA[i++]);
            Date dexp = grego.getTime();
            int cyear = DATA[i++];
            int cmonth = DATA[i++];
            int cdayofmonth = DATA[i++];
            cal.clear();
            cal.set(Calendar.YEAR, cyear);
            cal.set(Calendar.MONTH, cmonth - 1);
            cal.set(Calendar.DAY_OF_MONTH, cdayofmonth);
            Date date = cal.getTime();
            buf.setLength(0);
            buf.append(cyear + "/" + cmonth + "/" + cdayofmonth);
            buf.append(" -> " + date);
            if (date.equals(dexp)) {
                logln("OK: " + buf.toString());
            } else {
                errln("Fail: " + buf.toString() + ", expected " + dexp);
            }
        }
    }

    @Test
    public void TestCoverage12424() {
        class StubCalendar extends PersianCalendar {
            private static final long serialVersionUID = 1L;

            public StubCalendar() {
                assertEquals("Persian month 0 length", 31, handleGetMonthLength(1000, 0));
                assertEquals("Persian month 7 length", 30, handleGetMonthLength(1000, 7));

                int leastWeeks = handleGetLimit(Calendar.WEEK_OF_YEAR, Calendar.LEAST_MAXIMUM);
                assertEquals("Persian Week of Year least maximum", 52, leastWeeks);
            }
        }

        new StubCalendar();
    }

    // Test data copy from
    // https://github.com/unicode-org/icu4x/blob/main/components/calendar/src/persian.rs#L299
    final int[] PERSIAN_TEST_CASE_1 = {
        // rd,  year, month, day
        656786, 1178, 1, 1,
        664224, 1198, 5, 10,
        671401, 1218, 1, 7,
        694799, 1282, 1, 29,
        702806, 1304, 1, 1,
        704424, 1308, 6, 3,
        708842, 1320, 7, 7,
        709409, 1322, 1, 29,
        709580, 1322, 7, 14,
        727274, 1370, 12, 27,
        728714, 1374, 12, 6,
        739330, 1403, 12, 30,
        739331, 1404, 1, 1,
        744313, 1417, 8, 19,
        763436, 1469, 12, 30,
        763437, 1470, 1, 1,
        764652, 1473, 4, 28,
        775123, 1501, 12, 29,
        775488, 1502, 12, 29,
        775487, 1502, 12, 28,
        775488, 1502, 12, 29,
        775489, 1503, 1, 1,
        775490, 1503, 1, 2,
        1317873, 2987, 12, 29,
        1317874, 2988, 1, 1,
        1317875, 2988, 1, 2,
    };

    @Test
    public void TestPersianJulianDayToYMD() {
        Calendar cal =
                Calendar.getInstance(
                        TimeZone.getTimeZone("Asia/Tehran"), new ULocale("fa_IR@calendar=persian"));
        for (int i = 0; i < PERSIAN_TEST_CASE_1.length; ) {
            int rd = PERSIAN_TEST_CASE_1[i++];
            int year = PERSIAN_TEST_CASE_1[i++];
            int month = PERSIAN_TEST_CASE_1[i++];
            int day = PERSIAN_TEST_CASE_1[i++];
            int jday = rd + 1721425;
            cal.clear();
            cal.set(Calendar.JULIAN_DAY, jday);
            int actualYear = cal.get(Calendar.YEAR);
            int actualMonth = cal.get(Calendar.MONTH) + 1;
            int actualDay = cal.get(Calendar.DAY_OF_MONTH);
            if (actualYear != year || actualMonth != month || actualDay != day) {
                errln(
                        "Fail: rd "
                                + rd
                                + " = jday "
                                + jday
                                + " -> expect Persian("
                                + year
                                + "/"
                                + month
                                + "/"
                                + day
                                + ") "
                                + "actual Persian("
                                + actualYear
                                + "/"
                                + actualMonth
                                + "/"
                                + actualDay
                                + ")");
            }
        }
    }

    @Test
    public void TestPersianYMDToJulianDay() {
        Calendar cal =
                Calendar.getInstance(
                        TimeZone.getTimeZone("Asia/Tehran"), new ULocale("fa_IR@calendar=persian"));
        for (int i = 0; i < PERSIAN_TEST_CASE_1.length; ) {
            int rd = PERSIAN_TEST_CASE_1[i++];
            int year = PERSIAN_TEST_CASE_1[i++];
            int month = PERSIAN_TEST_CASE_1[i++];
            int day = PERSIAN_TEST_CASE_1[i++];
            int jday = rd + 1721425;
            cal.clear();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month - 1);
            cal.set(Calendar.DAY_OF_MONTH, day);
            int actualJday = cal.get(Calendar.JULIAN_DAY);
            int actualRD = actualJday - 1721425;
            if (actualRD != rd) {
                errln(
                        "Fail: Persian("
                                + year
                                + "/"
                                + month
                                + "/"
                                + day
                                + ") => "
                                + "expect rd "
                                + rd
                                + " but actual jd: "
                                + actualJday
                                + " = rd "
                                + actualRD);
            }
        }
    }

    // Test data copy from
    // https://github.com/unicode-org/icu4x/blob/main/components/calendar/src/persian.rs#L534
    // From https://calendar.ut.ac.ir/Fa/News/Data/Doc/KabiseShamsi1206-1498-new.pdf
    // Plain text version at https://github.com/roozbehp/persiancalendar/blob/main/kabise.txt
    final int[] PERSIAN_TEST_CASE_2 = {
        // pYear, pLeap, year, month, day
        1206, 0, 1827, 3, 22,
        1207, 0, 1828, 3, 21,
        1208, 0, 1829, 3, 21,
        1209, 0, 1830, 3, 21,
        1210, 1, 1831, 3, 21,
        1211, 0, 1832, 3, 21,
        1212, 0, 1833, 3, 21,
        1213, 0, 1834, 3, 21,
        1214, 1, 1835, 3, 21,
        1215, 0, 1836, 3, 21,
        1216, 0, 1837, 3, 21,
        1217, 0, 1838, 3, 21,
        1218, 1, 1839, 3, 21,
        1219, 0, 1840, 3, 21,
        1220, 0, 1841, 3, 21,
        1221, 0, 1842, 3, 21,
        1222, 1, 1843, 3, 21,
        1223, 0, 1844, 3, 21,
        1224, 0, 1845, 3, 21,
        1225, 0, 1846, 3, 21,
        1226, 1, 1847, 3, 21,
        1227, 0, 1848, 3, 21,
        1228, 0, 1849, 3, 21,
        1229, 0, 1850, 3, 21,
        1230, 1, 1851, 3, 21,
        1231, 0, 1852, 3, 21,
        1232, 0, 1853, 3, 21,
        1233, 0, 1854, 3, 21,
        1234, 1, 1855, 3, 21,
        1235, 0, 1856, 3, 21,
        1236, 0, 1857, 3, 21,
        1237, 0, 1858, 3, 21,
        1238, 1, 1859, 3, 21,
        1239, 0, 1860, 3, 21,
        1240, 0, 1861, 3, 21,
        1241, 0, 1862, 3, 21,
        1242, 0, 1863, 3, 21,
        1243, 1, 1864, 3, 20,
        1244, 0, 1865, 3, 21,
        1245, 0, 1866, 3, 21,
        1246, 0, 1867, 3, 21,
        1247, 1, 1868, 3, 20,
        1248, 0, 1869, 3, 21,
        1249, 0, 1870, 3, 21,
        1250, 0, 1871, 3, 21,
        1251, 1, 1872, 3, 20,
        1252, 0, 1873, 3, 21,
        1253, 0, 1874, 3, 21,
        1254, 0, 1875, 3, 21,
        1255, 1, 1876, 3, 20,
        1256, 0, 1877, 3, 21,
        1257, 0, 1878, 3, 21,
        1258, 0, 1879, 3, 21,
        1259, 1, 1880, 3, 20,
        1260, 0, 1881, 3, 21,
        1261, 0, 1882, 3, 21,
        1262, 0, 1883, 3, 21,
        1263, 1, 1884, 3, 20,
        1264, 0, 1885, 3, 21,
        1265, 0, 1886, 3, 21,
        1266, 0, 1887, 3, 21,
        1267, 1, 1888, 3, 20,
        1268, 0, 1889, 3, 21,
        1269, 0, 1890, 3, 21,
        1270, 0, 1891, 3, 21,
        1271, 1, 1892, 3, 20,
        1272, 0, 1893, 3, 21,
        1273, 0, 1894, 3, 21,
        1274, 0, 1895, 3, 21,
        1275, 0, 1896, 3, 20,
        1276, 1, 1897, 3, 20,
        1277, 0, 1898, 3, 21,
        1278, 0, 1899, 3, 21,
        1279, 0, 1900, 3, 21,
        1280, 1, 1901, 3, 21,
        1281, 0, 1902, 3, 22,
        1282, 0, 1903, 3, 22,
        1283, 0, 1904, 3, 21,
        1284, 1, 1905, 3, 21,
        1285, 0, 1906, 3, 22,
        1286, 0, 1907, 3, 22,
        1287, 0, 1908, 3, 21,
        1288, 1, 1909, 3, 21,
        1289, 0, 1910, 3, 22,
        1290, 0, 1911, 3, 22,
        1291, 0, 1912, 3, 21,
        1292, 1, 1913, 3, 21,
        1293, 0, 1914, 3, 22,
        1294, 0, 1915, 3, 22,
        1295, 0, 1916, 3, 21,
        1296, 1, 1917, 3, 21,
        1297, 0, 1918, 3, 22,
        1298, 0, 1919, 3, 22,
        1299, 0, 1920, 3, 21,
        1300, 1, 1921, 3, 21,
        1301, 0, 1922, 3, 22,
        1302, 0, 1923, 3, 22,
        1303, 0, 1924, 3, 21,
        1304, 1, 1925, 3, 21,
        1305, 0, 1926, 3, 22,
        1306, 0, 1927, 3, 22,
        1307, 0, 1928, 3, 21,
        1308, 0, 1929, 3, 21,
        1309, 1, 1930, 3, 21,
        1310, 0, 1931, 3, 22,
        1311, 0, 1932, 3, 21,
        1312, 0, 1933, 3, 21,
        1313, 1, 1934, 3, 21,
        1314, 0, 1935, 3, 22,
        1315, 0, 1936, 3, 21,
        1316, 0, 1937, 3, 21,
        1317, 1, 1938, 3, 21,
        1318, 0, 1939, 3, 22,
        1319, 0, 1940, 3, 21,
        1320, 0, 1941, 3, 21,
        1321, 1, 1942, 3, 21,
        1322, 0, 1943, 3, 22,
        1323, 0, 1944, 3, 21,
        1324, 0, 1945, 3, 21,
        1325, 1, 1946, 3, 21,
        1326, 0, 1947, 3, 22,
        1327, 0, 1948, 3, 21,
        1328, 0, 1949, 3, 21,
        1329, 1, 1950, 3, 21,
        1330, 0, 1951, 3, 22,
        1331, 0, 1952, 3, 21,
        1332, 0, 1953, 3, 21,
        1333, 1, 1954, 3, 21,
        1334, 0, 1955, 3, 22,
        1335, 0, 1956, 3, 21,
        1336, 0, 1957, 3, 21,
        1337, 1, 1958, 3, 21,
        1338, 0, 1959, 3, 22,
        1339, 0, 1960, 3, 21,
        1340, 0, 1961, 3, 21,
        1341, 0, 1962, 3, 21,
        1342, 1, 1963, 3, 21,
        1343, 0, 1964, 3, 21,
        1344, 0, 1965, 3, 21,
        1345, 0, 1966, 3, 21,
        1346, 1, 1967, 3, 21,
        1347, 0, 1968, 3, 21,
        1348, 0, 1969, 3, 21,
        1349, 0, 1970, 3, 21,
        1350, 1, 1971, 3, 21,
        1351, 0, 1972, 3, 21,
        1352, 0, 1973, 3, 21,
        1353, 0, 1974, 3, 21,
        1354, 1, 1975, 3, 21,
        1355, 0, 1976, 3, 21,
        1356, 0, 1977, 3, 21,
        1357, 0, 1978, 3, 21,
        1358, 1, 1979, 3, 21,
        1359, 0, 1980, 3, 21,
        1360, 0, 1981, 3, 21,
        1361, 0, 1982, 3, 21,
        1362, 1, 1983, 3, 21,
        1363, 0, 1984, 3, 21,
        1364, 0, 1985, 3, 21,
        1365, 0, 1986, 3, 21,
        1366, 1, 1987, 3, 21,
        1367, 0, 1988, 3, 21,
        1368, 0, 1989, 3, 21,
        1369, 0, 1990, 3, 21,
        1370, 1, 1991, 3, 21,
        1371, 0, 1992, 3, 21,
        1372, 0, 1993, 3, 21,
        1373, 0, 1994, 3, 21,
        1374, 0, 1995, 3, 21,
        1375, 1, 1996, 3, 20,
        1376, 0, 1997, 3, 21,
        1377, 0, 1998, 3, 21,
        1378, 0, 1999, 3, 21,
        1379, 1, 2000, 3, 20,
        1380, 0, 2001, 3, 21,
        1381, 0, 2002, 3, 21,
        1382, 0, 2003, 3, 21,
        1383, 1, 2004, 3, 20,
        1384, 0, 2005, 3, 21,
        1385, 0, 2006, 3, 21,
        1386, 0, 2007, 3, 21,
        1387, 1, 2008, 3, 20,
        1388, 0, 2009, 3, 21,
        1389, 0, 2010, 3, 21,
        1390, 0, 2011, 3, 21,
        1391, 1, 2012, 3, 20,
        1392, 0, 2013, 3, 21,
        1393, 0, 2014, 3, 21,
        1394, 0, 2015, 3, 21,
        1395, 1, 2016, 3, 20,
        1396, 0, 2017, 3, 21,
        1397, 0, 2018, 3, 21,
        1398, 0, 2019, 3, 21,
        1399, 1, 2020, 3, 20,
        1400, 0, 2021, 3, 21,
        1401, 0, 2022, 3, 21,
        1402, 0, 2023, 3, 21,
        1403, 1, 2024, 3, 20,
        1404, 0, 2025, 3, 21,
        1405, 0, 2026, 3, 21,
        1406, 0, 2027, 3, 21,
        1407, 0, 2028, 3, 20,
        1408, 1, 2029, 3, 20,
        1409, 0, 2030, 3, 21,
        1410, 0, 2031, 3, 21,
        1411, 0, 2032, 3, 20,
        1412, 1, 2033, 3, 20,
        1413, 0, 2034, 3, 21,
        1414, 0, 2035, 3, 21,
        1415, 0, 2036, 3, 20,
        1416, 1, 2037, 3, 20,
        1417, 0, 2038, 3, 21,
        1418, 0, 2039, 3, 21,
        1419, 0, 2040, 3, 20,
        1420, 1, 2041, 3, 20,
        1421, 0, 2042, 3, 21,
        1422, 0, 2043, 3, 21,
        1423, 0, 2044, 3, 20,
        1424, 1, 2045, 3, 20,
        1425, 0, 2046, 3, 21,
        1426, 0, 2047, 3, 21,
        1427, 0, 2048, 3, 20,
        1428, 1, 2049, 3, 20,
        1429, 0, 2050, 3, 21,
        1430, 0, 2051, 3, 21,
        1431, 0, 2052, 3, 20,
        1432, 1, 2053, 3, 20,
        1433, 0, 2054, 3, 21,
        1434, 0, 2055, 3, 21,
        1435, 0, 2056, 3, 20,
        1436, 1, 2057, 3, 20,
        1437, 0, 2058, 3, 21,
        1438, 0, 2059, 3, 21,
        1439, 0, 2060, 3, 20,
        1440, 0, 2061, 3, 20,
        1441, 1, 2062, 3, 20,
        1442, 0, 2063, 3, 21,
        1443, 0, 2064, 3, 20,
        1444, 0, 2065, 3, 20,
        1445, 1, 2066, 3, 20,
        1446, 0, 2067, 3, 21,
        1447, 0, 2068, 3, 20,
        1448, 0, 2069, 3, 20,
        1449, 1, 2070, 3, 20,
        1450, 0, 2071, 3, 21,
        1451, 0, 2072, 3, 20,
        1452, 0, 2073, 3, 20,
        1453, 1, 2074, 3, 20,
        1454, 0, 2075, 3, 21,
        1455, 0, 2076, 3, 20,
        1456, 0, 2077, 3, 20,
        1457, 1, 2078, 3, 20,
        1458, 0, 2079, 3, 21,
        1459, 0, 2080, 3, 20,
        1460, 0, 2081, 3, 20,
        1461, 1, 2082, 3, 20,
        1462, 0, 2083, 3, 21,
        1463, 0, 2084, 3, 20,
        1464, 0, 2085, 3, 20,
        1465, 1, 2086, 3, 20,
        1466, 0, 2087, 3, 21,
        1467, 0, 2088, 3, 20,
        1468, 0, 2089, 3, 20,
        1469, 1, 2090, 3, 20,
        1470, 0, 2091, 3, 21,
        1471, 0, 2092, 3, 20,
        1472, 0, 2093, 3, 20,
        1473, 0, 2094, 3, 20,
        1474, 1, 2095, 3, 20,
        1475, 0, 2096, 3, 20,
        1476, 0, 2097, 3, 20,
        1477, 0, 2098, 3, 20,
        1478, 1, 2099, 3, 20,
        1479, 0, 2100, 3, 21,
        1480, 0, 2101, 3, 21,
        1481, 0, 2102, 3, 21,
        1482, 1, 2103, 3, 21,
        1483, 0, 2104, 3, 21,
        1484, 0, 2105, 3, 21,
        1485, 0, 2106, 3, 21,
        1486, 1, 2107, 3, 21,
        1487, 0, 2108, 3, 21,
        1488, 0, 2109, 3, 21,
        1489, 0, 2110, 3, 21,
        1490, 1, 2111, 3, 21,
        1491, 0, 2112, 3, 21,
        1492, 0, 2113, 3, 21,
        1493, 0, 2114, 3, 21,
        1494, 1, 2115, 3, 21,
        1495, 0, 2116, 3, 21,
        1496, 0, 2117, 3, 21,
        1497, 0, 2118, 3, 21,
        1498, 1, 2119, 3, 21,
    };

    @Test
    public void TestPersianJan1ToGregorian() {
        Calendar gcal =
                Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"), new ULocale("en"));
        Calendar cal =
                Calendar.getInstance(
                        TimeZone.getTimeZone("Asia/Tehran"), new ULocale("fa_IR@calendar=persian"));
        for (int i = 0; i < PERSIAN_TEST_CASE_2.length; ) {
            int pYear = PERSIAN_TEST_CASE_2[i++];
            boolean pLeap = PERSIAN_TEST_CASE_2[i++] != 0;
            int year = PERSIAN_TEST_CASE_2[i++];
            int month = PERSIAN_TEST_CASE_2[i++];
            int day = PERSIAN_TEST_CASE_2[i++];
            cal.clear();
            cal.set(Calendar.YEAR, pYear);
            cal.set(Calendar.MONTH, 0);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            gcal.setTime(cal.getTime());
            int actualYear = gcal.get(Calendar.YEAR);
            int actualMonth = gcal.get(Calendar.MONTH) + 1;
            int actualDay = gcal.get(Calendar.DAY_OF_MONTH);
            if (actualYear != year || actualMonth != month || actualDay != day) {
                errln(
                        "Fail: Persian("
                                + pYear
                                + ", 1, 1) => "
                                + "expect Gregorian("
                                + year
                                + "/"
                                + month
                                + "/"
                                + day
                                + ") "
                                + "actual Gregorian("
                                + actualYear
                                + "/"
                                + actualMonth
                                + "/"
                                + actualDay
                                + ")");
            }
        }
    }

    @Test
    public void TestGregorianToPersian() {
        Calendar gcal =
                Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"), new ULocale("en"));
        Calendar cal =
                Calendar.getInstance(
                        TimeZone.getTimeZone("Asia/Tehran"), new ULocale("fa_IR@calendar=persian"));
        for (int i = 0; i < PERSIAN_TEST_CASE_2.length; ) {
            int pYear = PERSIAN_TEST_CASE_2[i++];
            boolean pLeap = PERSIAN_TEST_CASE_2[i++] != 0;
            int year = PERSIAN_TEST_CASE_2[i++];
            int month = PERSIAN_TEST_CASE_2[i++];
            int day = PERSIAN_TEST_CASE_2[i++];
            gcal.clear();
            gcal.set(Calendar.YEAR, year);
            gcal.set(Calendar.MONTH, month - 1);
            gcal.set(Calendar.DAY_OF_MONTH, day);
            cal.setTime(gcal.getTime());
            int persianYear = cal.get(Calendar.YEAR);
            int persianMonth = cal.get(Calendar.MONTH) + 1;
            int persianDay = cal.get(Calendar.DAY_OF_MONTH);
            if (persianYear != pYear || persianMonth != 1 || persianDay != 1) {
                errln(
                        "Fail: Gregorian("
                                + year
                                + "/"
                                + month
                                + "/"
                                + day
                                + ") "
                                + " => expect Persian("
                                + pYear
                                + "/1/1) actual "
                                + "Persian("
                                + persianYear
                                + "/"
                                + persianMonth
                                + "/"
                                + persianDay
                                + ")");
            }
        }
    }
}
