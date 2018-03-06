import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class Neo4Demo {


    private static final File DATABASE_DIRECTORY = new File("/var/lib/neo4j/data/databases/administrative-enheter.db");
    // Predefined queries.
    private static final String DETACH_DELETE = "MATCH (n) DETACH DELETE n";

    public enum NodeType implements Label {
        Koordinat,
        Kommune,
        Fylke;
    }

    public enum RelationType implements RelationshipType {
        HAR_KOMMUNE,
        KOMMUNE_GRENSE;
    }


    /**
     * e
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        JSONObject fylkeData = new JSONObject(readFile().toString());


        JSONArray kommuneFeatures = fylkeData.getJSONObject("administrative_enheter_kommune")
                .getJSONArray("features");


        GraphDatabaseFactory dbFactory = new GraphDatabaseFactory();
        GraphDatabaseBuilder builder = dbFactory.newEmbeddedDatabaseBuilder(DATABASE_DIRECTORY);
        GraphDatabaseService graphdb = builder.newGraphDatabase();
        registerShutdownHook(graphdb);


        // All access to graphdb goes in here
        try (Transaction tx = graphdb.beginTx()) {
            graphdb.execute(DETACH_DELETE);

            Node telemark = graphdb.createNode(NodeType.Fylke);
            telemark.setProperty("navn", "Telemark");
            telemark.setProperty("nr", "NO-08");

            for (int i = 0; i < kommuneFeatures.length(); i++) {
                JSONObject kommune = kommuneFeatures.getJSONObject(i);
                JSONObject kommuneProperties = kommune.getJSONObject("properties");

                // Lag kommunennode
                String komNavn = kommuneProperties.getJSONArray("navn").getJSONObject(0).getString("navn");
                String komNr = kommuneProperties.getString("kommunenummer");

                Node komNode = graphdb.createNode(NodeType.Kommune);
                komNode.setProperty("navn", komNavn);
                komNode.setProperty("nr", komNavn);

                telemark.createRelationshipTo(komNode, RelationType.HAR_KOMMUNE);

                // Behandle koordinater og lenk
                JSONArray koordinater = kommune.getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0);
                for (int j = 0; j < koordinater.length(); j++) {
                    JSONArray punkt = koordinater.getJSONArray(j);
                    Node koordinatSett = graphdb.createNode(NodeType.Koordinat);
                    koordinatSett.setProperty("lat", punkt.getDouble(0));
                    koordinatSett.setProperty("lon", punkt.getDouble(1));
                    komNode.createRelationshipTo(koordinatSett, RelationType.KOMMUNE_GRENSE);
                }

            }
            tx.success();
            tx.close();
        }
        graphdb.shutdown();
    }

    /**
     * Reads a file with character data.
     *
     * @return StringBuil
     */
    public static StringBuilder readFile() {
        StringBuilder builder = new StringBuilder();
        try (FileInputStream stream = new FileInputStream("/home/gitsieg/IdeaProjects/Neo4Java/src/main/java/telemark.geojson")) {
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
     * @param grapdb
     * @return
     */
    private ArrayList<Node> setFylker(GraphDatabaseService grapdb) {

        ArrayList<Node> listFylke = new ArrayList<>();
        Node nodeOstfold = grapdb.createNode(NodeType.Fylke);
        nodeOstfold.setProperty("fylkenr", "NO-01");
        nodeOstfold.setProperty("name", "Østfold");

        Node nodeAkershus = grapdb.createNode(NodeType.Fylke);
        nodeAkershus.setProperty("fylkenr", "NO-02");
        nodeAkershus.setProperty("name", "Akershus");

        Node nodeOslo = grapdb.createNode(NodeType.Fylke);
        nodeOslo.setProperty("fylkenr", "NO-03");
        nodeOslo.setProperty("name", "Oslo");

        Node nodeHedmark = grapdb.createNode(NodeType.Fylke);
        nodeHedmark.setProperty("fylkenr", "NO-04");
        nodeHedmark.setProperty("name", "Hedmark");

        Node nodeOppland = grapdb.createNode(NodeType.Fylke);
        nodeOppland.setProperty("fylkenr", "NO-05");
        nodeOppland.setProperty("name", " Oppland");

        Node nodeBuskerud = grapdb.createNode(NodeType.Fylke);
        nodeBuskerud.setProperty("fylkenr", "NO-06");
        nodeBuskerud.setProperty("name", "Buskerud");

        Node nodeVestfold= grapdb.createNode(NodeType.Fylke);
        nodeVestfold.setProperty("fylkenr", "NO-07");
        nodeVestfold.setProperty("name", "Vestfold");

        Node nodeTelemark = grapdb.createNode(NodeType.Fylke);
        nodeTelemark.setProperty("fylkenr", "NO-08");
        nodeTelemark.setProperty("name", "Telemark");

        Node nodeAustAgder = grapdb.createNode(NodeType.Fylke);
        nodeAustAgder.setProperty("fylkenr", "NO-09");
        nodeAustAgder.setProperty("name", "Aust-Agder");

        Node nodeVestAgder= grapdb.createNode(NodeType.Fylke);
        nodeVestAgder.setProperty("fylkenr", "NO-10");
        nodeVestAgder.setProperty("name", "Vest-Agder");

        Node nodeRogaland = grapdb.createNode(NodeType.Fylke);
        nodeRogaland.setProperty("fylkenr", "NO-11");
        nodeRogaland.setProperty("name", "Rogaland");

        Node nodeHordaland = grapdb.createNode(NodeType.Fylke);
        nodeHordaland.setProperty("fylkenr", "NO-12");
        nodeHordaland.setProperty("name", "Hordaland");

        Node nodeSognOgFjordane = grapdb.createNode(NodeType.Fylke);
        nodeSognOgFjordane.setProperty("fylkenr", "NO-14");
        nodeSognOgFjordane.setProperty("name", "Sogn og Fjordane");

        Node nodeMoreOgRomsdal = grapdb.createNode(NodeType.Fylke);
        nodeMoreOgRomsdal.setProperty("fylkenr", "NO-15");
        nodeMoreOgRomsdal.setProperty("name", "Møre og Romsdal");

        Node nodeTrondelag = grapdb.createNode(NodeType.Fylke);
        nodeTrondelag.setProperty("fylkenr", "NO-50");
        nodeTrondelag.setProperty("name", "Trøndelag");

        Node nodeNordland = grapdb.createNode(NodeType.Fylke);
        nodeNordland.setProperty("fylkenr", "NO-18");
        nodeNordland.setProperty("name", "Nordland");

        Node nodeTroms = grapdb.createNode(NodeType.Fylke);
        nodeTroms.setProperty("fylkenr", "NO-19");
        nodeTroms.setProperty("name", "Troms");

        Node nodeFinnmark = grapdb.createNode(NodeType.Fylke);
        nodeFinnmark.setProperty("fylkenr", "NO-20");
        nodeFinnmark.setProperty("name", "Finnmark");

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
