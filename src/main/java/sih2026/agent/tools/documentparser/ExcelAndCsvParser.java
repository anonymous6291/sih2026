package sih2026.agent.tools.documentparser;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ExcelAndCsvParser implements DocumentParser {
    private static final Set<String> supportedMimeTypes = Set.of(
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv"
    );

    public ExcelAndCsvParser(DocumentParserTool documentParserTool) {
        for (String mimeType : supportedMimeTypes) {
            documentParserTool.registerDocumentParser(mimeType, this);
        }
    }


    /*
     * =========================================================
     * CSV
     * =========================================================
     */

    public String extractCsv(Path filePath) throws IOException {

        List<List<String>> rows = new ArrayList<>();

        try (
                Reader reader = new InputStreamReader(
                        Files.newInputStream(filePath),
                        StandardCharsets.UTF_8
                );

                CSVParser parser = CSVFormat.DEFAULT.parse(reader)
        ) {

            for (CSVRecord record : parser) {

                List<String> row = new ArrayList<>();

                for (String value : record) {
                    row.add(value);
                }

                rows.add(row);
            }
        }

        return createMarkdownTable(rows);
    }

    /*
     * =========================================================
     * EXCEL
     * =========================================================
     */

    public String extractExcel(Path filePath)
            throws IOException {

        StringBuilder result = new StringBuilder();

        try (
                InputStream inputStream =
                        Files.newInputStream(filePath);

                Workbook workbook =
                        WorkbookFactory.create(inputStream)
        ) {

            DataFormatter formatter =
                    new DataFormatter();

            for (int sheetIndex = 0;
                 sheetIndex < workbook.getNumberOfSheets();
                 sheetIndex++) {

                Sheet sheet =
                        workbook.getSheetAt(sheetIndex);

                List<List<String>> rows =
                        readSheet(sheet, formatter);

                if (rows.isEmpty()) {
                    continue;
                }

                // Multiple Excel sheets
                if (workbook.getNumberOfSheets() > 1) {

                    result.append("Sheet: ")
                            .append(sheet.getSheetName())
                            .append("\n\n");
                }

                result.append(
                        createMarkdownTable(rows)
                );

                result.append("\n\n");
            }
        }

        return result.toString().strip();
    }

    /*
     * Read one Excel sheet
     */

    private List<List<String>> readSheet(
            Sheet sheet,
            DataFormatter formatter
    ) {

        List<List<String>> rows =
                new ArrayList<>();

        int firstRow =
                sheet.getFirstRowNum();

        int lastRow =
                sheet.getLastRowNum();

        if (lastRow < firstRow) {
            return rows;
        }

        /*
         * Determine the maximum number
         * of columns in the sheet.
         */
        int maxColumns = 0;

        for (int rowIndex = firstRow;
             rowIndex <= lastRow;
             rowIndex++) {

            Row row =
                    sheet.getRow(rowIndex);

            if (row != null) {

                short lastCell =
                        row.getLastCellNum();

                if (lastCell > maxColumns) {
                    maxColumns = lastCell;
                }
            }
        }

        /*
         * Extract every cell.
         */
        for (int rowIndex = firstRow;
             rowIndex <= lastRow;
             rowIndex++) {

            Row excelRow =
                    sheet.getRow(rowIndex);

            List<String> row =
                    new ArrayList<>();

            for (int columnIndex = 0;
                 columnIndex < maxColumns;
                 columnIndex++) {

                String value = "";

                if (excelRow != null) {

                    Cell cell =
                            excelRow.getCell(
                                    columnIndex,
                                    Row.MissingCellPolicy
                                            .RETURN_BLANK_AS_NULL
                            );

                    if (cell != null) {

                        value =
                                formatter.formatCellValue(
                                        cell
                                );
                    }
                }

                row.add(value);
            }

            rows.add(row);
        }

        return rows;
    }

    /*
     * =========================================================
     * Markdown table
     * =========================================================
     */

    private String createMarkdownTable(
            List<List<String>> rows
    ) {

        if (rows == null || rows.isEmpty()) {
            return "";
        }

        /*
         * Find maximum number of columns.
         */
        int columnCount = 0;

        for (List<String> row : rows) {

            if (row != null) {

                columnCount =
                        Math.max(
                                columnCount,
                                row.size()
                        );
            }
        }

        if (columnCount == 0) {
            return "";
        }

        StringBuilder table =
                new StringBuilder();

        /*
         * ==========================
         * HEADER
         * ==========================
         */

        List<String> header =
                rows.getFirst();

        table.append("|");

        for (int column = 0;
             column < columnCount;
             column++) {

            String value =
                    getCell(header, column);

            table.append(" ")
                    .append(escape(value))
                    .append(" |");
        }

        table.append("\n");

        /*
         * ==========================
         * HEADER BORDER
         * ==========================
         */

        table.append("|");

        table.repeat("---|", columnCount);

        table.append("\n");

        /*
         * ==========================
         * DATA
         * ==========================
         */

        for (int rowIndex = 1;
             rowIndex < rows.size();
             rowIndex++) {

            List<String> row =
                    rows.get(rowIndex);

            table.append("|");

            for (int column = 0;
                 column < columnCount;
                 column++) {

                String value =
                        getCell(row, column);

                table.append(" ")
                        .append(escape(value))
                        .append(" |");
            }

            table.append("\n");
        }

        return table.toString().stripTrailing();
    }

    /*
     * Safely get a cell.
     */
    private String getCell(
            List<String> row,
            int index
    ) {

        if (row == null ||
                index >= row.size()) {

            return "";
        }

        String value = row.get(index);

        return value == null ? "" : value;
    }

    /*
     * Escape Markdown's column separator.
     */
    private String escape(String value) {

        return value
                .replace("|", "\\|")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    @Override
    public boolean supportsMimeType(String mimeType) {
        return supportedMimeTypes.contains(mimeType);
    }

    @Override
    public String getText(String mimeType, Path filePath) throws Exception {
        if (!supportsMimeType(mimeType)) {
            throwUnsupportedMimeTypeException(mimeType);
        }

        if (mimeType.equals("application/vnd.ms-excel") ||
                mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            return extractExcel(filePath);
        }

        return extractCsv(filePath);
    }

    /*
     * Get extension without depending
     * on Windows/Linux path separators.
     */
   /* private String getExtension(String filename) {

        int dot =
                filename.lastIndexOf('.');

        if (dot < 0 ||
                dot == filename.length() - 1) {

            return "";
        }

        return filename
                .substring(dot + 1)
                .toLowerCase(Locale.ROOT);
    }


    public String extract(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String filename = file.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("File name is missing");
        }

        String extension = getExtension(filename);

        return switch (extension) {

            case "csv" -> extractCsv(file);

            case "xlsx", "xls" -> extractExcel(file);

            default -> throw new IllegalArgumentException(
                    "Unsupported file type: " + extension
            );
        };
    }*/

}