/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/HebrewHoliday.java,v $ 
 * $Date: 2001/02/26 22:21:15 $ 
 * $Revision: 1.3 $
 *
 *****************************************************************************************
 */

package com.ibm.util;

public class HebrewHoliday extends Holiday
{
    private static final HebrewCalendar gCalendar = new HebrewCalendar();

    /**
     * Construct a holiday defined in reference to the Hebrew calendar.
     *
     * @param name The name of the holiday
     */
    public HebrewHoliday(int month, int date, String name)
    {
        this(month, date, 1, name);
    }

    public HebrewHoliday(int month, int date, int length, String name)
    {
        super(name, null);

        SimpleDateRule rule = new SimpleDateRule(month, date);
        rule.setCalendar(gCalendar);

        setRule(rule);
    }

    public static HebrewHoliday
        ROSH_HASHANAH   = new HebrewHoliday(HebrewCalendar.TISHRI,  1,  2,  "Rosh Hashanah"),
        GEDALIAH        = new HebrewHoliday(HebrewCalendar.TISHRI,  3,      "Fast of Gedaliah"),
        YOM_KIPPUR      = new HebrewHoliday(HebrewCalendar.TISHRI, 10,      "Yom Kippur"),
        SUKKOT          = new HebrewHoliday(HebrewCalendar.TISHRI, 15,  6,  "Sukkot"),
        HOSHANAH_RABBAH = new HebrewHoliday(HebrewCalendar.TISHRI, 21,      "Hoshanah Rabbah"),
        SHEMINI_ATZERET = new HebrewHoliday(HebrewCalendar.TISHRI, 22,      "Shemini Atzeret"),
        SIMCHAT_TORAH   = new HebrewHoliday(HebrewCalendar.TISHRI, 23,      "Simchat Torah"),
        HANUKKAH        = new HebrewHoliday(HebrewCalendar.KISLEV, 25,      "Hanukkah"),
        TEVET_10        = new HebrewHoliday(HebrewCalendar.TEVET,  10,      "Fast of Tevet 10"),
        TU_BSHEVAT      = new HebrewHoliday(HebrewCalendar.SHEVAT, 15,      "Tu B'Shevat"),
        ESTHER          = new HebrewHoliday(HebrewCalendar.ADAR,   13,      "Fast of Esther"),
        PURIM           = new HebrewHoliday(HebrewCalendar.ADAR,   14,      "Purim"),
        SHUSHAN_PURIM   = new HebrewHoliday(HebrewCalendar.ADAR,   15,      "Shushan Purim"),
        PASSOVER        = new HebrewHoliday(HebrewCalendar.NISAN,  15,  8,  "Passover"),
        YOM_HASHOAH     = new HebrewHoliday(HebrewCalendar.NISAN,  27,      "Yom Hashoah"),
        YOM_HAZIKARON   = new HebrewHoliday(HebrewCalendar.IYAR,    4,      "Yom Hazikaron"),
        YOM_HAATZMAUT   = new HebrewHoliday(HebrewCalendar.IYAR,    5,      "Yom Ha'Atzmaut"),
        PESACH_SHEINI   = new HebrewHoliday(HebrewCalendar.IYAR,   14,      "Pesach Sheini"),
        LAG_BOMER       = new HebrewHoliday(HebrewCalendar.IYAR,   18,      "Lab B'Omer"),
        YOM_YERUSHALAYIM= new HebrewHoliday(HebrewCalendar.IYAR,   28,      "Yom Yerushalayim"),
        SHAVUOT         = new HebrewHoliday(HebrewCalendar.SIVAN,   6,  2,  "Shavuot"),
        TAMMUZ_17       = new HebrewHoliday(HebrewCalendar.TAMUZ,  17,      "Fast of Tammuz 17"),
        TISHA_BAV       = new HebrewHoliday(HebrewCalendar.AV,      9,      "Fast of Tisha B'Av"),
        SELIHOT         = new HebrewHoliday(HebrewCalendar.ELUL,   21,      "Selihot");

}
