// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1997-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.demo.calendar;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;

import javax.swing.JTextField;

import com.ibm.icu.dev.demo.impl.DemoApplet;
import com.ibm.icu.dev.demo.impl.DemoUtility;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.BuddhistCalendar;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.HebrewCalendar;
import com.ibm.icu.util.IslamicCalendar;
import com.ibm.icu.util.JapaneseCalendar;
import com.ibm.icu.util.TimeZone;

/**
 * CalendarCalc demonstrates how Date/Time formatter works.
 */
public class CalendarCalc extends DemoApplet
{
    /**
     * For serialization
     */
    private static final long serialVersionUID = 4540103433916539296L;

    /**
     * The main function which defines the behavior of the MultiCalendarDemo
     * applet when an applet is started.
     */
    public static void main(String argv[]) {
        new CalendarCalc().showDemo();
    }

    /**
     * This creates a CalendarCalcFrame for the demo applet.
     */
    public Frame createDemoFrame(DemoApplet applet) {
        return new CalendarCalcFrame(applet);
    }
}

/**
 * A Frame is a top-level window with a title. The default layout for a frame
 * is BorderLayout.  The CalendarCalcFrame class defines the window layout of
 * MultiCalendarDemo.
 */
class CalendarCalcFrame extends Frame implements ActionListener
{
    /**
     * For serialization
     */
    private static final long serialVersionUID = 8901485296258761846L;

    static final Locale[] locales = DemoUtility.getG7Locales();

    private DemoApplet              applet;
    private long                    time = System.currentTimeMillis();

    private static final RollAddField kRollAddFields[] = {
        new RollAddField(Calendar.YEAR,                 "Year" ),
        new RollAddField(Calendar.MONTH,                "Month" ),
        new RollAddField(Calendar.WEEK_OF_MONTH,        "Week of Month" ),
        new RollAddField(Calendar.WEEK_OF_YEAR,         "Week of Year" ),
        new RollAddField(Calendar.DAY_OF_MONTH,         "Day of Month" ),
        new RollAddField(Calendar.DAY_OF_WEEK,          "Day of Week" ),
        new RollAddField(Calendar.DAY_OF_WEEK_IN_MONTH, "Day of Week in Month" ),
        new RollAddField(Calendar.DAY_OF_YEAR,          "Day of Year" ),
        new RollAddField(Calendar.AM_PM,                "AM/PM" ),
        new RollAddField(Calendar.HOUR_OF_DAY,          "Hour of day" ),
        new RollAddField(Calendar.HOUR,                 "Hour" ),
        new RollAddField(Calendar.MINUTE,               "Minute" ),
        new RollAddField(Calendar.SECOND,               "Second" ),
    };

    /**
     * Constructs a new CalendarCalcFrame that is initially invisible.
     */
    public CalendarCalcFrame(DemoApplet applet)
    {
        super("Multiple Calendar Demo");
        this.applet = applet;
        init();
        start();
    }

    /**
     * Initializes the applet. You never need to call this directly, it
     * is called automatically by the system once the applet is created.
     */
    public void init()
    {
        buildGUI();

        patternText.setText( calendars[0].toPattern() );

        // Force an update of the display
        cityChanged();
        millisFormat();
        enableEvents(KeyEvent.KEY_RELEASED);
        enableEvents(WindowEvent.WINDOW_CLOSING);
    }

    //------------------------------------------------------------
    // package private
    //------------------------------------------------------------
    void addWithFont(Container container, Component foo, Font font) {
        if (font != null)
            foo.setFont(font);
        container.add(foo);
    }

    /**
     * Called to start the applet. You never need to call this method
     * directly, it is called when the applet's document is visited.
     */
    public void start()
    {
        // do nothing
    }

    TextField patternText;

    Choice dateMenu;
    Choice localeMenu;

    Button up;
    Button down;

    Checkbox getRoll;
    Checkbox getAdd;

    public void buildGUI()
    {
        setBackground(DemoUtility.bgColor);
        setLayout(new FlowLayout()); // shouldn't be necessary, but it is.

// TITLE
        Label label1=new Label("Calendar Converter", Label.CENTER);
        label1.setFont(DemoUtility.titleFont);
        add(label1);
        add(DemoUtility.createSpacer());

// IO Panel
        Panel topPanel = new Panel();
        topPanel.setLayout(new FlowLayout());

        CheckboxGroup group1= new CheckboxGroup();

        // Set up the controls for each calendar we're demonstrating
        for (int i = 0; i < calendars.length; i++)
        {
            Label label = new Label(calendars[i].name, Label.RIGHT);
            label.setFont(DemoUtility.labelFont);
            topPanel.add(label);

            topPanel.add(calendars[i].text);

            final int j = i;
            calendars[i].text.addActionListener( new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    textChanged(j);
                }
            } );

            calendars[i].rollAdd.setCheckboxGroup(group1);
            topPanel.add(calendars[i].rollAdd);
        }
        calendars[0].rollAdd.setState(true);    // Make the first one selected

        Label label4=new Label("Pattern", Label.RIGHT);
        label4.setFont(DemoUtility.labelFont);
        topPanel.add(label4);

        patternText=new TextField(FIELD_COLUMNS);
        patternText.setFont(DemoUtility.editFont);
        topPanel.add(patternText);
        topPanel.add(new Label(""));

        DemoUtility.fixGrid(topPanel,3);
        add(topPanel);
        add(DemoUtility.createSpacer());

// ROLL / ADD
        Panel rollAddPanel=new Panel();
        {
            rollAddPanel.setLayout(new FlowLayout());

            Panel rollAddBoxes = new Panel();
            {
                rollAddBoxes.setLayout(new GridLayout(2,1));
                CheckboxGroup group2= new CheckboxGroup();
                getRoll = new Checkbox("Roll",group2, false);
                getAdd = new Checkbox("Add",group2, true);

                rollAddBoxes.add(getRoll);
                rollAddBoxes.add(getAdd);
            }

            Label dateLabel=new Label("Date Fields");
            dateLabel.setFont(DemoUtility.labelFont);

            dateMenu= new Choice();
            dateMenu.setBackground(DemoUtility.choiceColor);
            for (int i = 0; i < kRollAddFields.length; i++) {
                dateMenu.addItem(kRollAddFields[i].name);
                if (kRollAddFields[i].field == Calendar.MONTH) {
                    dateMenu.select(i);
                }
            }

            Panel upDown = new Panel();
            {
                upDown.setLayout(new GridLayout(2,1));

                // *** If the images are not found, we use the label.
                up = new Button("^");
                down = new Button("v");
                up.setBackground(DemoUtility.bgColor);
                down.setBackground(DemoUtility.bgColor);
                upDown.add(up);
                upDown.add(down);
                up.addActionListener(this);
                down.addActionListener(this);
            }

            rollAddPanel.add(dateLabel);
            rollAddPanel.add(dateMenu);
            rollAddPanel.add(rollAddBoxes);
            rollAddPanel.add(upDown);

        }
        Panel localePanel = new Panel();
        {
            // Make the locale popup menus
            localeMenu= new Choice();
            Locale defaultLocale = Locale.getDefault();
            int bestMatch = -1, thisMatch = -1;
            int selectMe = 0;
            
            for (int i = 0; i < locales.length; i++) {
                if (i > 0 && locales[i].getLanguage().equals(locales[i-1].getLanguage()) ||
                    i < locales.length - 1 &&
                        locales[i].getLanguage().equals(locales[i+1].getLanguage()))
                {
                    localeMenu.addItem( locales[i].getDisplayName() );
                } else {
                    localeMenu.addItem( locales[i].getDisplayLanguage());
                }
                
                thisMatch = DemoUtility.compareLocales(locales[i], defaultLocale);
                
                if (thisMatch >= bestMatch) {
                    bestMatch = thisMatch;
                    selectMe = i;
                }
            }
            
            localeMenu.setBackground(DemoUtility.choiceColor);
            localeMenu.select(selectMe);

            Label localeLabel =new Label("Display Locale");
            localeLabel.setFont(DemoUtility.labelFont);

            localePanel.add(localeLabel);
            localePanel.add(localeMenu);
            DemoUtility.fixGrid(localePanel,2);

            localeMenu.addItemListener( new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    Locale loc = locales[localeMenu.getSelectedIndex()];
                    System.out.println("Change locale to " + loc.getDisplayName());

                    for (int i = 0; i < calendars.length; i++) {
                        calendars[i].setLocale(loc);
                    }
                    millisFormat();
                }
            } );
        }
        add(rollAddPanel);
        add(DemoUtility.createSpacer());
        add(localePanel);
        add(DemoUtility.createSpacer());

// COPYRIGHT
        Panel copyrightPanel = new Panel();
        addWithFont (copyrightPanel,new Label(DemoUtility.copyright1, Label.LEFT),
            DemoUtility.creditFont);
        DemoUtility.fixGrid(copyrightPanel,1);
        add(copyrightPanel);
    }

    /**
     * This function is called when users change the pattern text.
     */
    public void setFormatFromPattern() {
        String timePattern = patternText.getText();

        for (int i = 0; i < calendars.length; i++) {
            calendars[i].applyPattern(timePattern);
        }

        millisFormat();
    }

    /**
     * This function is called when it is necessary to parse the time
     * string in one of the formatted date fields
     */
    public void textChanged(int index) {
        String rightString = calendars[index].text.getText();

        ParsePosition status = new ParsePosition(0);

        if (rightString.length() == 0)
        {
            errorText("Error: no input to parse!");
            return;
        }

        try {
            Date date = calendars[index].format.parse(rightString, status);
            time = date.getTime();
        }
        catch (Exception e) {
            for (int i = 0; i < calendars.length; i++) {
                if (i != index) {
                    calendars[i].text.setText("ERROR");
                }
            }
            errorText("Exception: " + e.getClass().toString() + " parsing: "+rightString);
            return;
        }

        int start = calendars[index].text.getSelectionStart();
        int end = calendars[index].text.getSelectionEnd();

        millisFormat();

        calendars[index].text.select(start,end);
    }

    /**
     * This function is called when it is necessary to format the time
     * in the "Millis" text field.
     */
    public void millisFormat() {
        String out = "";

        for (int i = 0; i < calendars.length; i++) {
            try {
                out = calendars[i].format.format(new Date(time));
                calendars[i].text.setText(out);
            }
            catch (Exception e) {
                calendars[i].text.setText("ERROR");
                errorText("Exception: " + e.getClass().toString() + " formatting "
                            + calendars[i].name + " " + time);
            }
        }
    }


    /**
     * This function is called when users change the pattern text.
     */
    public void patternTextChanged() {
        setFormatFromPattern();
    }

    /**
     * This function is called when users select a new representative city.
     */
    public void cityChanged() {
        TimeZone timeZone = TimeZone.getDefault();

        for (int i = 0; i < calendars.length; i++) {
            calendars[i].format.setTimeZone(timeZone);
        }
        millisFormat();
    }

    /**
     * This function is called when users select a new time field
     * to add or roll its value.
     */
    public void dateFieldChanged(boolean isUp) {
        int field = kRollAddFields[dateMenu.getSelectedIndex()].field;

        for (int i = 0; i < calendars.length; i++)
        {
            if (calendars[i].rollAdd.getState())
            {
                Calendar c = calendars[i].calendar;
                c.setTime(new Date(time));

                if (getAdd.getState()) {
                    c.add(field, isUp ? 1 : -1);
                } else {
                    c.roll(field, isUp);
                }

                time = c.getTime().getTime();
                millisFormat();
                break;
            }
        }
    }

    /**
     * Print out the error message while debugging this program.
     */
    public void errorText(String s)
    {
        if (true) {
            System.out.println(s);
        }
    }
    
    /**
     * Called if an action occurs in the CalendarCalcFrame object.
     */
    public void actionPerformed(ActionEvent evt)
    {
        // *** Button events are handled here.
        Object obj = evt.getSource();
        System.out.println("action " + obj);
        if (obj instanceof Button) {
            if (evt.getSource() == up) {
                dateFieldChanged(false);
            } else
                if (evt.getSource() == down) {
                    dateFieldChanged(true);
            }
        }
    }
    
    /**
     * Handles the event. Returns true if the event is handled and should not
     * be passed to the parent of this component. The default event handler
     * calls some helper methods to make life easier on the programmer.
     */
    protected void processKeyEvent(KeyEvent evt)
    {
        System.out.println("key " + evt);
        if (evt.getID() == KeyEvent.KEY_RELEASED) { 
            if (evt.getSource() == patternText) {
                patternTextChanged();
            }
            else {
                for (int i = 0; i < calendars.length; i++) {
                    if (evt.getSource() == calendars[i].text) {
                        textChanged(i);
                    }
                }
            }
        }
    }
    
    protected void processWindowEvent(WindowEvent evt) 
    {
        System.out.println("window " + evt);
        if (evt.getID() == WindowEvent.WINDOW_CLOSING && 
            evt.getSource() == this) {
            this.hide();
            this.dispose();

            if (applet != null) {
               applet.demoClosed();
            } else System.exit(0);
        }
    }
    
    /*
    protected void processEvent(AWTEvent evt)
    {
        if (evt.getID() == AWTEvent. Event.ACTION_EVENT && evt.target == up) {
            dateFieldChanged(true);
            return true;
        }
        else if (evt.id == Event.ACTION_EVENT && evt.target == down) {
            dateFieldChanged(false);
            return true;
        }
    }
    */

    private static final int        FIELD_COLUMNS = 35;


    class CalendarRec {
        public CalendarRec(String nameStr, Calendar cal)
        {
            name = nameStr;
            calendar = cal;
            rollAdd = new Checkbox();

            text = new JTextField("",FIELD_COLUMNS);
            text.setFont(DemoUtility.editFont);

            format = DateFormat.getDateInstance(cal, DateFormat.FULL,
                                                Locale.getDefault());
            //format.applyPattern(DEFAULT_FORMAT);
        }

        public void setLocale(Locale loc) {
            String pattern = toPattern();

            format = DateFormat.getDateInstance(calendar, DateFormat.FULL,
                                                loc);
            applyPattern(pattern);
        }

        public void applyPattern(String pattern) {
            if (format instanceof SimpleDateFormat) {
                ((SimpleDateFormat)format).applyPattern(pattern);
//hey {al} - 
//            } else if (format instanceof java.text.SimpleDateFormat) {
//                ((java.text.SimpleDateFormat)format).applyPattern(pattern);
            }
        }
        
        private String toPattern() {
            if (format instanceof SimpleDateFormat) {
                return ((SimpleDateFormat)format).toPattern();
//hey {al} - 
//            } else if (format instanceof java.text.SimpleDateFormat) {
//                return ((java.text.SimpleDateFormat)format).toPattern();
            }
            return "";
        }

        Calendar  calendar;
        DateFormat          format;
        String              name;
        JTextField           text;
        Checkbox            rollAdd;
    }

    private final CalendarRec[] calendars = {
        new CalendarRec("Gregorian",        new GregorianCalendar()),
        new CalendarRec("Hebrew",           new HebrewCalendar()),
        new CalendarRec("Islamic (civil)",  makeIslamic(true)),
        new CalendarRec("Islamic (true)",   makeIslamic(false)),
        new CalendarRec("Buddhist",         new BuddhistCalendar()),
        new CalendarRec("Japanese",         new JapaneseCalendar()),
//        new CalendarRec("Chinese",          new ChineseCalendar()),
    };

    static private final Calendar makeIslamic(boolean civil) {
        IslamicCalendar cal = new IslamicCalendar();
        cal.setCivil(civil);
        return cal;
    }
}

class RollAddField {
    RollAddField(int field, String name) {
        this.field = field;
        this.name = name;
    }
    int field;
    String name;
}
