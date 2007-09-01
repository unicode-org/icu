/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and others.
 * All Rights Reserved.
 * ******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.awt.Component;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.net.URL;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;

/**
 * The path list GUI component.
 */
public class ResultComponent extends JComponent {
    /**
     * The serializable UID.
     */
    public static final long serialVersionUID = 1341;

    /**
     * A cancel search button to cancel a search if one is currently occuring.
     */
    private JButton resultCancelSearchButton = new JButton("Stop Search");

    /**
     * A cancel update button to cancel an update if one is currently occuring.
     */
    private JButton resultCancelUpdateButton = new JButton("Stop Update");

    /**
     * The dialog that comes up when the browse button is clicked.
     */
    private JFileChooser resultChooser = new JFileChooser();

    /**
     * A menu item for copying a filename to the clipboard.
     */
    private JMenuItem resultCopyItem = new JMenuItem("Copy Selected");

    /**
     * The model for all the results from a search.
     */
    private ResultModel resultModel;

    /**
     * The panel where option components are shown.
     */
    private JPanel resultOptionPanel = new JPanel();

    /**
     * The context menu that pops up with more options.
     */
    private JPopupMenu resultPopup = new JPopupMenu();

    /**
     * A menu item for <code>pathPopup</code> to remove all files from the
     * result model.
     */
    private JMenuItem resultRemoveAllItem = new JMenuItem("Remove All");

    /**
     * A menu item for <code>pathPopup</code> to remove the selected files
     * from the result model.
     */
    private JMenuItem resultRemoveSelectedItem = new JMenuItem(
            "Remove Selected Items");

    /**
     * The label for the result source list.
     */
    private JLabel resultSourceLabel = new JLabel("New Time Zone Version: ");

    /**
     * The combobox for choosing which timezone resource on the web to use in an
     * update.
     */
    private JComboBox resultSourceList = new JComboBox();

    /**
     * The panel where status components are shown.
     */
    private JPanel resultStatusPanel = new JPanel();

    /**
     * The label for the path list.
     */
    private JLabel resultTableLabel = new JLabel("ICU4J Jar Files found:");

    /**
     * The table where the result model is shown.
     */
    private JTable resultTable = new JTable();

    /**
     * A menu item for <code>pathPopup</code> to update all files in the
     * result model.
     */
    private JMenuItem resultUpdateAllItem = new JMenuItem("Update All");

    /**
     * An update button to update the selected files, or all files if none are
     * selected.
     */
    private JButton resultUpdateSelectedButton = new JButton("Update Selected");

    /**
     * The panel where update components are shown.
     */
    private JPanel resultUpdatePanel = new JPanel();

    /**
     * A menu item for <code>pathPopup</code> to update the selected files in
     * the result model.
     */
    private JMenuItem resultUpdateSelectedItem = new JMenuItem(
            "Update Selected Items");

    /**
     * The model for all the timezone resources on the web.
     */
    private SourceModel sourceModel;

    /**
     * The status bar for status messages.
     */
    private JLabel statusBar = new JLabel();

    /**
     * @param owner
     *            The GUILoader object that ownes this component.
     */
    public ResultComponent(final GUILoader owner) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(resultTableLabel);
        add(new JScrollPane(resultTable));
        add(resultStatusPanel);
        add(resultOptionPanel);
        add(resultUpdatePanel);

        resultStatusPanel.add(statusBar);
        resultOptionPanel.add(resultSourceLabel);
        resultOptionPanel.add(resultSourceList);
        resultUpdatePanel.add(resultCancelSearchButton);
        resultUpdatePanel.add(resultUpdateSelectedButton);
        resultUpdatePanel.add(resultCancelUpdateButton);

        resultChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        resultPopup.add(resultCopyItem);
        resultPopup.add(new JSeparator());
        resultPopup.add(resultRemoveSelectedItem);
        resultPopup.add(resultRemoveAllItem);
        resultPopup.add(new JSeparator());
        resultPopup.add(resultUpdateSelectedItem);
        resultPopup.add(resultUpdateAllItem);

        resultTable.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent event) {
                checkPopup(event);
            }

            public void mouseEntered(MouseEvent event) {
                checkPopup(event);
            }

            public void mouseExited(MouseEvent event) {
                checkPopup(event);
            }

            public void mousePressed(MouseEvent event) {
                checkPopup(event);
            }

            public void mouseReleased(MouseEvent event) {
                checkPopup(event);
            }

            private void checkPopup(MouseEvent event) {
                if (event.isPopupTrigger())
                    resultPopup.show((Component) event.getSource(), event
                            .getX(), event.getY());
            }
        });

        resultTable.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event) {
                int code = event.getKeyCode();
                if (code == KeyEvent.VK_DELETE
                        || code == KeyEvent.VK_BACK_SPACE)
                    resultModel.remove(resultTable.getSelectedRows());
            }
        });

        resultCopyItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String selection = "";
                int[] rows = resultTable.getSelectedRows();
                for (int i = 0; i < rows.length; i++)
                    selection += new File(resultModel.getValueAt(rows[i],
                            ResultModel.COLUMN_FILE_PATH).toString(),
                            resultModel.getValueAt(rows[i],
                                    ResultModel.COLUMN_FILE_NAME).toString())
                            .toString()
                            + "\n";
                getToolkit().getSystemClipboard().setContents(
                        new StringSelection(selection), null);
            }
        });

        resultRemoveSelectedItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                resultModel.remove(resultTable.getSelectedRows());
            }
        });

        resultRemoveAllItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                resultModel.removeAll();
            }
        });

        resultUpdateSelectedItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                owner
                        .update(resultTable.getSelectedRows(),
                                getSelectedSource());
            }
        });

        resultUpdateAllItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                owner.updateAll(getSelectedSource());
            }
        });

        resultUpdateSelectedButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int[] indices = resultTable.getSelectedRows();
                if (indices.length > 0)
                    owner.update(indices, getSelectedSource());
            }
        });

        resultCancelSearchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                owner.cancelSearch();
            }
        });

        resultCancelUpdateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                owner.cancelUpdate();
            }
        });
    }

    /**
     * Returns the status bar.
     * 
     * @return The status bar.
     */
    public JLabel getStatusBar() {
        return statusBar;
    }

    /**
     * Sets whether the cancel search button should be enabled.
     * 
     * @param value
     *            Whether the cancel search button should be enabled.
     */
    public void setCancelSearchEnabled(boolean value) {
        resultCancelSearchButton.setEnabled(value);
    }

    /**
     * Sets whether the cancel update button should be enabled.
     * 
     * @param value
     *            Whether the cancel update button should be enabled.
     */
    public void setCancelUpdateEnabled(boolean value) {
        resultCancelUpdateButton.setEnabled(value);
    }

    /**
     * Sets the result model.
     * 
     * @param resultModel
     *            The result model.
     */
    public void setResultModel(ResultModel resultModel) {
        this.resultModel = resultModel;
        resultTable.setModel(resultModel);
    }

    /**
     * Sets the source model.
     * 
     * @param sourceModel
     *            The source model.
     */
    public void setSourceModel(SourceModel sourceModel) {
        this.sourceModel = sourceModel;
        resultSourceList.setModel(sourceModel);
    }

    /**
     * Sets whether the update button should be enabled.
     * 
     * @param value
     *            Whether the update button should be enabled.
     */
    public void setUpdateEnabled(boolean value) {
        resultUpdateSelectedButton.setEnabled(value);
    }

    /**
     * Returns the URL of the currently selected item in the result source list.
     * 
     * @return The URL of the currently selected item in the result source list.
     */
    private URL getSelectedSource() {
        return sourceModel.getURL(resultSourceList.getSelectedItem());
    }
}
