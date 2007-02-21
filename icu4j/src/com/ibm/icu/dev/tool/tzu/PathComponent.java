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
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;

public class PathComponent extends JComponent {
    public PathComponent(final GUILoader owner, final PathModel pathModel) {
        this.owner = owner;
        this.pathModel = pathModel;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(pathInputPanel);
        add(new JScrollPane(pathList));
        add(pathOptionPanel);
        add(pathSearchPanel);

        pathInputPanel.add(pathSignBox);
        pathInputPanel.add(pathField);
        pathInputPanel.add(pathBrowseButton);
        pathOptionPanel.add(pathSubdirOption);
        pathSearchPanel.add(pathSearchButton);

        pathList.setModel(pathModel);

        pathChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        pathPopup.add(pathAddAllDrivesItem);
        pathPopup.add(new JSeparator());
        pathPopup.add(pathRemoveSelectedItem);
        pathPopup.add(pathRemoveAllItem);
        pathPopup.add(new JSeparator());
        pathPopup.add(pathSearchSelectedItem);
        pathPopup.add(pathSearchAllItem);

        pathField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                addFile(new File(pathField.getText()));
                pathField.selectAll();
            }
        });

        pathList.addMouseListener(new MouseListener() {
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
                    pathPopup.show((Component) event.getSource(), event.getX(),
                            event.getY());
            }
        });

        pathList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_DELETE)
                    pathModel.remove(pathList.getSelectedIndices());
            }
        });

        pathRemoveSelectedItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                pathModel.remove(pathList.getSelectedIndices());
            }
        });

        pathRemoveAllItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                pathModel.removeAll();
            }
        });

        pathSearchSelectedItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                owner.search(pathList.getSelectedIndices(), pathSubdirOption
                        .isSelected());
            }
        });

        pathSearchAllItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                owner.searchAll(pathSubdirOption.isSelected());
            }
        });

        pathSearchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int[] indices = pathList.getSelectedIndices();
                if (indices.length > 0)
                    owner.search(indices, pathSubdirOption.isSelected());
                else
                    owner.searchAll(pathSubdirOption.isSelected());
            }
        });

        pathAddAllDrivesItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                pathModel.addAllDrives();
            }
        });

        pathBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int returnVal = pathChooser.showOpenDialog(PathComponent.this);
                if (returnVal == JFileChooser.APPROVE_OPTION)
                    addFile(pathChooser.getSelectedFile());
            }
        });

        pathModel.loadPaths();
    }

    private boolean isIncluded() {
        return ((String) pathSignBox.getSelectedItem()).equals("Include");
    }

    private void addFile(File file) {
        if (!pathModel.add(new IncludePath(file, isIncluded())))
            JOptionPane.showMessageDialog(PathComponent.this, "\""
                    + file.getPath() + "\" is not a valid file or path.",
                    "Cannot add path/file", JOptionPane.ERROR_MESSAGE);
    }

    public void setSearchEnabled(boolean value) {
        pathSearchButton.setEnabled(value);
    }

    private GUILoader owner;

    private JPanel pathInputPanel = new JPanel();

    private JPanel pathListPanel = new JPanel();

    private JPanel pathOptionPanel = new JPanel();

    private JPanel pathSearchPanel = new JPanel();

    private JList pathList = new JList();

    private JComboBox pathSignBox = new JComboBox(new Object[] { "Include",
            "Exclude" });

    private JTextField pathField = new JTextField(30);

    private JCheckBox pathSubdirOption = new JCheckBox("Search Subdirectories",
            true);

    private JButton pathBrowseButton = new JButton("Browse...");

    private JButton pathSearchButton = new JButton("Search");

    private JFileChooser pathChooser = new JFileChooser();

    private JPopupMenu pathPopup = new JPopupMenu();

    private JMenuItem pathAddAllDrivesItem = new JMenuItem(
            "Add All Drives to List");

    private JMenuItem pathRemoveSelectedItem = new JMenuItem(
            "Remove Selected Items");

    private JMenuItem pathRemoveAllItem = new JMenuItem("Remove All");

    private JMenuItem pathSearchSelectedItem = new JMenuItem(
            "Search Selected Items");

    private JMenuItem pathSearchAllItem = new JMenuItem("Search All");

    private PathModel pathModel;
}
