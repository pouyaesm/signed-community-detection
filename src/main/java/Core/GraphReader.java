package Core;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class GraphReader {


    public static Graph readGraph(String address, int lineCount) throws Exception{
        ListMatrix listMatrix = readListMatrix(address, lineCount);
        return new Graph(listMatrix.sort(true, ListMatrix.MODE_REMOVE_DUPLICATE).normalize());
    }

    /**
     * Read edge list with "row column  value" format in each line
     * @param address
     * @return
     */
    public static ListMatrix readListMatrix(String address, int lineCount) throws Exception{
        FileInputStream fis = new FileInputStream(address);
        Scanner scanner = new Scanner(fis);
        int rows[] = new int[lineCount];
        int columns[] = new int[lineCount];
        float values[] = new float[lineCount];
        int insertAt = 0;
        while(scanner.hasNextInt()){
            if(insertAt >= lineCount){
                throw new Exception("Line count of graph file exceeded " + lineCount);
            }
            rows[insertAt] = scanner.nextInt();
            columns[insertAt] = scanner.nextInt();
            values[insertAt] = scanner.nextFloat();
            insertAt++;
        }
        if(insertAt < lineCount){
            throw new Exception("Correct the line count to " + lineCount);
        }
        ListMatrix listMatrix = new ListMatrix().init(rows, columns, values, true);
        return listMatrix;
    }
}
