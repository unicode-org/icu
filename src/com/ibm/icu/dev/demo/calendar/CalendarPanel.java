/*
 *******************************************************************************
 * Copyright (C) 1997-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.demo.calendar;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.util.Date;
import java.util.Locale;

import com.ibm.icu.dev.demo.impl.DemoUtility;
import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.SimpleTimeZone;

class CalendarPanel extends Canvas {

    public CalendarPanel( Locale locale ) {
        setLocale(locale);
    }

    public void setLocale(Locale locale) {
        if (fDisplayLocale == null || !fDisplayLocale.equals(locale)) {
            fDisplayLocale = locale;
            dirty = true;

            for (int i = 0; i < fCalendar.length; i++) {
                if (fCalendar[i] != null) {
                    fSymbols[i] = new DateFormatSymbols(fCalendar[i],
                                                        fDisplayLocale);
                }
            }
            String lang = locale.getLanguage();
            leftToRight = !(lang.equals("iw") || lang.equals("ar"));

            repaint();
        }
    }

    public void setDate(Date date) {
        fStartOfMonth = date;
        dirty = true;
        repaint();
    }

    public void add(int field, int delta)
    {
        synchronized(fCalendar) {
            fCalendar[0].setTime(fStartOfMonth);
            fCalendar[0].add(field, delta);
            fStartOfMonth = fCalendar[0].getTime();
        }
        dirty = true;
        repaint();
    }

    public void setColor(int index, Color c) {
        fColor[index] = c;
        repaint();
    }

    public void setCalendar(int index, Calendar c) {
        Date date = (fCalendar[index] == null) ? new Date()
                                               : fCalendar[index].getTime();

        fCalendar[index] = c;
        fCalendar[index].setTime(date);

        fSymbols[index] = new DateFormatSymbols(c, fDisplayLocale);
        dirty = true;
        repaint();
    }

    public Calendar getCalendar(int index) {
        return fCalendar[index];
    }

    public Locale getDisplayLocale() {
        return fDisplayLocale;
    }

    public Date firstOfMonth() {
        return fStartOfMonth;
    }

    private Date startOfMonth(Date dateInMonth)
    {
        synchronized(fCalendar) {
            fCalendar[0].setTime(dateInMonth);

            int era = fCalendar[0].get(Calendar.ERA);
            int year = fCalendar[0].get(Calendar.YEAR);
            int month = fCalendar[0].get(Calendar.MONTH);

            fCalendar[0].clear();
            fCalendar[0].set(Calendar.ERA, era);
            fCalendar[0].set(Calendar.YEAR, year);
            fCalendar[0].set(Calendar.MONTH, month);
            fCalendar[0].set(Calendar.DATE, 1);

            return fCalendar[0].getTime();
        }
    }

    private void calculate()
    {
        //
        // As a workaround for JDK 1.1.3 and below, where Calendars and time
        // zones are a bit goofy, always set my calendar's time zone to UTC.
        // You would think I would want to do this in the "set" function above,
        // but if I do that, the program hangs when this class is loaded,
        // perhaps due to some sort of static initialization ordering problem.
        // So I do it here instead.
        //
        fCalendar[0].setTimeZone(new SimpleTimeZone(0, "UTC"));

        Calendar c = (Calendar)fCalendar[0].clone(); // Temporary copy

        fStartOfMonth = startOfMonth(fStartOfMonth);

        // Stash away a few useful constants for this calendar and display
        minDay = c.getMinimum(Calendar.DAY_OF_WEEK);
        daysInWeek = c.getMaximum(Calendar.DAY_OF_WEEK) - minDay + 1;

        firstDayOfWeek = Calendar.getInstance(fDisplayLocale).getFirstDayOfWeek();

        // Stash away a Date for the start of this month

        // Find the day of week of the first day in this month
        c.setTime(fStartOfMonth);
        firstDayInMonth = c.get(Calendar.DAY_OF_WEEK);
        int firstWeek = c.get(Calendar.WEEK_OF_MONTH);

        // Now find the # of days in the month
        c.roll(Calendar.DATE, false);
        daysInMonth = c.get(Calendar.DATE);

        // Finally, find the end of the month, i.e. the start of the next one
        c.roll(Calendar.DATE, true);
        c.add(Calendar.MONTH, 1);
        c.getTime();        // JDK 1.1.2 bug workaround
        c.add(Calendar.SECOND, -1);
        Date endOfMonth = c.getTime();
        if(endOfMonth==null){
         //do nothing
        }
        endOfMonth = null;
        int lastWeek = c.get(Calendar.WEEK_OF_MONTH);
        
        // Calculate the number of full or partial weeks in this month.
        numWeeks = lastWeek - firstWeek + 1;

        dirty = false;
    }

    static final int XINSET = 4;
    static final int YINSET = 2;

    /*
     * Convert from the day number within a month (1-based)
     * to the cell coordinates on the calendar (0-based)
     */
    private void dateToCell(int date, Point pos)
    {
        int cell = (date + firstDayInMonth - firstDayOfWeek - minDay);
        if (firstDayInMonth < firstDayOfWeek) {
            cell += daysInWeek;
        }

        pos.x = cell % daysInWeek;
        pos.y = cell / daysInWeek;
    }
    //private Point dateToCell(int date) {
    //    Point p = new Point(0,0);
    //    dateToCell(date, p);
    //    return p;
    //}

    public void paint(Graphics g) {

        if (dirty) {
            calculate();
        }

        Point cellPos = new Point(0,0);     // Temporary variable
        Dimension d = this.getSize();

        g.setColor(Color.lightGray);
        g.fillRect(0,0,d.width,d.height);

        // Draw the day names at the top
        g.setColor(Color.black);
        g.setFont(DemoUtility.labelFont);
        FontMetrics fm = g.getFontMetrics();
        int labelHeight = fm.getHeight() + YINSET * 2;

        int v = fm.getAscent() + YINSET;
        for (int i = 0; i < daysInWeek; i++) {
            int dayNum = (i + minDay + firstDayOfWeek - 2) % daysInWeek + 1;
            String dayName = fSymbols[0].getWeekdays()[dayNum];


            double h;
            if (leftToRight) {
                h = d.width*(i + 0.5) / daysInWeek;
            } else {
                h = d.width*(daysInWeek - i - 0.5) / daysInWeek;
            }
            h -= fm.stringWidth(dayName) / 2;

            g.drawString(dayName, (int)h, v);
        }

        double cellHeight = (d.height - labelHeight - 1) / numWeeks;
        double cellWidth = (double)(d.width - 1) / daysInWeek;

        // Draw a white background in the part of the calendar
        // that displays this month.
        // First figure out how much of the first week should be shaded.
        {
            g.setColor(Color.white);
            dateToCell(1, cellPos);
            int width = (int)(cellPos.x*cellWidth);  // Width of unshaded area

            if (leftToRight) {
                g.fillRect((int)(width), labelHeight ,
                           d.width - width, (int)cellHeight);
            } else {
                g.fillRect(0, labelHeight ,
                           d.width - width, (int)cellHeight);
            }

            // All of the intermediate weeks get shaded completely
            g.fillRect(0, (int)(labelHeight + cellHeight),
                        d.width, (int)(cellHeight * (numWeeks - 2)));

            // Now figure out the last week.
            dateToCell(daysInMonth, cellPos);
            width = (int)((cellPos.x+1)*cellWidth);  // Width of shaded area

            if (leftToRight) {
                g.fillRect(0, (int)(labelHeight + (numWeeks-1) * cellHeight),
                           width, (int)cellHeight);
            } else {
                g.fillRect(d.width - width, (int)(labelHeight + (numWeeks-1) * cellHeight),
                           width, (int)cellHeight);
            }

        }
        // Draw the X/Y grid lines
        g.setColor(Color.black);
        for (int i = 0; i <= numWeeks; i++) {
            int y = (int)(labelHeight + i * cellHeight);
            g.drawLine(0, y, d.width - 1, y);
        }
        for (int i = 0; i <= daysInWeek; i++) {
            int x = (int)(i * cellWidth);
            g.drawLine(x, labelHeight, x, d.height - 1);
        }

        // Now loop through all of the days in the month, figure out where
        // they go in the grid, and draw the day # for each one

        // Figure out the date of the first cell in the calendar display
        int cell = (1 + firstDayInMonth - firstDayOfWeek - minDay);
        if (firstDayInMonth < firstDayOfWeek) {
            cell += daysInWeek;
        }

        Calendar c = (Calendar)fCalendar[0].clone();
        c.setTime(fStartOfMonth);
        c.add(Calendar.DATE, -cell);

        StringBuffer buffer = new StringBuffer();

        for (int row = 0; row < numWeeks; row++) {
            for (int col = 0; col < daysInWeek; col++) {

                g.setFont(DemoUtility.numberFont);
                g.setColor(Color.black);
                fm = g.getFontMetrics();

                int cellx;
                if (leftToRight) {
                    cellx = (int)((col) * cellWidth);
                } else {
                    cellx = (int)((daysInWeek - col - 1) * cellWidth);
                }

                int celly = (int)(row * cellHeight + labelHeight);

                for (int i = 0; i < 2; i++) {
                    fCalendar[i].setTime(c.getTime());

                    int date = fCalendar[i].get(Calendar.DATE);
                    buffer.setLength(0);
                    buffer.append(date);
                    String dayNum = buffer.toString();

                    int x;

                    if (leftToRight) {
                        x = cellx + (int)cellWidth - XINSET - fm.stringWidth(dayNum);
                    } else {
                        x = cellx + XINSET;
                    }
                    int y = celly + + fm.getAscent() + YINSET + i * fm.getHeight();

                    if (fColor[i] != null) {
                        g.setColor(fColor[i]);
                    }
                    g.drawString(dayNum, x, y);

                    if (date == 1 || row == 0 && col == 0) {
                        g.setFont(DemoUtility.numberFont);
                        String month = fSymbols[i].getMonths()[
                                            fCalendar[i].get(Calendar.MONTH)];

                        if (leftToRight) {
                            x = cellx + XINSET;
                        } else {
                            x = cellx + (int)cellWidth - XINSET - fm.stringWidth(month);
                        }
                        g.drawString(month, x, y);
                    }
                }

                c.add(Calendar.DATE, 1);
            }
        }
    }

    // Important state variables
    private Calendar[]          fCalendar = new Calendar[4];
    private Color[]             fColor = new Color[4];

    private Locale              fDisplayLocale;
    private DateFormatSymbols[] fSymbols = new DateFormatSymbols[4];

    private Date                fStartOfMonth = new Date();     // 00:00:00 on first day of month

    // Cached calculations to make drawing faster.
    private transient int       minDay;           // Minimum legal day #
    private transient int       daysInWeek;       // # of days in a week
    private transient int       firstDayOfWeek;   // First day to display in week
    private transient int       numWeeks;         // # full or partial weeks in month
    private transient int       daysInMonth;      // # days in this month
    private transient int       firstDayInMonth;  // Day of week of first day in month
    private transient boolean   leftToRight;

    private transient boolean dirty = true;
}
