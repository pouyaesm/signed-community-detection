package Network.Core;

import org.apache.commons.lang3.StringUtils;

import java.io.FileInputStream;
import java.util.Scanner;
import java.util.StringTokenizer;

public class GraphReader {


    public static Graph readGraph(String address, boolean symmetric, int lineCount) throws Exception{
        ListMatrix listMatrix = readListMatrix(address, symmetric, lineCount);
        return new Graph(listMatrix.sort(true, ListMatrix.MODE_REMOVE_DUPLICATE).normalize());
    }

    /**
     * Read edge list with "row column  value" format in each line
     * @param address
     * @return
     */
    public static ListMatrix readListMatrix(String address, boolean symmetric, int cellCount) throws Exception{
        FileInputStream fis = new FileInputStream(address);
        Scanner scanner = new Scanner(fis);
        int rows[] = new int[cellCount];
        int columns[] = new int[cellCount];
        float values[] = new float[cellCount];
        int insertAt = 0;
        while(scanner.hasNextLine()){
            if(insertAt >= cellCount){
                throw new Exception("Line count of list exceeded " + cellCount);
            }
            String line = scanner.nextLine();
            StringTokenizer tkn = new StringTokenizer(line," \t");
            int tokenCount = tkn.countTokens();
            if (tokenCount == 2) {
                rows[insertAt] = Integer.parseInt(tkn.nextToken());
                columns[insertAt++] = Integer.parseInt(tkn.nextToken());
            } else if (tokenCount == 3) {
                rows[insertAt] = Integer.parseInt(tkn.nextToken());
                columns[insertAt] = Integer.parseInt(tkn.nextToken());
                values[insertAt++] = Float.parseFloat(tkn.nextToken());
            }else if(StringUtils.isNumericSpace(line)){
                throw new Exception("Each line of file must contain 'sourceId targetId' " +
                        "or 'sourceId targetId weight'");
            }else{
                // Line is ignored as it may contain comments
            }
        }
        if(insertAt < cellCount){
            throw new Exception("Please change the line count to " + insertAt);
        }
        ListMatrix listMatrix = new ListMatrix().init(rows, columns, values, true);
        return symmetric ? listMatrix.symmetrize() : listMatrix;
    }
}
