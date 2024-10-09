package iudx.arx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import org.json.JSONArray;
//import iudx.arx.service.ARXTestingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.*;
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

    @PostMapping("/process")
    public ARXResponse processProperties(
            @RequestBody ARXRequestBody request_body
    ) throws NoSuchAlgorithmException, IOException {
        try {
            String datasetType = request_body.getDatasetType();
            int k = request_body.getK();
            String suppress_columns =request_body.getSuppress_columns();
            String pseudonymize_columns = request_body.getPseudonymize_columns();
            String insensitive_columns = request_body.getInsensitive_columns();
            String allow_record_suppression = request_body.getAllow_record_suppression();
            String generalized_columns = request_body.getGeneralized_columns();
            Map<String, Double> widths = request_body.getWidths();
            Map<String, Integer> num_levels = request_body.getNum_levels();
            System.out.println(widths);
            System.out.println(num_levels);
            System.out.println("Value of k = " + k);
            System.out.println("Data type = " + datasetType);
            System.out.println("Suppress Columns = " + suppress_columns);
            System.out.println("Pseudonymize Columns = " + pseudonymize_columns);
            System.out.println("Insensitive Columns = " + insensitive_columns);
            System.out.println("Generalized Columns = " + generalized_columns);
            System.out.println("Allow Record Suppression = " + allow_record_suppression);
   
            return arxTestingService.processProperties(
                datasetType,k, suppress_columns, pseudonymize_columns, generalized_columns, insensitive_columns,widths,num_levels, allow_record_suppression
            );
        } catch (Exception e) {
            return new ARXTestingService.ARXResponse("Failed", Collections.emptyList());
        }
    }
}