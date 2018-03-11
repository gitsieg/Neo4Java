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
    int lolcount = 0;
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

                final JSONObject json_fylkeFil = new JSONObject(LibJSON.readFile(fylkeFil).toString());
                final JSONObject json_fylke = json_fylkeFil .getJSONObject("administrative_enheter.fylkesgrense");
                final JSONObject json_kommuner = json_fylkeFil .getJSONObject("administrative_enheter.kommune");;
                JSONObject kommune;

                // Objekter for gjenbruk
                JSONArray features;
                JSONArray coordinates;
                JSONArray coordinateContainer;

                Node koordinatNodePrevious = null, koordinatNodeCurrent = null;
                Node koordinatFylkeStart = null, koordinatKommuneStart = null;
                /* -------- Innlesing a fylkedata -------- */
                features = json_fylke.getJSONArray("features");
                for (int i = 0, j = 0; i < 5/*features.length()*/; i++, j = 0) {
                    coordinates = features
                            .getJSONObject(i) // {} 0, 1, 2, 3, 4
                            .getJSONObject("geometry") // {} geometry
                            .getJSONArray("coordinates"); // [] coordinates

                    coordinateContainer = coordinates.getJSONArray(0);

                    Koordinat startKoordinat = new Koordinat(
                            coordinateContainer.getDouble(0),
                            coordinateContainer.getDouble(1)
                    );
                    if (koordinatNodePrevious == null) {
                        if (koordinater.add(startKoordinat)) {
                            koordinatNodeCurrent = graphdb.createNode(NodeType.Koordinat);
                            koordinatNodeCurrent.setProperty("lat", startKoordinat.lat);
                            koordinatNodeCurrent.setProperty("lon", startKoordinat.lng);
                            fylke.createRelationshipTo(koordinatNodeCurrent, RelationType.HAR_KOORDINAT);
                            if (koordinatNodePrevious != null)
                                koordinatNodePrevious.createRelationshipTo(koordinatNodeCurrent, RelationType.NESTE_PUNKT_FYLKE);

                            startKoordinat.kobleNode(koordinatNodeCurrent);
                        }
                    }

                    if (koordinatFylkeStart == null)
                        koordinatFylkeStart = koordinatNodeCurrent;

                    for ( ; j < coordinates.length(); j++) {
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
                            koordinatNodePrevious.createRelationshipTo(koordinatNodeCurrent, RelationType.NESTE_PUNKT_FYLKE);
                            fylke.createRelationshipTo(koordinatNodeCurrent, RelationType.HAR_KOORDINAT);

                            tmpKoordinat.kobleNode(koordinatNodeCurrent);
                        }
                    }
                }
                koordinatNodeCurrent.createRelationshipTo(koordinatFylkeStart, RelationType.NESTE_PUNKT_FYLKE);
                koordinatFylkeStart.setProperty("start", "start");

                System.out.println("Final size: "+koordinater.size());

                /* -------- Innlessing av kommunedata -------- */
                features = json_kommuner.getJSONArray("features");
                for (int i = 0; i < features.length(); i++) {

                    kommune = features.getJSONObject(i); // {} 0

                    Node kommuneNode = graphdb.createNode(NodeType.Kommune);
                    kommuneNode.setProperty("navn",kommune
                            .getJSONObject("properties")
                            .getJSONArray("navn")
                            .getJSONObject(0).getString("navn"));
                    fylke.createRelationshipTo(kommuneNode, RelationType.HAR_KOMMUNE);

                    coordinates = kommune
                            .getJSONObject("geometry") // {} geometry
                            .getJSONArray("coordinates") // [] coordinates
                            .getJSONArray(0); // [] 0

                    // coordinates er nå en liste over sammtlige koordinater for kommunen

                    coordinateContainer = coordinates.getJSONArray(0); // Første av [] 0, 1, 2, 3, 5 ...

                    Koordinat startKoordinat = new Koordinat(
                            coordinateContainer.getDouble(0),
                            coordinateContainer.getDouble(1)
                    );
                    if (koordinater.add(startKoordinat)) {
                        koordinatNodeCurrent = graphdb.createNode(NodeType.Koordinat);
                        koordinatNodeCurrent.setProperty("lat", startKoordinat.lat);
                        koordinatNodeCurrent.setProperty("lon", startKoordinat.lng);
                        kommuneNode.createRelationshipTo(koordinatNodeCurrent, RelationType.HAR_KOORDINAT);
                        koordinatKommuneStart = koordinatNodeCurrent;

                        startKoordinat.kobleNode(koordinatNodeCurrent);
                    } else {
                        Koordinat c = koordinater.ceiling(startKoordinat);
                        Koordinat f = koordinater.floor(startKoordinat);
                        if (c == f) {
                            kommuneNode.createRelationshipTo(c.tilkobletNode, RelationType.HAR_KOORDINAT);
                            koordinatKommuneStart = c.tilkobletNode;
                        } else
                            System.out.println("Yeah holy shit det skal være umulig, this is bad.");
                    }
//                    if (koordinatKommuneStart == null)
//                        koordinatKommuneStart = koordinatNodeCurrent;

                    for (int j = 1; j < 5/*coordinates.length()*/; j++) {
                        coordinateContainer = coordinates.getJSONArray(j);

                        Koordinat tmpKoordinat = new Koordinat(
                                coordinateContainer.getDouble(0),
                                coordinateContainer.getDouble(1)
                        );
                        if (koordinater.add(tmpKoordinat)) {
                            koordinatNodePrevious = koordinatNodeCurrent;
                            koordinatNodeCurrent = graphdb.createNode(NodeType.Koordinat);
                            koordinatNodeCurrent.setProperty("lat", tmpKoordinat.lat);
                            koordinatNodeCurrent.setProperty("lon", tmpKoordinat.lng);

                            koordinatNodePrevious.createRelationshipTo(koordinatNodeCurrent, RelationType.NESTE_PUNKT_KOMMUNE);
                            kommuneNode.createRelationshipTo(koordinatNodeCurrent, RelationType.HAR_KOORDINAT);

                            tmpKoordinat.kobleNode(koordinatNodeCurrent);
                        } else {
                            Koordinat c = koordinater.ceiling(tmpKoordinat);
                            Koordinat f = koordinater.floor(tmpKoordinat);
                            if (c == f) {
                                System.out.println("Found some shit.");
                                kommuneNode.createRelationshipTo(c.tilkobletNode, RelationType.HAR_KOORDINAT);
                                koordinatNodePrevious.createRelationshipTo(c.tilkobletNode, RelationType.NESTE_PUNKT_KOMMUNE);
                            } else
                                System.out.println("Yeah holy shit det skal være umulig, this is bad.");

                            // Sjekke om previous allerede er koblet
                            // om ikke, koble previous med c.tilkobletnode
                        }
                    } // Slutt - Gjennomløping av kommunens grnsepunkter

                    koordinatNodeCurrent.createRelationshipTo(koordinatKommuneStart, RelationType.NESTE_PUNKT_KOMMUNE);
                    koordinatKommuneStart.setProperty("start", "start");
                } // Slutt - Gjennomløping av fylke
//                return;
            } // Slutt - Gjennomløping av fylker
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
        NESTE_PUNKT_FYLKE,
        NESTE_PUNKT_KOMMUNE,
        HAR_KOORDINAT
    }
}
