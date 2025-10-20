package app;

import com.fazecast.jSerialComm.SerialPort;
import com.google.zxing.WriterException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MainWindow extends JFrame {
    private static final int MAX_DISPLAY_ROWS = 500;

    private final Database database;
    private final UserSettings settings;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextArea rawArea;
    private final JTextArea normalizedArea;
    private final DefaultTableModel parsedModel;
    private final JTextArea warningsArea;
    private final JLabel parseStatusLabel;
    private final JLabel metadataLabel;
    private final JButton previewButton;
    private final JButton printButton;
    private final JButton copyRawButton;
    private final JButton copyNormalizedButton;
    private final JButton copyHriButton;
    private final JTextField hidInputField;
    private final JCheckBox autoSaveCheckBox;
    private final JCheckBox focusLockCheckBox;
    private final JLabel serialStatusLabel;
    private final JLabel serialConfigLabel;
    private final JComboBox<String> portComboBox;
    private final JButton connectButton;
    private final JButton disconnectButton;
    private final DefaultListModel<GS1Element> manualModel;
    private final JLabel statusBar;

    private SerialPort activeSerialPort;
    private Thread serialThread;
    private volatile boolean serialRunning;

    private List<Scan> currentScans = new ArrayList<>();
    private Scan selectedScan;
    private ParseResult selectedParseResult;
    private List<GS1Element> selectedElements = new ArrayList<>();
    private String selectedNormalized;

    private String lastHidNormalized;
    private long lastHidTimestamp;
    private String lastSerialNormalized;
    private long lastSerialTimestamp;

    public MainWindow(Database database) {
        this.database = database;
        this.settings = UserSettings.load();

        setTitle("GS1Desk");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 700));

        tableModel = new DefaultTableModel(new Object[]{"ID", "Created", "Source", "Status", "Raw"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new TableSelectionHandler());

        rawArea = new JTextArea();
        rawArea.setEditable(false);
        rawArea.setLineWrap(true);
        rawArea.setWrapStyleWord(true);
        rawArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        normalizedArea = new JTextArea();
        normalizedArea.setEditable(false);
        normalizedArea.setLineWrap(true);
        normalizedArea.setWrapStyleWord(true);
        normalizedArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        parsedModel = new DefaultTableModel(new Object[]{"AI", "Name", "Value", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        warningsArea = new JTextArea();
        warningsArea.setEditable(false);
        warningsArea.setLineWrap(true);
        warningsArea.setWrapStyleWord(true);
        warningsArea.setRows(6);

        parseStatusLabel = new JLabel(" ");
        metadataLabel = new JLabel(" ");

        previewButton = new JButton("Preview");
        previewButton.setEnabled(false);
        previewButton.addActionListener(e -> previewSelected());

        printButton = new JButton("Print");
        printButton.setEnabled(false);
        printButton.addActionListener(e -> printSelected());

        copyRawButton = new JButton("Copy Raw");
        copyRawButton.setEnabled(false);
        copyRawButton.addActionListener(e -> copyRaw());

        copyNormalizedButton = new JButton("Copy Normalized");
        copyNormalizedButton.setEnabled(false);
        copyNormalizedButton.addActionListener(e -> copyNormalized());

        copyHriButton = new JButton("Copy HRI");
        copyHriButton.setEnabled(false);
        copyHriButton.addActionListener(e -> copyHri());

        hidInputField = new JTextField();
        autoSaveCheckBox = new JCheckBox("Auto-save on Enter", settings.isAutoSaveOnEnter());
        autoSaveCheckBox.addActionListener(e -> {
            settings.setAutoSaveOnEnter(autoSaveCheckBox.isSelected());
            settings.save();
        });
        focusLockCheckBox = new JCheckBox("Focus lock (F9)", settings.isFocusLock());
        focusLockCheckBox.addActionListener(e -> toggleFocusLock());

        serialStatusLabel = new JLabel("Not connected");
        serialConfigLabel = new JLabel();
        portComboBox = new JComboBox<>();
        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connectSerial());
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);
        disconnectButton.addActionListener(e -> disconnectSerial());

        manualModel = new DefaultListModel<>();
        statusBar = new JLabel("Ready");
        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JPanel leftPanel = buildLeftPanel();
        JPanel rightPanel = buildRightPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.33);
        add(splitPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        initMenu();
        applySettings();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                disconnectSerial();
            }
        });

        SwingUtilities.invokeLater(() -> hidInputField.requestFocusInWindow());
        refreshPorts();
        loadScans();
    }

    private void initMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem exportCsv = new JMenuItem("Export CSV...");
        exportCsv.addActionListener(e -> exportCsv());
        JMenuItem exportJson = new JMenuItem("Export JSON...");
        exportJson.addActionListener(e -> exportJson());
        JMenuItem importNormalized = new JMenuItem("Import normalized...");
        importNormalized.addActionListener(e -> importNormalized());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> dispatchEvent(new java.awt.event.WindowEvent(this, java.awt.event.WindowEvent.WINDOW_CLOSING)));

        fileMenu.add(exportCsv);
        fileMenu.add(exportJson);
        fileMenu.add(importNormalized);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem preferencesItem = new JMenuItem("Preferences...");
        preferencesItem.addActionListener(e -> openSettings());
        settingsMenu.add(preferencesItem);

        menuBar.add(fileMenu);
        menuBar.add(settingsMenu);
        setJMenuBar(menuBar);
    }

    private JPanel buildLeftPanel() {
        JPanel hidPanel = buildHidPanel();
        JPanel serialPanel = buildSerialPanel();

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("HID Scanner", hidPanel);
        tabbedPane.addTab("Serial Scanner", serialPanel);

        JPanel manualPanel = buildManualPanel();

        JPanel container = new JPanel(new BorderLayout());
        container.add(tabbedPane, BorderLayout.CENTER);
        container.add(manualPanel, BorderLayout.SOUTH);
        container.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return container;
    }

    private JPanel buildHidPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("Scan input"), gbc);

        gbc.gridy++;
        panel.add(hidInputField, gbc);

        hidInputField.addActionListener(e -> {
            if (autoSaveCheckBox.isSelected()) {
                saveHidInput();
            }
        });

        hidInputField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (focusLockCheckBox.isSelected()) {
                    SwingUtilities.invokeLater(() -> hidInputField.requestFocusInWindow());
                }
            }
        });

        hidInputField.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), "toggleFocusLock");
        hidInputField.getActionMap().put("toggleFocusLock", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                focusLockCheckBox.setSelected(!focusLockCheckBox.isSelected());
                toggleFocusLock();
            }
        });

        gbc.gridy++;
        panel.add(autoSaveCheckBox, gbc);

        gbc.gridy++;
        panel.add(focusLockCheckBox, gbc);

        gbc.gridy++;
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveHidInput());
        panel.add(saveButton, gbc);

        return panel;
    }

    private JPanel buildSerialPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Port"), gbc);

        gbc.gridy++;
        panel.add(portComboBox, gbc);

        gbc.gridy++;
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshPorts());
        panel.add(refreshButton, gbc);

        gbc.gridy++;
        panel.add(serialConfigLabel, gbc);

        gbc.gridy++;
        panel.add(connectButton, gbc);

        gbc.gridy++;
        panel.add(disconnectButton, gbc);

        gbc.gridy++;
        serialStatusLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        panel.add(serialStatusLabel, gbc);

        return panel;
    }

    private JPanel buildManualPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Manual Encode"));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField aiField = new JTextField(6);
        JTextField valueField = new JTextField(15);
        JButton addButton = new JButton("Add");
        inputPanel.add(new JLabel("AI:"));
        inputPanel.add(aiField);
        inputPanel.add(new JLabel("Value:"));
        inputPanel.add(valueField);
        inputPanel.add(addButton);

        JList<GS1Element> list = new JList<>(manualModel);
        list.setVisibleRowCount(6);
        list.setCellRenderer((jList, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value.toString());
            label.setOpaque(true);
            if (isSelected) {
                label.setBackground(jList.getSelectionBackground());
                label.setForeground(jList.getSelectionForeground());
            } else {
                label.setBackground(jList.getBackground());
                label.setForeground(jList.getForeground());
            }
            label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            return label;
        });

        JButton removeButton = new JButton("Remove Selected");
        removeButton.addActionListener(e -> {
            int index = list.getSelectedIndex();
            if (index >= 0) {
                manualModel.remove(index);
            }
        });

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> manualModel.clear());

        JButton copyManualRaw = new JButton("Copy Raw");
        copyManualRaw.addActionListener(e -> manualCopy(false));

        JButton copyManualHri = new JButton("Copy HRI");
        copyManualHri.addActionListener(e -> manualCopy(true));

        JButton previewManual = new JButton("Preview");
        previewManual.addActionListener(e -> manualPreview());

        JButton printManual = new JButton("Print");
        printManual.addActionListener(e -> manualPrint());

        addButton.addActionListener(e -> {
            String ai = aiField.getText() == null ? "" : aiField.getText().trim();
            String value = valueField.getText() == null ? "" : valueField.getText();
            if (ai.isEmpty() || value.isEmpty()) {
                JOptionPane.showMessageDialog(this, "AI and value are required.", "Manual Encode", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                ParseResult result = GS1Parser.parse(ai + value, settings.toParseOptions());
                if (!result.success() || result.elements().isEmpty()) {
                    throw new IllegalArgumentException("Invalid AI/value combination");
                }
                GS1Element element = result.elements().get(0).element();
                if (!element.ai().equals(ai)) {
                    throw new IllegalArgumentException("Unexpected AI parsed: " + element.ai());
                }
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Manual Encode", JOptionPane.ERROR_MESSAGE);
                return;
            }
            manualModel.addElement(new GS1Element(ai, value));
            aiField.setText("");
            valueField.setText("");
            aiField.requestFocusInWindow();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(removeButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(copyManualRaw);
        buttonPanel.add(copyManualHri);
        buttonPanel.add(previewManual);
        buttonPanel.add(printManual);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(600, 360));
        panel.add(tableScroll, BorderLayout.CENTER);

        JPanel detailPanel = new JPanel(new BorderLayout(5, 5));
        detailPanel.setBorder(BorderFactory.createTitledBorder("Details"));

        JPanel rawPanel = new JPanel(new BorderLayout());
        rawPanel.add(new JLabel("Raw"), BorderLayout.NORTH);
        rawPanel.add(new JScrollPane(rawArea), BorderLayout.CENTER);

        JPanel normalizedPanel = new JPanel(new BorderLayout());
        normalizedPanel.add(new JLabel("Normalized"), BorderLayout.NORTH);
        normalizedPanel.add(new JScrollPane(normalizedArea), BorderLayout.CENTER);

        JPanel metadataPanel = new JPanel(new BorderLayout());
        metadataPanel.add(metadataLabel, BorderLayout.CENTER);

        JPanel topDetail = new JPanel(new BorderLayout(5, 5));
        topDetail.add(rawPanel, BorderLayout.NORTH);
        topDetail.add(normalizedPanel, BorderLayout.CENTER);
        topDetail.add(metadataPanel, BorderLayout.SOUTH);

        detailPanel.add(topDetail, BorderLayout.NORTH);

        JTable parsedTable = new JTable(parsedModel);
        parsedTable.setFillsViewportHeight(true);
        JPanel parsedPanel = new JPanel(new BorderLayout());
        parsedPanel.add(new JLabel("Parsed AIs"), BorderLayout.NORTH);
        parsedPanel.add(new JScrollPane(parsedTable), BorderLayout.CENTER);

        JPanel warningsPanel = new JPanel(new BorderLayout());
        warningsPanel.add(new JLabel("Messages"), BorderLayout.NORTH);
        warningsPanel.add(new JScrollPane(warningsArea), BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.add(parsedPanel, BorderLayout.CENTER);
        centerPanel.add(warningsPanel, BorderLayout.SOUTH);

        detailPanel.add(centerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(copyRawButton);
        buttonPanel.add(copyNormalizedButton);
        buttonPanel.add(copyHriButton);
        buttonPanel.add(previewButton);
        buttonPanel.add(printButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(parseStatusLabel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        detailPanel.add(bottomPanel, BorderLayout.SOUTH);

        panel.add(detailPanel, BorderLayout.SOUTH);

        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);

        return panel;
    }

    private void applySettings() {
        autoSaveCheckBox.setSelected(settings.isAutoSaveOnEnter());
        focusLockCheckBox.setSelected(settings.isFocusLock());
        serialConfigLabel.setText(settings.getSerialBaudRate() + " / " + settings.getSerialDataBits() + "-" + parityLabel(settings.getSerialParity()) + "-" + stopBitsLabel(settings.getSerialStopBits()));
        if (focusLockCheckBox.isSelected()) {
            SwingUtilities.invokeLater(() -> hidInputField.requestFocusInWindow());
        }
    }

    private String parityLabel(int parity) {
        return switch (parity) {
            case SerialPort.ODD_PARITY -> "O";
            case SerialPort.EVEN_PARITY -> "E";
            case SerialPort.MARK_PARITY -> "M";
            case SerialPort.SPACE_PARITY -> "S";
            default -> "N";
        };
    }

    private String stopBitsLabel(int stopBits) {
        return switch (stopBits) {
            case SerialPort.ONE_POINT_FIVE_STOP_BITS -> "1.5";
            case SerialPort.TWO_STOP_BITS -> "2";
            default -> "1";
        };
    }

    private void refreshPorts() {
        portComboBox.removeAllItems();
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            portComboBox.addItem(port.getSystemPortName());
        }
        if (portComboBox.getItemCount() == 0) {
            serialStatusLabel.setText("No ports detected");
        } else {
            serialStatusLabel.setText("Select a port and connect");
        }
    }

    private void connectSerial() {
        if (activeSerialPort != null) {
            return;
        }
        String portName = (String) portComboBox.getSelectedItem();
        if (portName == null) {
            JOptionPane.showMessageDialog(this, "Select a port first.", "Serial", JOptionPane.WARNING_MESSAGE);
            return;
        }
        SerialPort port = SerialPort.getCommPort(portName);
        port.setComPortParameters(settings.getSerialBaudRate(), settings.getSerialDataBits(), settings.getSerialStopBits(), settings.getSerialParity());
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, settings.getSerialIdleTimeoutMs(), settings.getSerialIdleTimeoutMs());
        if (!port.openPort()) {
            JOptionPane.showMessageDialog(this, "Unable to open port.", "Serial", JOptionPane.ERROR_MESSAGE);
            return;
        }
        activeSerialPort = port;
        serialRunning = true;
        serialThread = new Thread(() -> readSerialLoop(portName, port), "SerialReader");
        serialThread.start();
        serialStatusLabel.setText("Connected to " + portName);
        setStatus("Connected to serial port " + portName);
        connectButton.setEnabled(false);
        disconnectButton.setEnabled(true);
    }

    private void disconnectSerial() {
        serialRunning = false;
        if (serialThread != null) {
            try {
                serialThread.join(200);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            serialThread = null;
        }
        if (activeSerialPort != null) {
            activeSerialPort.closePort();
            activeSerialPort = null;
        }
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
        serialStatusLabel.setText("Not connected");
    }

    private void readSerialLoop(String portName, SerialPort port) {
        StringBuilder builder = new StringBuilder();
        long lastByteTime = System.currentTimeMillis();
        try (var stream = port.getInputStream()) {
            while (serialRunning) {
                int value = stream.read();
                if (value == -1) {
                    break;
                }
                char c = (char) value;
                long now = System.currentTimeMillis();
                if (c == '\r' || c == '\n') {
                    if (builder.length() > 0) {
                        String data = builder.toString();
                        builder.setLength(0);
                        handleSerialLine(data, portName);
                    }
                } else if (c == 0x03 || c == 0x00) {
                    if (builder.length() > 0) {
                        String data = builder.toString();
                        builder.setLength(0);
                        handleSerialLine(data, portName);
                    }
                } else {
                    builder.append(c);
                }
                if (builder.length() > 0 && now - lastByteTime >= settings.getSerialIdleTimeoutMs()) {
                    String data = builder.toString();
                    builder.setLength(0);
                    handleSerialLine(data, portName);
                }
                lastByteTime = now;
            }
            if (builder.length() > 0) {
                String data = builder.toString();
                builder.setLength(0);
                handleSerialLine(data, portName);
            }
        } catch (IOException ex) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, ex.getMessage(), "Serial error", JOptionPane.ERROR_MESSAGE));
        } finally {
            SwingUtilities.invokeLater(this::disconnectSerial);
        }
    }

    private void handleSerialLine(String line, String portName) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        SwingUtilities.invokeLater(() -> storeScan(trimmed, ScanSource.SERIAL, portName));
    }

    private void saveHidInput() {
        String text = hidInputField.getText();
        if (text == null || text.isEmpty()) {
            return;
        }
        storeScan(text, ScanSource.HID, null);
        hidInputField.setText("");
        hidInputField.requestFocusInWindow();
    }

    private void storeScan(String raw, ScanSource source, String portName) {
        storeScan(raw, source, portName, false);
    }

    private void storeScan(String raw, ScanSource source, String portName, boolean silent) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        NormalizationResult normalization = Normalizer.normalizeRaw(raw, settings.toNormalizationOptions());
        String normalized = normalization.normalized();
        if (normalized.isEmpty()) {
            if (!silent) {
                beepError();
                JOptionPane.showMessageDialog(this, "Normalized result is empty after stripping control characters.", "Normalization", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        if (normalized.length() > settings.getMaxScanLength()) {
            if (!silent) {
                beepError();
                JOptionPane.showMessageDialog(this, "Normalized value exceeds maximum length of " + settings.getMaxScanLength() + ".", "Normalization", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        long now = System.currentTimeMillis();
        if (source == ScanSource.HID) {
            if (normalized.equals(lastHidNormalized) && now - lastHidTimestamp < settings.getDuplicateSuppressionMs()) {
                if (!silent) {
                    beepError();
                    setStatus("Duplicate HID scan suppressed");
                }
                return;
            }
            lastHidNormalized = normalized;
            lastHidTimestamp = now;
        } else if (source == ScanSource.SERIAL) {
            if (normalized.equals(lastSerialNormalized) && now - lastSerialTimestamp < settings.getDuplicateSuppressionMs()) {
                if (!silent) {
                    beepError();
                    setStatus("Duplicate serial scan suppressed");
                }
                return;
            }
            lastSerialNormalized = normalized;
            lastSerialTimestamp = now;
        }

        ParseResult parseResult = GS1Parser.parse(normalized, settings.toParseOptions());
        boolean parseOk = parseResult.success();
        String error = parseOk ? null : (parseResult.errors().isEmpty() ? "Unknown parse error" : parseResult.errors().get(0).message());
        try {
            database.insertScan(raw, normalized, source.display(), portName, normalization.symbologyId(), parseOk, error, parseResult.elements());
            loadScans();
            if (!silent) {
                if (parseOk) {
                    beepSuccess();
                    setStatus("Stored scan from " + source.display());
                } else {
                    beepError();
                    setStatus("Stored scan with errors from " + source.display());
                }
            }
        } catch (SQLException ex) {
            if (!silent) {
                beepError();
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void loadScans() {
        try {
            currentScans = database.listScans(MAX_DISPLAY_ROWS);
            tableModel.setRowCount(0);
            for (Scan scan : currentScans) {
                String status = scan.isParseOk() ? "OK" : "Error";
                tableModel.addRow(new Object[]{scan.getId(), scan.getCreatedAt(), scan.getSource(), status, GS1Parser.withVisibleControlChars(scan.getRaw())});
            }
            if (!currentScans.isEmpty()) {
                table.setRowSelectionInterval(0, 0);
            } else {
                clearDetails();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearDetails() {
        selectedScan = null;
        selectedParseResult = null;
        selectedElements = new ArrayList<>();
        selectedNormalized = null;
        rawArea.setText("");
        normalizedArea.setText("");
        metadataLabel.setText(" ");
        warningsArea.setText("");
        parsedModel.setRowCount(0);
        parseStatusLabel.setText(" ");
        previewButton.setEnabled(false);
        printButton.setEnabled(false);
        copyRawButton.setEnabled(false);
        copyNormalizedButton.setEnabled(false);
        copyHriButton.setEnabled(false);
    }

    private void updateDetails(Scan scan) {
        selectedScan = scan;
        copyRawButton.setEnabled(true);

        rawArea.setText(GS1Parser.withVisibleControlChars(scan.getRaw()));
        String normalized = scan.getNormalized();
        if (normalized == null) {
            normalized = Normalizer.normalizeRaw(scan.getRaw(), settings.toNormalizationOptions()).normalized();
        }
        selectedNormalized = normalized;
        normalizedArea.setText(GS1Parser.withVisibleControlChars(normalized));

        StringBuilder meta = new StringBuilder();
        meta.append("Source: ").append(scan.getSource() == null ? "Unknown" : scan.getSource());
        if (scan.getPort() != null) {
            meta.append(" | Port: ").append(scan.getPort());
        }
        if (scan.getSymbologyId() != null) {
            meta.append(" | Symbology ID: ").append(scan.getSymbologyId());
        }
        meta.append(" | Stored: ").append(scan.getCreatedAt());
        metadataLabel.setText(meta.toString());

        ParseResult parseResult = GS1Parser.parse(normalized, settings.toParseOptions());
        selectedParseResult = parseResult;
        parsedModel.setRowCount(0);
        selectedElements = new ArrayList<>();
        for (ParsedElement element : parseResult.elements()) {
            selectedElements.add(element.element());
            AiDefinition definition = element.definition();
            String name = definition == null ? "" : definition.description();
            String status = element.valid() ? "✓" : "⚠";
            parsedModel.addRow(new Object[]{element.element().ai(), name, element.element().value(), status});
        }

        StringBuilder statusText = new StringBuilder();
        if (parseResult.success()) {
            statusText.append("Parsed ").append(parseResult.elements().size()).append(" elements");
        } else {
            statusText.append("Errors: ");
            for (ParseMessage message : parseResult.errors()) {
                statusText.append(message.message()).append("; ");
            }
        }
        if (!parseResult.warnings().isEmpty()) {
            statusText.append(" | Warnings: ").append(parseResult.warnings().size());
        }
        if (parseResult.heuristicsApplied()) {
            statusText.append(" | Heuristic repair applied");
        }
        parseStatusLabel.setText(statusText.toString());

        List<String> messages = new ArrayList<>();
        NormalizationResult normalization = Normalizer.normalizeRaw(scan.getRaw(), settings.toNormalizationOptions());
        messages.addAll(normalization.warnings());
        for (ParsedElement element : parseResult.elements()) {
            for (String warning : element.warnings()) {
                messages.add("(" + element.element().ai() + ") " + warning);
            }
        }
        for (ParseMessage warning : parseResult.warnings()) {
            messages.add(warning.message());
        }
        if (!parseResult.success()) {
            for (ParseMessage error : parseResult.errors()) {
                messages.add("Error: " + error.message());
            }
        }
        if (scan.getError() != null && !scan.getError().isBlank()) {
            messages.add("Stored error: " + scan.getError());
        }
        warningsArea.setText(String.join("\n", messages));

        boolean canEncode = parseResult.success() && !selectedElements.isEmpty();
        previewButton.setEnabled(canEncode);
        printButton.setEnabled(canEncode);
        copyNormalizedButton.setEnabled(true);
        copyHriButton.setEnabled(!selectedElements.isEmpty());
    }

    private void previewSelected() {
        if (selectedParseResult == null || selectedElements.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No parsed data available.", "Preview", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String composed = GS1Parser.composeGs1(selectedElements);
            var matrix = BarcodeGenerator.encodeDataMatrix(composed, settings.getBarcodePixels(), settings.getBarcodeMargin());
            BufferedImage image = BarcodeGenerator.toImage(matrix);
            showPreviewDialog(matrix, image, composed);
        } catch (WriterException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Encoding error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showPreviewDialog(com.google.zxing.common.BitMatrix matrix, BufferedImage image, String raw) {
        JDialog dialog = new JDialog(this, "Barcode Preview", true);
        dialog.setLayout(new BorderLayout());
        JLabel label = new JLabel(new javax.swing.ImageIcon(image));
        dialog.add(new JScrollPane(label), BorderLayout.CENTER);
        JTextArea textArea = new JTextArea(GS1Parser.withVisibleControlChars(raw));
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        dialog.add(new JScrollPane(textArea), BorderLayout.SOUTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton savePng = new JButton("Save PNG...");
        savePng.addActionListener(e -> savePng(matrix));
        JButton saveSvg = new JButton("Save SVG...");
        saveSvg.addActionListener(e -> saveSvg(matrix));
        JButton copyRawButton = new JButton("Copy Raw");
        copyRawButton.addActionListener(e -> ClipboardUtil.copyToClipboard(raw));
        buttons.add(copyRawButton);
        buttons.add(savePng);
        buttons.add(saveSvg);
        dialog.add(buttons, BorderLayout.NORTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void savePng(com.google.zxing.common.BitMatrix matrix) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save PNG");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            Path path = file.toPath();
            try {
                BarcodeGenerator.writePng(matrix, path);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Save PNG", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveSvg(com.google.zxing.common.BitMatrix matrix) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save SVG");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writer.write(BarcodeGenerator.toSvg(matrix));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Save SVG", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void printSelected() {
        if (selectedParseResult == null || selectedElements.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No parsed data available.", "Print", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String defaultTitle = selectedScan == null ? "" : ("Scan " + selectedScan.getId());
        String hri = GS1Parser.composeHri(selectedElements);
        PrintOptionsDialog dialog = new PrintOptionsDialog(this, settings, defaultTitle, hri);
        dialog.setVisible(true);
        PrintOptions options = dialog.getOptions();
        if (options == null) {
            return;
        }
        try {
            String composed = GS1Parser.composeGs1(selectedElements);
            BufferedImage image = BarcodeGenerator.generateDataMatrix(composed, settings.getBarcodePixels(), settings.getBarcodeMargin());
            PrinterUtil.printBarcode(image, options);
        } catch (WriterException | PrinterException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Print", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void copyRaw() {
        if (selectedScan == null) {
            return;
        }
        ClipboardUtil.copyToClipboard(selectedScan.getRaw());
        setStatus("Raw value copied to clipboard");
    }

    private void copyNormalized() {
        if (selectedNormalized == null) {
            return;
        }
        ClipboardUtil.copyToClipboard(selectedNormalized);
        setStatus("Normalized value copied to clipboard");
    }

    private void copyHri() {
        if (selectedElements.isEmpty()) {
            return;
        }
        ClipboardUtil.copyToClipboard(GS1Parser.composeHri(selectedElements));
        setStatus("HRI copied to clipboard");
    }

    private List<GS1Element> getManualElements() {
        List<GS1Element> elements = new ArrayList<>();
        for (int i = 0; i < manualModel.size(); i++) {
            elements.add(manualModel.get(i));
        }
        return elements;
    }

    private void manualPreview() {
        List<GS1Element> elements = getManualElements();
        if (elements.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one AI/value pair.", "Manual Encode", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String composed = GS1Parser.composeGs1(elements);
            var matrix = BarcodeGenerator.encodeDataMatrix(composed, settings.getBarcodePixels(), settings.getBarcodeMargin());
            BufferedImage image = BarcodeGenerator.toImage(matrix);
            showPreviewDialog(matrix, image, composed);
        } catch (WriterException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Encoding error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void manualPrint() {
        List<GS1Element> elements = getManualElements();
        if (elements.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one AI/value pair.", "Manual Encode", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String hri = GS1Parser.composeHri(elements);
        PrintOptionsDialog dialog = new PrintOptionsDialog(this, settings, "Manual", hri);
        dialog.setVisible(true);
        PrintOptions options = dialog.getOptions();
        if (options == null) {
            return;
        }
        try {
            String composed = GS1Parser.composeGs1(elements);
            BufferedImage image = BarcodeGenerator.generateDataMatrix(composed, settings.getBarcodePixels(), settings.getBarcodeMargin());
            PrinterUtil.printBarcode(image, options);
        } catch (WriterException | PrinterException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Print", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void manualCopy(boolean hri) {
        List<GS1Element> elements = getManualElements();
        if (elements.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one AI/value pair.", "Manual Encode", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String value = hri ? GS1Parser.composeHri(elements) : GS1Parser.composeGs1(elements);
        ClipboardUtil.copyToClipboard(value);
        setStatus(hri ? "Manual HRI copied" : "Manual raw copied");
    }

    private void toggleFocusLock() {
        settings.setFocusLock(focusLockCheckBox.isSelected());
        settings.save();
        if (focusLockCheckBox.isSelected()) {
            SwingUtilities.invokeLater(() -> hidInputField.requestFocusInWindow());
        }
    }

    private void openSettings() {
        SettingsDialog dialog = new SettingsDialog(this, settings);
        dialog.setVisible(true);
        applySettings();
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export CSV");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writer.write("id,created_at,source,port,symbology_id,parse_ok,error,raw,normalized\n");
                for (Scan scan : database.listAllScans()) {
                    writer.write(escapeCsv(Long.toString(scan.getId())) + ',');
                    writer.write(escapeCsv(scan.getCreatedAt()) + ',');
                    writer.write(escapeCsv(scan.getSource()) + ',');
                    writer.write(escapeCsv(scan.getPort()) + ',');
                    writer.write(escapeCsv(scan.getSymbologyId()) + ',');
                    writer.write(scan.isParseOk() ? "true," : "false,");
                    writer.write(escapeCsv(scan.getError()) + ',');
                    writer.write(escapeCsv(scan.getRaw()) + ',');
                    writer.write(escapeCsv(scan.getNormalized()));
                    writer.write("\n");
                }
                setStatus("Exported CSV to " + file.getName());
            } catch (IOException | SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Export CSV", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportJson() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export JSON");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writer.write("[\n");
                List<Scan> scans = database.listAllScans();
                for (int i = 0; i < scans.size(); i++) {
                    Scan scan = scans.get(i);
                    writer.write("  {\n");
                    writer.write("    \"id\": " + scan.getId() + ",\n");
                    writer.write("    \"created_at\": \"" + escapeJson(scan.getCreatedAt()) + "\",\n");
                    writer.write("    \"source\": \"" + escapeJson(scan.getSource()) + "\",\n");
                    if (scan.getPort() != null) {
                        writer.write("    \"port\": \"" + escapeJson(scan.getPort()) + "\",\n");
                    }
                    if (scan.getSymbologyId() != null) {
                        writer.write("    \"symbology_id\": \"" + escapeJson(scan.getSymbologyId()) + "\",\n");
                    }
                    writer.write("    \"parse_ok\": " + scan.isParseOk() + ",\n");
                    if (scan.getError() != null) {
                        writer.write("    \"error\": \"" + escapeJson(scan.getError()) + "\",\n");
                    }
                    writer.write("    \"raw\": \"" + escapeJson(scan.getRaw()) + "\",\n");
                    writer.write("    \"normalized\": \"" + escapeJson(scan.getNormalized()) + "\",\n");
                    writer.write("    \"elements\": [");
                    List<ScanElement> elements = scan.getElements();
                    for (int j = 0; j < elements.size(); j++) {
                        ScanElement element = elements.get(j);
                        writer.write("{\"ai\": \"" + escapeJson(element.ai()) + "\", \"value\": \"" + escapeJson(element.value()) + "\"}");
                        if (j < elements.size() - 1) {
                            writer.write(", ");
                        }
                    }
                    writer.write("]\n");
                    writer.write("  }");
                    if (i < scans.size() - 1) {
                        writer.write(",");
                    }
                    writer.write("\n");
                }
                writer.write("]\n");
                setStatus("Exported JSON to " + file.getName());
            } catch (IOException | SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Export JSON", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importNormalized() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import normalized");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            int imported = 0;
            try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        storeScan(line, ScanSource.IMPORT, null, true);
                        imported++;
                    }
                }
                loadScans();
                setStatus("Imported " + imported + " scans from " + file.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Import", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\', '"' -> builder.append('\\').append(c);
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(c);
            }
        }
        return builder.toString();
    }

    private void setStatus(String message) {
        statusBar.setText(message);
        Timer timer = new Timer(5000, e -> statusBar.setText("Ready"));
        timer.setRepeats(false);
        timer.start();
    }

    private void beepSuccess() {
        Toolkit.getDefaultToolkit().beep();
    }

    private void beepError() {
        Toolkit.getDefaultToolkit().beep();
        Timer timer = new Timer(150, e -> Toolkit.getDefaultToolkit().beep());
        timer.setRepeats(false);
        timer.start();
    }

    private class TableSelectionHandler implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                int viewRow = table.getSelectedRow();
                if (viewRow >= 0) {
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    if (modelRow >= 0 && modelRow < currentScans.size()) {
                        updateDetails(currentScans.get(modelRow));
                        return;
                    }
                }
                clearDetails();
            }
        }
    }
}
