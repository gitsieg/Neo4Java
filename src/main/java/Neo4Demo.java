import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.*;

public class Neo4Demo {


    private static final File DATABASE_DIRECTORY = new File("/var/lib/neo4j/data/databases/javatest.db");
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


//        System.out.println(kommuneFeatures.toString(2));

        GraphDatabaseFactory dbFactory = new GraphDatabaseFactory();
        GraphDatabaseBuilder builder = dbFactory.newEmbeddedDatabaseBuilder(DATABASE_DIRECTORY);
        GraphDatabaseService graphdb = builder.newGraphDatabase();
        Result result;

        //TODO: Fix neo4j process server failing to stop with systemctl stop neo4j.... -_-

        // All access to graphdb goes in here
        try (Transaction tx = graphdb.beginTx()) {
            graphdb.execute(DETACH_DELETE);

            Node telemark = graphdb.createNode(NodeType.Fylke);
            telemark.setProperty("Navn", "Telemark");
            telemark.setProperty("Kode", "NO-08");

            for (int i = 0; i < kommuneFeatures.length(); i++) {
                JSONObject kommune = kommuneFeatures.getJSONObject(i);
                JSONObject kommuneProperties = kommune.getJSONObject("properties");

                // Lag kommunennode
                String kommuneNavn = kommuneProperties.getJSONArray("navn").getJSONObject(0).getString("navn");
                String kommuneNr = kommuneProperties.getString("kommunenummer");

                Node kommuneNode = graphdb.createNode(NodeType.Kommune);
                kommuneNode.setProperty("KommuneNavn", kommuneNavn);
                kommuneNode.setProperty("Kommunenr", kommuneNavn);

                telemark.createRelationshipTo(kommuneNode, RelationType.HAR_KOMMUNE);


                // Behandle koordinater og lenk
                JSONArray koordinater = kommune.getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0);
                JSONArray punkt = null;
                for (int j = 0; j < koordinater.length(); j++) {
                    punkt = koordinater.getJSONArray(i);
                    Node koordinatSett = graphdb.createNode(NodeType.Koordinat);
                    koordinatSett.setProperty("lat", punkt.getInt(0));
                    koordinatSett.setProperty("lon", punkt.getInt(1));
                    kommuneNode.createRelationshipTo(koordinatSett, RelationType.KOMMUNE_GRENSE);
                }

            }
            tx.success();
        }
        graphdb.shutdown();
    }

        //
//        try (Transaction tx = graphdb.beginTx()) {
//
//            graphdb.execute(DETACH_DELETE);
//
//            Node bobNode = graphdb.createNode(NodeType.Person);
//            bobNode.setProperty("PID", 5001);
//            bobNode.setProperty("Name", "Bob");
//            bobNode.setProperty("Age", 23);
//
//            Node aliceNode = graphdb.createNode(NodeType.Person);
//            aliceNode.setProperty("PID", 1300);
//            aliceNode.setProperty("Name", "Alice");
//
//            Node eveNode = graphdb.createNode(NodeType.Person);
//            eveNode.setProperty("Name", "Eve");
//
//            Node itNode = graphdb.createNode(NodeType.Course);
//            itNode.setProperty("PID", 1);
//            itNode.setProperty("Name", "IT GraphDB");
//            itNode.setProperty("Location", "Room foo.bar");
//
//            Node electronicNode = graphdb.createNode(NodeType.Course);
//            electronicNode.setProperty("Name", "Electronics");
//
//            bobNode.createRelationshipTo(aliceNode, RelationType.Knows);
//
//            Relationship bobRelIt = bobNode.createRelationshipTo(itNode, RelationType.BelongsTo);
//            bobRelIt.setProperty("Function", "Student");
//
//            Relationship bobRelElectronics = bobNode.createRelationshipTo(electronicNode, RelationType.BelongsTo);
//            bobRelElectronics.setProperty("Function", "Supply Teacher");
//
//            Relationship aliceRelIt = aliceNode.createRelationshipTo(itNode, RelationType.BelongsTo);
//            aliceRelIt.setProperty("Function", "Teacher");
//
//            tx.success();
//
//            result = graphdb.execute("match (n) return n");
//            Iterator<Node> itr = result.columnAs("n");
//
//            while (itr.hasNext())
//                System.out.println(itr.next().toString());
//
//        }
//        graphdb.shutdown();
//    }

    /**
     * Reads a file with character data.
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
}
