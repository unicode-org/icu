/*
 *****************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;

/**
 * An exporter plug-in class for RBManager. The resources exported here conform to
 * the Java standard for Resource Bundles as specified in java.util.ListResourceBundle.
 * The output files are compilable java files that are not associated with any
 * package.
 * 
 * @author Jared Jackson
 * @see com.ibm.rbm.RBManager
 */
public class RBJavaExporter extends RBExporter {
    private String packageName = null;
    private boolean publicClass = true;
    private boolean publicMethods = true;
	
	
    public RBJavaExporter() {
        super();
		
        // Initialize the file chooser if necessary
        if (chooser == null) {
            chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileFilter(){
                public String getDescription() {
                    return "Java Source Files";
                }
                public boolean accept(File f) {
                    if (f.isDirectory()) return true;
                    if (f.getName().endsWith(".java") && f.getName().indexOf("_") < 0) return true;
                    return false;
                }
            });
        }
    }
	
    public void export(RBManager rbm) throws IOException {
        if (rbm == null) return;
        // Open the additional Dialog
        RBJavaExporterDialog parametersDialog = new RBJavaExporterDialog();
        packageName = parametersDialog.getPackageName();
        publicClass = parametersDialog.isClassPublic();
        publicMethods = parametersDialog.isMethodsPublic();
		
        // Open the Save Dialog
        int ret_val = chooser.showSaveDialog(null);
        if (ret_val != JFileChooser.APPROVE_OPTION) return;
        // Retrieve basic file information
        File file = chooser.getSelectedFile();                  // The file(s) we will be working with
        File directory = new File(file.getParent());            // The directory we will be writing to
        String base_name = file.getName();                      // The base name of the files we will write
        if (base_name == null || base_name.equals("")) base_name = rbm.getBaseClass();
        if (base_name.endsWith(".java")) base_name = base_name.substring(0,base_name.length()-5);
		
        Vector bundle_v = rbm.getBundles();
        for (int i=0; i < bundle_v.size(); i++) {
            Bundle bundle = (Bundle)bundle_v.elementAt(i);
            String base_enc = base_name;
            if (bundle.encoding != null && !bundle.encoding.equals("")) base_enc = base_enc + "_" + bundle.encoding;
            String file_name = base_enc + ".java";
			
            StringBuffer buffer = new StringBuffer();
            buffer.append("/* File:    " + file_name + "\n");
            buffer.append(" * Date:    " + (new Date()) + "\n");
            buffer.append(" * Comment: This file was generated automatically by RBManager" + "\n");
            buffer.append(" */\n\n");
            if (packageName != null) {
                buffer.append("package " + packageName + ";\n\n");
            }	
            buffer.append("import java.util.ListResourceBundle;\n\n");
            buffer.append((publicClass ? "public " : "protected "));
            buffer.append("class " + base_enc + " extends ListResourceBundle {\n");
            buffer.append("\t" + (publicMethods ? "public" : "protected") + " Object[][] getContents() {\n");
            buffer.append("\t\treturn contents;\n");
            buffer.append("\t}\n");
            buffer.append("\tprivate static final Object[][] contents = {\n");
            buffer.append("\t// LOCALIZE THIS\n");
            
            Vector group_v = bundle.getGroupsAsVector();
            for (int j=0; j < group_v.size(); j++) {
                BundleGroup group = (BundleGroup)group_v.elementAt(j);
                Vector item_v = group.getItemsAsVector();
                for (int k=0; k < item_v.size(); k++) {
                    BundleItem item = (BundleItem)item_v.elementAt(k);
                    buffer.append("\t\t{\"" + item.getKey() + "\", \"" + item.getTranslation() + "\"},\t// " + item.getComment() + "\n");
                } // end for - k
            } // end for - j
            
            buffer.append("\t// END OF MATERIAL TO LOCALIZE\n");
            buffer.append("\t};\n");
            buffer.append("}");
            
            // Write out the file
            File write_file = new File(directory, file_name);
            FileWriter writer = new FileWriter(write_file);
            writer.write(buffer.toString());
            writer.flush();
            writer.close();
        } // end for - i
    }
}

class RBJavaExporterDialog extends JDialog {
    JCheckBox packageCheck;
    JRadioButton classPublicRadio;
    JRadioButton classProtectedRadio;
    JRadioButton methodsPublicRadio;
    JRadioButton methodsProtectedRadio;
    JTextField packageField;
	
    public RBJavaExporterDialog() {
        super(new JFrame(), Resources.getTranslation("dialog_title_export_java_options"), true);
        initComponents();
    }
	
    public String getPackageName() {
        if (!(packageCheck.isSelected())) return null;
        String retVal = packageField.getText();
        if (retVal == null || retVal.trim().equals("")) return null;
        return retVal.trim();
    }
	
    public boolean isClassPublic() {	
        return classPublicRadio.isSelected();	
    }
    
    public boolean isMethodsPublic() {
        return methodsPublicRadio.isSelected();
    }
	
    private void handleClose() {
        setVisible(false);
        dispose();
    }
	
    private void initComponents() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().removeAll();
		
        packageCheck = new JCheckBox(Resources.getTranslation("export_java_package"), false);
        classPublicRadio = new JRadioButton(Resources.getTranslation("export_java_class_public"), true);
        classProtectedRadio = new JRadioButton(Resources.getTranslation("export_java_class_protected"), false);
        methodsPublicRadio = new JRadioButton(Resources.getTranslation("export_java_class_public"), true);
        methodsProtectedRadio = new JRadioButton(Resources.getTranslation("export_java_class_protected"), false);
        packageField = new JTextField();
        packageField.setColumns(30);
		
        JButton okButton = new JButton(Resources.getTranslation("OK"));
        JLabel titleLabel = new JLabel(Resources.getTranslation("export_java_title"), SwingConstants.LEFT);
		
        JPanel okPanel = new JPanel();
        okPanel.add(okButton);
        JPanel centerPanel = new JPanel(new GridLayout(1,1));
        centerPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        Box centerBox = Box.createVerticalBox();
        Box packageBox = Box.createHorizontalBox();
        packageBox.add(packageCheck);
        packageBox.add(packageField);
        centerBox.add(packageBox);
        centerBox.add(new JSeparator());
        centerBox.add(classPublicRadio);
        centerBox.add(classProtectedRadio);
        centerBox.add(new JSeparator());
        centerBox.add(methodsPublicRadio);
        centerBox.add(methodsProtectedRadio);
        centerPanel.add(centerBox);
		
        getContentPane().add(titleLabel, BorderLayout.NORTH);
        getContentPane().add(okPanel, BorderLayout.SOUTH);
        getContentPane().add(centerPanel, BorderLayout.CENTER);
		
        okButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ev) {
                handleClose();
            }
        });
		
        ButtonGroup classGroup = new ButtonGroup();
        ButtonGroup methodsGroup = new ButtonGroup();
        classGroup.add(classPublicRadio);
        classGroup.add(classProtectedRadio);
        methodsGroup.add(methodsPublicRadio);
        methodsGroup.add(methodsProtectedRadio);
		
        //validateTree();
        pack();
        //setLocation(new Point(25,25));
        setVisible(true);
    }
}