import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("Duplicates")
public class GraphLoader {
/* ------ Statiske variabler------ */
    private final static File DATABASE_DIRECTORY =
            (System.getProperty("os.name").startsWith("Windows")) ?
                    new File("some\\laughable\\fucking\\shit") :
                    new File("/var/lib/neo4j/data/databases/administrative-enheter.db");
    private final static String RES_JSON = "./src/res/json/";

    private final static String DETACH_DELETE = "MATCH (n) DETACH DELETE n";

    private static final String NAVN = "navn", KODE = "kode";

/* ------ Objekt-variabler ------ */
    final GraphDatabaseFactory dbFactory;
    final GraphDatabaseBuilder builder;
    final GraphDatabaseService graphdb;

    final Logger graphLogger;

    GraphLoader() {
        dbFactory = new GraphDatabaseFactory();
        builder = dbFactory.newEmbeddedDatabaseBuilder(DATABASE_DIRECTORY);
        graphdb = builder.newGraphDatabase();

        graphLogger = Logger.getLogger("GraphLoader");

        registerShutdownHook(graphdb);
    }


    // todo- Feilhåndtering
    public void registerDatasets() {
        LibGraph.graphTransaction(this.graphdb, txCmd -> {
            graphdb.execute(DETACH_DELETE); // For å fjerne all eksisterende data

            Node nodeNorge = graphdb.createNode(NodeType.Land);
            nodeNorge.setProperty(NAVN, "Norge");
            nodeNorge.setProperty(KODE, "NO");

            // ArrayList for alle fylkenoder.
            ArrayList<Node> fylkeNoder = new ArrayList<>();

            // Peker til mappe som inneholder json-ressurser
            File res = new File(RES_JSON);

            HashMap<String, String> fylkeMap = LibJSON.getFylker(new File(res,"fylker.json"));
            for (String key : fylkeMap.keySet()) {
                Node node = graphdb.createNode(NodeType.Fylke);
                node.setProperty(NAVN, fylkeMap.get(key));
                node.setProperty(KODE, key);
                nodeNorge.createRelationshipTo(node, RelationType.HAR_FYLKE);
                fylkeNoder.add(node);
            }

            // Gjennomløp fylkedata
            for (Node fylke : fylkeNoder) {
                String isoKode = fylke.getProperty(KODE).toString();

                // Sjekk om fil eksisterer
                File fylkeFil = new File(res, "fylker/".concat(isoKode).concat(".geojson"));
                graphLogger.log(Level.INFO, "Fil eksisterer:"+fylkeFil.exists());
                graphLogger.log(Level.INFO, "Filsti:" + fylkeFil.getAbsolutePath());

                // Hent JSON data
                TreeSet<Koordinat> koordinater = new TreeSet<>();

                Node koordinatNodePrevious = null, koordinatNodeCurrent = null;

                final JSONObject json_fylkeFil = new JSONObject(LibJSON.readFile(fylkeFil).toString());
                final JSONObject json_fylke = json_fylkeFil .getJSONObject("administrative_enheter.fylkesgrense");
                final JSONObject json_kommune = json_fylkeFil .getJSONObject("administrative_enheter.kommunegrense");;

                // Objekter for gjenbruk
                JSONArray features;
                JSONArray coordinates;
                JSONArray coordinateContainer;

                Node koordinatFylkeStart = null;
                /* -------- Innlesing a fylkedata -------- */
                features = json_fylke.getJSONArray("features");
                for (int i = 0; i < features.length(); i++) {
                    coordinates = features
                            .getJSONObject(i) // {} 0, 1, 2, 3, 4
                            .getJSONObject("geometry") // {} geometry
                            .getJSONArray("coordinates"); // [] coordinates

                    coordinateContainer = coordinates.getJSONArray(0);

                    Koordinat startKoordinat = new Koordinat(
                            coordinateContainer.getDouble(0),
                            coordinateContainer.getDouble(1)
                    );
                    if (koordinater.add(startKoordinat)) {
                        koordinatNodeCurrent = graphdb.createNode(NodeType.Koordinat);
                        koordinatNodeCurrent.setProperty("lat", startKoordinat.lat);
                        koordinatNodeCurrent.setProperty("lon", startKoordinat.lng);
                        fylke.createRelationshipTo(koordinatNodeCurrent, RelationType.HAR_KOORDINAT);
                        if (koordinatNodePrevious != null)
                            koordinatNodePrevious.createRelationshipTo(koordinatNodeCurrent, RelationType.NESTE_PUNKT);
                    }

                    if (koordinatFylkeStart == null)
                        koordinatFylkeStart = koordinatNodeCurrent;

                    for (int j = 1; j < coordinates.length(); j++) {
                        coordinateContainer = coordinates.getJSONArray(j); // [] 0, 1, 2, 3, 4

                        Koordinat tmpKoordinat = new Koordinat(
                            coordinateContainer.getDouble(0),
                            coordinateContainer.getDouble(1)
                        );
                        if (koordinater.add(tmpKoordinat)) {
                            koordinatNodePrevious = koordinatNodeCurrent;
                            koordinatNodeCurrent = graphdb.createNode(NodeType.Koordinat);
                            koordinatNodeCurrent.setProperty("lat", tmpKoordinat.lat);
                            koordinatNodeCurrent.setProperty("lon", tmpKoordinat.lng);
                            koordinatNodePrevious.createRelationshipTo(koordinatNodeCurrent, RelationType.NESTE_PUNKT);
                            fylke.createRelationshipTo(koordinatNodeCurrent, RelationType.HAR_KOORDINAT);
                        }
                    }
                }
                if (koordinatNodeCurrent != null)
                koordinatNodeCurrent.createRelationshipTo(koordinatFylkeStart, RelationType.NESTE_PUNKT);
                koordinatFylkeStart.setProperty("start", "start");

                System.out.println("Final size: "+koordinater.size());

                /* -------- Innlessing av kommunedata -------- */

/*
                features = json_kommune.getJSONArray("features");
                for (int i = 0; i < features.length(); i++) {
                    coordinates = features
                            .getJSONObject(i) // {} 0, 1, 2, 3, 4
                            .getJSONObject("geometry") // {} geometry
                            .getJSONArray("coordinates"); // [] coordinates

                    coordinateContainer = coordinates.getJSONArray(0);

                    Koordinat startKoordinat = new Koordinat(
                            coordinateContainer.getDouble(0),
                            coordinateContainer.getDouble(1)
                    );
                    if (koordinater.add(startKoordinat)) {
                        koordinatNodeCurrent = graphdb.createNode(NodeType.Koordinat);
                        koordinatNodeCurrent.setProperty("lat", startKoordinat.lat);
                        koordinatNodeCurrent.setProperty("lon", startKoordinat.lng);
                        fylke.createRelationshipTo(koordinatNodeCurrent, RelationType.HAR_KOORDINAT);
                        if (koordinatNodePrevious != null)
                            koordinatNodePrevious.createRelationshipTo(koordinatNodeCurrent, RelationType.NESTE_PUNKT);
                    }
                }*/
            }
        });
    }
    void registerDummyDatasets() {
        LibGraph.graphTransaction(this.graphdb, txCmd -> {
            graphdb.execute(DETACH_DELETE); // For å fjerne all eksisterende data

            Node nodeLand = graphdb.createNode(NodeType.Land);

            ArrayList<Node> fylkeNoder = new ArrayList<>();

            File fylkeFil1 = new File(RES_JSON+"fylker_dummy/A.geojson");
            File fylkeFil2 = new File(RES_JSON+"fylker_dummy/B.geojson");
            File fylkeFil3 = new File(RES_JSON+"fylker_dummy/C.geojson");

            Node fn1 = graphdb.createNode(NodeType.Fylke);
            fn1.setProperty(NAVN, "A");
            Node fn3 = graphdb.createNode(NodeType.Fylke);
            fn1.setProperty(NAVN, "B");
            Node fn2 = graphdb.createNode(NodeType.Fylke);
            fn1.setProperty(NAVN, "C");
            fylkeNoder.add(fn1); fylkeNoder.add(fn2); fylkeNoder.add(fn3);

            TreeSet<Koordinat> koordinater = new TreeSet<>();

            for (Node fylke : fylkeNoder) {

            }
        });
    }

    private void registerShutdownHook(final GraphDatabaseService graphDb) {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }
    public enum NodeType implements Label {
        Koordinat,
        Kommune,
        Fylke,
        Land,
        LineString,
        FylkeGrense
    }

    public enum RelationType implements RelationshipType {
        HAR_KOMMUNE,
        HAR_FYLKE,
        HAR_FYLKEGRENSE,
        HAR_LINESTRING,
        NESTE_PUNKT,
        HAR_KOORDINAT
    }
}
