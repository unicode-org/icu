/**
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

class ResultModel extends AbstractTableModel {
    public ResultModel(Logger logger) {
        this.logger = logger;
    }

    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    public String getColumnName(int col) {
        return COLUMN_NAMES[col];
    }

    public int getRowCount() {
        List list = hidden ? permissibleList : completeList;
        return (list == null) ? 0 : list.size();
    }

    public Object getValueAt(int row, int col) {
        List list = hidden ? permissibleList : completeList;
        ICUFile entry = ((ICUFile) list.get(row));
        switch (col) {
        case COLUMN_FILE_NAME:
            return entry.getPath();
        case COLUMN_FILE_PATH:
            return entry.getFilename();
        case COLUMN_ICU_VERSION:
            return entry.getICUVersion();
        case COLUMN_TZ_VERSION:
            return entry.getTZVersion();
        case COLUMN_READABLE:
            return entry.getFile().canRead() ? "Yes" : "No";
        case COLUMN_WRITABLE:
            return entry.getFile().canWrite() ? "Yes" : "No";
        default:
            return null;
        }
    }

    public Iterator iterator() {
        List list = hidden ? permissibleList : completeList;
        return list.iterator();
    }

    public void setHidden(boolean value) {
        hidden = value;
        fireTableDataChanged();
    }

    public boolean add(String filename) {
        return add(new File(filename));
    }

    public boolean add(File file) {
        try {
            ICUFile entry = new ICUFile(file, logger);
            if (file.canRead() && file.canWrite())
                add(permissibleList, hidden, entry);
            add(completeList, !hidden, entry);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public void add(ICUFile entry) {
        File file = entry.getFile();
        if (file.canRead() && file.canWrite())
            add(permissibleList, hidden, entry);

        add(completeList, !hidden, entry);
    }

    public void remove(File file) {
        remove(permissibleList, hidden, file);
        remove(completeList, !hidden, file);
    }

    public void remove(int[] indices) {
        remove(permissibleList, hidden, indices);
        remove(completeList, !hidden, indices);
    }

    public void removeAll() {
        removeAll(permissibleList, hidden);
        removeAll(completeList, !hidden);
    }

    public void update(int[] indices, URL updateURL, File backupDir) throws InterruptedException {
        if (hidden)
            update(permissibleList, indices, updateURL, backupDir);
        else
            update(completeList, indices, updateURL, backupDir);
    }

    public void updateAll(URL updateURL, File backupDir) throws InterruptedException {
        if (hidden)
            updateAll(permissibleList, updateURL, backupDir);
        else
            updateAll(completeList, updateURL, backupDir);
    }

    private void add(List list, boolean fire, ICUFile entry) {
        remove(list, fire, entry.getFile());
        list.add(entry);
        int index = list.size() - 1;
        if (fire)
            fireTableRowsInserted(index, index);
    }

    private void remove(List list, boolean fire, File file) {
        if (list.size() > 0) {
            Iterator iter = list.iterator();
            int i = 0;
            while (iter.hasNext()) {
                ICUFile entry = (ICUFile) iter.next();
                if (entry.getFile().equals(file)) {
                    list.remove(entry);
                    if (fire)
                        fireTableRowsDeleted(i, i);
                    return;
                }
                i++;
            }
        }
    }

    private void remove(List list, boolean fire, int[] indices) {
        if (list.size() > 0 && indices.length > 0) {
            Arrays.sort(indices);
            int max = indices[indices.length - 1];
            int min = indices[0];
            for (int i = indices.length - 1; i >= 0; i--)
                list.remove(indices[i]);
            if (fire)
                fireTableRowsDeleted(min, max);
        }
    }

    private void removeAll(List list, boolean fire) {
        if (list.size() > 0) {
            int index = list.size() - 1;
            list.clear();
            if (fire)
                fireTableRowsDeleted(0, index);
        }
    }

    private void update(List list, int[] indices, URL updateURL, File backupDir) throws InterruptedException {
        if (list.size() > 0 && indices.length > 0) {
            Arrays.sort(indices);
            int n = indices.length;

            int k = 0;
            Iterator iter = list.iterator();
            for (int i = 0; k < n && iter.hasNext(); i++)
                if (i == indices[k])
                    try {
                        k++;
                        ((ICUFile) iter.next()).updateJar(updateURL, backupDir);
                        fireTableRowsUpdated(i, i);
                        Thread.sleep(0);
                    } catch (IOException ex) {
                        // could not update the jar
                        ex.printStackTrace();
                    }
                else
                    iter.next();
        }
    }

    private void updateAll(List list, URL updateURL, File backupDir) throws InterruptedException {
        if (list.size() > 0) {
            int n = list.size();
            Iterator iter = list.iterator();
            for (int i = 0; i < n; i++)
                try {
                    ((ICUFile) iter.next()).updateJar(updateURL, backupDir);
                    fireTableRowsUpdated(i, i);
                    Thread.sleep(0);
                } catch (IOException ex) {
                    // could not update the jar
                    ex.printStackTrace();
                }
        }
    }

    public static final String[] COLUMN_NAMES = new String[] { "Path", "Name", "ICU Version", "TZ Version", "Readable",
            "Writable" };

    public static final int COLUMN_FILE_NAME = 0;

    public static final int COLUMN_FILE_PATH = 1;

    public static final int COLUMN_ICU_VERSION = 2;

    public static final int COLUMN_TZ_VERSION = 3;

    public static final int COLUMN_READABLE = 4;

    public static final int COLUMN_WRITABLE = 5;

    private List completeList = new ArrayList();

    private List permissibleList = new ArrayList();

    private boolean hidden = true;

    public static final long serialVersionUID = 1338;

    private Logger logger;
}
