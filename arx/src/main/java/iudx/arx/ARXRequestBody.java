package iudx.arx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class ARXRequestBody {
    private String datasetType;
    private int k;
    private int l;
    private double t;
    private String suppress_columns;
    private String pseudonymize_columns;
    private String generalized_columns;
    private String insensitive_columns;
    private String sensitive_column;
    private String allow_record_suppression;
    private Map<String, Double> widths;
    private Map<String, Integer> num_levels;


    public String getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }
    // Getters and setters
    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }

    public int getL() {
        return l;
    }

    public void setL(int l) {
        this.l = l;
    }

    public double getT() {
        return t;
    }

    public void setT(double t) {
        this.t = t;
    }

    public String getSuppress_columns() {
        return suppress_columns;
    }

    public void setSuppress_columns(String suppress_columns) {
        this.suppress_columns = suppress_columns;
    }

    public String getPseudonymize_columns() {
        return pseudonymize_columns;
    }

    public String getGeneralized_columns() {
        return generalized_columns;
    }

    public void setGeneralized_columns(String generalized_columns) {
        this.generalized_columns = generalized_columns;
    }

    public void setPseudonymize_columns(String pseudonymize_columns) {
        this.pseudonymize_columns = pseudonymize_columns;
    }

    public String getInsensitive_columns() {
        return insensitive_columns;
    }

    public void setInsensitive_columns(String insensitive_columns) {
        this.insensitive_columns = insensitive_columns;
    }

    public String getSensitive_column() {
        return sensitive_column;
    }

    public void setSensitive_column(String sensitive_column) {
        this.sensitive_column = sensitive_column;
    }
    public Map<String, Double> getWidths() {
        return widths;
    }

    public void setWidths(Map<String, Double> widths) {
        this.widths = widths;
    }

    public Map<String, Integer> getNum_levels() {
        return num_levels;
    }

    public void setNum_levels(Map<String, Integer> num_levels) {
        this.num_levels = num_levels;
    }

    public String getAllow_record_suppression() {
        return allow_record_suppression;
    }

    public void setAllow_record_suppression(String allow_record_suppression) {
        this.allow_record_suppression = allow_record_suppression;
    }

    public void print_request_body(){
        System.out.println(k+" "+allow_record_suppression);
    }
}