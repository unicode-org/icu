/**
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.util.List;
import java.util.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;

class SourceModel extends AbstractListModel implements ComboBoxModel {
    public SourceModel() {
        // map.put(TZ_LOCAL_CHOICE, TZ_LOCAL_URL);
    }

    public void findSources() {
        BufferedReader reader = null;
        final Thread t = Thread.currentThread();
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
                                map.put(str, new URL(TZ_BASE_URLSTRING_START
                                        + str + TZ_BASE_URLSTRING_END));

                                // update the selected item and fire off an
                                // event
                                selected = (String) map.lastKey();
                                int index = 0;
                                for (Iterator iter = map.keySet().iterator(); iter
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
        return map.entrySet().iterator();
    }

    public int getSize() {
        return map.size() + 1; // the added size is due to the local copy not
                                // being inside the map
    }

    public Object getElementAt(int index) {
        if (index == 0)
            return TZ_LOCAL_CHOICE;
        else if (index < 0 || index > map.size())
            return null;
        else {
            Iterator iter = map.keySet().iterator();
            for (int i = 1; i < index; i++)
                iter.next();
            return iter.next();
        }
    }

    public URL getValue(Object choice) {
        return (choice == null) ? null
                : (((String) choice).toLowerCase() == TZ_LOCAL_CHOICE
                        .toLowerCase()) ? TZ_LOCAL_URL : (URL) map.get(choice);
    }

    public Object getSelectedItem() {
        return selected;
    }

    public void setSelectedItem(Object selected) {
        this.selected = selected;
    }

    private Object selected = TZ_LOCAL_CHOICE;

    private TreeMap map = new TreeMap();

    public static final String TZ_LOCAL_CHOICE = "Local Copy";

    public static final String TZ_BASE_URLSTRING_START = "http://source.icu-project.org/repos/icu/data/trunk/tzdata/icu/";

    public static final String TZ_BASE_URLSTRING_END = "/be/zoneinfo.res";

    public static URL TZ_BASE_URL = null;

    public static URL TZ_LOCAL_URL = null;

    static {
        try {
            TZ_BASE_URL = new URL(TZ_BASE_URLSTRING_START);
            TZ_LOCAL_URL = new File("zoneinfo.res").toURL();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
    }
}
