package sih2026.agent.tools;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import sih2026.agent.model.vision.OcrAndVisionModel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

@Service
public class OcrAndVisionOnPdf {
    private final OcrAndVisionModel ocrAndVisionModel;

    public OcrAndVisionOnPdf(OcrAndVisionModel ocrAndVisionModel) {
        this.ocrAndVisionModel = ocrAndVisionModel;
    }

    /**
     * Process every page of a PDF.
     */
    public String process(
            Path pdfPath,
            String prompt
    ) throws IOException, InterruptedException {

        try (PDDocument document =
                     Loader.loadPDF(pdfPath.toFile())) {

            PDFRenderer renderer =
                    new PDFRenderer(document);

            int pageCount =
                    document.getNumberOfPages();

            System.out.println(
                    "PDF pages: " + pageCount
            );

            StringBuilder finalText = new StringBuilder();

            for (int pageNumber = 0;
                 pageNumber < pageCount;
                 pageNumber++) {

                System.out.println();
                System.out.println(
                        "================================"
                );

                System.out.println(
                        "Processing page "
                                + (pageNumber + 1)
                                + "/"
                                + pageCount
                );

                System.out.println(
                        "================================"
                );

                /*
                 * Render ONLY the current page.
                 *
                 * PDF page index is zero-based.
                 */
                BufferedImage image =
                        renderer.renderImageWithDPI(
                                pageNumber,
                                150,
                                ImageType.RGB
                        );

                /*
                 * Convert BufferedImage → PNG bytes
                 */
                byte[] imageBytes =
                        imageToPng(image);

                /*
                 * Send page to llama.cpp
                 */
                String response =
                        ocrAndVisionModel.sendMessage(
                                prompt,
                                new ByteArrayInputStream(imageBytes),
                                "image/png"
                        );

                System.out.println();
                System.out.println("LLM RESPONSE:");
                System.out.println(response);

                /*
                 * Release image memory
                 */
                image.flush();
                finalText.append("Page ").append(pageNumber).append(":\n");
                finalText.append(response).append("\n\n");
            }
            return finalText.toString();
        }
    }

    /**
     * Convert BufferedImage to PNG byte[].
     */
    private byte[] imageToPng(
            BufferedImage image
    ) throws IOException {

        try (ByteArrayOutputStream output =
                     new ByteArrayOutputStream()) {

            boolean written =
                    ImageIO.write(
                            image,
                            "png",
                            output
                    );

            if (!written) {
                throw new IOException(
                        "Could not encode image as PNG"
                );
            }

            return output.toByteArray();
        }
    }
}