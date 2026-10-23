package pas.matt.bielmatksef.web;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import pas.matt.bielmatksef.exceptions.PdfException;
import pas.matt.bielmatksef.pdf.PdfService;

@Controller
public class HomeController {


    @GetMapping("/")
    public String home() {
        return "index";
    }

}
