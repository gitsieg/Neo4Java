import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.graphdb.*;
import org.neo4j.kernel.StoreLockException;

import java.io.File;
import java.util.*;

@SuppressWarnings("Duplicates")

class LibGraph {
    final static String JSON_PATH = "./src/res/json";

    static void graphTransaction(GraphDatabaseService graphdb, TransactionCommand... commands ) {
        Transaction tx = null;
        try {
            for (TransactionCommand command : commands) {
                tx = graphdb.beginTx();
                command.performTransaction(graphdb);
                tx.success();
                tx.close();
            }
        } catch (StoreLockException sle) {
            System.out.println("Store lock exception");
        } catch (RuntimeException re) {
            System.out.println("Runtime exception");
        } catch (Exception e){
            System.out.println("Exception");
        } finally {
            if (tx != null)
                tx.close();
        }
    }
    interface TransactionCommand {
        void performTransaction(GraphDatabaseService graphdb) throws StoreLockException;
    }

    static void checkRelations(GraphDatabaseService graphdb) {
        Result result = graphdb.execute("match(n:Koordinat)<--(f:Fylke {navn: \"Telemark\"}) return (n) ");
        ResourceIterator<Node> it = result.columnAs("n");

        int errorCounter = 0;
        Node prev, cur;

        if (it.hasNext()) {
            cur = it.next();
            System.out.println("Lat: "+cur.getProperty("lat").getClass());
            while (it.hasNext()) {
                prev = cur;
                cur = it.next();
                if ((double)cur.getProperty("lat") == (double)prev.getProperty("lat")
                 && (double)cur.getProperty("lon") == (double)prev.getProperty("lon"))
                    errorCounter++;
            }
        } else
            System.out.println("What the actual fuck?");
        System.out.println("Error count: "+errorCounter);
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
                        coordinateContainer.getDouble(0),
                        coordinateContainer.getDouble(1)
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
                        coordinateContainer.getDouble(0),
                        coordinateContainer.getDouble(1)
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
                            coordinateSubContainer.getDouble(0),
                            coordinateSubContainer.getDouble(1)))) {
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


class Forhold implements Comparable<Forhold> {
    private final static String relationTypeExceptionMessage = "Feil RelationType. Må være"
            +"\n - RelationType.NESTE_PUNKT_FYLKE"
            +"\n - RelationType.NESTE_PUNKT_KOMMUNE";

    final Koordinat fra, til;
    final GraphLoader.RelationType type;

    public Forhold(Koordinat fra, Koordinat til, GraphLoader.RelationType type) throws IllegalArgumentException {
        if ( !(type == GraphLoader.RelationType.NESTE_PUNKT_FYLKE
                || type == GraphLoader.RelationType.NESTE_PUNKT_KOMMUNE) )
            throw new IllegalArgumentException(relationTypeExceptionMessage);
        this.fra = fra;
        this. til = til;
        this.type = type;
    }

    @Override
    public int compareTo(Forhold forhold) {
        if (this.fra.compareTo(forhold.fra) < 0)
            return -1;
        else if (this.fra.compareTo(forhold.fra) > 0)
            return 1;
        else
            if (this.til.compareTo(forhold.til) < 0)
                return -1;
            else if (this.til.compareTo(forhold.til) > 0)
                return 1;
            else
                if (this.type == GraphLoader.RelationType.NESTE_PUNKT_FYLKE
                        && forhold.type == GraphLoader.RelationType.NESTE_PUNKT_KOMMUNE)
                    return -1;
                else if (this.type == GraphLoader.RelationType.NESTE_PUNKT_KOMMUNE
                        && forhold.type == GraphLoader.RelationType.NESTE_PUNKT_FYLKE)
                    return 1;
                else
                    return 0;
    }
}
class Koordinat implements Comparable<Koordinat> {
    double lat, lon;
    Node tilkobletNode;

    public Koordinat(double lat, double lon) {
        this.lat = lat; this.lon = lon;
        tilkobletNode = null;
    }

    void kobleNode(Node node){
        if (tilkobletNode != null)
            throw new IllegalArgumentException();
        tilkobletNode = node;
        tilkobletNode.setProperty("lat", lat);
        tilkobletNode.setProperty("lon", lon);
    }
/*
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
*/
    @Override
    public int compareTo(Koordinat k) {
        double resultLat = lat - k.lat;
        double resultLng = lon - k.lon;

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
    @Override
    public String toString() {
        return "[Lat: "+lat+ "|" + "Long: "+lon+"]";
    }
}