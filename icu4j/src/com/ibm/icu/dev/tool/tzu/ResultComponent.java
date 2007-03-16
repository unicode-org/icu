/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and others.
 * All Rights Reserved.
 * ******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.net.URL;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;

/**
 * The path list GUI component.
 */
public class ResultComponent extends JComponent {
    /**
     * The serializable UID.
     */
    public static final long serialVersionUID = 1341;

    /**
     * A browse button to add specific results.
     */
    private JButton resultBrowseButton = new JButton("Browse...");

    /**
     * A cancel search button to cancel a search if one is currently occuring.
     */
    private JButton resultCancelSearchButton = new JButton("Cancel Search");

    /**
     * A cancel update button to cancel an update if one is currently occuring.
     */
    private JButton resultCancelUpdateButton = new JButton("Cancel Update");

    /**
     * The dialog that comes up when the browse button is clicked.
     */
    private JFileChooser resultChooser = new JFileChooser();

    /**
     * The field where an ICU4J jar file can be directly added.
     */
    private JTextField resultField = new JTextField(30);

    /**
     * The checkbox for whether files that are either unreadable or unwritable
     * are displayed (and likewise updated).
     */
    private JCheckBox resultHideOption = new JCheckBox(
            "Hide Unreadable and Unwritable Files", true);

    /**
     * The panel where input components are shown.
     */
    private JPanel resultInputPanel = new JPanel();

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
     * The combobox for choosing which timezone resource on the web to use in an
     * update.
     */
    private JComboBox resultSourceList = new JComboBox();

    /**
     * The panel where status components are shown.
     */
    private JPanel resultStatusPanel = new JPanel();

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
    private JButton resultUpdateButton = new JButton("Update");

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
        add(resultInputPanel);
        add(new JScrollPane(resultTable));
        add(resultStatusPanel);
        add(resultOptionPanel);
        add(resultUpdatePanel);

        resultInputPanel.add(resultField);
        resultInputPanel.add(resultBrowseButton);
        resultStatusPanel.add(statusBar);
        resultOptionPanel.add(resultSourceList);
        resultUpdatePanel.add(resultCancelSearchButton);
        resultUpdatePanel.add(resultUpdateButton);
        resultUpdatePanel.add(resultCancelUpdateButton);

        resultChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        resultPopup.add(resultRemoveSelectedItem);
        resultPopup.add(resultRemoveAllItem);
        resultPopup.add(new JSeparator());
        resultPopup.add(resultUpdateSelectedItem);
        resultPopup.add(resultUpdateAllItem);

        resultField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                addFile(new File(resultField.getText()));
                resultField.selectAll();
            }
        });

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
                if (event.getKeyCode() == KeyEvent.VK_DELETE)
                    resultModel.remove(resultTable.getSelectedRows());
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

        resultUpdateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int[] indices = resultTable.getSelectedRows();
                if (indices.length > 0)
                    owner.update(indices, getSelectedSource());
                else
                    owner.updateAll(getSelectedSource());
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

        resultBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int returnVal = resultChooser
                        .showOpenDialog(ResultComponent.this);
                if (returnVal == JFileChooser.APPROVE_OPTION)
                    addFile(resultChooser.getSelectedFile());
            }
        });

        resultHideOption.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                resultModel
                        .setHidden(event.getStateChange() == ItemEvent.SELECTED);
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
        resultUpdateButton.setEnabled(value);
    }

    /**
     * Adds a file to the result list. The file must be an updatable ICU4J jar
     * by the standards laid out in ICUFile.
     * 
     * @param file
     *            The file to add.
     * @return Whether the file was added successfully.
     */
    private boolean addFile(File file) {
        if (!resultModel.add(file)) {
            JOptionPane.showMessageDialog(ResultComponent.this, "\""
                    + file.toString() + "\" is not an updatable ICU jar file.",
                    "Cannot add file", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
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
