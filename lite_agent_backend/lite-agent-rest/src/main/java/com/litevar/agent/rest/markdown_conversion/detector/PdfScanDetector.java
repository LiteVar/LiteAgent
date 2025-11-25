package com.litevar.agent.rest.markdown_conversion.detector;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@Component
public class PdfScanDetector {

    private static final int MAX_SAMPLE_PAGES = 5;
    private static final int TEXT_PAGE_THRESHOLD = 160;
    private static final int LIGHT_TEXT_THRESHOLD = 40;

    public boolean isLikelyScanned(Path pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            return isLikelyScanned(document);
        }
    }

    boolean isLikelyScanned(PDDocument document) throws IOException {
        int pageCount = document.getNumberOfPages();
        if (pageCount <= 0) {
            return false;
        }
        int sample = Math.min(MAX_SAMPLE_PAGES, pageCount);
        PDFTextStripper stripper = new PDFTextStripper();
        int totalChars = 0;
        int pagesWithText = 0;
        int pagesWithLightText = 0;
        int pagesWithImages = 0;
        for (int i = 1; i <= sample; i++) {
            stripper.setStartPage(i);
            stripper.setEndPage(i);
            int charCount = countNonWhitespaceChars(stripper, document);
            totalChars += charCount;
            if (charCount >= TEXT_PAGE_THRESHOLD) {
                pagesWithText++;
            }
            if (charCount >= LIGHT_TEXT_THRESHOLD) {
                pagesWithLightText++;
            }
            PDPage page = document.getPage(i - 1);
            if (hasImages(page.getResources(), 0)) {
                pagesWithImages++;
            }
        }

        double sampleSize = sample;
        double avgChars = totalChars / sampleSize;
        double imageRatio = pagesWithImages / sampleSize;

        boolean mostlyImages = imageRatio >= 0.6;
        boolean veryFewTextPages = pagesWithText == 0 && pagesWithLightText <= 1;
        boolean textSparse = avgChars < 120;
        boolean extremelySparse = avgChars < 25;

        return (mostlyImages && (veryFewTextPages || textSparse))
            || (pagesWithImages >= sample && pagesWithLightText == 0)
            || (extremelySparse && imageRatio >= 0.4);
    }

    private int countNonWhitespaceChars(PDFTextStripper stripper, PDDocument document) {
        try {
            String text = stripper.getText(document);
            if (text == null) {
                return 0;
            }
            return text.replaceAll("\\s+", "").length();
        } catch (IOException e) {
            log.warn("Failed to extract text while detecting scanned PDF: {}", e.getMessage());
            return 0;
        }
    }

    private boolean hasImages(PDResources resources, int depth) throws IOException {
        if (resources == null || depth > 5) {
            return false;
        }
        for (COSName name : resources.getXObjectNames()) {
            PDXObject xobject = resources.getXObject(name);
            if (xobject instanceof PDImageXObject) {
                return true;
            } else if (xobject instanceof PDFormXObject form) {
                if (hasImages(form.getResources(), depth + 1)) {
                    return true;
                }
            }
        }
        return false;
    }
}
