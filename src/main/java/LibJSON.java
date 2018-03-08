import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.graphdb.Node;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class LibJSON {

    static HashMap<String, String> getFylker(File file) {
        HashMap<String, String> fylker = new HashMap<>();

        JSONObject json_file_fylker = new JSONObject(readFile(file).toString());

        JSONArray json_content_fylker = json_file_fylker.getJSONArray("fylker");
        String prefix = json_file_fylker.getString("ISO_prefix");

        for (int i = 0; i < json_content_fylker.length(); i++) {
            JSONObject fylke = json_content_fylker.getJSONObject(i);
            fylker.put(prefix.concat(fylke.getString("nr")), fylke.getString("navn"));
        }


        return fylker;
    }







/* ---------- Helpers ---------- */

    /**
     * Takes a JSON file and returns a JSONObject
     * @param file The file to be read from
     * @return a StringBuilder containing the whole file
     */
    static StringBuilder readFile(File file) {
        StringBuilder builder = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(file)));

            for (String line = reader.readLine(); line != null; line = reader.readLine())
                builder.append(line);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder;
    }
}


