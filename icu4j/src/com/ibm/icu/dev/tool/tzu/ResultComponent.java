/**
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
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
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

public class ResultComponent extends JComponent {
    public ResultComponent(final GUILoader owner, final ResultModel resultModel, final SourceModel sourceModel) {
        this.resultModel = resultModel;
        this.sourceModel = sourceModel;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(resultInputPanel);
        add(new JScrollPane(resultTable));
        add(resultOptionPanel);
        add(resultUpdatePanel);

        resultInputPanel.add(resultField);
        resultInputPanel.add(resultBrowseButton);
        // resultOptionPanel.add(resultHideOption);
        resultOptionPanel.add(resultSourceList);
        resultUpdatePanel.add(resultCancelSearchButton);
        resultUpdatePanel.add(resultUpdateButton);
        resultUpdatePanel.add(resultCancelUpdateButton);

        resultTable.setModel(resultModel);
        resultSourceList.setModel(sourceModel);

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

            public void mouseClicked(MouseEvent event) {
                checkPopup(event);
            }

            private void checkPopup(MouseEvent event) {
                if (event.isPopupTrigger())
                    resultPopup.show((Component) event.getSource(), event.getX(), event.getY());
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
                owner.update(resultTable.getSelectedRows(), getSelectedSource());
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
                int returnVal = resultChooser.showOpenDialog(ResultComponent.this);
                if (returnVal == JFileChooser.APPROVE_OPTION)
                    addFile(resultChooser.getSelectedFile());
            }
        });

        resultHideOption.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                resultModel.setHidden(event.getStateChange() == ItemEvent.SELECTED);
            }
        });
    }

    private URL getSelectedSource() {
        return sourceModel.getURL(resultSourceList.getSelectedItem());
    }

    private boolean addFile(File file) {
        if (!resultModel.add(file)) {
            JOptionPane.showMessageDialog(ResultComponent.this, "\"" + file.toString()
                    + "\" is not an updatable ICU jar file.", "Cannot add file", JOptionPane.ERROR_MESSAGE);
            return false;
        } else {
            return true;
        }
    }

    public void setUpdateEnabled(boolean value) {
        resultUpdateButton.setEnabled(value);
    }

    public void setCancelSearchEnabled(boolean value) {
        resultCancelSearchButton.setEnabled(value);
    }

    public void setCancelUpdateEnabled(boolean value) {
        resultCancelUpdateButton.setEnabled(value);
    }

    public JTextComponent getStatusBar() {
        return statusBar;
    }

    private JPanel resultInputPanel = new JPanel();

    // private JPanel resultTablePanel = new JPanel();

    private JPanel resultOptionPanel = new JPanel();

    private JPanel resultUpdatePanel = new JPanel();

    private JTable resultTable = new JTable();

    private JTextField resultField = new JTextField(30);

    private JCheckBox resultHideOption = new JCheckBox("Hide Unreadable/Unwritable Files", true);

    private JButton resultBrowseButton = new JButton("Browse...");

    private JButton resultUpdateButton = new JButton("Update");

    private JButton resultCancelUpdateButton = new JButton("Cancel Update");

    private JButton resultCancelSearchButton = new JButton("Cancel Search");

    private JFileChooser resultChooser = new JFileChooser();

    private JPopupMenu resultPopup = new JPopupMenu();

    private JMenuItem resultRemoveSelectedItem = new JMenuItem("Remove Selected Items");

    private JMenuItem resultRemoveAllItem = new JMenuItem("Remove All");

    private JMenuItem resultUpdateSelectedItem = new JMenuItem("Update Selected Items");

    private JMenuItem resultUpdateAllItem = new JMenuItem("Update All");

    private JComboBox resultSourceList = new JComboBox();

    private ResultModel resultModel;

    private SourceModel sourceModel;

    private JTextComponent statusBar = new JTextField(30);

    public static final long serialVersionUID = 1341;
}
