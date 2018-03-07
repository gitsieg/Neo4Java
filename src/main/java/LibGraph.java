import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

class LibGraph {
    final static String JSON_PATH = "./src/res/json";

    static Fylke loadFylke(File fylke) {
        return null;
    }

    static void graphTransaction(GraphDatabaseService graphdb, TransactionCommand... commands ) {
        Transaction tx = graphdb.beginTx();

        for (TransactionCommand command : commands)
            command.performTransaction(graphdb);

        tx.success();
        tx.close();
    }

    static void sjekkKoordinater(File fil) {
        JSONObject fylke = new JSONObject(LibJSON.readJSON(fil).toString());

        HashSet<Koordinat> koordinater = new HashSet<>();
        int counter = 0, fylkegrenseTotal = 0, kommunegrenseTotal = 0;

        JSONObject fylkesgrense = fylke.getJSONObject("administrative_enheter.fylkesgrense");
        JSONObject kommuner = fylke.getJSONObject("administrative_enheter.kommune");

        JSONArray features;
        JSONArray coordinates;
        JSONArray coordinateContainer;
        JSONArray coordinateSubContainer;

        features = fylkesgrense.getJSONArray("features");
        for (int i = 0; i < features.length(); i++) {
            coordinates = features
                    .getJSONObject(i) // {} 0, 1, 2, 3, 4
                    .getJSONObject("geometry") // {} geometry
                    .getJSONArray("coordinates"); // [] coordinates

            for (int j = 0; j < coordinates.length(); j++) {
                coordinateContainer = coordinates.getJSONArray(j); // [] 0, 1, 2, 3, 4
                koordinater.add(new Koordinat(
                        coordinateContainer.getFloat(0),
                        coordinateContainer.getFloat(1)
                ));
                fylkegrenseTotal++;
            }
        }

        features = kommuner.getJSONArray("features");
        for (int i = 0; i < features.length(); i++) {
            coordinates = features
                    .getJSONObject(i)
                    .getJSONObject("geometry")
                    .getJSONArray("coordinates");
            for (int j = 0; j < coordinates.length(); j++) {
                coordinateContainer = coordinates.getJSONArray(j);
                for (int k = 0; k < coordinateContainer.length(); k++) {
                    coordinateSubContainer = coordinateContainer.getJSONArray(k);
                    if (koordinater.contains(new Koordinat(
                            coordinateSubContainer.getFloat(0),
                            coordinateSubContainer.getFloat(1)))) {
                        counter++;
                    }
                    kommunegrenseTotal++;
                }
            }
        }

        System.out.println("Fylkegrense-koordinater: "+fylkegrenseTotal);
        System.out.println("Kommunegrense-koordinater: "+kommunegrenseTotal);
        System.out.println("Equal count: "+counter);

    }


    interface TransactionCommand {
        void performTransaction(GraphDatabaseService graphdb);
    }
}

class Fylke {
    int kode;
    String navn;
    ArrayList<Koordinat> grensepunkter;

    private Fylke(int kode, String navn, ArrayList<Koordinat> grensepunkter, ArrayList<Kommune> kommuner) {
        this.kode = kode;
        this.navn = navn;
        this.grensepunkter = grensepunkter;
        this.kommuner = kommuner;
    }

    ArrayList<Kommune> kommuner;
}
class Kommune {
    int kode;
    String navn;
    ArrayList<Koordinat> grensepunkter;
    HashMap<String, Object> egenskaper;

    /**
     *
     * @param kode Kommunens tallkode
     * @param navn Kommunens navn
     * @param grensepunkter Koordinatparene som tilsammen utgjør kommunens grenser
     * @param egenskaper Tar vare på kommunens egenskaper i form av &lt;String, Object&gt; referanser. Det må gjøres klassetest på Object-verdien.
     */
    private Kommune(int kode, String navn, ArrayList<Koordinat> grensepunkter, HashMap<String, Object> egenskaper) {
        this.kode = kode;
        this.navn = navn;
        this.grensepunkter = grensepunkter;
        this.egenskaper = egenskaper;
    }
}
class Koordinat {
    float lat, lng;
    public Koordinat(float lat, float lng) {
        this.lat = lat; this.lng = lng;
    }
    @Override
    public int hashCode() {
        return Objects.hash(lat, lng);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Koordinat))
            return false;
        Koordinat k = (Koordinat)o;
        return (lat == k.lat && lng == k.lng);
    }
}