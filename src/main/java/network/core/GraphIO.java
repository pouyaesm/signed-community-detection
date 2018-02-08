package network.core;

import network.Shared;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

public class GraphIO {


    public static Graph readGraph(String address, boolean symmetric) throws Exception{
        ListMatrix listMatrix = readListMatrix(address, symmetric);
        // Normalizing without sorting causes [3, 1, 2] to be mapped to [0, 1, 2]
        // But after sorting: [1, 2, 3] -> [0, 1, 2] easier to track and test
        return (Graph) new Graph().init(listMatrix.sort().normalize());
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
        int[] toRaw = graph.getToRaw()[0]; // convert nodeIds back to un-normalized inputs
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(address));
            for(int nodeId = 0 ; nodeId < partition.length ; nodeId++){
                writer.write(toRaw[nodeId] + "\t" + partition[nodeId] + "\n");
            }
            writer.flush();
            Shared.log("Partition has been saved to " + getFileName(address));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write the list matrix as "sourceId targetId weight"
     */
    public static void writeListMatrix(ListMatrix listMatrix, String address){
        writeListMatrix(new ListMatrix[]{listMatrix}, new String[]{address});
    }

    /**
     * Write the list matrix as "sourceId targetId weight"
     */
    public static void writeListMatrix(ListMatrix[] listMatrices, String[] addresses){
        BufferedWriter[] writers;
        try {
            writers = new BufferedWriter[listMatrices.length];
            int totalEdgeCount = 0; // this is adapt the level of verbosity to size of matrices
            for(int m = 0 ; m < listMatrices.length ; m++) {
                if(listMatrices[m] == null) continue;
                writers[m] = new BufferedWriter(new FileWriter(addresses[m]));
                int[] rows = listMatrices[m].getRows();
                int[] columns = listMatrices[m].getColumns();
                float[] values = listMatrices[m].getValues();
                int[] toRaw = listMatrices[m].getToRaw() != null ?
                        listMatrices[m].getToRaw()[0] : null; // convert nodeIds back to un-normalized inputs
                for (int p = 0; p < rows.length; p++) {
                    if (toRaw != null) {
                        writers[m].write(toRaw[rows[p]] + "\t" + toRaw[columns[p]] + "\t" + values[p] + "\n");
                    } else {
                        writers[m].write(rows[p] + "\t" + columns[p] + "\t" + values[p] + "\n");
                    }
                }
                totalEdgeCount += listMatrices[m].getRows().length;
            }
            // Flush the writers
            for(int graphId = 0 ; graphId < writers.length ; graphId++){
                if(writers[graphId] == null) continue;
                writers[graphId].flush();
                // Ensure that the logged lists do not exceed 10
                double edgeRatio = (double) listMatrices[graphId].getRows().length / totalEdgeCount;
                if(edgeRatio >= 0.1) {
                    Shared.log("List " + graphId
                            + " has been saved to " + getFileName(addresses[graphId]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Extract the file name from address
     * @param address
     * @return
     */
    public static String getFileName(String address){
        int lastSeparator = Math.max(address.lastIndexOf("/"), address.lastIndexOf("\\"));
        if(lastSeparator < 0){
            return address;
        }else{
            return address.substring(lastSeparator + 1);
        }
    }
}
