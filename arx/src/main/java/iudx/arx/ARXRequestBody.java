package iudx.arx;

public class ARXRequestBody {
    private int k;
    private String suppress_columns;
    private String pseudonymize_columns;
    private String insensitive_columns;
    private String allow_record_suppression;

    // Getters and setters
    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
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

    public void setPseudonymize_columns(String pseudonymize_columns) {
        this.pseudonymize_columns = pseudonymize_columns;
    }

    public String getInsensitive_columns() {
        return insensitive_columns;
    }

    public void setInsensitive_columns(String insensitive_columns) {
        this.insensitive_columns = insensitive_columns;
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