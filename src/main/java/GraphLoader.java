import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("Duplicates")
public class GraphLoader {
/* ------ Statiske variabler------ */
    private static final String WIN_DATABASE_PATH = "C:\\Users\\AtleAmun\\AppData\\Roaming\\Neo4j Desktop\\Application\\neo4jDatabases\\database-4636402a-6111-427f-bbd9-dd6959fd5a6f\\installation-3.3.3\\data\\databases\\graph.db";
    private final static File DATABASE_DIRECTORY =
            (System.getProperty("os.name").startsWith("Windows")) ?
                    new File(WIN_DATABASE_PATH) :
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

        fylkeNoder = new ArrayList<>();
        koordinater = new TreeSet<>();
        forholdFylke = new TreeSet<>();
        forholdKommune = new TreeSet<>();

        registerShutdownHook(graphdb);
    }

    private final ArrayList<Node> fylkeNoder;
    private final TreeSet<Koordinat> koordinater;
    private final TreeSet<Forhold> forholdFylke;
    private final TreeSet<Forhold> forholdKommune;

    /**
     *
     * todo- Eventuell feilhåndtering
     */
    public void registerDatasets() {

        LibGraph.graphTransaction(this.graphdb, txCmd -> {
                graphdb.execute(DETACH_DELETE); // For å fjerne all eksisterende data

                Node nodeNorge = graphdb.createNode(NodeType.Land);
                nodeNorge.setProperty(NAVN, "Norge");
                nodeNorge.setProperty(KODE, "NO");

                // Peker til mappe som inneholder json-ressurser
                File res = new File(RES_JSON);

                HashMap<String, String> fylkeMap = LibJSON.getFylker(new File(res, "fylker.json"));
                for (String key : fylkeMap.keySet()) {
                    Node node = graphdb.createNode(NodeType.Fylke);
                    node.setProperty(NAVN, fylkeMap.get(key));
                    node.setProperty(KODE, key);
                    nodeNorge.createRelationshipTo(node, RelationType.HAR_FYLKE);
                    fylkeNoder.add(node);
                }
            });


            // Gjennomløp fylkedata
        for (Node fylke : fylkeNoder) {
            LibGraph.graphTransaction(this.graphdb, txCmd -> {
                final String isoKode = fylke.getProperty(KODE).toString();

                // Sjekk om fil eksisterer
                final File res = new File(RES_JSON);
                final File fylkeFil = new File(res, "fylker/".concat(isoKode).concat(".geojson"));

                graphLogger.log(Level.INFO, "Fil eksisterer:"+fylkeFil.exists());
                graphLogger.log(Level.INFO, "Filsti:" + fylkeFil.getAbsolutePath());

                // Hent JSON data
                final JSONObject json_fylkeFil = new JSONObject(LibJSON.readFile(fylkeFil).toString());
                final JSONObject json_fylke = json_fylkeFil .getJSONObject("administrative_enheter.fylkesgrense");
                final JSONObject json_kommuner = json_fylkeFil .getJSONObject("administrative_enheter.kommune");

                // Objekter for gjenbruk
                JSONObject json_kommune;
                JSONArray features;
                JSONArray coordinates;
                JSONArray coordinateContainer;

                /* -------- Innlesing a fylkedata -------- */
                features = json_fylke.getJSONArray("features");

                // Spesialbehandling av første entry
                coordinateContainer = features
                        .getJSONObject(0) // {} 0, 1, 2, 3, 4
                        .getJSONObject("geometry") // {} geometry
                        .getJSONArray("coordinates") // [] coordinates
                        .getJSONArray(0);

                Koordinat koordinatFylkeStart = new Koordinat(
                        coordinateContainer.getDouble(0),
                        coordinateContainer.getDouble(1)
                );
                if (koordinater.add(koordinatFylkeStart)) {
                    koordinatFylkeStart.kobleNode( graphdb.createNode(NodeType.Koordinat) );
                    fylke.createRelationshipTo(koordinatFylkeStart.tilkobletNode, RelationType.HAR_KOORDINAT);
                } else {
                    Koordinat k1 = koordinater.ceiling(koordinatFylkeStart);
                    Koordinat k2 = koordinater.floor(koordinatFylkeStart);
                    if (k1 == k2 && k2 != null)
                        koordinatFylkeStart = k1;
                    else
                        throw new Exception();
//                    koordinatFylkeStart = koordinater.ceiling(koordinatFylkeStart);
                    fylke.createRelationshipTo(koordinatFylkeStart.tilkobletNode, RelationType.HAR_KOORDINAT);
                }
                koordinatFylkeStart.tilkobletNode.setProperty("startFylke", fylke.getProperty(NAVN));

                Koordinat koordinatFylkePrevious = koordinatFylkeStart;
                for (int i = 0, j = 1; i < features.length(); i++, j = 0) {
                    coordinates = features
                            .getJSONObject(i) // {} 0, 1, 2, 3, 4
                            .getJSONObject("geometry") // {} geometry
                            .getJSONArray("coordinates"); // [] coordinates

                    for ( ; j < coordinates.length(); j++) {
                        coordinateContainer = coordinates.getJSONArray(j); // [] 0, 1, 2, 3, 4

                        Koordinat koordinatFylkeCurrent = new Koordinat(
                                coordinateContainer.getDouble(0),
                                coordinateContainer.getDouble(1)
                        );
                        if (koordinater.add(koordinatFylkeCurrent)) {
                            koordinatFylkeCurrent.kobleNode(graphdb.createNode(NodeType.Koordinat));
                            if (LibGraph.addForholdToSet (
                                    koordinatFylkePrevious,
                                    koordinatFylkeCurrent,
                                    RelationType.NESTE_PUNKT_FYLKE,
                                    forholdFylke))
                                koordinatFylkePrevious.tilkobletNode.createRelationshipTo(
                                        koordinatFylkeCurrent.tilkobletNode, RelationType.NESTE_PUNKT_FYLKE);

//                            koordinatFylkePrevious = koordinatFylkeCurrent;
                        } else {
                            Koordinat k1 = koordinater.ceiling(koordinatFylkeCurrent);
                            Koordinat k2 = koordinater.floor(koordinatFylkeCurrent);
                            if (k1 == k2 && k2 != null)
                                koordinatFylkeCurrent = k1;
                            else
                                throw new Exception();

                            if (LibGraph.addForholdToSet (
                                    koordinatFylkePrevious,
                                    koordinatFylkeCurrent,
                                    RelationType.NESTE_PUNKT_FYLKE,
                                    forholdFylke))
                                koordinatFylkePrevious.tilkobletNode.createRelationshipTo(
                                        koordinatFylkeCurrent.tilkobletNode, RelationType.NESTE_PUNKT_FYLKE);

                        }
                        fylke.createRelationshipTo(koordinatFylkeCurrent.tilkobletNode, RelationType.HAR_KOORDINAT);
                        koordinatFylkePrevious = koordinatFylkeCurrent;
                    }
                }
                if (forholdFylke.add(new Forhold(
                        koordinatFylkePrevious,
                        koordinatFylkeStart,
                        RelationType.NESTE_PUNKT_FYLKE)))
                    koordinatFylkePrevious.tilkobletNode.createRelationshipTo(
                            koordinatFylkeStart.tilkobletNode, RelationType.NESTE_PUNKT_FYLKE);


                System.out.println("Antall koordinater: "+koordinater.size());
                System.out.println("Antall forhold: "+ (forholdFylke.size()+forholdKommune.size()) );

                /* -------- Innlessing av kommunedata -------- */
                features = json_kommuner.getJSONArray("features");
                for (int i = 0; i < features.length(); i++) {

                    json_kommune = features.getJSONObject(i); // {} 0

                    Node kommune = graphdb.createNode(NodeType.Kommune);
                    kommune.setProperty("navn",json_kommune
                            .getJSONObject("properties")
                            .getJSONArray("navn")
                            .getJSONObject(0).getString("navn"));
                    fylke.createRelationshipTo(kommune, RelationType.HAR_KOMMUNE);

                    coordinates = json_kommune
                            .getJSONObject("geometry") // {} geometry
                            .getJSONArray("coordinates") // [] coordinates
                            .getJSONArray(0); // [] 0

                    // coordinates er nå en liste over sammtlige koordinater for kommunen

                    // Spesialbehandling av første koordinat
                    coordinateContainer = coordinates.getJSONArray(0); // Første av [] 0, 1, 2, 3, 5 ...
                    Koordinat koordinatKommunePrevious, koordinatKommuneStart = new Koordinat(
                            coordinateContainer.getDouble(0),
                            coordinateContainer.getDouble(1)
                    );
                    if (koordinater.add(koordinatKommuneStart)) {
                        koordinatKommuneStart.kobleNode(graphdb.createNode(NodeType.Koordinat));
                        koordinatKommunePrevious = koordinatKommuneStart;
                    } else {
                        Koordinat k1 = koordinater.ceiling(koordinatKommuneStart);
                        Koordinat k2 = koordinater.floor(koordinatKommuneStart);
                        if (k1 == k2 && k2 != null)
                            koordinatKommunePrevious = koordinatKommuneStart = k1;
                        else
                            throw new Exception("Node mismatch in TreeSet.");
                    }
                    kommune.createRelationshipTo(koordinatKommuneStart.tilkobletNode, RelationType.HAR_KOORDINAT);
                    koordinatKommuneStart.tilkobletNode.setProperty("startKommune", json_kommune
                            .getJSONObject("properties")
                            .getJSONArray("navn")
                            .getJSONObject(0).getString("navn"));

                    for (int j = 1; j < coordinates.length(); j++) {
                        coordinateContainer = coordinates.getJSONArray(j);

                        Koordinat koordinatKommuneCurrent = new Koordinat(
                                coordinateContainer.getDouble(0),
                                coordinateContainer.getDouble(1)
                        );

                        if ( koordinater.add(koordinatKommuneCurrent) ) {
                            koordinatKommuneCurrent.kobleNode(graphdb.createNode(NodeType.Koordinat));
                        } else {
                            Koordinat k1 = koordinater.ceiling(koordinatKommuneCurrent);
                            Koordinat k2 = koordinater.floor(koordinatKommuneCurrent);
                            if (k1 == k2 && k2 != null)
                                koordinatKommuneCurrent = k1;
                            else
                                throw new Exception();
                        }
                        // Sørger for at en node ikke blir koblet til kommunen flere enn én gang
                        if (koordinatKommuneCurrent.tilkobletNode != koordinatKommuneStart.tilkobletNode)
                            kommune.createRelationshipTo(koordinatKommuneCurrent.tilkobletNode, RelationType.HAR_KOORDINAT);

                        if (LibGraph.addForholdToSet(
                                koordinatKommunePrevious,
                                koordinatKommuneCurrent,
                                RelationType.NESTE_PUNKT_KOMMUNE,
                                forholdKommune))
                            koordinatKommunePrevious.tilkobletNode.createRelationshipTo(
                                        koordinatKommuneCurrent.tilkobletNode, RelationType.NESTE_PUNKT_KOMMUNE);

                        koordinatKommunePrevious = koordinatKommuneCurrent;
                    } // Slutt - Gjennomløping av kommunens grnsepunkter
                    if (LibGraph.addForholdToSet(
                            koordinatKommunePrevious,
                            koordinatKommuneStart,
                            RelationType.NESTE_PUNKT_KOMMUNE,
                            forholdKommune))
                        koordinatKommunePrevious.tilkobletNode.createRelationshipTo(
                                koordinatKommuneStart.tilkobletNode, RelationType.NESTE_PUNKT_KOMMUNE);

                } // Slutt - Gjennomløping av kommune
            }); // Slutt - Gjennomløping av fylke
        } // Slutt - Gjennomløping av fylker

        graphdb.shutdown();
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
