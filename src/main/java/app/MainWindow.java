package app;

import com.fazecast.jSerialComm.SerialPort;
import com.google.zxing.WriterException;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainWindow extends JFrame {
    private final Database database;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextArea rawArea;
    private final DefaultTableModel parsedModel;
    private final JLabel parseStatusLabel;
    private final JButton previewButton;
    private final JButton printButton;
    private final JButton copyButton;
    private final JTextField hidInputField;
    private final JCheckBox autoSaveCheckBox;
    private final JLabel serialStatusLabel;
    private final JComboBox<String> portComboBox;
    private final JButton connectButton;
    private final JButton disconnectButton;
    private final DefaultListModel<GS1Element> manualModel;
    private SerialPort activeSerialPort;
    private Thread serialThread;
    private volatile boolean serialRunning;
    private List<Scan> currentScans = new ArrayList<>();
    private Scan selectedScan;
    private List<GS1Element> selectedElements;

    public MainWindow(Database database) {
        this.database = database;
        setTitle("GS1Desk");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 620));

        tableModel = new DefaultTableModel(new Object[]{"ID", "Created", "Raw"}, 0) {
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
        rawArea.setRows(3);

        parsedModel = new DefaultTableModel(new Object[]{"AI", "Value"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable parsedTable = new JTable(parsedModel);
        parsedTable.setFillsViewportHeight(true);

        parseStatusLabel = new JLabel(" ");

        previewButton = new JButton("Preview");
        previewButton.setEnabled(false);
        previewButton.addActionListener(e -> previewSelected());

        printButton = new JButton("Print");
        printButton.setEnabled(false);
        printButton.addActionListener(e -> printSelected());

        copyButton = new JButton("Copy Raw");
        copyButton.setEnabled(false);
        copyButton.addActionListener(e -> copySelected());

        hidInputField = new JTextField();
        autoSaveCheckBox = new JCheckBox("Auto-save on Enter", true);
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveHidInput());
        hidInputField.addActionListener(e -> {
            if (autoSaveCheckBox.isSelected()) {
                saveHidInput();
            }
        });

        portComboBox = new JComboBox<>();
        JButton refreshPortsButton = new JButton("Refresh");
        refreshPortsButton.addActionListener(e -> refreshPorts());
        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connectSerial());
        disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(e -> disconnectSerial());
        disconnectButton.setEnabled(false);
        serialStatusLabel = new JLabel("Not connected");

        manualModel = new DefaultListModel<>();

        JPanel leftPanel = buildLeftPanel(saveButton, refreshPortsButton);
        JPanel rightPanel = buildRightPanel(parsedTable);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.32);
        add(splitPane, BorderLayout.CENTER);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                disconnectSerial();
            }
        });

        SwingUtilities.invokeLater(() -> hidInputField.requestFocusInWindow());
        refreshPorts();
    }

    private JPanel buildLeftPanel(JButton saveButton, JButton refreshPortsButton) {
        JPanel hidPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        hidPanel.add(new JLabel("Scan input"), gbc);
        gbc.gridy++;
        gbc.weightx = 1.0;
        hidPanel.add(hidInputField, gbc);
        gbc.gridy++;
        gbc.weightx = 0;
        hidPanel.add(autoSaveCheckBox, gbc);
        gbc.gridy++;
        hidPanel.add(saveButton, gbc);

        JPanel serialPanel = new JPanel(new GridBagLayout());
        GridBagConstraints sgbc = new GridBagConstraints();
        sgbc.insets = new Insets(5, 5, 5, 5);
        sgbc.fill = GridBagConstraints.HORIZONTAL;
        sgbc.gridx = 0;
        sgbc.gridy = 0;
        serialPanel.add(new JLabel("Port"), sgbc);
        sgbc.gridy++;
        serialPanel.add(portComboBox, sgbc);
        sgbc.gridy++;
        serialPanel.add(refreshPortsButton, sgbc);
        sgbc.gridy++;
        serialPanel.add(connectButton, sgbc);
        sgbc.gridy++;
        serialPanel.add(disconnectButton, sgbc);
        sgbc.gridy++;
        serialPanel.add(serialStatusLabel, sgbc);

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

        JButton previewManualButton = new JButton("Preview");
        previewManualButton.addActionListener(e -> manualPreview());

        JButton printManualButton = new JButton("Print");
        printManualButton.addActionListener(e -> manualPrint());

        JButton copyManualButton = new JButton("Copy Raw");
        copyManualButton.addActionListener(e -> manualCopy());

        addButton.addActionListener(e -> {
            String ai = aiField.getText() == null ? "" : aiField.getText().trim();
            String value = valueField.getText() == null ? "" : valueField.getText();
            if (ai.isEmpty() || value.isEmpty()) {
                JOptionPane.showMessageDialog(this, "AI and value are required.", "Manual Encode", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String candidate = ai + value;
            try {
                List<GS1Element> parsed = GS1Parser.parseGs1(candidate);
                if (parsed.size() != 1 || !Objects.equals(parsed.get(0).ai(), ai)) {
                    throw new IllegalArgumentException("Unable to validate AI/value pair");
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
        buttonPanel.add(copyManualButton);
        buttonPanel.add(previewManualButton);
        buttonPanel.add(printManualButton);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildRightPanel(JTable parsedTable) {
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
        detailPanel.add(rawPanel, BorderLayout.NORTH);

        JPanel parsedPanel = new JPanel(new BorderLayout());
        parsedPanel.add(new JLabel("Parsed AIs"), BorderLayout.NORTH);
        parsedPanel.add(new JScrollPane(parsedTable), BorderLayout.CENTER);
        detailPanel.add(parsedPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(parseStatusLabel, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(copyButton);
        buttonPanel.add(previewButton);
        buttonPanel.add(printButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        detailPanel.add(bottomPanel, BorderLayout.SOUTH);

        panel.add(detailPanel, BorderLayout.SOUTH);
        return panel;
    }

    public void loadScans() {
        try {
            currentScans = database.listScans(200);
            tableModel.setRowCount(0);
            for (Scan scan : currentScans) {
                tableModel.addRow(new Object[]{scan.getId(), scan.getCreatedAt(), GS1Parser.withVisibleGs(scan.getRaw())});
            }
            if (!currentScans.isEmpty()) {
                table.setRowSelectionInterval(0, 0);
                copyButton.setEnabled(true);
            } else {
                clearDetails();
                copyButton.setEnabled(false);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveHidInput() {
        String text = hidInputField.getText();
        if (text == null || text.isEmpty()) {
            return;
        }
        storeScan(text);
        hidInputField.setText("");
        hidInputField.requestFocusInWindow();
    }

    private void storeScan(String raw) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        try {
            database.insertScan(raw);
            loadScans();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void previewSelected() {
        if (selectedElements == null || selectedElements.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No parsed data available.", "Preview", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String composed = GS1Parser.composeGs1(selectedElements);
            BufferedImage image = BarcodeGenerator.generateDataMatrix(composed, 300);
            showPreviewDialog(image, composed);
        } catch (WriterException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Encoding error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printSelected() {
        if (selectedElements == null || selectedElements.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No parsed data available.", "Print", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String title = JOptionPane.showInputDialog(this, "Optional title for printout", "Print Barcode", JOptionPane.PLAIN_MESSAGE);
        if (title != null) {
            try {
                String composed = GS1Parser.composeGs1(selectedElements);
                BufferedImage image = BarcodeGenerator.generateDataMatrix(composed, 300);
                PrinterUtil.printBarcode(image, title, 300);
            } catch (WriterException | PrinterException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Print error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void copySelected() {
        if (selectedScan == null) {
            JOptionPane.showMessageDialog(this, "No scan selected.", "Copy", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ClipboardUtil.copyToClipboard(selectedScan.getRaw());
        JOptionPane.showMessageDialog(this, "Raw value copied to clipboard.", "Copy", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showPreviewDialog(BufferedImage image, String text) {
        JDialog dialog = new JDialog(this, "Barcode Preview", true);
        JLabel label = new JLabel(new ImageIcon(image));
        dialog.add(new JScrollPane(label), BorderLayout.CENTER);
        JTextArea textArea = new JTextArea(GS1Parser.withVisibleGs(text));
        textArea.setEditable(false);
        dialog.add(textArea, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
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
        port.setComPortParameters(9600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
        if (!port.openPort()) {
            JOptionPane.showMessageDialog(this, "Unable to open port.", "Serial", JOptionPane.ERROR_MESSAGE);
            return;
        }
        activeSerialPort = port;
        serialRunning = true;
        serialThread = new Thread(this::readSerialLoop, "SerialReader");
        serialThread.start();
        serialStatusLabel.setText("Connected to " + portName);
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

    private void readSerialLoop() {
        SerialPort port = activeSerialPort;
        if (port == null) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        try (InputStream stream = port.getInputStream()) {
            while (serialRunning) {
                int read = stream.read();
                if (read == -1) {
                    break;
                }
                if (read == '\r' || read == '\n') {
                    if (builder.length() > 0) {
                        String line = builder.toString();
                        builder.setLength(0);
                        handleSerialLine(line);
                    }
                } else {
                    builder.append((char) read);
                }
            }
        } catch (IOException ex) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, ex.getMessage(), "Serial error", JOptionPane.ERROR_MESSAGE));
        } finally {
            SwingUtilities.invokeLater(this::disconnectSerial);
        }
    }

    private void handleSerialLine(String line) {
        SwingUtilities.invokeLater(() -> storeScan(line));
    }

    private void manualPreview() {
        List<GS1Element> elements = getManualElements();
        if (elements.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one AI/value pair.", "Manual Encode", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String composed = GS1Parser.composeGs1(elements);
            BufferedImage image = BarcodeGenerator.generateDataMatrix(composed, 300);
            showPreviewDialog(image, composed);
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
        String title = JOptionPane.showInputDialog(this, "Optional title for printout", "Print Barcode", JOptionPane.PLAIN_MESSAGE);
        if (title != null) {
            try {
                String composed = GS1Parser.composeGs1(elements);
                BufferedImage image = BarcodeGenerator.generateDataMatrix(composed, 300);
                PrinterUtil.printBarcode(image, title, 300);
            } catch (WriterException | PrinterException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Print error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void manualCopy() {
        List<GS1Element> elements = getManualElements();
        if (elements.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one AI/value pair.", "Manual Encode", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String composed = GS1Parser.composeGs1(elements);
        ClipboardUtil.copyToClipboard(composed);
        JOptionPane.showMessageDialog(this, "Raw value copied to clipboard.", "Manual Encode", JOptionPane.INFORMATION_MESSAGE);
    }

    private List<GS1Element> getManualElements() {
        List<GS1Element> elements = new ArrayList<>();
        for (int i = 0; i < manualModel.size(); i++) {
            elements.add(manualModel.get(i));
        }
        return elements;
    }

    private void clearDetails() {
        selectedScan = null;
        selectedElements = null;
        rawArea.setText("");
        parsedModel.setRowCount(0);
        parseStatusLabel.setText(" ");
        previewButton.setEnabled(false);
        printButton.setEnabled(false);
    }

    private void updateDetails(Scan scan) {
        selectedScan = scan;
        copyButton.setEnabled(true);
        rawArea.setText(GS1Parser.withVisibleGs(scan.getRaw()));
        try {
            selectedElements = GS1Parser.parseGs1(scan.getRaw());
            parsedModel.setRowCount(0);
            for (GS1Element element : selectedElements) {
                parsedModel.addRow(new Object[]{element.ai(), element.value()});
            }
            parseStatusLabel.setText("Parsed " + selectedElements.size() + " elements");
            previewButton.setEnabled(true);
            printButton.setEnabled(true);
        } catch (IllegalArgumentException ex) {
            selectedElements = null;
            parsedModel.setRowCount(0);
            parseStatusLabel.setText("Error: " + ex.getMessage());
            previewButton.setEnabled(false);
            printButton.setEnabled(false);
        }
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
