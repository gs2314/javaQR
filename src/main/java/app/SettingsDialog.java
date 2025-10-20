package app;

import com.fazecast.jSerialComm.SerialPort;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;

public final class SettingsDialog extends JDialog {
    public SettingsDialog(JFrame owner, UserSettings settings) {
        super(owner, "Settings", true);
        setLayout(new BorderLayout(10, 10));

        JCheckBox stripAimCheck = new JCheckBox("Strip AIM symbology ID", settings.isStripAimId());
        JCheckBox collapseGsCheck = new JCheckBox("Collapse consecutive GS characters", settings.isCollapseGs());
        JCheckBox allowLowerCheck = new JCheckBox("Allow lowercase in alphanumeric AIs", settings.isAllowLowercase());
        JCheckBox heuristicCheck = new JCheckBox("Enable (10)/(21) heuristic repair", settings.isHeuristicRepair());

        JTextArea placeholderArea = new JTextArea(String.join("\n", settings.getGsPlaceholders()), 5, 24);
        placeholderArea.setLineWrap(true);
        placeholderArea.setWrapStyleWord(true);

        JSpinner duplicateSpinner = new JSpinner(new SpinnerNumberModel(settings.getDuplicateSuppressionMs(), 0, 10000, 50));
        JSpinner maxLengthSpinner = new JSpinner(new SpinnerNumberModel(settings.getMaxScanLength(), 1, 4096, 1));

        JSpinner baudSpinner = new JSpinner(new SpinnerNumberModel(settings.getSerialBaudRate(), 1200, 921600, 300));
        JComboBox<Integer> dataBitsCombo = new JComboBox<>(new Integer[]{5, 6, 7, 8});
        dataBitsCombo.setSelectedItem(settings.getSerialDataBits());
        JComboBox<StopBitsOption> stopBitsCombo = new JComboBox<>(StopBitsOption.values());
        stopBitsCombo.setSelectedItem(StopBitsOption.fromValue(settings.getSerialStopBits()));
        JComboBox<ParityOption> parityCombo = new JComboBox<>(ParityOption.values());
        parityCombo.setSelectedItem(ParityOption.fromValue(settings.getSerialParity()));
        JSpinner idleSpinner = new JSpinner(new SpinnerNumberModel(settings.getSerialIdleTimeoutMs(), 20, 2000, 10));

        JSpinner barcodeSizeSpinner = new JSpinner(new SpinnerNumberModel(settings.getBarcodePixels(), 80, 1024, 20));
        JSpinner barcodeMarginSpinner = new JSpinner(new SpinnerNumberModel(settings.getBarcodeMargin(), 0, 32, 1));
        JSpinner printSizeSpinner = new JSpinner(new SpinnerNumberModel(settings.getPrintSizeMillimetres(), 10.0, 200.0, 1.0));
        JSpinner printDpiSpinner = new JSpinner(new SpinnerNumberModel(settings.getPrintDpi(), 150, 1200, 25));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        form.add(stripAimCheck, gbc);
        gbc.gridy++;
        form.add(collapseGsCheck, gbc);
        gbc.gridy++;
        form.add(allowLowerCheck, gbc);
        gbc.gridy++;
        form.add(heuristicCheck, gbc);

        gbc.gridy++;
        form.add(new JLabel("Accepted GS placeholders (one per line)"), gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        form.add(new JScrollPane(placeholderArea), gbc);
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;

        gbc.gridy++;
        form.add(new JLabel("Duplicate suppression window (ms)"), gbc);
        gbc.gridy++;
        form.add(duplicateSpinner, gbc);

        gbc.gridy++;
        form.add(new JLabel("Max normalized scan length"), gbc);
        gbc.gridy++;
        form.add(maxLengthSpinner, gbc);

        gbc.gridy++;
        form.add(new JLabel("Serial baud rate"), gbc);
        gbc.gridy++;
        form.add(baudSpinner, gbc);

        gbc.gridy++;
        form.add(new JLabel("Serial data bits"), gbc);
        gbc.gridy++;
        form.add(dataBitsCombo, gbc);

        gbc.gridy++;
        form.add(new JLabel("Serial stop bits"), gbc);
        gbc.gridy++;
        form.add(stopBitsCombo, gbc);

        gbc.gridy++;
        form.add(new JLabel("Serial parity"), gbc);
        gbc.gridy++;
        form.add(parityCombo, gbc);

        gbc.gridy++;
        form.add(new JLabel("Serial idle flush (ms)"), gbc);
        gbc.gridy++;
        form.add(idleSpinner, gbc);

        gbc.gridy++;
        form.add(new JLabel("Barcode image size (px)"), gbc);
        gbc.gridy++;
        form.add(barcodeSizeSpinner, gbc);

        gbc.gridy++;
        form.add(new JLabel("Barcode quiet zone (modules)"), gbc);
        gbc.gridy++;
        form.add(barcodeMarginSpinner, gbc);

        gbc.gridy++;
        form.add(new JLabel("Default print size (mm)"), gbc);
        gbc.gridy++;
        form.add(printSizeSpinner, gbc);

        gbc.gridy++;
        form.add(new JLabel("Default print DPI"), gbc);
        gbc.gridy++;
        form.add(printDpiSpinner, gbc);

        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            settings.setStripAimId(stripAimCheck.isSelected());
            settings.setCollapseGs(collapseGsCheck.isSelected());
            settings.setAllowLowercase(allowLowerCheck.isSelected());
            settings.setHeuristicRepair(heuristicCheck.isSelected());
            settings.setGsPlaceholders(Arrays.asList(placeholderArea.getText().split("\n")));
            settings.setDuplicateSuppressionMs((Integer) duplicateSpinner.getValue());
            settings.setMaxScanLength((Integer) maxLengthSpinner.getValue());
            settings.setSerialBaudRate((Integer) baudSpinner.getValue());
            settings.setSerialDataBits((Integer) dataBitsCombo.getSelectedItem());
            settings.setSerialStopBits(((StopBitsOption) stopBitsCombo.getSelectedItem()).value);
            settings.setSerialParity(((ParityOption) parityCombo.getSelectedItem()).value);
            settings.setSerialIdleTimeoutMs((Integer) idleSpinner.getValue());
            settings.setBarcodePixels((Integer) barcodeSizeSpinner.getValue());
            settings.setBarcodeMargin((Integer) barcodeMarginSpinner.getValue());
            settings.setPrintSizeMillimetres(((Number) printSizeSpinner.getValue()).doubleValue());
            settings.setPrintDpi((Integer) printDpiSpinner.getValue());
            settings.save();
            dispose();
        });
        buttons.add(cancelButton);
        buttons.add(saveButton);
        add(buttons, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private enum StopBitsOption {
        ONE("1", SerialPort.ONE_STOP_BIT),
        ONE_POINT_FIVE("1.5", SerialPort.ONE_POINT_FIVE_STOP_BITS),
        TWO("2", SerialPort.TWO_STOP_BITS);

        private final String label;
        private final int value;

        StopBitsOption(String label, int value) {
            this.label = label;
            this.value = value;
        }

        @Override
        public String toString() {
            return label;
        }

        private static StopBitsOption fromValue(int value) {
            for (StopBitsOption option : values()) {
                if (option.value == value) {
                    return option;
                }
            }
            return ONE;
        }
    }

    private enum ParityOption {
        NONE("None", SerialPort.NO_PARITY),
        EVEN("Even", SerialPort.EVEN_PARITY),
        ODD("Odd", SerialPort.ODD_PARITY),
        MARK("Mark", SerialPort.MARK_PARITY),
        SPACE("Space", SerialPort.SPACE_PARITY);

        private final String label;
        private final int value;

        ParityOption(String label, int value) {
            this.label = label;
            this.value = value;
        }

        @Override
        public String toString() {
            return label;
        }

        private static ParityOption fromValue(int value) {
            for (ParityOption option : values()) {
                if (option.value == value) {
                    return option;
                }
            }
            return NONE;
        }
    }
}
