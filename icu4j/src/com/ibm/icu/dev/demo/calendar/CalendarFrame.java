/**
 * $RCSfile: CalendarFrame.java,v $ $Revision: 1.2 $ $Date: 2000/02/28 04:09:23 $
 *
 * (C) Copyright IBM Corp. 1998.  All Rights Reserved.
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */

package com.ibm.demo.calendar;

import com.ibm.demo.*;
import com.ibm.text.*;
import com.ibm.util.*;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
//import java.util.*;
import java.net.*;
import java.io.*;

import java.text.DateFormat;
import java.util.SimpleTimeZone;
import java.util.Locale;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A Frame is a top-level window with a title. The default layout for a frame
 * is BorderLayout.  The CalendarFrame class defines the window layout of
 * CalendarDemo.
 */
class CalendarFrame extends Frame
{
    private static final boolean DEBUG = false;

    private DemoApplet applet;

    /**
     * Constructs a new CalendarFrame that is initially invisible.
     */
    public CalendarFrame(DemoApplet myApplet)
    {
        super("Calendar Demo");
        this.applet = myApplet;
        init();

        // When the window is closed, we want to shut down the applet or application
        addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    setVisible(false);
                    dispose();

                    if (applet != null) {
                        applet.demoClosed();
                    } else System.exit(0);
                }
            } );
    }

    private Choice          displayMenu;
    private Locale[]        locales = DemoUtility.getG7Locales();

    private Calendar        calendars[]   = new Calendar[2];
    private Choice          calMenu[]     = new Choice[2];
    private ColoredLabel    monthLabel[]  = new ColoredLabel[2];
    private DateFormat      monthFormat[] = new DateFormat[2];

    private Button          prevYear;
    private Button          prevMonth;
    private Button          gotoToday;
    private Button          nextMonth;
    private Button          nextYear;
    private CalendarPanel   calendarPanel;

    private static void add(Container container, Component component,
                            GridBagLayout g, GridBagConstraints c,
                            int gridwidth, int weightx)
    {
        c.gridwidth = gridwidth;
        c.weightx = weightx;
        g.setConstraints(component, c);
        container.add(component);
    }

    /**
     * Initializes the applet. You never need to call this directly, it
     * is called automatically by the system once the applet is created.
     */
    public void init() {
        setBackground(DemoUtility.bgColor);
        setLayout(new BorderLayout(10,10));

        Panel topPanel = new Panel();
        GridBagLayout g = new GridBagLayout();
        topPanel.setLayout(g);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        // Build the two menus for selecting which calendar is displayed,
        // plus the month/year label for each calendar
        for (int i = 0; i < 2; i++) {
            calMenu[i] = new Choice();
            for (int j = 0; j < CALENDARS.length; j++) {
                calMenu[i].addItem(CALENDARS[j].name);
            }
            calMenu[i].setBackground(DemoUtility.choiceColor);
            calMenu[i].select(i);
            calMenu[i].addItemListener(new CalMenuListener());

            // Label for the current month name
            monthLabel[i] = new ColoredLabel("", COLORS[i]);
            monthLabel[i].setFont(DemoUtility.titleFont);

            // And the default calendar to use for this slot
            calendars[i] = CALENDARS[i].calendar;

            add(topPanel, calMenu[i], g, c, 5, 0);
            add(topPanel, monthLabel[i], g, c, GridBagConstraints.REMAINDER, 1);
        }

        // Now add the next/previous year/month buttons:
        prevYear = new Button("<<");
        prevYear.addActionListener(new AddAction(Calendar.YEAR, -1));

        prevMonth = new Button("<");
        prevMonth.addActionListener(new AddAction(Calendar.MONTH, -1));

        gotoToday = new Button("Today");
        gotoToday.addActionListener( new ActionListener()
        {
            public void actionPerformed(ActionEvent e) {
                calendarPanel.setDate( new Date() );
                updateMonthName();
            }
        } );

        nextMonth = new Button(">");
        nextMonth.addActionListener(new AddAction(Calendar.MONTH, 1));

        nextYear = new Button(">>");
        nextYear.addActionListener(new AddAction(Calendar.YEAR, 1));

        c.fill = GridBagConstraints.NONE;
        add(topPanel, prevYear,  g, c, 1, 0);
        add(topPanel, prevMonth, g, c, 1, 0);
        add(topPanel, gotoToday, g, c, 1, 0);
        add(topPanel, nextMonth, g, c, 1, 0);
        add(topPanel, nextYear,  g, c, 1, 0);

        // Now add the menu for selecting the display language
        Panel displayPanel = new Panel();
        {
            displayMenu = new Choice();
            int selectMe = 1;
            for (int i = 0; i < locales.length; i++) {
                if (i > 0 &&
                        locales[i].getLanguage().equals(locales[i-1].getLanguage()) ||
                    i < locales.length - 1 &&
                        locales[i].getLanguage().equals(locales[i+1].getLanguage()))
                {
                    displayMenu.addItem( locales[i].getDisplayName() );
                } else {
                    displayMenu.addItem( locales[i].getDisplayLanguage());
                }

                if (locales[i].getLanguage().equals(Locale.getDefault().getLanguage())) {
                    selectMe = i;
                }
            }
            displayMenu.setBackground(DemoUtility.choiceColor);
            displayMenu.select(selectMe);

            displayMenu.addItemListener( new ItemListener()
            {
                 public void itemStateChanged(ItemEvent e) {
                    Locale loc = locales[displayMenu.getSelectedIndex()];
                    calendarPanel.setLocale( loc );
                    monthFormat[0] = monthFormat[1] = null;
                    updateMonthName();
                    repaint();
                }
            } );

            Label l1 = new Label("Display Language:", Label.RIGHT);
            l1.setFont(DemoUtility.labelFont);

            displayPanel.setLayout(new FlowLayout());
            displayPanel.add(l1);
            displayPanel.add(displayMenu);

        }
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;

        add(topPanel, displayPanel, g, c, GridBagConstraints.REMAINDER, 0);

        // The title, buttons, etc. go in a panel at the top of the window
        add("North", topPanel);

        // The copyright notice goes at the bottom of the window
        Label copyright = new Label(DemoUtility.copyright1, Label.LEFT);
        copyright.setFont(DemoUtility.creditFont);
        add("South", copyright);

        // Now create the big calendar panel and stick it in the middle
        calendarPanel = new CalendarPanel( locales[displayMenu.getSelectedIndex()] );
        add("Center", calendarPanel);

        for (int i = 0; i < 2; i++) {
            calendarPanel.setCalendar(i, calendars[i]);
            calendarPanel.setColor(i, COLORS[i]);
        }

        updateMonthName();
    };


    private void updateMonthName()
    {
        for (int i = 0; i < 2; i++) {
            if (monthFormat[i] == null) {     // TODO: optimize
                SimpleDateFormat f = (SimpleDateFormat) IBMCalendar.getDateTimeFormat(
                                        calendars[i], DateFormat.MEDIUM, -1,
                                        locales[displayMenu.getSelectedIndex()]);
                f.applyPattern("MMMM, yyyy G");
                f.setTimeZone(new SimpleTimeZone(0, "UTC"));
                monthFormat[i] = f;
            }

            monthLabel[i].setText( monthFormat[i].format( calendarPanel.firstOfMonth() ));
        }
    }

    /**
     * CalMenuListener responds to events in the two popup menus that select
     * the calendar systems to be used in the display.  It figures out which
     * of the two menus the event occurred in and updates the corresponding
     * element of the calendars[] array to match the new selection.
     */
    private class CalMenuListener implements ItemListener
    {
         public void itemStateChanged(ItemEvent e)
         {
            for (int i = 0; i < calMenu.length; i++)
            {
                if (e.getItemSelectable() == calMenu[i])
                {
                    // We found the menu that the event happened in.
                    // Figure out which new calendar they selected.
                    Calendar newCal = CALENDARS[ calMenu[i].getSelectedIndex() ].calendar;

                    if (newCal != calendars[i])
                    {
                        // If any of the other menus are set to the same new calendar
                        // we're about to use for this menu, set them to the current
                        // calendar from *this* menu so we won't have two the same
                        for (int j = 0; j < calendars.length; j++) {
                            if (j != i && calendars[j] == newCal) {
                                calendars[j] = calendars[i];
                                calendarPanel.setCalendar(j, calendars[j]);
                                monthFormat[j] = null;

                                for (int k = 0; k < CALENDARS.length; k++) {
                                    if (calendars[j] == CALENDARS[k].calendar) {
                                        calMenu[j].select(k);
                                        break;
                                    }
                                }
                            }
                        }
                        // Now update this menu to use the new calendar the user selected
                        calendars[i] = newCal;
                        calendarPanel.setCalendar(i, newCal);
                        monthFormat[i] = null;

                        updateMonthName();
                    }
                    break;
                }
            }
         }
    };

    /**
     * AddAction handles the next/previous year/month buttons...
     */
    private class AddAction implements ActionListener {
        AddAction(int field, int amount) {
            this.field = field;
            this.amount = amount;
        }

        public void actionPerformed(ActionEvent e) {
            calendarPanel.add(field, amount);
            updateMonthName();
        }

        private int field, amount;
    }

    /**
     * ColoredLabel is similar to java.awt.Label, with two differences:
     *
     *  - You can set its text color
     *
     *  - It draws text using drawString rather than using a host-specific
     *    "Peer" object like AWT does.  On 1.2, using drawString gives
     *    us Bidi reordering for free.
     */
    static private class ColoredLabel extends Component {
        public ColoredLabel(String label) {
            text = label;
        }

        public ColoredLabel(String label, Color c) {
            text = label;
            color = c;
        }

        public void setText(String label) {
            text = label;
            repaint();
        }

        public void setFont(Font f) {
            font = f;
            repaint();
        }

        public void paint(Graphics g) {
            FontMetrics fm = g.getFontMetrics(font);

            Rectangle bounds = getBounds();

            g.setColor(color);
            g.setFont(font);
            g.drawString(text, fm.stringWidth("\u00a0"),
                         bounds.height/2 + fm.getHeight()
                         - fm.getAscent() + fm.getLeading()/2);
        }

        public Dimension getPreferredSize() {
            return getMinimumSize();
        }

        public Dimension getMinimumSize() {
            FontMetrics fm = getFontMetrics(font);

            return new Dimension( fm.stringWidth(text) + 2*fm.stringWidth("\u00a0"),
                                  fm.getHeight() + fm.getLeading()*2);
        }

        String text;
        Color color = Color.black;
        Font font = DemoUtility.labelFont;
    }

    /**
     * Print out the error message while debugging this program.
     */
    public void errorText(String s)
    {
        if (DEBUG)
        {
            System.out.println(s);
        }
    }

    class CalendarRec {
        public CalendarRec(String nameStr, java.util.Calendar cal)
        {
            name = nameStr;
            calendar = cal;
        }

        java.util.Calendar  calendar;
        String              name;
    };

    private final CalendarRec[] CALENDARS = {
        new CalendarRec("Gregorian Calendar",       new GregorianCalendar()),
        new CalendarRec("Hebrew Calendar",          new HebrewCalendar()),
        new CalendarRec("Islamic Calendar",         makeIslamic(false)),
        new CalendarRec("Islamic Civil Calendar ",  makeIslamic(true)),
        new CalendarRec("Buddhist Calendar",        new BuddhistCalendar()),
        new CalendarRec("Japanese Calendar",        new JapaneseCalendar()),
    };

    static private final Calendar makeIslamic(boolean civil) {
        IslamicCalendar cal = new IslamicCalendar();
        cal.setCivil(civil);
        return cal;
    };

    static final Color[] COLORS = { Color.blue, Color.black };
}

