import org.deidentifier.arx.*;
import org.deidentifier.arx.aggregates.HierarchyBuilder;
import org.deidentifier.arx.aggregates.HierarchyBuilderIntervalBased;
import org.deidentifier.arx.aggregates.StatisticsFrequencyDistribution;
import org.deidentifier.arx.criteria.KAnonymity;

import java.util.*;
import java.io.IOException;
import java.nio.charset.Charset;

public class ARXTesting {
    public static void main(String[] args) throws IOException {

        Data dataset = Data.create("/Users/shashankpandey/IdeaProjects/arx-attempt/maven-arx/src/main/java/categoricalDatasetProcessed_fin_TEL_bounds_corrected_1_22.csv", Charset.defaultCharset(), ',');

        HierarchyBuilderIntervalBased<Double> builder_long = HierarchyBuilderIntervalBased.create(DataType.DECIMAL);

        for (double i = 77.0; i<82.0; i+=0.5){
            builder_long.addInterval(i,i+0.5);
        }

        builder_long.getLevel(0).addGroup(1);
        builder_long.getLevel(1).addGroup(2);
        builder_long.getLevel(2).addGroup(2);
        builder_long.getLevel(3).addGroup(2);

        HierarchyBuilderIntervalBased<Double> builder_lat = HierarchyBuilderIntervalBased.create(DataType.DECIMAL);

        for (double i = 15.0; i<20.0; i+=0.5){
            builder_lat.addInterval(i,i+0.5);
        }

        builder_lat.getLevel(0).addGroup(1);
        builder_lat.getLevel(1).addGroup(2);
        builder_lat.getLevel(2).addGroup(2);
        builder_lat.getLevel(3).addGroup(2);

        String[] identifying = new String[]{"Record","FarmerName","LandArea","KhasraNo./DagNo.","SurveyNo"};

        for (int i = 0; i<identifying.length; i++){
            dataset.getDefinition().setAttributeType(identifying[i],AttributeType.IDENTIFYING_ATTRIBUTE);
        }

        String[] insensitive = new String[]{"SampleNo.","village","pH","EC","OC","N","P","K","S","Zn","Fe","Cu","Mn","B","state","district","subdistrict","cycle"};

        for (int i = 0; i<insensitive.length; i++){
            dataset.getDefinition().setAttributeType(insensitive[i],AttributeType.INSENSITIVE_ATTRIBUTE);
        }

        dataset.getDefinition().setAttributeType("Longitude", builder_long);
        dataset.getDefinition().setAttributeType("Latitude", builder_lat);

        int k = 2;
        int[] colList = new int[]{2,23,22,21};
        List<String> colNames = new ArrayList<String>();

        for (int i = 0; i<colList.length; i++){
            colNames.add(dataset.getHandle().getAttributeName(colList[i]));
        }

        for (int i = 0; i<4; i++){

            if (i>0){
                for (int l = 0; l<i; l++){
                    dataset.getDefinition().setAttributeType(colNames.get(l),AttributeType.IDENTIFYING_ATTRIBUTE);
                }
            }

            ARXConfiguration config = ARXConfiguration.create();

            config.addPrivacyModel(new KAnonymity(k));
            config.setSuppressionLimit(0.00d);

            ARXSolverConfiguration.create();

            ARXAnonymizer anonymizer = new ARXAnonymizer();
            dataset.getHandle().release();

            System.out.println('\n');
            System.out.println("k-anonymity for k: "+k+" || Generalizing on: "+colNames.get(i));

            ARXResult result = anonymizer.anonymize(dataset, config);

            DataHandle handle = result.getOutput();
            DataHandle view = handle.getView();


            StatisticsFrequencyDistribution anonResult = view.getStatistics().getFrequencyDistribution(colList[i]);

            String[] vals  = anonResult.values;
            double[] counts = anonResult.frequency;

            List<String> valList = new ArrayList<>();
            List<Double> countList = new ArrayList<>();

            List<Pair<String, Double>> pairList = new ArrayList<>();
        //List<OutputVals> dist_freq = new List<OutputVals>();


        //Map<String, Double> dist_freq= new HashMap<String, Double>();
            for(int j = 0; j<vals.length; j++){
           // dist_freq.put(sortedVals[i],sortedCounts[i]);
                valList.add(vals[j]);
                countList.add(counts[j]);
                pairList.add(new Pair<>(vals[j],counts[j]));

            }
            Collections.sort(pairList, Comparator.comparing(Pair::getValue));

            if ((pairList.get(0).getValue())*anonResult.count >= k){
                String fileName = "categoricalDataset_"+k+"Anonymized_"+handle.getAttributeName(colList[i])+"level.csv";
                System.out.println("k-anonymity successful for k: "+k+", on "+ handle.getAttributeName(colList[i])+", saving dataset as categoricalDataset_"+k+"Anonymized_"+handle.getAttributeName(colList[i])+"level.csv");
                handle.save(fileName,',');
            }
            else {
                System.out.println("k-anonymity failed for k: "+k+", on "+ handle.getAttributeName(colList[i]));
                continue;
            }

        }
    }

    static class Pair<K, V> {
        private final K key;
        private final V value;

        Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }

}