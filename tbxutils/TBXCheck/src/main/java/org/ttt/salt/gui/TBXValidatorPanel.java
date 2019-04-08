/*
 * $Id$
 *-----------------------------------------------------------------------------
 * Copyright 2000 Lance Finn Helsten (helsten@acm.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ttt.salt.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.SortedSet;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.text.MutableAttributeSet;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 *
 * @author  Lance Finn Helsten
 * @version $Id$
 * @license Licensed under the Apache License, Version 2.0.
 */
public class TBXValidatorPanel extends javax.swing.JPanel
    implements java.awt.event.HierarchyBoundsListener, ActionListener
{
    /*
     */

    /** SCM information. */
    public static final String RCSID = "$Id$";

    /** Main logger for this panel. */
    private static Logger LOGGER;
    
    /** Validation option */
    private String validationOption;


    //These variables define the various parts of the panel. Since this will
    //change the javadoc is not required on each, but the name of the variable
    //should be highly descriptive.
    //CHECKSTYLE: JavadocVariable OFF
    private javax.swing.JComboBox boxCountry;
    private javax.swing.JComboBox boxLanguage;
    private javax.swing.JComboBox boxLogging;
    private javax.swing.JCheckBox cbNoLangCheck;
    private javax.swing.JRadioButton rbVersionTwoCheck;
    private javax.swing.JRadioButton rbVersionThreeCheck;
    private javax.swing.JButton buttonValidate;
    private javax.swing.JButton syncFilesButton;
    private javax.swing.JButton addSCHFile;
    private javax.swing.JButton addRNGFile;
    private javax.swing.JLabel labelCountry;
    private javax.swing.JLabel labelLanguage;
    private javax.swing.JLabel labelLogging;
    private javax.swing.JPanel panelButtons;
    private javax.swing.JPanel panelOptions;
    private javax.swing.JPanel titlePanel;
    private javax.swing.JLabel version2Text;
    private javax.swing.JLabel version3Text;
    private javax.swing.JPanel versionSelectPanel;
    private javax.swing.JPanel version2Panel;
    private javax.swing.JPanel version3Panel;
    private javax.swing.JPanel versionWorkArea;
    private javax.swing.JButton helpButton;
    //CHECKSTYLE: JavadocVariable OFF
    
    /**
     * Creates new form TBXValidatorPanel.
     *
     * @param bndl The {@link java.util.ResourceBundle} that contains all the
     *  resources necessary to build this validation panel.
     */
    public TBXValidatorPanel(ResourceBundle bndl)
    {
        LOGGER = Logger.getLogger("org.ttt.salt");

        cbNoLangCheck = new javax.swing.JCheckBox("No xml:lang validation.");
        cbNoLangCheck.setActionCommand("NoLangCheckChanged");

        rbVersionTwoCheck = new javax.swing.JRadioButton("Validate TBX Version 2");
        rbVersionTwoCheck.setActionCommand("validate-two");

        rbVersionThreeCheck = new javax.swing.JRadioButton("Validate TBX Version 3");
        rbVersionThreeCheck.setActionCommand("validate-three");

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(rbVersionTwoCheck);
        buttonGroup.add(rbVersionThreeCheck);

        rbVersionTwoCheck.addActionListener(this);
        rbVersionThreeCheck.addActionListener(this);

        version2Text = new javax.swing.JLabel("TBX Version 2");
        version2Text.setFont(new Font("", Font.BOLD, 20));
        version2Text.setHorizontalAlignment(JLabel.LEFT);
        version2Text.setVerticalAlignment(JLabel.TOP);
        version2Text.setBorder(new EmptyBorder(0, 100, 0, 100));

        version3Text = new javax.swing.JLabel("TBX Version 3");
        version3Text.setFont(new Font("", Font.BOLD, 20));
        version3Text.setHorizontalAlignment(JLabel.RIGHT);
        version3Text.setVerticalAlignment(JLabel.TOP);
        version3Text.setBorder(new EmptyBorder(0, 100, 0, 100));

        titlePanel = new JPanel();
        titlePanel.add(version2Text);
        titlePanel.add(version3Text);

        syncFilesButton = new JButton("Sync Validation Files over Internet");
        syncFilesButton.addActionListener(e -> {
            // Perform Sync from internet
            download();
        });

        addSCHFile = new JButton("Upload a custom .sch file for validation");
        addRNGFile = new JButton("Upload a custom .rng file for validation");

        addSCHFile.addActionListener(e -> {
            // Take the file, rename it, store it, detect dialect of tbx file,
            // check if we have a custom validation set for that file name,
            // inform, validate, finish

            storeCustomFileSCH();
        });

        addRNGFile.addActionListener(e -> {
            storeCustomFileRNG();
        });



        versionSelectPanel = new JPanel();
        versionSelectPanel.add(rbVersionTwoCheck);
        versionSelectPanel.add(rbVersionThreeCheck);

        boxLogging = new javax.swing.JComboBox();
        boxLanguage = new javax.swing.JComboBox();
        boxCountry = new javax.swing.JComboBox();

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        add(titlePanel);

        add(Box.createRigidArea(new Dimension(0,5)));

        // Version 2 / 3 Workspace START

        version2Panel = new JPanel();
        version3Panel = new JPanel();
        version3Panel.setLayout(new BoxLayout(version3Panel, BoxLayout.PAGE_AXIS));

        layoutPanelOptions(bndl);
        version2Panel.add(panelOptions);

        version3Panel.add(Box.createRigidArea(new Dimension(0,10)));
        syncFilesButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addSCHFile.setAlignmentX(Component.CENTER_ALIGNMENT);
        addRNGFile.setAlignmentX(Component.CENTER_ALIGNMENT);

        version3Panel.add(syncFilesButton);
        version3Panel.add(Box.createRigidArea(new Dimension(0, 3)));
        version3Panel.add(addRNGFile);
        version3Panel.add(Box.createRigidArea(new Dimension(0, 3)));
        version3Panel.add(addSCHFile);

        versionWorkArea = new JPanel();
        versionWorkArea.setLayout(new BoxLayout(versionWorkArea, BoxLayout.LINE_AXIS));
        versionWorkArea.add(Box.createRigidArea(new Dimension(15, 0)));
        versionWorkArea.add(version2Panel);
        versionWorkArea.add(Box.createHorizontalGlue());
        versionWorkArea.add(new JSeparator(JSeparator.VERTICAL), BorderLayout.LINE_START);
        versionWorkArea.add(version3Panel);
        versionWorkArea.add(Box.createRigidArea(new Dimension(15, 0)));
        versionWorkArea.setAlignmentX(JComponent.CENTER_ALIGNMENT);

        add(versionWorkArea);

        // Version 2 / 3 Workspace END

        add(Box.createRigidArea(new Dimension(0,5)));

        add(versionSelectPanel);

        add(Box.createRigidArea(new Dimension(0,5)));

        layoutButtons(bndl);
        add(panelButtons);


        try
        {
            System.setProperty("java.util.logging.config.class", "org.ttt.salt.LogConfig");
            LogManager.getLogManager().readConfiguration();
        }
        catch (java.io.IOException err)
        {
            err.printStackTrace();
            System.exit(1);
        }        
    }

    /**
     * Layout the buttons panel.
     *
     * @param bndl The {@link java.util.ResourceBundle} that contains all the
     *  resources needed to build this panel.
     */
    private void layoutButtons(ResourceBundle bndl)
    {
        ActionOpen action = (ActionOpen) TBXAbstractAction.getAction(ActionOpen.class);
        buttonValidate = new javax.swing.JButton(action);
        cbNoLangCheck.addActionListener(action);
        boxLogging.addActionListener(action);


        panelButtons = new javax.swing.JPanel();
        panelButtons.setLayout(new FlowLayout());

        panelButtons.add(buttonValidate);
        helpButton = new JButton("Help");
        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createFrame();
            }
        });

        panelButtons.add(helpButton);

    }
    
    /**
     * Layout the options panel.
     *
     * @param bndl The {@link java.util.ResourceBundle} that contains all the
     *  resources needed to build this panel.
     */
    private void layoutPanelOptions(ResourceBundle bndl)
    {
        panelOptions = new javax.swing.JPanel();
        labelLogging = new javax.swing.JLabel("Logging");
        labelLanguage = new javax.swing.JLabel("Language");
        labelCountry = new javax.swing.JLabel("Country");
        
        
        //Configure        
        String[] logvals = {"SEVERE", "WARNING", "INFO", "CONFIG", "FINE", "FINER", "FINEST"};
        boxLogging.setModel(new javax.swing.DefaultComboBoxModel(logvals));
        boxLogging.setSelectedItem("INFO"); //TODO: this needs to based on preferences
        
        SortedSet<String> iso639 = ISOReference.getInstance().get639alpha2();
        boxLanguage.setModel(new javax.swing.DefaultComboBoxModel(iso639.toArray()));
        boxLanguage.setSelectedItem("en"); //TODO: this needs to based on LOCALE and preferences
        
        SortedSet<String> iso3166 = ISOReference.getInstance().get3166alpha2();
        boxCountry.setModel(new javax.swing.DefaultComboBoxModel(iso3166.toArray()));
        boxCountry.setSelectedItem("US"); //TODO: this needs to based on LOCALE and preferences
        
        
        //Layout
        panelOptions.setLayout(new SpringLayout());
        
        panelOptions.add(new javax.swing.JLabel(" "));
        panelOptions.add(cbNoLangCheck);
        
        labelLogging.setLabelFor(boxLogging);
        panelOptions.add(labelLogging);
        panelOptions.add(boxLogging);

        
        /* TODO: Remove when localization returns
        labelLanguage.setLabelFor(boxLanguage);
        panelOptions.add(labelLanguage);
        panelOptions.add(boxLanguage);

        labelCountry.setLabelFor(boxCountry);
        panelOptions.add(labelCountry);
        panelOptions.add(boxCountry);
        */
        
        final int rows = 2; //TODO: Change to 4 when language and country return
        final int cols = 2;
        final int initX = 6;
        final int initY = 6;
        final int xPad = 6;
        final int yPad = 6;
        SpringUtilities.makeCompactGrid(panelOptions, rows, cols, initX, initY, xPad, yPad);
        panelOptions.setOpaque(true);  //content panes must be opaque

        /* TODO: When JDK 1.6 is available on MacOS then move to GroupLayout
        org.jdesktop.layout.GroupLayout panelOptionsLayout = new org.jdesktop.layout.GroupLayout(panelOptions);
        panelOptions.setLayout(panelOptionsLayout);
        panelOptionsLayout.setHorizontalGroup(
            panelOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panelOptionsLayout.createSequentialGroup()
                .addContainerGap()
                .add(panelOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(labelLogging)
                    .add(labelLanguage)
                    .add(labelCountry))
                .add(24, 24, 24)
                .add(panelOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(boxLogging, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 131,
                                    org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(panelOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, boxLanguage, 0,
                                    org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, boxCountry,
                                    org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 68,
                                    org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(265, Short.MAX_VALUE))
        );
        panelOptionsLayout.setVerticalGroup(
            panelOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panelOptionsLayout.createSequentialGroup()
                .addContainerGap()
                .add(panelOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(boxLogging, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
                                org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
                                org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(labelLogging))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(panelOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(boxLanguage, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
                                org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
                                org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(labelLanguage))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(panelOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(boxCountry, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
                                org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
                                org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(labelCountry))
                .addContainerGap(169, Short.MAX_VALUE))
        );
        */
    }

    /** {@inheritDoc} */
    public void ancestorMoved(java.awt.event.HierarchyEvent evt)
    {
    }
    
    /** {@inheritDoc} */
    public void ancestorResized(java.awt.event.HierarchyEvent evt)
    {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Set value for selected version
        validationOption = e.getActionCommand();
        Singleton.getSingletonInstance().setVersionSelection(validationOption);
    }

    void download() {

        InputStream core_sch = null;
        InputStream core_rng = null;
        InputStream min_sch = null;
        InputStream min_rng = null;
        InputStream basic_sch = null;
        InputStream basic_rng = null;

        try {
            core_sch = new URL("https://raw.githubusercontent.com/LTAC-Global/TBX-Core_dialect/master/Schemas/TBX-Core.sch").openStream();
            core_rng = new URL("https://raw.githubusercontent.com/LTAC-Global/TBX-Core_dialect/master/Schemas/TBXcoreStructV03_TBX-Core_integrated.rng").openStream();
            min_sch = new URL("https://raw.githubusercontent.com/LTAC-Global/TBX-Min_dialect/master/DCA/TBX-Min_DCA.sch").openStream();
            min_rng = new URL("https://raw.githubusercontent.com/LTAC-Global/TBX-Min_dialect/master/DCA/TBXcoreStructV03_TBX-Min_integrated.rng").openStream();
            basic_sch = new URL("https://raw.githubusercontent.com/LTAC-Global/TBX-Basic_dialect/master/DCA/TBX-Basic_DCA.sch").openStream();
            basic_rng = new URL("https://raw.githubusercontent.com/LTAC-Global/TBX-Basic_dialect/master/DCA/TBXcoreStructV03_TBX-Basic_integrated.rng").openStream();
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            Files.copy(core_sch, Paths.get("TBXCheck/src/main/resources/xml/TBX-Core_dialect-master/Schemas/TBX-Core.sch"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(core_rng, Paths.get("TBXCheck/src/main/resources/xml/TBX-Core_dialect-master/Schemas/TBXcoreStructV03_TBX-Core_integrated.rng"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(min_sch, Paths.get("TBXCheck/src/main/resources/xml/TBX-Min_dialect-master/DCA/TBX-Min_DCA.sch"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(min_rng, Paths.get("TBXCheck/src/main/resources/xml/TBX-Min_dialect-master/DCA/TBXcoreStructV03_TBX-Min_integrated.rng"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(basic_sch, Paths.get("TBXCheck/src/main/resources/xml/TBX-Basic_dialect-master/DCA/TBX-Basic_DCA.sch"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(basic_rng, Paths.get("TBXCheck/src/main/resources/xml/TBX-Basic_dialect-master/DCA/TBXcoreStructV03_TBX-Basic_integrated.rng"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            core_sch.close();
            core_rng.close();
            min_sch.close();
            min_rng.close();
            basic_sch.close();
            basic_rng.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Inform the user of our success!
        createFrameForDownloadSuccess();
    }

    private void createFrame() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Help Window");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                try
                {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

                JTextPane questionHowDoIKnow, questionHowToUse;
                questionHowToUse = new JTextPane();
                questionHowToUse.setContentType("text/html"); // let the text pane know this is what you want
                questionHowToUse.setText("<html><strong>How do I use the TBX-Checker?</strong></html>"); // showing off
                questionHowToUse.setEditable(false); // as before
                questionHowToUse.setBackground(null); // this is the same as a JLabel
                questionHowToUse.setBorder(null); // remove the border

                questionHowDoIKnow = new JTextPane();
                questionHowDoIKnow.setContentType("text/html"); // let the text pane know this is what you want
                questionHowDoIKnow.setText("<html><strong>How do I know if my file is version 2 or version 3?</strong></html>");
                questionHowDoIKnow.setEditable(false); // as before
                questionHowDoIKnow.setBackground(null); // this is the same as a JLabel
                questionHowDoIKnow.setBorder(null); // remove the border

                Font f = questionHowDoIKnow.getFont();
                questionHowDoIKnow.setFont(f.deriveFont(f.getStyle() | Font.BOLD));

                f = questionHowToUse.getFont();
                questionHowToUse.setFont(f.deriveFont(f.getStyle() | Font.BOLD));

                JTextPane answerSpyglass, answerHowToUse2, answerHowToUse3;

                answerHowToUse2 = new JTextPane();
                answerHowToUse2.setContentType("text/html"); // let the text pane know this is what you want
                answerHowToUse2.setText("<html>For version 2, select if you wish to validate the language codes, " +
                        "and then select how detailed of logs you want. Select the version 2 option and then open " +
                        "your file.</html>");
                answerHowToUse2.setEditable(false); // as before
                answerHowToUse2.setBackground(null); // this is the same as a JLabel
                answerHowToUse2.setBorder(null); // remove the border

                answerHowToUse3 = new JTextPane();
                answerHowToUse3.setContentType("text/html"); // let the text pane know this is what you want
                answerHowToUse3.setText("<html>For version 3, you may choose to download the latest versions of the " +
                        "validation files over an internet connection if you wish. Otherwise select the version 3 button " +
                        "and open your file.</html>");
                answerHowToUse3.setEditable(false); // as before
                answerHowToUse3.setBackground(null); // this is the same as a JLabel
                answerHowToUse3.setBorder(null); // remove the border

                answerSpyglass = new JTextPane();
                answerSpyglass.setContentType("text/html"); // let the text pane know this is what you want
                answerSpyglass.setText("<html>Visit <a href=\"https://www.tbxinfo.net/tbx-spyglass/\">https://www.tbxinfo.net/tbx-spyglass/</a> which is an easy to use tool " +
                        "that will help you identify which version your file is.</html>");
                answerSpyglass.setEditable(false); // as before
                answerSpyglass.setBackground(null); // this is the same as a JLabel
                answerSpyglass.setBorder(null); // remove the border

                answerSpyglass.addHyperlinkListener(new HyperlinkListener() {
                    public void hyperlinkUpdate(HyperlinkEvent e) {
                        if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                            if(Desktop.isDesktopSupported()) {
                                try {
                                    Desktop.getDesktop().browse(e.getURL().toURI());
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                } catch (URISyntaxException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }
                    }
                });

                JTextPane questionUpdate;
                questionUpdate = new JTextPane();
                questionUpdate.setContentType("text/html"); // let the text pane know this is what you want
                questionUpdate.setText("<html><strong>How can I update my version 2 TBX file to version 3?</strong></html>"); // showing off
                questionUpdate.setEditable(false); // as before
                questionUpdate.setBackground(null); // this is the same as a JLabel
                questionUpdate.setBorder(null); // remove the border

                JTextPane answerUpdate;
                answerUpdate = new JTextPane();
                answerUpdate.setContentType("text/html"); // let the text pane know this is what you want
                answerUpdate.setText("<html>Visit <a href=\"https://www.tbxinfo.net/tbx-updater/\">https://www.tbxinfo.net/tbx-updater/</a> " +
                        "where you can convert version 2 TBX files to version 3.</html>");
                answerUpdate.setEditable(false); // as before
                answerUpdate.setBackground(null); // this is the same as a JLabel
                answerUpdate.setBorder(null); // remove the border

                answerUpdate.addHyperlinkListener(new HyperlinkListener() {
                    public void hyperlinkUpdate(HyperlinkEvent e) {
                        if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                            if(Desktop.isDesktopSupported()) {
                                try {
                                    Desktop.getDesktop().browse(e.getURL().toURI());
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                } catch (URISyntaxException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }
                    }
                });


                panel.add(questionHowToUse);
                panel.add(Box.createRigidArea(new Dimension(0,15)));
                panel.add(answerHowToUse2);
                panel.add(answerHowToUse3);
                panel.add(Box.createRigidArea(new Dimension(0,15)));
                panel.add(questionHowDoIKnow);
                panel.add(answerSpyglass);
                panel.add(Box.createRigidArea(new Dimension(0,15)));
                panel.add(questionUpdate);
                panel.add(answerUpdate);

                panel.setBorder(new EmptyBorder(25,25,35,25));

                frame.getContentPane().add(BorderLayout.CENTER, panel);
                frame.pack();
                frame.setLocationByPlatform(true);
                frame.setVisible(true);
                frame.setResizable(false);
            }
        });
    }

    private void storeCustomFileRNG() {
        final JFileChooser fc = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("RELAX NG schema (.rng)", "rng");
        fc.setFileFilter(filter);
        int returnVal = fc.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String filename = file.getName();

            Pattern p = Pattern.compile("_TBX-.*_");   // the pattern to search for
            Matcher m = p.matcher(filename);

            if (m.find()) {
                String matchedString = m.group(1);
                String dialect_name = matchedString.substring(1, matchedString.length()-1);
                String path_to_new_resource = "TBXCheck/src/main/resources/xml/" + dialect_name + ".rng";
                File new_resource = new File(path_to_new_resource);
                try {
                    copyFile(file, new_resource);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                createFrameForError();
            }

        }
    }

    private void storeCustomFileSCH() {
        final JFileChooser fc = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Schematron schema (.sch)", "sch");
        fc.setFileFilter(filter);
        int returnVal = fc.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String filename = file.getName();

            Pattern p = Pattern.compile("_TBX-.*_");   // the pattern to search for
            Matcher m = p.matcher(filename);

            if (m.find()) {
                String matchedString = m.group(1);
                String dialect_name = matchedString.substring(1, matchedString.length()-1);
                String path_to_new_resource = "TBXCheck/src/main/resources/xml/" + dialect_name + ".sch";
                File new_resource = new File(path_to_new_resource);
                try {
                    copyFile(file, new_resource);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                // Print an error
                createFrameForError();
            }

        }
    }

    private static void copyFile(File sourceFile, File destFile) throws IOException {
        if(!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        }
        finally {
            if(source != null) {
                source.close();
            }
            if(destination != null) {
                destination.close();
            }
        }
    }

    private void createFrameForError() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("ERROR!");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                try
                {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                JPanel panel = new JPanel();
                JTextPane errorMessage = new JTextPane();
                errorMessage.setContentType("text/html"); // let the text pane know this is what you want
                errorMessage.setText("<html>Unable to find dialect name in file name. Please include the name of the " +
                        "dialect in your file name, surrounded by underscores. For example: " +
                        "\"my_file_TBX-Example_version_1.rng\"</html>");

                // Font
                setJTextPaneFont(errorMessage, new Font("", Font.BOLD, 20), Color.BLACK);

                errorMessage.setEditable(false); // as before
                errorMessage.setBackground(null); // this is the same as a JLabel
                errorMessage.setBorder(null); // remove the border

                panel.add(errorMessage);
                panel.setBorder(new EmptyBorder(25,25,35,25));

                frame.getContentPane().add(BorderLayout.CENTER, panel);
                frame.pack();
                frame.setLocationByPlatform(true);
                frame.setVisible(true);
                frame.setResizable(false);
            }
        });
    }

    private void createFrameForDownloadSuccess() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Sync Results");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                try
                {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                JPanel panel = new JPanel();
                JTextPane message = new JTextPane();
                message.setContentType("text/html"); // let the text pane know this is what you want
                message.setText("<html>Success!</html>");

                // Font
                setJTextPaneFont(message, new Font("", Font.BOLD, 20), Color.BLACK);

                message.setEditable(false); // as before
                message.setBackground(null); // this is the same as a JLabel
                message.setBorder(null); // remove the border

                panel.add(message);
                panel.setBorder(new EmptyBorder(25,25,35,25));

                frame.getContentPane().add(BorderLayout.CENTER, panel);
                frame.pack();
                frame.setLocationByPlatform(true);
                frame.setVisible(true);
                frame.setResizable(false);
            }
        });
    }

    private static void setJTextPaneFont(JTextPane jtp, Font font, Color c) {
        // Start with the current input attributes for the JTextPane. This
        // should ensure that we do not wipe out any existing attributes
        // (such as alignment or other paragraph attributes) currently
        // set on the text area.
        MutableAttributeSet attrs = jtp.getInputAttributes();

        // Set the font family, size, and style, based on properties of
        // the Font object. Note that JTextPane supports a number of
        // character attributes beyond those supported by the Font class.
        // For example, underline, strike-through, super- and sub-script.
        StyleConstants.setFontFamily(attrs, font.getFamily());
        StyleConstants.setFontSize(attrs, font.getSize());
        StyleConstants.setItalic(attrs, (font.getStyle() & Font.ITALIC) != 0);
        StyleConstants.setBold(attrs, (font.getStyle() & Font.BOLD) != 0);

        // Set the font color
        StyleConstants.setForeground(attrs, c);

        // Retrieve the pane's document object
        StyledDocument doc = jtp.getStyledDocument();

        // Replace the style for the entire document. We exceed the length
        // of the document by 1 so that text entered at the end of the
        // document uses the attributes.
        doc.setCharacterAttributes(0, doc.getLength() + 1, attrs, false);
    }

}
