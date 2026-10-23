package pas.matt.bielmatksef.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pas.matt.bielmatksef.exceptions.PdfException;

import java.io.IOException;

@Service
public class PdfService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;


    public String convertPdfToText(MultipartFile file) {

        verifyPdf(file);

        try (PDDocument document =
                     Loader.loadPDF(file.getBytes())) {

            if (document.isEncrypted())
                throw new PdfException("ZPDF jest zaszyfrowany.");

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setLineSeparator("\n");

            String text = stripper.getText(document);

            if (text.isBlank()) {
                throw new PdfException("Nie znaleziono tekstu w PDF. Dokument może być skanem.");
            }
            return text;

        } catch (IOException | IllegalArgumentException e) {
            throw new PdfException("Błąd podczas przetwarzania pliku PDF");
        }

    }

    private void verifyPdf(MultipartFile file) {

        if (file.isEmpty())
            throw new PdfException("Wybierz plik PDF.");

        if (file.getSize() > MAX_FILE_SIZE)
            throw new PdfException("Maksymalny rozmiar pliku to 10 MB.");

        String fileName = file.getOriginalFilename();

        if (fileName == null || !fileName.toLowerCase().endsWith(".pdf"))
            throw new PdfException("Prześlij plik z rozszerzeniem PDF.");

    }
}
