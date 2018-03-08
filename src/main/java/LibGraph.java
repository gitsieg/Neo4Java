import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.StoreLockException;

import java.io.File;
import java.util.*;

@SuppressWarnings("Duplicates")

class LibGraph {
    final static String JSON_PATH = "./src/res/json";

    static Fylke loadFylke(File fylke) {
        return null;
    }

    static void graphTransaction(GraphDatabaseService graphdb, TransactionCommand... commands ) {
        Transaction tx = null;
        try {
            for (TransactionCommand command : commands) {
                tx = graphdb.beginTx();
                command.performTransaction(graphdb);
                tx.success();
                tx.close();
            }
        } catch (StoreLockException e) {
            if (tx != null)
                tx.close();
        } finally {
            graphdb.shutdown();
        }
    }
    interface TransactionCommand {
        void performTransaction(GraphDatabaseService graphdb);
    }



    static void sjekkKoordinater(File fil) {
        long start = System.currentTimeMillis();
        JSONObject fylke = new JSONObject(LibJSON.readFile(fil).toString());

        TreeSet<Koordinat> koordinater = new TreeSet<>();
        int counter = 0, fylkegrenseTotal = 0, kommunegrenseTotal = 0;


        JSONObject fylkesgrense = fylke.getJSONObject("administrative_enheter.fylkesgrense");
        JSONObject kommuner = fylke.getJSONObject("administrative_enheter.kommunegrense");

        JSONArray features;
        JSONArray coordinates;
        JSONArray coordinateContainer;
        JSONArray coordinateSubContainer;

        Koordinat koordinat;

        features = fylkesgrense.getJSONArray("features");
        for (int i = 0; i < features.length(); i++) {
            coordinates = features
                    .getJSONObject(i) // {} 0, 1, 2, 3, 4
                    .getJSONObject("geometry") // {} geometry
                    .getJSONArray("coordinates"); // [] coordinates

            for (int j = 0; j < coordinates.length(); j++) {
                coordinateContainer = coordinates.getJSONArray(j); // [] 0, 1, 2, 3, 4
                koordinat = new Koordinat(
                        coordinateContainer.getFloat(0),
                        coordinateContainer.getFloat(1)
                );
                if (!koordinater.contains(koordinat)) {
                    koordinater.add(koordinat);
                    fylkegrenseTotal++;
                }
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
                koordinat = new Koordinat(
                        coordinateContainer.getFloat(0),
                        coordinateContainer.getFloat(1)
                );
                if (!koordinater.contains(koordinat)) {
                    koordinater.add(koordinat);
                    kommunegrenseTotal++;
                } else
                    counter++;
                /*
                for (int k = 0; k < coordinateContainer.length(); k++) {
                    coordinateSubContainer = coordinateContainer.getJSONArray(k);
                    if (koordinater.contains(new Koordinat(
                            coordinateSubContainer.getFloat(0),
                            coordinateSubContainer.getFloat(1)))) {
                        counter++;
                    }
                    kommunegrenseTotal++;
                }
                */
            }
        }

        long stop = System.currentTimeMillis();
        System.out.println("Time used: "+(stop-start));

        System.out.println("Fylkegrense-koordinater: "+fylkegrenseTotal);
        System.out.println("Kommunegrense-koordinater: "+kommunegrenseTotal);
        System.out.println("Equal count: "+counter);

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
class Koordinat implements Comparable<Koordinat>{
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

    @Override
    public int compareTo(Koordinat k) {
        float resultLat = lat - k.lat;
        float resultLng = lng - k.lng;

        if (resultLat < 0)
            return -1;
        else if (resultLat > 0)
            return 1;
        else
            if (resultLng < 0)
                return -1;
            else if (resultLng > 0)
                return 1;
            else
                return 0;
    }
}