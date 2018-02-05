package Network.Optimization;

import Network.Core.Graph;
import Network.Core.MultiRunnable;

/**
 * Runs Louvain algorithm on multiple graphs on parallel
 */
abstract public class ParallelLouvain extends Louvain {

    public int[][] detect(Graph[] graphs, int foldCount, int threadCount){
        int[][] initialPartitions = new int[graphs.length][];
        for(int graphId = 0 ; graphId < graphs.length ; graphId++){
            initialPartitions[graphId] = getInitialPartition(graphs[graphId].getNodeCount());
        }
        return detect(graphs, initialPartitions, foldCount, threadCount);
    }

    public int[][] detect(Graph[] graphs, int[][] initialPartitions, int foldCount, int threadCount){
        MultiRunnable[] workers = new MultiRunnable[threadCount]; // one worker per thread
        Thread[] threads = new Thread[threadCount];
        for(int t = 0 ; t < threadCount ; t++){
            workers[t] = new MultiRunnable();
            threads[t] = new Thread(workers[t], "Louvain " + t);
            threads[t].setPriority(Thread.MAX_PRIORITY);
        }
        // Assign each louvain detector to a worker
        // each worker roughly runs the same number of algorithms by using modulo %
        for(int g = 0 ; g < graphs.length ; g++){
            if(graphs[g] == null || !graphs[g].hasEdge()) continue;
            // set the graphId for detector to distinguish it when the partitions are detected
            Louvain detector = newDetector().init(graphs[g], initialPartitions[g], foldCount).setId(g);
            workers[g % threadCount].add(detector);
        }
        // Run workers
        for(Thread thread : threads){
            thread.start();
        }
        // Accumulate the detected partitions
        try {
            int[][] partitions = new int[graphs.length][];
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
    abstract public ParallelLouvain newDetector();
}
