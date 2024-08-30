package iudx.arx;


import org.json.JSONArray;
// import iudx.arx.service.ARXTestingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import iudx.arx.ARXTestingService.ARXResponse;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

@RestController
@RequestMapping("/api/arx")
public class ARXTestingController {

    private final ARXTestingService arxTestingService;

    public ARXTestingController(ARXTestingService arxTestingService) {
        this.arxTestingService = arxTestingService;
    }

    @GetMapping("/process")
    public ARXResponse processProperties() throws NoSuchAlgorithmException {
        try {
            return arxTestingService.processProperties();
        } catch (IOException e) {
            return new ARXTestingService.ARXResponse("Failed", Collections.emptyList());
        }
    }
}