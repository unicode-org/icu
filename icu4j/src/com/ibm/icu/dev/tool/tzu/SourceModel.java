/**
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.util.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;

class SourceModel extends AbstractListModel implements ComboBoxModel {
    public void findSources() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(TZ_BASE_URL
                    .openStream()));

            HTMLEditorKit.ParserCallback htmlCallback = new HTMLEditorKit.ParserCallback() {
                public void handleStartTag(HTML.Tag tag,
                        MutableAttributeSet attr, int pos) {
                    if (tag == HTML.Tag.LI)
                        listItem = true;
                }

                public void handleEndTag(HTML.Tag tag, int pos) {
                    if (tag == HTML.Tag.LI)
                        listItem = false;
                }

                public void handleText(char[] data, int pos) {
                    if (listItem) {
                        String str = new String(data);
                        if (str.charAt(str.length() - 1) == '/')
                            str = str.substring(0, str.length() - 1);
                        if (!"..".equals(str))
                            try {
                                // add the new item to the map
                                urlMap.put(str, new URL(TZ_BASE_URLSTRING_START
                                        + str + TZ_BASE_URLSTRING_END));

                                // update the selected item and fire off an
                                // event
                                selected = (String) urlMap.lastKey();
                                int index = 0;
                                for (Iterator iter = urlMap.keySet().iterator(); iter
                                        .hasNext();) {
                                    if (iter.next().equals(str))
                                        index++;
                                }
                                fireIntervalAdded(this, index, index);
                            } catch (MalformedURLException ex) {
                                ex.printStackTrace();
                            }
                    }
                }

                private boolean listItem = false;
            };

            new ParserDelegator().parse(reader, htmlCallback, false);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            // close the reader gracefully
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
        }
    }

    public Iterator iterator() {
        return urlMap.entrySet().iterator();
    }

    public int getSize() {
        // the added size (+1) is due to the local copy not being inside the map
        return urlMap.size() + 1;
    }

    public Object getElementAt(int index) {
        if (index == 0)
            return TZ_LOCAL_CHOICE;
        else if (index < 0 || index > urlMap.size())
            return null;
        else {
            Iterator iter = urlMap.keySet().iterator();
            for (int i = 1; i < index; i++)
                iter.next();
            return iter.next();
        }
    }

    public URL getURL(Object choice) {
        if (choice == null || !(choice instanceof String))
            return null;
        else if (TZ_LOCAL_CHOICE.equalsIgnoreCase((String) choice))
            return TZ_LOCAL_URL;
        else
            return (URL) urlMap.get(choice);
    }

    public String getVersion(Object choice) {
        if (choice == null || !(choice instanceof String))
            return null;
        else if (TZ_LOCAL_CHOICE.equalsIgnoreCase((String) choice))
            return TZ_LOCAL_VERSION;
        else
            return (String) choice;
    }

    public Object getSelectedItem() {
        return selected;
    }

    public void setSelectedItem(Object selected) {
        this.selected = selected;
    }

    private Object selected = TZ_LOCAL_CHOICE;

    private TreeMap urlMap = new TreeMap();

    public static String TZ_LOCAL_CHOICE;

    public static final String TZ_BASE_URLSTRING_START = "http://source.icu-project.org/repos/icu/data/trunk/tzdata/icu/";

    public static final String TZ_BASE_URLSTRING_END = "/be/zoneinfo.res";

    public static final File TZ_LOCAL_FILE = new File("zoneinfo.res");

    public static String TZ_LOCAL_VERSION = null;

    public static URL TZ_BASE_URL = null;

    public static URL TZ_LOCAL_URL = null;

    static {
        // cannot make TZ_BASE_URL and TZ_LOCAL_URL final since url creations
        // need to be try-catched
        try {
            TZ_BASE_URL = new URL(TZ_BASE_URLSTRING_START);

            if (!TZ_LOCAL_FILE.exists()) {
                Logger.errorln("Local copy (zoneinfo.res) does not exist.");
            } else {
                TZ_LOCAL_URL = TZ_LOCAL_FILE.toURL();
                TZ_LOCAL_VERSION = ICUFile.findFileTZVersion(TZ_LOCAL_FILE);
                if (TZ_LOCAL_VERSION == null) {
                    Logger.errorln("Failed to determine version of local copy");
                } else {
                    TZ_LOCAL_CHOICE = "Local Copy (" + TZ_LOCAL_VERSION + ")";
                }
            }
        } catch (MalformedURLException ex) {
            // this shouldn't happen
            ex.printStackTrace();
        }
    }

    public static final long serialVersionUID = 1339;
}
