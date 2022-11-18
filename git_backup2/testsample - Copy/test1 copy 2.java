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

/*
Ceate a Java archive:
jar cf ic.jar ic/*.*

run in new Java project:
java -classpath .;ic.jar test.java

*/

public class test1 extends JFrame {
    private static final long serialVersionUID = 1L;
    DShowLib _DShowLib;
    Grabber _grabber;
    FrameHandlerSink _sink;
    JButton _SelectDevice;
    JButton _DeviceProperties;
    JButton _StartVideo;
    JButton _StopVideo;
    JButton _SaveImage;
    Listener _Listener;
    JLabel _ExposureLabel;
    JSlider _ExposureSlider;
    JLabel _Exposurevalue;
    JCheckBox _ExposureAuto;
    JLabel _GainLabel;
    JSlider _GainSlider;
    JLabel _Gainvalue;
    JCheckBox _GainAuto;
    JCheckBox _TriggerMode;
    JButton _SoftwareTrigger;

    PictureBox _PictureBox;

    // Gain and Exposure are double values (Not for all, but most cameras.)
    // Therefore, an array with 100 values is created which is indexed by the
    // the according sliders. These sliders have a range from 0 to 99.
    private double[] _ExposureArray = new double[100]; // array for exposure values
    private double[] _GainArray = new double[100]; // array for Gain values

    public test1() {
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        createUserInterface();
        CreateICObjects();
    }

    /**
     * Create buttons, windows and sliders
     */
    private void createUserInterface() {
        int y = 5; // Used for "automatic" Y position of new elemtents

        _SelectDevice = new JButton("Select Device");
        _SelectDevice.setBounds(5, y, 120, 30);
        _DeviceProperties = new JButton("Device Properties");
        _DeviceProperties.setBounds(5, y += 40, 120, 30);

        _StartVideo = new JButton("Start Video");
        _StartVideo.setBounds(5, y += 40, 120, 30);
        _StopVideo = new JButton("Stop Video");
        _StopVideo.setBounds(5, y += 40, 120, 30);

        _SaveImage = new JButton("Start Server");
        _SaveImage.setBounds(5, y += 40, 120, 30);

        y += 40;
        _PictureBox = new PictureBox();
        _PictureBox.setBounds(130, 5, 320, y - 5);

        _ExposureLabel = new JLabel("Exposure");
        _ExposureLabel.setBounds(5, y, 100, 30);

        _ExposureSlider = new JSlider();
        _ExposureSlider.setBounds(105, y, 200, 30);
        _ExposureSlider.setMinimum(0);
        _ExposureSlider.setMaximum(99);

        // Listening to changes in the exposure slider
        ExpChangeListener lst = new ExpChangeListener();
        _ExposureSlider.addChangeListener(lst);
        lst.stateChanged(new ChangeEvent(_ExposureSlider));

        _Exposurevalue = new JLabel("0.00");
        _Exposurevalue.setBounds(310, y, 70, 30);

        _ExposureAuto = new JCheckBox("Auto");
        _ExposureAuto.setBounds(385, y, 70, 30);

        _GainLabel = new JLabel("Gain");
        _GainLabel.setBounds(5, y += 40, 100, 30);

        _GainSlider = new JSlider();
        _GainSlider.setBounds(105, y, 200, 30);
        _GainSlider.setMinimum(0);
        _GainSlider.setMaximum(99);

        // Listening to changes in the gain slider
        GainChangeListener Gainlst = new GainChangeListener();
        _GainSlider.addChangeListener(Gainlst);
        Gainlst.stateChanged(new ChangeEvent(_GainSlider));

        _Gainvalue = new JLabel("0.00");
        _Gainvalue.setBounds(310, y, 70, 30);

        _GainAuto = new JCheckBox("Auto");
        _GainAuto.setBounds(385, y, 70, 30);

        _TriggerMode = new JCheckBox("Trigger Mode");
        _TriggerMode.setBounds(5, y += 40, 100, 30);

        _SoftwareTrigger = new JButton("Software Trigger");
        _SoftwareTrigger.setBounds(110, y, 130, 30);
        _SoftwareTrigger.setEnabled(false);

        this.add(_ExposureLabel);
        this.add(_ExposureSlider);
        this.add(_Exposurevalue);
        this.add(_ExposureAuto);

        this.add(_GainLabel);
        this.add(_GainSlider);
        this.add(_Gainvalue);
        this.add(_GainAuto);

        //this.add(_TriggerMode);
        //this.add(_SoftwareTrigger);

        // this.add(_SelectDevice);
        // this.add(_DeviceProperties); //preset
        // this.add(_StartVideo);
        // this.add(_StopVideo);
        this.add(_SaveImage);

        this.add(_PictureBox);

        this.setSize(500, 480);
        this.setLayout(null);

        this.setVisible(true);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                // _grabber.stopLive();
                _DShowLib.ExitLibrary();
                System.exit(0);
            }
        });

        _SelectDevice.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (_grabber.isLive()) {
                    _grabber.stopLive();

                }
                _grabber.showDevicePage();

                _grabber.openDev("DFK 33GX183");

                if (_grabber.isDevValid()) {
                    if (_grabber.getProperties() > 0) { // Query the properties.
                        setupPropertyControls();
                    }
                    _grabber.saveDeviceStateToFile("device.xml");

                }
            }
        });

        _DeviceProperties.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (_grabber.isDevValid()) {
                    _grabber.showVCDPropertyPage();
                    _grabber.saveDeviceStateToFile("device.xml");
                }
            }
        });

        _StartVideo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (_grabber.isDevValid()) {
                    _grabber.startLive(false);
                }
            }
        });

        _StopVideo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _grabber.stopLive();
            }
        });

        // ######################################################################

        // Run this whenever we want an image

        // Server socket is created after we click SAVE IMAGE for the first time

        _SaveImage.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                System.out.println("\n _grabber.getproperties(): " + _grabber.getProperties());
                // System.out.println("\n _grabber.PropertyGet(exposure):
                // "+_grabber.PropertyGet("Exposure Auto"));

                // String gain = _grabber.PropertyGet("Gain").toString();

                // System.out.println("\n gain value: "+gain);

                // start video feed
                if (_grabber.isDevValid()) {
                    _grabber.startLive(false);
                }

                Thread t = new Thread() {
                    public void run() {

                        String file_prefix = null;

                        try {
                            // 1234 is the port
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

        });

        _TriggerMode.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                AbstractButton abstractButton = (AbstractButton) actionEvent.getSource();
                boolean selected = abstractButton.getModel().isSelected();
                try {
                    _grabber.PropertySet("Trigger Mode", selected);
                    _SoftwareTrigger.setEnabled(selected);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
        });

        _SoftwareTrigger.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    _grabber.PropertySet("Trigger_Software Trigger");
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }

                // Look into these and their use
                // cases.##################################################################################################
                System.out.println("Software Trigger");
                System.out.println("Available Properties:");

                for (String pn : _grabber.getAvailableProperties()) {
                    System.out.println((pn));
                }

            }
        });

        _ExposureAuto.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                AbstractButton abstractButton = (AbstractButton) actionEvent.getSource();
                boolean selected = abstractButton.getModel().isSelected();
                try {
                    _grabber.PropertySet("Exposure Auto", selected);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }

            }
        });

        _GainAuto.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                AbstractButton abstractButton = (AbstractButton) actionEvent.getSource();
                boolean selected = abstractButton.getModel().isSelected();
                try {
                    _grabber.PropertySet("Gain Auto", selected);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }

            }
        });
    }

    /**
     * Sample how to open a device an setup properties
     */
    private void CreateICObjects() {
        _DShowLib = new DShowLib();
        _DShowLib.InitLibrary();
        _grabber = new Grabber();
        _sink = new FrameHandlerSink(DShowLib.tColorformatEnum.eY16, 5);
        _grabber.setSinkType(_sink);

        _Listener = new Listener();
        _grabber.addListener(_Listener);
        _Listener._PictureBox = _PictureBox;

        _sink.setSnapMode(false);

        // Resize the output window to 640x480
        _grabber.setDefaultWindowPosition(false);
        _grabber.setWindowSize(1280, 1024);

        _grabber.loadDeviceStateFromFile("device.xml", true);

        if (_grabber.isDevValid()) {
            if (_grabber.getProperties() > 0) {
                /*
                 * System.out.println("Available Properties:");
                 * for(String pn : _grabber.getAvailableProperties()){
                 * System.out.println((pn));
                 * }
                 */
                setupPropertyControls();
            }

        } else {
            this.setTitle("No device opened!");
        }
    }

    /**
     * Setup the property controls for gain, exposure and trigger on the main
     * window.
     */
    private void setupPropertyControls() {
        try {

            _ExposureAuto.setSelected(_grabber.PropertyGet("Exposure Auto"));
            _GainAuto.setSelected(_grabber.PropertyGet("Gain Auto"));
            _TriggerMode.setSelected(_grabber.PropertyGet("Trigger Mode"));

            CreateExposureArray(_grabber.PropertyGetMin("Exposure"), _grabber.PropertyGetMax("Exposure"));
            SetExposureSliderValue(_grabber.PropertyGet("Exposure"));
            _Exposurevalue.setText(_grabber.PropertyGet("Exposure").toString());

            CreateGainArray(_grabber.PropertyGetMin("Gain"), _grabber.PropertyGetMax("Gain"));
            SetGainSliderValue(_grabber.PropertyGet("Gain"));
            _Gainvalue.setText(_grabber.PropertyGet("Gain").toString());

            // Test delete after
            System.out.println("[414] Gain " + _grabber.PropertyGet("Gain").toString());
            System.out.println("[390] Exposure " + _grabber.PropertyGet("Exposure").toString());

            System.out.println("[415] Device Name" + _grabber.PropertyGet("Device Name").toString());

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Create a logarithmic scale for exposure.
     * 
     * @param dMin Min Exposure
     * @param dMax Max Exposure.
     */
    private void CreateExposureArray(double dMin, double dMax) {
        double rangelen = Math.log(dMax) - Math.log(dMin);
        int num_samples = 100;

        for (int i = 0; i < num_samples; i++) {
            double val = Math.exp(Math.log(dMin) + rangelen / num_samples * i);

            if (val < dMin)
                val = dMin;
            if (val > dMax)
                val = dMax;
            _ExposureArray[i] = val;
        }
    }

    /**
     * Set the slider value to the index with nearest Exposure value
     * 
     * @param ExposureValue
     */
    private void SetExposureSliderValue(double ExposureValue) {
        int num_samples = 100;

        for (int i = 0; i < num_samples; i++) {
            if (ExposureValue >= _ExposureArray[i] && ExposureValue < _ExposureArray[i + 1]) {
                _ExposureSlider.setValue(i);
            }
        }
    }

    /**
     * Create a linear scale for Gain values
     * 
     * @param dMin Min Gain
     * @param dMax Max Gain
     */
    private void CreateGainArray(double dMin, double dMax) {
        double rangelen = dMax - dMin;
        int num_samples = 100;

        for (int i = 0; i < num_samples; i++) {
            double val = dMin + (rangelen / num_samples * i);

            if (val < dMin)
                val = dMin;
            if (val > dMax)
                val = dMax;
            _GainArray[i] = val;
        }
    }

    /**
     * Set the slider value to the index with nearest Gain value
     * 
     * @param GainValue
     */
    private void SetGainSliderValue(double GainValue) {
        int num_samples = 100;

        for (int i = 0; i < num_samples; i++) {
            if (GainValue >= _GainArray[i] && GainValue < _GainArray[i + 1]) {
                _GainSlider.setValue(i);
            }
        }
    }

    class ExpChangeListener implements ChangeListener {
        ExpChangeListener() {
        }

        public synchronized void stateChanged(ChangeEvent e) {
            if (_grabber != null) {
                try {
                    _grabber.PropertySet("Exposure", _ExposureArray[_ExposureSlider.getValue()]);
                    _Exposurevalue.setText(_grabber.PropertyGet("Exposure").toString());
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }
    }

    class GainChangeListener implements ChangeListener {
        GainChangeListener() {
        }

        public synchronized void stateChanged(ChangeEvent e) {
            if (_grabber != null) {
                try {
                    _grabber.PropertySet("Gain", _GainArray[_GainSlider.getValue()]);
                    _Gainvalue.setText(_grabber.PropertyGet("Gain").toString());
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }
    }

    // ############################################# MY METHODS
    // ##############################################################################

    /*
     * Captures the images and stores the image file
     */
    private void captureImage(String file_name) {

        String cam_snap_dir = "C:\\Users\\MelvinHartley\\OneDrive - Autonoma\\Desktop\\Industrial Camera\\camera snaps\\";

        if (_grabber.isDevValid()) {
            _sink.snapImages(1, 1000);
            ic.MemBuffer m = _sink.getLastAcqMemBuffer();
            m.save(cam_snap_dir + file_name);
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
        // String line = reader.readLine();

        // and also the stream of data going out
        OutputStream outputStream = socket.getOutputStream();
        // I'll send text data, so wrap this in a text writer
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream));

        // File name construction
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        LocalDateTime now = LocalDateTime.now();

        while (!quit) {
            sendMessage(writer, "awaiting command... ('h' for help)"); // this may not be the right spot for it

            // pause? int guess = Integer.parseInt(reader.readLine());
            String line = reader.readLine();

            // Message confirmation
            sendMessage(writer, "\nReceived: " + line);

            // Message Handling
            switch (line) {
                // Help option: Lists the available commands
                case "h":
                    sendMessage(writer, "|      Available Options      |");
                    sendMessage(writer, "a,b : sample types.");
                    sendMessage(writer, "p: a ping request to check server connectivity");
                    sendMessage(writer, "q: disable the connection with the server");
                    break;

                // Sample A
                case "a":

                    // File Name Construction
                    file_prefix = "smp_a_";
                    file_name = file_prefix + "_" + dtf.format(now) + ".png";

                    System.out.println("Message A: This is a class A sample, file prefix: " + file_prefix);
                    // capture the image
                    captureImage(file_name);
                    sendMessage(writer, "Image captured successfully");
                    sendMessage(writer, "File Name: " + file_name);

                    System.out.println("Image Captured Successfully");
                    break;

                // Sample B
                case "b":
                    file_prefix = "smp_b_";
                    file_name = file_prefix + "_" + dtf.format(now) + ".png";

                    System.out.println("Message B: This is a class B sample, file prefix: " + file_prefix);

                    captureImage(file_name);

                    sendMessage(writer, "Image captured successfully");
                    sendMessage(writer, "File Name: " + file_name);
                    System.out.println("Image Captured Successfully");
                    break;

                // Ping Request
                case "p": // client pings the server to ensure they are still connected
                    System.out.println("'PING' has been sent from the client.");
                    sendMessage(writer, "'PING': Connection is still stable with server.");
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

    private void sendMessage(PrintWriter writer, String message) {
        writer.println(message);
        writer.flush();
    }

    public static void main(String[] args) {
        // JOptionPane.showMessageDialog(null, "Connect debugger");
        new test1();
    }
}
