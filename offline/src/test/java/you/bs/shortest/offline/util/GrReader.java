package you.bs.shortest.offline.util;

import it.unimi.dsi.fastutil.doubles.DoubleDoublePair;
import org.jgrapht.alg.util.Triple;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GrReader {

    public List<Triple<Integer, Integer, DoubleDoublePair>> read(String name) throws URISyntaxException {
        URL url = GrReader.class.getResource("/data/" + name + ".gr");
        File gr = Paths.get(url.toURI()).toFile();
        List<Triple<Integer, Integer, DoubleDoublePair>> ans = new ArrayList<>();
        try (FileReader fileReader = new FileReader(gr);
             LineNumberReader reader = new LineNumberReader(fileReader)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("a")) {
                    String[] content = line.split(" ");
                    int from = Integer.parseInt(content[1]);
                    int to = Integer.parseInt(content[2]);
                    int w = Integer.parseInt(content[3]);
                    Triple<Integer, Integer, DoubleDoublePair> triple = new Triple<>(from, to, DoubleDoublePair.of(w, w));
                    ans.add(triple);
                }
            }
            return ans;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
