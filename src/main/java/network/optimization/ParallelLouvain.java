package network.optimization;

import network.core.MultiGraph;
import network.core.Util;
import network.utils.Entry;
import network.utils.MultiRunnable;

import java.util.PriorityQueue;

/**
 * Runs Louvain algorithm on multiple graphs on parallel
 */
abstract public class ParallelLouvain extends Louvain {

    /**
     * Number of threads for parallel computing
     */
    private int threadCount;

    /**
     * Thread groupCount is set to one by default
     */
    public ParallelLouvain(){
        setThreadCount(1);
    }

    public int[][] detect(MultiGraph[] graphs, int foldCount){
        int[][] initialPartitions = new int[graphs.length][];
        for(int graphId = 0 ; graphId < graphs.length ; graphId++){
            initialPartitions[graphId] = Util.ramp(graphs[graphId].getNodeMaxId() + 1);
        }
        return detect(graphs, initialPartitions, foldCount);
    }

    public int[][] detect(MultiGraph[] graphs, int[][] initialPartitions, int foldCount){
        int[][] partitions = new int[graphs.length][];
        // Serial execution
        if(threadCount <= 1 || graphs.length == 1){
//            Shared.log("Run serial detection");
            Louvain detector = newInstance();
            for(int g = 0 ; g < graphs.length ; g++){
                partitions[g] = initialPartitions[g];
                if(graphs[g] == null || graphs[g].isEmpty()) continue;
                partitions[g] = detector.detect(graphs[g], initialPartitions[g], foldCount);
            }
            return partitions;
        }
        // Parallel execution
        int threadCount = Math.min(graphs.length, getThreadCount()); // no more than no. graphs
        MultiRunnable[] workers = new MultiRunnable[threadCount]; // one worker per thread
        Thread[] threads = new Thread[threadCount];
        // threadLoad is used to balance work load based on graph sizes
        PriorityQueue<Entry> threadLoad = new PriorityQueue<>(threadCount);
        for(int t = 0 ; t < threadCount ; t++){
            workers[t] = new MultiRunnable();
            threads[t] = new Thread(workers[t], "Louvain " + t);
            threads[t].setPriority(Thread.MAX_PRIORITY);
            threadLoad.add(new Entry(t, 0)); // each thread is initialized with zero load
        }
        // Assign each louvain detector to a worker
        // each worker roughly runs the same number of algorithms by using modulo %
        for(int g = 0 ; g < graphs.length ; g++){
            partitions[g] = initialPartitions[g]; // as the default answer if no detection is carried out
            if(graphs[g] == null || graphs[g].isEmpty()) continue; // no edge to detect
            // set the graphId for detector to distinguish it when the partitions are detected
            Louvain detector = newInstance().init(graphs[g], initialPartitions[g], foldCount).setId(g);
            // Add the job to lightest thread load, then re-insert it into priority queue
            Entry load = threadLoad.poll();
            workers[load.getKey()].add(detector);
            threadLoad.add(load.add(graphs[g].getEdgeCount())); // add the edge to load
        }
        // Run workers
//        Shared.log("Run parallel detection");
        for(Thread thread : threads){
            thread.start();
        }
        // Accumulate the detected partitions
        try {
            for (int workerId = 0; workerId < threadCount; workerId++) {
                threads[workerId].join(); // wait for this worker to finish its job
                MultiRunnable worker = workers[workerId];
                int detectorCount = worker.size();
                for(int detectorId = 0 ; detectorId < detectorCount ; detectorId++){
                    Louvain detector = (Louvain) worker.get(detectorId);
                    partitions[detector.getId()] = detector.getPartition();
                }
            }
            return partitions;
        }catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Instantiate a detector to carry on the task
     * @return
     */
    abstract public ParallelLouvain newInstance();

    public ParallelLouvain setThreadCount(int threadCount) {
        this.threadCount = threadCount;
        return this;
    }

    public int getThreadCount() {
        return threadCount;
    }
}
