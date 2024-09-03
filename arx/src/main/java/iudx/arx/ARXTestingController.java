package iudx.arx;


import org.json.JSONArray;
//import iudx.arx.service.ARXTestingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ARXResponse processProperties(
            @RequestParam String k,
            @RequestParam String suppress_columns,
            @RequestParam String pseudonymize_columns,
            @RequestParam String insensitive_columns,
            @RequestParam String allow_record_suppression
    ) throws NoSuchAlgorithmException {
        try {
            System.out.println("Value of k = " + k);
            System.out.println("Suppress Columns = " + suppress_columns);
            System.out.println("Pseudonymize Columns = " + pseudonymize_columns);
            System.out.println("Insensitive Columns = " + insensitive_columns);
            System.out.println("Allow Record Suppression = " + allow_record_suppression);
            return arxTestingService.processProperties(
                    k, suppress_columns, pseudonymize_columns, insensitive_columns, allow_record_suppression
            );
        } catch (IOException e) {
            return new ARXTestingService.ARXResponse("Failed", Collections.emptyList());
        }
    }
}