package com.careerpilot.infrastructure.document;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * infrastructure/document/AtsDocumentRenderer.java
 *
 * Converts a structured ResumeJson into ATS-safe PDF (Apache PDFBox 3.x)
 * and DOCX (docx4j) formats.
 *
 * ATS safety constraints enforced here — NOT by the AI:
 *   - Single column layout only
 *   - No tables, no text boxes, no borders
 *   - No images, no icons
 *   - Standard fonts: Helvetica (PDF), Calibri (DOCX) — both ATS-safe
 *   - No headers/footers containing PII
 *   - Bullet character: ASCII hyphen '-' (not Unicode •)
 *   - Section order: Summary → Skills → Experience → Education → Certifications
 */
@Slf4j
@Component
public class AtsDocumentRenderer {

    // PDF layout constants
    private static final float MARGIN = 50f;
    private static final float PAGE_WIDTH = PDRectangle.LETTER.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.LETTER.getHeight();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final float LINE_HEIGHT_BODY = 14f;
    private static final float LINE_HEIGHT_HEADER = 18f;
    private static final float SECTION_GAP = 8f;
    private static final float FONT_SIZE_HEADING = 13f;
    private static final float FONT_SIZE_SUBHEADING = 11f;
    private static final float FONT_SIZE_BODY = 10f;

    // ── Public API ────────────────────────────────────────────────────────────

    public AtsRenderedDocument render(ResumeJson resume, String candidateName) {
        byte[] pdf = renderPdf(resume, candidateName);
        byte[] docx = renderDocx(resume, candidateName);
        return AtsRenderedDocument.of(pdf, docx);
    }

    // ── PDF Rendering (Apache PDFBox 3.x) ────────────────────────────────────

    private byte[] renderPdf(ResumeJson resume, String candidateName) {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                // Candidate name (largest heading)
                y = writeLine(cs, candidateName, fontBold, 16f, MARGIN, y, LINE_HEIGHT_HEADER);
                y -= SECTION_GAP;

                // Summary
                if (resume.getSummary() != null && !resume.getSummary().isBlank()) {
                    y = writeSectionHeader(cs, "SUMMARY", fontBold, MARGIN, y);
                    y = writeWrappedText(cs, resume.getSummary(), fontRegular, FONT_SIZE_BODY,
                            MARGIN, y, CONTENT_WIDTH, LINE_HEIGHT_BODY);
                    y -= SECTION_GAP;
                }

                // Skills
                if (resume.getSkills() != null && !resume.getSkills().isEmpty()) {
                    y = writeSectionHeader(cs, "SKILLS", fontBold, MARGIN, y);
                    String skillLine = String.join(" - ", resume.getSkills());
                    y = writeWrappedText(cs, skillLine, fontRegular, FONT_SIZE_BODY,
                            MARGIN, y, CONTENT_WIDTH, LINE_HEIGHT_BODY);
                    y -= SECTION_GAP;
                }

                // Experience
                if (resume.getExperience() != null && !resume.getExperience().isEmpty()) {
                    y = writeSectionHeader(cs, "EXPERIENCE", fontBold, MARGIN, y);
                    for (ResumeJson.ExperienceEntry exp : resume.getExperience()) {
                        y = writeLine(cs, exp.getTitle() + " | " + exp.getCompany() + " | " + exp.getDuration(),
                                fontBold, FONT_SIZE_SUBHEADING, MARGIN, y, LINE_HEIGHT_BODY);
                        if (exp.getBullets() != null) {
                            for (String bullet : exp.getBullets()) {
                                y = writeWrappedText(cs, "- " + bullet, fontRegular, FONT_SIZE_BODY,
                                        MARGIN + 10f, y, CONTENT_WIDTH - 10f, LINE_HEIGHT_BODY);
                            }
                        }
                        y -= 4f;
                    }
                    y -= SECTION_GAP;
                }

                // Education
                if (resume.getEducation() != null && !resume.getEducation().isEmpty()) {
                    y = writeSectionHeader(cs, "EDUCATION", fontBold, MARGIN, y);
                    for (ResumeJson.EducationEntry edu : resume.getEducation()) {
                        y = writeLine(cs, edu.getDegree() + " | " + edu.getInstitution() + " | " + edu.getYear(),
                                fontRegular, FONT_SIZE_BODY, MARGIN, y, LINE_HEIGHT_BODY);
                    }
                    y -= SECTION_GAP;
                }

                // Certifications
                if (resume.getCertifications() != null && !resume.getCertifications().isEmpty()) {
                    y = writeSectionHeader(cs, "CERTIFICATIONS", fontBold, MARGIN, y);
                    for (String cert : resume.getCertifications()) {
                        y = writeLine(cs, "- " + cert, fontRegular, FONT_SIZE_BODY, MARGIN, y, LINE_HEIGHT_BODY);
                    }
                }
            }

            doc.save(out);
            return out.toByteArray();

        } catch (Exception e) {
            log.error("PDF rendering failed", e);
            throw new RuntimeException("Failed to render ATS PDF", e);
        }
    }

    private float writeSectionHeader(PDPageContentStream cs, String title,
                                      PDType1Font font, float x, float y) throws Exception {
        float newY = writeLine(cs, title, font, FONT_SIZE_HEADING, x, y, LINE_HEIGHT_HEADER);
        // Underline via a thin horizontal line
        cs.setLineWidth(0.5f);
        cs.moveTo(x, newY + 2f);
        cs.lineTo(x + CONTENT_WIDTH, newY + 2f);
        cs.stroke();
        return newY - 2f;
    }

    private float writeLine(PDPageContentStream cs, String text,
                             PDType1Font font, float fontSize,
                             float x, float y, float lineHeight) throws Exception {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
        return y - lineHeight;
    }

    private float writeWrappedText(PDPageContentStream cs, String text,
                                    PDType1Font font, float fontSize,
                                    float x, float y, float maxWidth, float lineHeight) throws Exception {
        List<String> lines = wrapText(text, font, fontSize, maxWidth);
        for (String line : lines) {
            y = writeLine(cs, line, font, fontSize, x, y, lineHeight);
        }
        return y;
    }

    private List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth) throws Exception {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            float width = font.getStringWidth(candidate) / 1000 * fontSize;
            if (width > maxWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    /** Strip non-ASCII characters that PDFBox standard fonts cannot encode */
    private String sanitize(String text) {
        if (text == null) return "";
        return text.replaceAll("[^\\x00-\\x7F]", "");
    }

    // ── DOCX Rendering (docx4j) ───────────────────────────────────────────────

    private byte[] renderDocx(ResumeJson resume, String candidateName) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            WordprocessingMLPackage pkg = WordprocessingMLPackage.createPackage();
            MainDocumentPart mdp = pkg.getMainDocumentPart();

            ObjectFactory factory = new ObjectFactory();

            // Set narrow margins for ATS readability
            setMargins(mdp, factory);

            // Candidate name
            mdp.addParagraphOfText(candidateName);
            styleLastParagraph(mdp, factory, "Heading1", true);

            // Summary
            if (resume.getSummary() != null && !resume.getSummary().isBlank()) {
                addDocxSectionHeader(mdp, factory, "SUMMARY");
                mdp.addParagraphOfText(resume.getSummary());
            }

            // Skills
            if (resume.getSkills() != null && !resume.getSkills().isEmpty()) {
                addDocxSectionHeader(mdp, factory, "SKILLS");
                mdp.addParagraphOfText(String.join(" - ", resume.getSkills()));
            }

            // Experience
            if (resume.getExperience() != null) {
                addDocxSectionHeader(mdp, factory, "EXPERIENCE");
                for (ResumeJson.ExperienceEntry exp : resume.getExperience()) {
                    mdp.addParagraphOfText(exp.getTitle() + " | " + exp.getCompany() + " | " + exp.getDuration());
                    styleLastParagraph(mdp, factory, null, true);
                    if (exp.getBullets() != null) {
                        for (String bullet : exp.getBullets()) {
                            mdp.addParagraphOfText("- " + bullet);
                        }
                    }
                }
            }

            // Education
            if (resume.getEducation() != null && !resume.getEducation().isEmpty()) {
                addDocxSectionHeader(mdp, factory, "EDUCATION");
                for (ResumeJson.EducationEntry edu : resume.getEducation()) {
                    mdp.addParagraphOfText(edu.getDegree() + " | " + edu.getInstitution() + " | " + edu.getYear());
                }
            }

            // Certifications
            if (resume.getCertifications() != null && !resume.getCertifications().isEmpty()) {
                addDocxSectionHeader(mdp, factory, "CERTIFICATIONS");
                for (String cert : resume.getCertifications()) {
                    mdp.addParagraphOfText("- " + cert);
                }
            }

            pkg.save(out);
            return out.toByteArray();

        } catch (Exception e) {
            log.error("DOCX rendering failed", e);
            throw new RuntimeException("Failed to render ATS DOCX", e);
        }
    }

    private void addDocxSectionHeader(MainDocumentPart mdp, ObjectFactory factory, String title) throws Exception {
        mdp.addParagraphOfText(title);
        styleLastParagraph(mdp, factory, "Heading2", true);
    }

    private void styleLastParagraph(MainDocumentPart mdp, ObjectFactory factory,
                                     String styleId, boolean bold) {
        List<Object> content = mdp.getContent();
        if (content.isEmpty()) return;
        Object last = content.get(content.size() - 1);
        if (!(last instanceof P p)) return;

        PPr ppr = p.getPPr();
        if (ppr == null) {
            ppr = factory.createPPr();
            p.setPPr(ppr);
        }
        if (styleId != null) {
            PPrBase.PStyle pStyle = factory.createPPrBasePStyle();
            pStyle.setVal(styleId);
            ppr.setPStyle(pStyle);
        }
        if (bold) {
            for (Object run : p.getContent()) {
                if (run instanceof R r) {
                    RPr rpr = r.getRPr();
                    if (rpr == null) {
                        rpr = factory.createRPr();
                        r.setRPr(rpr);
                    }
                    BooleanDefaultTrue boldVal = factory.createBooleanDefaultTrue();
                    rpr.setB(boldVal);
                }
            }
        }
    }

    private void setMargins(MainDocumentPart mdp, ObjectFactory factory) throws Exception {
        Body body = mdp.getContents().getBody();
        SectPr sectPr = body.getSectPr();
        if (sectPr == null) {
            sectPr = factory.createSectPr();
            body.setSectPr(sectPr);
        }
        SectPr.PgMar margins = factory.createSectPrPgMar();
        // 720 twips = 0.5 inch, 1440 = 1 inch
        BigInteger margin720 = BigInteger.valueOf(720);
        margins.setTop(margin720);
        margins.setBottom(margin720);
        margins.setLeft(margin720);
        margins.setRight(margin720);
        sectPr.setPgMar(margins);
    }
}
