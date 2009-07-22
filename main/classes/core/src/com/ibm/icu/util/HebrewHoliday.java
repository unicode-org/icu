/*
 *******************************************************************************
 * Copyright (C) 1996-2006, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.util;

/**
 * @draft ICU 2.8 (retainAll)
 * @provisional This API might change or be removed in a future release.
 */
public class HebrewHoliday extends Holiday
{
    private static final HebrewCalendar gCalendar = new HebrewCalendar();

    /**
     * Construct a holiday defined in reference to the Hebrew calendar.
     *
     * @param name The name of the holiday
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public HebrewHoliday(int month, int date, String name)
    {
        this(month, date, 1, name);
    }

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public HebrewHoliday(int month, int date, int length, String name)
    {
        super(name, new SimpleDateRule(month, date, gCalendar));
    }

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday ROSH_HASHANAH   = new HebrewHoliday(HebrewCalendar.TISHRI,  1,  2,  "Rosh Hashanah");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday GEDALIAH        = new HebrewHoliday(HebrewCalendar.TISHRI,  3,      "Fast of Gedaliah");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday YOM_KIPPUR      = new HebrewHoliday(HebrewCalendar.TISHRI, 10,      "Yom Kippur");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday SUKKOT          = new HebrewHoliday(HebrewCalendar.TISHRI, 15,  6,  "Sukkot");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday HOSHANAH_RABBAH = new HebrewHoliday(HebrewCalendar.TISHRI, 21,      "Hoshanah Rabbah");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday SHEMINI_ATZERET = new HebrewHoliday(HebrewCalendar.TISHRI, 22,      "Shemini Atzeret");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday SIMCHAT_TORAH   = new HebrewHoliday(HebrewCalendar.TISHRI, 23,      "Simchat Torah");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday HANUKKAH        = new HebrewHoliday(HebrewCalendar.KISLEV, 25,      "Hanukkah");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday TEVET_10        = new HebrewHoliday(HebrewCalendar.TEVET,  10,      "Fast of Tevet 10");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday TU_BSHEVAT      = new HebrewHoliday(HebrewCalendar.SHEVAT, 15,      "Tu B'Shevat");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday ESTHER          = new HebrewHoliday(HebrewCalendar.ADAR,   13,      "Fast of Esther");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday PURIM           = new HebrewHoliday(HebrewCalendar.ADAR,   14,      "Purim");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday SHUSHAN_PURIM   = new HebrewHoliday(HebrewCalendar.ADAR,   15,      "Shushan Purim");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday PASSOVER        = new HebrewHoliday(HebrewCalendar.NISAN,  15,  8,  "Passover");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday YOM_HASHOAH     = new HebrewHoliday(HebrewCalendar.NISAN,  27,      "Yom Hashoah");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday YOM_HAZIKARON   = new HebrewHoliday(HebrewCalendar.IYAR,    4,      "Yom Hazikaron");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday YOM_HAATZMAUT   = new HebrewHoliday(HebrewCalendar.IYAR,    5,      "Yom Ha'Atzmaut");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday PESACH_SHEINI   = new HebrewHoliday(HebrewCalendar.IYAR,   14,      "Pesach Sheini");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday LAG_BOMER       = new HebrewHoliday(HebrewCalendar.IYAR,   18,      "Lab B'Omer");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday YOM_YERUSHALAYIM = new HebrewHoliday(HebrewCalendar.IYAR,   28,      "Yom Yerushalayim");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday SHAVUOT         = new HebrewHoliday(HebrewCalendar.SIVAN,   6,  2,  "Shavuot");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday TAMMUZ_17       = new HebrewHoliday(HebrewCalendar.TAMUZ,  17,      "Fast of Tammuz 17");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday TISHA_BAV       = new HebrewHoliday(HebrewCalendar.AV,      9,      "Fast of Tisha B'Av");

    /**
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    public static HebrewHoliday SELIHOT         = new HebrewHoliday(HebrewCalendar.ELUL,   21,      "Selihot");
}
