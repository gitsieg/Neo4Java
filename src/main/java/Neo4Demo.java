import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.cypher.internal.compiler.v2_3.No;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Neo4Demo {

    private static final String NAVN = "navn", KODE = "kode";

    private static final String WIN_DATABASE_PATH = "C:\\Users\\AtleAmun\\AppData\\Roaming\\Neo4j Desktop\\Application\\neo4jDatabases\\database-4636402a-6111-427f-bbd9-dd6959fd5a6f\\installation-3.3.3\\data\\databases\\graph.db";


    private static File DATABASE_DIRECTORY =
            (System.getProperty("os.name").startsWith("Windows")) ?
                    new File(WIN_DATABASE_PATH) : // Windows
                    new File("/var/lib/neo4j/data/databases/administrative-enheter.db"); // Linux

    // Predefined queries.
    private static final String DETACH_DELETE = "MATCH (n) DETACH DELETE n";

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


    @SuppressWarnings("Duplicates")
    /**
     * e
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        GraphLoader graphLoader = new GraphLoader();
        graphLoader.registerDatasets();
/*
        LibGraph.graphTransaction(graphLoader.graphdb, new LibGraph.TransactionCommand() {
            @Override
            public void performTransaction(GraphDatabaseService graphdb) {
                LibGraph.checkRelations(graphLoader.graphdb);
            }
        });

//        LibGraph.sjekkKoordinater(new File("./src/res/json/fylker/NO-01.geojson"));

//        JSONArray kommuneFeatures = fylkeData.getJSONObject("administrative_enheter_kommune")
//                .getJSONArray("features");

/*
        GraphDatabaseFactory dbFactory = new GraphDatabaseFactory();
        GraphDatabaseBuilder builder = dbFactory.newEmbeddedDatabaseBuilder(DATABASE_DIRECTORY);
        GraphDatabaseService graphdb = builder.newGraphDatabase();
        registerShutdownHook(graphdb);

        // All access to graphdb goes in here
        try (Transaction tx = graphdb.beginTx()) {
            graphdb.execute(DETACH_DELETE);

            // Opprett toppnivå i grafen
            Node nodeNorge = graphdb.createNode(NodeType.Land);
            nodeNorge.setProperty(NAVN, "Norge");
            nodeNorge.setProperty(KODE, "NO");

            // ArrayList for alle fylkeNoder.
            ArrayList<Node> fylkeNoder = new ArrayList<>();

            // Peker til mappe som inneholder json-ressurser
            File res = new File("./src/res/json/");

            // Opprett Node for hvert fylke i fylkemap og sett relasjon land->fylke
            HashMap<String, String> fylkeMap = LibJSON.getFylker(new File(res, "fylker.json"));
            for (String key : fylkeMap.keySet()) {
                Node node = graphdb.createNode(NodeType.Fylke);
                node.setProperty(NAVN, fylkeMap.get(key));
                node.setProperty(KODE, key);
                nodeNorge.createRelationshipTo(node, RelationType.HAR_FYLKE);
                fylkeNoder.add(node);
            }

            // Parse fylkedata
            for (Node fylke :fylkeNoder) {
                String isoKode = fylke.getProperty(KODE).toString();

                // Sjekk om fil eksisterer
                File fylkeFil = new File(res, "fylker/" + isoKode + ".geojson");
                System.out.println("Fil eksisterer:" + fylkeFil.exists());
                System.out.println("Filsti:" + fylkeFil.getAbsolutePath());

                // Hent JSON data
                JSONObject fylkeJSON = new JSONObject(LibJSON.readFile(fylkeFil).toString());

                // JSONarray for fylke grense
                JSONArray grenseFeatures = fylkeJSON.getJSONObject("administrative_enheter.fylkesgrense")
                                                        .getJSONArray("features");

                // Fylke har node fylkegrense som har relasjoner mot flere LineStrings.
                Node nodeFylkeGrense = graphdb.createNode(NodeType.FylkeGrense);
                fylke.createRelationshipTo(nodeFylkeGrense, RelationType.HAR_FYLKEGRENSE);

                for (int i = 0; i < grenseFeatures.length(); i++) {
                    JSONObject lineString = grenseFeatures.getJSONObject(i);
                    JSONArray lineGeoCoords = lineString.getJSONObject("geometry").getJSONArray("coordinates");
                    JSONObject lineProperties = lineString.getJSONObject("properties");


                    // LineString -> koordinater
                    Node nodeLineString = graphdb.createNode(NodeType.LineString);
                    nodeFylkeGrense.createRelationshipTo(nodeLineString, RelationType.HAR_LINESTRING);


                    ArrayList<Node> koordinatNoder = new ArrayList<>();
                    for (int j = 0; j < lineGeoCoords.length(); j++) {
                        JSONArray arrKoordinater = lineGeoCoords.getJSONArray(j);
                        Node koordinatNode = graphdb.createNode(NodeType.Koordinat);
                        nodeLineString.createRelationshipTo(koordinatNode, RelationType.HAR_KOORDINAT);
                        koordinatNode.setProperty("lat", arrKoordinater.getDouble(0));
                        koordinatNode.setProperty("lon", arrKoordinater.getDouble(1));
                        koordinatNoder.add(koordinatNode);

                    }

//                     Lenk noder
                    Iterator<Node> nodeIterator = koordinatNoder.iterator();
                    Node fNode = nodeIterator.next();
                    while (nodeIterator.hasNext()) {
                        Node sNode = nodeIterator.next();
                        fNode.createRelationshipTo(sNode, RelationType.NESTE_PUNKT);
                        fNode = sNode;
                    }


//                    // Iterer over properties
                    Iterator<String> propIterator = lineProperties.keys();
                    while (propIterator.hasNext()) {
                        String key = propIterator.next();
                        String value = lineProperties.get(key).toString();
                        nodeLineString.setProperty(key, value);
                    }

                    koordinatNoder.clear();

                }
            }

            tx.success();
            tx.close();
        }
        graphdb.shutdown();
        */
    }
    /**
     * Reads a file with character data.
     *
     * @return StringBuil
     */
    public static StringBuilder readFile(String fylkenavn) {
        StringBuilder builder = new StringBuilder();
        try (FileInputStream stream = new FileInputStream("src/main/java/fylker/" + fylkenavn + ".geojson")) {
            InputStreamReader streamReader = new InputStreamReader(stream);
            BufferedReader reader = new BufferedReader(streamReader);

            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder;
    }

    private static void registerShutdownHook(final GraphDatabaseService graphDb) {
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

    /**
     * Sets graph topleveldata
     *
     * @param grapdb
     * @return
     */
    private static ArrayList<Node> getFylker(GraphDatabaseService grapdb) {
        String fylkenr = "fylkenr", navn = "navn";
        ArrayList<Node> listFylke = new ArrayList<>();
        Node nodeOstfold = grapdb.createNode(NodeType.Fylke);
        nodeOstfold.setProperty(fylkenr, "NO-01");
        nodeOstfold.setProperty(navn, "Østfold");

        Node nodeAkershus = grapdb.createNode(NodeType.Fylke);
        nodeAkershus.setProperty(fylkenr, "NO-02");
        nodeAkershus.setProperty(navn, "Akershus");

        Node nodeOslo = grapdb.createNode(NodeType.Fylke);
        nodeOslo.setProperty(fylkenr, "NO-03");
        nodeOslo.setProperty(navn, "Oslo");

        Node nodeHedmark = grapdb.createNode(NodeType.Fylke);
        nodeHedmark.setProperty(fylkenr, "NO-04");
        nodeHedmark.setProperty(navn, "Hedmark");

        Node nodeOppland = grapdb.createNode(NodeType.Fylke);
        nodeOppland.setProperty(fylkenr, "NO-05");
        nodeOppland.setProperty(navn, "Oppland");

        Node nodeBuskerud = grapdb.createNode(NodeType.Fylke);
        nodeBuskerud.setProperty(fylkenr, "NO-06");
        nodeBuskerud.setProperty(navn, "Buskerud");

        Node nodeVestfold = grapdb.createNode(NodeType.Fylke);
        nodeVestfold.setProperty(fylkenr, "NO-07");
        nodeVestfold.setProperty(navn, "Vestfold");

        Node nodeTelemark = grapdb.createNode(NodeType.Fylke);
        nodeTelemark.setProperty(fylkenr, "NO-08");
        nodeTelemark.setProperty(navn, "Telemark");

        Node nodeAustAgder = grapdb.createNode(NodeType.Fylke);
        nodeAustAgder.setProperty(fylkenr, "NO-09");
        nodeAustAgder.setProperty(navn, "Aust-Agder");

        Node nodeVestAgder = grapdb.createNode(NodeType.Fylke);
        nodeVestAgder.setProperty(fylkenr, "NO-10");
        nodeVestAgder.setProperty(navn, "Vest-Agder");

        Node nodeRogaland = grapdb.createNode(NodeType.Fylke);
        nodeRogaland.setProperty(fylkenr, "NO-11");
        nodeRogaland.setProperty(navn, "Rogaland");

        Node nodeHordaland = grapdb.createNode(NodeType.Fylke);
        nodeHordaland.setProperty(fylkenr, "NO-12");
        nodeHordaland.setProperty(navn, "Hordaland");

        Node nodeSognOgFjordane = grapdb.createNode(NodeType.Fylke);
        nodeSognOgFjordane.setProperty(fylkenr, "NO-14");
        nodeSognOgFjordane.setProperty(navn, "Sogn og Fjordane");

        Node nodeMoreOgRomsdal = grapdb.createNode(NodeType.Fylke);
        nodeMoreOgRomsdal.setProperty(fylkenr, "NO-15");
        nodeMoreOgRomsdal.setProperty(navn, "Møre og Romsdal");

        Node nodeTrondelag = grapdb.createNode(NodeType.Fylke);
        nodeTrondelag.setProperty(fylkenr, "NO-50");
        nodeTrondelag.setProperty(navn, "Trøndelag");

        Node nodeNordland = grapdb.createNode(NodeType.Fylke);
        nodeNordland.setProperty(fylkenr, "NO-18");
        nodeNordland.setProperty(navn, "Nordland");

        Node nodeTroms = grapdb.createNode(NodeType.Fylke);
        nodeTroms.setProperty(fylkenr, "NO-19");
        nodeTroms.setProperty(navn, "Troms");

        Node nodeFinnmark = grapdb.createNode(NodeType.Fylke);
        nodeFinnmark.setProperty(fylkenr, "NO-20");
        nodeFinnmark.setProperty(navn, "Finnmark");

        listFylke.add(nodeOstfold);
        listFylke.add(nodeAkershus);
        listFylke.add(nodeOslo);
        listFylke.add(nodeHedmark);
        listFylke.add(nodeOppland);
        listFylke.add(nodeBuskerud);
        listFylke.add(nodeVestfold);
        listFylke.add(nodeTelemark);
        listFylke.add(nodeAustAgder);
        listFylke.add(nodeVestAgder);
        listFylke.add(nodeRogaland);
        listFylke.add(nodeHordaland);
        listFylke.add(nodeSognOgFjordane);
        listFylke.add(nodeMoreOgRomsdal);
        listFylke.add(nodeTrondelag);
        listFylke.add(nodeNordland);
        listFylke.add(nodeTroms);
        listFylke.add(nodeFinnmark);

        return listFylke;
    }
}
