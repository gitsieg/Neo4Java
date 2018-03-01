import jdk.nashorn.internal.parser.JSONParser;
import org.json.JSONObject;
import org.neo4j.cypher.internal.ExecutionEngine;
import org.neo4j.cypher.internal.compatibility.v2_3.StringInfoLogger;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.io.IOUtils;

import java.io.*;
import java.util.Iterator;

public class Neo4Demo {

    private static final File dbDir = new File("/var/lib/neo4j/data/databases/javatest.db");
    // Predefined queries.
    private static final String DETACH_DELETE = "MATCH (n) DETACH DELETE n";

    public enum NodeType implements Label {
        Koordinat, Kommune, Fylke, ;
    }

    public enum RelationType implements RelationshipType {
        Has, BelongsTo;
    }


    /**
     * e
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        JSONObject jsonDump = new JSONObject(readFile().toString());
        System.out.println(jsonDump.toString(2));

        GraphDatabaseFactory dbFactory = new GraphDatabaseFactory();
        GraphDatabaseBuilder builder = dbFactory.newEmbeddedDatabaseBuilder(dbDir);
        GraphDatabaseService graphdb = builder.newGraphDatabase();
        Result result;

        // All access to graphdb goes in here
//        try (Transaction tx = graphdb.beginTx()) {
//
//
//        }


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
    }

    public static StringBuilder readFile() {
        StringBuilder builder = new StringBuilder();
        try (FileInputStream stream = new FileInputStream("/home/gitsieg/IdeaProjects/Neo4Java/src/main/java/bo.geojson")) {
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
