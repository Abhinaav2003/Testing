package com.example.exam.service;

import com.example.exam.model.Exam;
import com.example.exam.model.ExamAnswer;
import com.example.exam.model.ExamResult;
import com.example.exam.model.Question;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the Excel (admin marks sheet) and PDF (student answer sheet) downloads.
 */
@Service
public class ExportService {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // Same pass mark the admin results page uses
    private static final double ADMIN_PASS_PERCENT = 50.0;

    // ==================== EXCEL: ALL STUDENT MARKS FOR ONE QUIZ ====================

    @Transactional(readOnly = true)
    public byte[] buildMarksWorkbook(Exam exam, List<ExamResult> results) throws IOException {

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Marks");

            // ---- Styles ----
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle percentStyle = workbook.createCellStyle();
            percentStyle.setDataFormat(workbook.createDataFormat().getFormat("0.0"));

            // ---- Title rows ----
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Quiz: " + exam.getTitle());
            titleCell.setCellStyle(titleStyle);

            sheet.createRow(1).createCell(0)
                    .setCellValue("Total submissions: " + results.size());

            // ---- Header ----
            String[] headers = {
                    "Rank", "Student Name", "Username", "Mobile",
                    "Marks Obtained", "Total Marks", "Percentage (%)",
                    "Result", "Time Taken", "Submitted At"
            };

            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // ---- Data (highest marks first) ----
            List<ExamResult> sorted = results.stream()
                    .sorted(Comparator.comparingInt(ExamResult::getScoreAchieved).reversed())
                    .toList();

            int rowIndex = 4;
            int rank = 1;
            for (ExamResult r : sorted) {
                Row row = sheet.createRow(rowIndex++);
                double percent = r.getTotalMarks() > 0
                        ? r.getScoreAchieved() * 100.0 / r.getTotalMarks() : 0;

                row.createCell(0).setCellValue(rank++);
                row.createCell(1).setCellValue(nullSafe(r.getStudent().getFullName()));
                row.createCell(2).setCellValue(nullSafe(r.getStudent().getUsername()));
                row.createCell(3).setCellValue(nullSafe(r.getStudent().getMobileNumber()));
                row.createCell(4).setCellValue(r.getScoreAchieved());
                row.createCell(5).setCellValue(r.getTotalMarks());

                Cell pct = row.createCell(6);
                pct.setCellValue(Math.round(percent * 10) / 10.0);
                pct.setCellStyle(percentStyle);

                row.createCell(7).setCellValue(
                        r.getTotalMarks() <= 0 ? "N/A"
                                : (percent >= ADMIN_PASS_PERCENT ? "Pass" : "Fail"));
                row.createCell(8).setCellValue(formatDuration(r.getDurationSeconds()));
                row.createCell(9).setCellValue(
                        r.getSubmissionTime() != null
                                ? r.getSubmissionTime().format(DATE_TIME) : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            sheet.createFreezePane(0, 4);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ==================== PDF: ONE STUDENT'S ANSWER SHEET ====================

    @Transactional(readOnly = true)
    public byte[] buildAnswerSheetPdf(ExamResult result) throws DocumentException {

        Exam exam = result.getExam();
        List<Question> questions = exam.getQuestions();

        Map<Long, Integer> studentAnswers = new HashMap<>();
        for (ExamAnswer a : result.getAnswers()) {
            studentAnswers.put(a.getQuestion().getId(), a.getSelectedOption());
        }

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
        Font questionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font okFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(0x12, 0x80, 0x5C));
        Font badFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(0xB4, 0x23, 0x18));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(document, out);
        document.open();

        // ---- Heading ----
        Paragraph title = new Paragraph("Answer Sheet", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph examTitle = new Paragraph(exam.getTitle(), boldFont);
        examTitle.setAlignment(Element.ALIGN_CENTER);
        examTitle.setSpacingAfter(12);
        document.add(examTitle);

        // ---- Student / result summary ----
        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(100);
        info.setSpacingAfter(14);

        double percent = result.getTotalMarks() > 0
                ? result.getScoreAchieved() * 100.0 / result.getTotalMarks() : 0;

        addInfoRow(info, "Student", nullSafe(result.getStudent().getFullName()), boldFont, normalFont);
        addInfoRow(info, "Username", nullSafe(result.getStudent().getUsername()), boldFont, normalFont);
        addInfoRow(info, "Score",
                result.getScoreAchieved() + " / " + result.getTotalMarks()
                        + (result.getTotalMarks() > 0 ? String.format("  (%.1f%%)", percent) : ""),
                boldFont, normalFont);
        addInfoRow(info, "Time taken", formatDuration(result.getDurationSeconds()), boldFont, normalFont);
        addInfoRow(info, "Submitted",
                result.getSubmissionTime() != null ? result.getSubmissionTime().format(DATE_TIME) : "-",
                boldFont, normalFont);
        document.add(info);

        // ---- Questions ----
        int number = 1;
        for (Question q : questions) {
            Integer selectedObj = studentAnswers.get(q.getId());
            int selected = selectedObj == null ? 0 : selectedObj;
            boolean correct = selected == q.getCorrectAnswer();

            Paragraph qp = new Paragraph(
                    "Q" + number++ + ". " + q.getText() + "  (Marks: " + q.getMarks() + ")",
                    questionFont);
            qp.setSpacingBefore(8);
            qp.setSpacingAfter(4);
            qp.setKeepTogether(true);
            document.add(qp);

            String[] options = {q.getOption1(), q.getOption2(), q.getOption3(), q.getOption4()};

            for (int i = 1; i <= 4; i++) {
                boolean isCorrectOpt = q.getCorrectAnswer() == i;
                boolean isStudentOpt = selected == i;

                Font font = normalFont;
                String tag = "";
                if (isCorrectOpt && isStudentOpt) {
                    font = okFont;
                    tag = "   [Your answer - Correct]";
                } else if (isCorrectOpt) {
                    font = okFont;
                    tag = "   [Correct answer]";
                } else if (isStudentOpt) {
                    font = badFont;
                    tag = "   [Your answer - Wrong]";
                }

                Paragraph op = new Paragraph(
                        (char) ('A' + i - 1) + ") " + nullSafe(options[i - 1]) + tag, font);
                op.setIndentationLeft(18);
                document.add(op);
            }

            Paragraph status;
            if (selected == 0) {
                status = new Paragraph("Not answered  |  Marks awarded: 0", smallFont);
            } else if (correct) {
                status = new Paragraph("Correct  |  Marks awarded: " + q.getMarks(), okFont);
            } else {
                status = new Paragraph("Wrong  |  Marks awarded: 0", badFont);
            }
            status.setIndentationLeft(18);
            status.setSpacingBefore(2);
            document.add(status);
        }

        document.close();
        return out.toByteArray();
    }

    // ==================== HELPERS ====================

    private void addInfoRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell l = new PdfPCell(new Phrase(label, labelFont));
        PdfPCell v = new PdfPCell(new Phrase(value, valueFont));
        l.setPadding(5);
        v.setPadding(5);
        table.addCell(l);
        table.addCell(v);
    }

    private String formatDuration(Long seconds) {
        if (seconds == null || seconds <= 0) {
            return "Not available";
        }
        return (seconds / 60) + " min " + (seconds % 60) + " sec";
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    /** Makes a string safe to use inside a download file name. */
    public static String safeFileName(String name) {
        String cleaned = name == null ? "file" : name.replaceAll("[^a-zA-Z0-9-_]+", "_");
        return cleaned.replaceAll("^_+|_+$", "").isEmpty() ? "file" : cleaned.replaceAll("^_+|_+$", "");
    }
}
