package pas.matt.bielmatksef.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import pas.matt.bielmatksef.exceptions.PdfException;
import pas.matt.bielmatksef.model.Invoice;
import pas.matt.bielmatksef.model.InvoiceValidation;
import pas.matt.bielmatksef.parser.InvoiceParser;
import pas.matt.bielmatksef.pdf.PdfService;
import pas.matt.bielmatksef.xml.XmlBuilder;

import javax.xml.stream.XMLStreamException;
import java.nio.charset.StandardCharsets;

@Controller
public class InvoiceController {

    private final PdfService pdfService;
    private final InvoiceParser invoiceParser;
    private final XmlBuilder xmlBuilder;

    public InvoiceController(PdfService pdfService, InvoiceParser invoiceParser, XmlBuilder xmlBuilder) {
        this.pdfService = pdfService;
        this.invoiceParser = invoiceParser;
        this.xmlBuilder = xmlBuilder;
    }

    @PostMapping(value = "/upload-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadPdf(@RequestParam("file") MultipartFile file, HttpSession session, Model model) {

        try {
            String text = pdfService.convertPdfToText(file);
            Invoice invoice = invoiceParser.parseInvoice(text);

            session.setAttribute("invoice", invoice);
            model.addAttribute("filename", file.getOriginalFilename());

//            model.addAttribute("pdfText", text);

            return "pdf-preview";

        } catch (PdfException e) {
            model.addAttribute("error", e.getMessage());
            return "index";
        }
    }

    @GetMapping("/edycja-faktury")
    public String invoiceEditForm(HttpSession session, Model model) {
        Invoice invoice = (Invoice) session.getAttribute("invoice");
        model.addAttribute("invoice", invoice);

        return "invoice-edit";
    }

    @PostMapping("/edycja-faktury")
    public String invoiceEdit(@ModelAttribute("invoice") Invoice invoice, HttpSession session, Model model) {

//        invoice.getInvoiceErrors().clear();
        InvoiceValidation.validateInvoice(invoice);

        session.setAttribute("invoice", invoice);
        model.addAttribute("filename", invoice.getInvoiceNumber());
        return "pdf-preview";
    }


    @PostMapping("/download-xml")
    public ResponseEntity<byte[]> downloadXml(HttpSession session)
            throws XMLStreamException {

        Invoice invoice = (Invoice) session.getAttribute("invoice");
        String fileName = invoice.getInvoiceNumber();

        byte[] xml;
        try {
            xml = xmlBuilder.generateXml(invoice);
        } catch (IllegalArgumentException e) {
            byte[] message = e.getMessage().getBytes(StandardCharsets.UTF_8);
            return ResponseEntity.badRequest()
                    .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                    .contentLength(message.length)
                    .body(message);
        }

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + ".xml\""
                )
                .contentType(MediaType.APPLICATION_XML)
                .contentLength(xml.length)
                .body(xml);
    }


}
