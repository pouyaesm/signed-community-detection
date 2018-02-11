package network.signedmapequation;

import network.core.SparseMatrix;
import network.core.Util;
import network.optimization.Louvain;
import network.utils.Entry;
import network.utils.MultiRunnable;

/**
 * Calculates P * G matrix multiplication in parallel
 * where matrix G is row-based sparse
 */
public class ParallelStationary implements Runnable {
    /**
     * Number of threads for parallel computing
     */
    private int threadCount;

    /**
     * Thread groupCount is set toRow one by default
     */

    /**
     * Transition matrix for multiplication
     */
    private SparseMatrix transition;

    /**
     * Distribution vector containing visiting probability of each node
     */
    private double[] distribution;

    /**
     * Result of partial multiplication of distribution * transition
     */
    private double[] multiplication;
    /**
     * Multiply fromRow this row of transition
     */
    private int fromRow;

    /**
     * To this row if transition
     */
    private int toRow;

    public ParallelStationary(int threadCount){
        setThreadCount(threadCount);
    }

    public ParallelStationary(SparseMatrix transition, double[] distribution, int from, int to){
        this.transition = transition;
        this.distribution = distribution;
        this.fromRow = from;
        this.toRow = to;
    }

    public double[] multiply (SparseMatrix transition, double[] distribution){
        if(threadCount < 1) return null;
        // Parallel execution
        Thread[] threads = new Thread[threadCount];
        ParallelStationary[] multipliers = new ParallelStationary[threadCount];
        // Number of rows per thread
        int rowQuota = distribution.length / threadCount;
        int[] rowInterval = new int[threadCount + 1];
        rowInterval[0] = 0;
        rowInterval[threadCount] = distribution.length;
        for(int t = 1 ; t < threadCount ; t++){
            rowInterval[t] = rowInterval[t - 1] + rowQuota;
        }
        for(int t = 0 ; t < threadCount ; t++){
            ParallelStationary multiplier = new ParallelStationary(
                    transition, distribution, rowInterval[t], rowInterval[t + 1]);
            threads[t] = new Thread(multiplier);
            multipliers[t] = multiplier;
        }
        for(Thread thread : threads){
            thread.start();
        }
        // Accumulate the detected partitions
        try {
            double[] multiplication = new double[distribution.length];
            for (int t = 0; t < threadCount; t++) {
                threads[t].join(); // wait for this thread to finish its job
                ParallelStationary multiplier = multipliers[t];
                Util.sum(multiplication, multiplier.getMultiplication(), true);
            }
            return multiplication;
        }catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ParallelStationary setThreadCount(int threadCount) {
        this.threadCount = threadCount;
        return this;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public double[] getMultiplication() {
        return multiplication;
    }

    public int getFromRow() {
        return fromRow;
    }

    public int getToRow() {
        return toRow;
    }

    @Override
    public void run() {
        this.multiplication = new double[transition.getMaxRowId() + 1];
        for(int nodeId = fromRow; nodeId < toRow; nodeId++){
            float[] transitionTo = transition.getValues(nodeId);
            int[] neighbors = transition.getColumns(nodeId);
            for(int n = 0 ; n < neighbors.length ; n++){
                multiplication[neighbors[n]] += distribution[nodeId] * transitionTo[n];
            }
        }
    }
}
