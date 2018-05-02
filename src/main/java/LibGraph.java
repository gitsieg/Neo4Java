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
        void performTransaction(GraphDatabaseService graphdb) throws Exception;
    }

    static boolean addForholdToSet(Koordinat fra, Koordinat til,
                           GraphLoader.RelationType type, TreeSet<Forhold> forhold) {
        if (fra.tilkobletNode == til.tilkobletNode && fra != til)
            System.out.println("What the fucking fuck");

        if (fra.tilkobletNode == til.tilkobletNode || fra == til)
            return false;
        else
            return forhold.add(new Forhold(
                    fra, til, type
            ));
    }

}

class Forhold implements Comparable<Forhold> {
    private final static String relationTypeExceptionMessage = "Feil RelationType. Må være"
            +"\n - RelationType.NESTE_PUNKT_FYLKE"
            +"\n - RelationType.NESTE_PUNKT_KOMMUNE";

    private final Koordinat fra, til;
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

        // Guards for when a relationship has been defined between the two nodes
        if (this.fra.compareTo(forhold.til) == 0 && this.til.compareTo(forhold.fra) == 0)
            return 0;
        if (this.fra.compareTo(forhold.fra) == 0 && this.til.compareTo(forhold.til) == 0)
            return 0;

        if (this.fra.compareTo(forhold.fra) < 0)
            return -1;
        else if (this.fra.compareTo(forhold.fra) > 0)
            return 1;
        else
            if (this.til.compareTo(forhold.til) < 0)
                return -1;
            else // if (this.til.compareTo(forhold.til) > 0) // Ooold
                return 1;
    }
}
class Koordinat implements Comparable<Koordinat> {
    private double lat, lon;
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

    @Override
    public int compareTo(Koordinat k) {
        int resultLat = Double.compare(lat, k.lat);
        int resultLon = Double.compare(lon, k.lon);

        if (resultLat < 0)
            return -1;
        else if (resultLat > 0)
            return 1;
        else
            if (resultLon < 0)
                return -1;
            else if (resultLon > 0)
                return 1;
            else
                return 0;
    }
    @Override
    public String toString() {
        return "[Lat: "+lat+ "|" + "Long: "+lon+"]";
    }
}