package Network.Optimization;

import Network.Core.Graph;
import Utils.MultiRunnable;
import Network.Core.Util;

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

    public int[][] detect(Graph[] graphs, int foldCount){
        int[][] initialPartitions = new int[graphs.length][];
        for(int graphId = 0 ; graphId < graphs.length ; graphId++){
            initialPartitions[graphId] = Util.ramp(graphs[graphId].getNodeCount());
        }
        return detect(graphs, initialPartitions, foldCount);
    }

    public int[][] detect(Graph[] graphs, int[][] initialPartitions, int foldCount){
        int[][] partitions = new int[graphs.length][];
        // Serial execution
        if(threadCount <= 1){
//            System.out.println("Run serial detection");
            Louvain detector = newDetector();
            for(int g = 0 ; g < graphs.length ; g++){
                partitions[g] = initialPartitions[g];
                if(graphs[g] == null || !graphs[g].hasEdge()) continue;
                // run the detector
                partitions[g] = detector.detect(graphs[g], initialPartitions[g], foldCount);
            }
            return partitions;
        }
        // Parallel execution
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
            partitions[g] = initialPartitions[g]; // as the default answer if no detection is carried out
            if(graphs[g] == null || !graphs[g].hasEdge()) continue; // no edge to detect
            // set the graphId for detector to distinguish it when the partitions are detected
            Louvain detector = newDetector().init(graphs[g], initialPartitions[g], foldCount).setId(g);
            workers[g % threadCount].add(detector);
        }
        // Run workers
//        System.out.println("Run parallel detection");
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
    abstract public ParallelLouvain newDetector();

    public ParallelLouvain setThreadCount(int threadCount) {
        this.threadCount = threadCount;
        return this;
    }

    public int getThreadCount() {
        return threadCount;
    }
}
