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
                Koordinat koordinat;
                Node koordinatNodePrevious, koordinatNodeCurrent, koordinatNodeStart;

                final JSONObject json_fylkeFil = new JSONObject(LibJSON.readFile(fylkeFil).toString());


                final JSONObject json_fylke = json_fylkeFil .getJSONObject("administrative_enheter.fylkesgrense");
//                final JSONObject json_kommune = json_fylkeFil .getJSONObject("administrative_enheter.kommunegrense");;

                // Objekter for gjenbruk
                JSONArray features;
                JSONArray coordinates;
                JSONArray coordinateContainer;

                /* -------- Innlesing a fylkedata -------- */
                features = json_fylke.getJSONArray("features");
                for (int i = 0; i < features.length(); i++) {
                    coordinates = features
                            .getJSONObject(i) // {} 0, 1, 2, 3, 4
                            .getJSONObject("geometry") // {} geometry
                            .getJSONArray("coordinates"); // [] coordinates

                    coordinateContainer = coordinates.getJSONArray(0);
                    koordinat = new Koordinat(
                            coordinateContainer.getDouble(0),
                            coordinateContainer.getDouble(1)
                    );
                    koordinater.add(koordinat);
                    System.out.println("Lat: "+coordinateContainer.getDouble(0)
                            +" Long: "+coordinateContainer.getDouble(1));
                    koordinatNodeStart = koordinatNodeCurrent = graphdb.createNode(NodeType.Koordinat);
                    koordinatNodeCurrent.setProperty("lat", koordinat.lat);
                    koordinatNodeCurrent.setProperty("lon", koordinat.lng);
                    fylke.createRelationshipTo(koordinatNodeCurrent, RelationType.HAR_KOORDINAT);

                    for (int j = 1; j < coordinates.length(); j++) {
                        coordinateContainer = coordinates.getJSONArray(j); // [] 0, 1, 2, 3, 4

                        koordinat = new Koordinat(
                            coordinateContainer.getDouble(0),
                            coordinateContainer.getDouble(1)
                        );
                        if (!koordinater.contains(koordinat)) {
                            koordinater.add(koordinat);
                            koordinatNodePrevious = koordinatNodeCurrent;
                            koordinatNodeCurrent = graphdb.createNode(NodeType.Koordinat);
                            koordinatNodeCurrent.setProperty("lat", koordinat.lat);
                            koordinatNodeCurrent.setProperty("lon", koordinat.lng);
                            koordinatNodePrevious.createRelationshipTo(koordinatNodeCurrent, RelationType.NESTE_PUNKT);
                            fylke.createRelationshipTo(koordinatNodeCurrent, RelationType.HAR_KOORDINAT);
                        }
                    }
                    koordinatNodeCurrent.createRelationshipTo(koordinatNodeStart, RelationType.NESTE_PUNKT);
                }

//
//                /* -------- Innlessing av kommunedata -------- */
//                features = json_kommune.getJSONArray("features");
//                for (int i = 0; i < features.length(); i++) {
//
//                }
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
