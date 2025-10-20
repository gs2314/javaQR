package app;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public final class PrintOptionsDialog extends JDialog {
    private PrintOptions options;

    public PrintOptionsDialog(JFrame owner, UserSettings settings, String defaultTitle, String hriText) {
        super(owner, "Print Options", true);
        setLayout(new BorderLayout(10, 10));

        JSpinner sizeSpinner = new JSpinner(new SpinnerNumberModel(settings.getPrintSizeMillimetres(), 10.0, 200.0, 1.0));
        JSpinner dpiSpinner = new JSpinner(new SpinnerNumberModel(settings.getPrintDpi(), 150, 1200, 25));
        JSpinner copiesSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        JCheckBox includeHri = new JCheckBox("Include HRI text", true);
        JTextField titleField = new JTextField(defaultTitle == null ? "" : defaultTitle, 20);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Label size (mm)"), gbc);
        gbc.gridx = 1;
        panel.add(sizeSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Print DPI"), gbc);
        gbc.gridx = 1;
        panel.add(dpiSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Copies"), gbc);
        gbc.gridx = 1;
        panel.add(copiesSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Title"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(titleField, gbc);
        gbc.fill = GridBagConstraints.NONE;

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(includeHri, gbc);

        add(panel, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        JButton printButton = new JButton("Print");
        printButton.addActionListener(e -> {
            double sizeMm = ((Number) sizeSpinner.getValue()).doubleValue();
            int dpi = (Integer) dpiSpinner.getValue();
            int copies = (Integer) copiesSpinner.getValue();
            options = new PrintOptions(sizeMm, dpi, copies, includeHri.isSelected(), titleField.getText(), hriText);
            settings.setPrintSizeMillimetres(sizeMm);
            settings.setPrintDpi(dpi);
            settings.save();
            dispose();
        });
        buttons.add(cancelButton);
        buttons.add(printButton);
        add(buttons, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    public PrintOptions getOptions() {
        return options;
    }
}
