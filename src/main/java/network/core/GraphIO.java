package network.core;

import network.Shared;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

public class GraphIO {


    public static Graph readGraph(String address, boolean symmetric) throws Exception{
        ListMatrix listMatrix = readListMatrix(address, symmetric);
        // Normalizing without sorting causes [3, 1, 2] to be mapped to [0, 1, 2]
        // But after sorting: [1, 2, 3] -> [0, 1, 2] easier to track and test
        return new Graph(listMatrix.sort().normalize());
    }

    /**
     * Read edge list with "row column  value" format in each line
     * @param address
     * @return
     */
    public static ListMatrix readListMatrix(String address, boolean symmetric) throws Exception{
        FileInputStream fis = new FileInputStream(address);
        Scanner scanner = new Scanner(fis);
        ArrayList<Integer> inputRows = new ArrayList<>();
        ArrayList<Integer> inputColumns = new ArrayList<>();
        ArrayList<Float> inputValues = new ArrayList<>();
        while(scanner.hasNextLine()){
            String line = scanner.nextLine();
            StringTokenizer tkn = new StringTokenizer(line," \t");
            int tokenCount = tkn.countTokens();
            if (tokenCount == 2) {
                inputRows.add(Integer.parseInt(tkn.nextToken()));
                inputColumns.add(Integer.parseInt(tkn.nextToken()));
            } else if (tokenCount == 3) {
                inputRows.add(Integer.parseInt(tkn.nextToken()));
                inputColumns.add(Integer.parseInt(tkn.nextToken()));
                inputValues.add(Float.parseFloat(tkn.nextToken()));
            }else if(Util.isNumber(tkn.nextToken())){
                throw new Exception("Each line of file must contain 'sourceId targetId' " +
                        "or 'sourceId targetId weight'");
            }
            // Otherwise line is ignored as it may contain comments
        }
        int[] rows = new int[inputRows.size()];
        int[] columns = new int[inputRows.size()];
        float[] values = new float[inputRows.size()];
        for(int p = 0 ; p < rows.length ; p++){
            rows[p] = inputRows.get(p);
            columns[p] = inputColumns.get(p);
            values[p] = inputValues.get(p);
        }
        ListMatrix listMatrix = new ListMatrix().init(rows, columns, values, true);
        Shared.log(inputRows.size() + " links has been read");
        return symmetric ? listMatrix.symmetrize() : listMatrix;
    }

    /**
     * Write detection partition to file using the un-Normalized nodes
     * @param graph
     * @param partition
     * @param address
     */
    public static void writePartition(Graph graph, int[] partition, String address){
        int[] toRaw = graph.getListMatrix().getToRaw()[0]; // convert nodeIds back to un-normalized inputs
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(address));
            for(int nodeId = 0 ; nodeId < partition.length ; nodeId++){
                writer.write(toRaw[nodeId] + "\t" + partition[nodeId] + "\n");
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
