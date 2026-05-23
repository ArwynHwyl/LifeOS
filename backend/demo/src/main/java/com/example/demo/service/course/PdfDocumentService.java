package com.example.demo.service.course;

import com.example.demo.service.exception.ValidationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
class PdfDocumentService {

    int countPages(InputStream inputStream) {
        try (PDDocument document = PDDocument.load(inputStream)) {
            return document.getNumberOfPages();
        } catch (IOException ex) {
            throw new ValidationException("Uploaded file is not a valid PDF");
        }
    }

    List<ExtractedPdfPage> extractPages(InputStream inputStream, int pageStart, int pageEnd) {
        try (PDDocument document = PDDocument.load(inputStream)) {
            int pageCount = document.getNumberOfPages();
            if (pageStart < 1 || pageEnd < pageStart || pageEnd > pageCount) {
                throw new ValidationException("Page range exceeds document page count");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            List<ExtractedPdfPage> pages = new ArrayList<>();
            for (int pageNumber = pageStart; pageNumber <= pageEnd; pageNumber++) {
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                pages.add(new ExtractedPdfPage(pageNumber, normalizeText(stripper.getText(document))));
            }
            return pages;
        } catch (IOException ex) {
            throw new ValidationException("Uploaded file is not a valid PDF");
        }
    }

    String extractText(InputStream inputStream, int pageStart, int pageEnd) {
        StringBuilder text = new StringBuilder();
        for (ExtractedPdfPage page : extractPages(inputStream, pageStart, pageEnd)) {
            if (!page.text().isBlank()) {
                if (!text.isEmpty()) {
                    text.append("\n\n");
                }
                text.append("Page ").append(page.pageNumber()).append(":\n").append(page.text());
            }
        }
        return text.toString().trim();
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    record ExtractedPdfPage(Integer pageNumber, String text) {
    }
}
