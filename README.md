# GS1Desk

GS1Desk is an offline desktop utility for collecting and re-encoding GS1 scan data. It supports both HID keyboard wedge scanners and serial/USB-CDC scanners, stores every scan locally in SQLite, and lets you preview or print GS1 DataMatrix barcodes.

## Building

```bash
mvn clean package
```

This creates `target/GS1Desk-1.0.0-shaded.jar` containing all dependencies.

## Running

```bash
java -jar target/GS1Desk-1.0.0-shaded.jar
```

A `scans.db` file is created next to the executable jar and stores every captured scan with a timestamp.

## Using the app

1. Launch GS1Desk. The HID tab provides a text field — focus it, scan, and press Enter. Leave **Auto-save on Enter** enabled for hands-free capture.
2. For serial/USB scanners, choose the COM/tty port on the Serial tab and click **Connect**. Incoming lines terminated by CR/LF are saved automatically.
3. The right-hand table shows the 200 most recent scans. Selecting a row shows the raw data (with `<GS>` placeholders) and parsed GS1 Application Identifiers.
4. Use **Preview** to render a GS1 DataMatrix, **Print** to send it to a printer (~40×40 mm at 300 DPI with optional title), or **Copy Raw** to copy the original data (including ASCII 29 field separators) to the clipboard.
5. The **Manual Encode** panel lets you build a barcode from scratch by adding AI/value pairs and then previewing, printing, or copying the composed raw string.

### Test scan

Try the following sample:

- Human-readable: `(01)09506000134352(17)251201(10)ABC123(21)SN987654321`
- Raw (ASCII 29 between variable elements):
  ```
  01095060001343521725120110ABC123\u001D21SN987654321
  ```

Paste the raw data into the HID field and press Enter to see it parsed, saved, and ready for preview/print.
