import java.awt.event.*;
import java.awt.*;
import java.awt.Graphics;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import ic.*;
import java.lang.reflect.*;
import java.util.*;
import java.time.*;
import java.time.format.*;
import java.net.*;
import java.io.*;

//XML reading
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class test1 extends JFrame {
    private static final long serialVersionUID = 1L;
    DShowLib _DShowLib;
    Grabber _grabber;
    FrameHandlerSink _sink;

    Listener _Listener;

    PictureBox _PictureBox;

    private String prev_file;
    private Integer dup_count;

    public test1() {
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        createUserInterface(); // eventually get rid of this

        prev_file = "";
        dup_count = 0;

        try {
            String hello = decodeXMLValue("boo");
        } catch (Exception e) {
            // do something, e.g. print e.getMessage()
        }

        CreateICObjects();
    }

    /**
     * Create picture box
     */
    private void createUserInterface() {
        int y = 205; // Used for "automatic" Y position of new elemtents

        _PictureBox = new PictureBox();
        _PictureBox.setBounds(130, 5, 320, y - 5);

        this.add(_PictureBox);

        this.setSize(500, 480);
        this.setLayout(null);

        this.setVisible(true);

        // If the window is closed
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                // _grabber.stopLive();
                _DShowLib.ExitLibrary();
                System.exit(0);
            }
        });
    }

    /**
     * Start the camera and server
     */
    private void CreateICObjects() {
        // immediately run everything from the get go

        _DShowLib = new DShowLib();
        _DShowLib.InitLibrary();
        _grabber = new Grabber();
        _sink = new FrameHandlerSink(DShowLib.tColorformatEnum.eY16, 5);
        _grabber.setSinkType(_sink);

        _Listener = new Listener();
        _grabber.addListener(_Listener);
        _Listener._PictureBox = _PictureBox;

        _sink.setSnapMode(false);

        // Resize the output window to 1280x1024
        _grabber.setDefaultWindowPosition(false);
        _grabber.setWindowSize(1280, 1024);
        _grabber.loadDeviceStateFromFile("class/device.xml", true);

        // _grabber.openDev("DFK 33GX183"); //Not necessary if specified in the xml file

        if (_grabber.isDevValid()) {
            if (_grabber.getProperties() > 0) {
                printInitialStatus();
            }

        } else {
            this.setTitle("No device opened!");
        }

        // Run the server
        startSequence();
    }

    /**
     * Print out the intitial status conditions
     * Not very necessary
     */
    private void printInitialStatus() {
        try {

            // Test delete after

            String dev_name = decodeXMLValue("/device_state/device/@name");
            String dev_res = decodeXMLValue("/device_state/device/videoformat");

            System.out.println("[414] Gain " + _grabber.PropertyGet("Gain").toString());
            System.out.println("[390] Exposure " + _grabber.PropertyGet("Exposure").toString());
            System.out.println("Device Name: " + dev_name + "; Device Resolution: " + dev_res);

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    /*
     * Read the XML file and return the correct parameter
     * 
     * @param path_to_element
     */
    private String decodeXMLValue(String path_to_element) {
        System.out.println("[120] path_to_element: " + path_to_element);

        try {
            InputStream is = getClass().getResourceAsStream("device.xml");
            DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = xmlFactory.newDocumentBuilder();
            Document xmlDoc = docBuilder.parse(is);
            XPathFactory xpathFact = XPathFactory.newInstance();
            XPath xpath = xpathFact.newXPath();

            String val = (String) xpath.evaluate(path_to_element, xmlDoc, XPathConstants.STRING);
            return val;

        } catch (Exception e) {
            // blah exception
            System.out.println("\nXML Reading ERROR");
        }

        return "";
    }

    /*
     * Captures the images and stores the image file
     */
    private void captureImage(String file_name) {

        // Needs to be adjusted when installed
        String cam_snap_dir = "C:\\Users\\MelvinHartley\\OneDrive - Autonoma\\Desktop\\Industrial Camera\\camera snaps\\";

        if (_grabber.isDevValid()) {
            _sink.snapImages(1, 1000);
            ic.MemBuffer m = _sink.getLastAcqMemBuffer();
            m.save(cam_snap_dir + file_name);
        }
    }

    /*
     * Returns an integer suffix if there is a duplicate file name.
     * There will be an issue if they go a a a a a and then b then a a a a all
     * within a second. Please just don't do that.
     */
    private int checkDupFiles(String filename) {
        // we have dup_count
        // we have prev_file

        String dc = Integer.toString(dup_count) + "_";

        System.out.println("\nfilename: " + filename + "; prev_file: " + prev_file + "; dc+prev: " + dc + prev_file);

        // if ("blah" == "blah") {
        // System.out.println("double blahs");
        // }

        // testing string lengths
        int f = filename.length();
        int p = prev_file.length();

        System.out.println("f: " + f + " p: " + p);

        // if (filename == prev_file) {
        // System.out.print("FOO we found a dup.");
        // }
        // if (filename.equals(prev_file)) {
        // System.out.print(".equals FOO we found a dup.");
        // }(filename.equals(dc + prev_file)) ||

        if (filename.equals(prev_file)) {
            System.out.println("dupcount: " + dup_count);
            return dup_count++;

        } else {
            dup_count = 0;
            System.out.println("Else dupcount: " + dup_count);

            return 0;
        }

    }

    /*
     * Handles communication from the server and returns messages back accordingly
     */
    private boolean handleServerComms(Socket socket) throws IOException {
        boolean quit = false;
        String file_prefix = null;
        String file_name = null;

        // get the stream of data coming in
        InputStream inputStream = socket.getInputStream();
        // I want text data, so wrap this in a text reader:
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        // and also the stream of data going out
        OutputStream outputStream = socket.getOutputStream();
        // I'll send text data, so wrap this in a text writer
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream));

        // File name construction
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

        while (!quit) {
            sendMessage(writer, "\nAwaiting command... ('h' for help)"); // this may not be the right spot for it

            // pause? int guess = Integer.parseInt(reader.readLine());
            String l = reader.readLine();
            String line = l.toLowerCase(); // Standardise input to all lower case

            // Message confirmation
            sendMessage(writer, "\nServer Received: " + line);

            // Message Handling
            switch (line) {
                // Help option: Lists the available commands
                case "h":
                    sendMessage(writer, "|      Available Options      |");
                    sendMessage(writer, "a,b : sample types.");
                    sendMessage(writer, "s: status of camera");
                    sendMessage(writer, "p: a ping request to check server connectivity");
                    sendMessage(writer, "q: disable the connection with the server");

                    break;

                // Sample A
                case "a":
                    // Get the current localdattime data as of now
                    LocalDateTime now_a = LocalDateTime.now();

                    // File Name Construction
                    file_prefix = "smp_a_";
                    file_name = file_prefix + "_" + dtf.format(now_a) + ".png";

                    // Consider Duplicates i.e. Photos Taken In Quick Succession
                    Integer c_a = checkDupFiles(file_name);
                    if (c_a != 0) {
                        String dup_prefix = Integer.toString(c_a) + "_";

                        prev_file = file_name; // we compare the un prefixed variants
                        file_name = dup_prefix + file_name;
                    } else {
                        prev_file = file_name; // unsuffixed
                    }

                    System.out.println("Message A: This is a class A sample, file prefix: " + file_prefix);
                    // capture the image
                    captureImage(file_name);
                    sendMessage(writer, "Image captured successfully");
                    sendMessage(writer, "File Name: " + file_name);
                    System.out.println("Image Captured Successfully");
                    break;

                // Sample B
                case "b":
                    // Get the current localdattime data as of now
                    LocalDateTime now_b = LocalDateTime.now();

                    // File Name Construction
                    file_prefix = "smp_b_";
                    file_name = file_prefix + "_" + dtf.format(now_b) + ".png";

                    // Consider Duplicates i.e. Photos Taken In Quick Succession
                    Integer c_b = checkDupFiles(file_name);
                    if (c_b != 0) {
                        String dup_prefix = Integer.toString(c_b) + "_";

                        prev_file = file_name; // we compare the un prefixed variants
                        file_name = dup_prefix + file_name;
                    } else {
                        prev_file = file_name; // unsuffixed
                    }

                    System.out.println("Message B: This is a class B sample, file prefix: " + file_prefix);
                    captureImage(file_name);
                    sendMessage(writer, "Image captured successfully");
                    sendMessage(writer, "File Name: " + file_name);
                    System.out.println("Image Captured Successfully");
                    break;

                // Return info about the camera being used
                case "s":
                    String dev_name = decodeXMLValue("/device_state/device/@name");
                    String dev_res = decodeXMLValue("/device_state/device/videoformat");
                    String mode = "man"; // on condition if this feature is introduced.
                    String msg = "@STATUS=< " + mode + " > @Dev: " + dev_name + " @Res: " + dev_res;
                    sendMessage(writer, msg);
                    break;

                // Ping Request
                case "p": // client pings the server to ensure they are still connected
                    System.out.println("Client: ARE_YOU_THERE?");
                    sendMessage(writer, "Server: I_AM_HERE");
                    break;

                // Drop Connection Request
                case "q": // We wish to cut the connection
                    System.out.println("Client is cutting the connection.");
                    sendMessage(writer, "Connection successfully cut with server.");
                    quit = true; // we wish to exit the loop
                    break;

                default:
                    System.out.println("Invalid state code");
                    sendMessage(writer, "Invalid State Code Error From Server");

            }

        }
        // close the stream and connection.
        reader.close();
        socket.close();
        return quit;
    }

    /**
     * Send a message
     * 
     * @param writer
     * @param message
     */
    private void sendMessage(PrintWriter writer, String message) {
        writer.println(message);
        writer.flush();
    }

    /**
     * Initialise the server side communication
     * 
     */
    private void startSequence() {

        // start video feed
        if (_grabber.isDevValid()) {
            _grabber.startLive(false);
        }

        Thread t = new Thread() {
            public void run() {

                String old_file_name = null;

                try {
                    int port = 4321;
                    ServerSocket serverSocket = new ServerSocket(port);
                    System.out.println("Server started on port: " + port);

                    // keep accepting connections until done
                    boolean[] done = { false };
                    while (!done[0]) {
                        // accept a conection

                        Socket socket = serverSocket.accept();
                        // the client socket, we will use this to send messages

                        // give the socket to some code that can process it:
                        Thread connectionHandlerThread = new Thread() {
                            public void run() {
                                try {

                                    boolean quit = handleServerComms(socket);
                                    // this is ugly, but it will kind of work
                                    done[0] = quit; // true if it is time to quit

                                    socket.close();
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                }
                            }
                        };
                        connectionHandlerThread.start();
                    }
                    serverSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    // lalala
                }
            }
        };
        t.start();
    }

    public static void main(String[] args) {
        // JOptionPane.showMessageDialog(null, "Connect debugger");
        new test1();
    }
}
