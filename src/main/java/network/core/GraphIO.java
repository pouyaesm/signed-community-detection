package network.core;

import cern.colt.map.OpenIntIntHashMap;
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
        return (Graph) new Graph(listMatrix.sort().normalize());
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
        int lineCount = 0;
        while(scanner.hasNextLine()){
            String line = scanner.nextLine();
            lineCount++;
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
                throw new Exception("Error at line " + lineCount
                        + ". Each line must contain 'sourceId targetId' " +
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
     * Read a node group assignment based on the normalization map
     * to normalize node ids appropriate for the corresponding nodes of a graph
     * @param address
     * @param toNormal
     * @return
     * @throws Exception
     */
    public static int[] readPartition(String address, OpenIntIntHashMap toNormal) throws Exception{
        int[] partition = Util.initArray(toNormal.size(), -1);
        FileInputStream fis = new FileInputStream(address);
        Scanner scanner = new Scanner(fis);
        int lineCount = 0, assigmentCount = 0;
        while(scanner.hasNextLine()){
            String line = scanner.nextLine();
            lineCount++;
            StringTokenizer tkn = new StringTokenizer(line," \t");
            int tokenCount = tkn.countTokens();
            if (tokenCount == 2) {
                int nodeId = toNormal.get(Integer.parseInt(tkn.nextToken()));
                int partitionId = Integer.parseInt(tkn.nextToken());
                if(partition[nodeId] != -1){
                    throw new Exception("Second group assignment at line " + lineCount);
                }
                partition[nodeId] = partitionId;
                assigmentCount++;
            } else if(Util.isNumber(tkn.nextToken())){
                throw new Exception("Error at line " + lineCount
                        + ". Each line must contain 'nodeId groupId'");
            }
        }
        if(assigmentCount != partition.length){
            throw new Exception("Some nodes are not assigned to any partition");
        }
        return partition;
    }
    /**
     * Write detection partition to file using the un-Normalized nodes
     * @param graph
     * @param partition
     * @param address
     */
    public static void writePartition(Graph graph, int[] partition, String address){
        writePartition(graph, new int[][]{partition}, new String[]{address});
    }

    /**
     * Write detection partition to file using the un-Normalized nodes
     * @param graph
     * @param partitions
     * @param addresses
     */
    public static void writePartition(Graph graph, int[][] partitions, String[] addresses){
        int[] toRaw = graph.getToRaw()[0]; // convert nodeIds back to un-normalized inputs
        BufferedWriter[] writers;
        try {
            writers = new BufferedWriter[partitions.length];
            for(int p = 0 ; p < partitions.length ; p++) {
                if(partitions[p] == null) continue;
                writers[p] = new BufferedWriter(new FileWriter(addresses[p]));
                int[] partition = partitions[p];
                for (int nodeId = 0; nodeId < partition.length; nodeId++) {
                    if (toRaw != null) {
                        writers[p].write(toRaw[nodeId] + "\t" + partition[nodeId] + "\n");
                    } else {
                        writers[p].write(nodeId + "\t" + partition[nodeId] + "\n");
                    }
                }
            }
            // Flush the writers
            for(int partitionId = 0 ; partitionId < writers.length ; partitionId++){
                if(writers[partitionId] == null) continue;
                writers[partitionId].flush();
            }
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
            }
            // Flush the writers
            for(int graphId = 0 ; graphId < writers.length ; graphId++){
                if(writers[graphId] == null) continue;
                writers[graphId].flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write array of evaluations
     * Each evaluation has a name (mostly the same as partition name) and a value
     * @param evaluations
     */
    public static void writeEvaluation(String[][] evaluations, String address){
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(address));
            for(int e = 0 ; e < evaluations.length ; e++) {
                writer.write(evaluations[e][0] + "\t" + evaluations[e][1] + "\n");
            }
            // Flush the writer
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
