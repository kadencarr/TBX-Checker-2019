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
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.file.FileSystemException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.schematron.ISchematronResource;
import com.helger.schematron.pure.SchematronResourcePure;
import com.helger.schematron.svrl.SVRLMarshaller;
import com.helger.schematron.xslt.SchematronResourceSCH;
import com.helger.xml.serialize.read.DOMReader;
import com.thaiopensource.relaxng.jaxp.XMLSyntaxSchemaFactory;
import org.oclc.purl.dsdl.svrl.FailedAssert;
import org.oclc.purl.dsdl.svrl.SchematronOutputType;
import org.ttt.salt.Configuration;
import org.ttt.salt.TBXFile;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import static java.lang.System.exit;

/**
 *
 * @author Lance Finn Helsten
 * @version $Id$
 * @license Licensed under the Apache License, Version 2.0.
 */
public class ActionOpen extends TBXAbstractAction implements FilenameFilter
{
    private static final Logger LOGGER = Logger.getLogger( ActionOpen.class.getName() );
    
    /**
     * Format log messages in a manner that is easier for non-programmer
     * (i.e. normal humans) to read.
     *
     * @author Lance Finn Helsten
     * @version $Id$
     * @license Licensed under the Apache License, Version 2.0.
     */
    private class LogFormatter extends Formatter
    {
        /** Builder to use on each log record. */
        private StringBuilder builder = new StringBuilder();
        
        /** {@inheritDoc} */
        public String format(LogRecord record)
        {
            String errmsg = "";
            String fmt;
            if (record.getThrown() != null)
            {
                fmt = "#%3$2d  %4$s---[%1$s#%2$s] %5$s%n\tException: %6$s%n";
                errmsg = record.getThrown().getLocalizedMessage();
            }
            else if (record.getLevel().intValue() > Level.FINE.intValue())
            {
                fmt = "#%3$2d  %4$s---%5$s%n";
            }
            else
            {
                fmt = "#%3$2d  %4$s---[%1$s#%2$s] %5$s%n";
            }
            return String.format(fmt,
                    record.getSourceClassName(), record.getSourceMethodName(),
                    record.getSequenceNumber(),
                    record.getLevel().getLocalizedName(),
                    formatMessage(record), errmsg);
        }
    }

    /** SCM information. */
    public static final String RCSID = "$Id$";
    
    /** Preferences key for the last directory the user opened a file from. */
    private static final String DIRECTORY = "ActionOpen_Directory";
        
    /** Formatter for the log messages. */
    private Formatter formatter;
    
    /** Handler for log messages. */
    private Handler handler;
    
    /** Level for the logging. */
    private Level level;
    
    /** Flag for xml:lang validation. */
    private boolean checkXmlLang = true;
    
    /** Buffer for log messages. */
    private ByteArrayOutputStream logbuffer;
    
    /**
     */
    public ActionOpen()
    {
        super("ActionOpen");
        try
        {
            logbuffer = new ByteArrayOutputStream();
            //formatter = new java.util.logging.SimpleFormatter();
            formatter = new LogFormatter();
            handler = new java.util.logging.StreamHandler(logbuffer, formatter);
            handler.setLevel(Level.FINEST);
            handler.setEncoding("UTF-8");
        }
        catch (java.io.UnsupportedEncodingException err)
        {   //Ignore: UTF-8 is always available
            err.printStackTrace();
        }
    }

    /** {@inheritDoc} */
    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("ActionOpen"))
        {
            FileDialog d = new FileDialog(JOptionPane.getRootFrame(),
                                getResourceBundle().getString("ActionOpenFileDialogTitle"),
                                FileDialog.LOAD);
            Preferences pref = Preferences.userNodeForPackage(getClass());
            d.setDirectory(pref.get(DIRECTORY, System.getProperty("user.home")));
            d.setFilenameFilter(this);
            d.setVisible(true);
            pref.put(DIRECTORY, d.getDirectory());
            if (d.getFile() != null)
            {
                File file = new File(d.getDirectory(), d.getFile());
                Object[] args = {file};
                if (!file.exists())
                {
                    JOptionPane.showMessageDialog(null,
                        MessageFormat.format(getResourceBundle().getString("PathMissing"), args),
                        getResourceBundle().getString("PathMissingTitle"),
                        JOptionPane.ERROR_MESSAGE);
                }
                else if (!file.isFile())
                {
                    JOptionPane.showMessageDialog(null,
                        MessageFormat.format(getResourceBundle().getString("PathNotNormalFile"), args),
                        getResourceBundle().getString("PathNotNormalFileTitle"),
                        JOptionPane.ERROR_MESSAGE);
                }
                else if (!file.canRead())
                {
                    JOptionPane.showMessageDialog(null,
                        MessageFormat.format(getResourceBundle().getString("PathSecurityViolation"), args),
                        getResourceBundle().getString("PathSecurityViolationTitle"),
                        JOptionPane.ERROR_MESSAGE);
                }
                else
                {
                    logbuffer.reset();
                    Logger.getLogger("org.ttt.salt").addHandler(handler);
                    Logger.getLogger("org.ttt.salt").setLevel(level);
                    Logger.getLogger("org.ttt.salt.dom.tbx").setLevel(level);
                    Logger.getLogger("org.ttt.salt.dom.xcs").setLevel(level);
                    
                    TBXFile dv = null;
                    try
                    {
                        if (Singleton.getSingletonInstance().getVersionSelection() == 0) {
                            Configuration config = new Configuration();
                            config.setCheckLang(checkXmlLang);

                            dv = new TBXFile(file.toURI().toURL(), config);
                            dv.parseAndValidate();
                            if (dv.isValid())
                            {
                                JOptionPane.showMessageDialog(null,
                                    MessageFormat.format(getResourceBundle().getString("FileValid"), args),
                                    getResourceBundle().getString("FileValidTitle"),
                                    JOptionPane.INFORMATION_MESSAGE);
                            }
                        } else if (Singleton.getSingletonInstance().getVersionSelection() == 1) {
                            // TODO: Validate with Xerxes for TBX V3
                            xercesValidation(file);
                        }
                    }
                    catch (IOException err)
                    {
                        String msg = MessageFormat.format(
                                getResourceBundle().getString("IOException"),
                                err.getLocalizedMessage(), file);
                        JOptionPane.showMessageDialog(null, msg,
                            getResourceBundle().getString("IOExceptionTitle"),
                            JOptionPane.ERROR_MESSAGE);
                    }
                    //CHECKSTYLE: IllegalCatch OFF
                    catch (Throwable err)
                    {
                        String msg = MessageFormat.format(
                                getResourceBundle().getString("UnknownError"),
                                err.getLocalizedMessage(), file);
                        JOptionPane.showMessageDialog(null, msg,
                            getResourceBundle().getString("UnknownErrorTitle"),
                            JOptionPane.ERROR_MESSAGE);
                        System.err.format("Unknown error for file %s%n", file);
                        err.printStackTrace();
                    }
                    //CHECKSTYLE: IllegalCatch ON
                    finally
                    {
                        try
                        {
                            handler.flush();
                            String log = logbuffer.toString("UTF-8");
                            Logger.getLogger("org.ttt.salt").removeHandler(handler);
                            new TBXResults(file, dv, log);
                        }
                        catch (java.io.UnsupportedEncodingException err)
                        {   //Ignore: UTF-8 is always available
                            err.printStackTrace();
                        }
                    }
                }
            }
        }
        else if (e.getActionCommand().equals("NoLangCheckChanged"))
        {
            JCheckBox check = (JCheckBox) e.getSource();
            checkXmlLang = !check.isSelected();
        }
        else if (e.getActionCommand().equals("comboBoxChanged"))
        {
            try
            {
                JComboBox cbox = (JComboBox) e.getSource();
                String selstr = (String) cbox.getSelectedItem();
                level = Level.parse(selstr);
            }
            catch (NullPointerException err)
            {   //Invalid level string so don't change anything
                err.printStackTrace();
            }
            catch (IllegalArgumentException err)
            {   //Invalid level string so don't change anything
                err.printStackTrace();
            }
        }
    }
    
    /** {@inheritDoc} */
    public boolean accept(File dir, String name)
    {
        return name.matches(".+?\\.((xml)|(XML)|(tbx)|(TBX))");
    }



    private void xercesValidation(File file) {
        try {
            // RNG Validation
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat dateFormatFull = new SimpleDateFormat("HH:mm:ss - yyyy-MM-dd");
            String fullFilePath = dateFormat.format(date) + "_TBXChecker-Results";

            int num = 0;
            File fileIncr = new File(fullFilePath + ".txt");
            String temp;
            while(fileIncr.exists()) {
                temp = fullFilePath + "-" + (num++);
                fileIncr = new File(temp + ".txt");
            }

            PrintWriter printWriter = new PrintWriter(fileIncr);
            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("TBX-Checker 2019\n");
            stringBuilder.append("Validation Results\n");
            stringBuilder.append(dateFormatFull.format(date));
            stringBuilder.append("\nFile saved to: " + System.getProperty("user.dir"));
            stringBuilder.append("\n\n");

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document doc = documentBuilder.parse(file);

            System.setProperty(SchemaFactory.class.getName() + ":" + XMLConstants.RELAXNG_NS_URI,
                    XMLSyntaxSchemaFactory.class.getName());

            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.RELAXNG_NS_URI);

            // Define the Schema from URL or local file?
            // Assuming offline use, we will include the file locally

            String rngPath = "";
            String schPath = "";
            String dialectMessage = "";
            String checkRNGPath = "";
            String checkSCHPath = "";
            int found_dialect = 0;

            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String lineFromFile = scanner.nextLine();
                if(lineFromFile.contains("type=\"TBX-Core\"")) {
                    rngPath = "TBXCheck/src/main/resources/xml/TBX-Core_dialect-master/Schemas/TBXcoreStructV03_TBX-Core_integrated.rng";
                    schPath = "TBXCheck/src/main/resources/xml/TBX-Core_dialect-master/Schemas/TBX-Core.sch";
                    dialectMessage = "The dialect of your file was automatically detected to be TBX-Core.";
                    found_dialect = 1;
                    break;
                }
                else if(lineFromFile.contains("type=\"TBX-Min\"") && lineFromFile.contains("dca")) {
                    rngPath = "TBXCheck/src/main/resources/xml/TBX-Min_dialect-master/DCA/TBXcoreStructV03_TBX-Min_integrated.rng";
                    schPath = "TBXCheck/src/main/resources/xml/TBX-Min_dialect-master/DCA/TBX-Min_DCA.sch";
                    dialectMessage = "The dialect of your file was automatically detected to be TBX-Min.";
                    found_dialect = 1;
                    break;
                }
                else if(lineFromFile.contains("type=\"TBX-Basic\"") && lineFromFile.contains("dca")) {
                    rngPath = "TBXCheck/src/main/resources/xml/TBX-Basic_dialect-master/DCA/TBXcoreStructV03_TBX-Basic_integrated.rng";
                    schPath = "TBXCheck/src/main/resources/xml/TBX-Basic_dialect-master/DCA/TBX-Basic_DCA.sch";
                    dialectMessage = "The dialect of your file was automatically detected to be TBX-Basic.";
                    found_dialect = 1;
                    break;
                }
                else if (lineFromFile.contains("type=\"TBX-\"")) {
                    Pattern p = Pattern.compile("TBX-.*\"");   // the pattern to search for
                    Matcher m = p.matcher(lineFromFile);

                    if (m.find()) {
                        String detected_dialect = m.group(1);
                        detected_dialect = detected_dialect.substring(0, detected_dialect.length() - 1);
                        checkRNGPath = "TBXCheck/src/main/resources/xml/External_Schemas/" + detected_dialect + ".rng";
                        checkSCHPath = "TBXCheck/src/main/resources/xml/External_Schemas/" + detected_dialect + ".sch";

                        File rng_test = new File(checkRNGPath);
                        File sch_test = new File(checkSCHPath);

                        if (rng_test.exists() && sch_test.exists()) {
                            dialectMessage = "The dialect of your file was automatically " +
                                    "detected to be the custom dialect " + detected_dialect + ".";
                            found_dialect = 1;
                            break;
                        }

                    }
                    else {
                        break;
                    }
                }
            }

            if (found_dialect == 0) {
                // Open error window
                createFrameForError();
            }

            File rng = new File(rngPath);
            File sch = new File(schPath);

            StreamSource streamSource = new StreamSource(file);

            stringBuilder.append("Current rng validation file path:\n");
            stringBuilder.append(rngPath);
            stringBuilder.append("\nCurrent sch validation file path:\n");
            stringBuilder.append(schPath);
            stringBuilder.append("\n\n\n");

            stringBuilder.append(dialectMessage);
            stringBuilder.append("\n\n\n");

            Schema schema;
            if (rng.exists() && rng.isFile() && rng.canRead()) {
                Source schemaFile = new StreamSource(rng);
                schema = schemaFactory.newSchema(schemaFile);
            }
            else {
                throw new FileNotFoundException();
            }

            // RNG Validation
            Validator validator = schema.newValidator();
            final List<SAXParseException> exceptions = new LinkedList<SAXParseException>();
            validator.setErrorHandler(new ErrorHandler()
            {
                @Override
                public void warning(SAXParseException exception) throws SAXException
                {
                    exceptions.add(exception);
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException
                {
                    exceptions.add(exception);
                }

                @Override
                public void error(SAXParseException exception) throws SAXException
                {
                    exceptions.add(exception);
                }
            });

            try {
                validator.validate(streamSource);
            } catch (SAXException e) {
                // TODO: Print to the Log any exceptions that arise from invalid files
                System.out.println("Invalid! Unsuccessful validation against the RNG file.");
                e.printStackTrace();
                System.out.println(e.getMessage());
                System.out.println("SAXException not handled!!!!");
            }

            if (exceptions.isEmpty()) {
                System.out.println("Valid! Successful validation against the RNG file.");
                stringBuilder.append("Valid! Successful validation against the RNG file.\n");
            }
            else {
                stringBuilder.append("Invalid! Unsuccessful validation against the RNG file. See errors below:\n\n");
                stringBuilder.append("(Line:Column)\n");
                for (SAXParseException e : exceptions) {
                    String message;
                    int lineNumber = e.getLineNumber();
                    int colNumber = e.getColumnNumber();
                    message = "\t(" + lineNumber + ":" + colNumber + ") " + e.getMessage() + "\n\n";
                    stringBuilder.append(message);
                }
                stringBuilder.append("\n\n");
            }

            //Schematron Validation
            try {
                String results = validateXMLViaXLSTSchematron(sch, file);
                System.out.println(results);
                stringBuilder.append(results);
            } catch (Exception e) {
                e.printStackTrace();
            }

            printWriter.printf("%s", stringBuilder.toString());
            printWriter.close();

            // Open for the user
            java.awt.Desktop.getDesktop().edit(fileIncr);

            exit(0);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String validateXMLViaXLSTSchematron (@Nonnull final File aSchematronFile, @Nonnull final File aXMLFile) throws Exception
    {
        StringBuilder results = new StringBuilder();

        final ISchematronResource aResPure = SchematronResourcePure.fromFile(aSchematronFile);
        if (!aResPure.isValidSchematron())
            throw new IllegalArgumentException ("Invalid Schematron!");

        final IReadableResource anXMLSource = new FileSystemResource(aXMLFile);

        Document aXMLNode = DOMReader.readXMLDOM(aXMLFile);
        if (aXMLNode == null) {
            throw new FileSystemException("Unable to read from file!");
        }
        SchematronOutputType returnedSVRL = aResPure.applySchematronValidationToSVRL(anXMLSource);

        List<Object> resultCollection = returnedSVRL.getActivePatternAndFiredRuleAndFailedAssert();

        boolean failed_flag = false;

        for (Object o : resultCollection) {
            if (o instanceof FailedAssert) {
                failed_flag = true;
            }
        }

        if (failed_flag) {
            results.append("Invalid! Unsuccessful validation against the SCH file.\n");
            for (Object o : resultCollection) {
                if (o instanceof FailedAssert) {
                    FailedAssert failedAssert = (FailedAssert)o;
                    String error = failedAssert.getText();
                    System.out.println(error);
                    results.append(error);
                }
            }
        } else {
            results.append("Valid! Successful validation against the SCH file.\n");
        }

        return results.toString();
    }

    private void createFrameForError() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("ERROR!");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                try
                {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                JPanel panel = new JPanel();
                JTextPane errorMessage = new JTextPane();
                errorMessage.setContentType("text/html"); // let the text pane know this is what you want
                errorMessage.setText("<html>Unable to locate validation files for the provided TBX file. " +
                        "If you are using a custom dialect, be sure to upload your own validation files.</html>");
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


}
