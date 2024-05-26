package network.extendedmapequation;

import network.Shared;
import network.core.Graph;
import network.core.Util;

public class Stationary {

    /**
     * Number of threads used for multi-thread multiplication
     */
    private int threadCount;

    public Stationary(int threadCount){
        this.threadCount = threadCount;
    }

    /**
     * Calculate visit probabilities of nodes and group of nodes, add the calculations to statistics
     * @param statistics
     * @param partition
     * @param tau
     * @return
     */
    public CPMapStatistics visitProbabilities(CPMapStatistics statistics, int[] partition, float tau){
        statistics.nodeRecorded = nodeRecorded(statistics.transition
                , statistics.teleport, statistics.negativeTeleport, tau, 0.0000000001);
        statistics.nodeUnRecorded = nodeUnRecorded(statistics.transition,
                statistics.nodeRecorded, statistics.negativeTeleport);
        statistics.groupRecorded = group(statistics, partition, tau, true);
        statistics.groupUnRecorded = group(statistics, partition, tau, false);
        return statistics;
    }

    /**
     * Find the nodeUnRecorded stationary state of transition matrix, and writePartition it to statistics
     * this is achieved by post-processing of calculated recorded probabilities
     * @param recorded recorded visiting probabilities
     * @param negativeTeleport
     * @return
     */
    public double[] nodeUnRecorded(Graph transitionMatrix, double[] recorded,
                                          double[] negativeTeleport){
        double[] unrecorded = new double[recorded.length]; // unRecorded node visit probability
        int nodeCount = transitionMatrix.getNodeCount();
        if (nodeCount == 0) return unrecorded;

        // Calculate P one step further without considering the teleportation
        for(int nodeId = 0 ; nodeId < recorded.length ; nodeId++){
            int[] neighbors = transitionMatrix.getColumns(nodeId);
            float[] transitionToNeighbor = transitionMatrix.getValues(nodeId);
            for(int n = 0 ; n < neighbors.length ; n++){
                unrecorded[neighbors[n]] += recorded[nodeId] * transitionToNeighbor[n];
            }
        }
        // Add 1/N of total teleport to each visiting probability
        double totalNegativeTeleport = 0;
        // Total negative teleportation in the air which goes into each node uniformly
        for(int nodeId = 0; nodeId < recorded.length ; nodeId++){
            totalNegativeTeleport += recorded[nodeId] * negativeTeleport[nodeId];
        }
        for(int nodeId = 0 ; nodeId < recorded.length ; nodeId++){
            unrecorded[nodeId] += totalNegativeTeleport / nodeCount;
        }
        return unrecorded;
    }
    /**
     * Find the nodeRecorded stationary state of transition matrix, and writePartition it to statistics
     * @param teleport
     * @param negativeTeleport
     * @param tau
     * @param minDistance minimum distance of two consecutive distributions to stop
     * @return
     */
    public double[] nodeRecorded(Graph transitionMatrix, double[] teleport,
                                        double[] negativeTeleport, double tau, double minDistance){
        int nodeCount = teleport.length;
        double[] Pt = Util.doubleArray(nodeCount, 1.0 / nodeCount); // distribution at t-th step
        double[] Pt_1 = Pt.clone(); // distribution at (t-1)-th step
        double distance = Integer.MAX_VALUE;
        int counter = 0; // number of iterations till convergence
        ParallelStationary multiplier = new ParallelStationary(threadCount);
        while(distance > minDistance){
            double totalNegativeTeleport = 0;
            // Calculate sum(P(n) * Ptele(n)) (used in part of calculations)
            for(int nodeId = 0; nodeId < nodeCount ; nodeId++){
                totalNegativeTeleport += Pt[nodeId] * negativeTeleport[nodeId];
            }
            // Multi-thread multiplication P(t - 1) * G
            double[] multiply = multiplier.multiply(transitionMatrix, Pt);
            // Update Pt to Pt+1, and calculate the distribution distance
            distance = 0;
            for(int nodeId = 0 ; nodeId < nodeCount ; nodeId++){
                Pt[nodeId] = tau * teleport[nodeId]
                        + (1.0 - tau) * multiply[nodeId]
                        + ((1.0 - tau) / nodeCount) * totalNegativeTeleport;
                // distance of Pt to Pt_1
                distance += Math.pow(Pt[nodeId] - Pt_1[nodeId], 2);
                Pt_1[nodeId] = Pt[nodeId];
            }
            distance = Math.sqrt(distance);
            counter++;
        } // while convergence
        Shared.log(counter + " iterations for calculating stationary distribution");
        return Pt;
    }


    /**
     * Calculate probability of entering or exiting the groups
     * @param statistics used for transition matrix, [negative teleport], node visiting probabilities
     * @param partition
     * @param tau
     * @param useRecorded
     * @return
     */
    public double[] group(CPMapStatistics statistics, int[] partition, double tau, boolean useRecorded){
        int nodeCount = statistics.transition.getNodeCount();
        int groupIdRange = Util.max(partition) + 1;
        // probability of exiting or entering groupId
        double[] Pg = new double[groupIdRange];
        // teleport coefficient (groupId) = 1 - sum of teleports of groupId nodes
        double[] teleCoef = new double[Pg.length];
        // negative teleport coefficient (groupId) = (N - N(groupId)) / N
        double[] negativeTeleCoef = new double[Pg.length];
        // probability of going from nodeId to groups other than its groupId
        double[] outProbability = new double[nodeCount];
        // Calculate probability of going from nodeId to groups other than its groupId
        for(int nodeId = 0 ; nodeId < nodeCount ; nodeId++){
            int groupId = partition[nodeId];
            int[] neighbors = statistics.transition.getColumns(nodeId);
            float[] probabilities = statistics.transition.getValues(nodeId);
            for (int n = 0 ; n < neighbors.length ; n++){
                int neighborGroupId = partition[neighbors[n]];
                if(groupId != neighborGroupId){
                    outProbability[nodeId] += probabilities[n];
                }
            }
        }
        // Calculate coefficients
        // First aggregate required statistics such as teleport(groupId) and N(groupId)
        for(int nodeId = 0 ; nodeId < nodeCount ; nodeId++){
            int groupId = partition[nodeId];
            teleCoef[groupId] += statistics.teleport[nodeId];
            negativeTeleCoef[groupId]++;
        }
        // Calculate the values
        for(int groupId = 0 ; groupId < Pg.length ; groupId++){
            teleCoef[groupId] = 1 - teleCoef[groupId]; // 1 - sum(node teleports)
            negativeTeleCoef[groupId] = (nodeCount - negativeTeleCoef[groupId]) / nodeCount;
        }
        // Calculate exiting/entering probability of each group
        for(int nodeId = 0 ; nodeId < nodeCount ; nodeId++){
            int groupId = partition[nodeId];
            // For both cases of [un]recorded group distribution,
            // node recorded must be used as the node stationary distribution
            if(useRecorded){ // consider teleportation steps in code length
                Pg[groupId] += tau * teleCoef[groupId] * statistics.nodeRecorded[nodeId]
                        + (1 - tau) * statistics.nodeRecorded[nodeId] *
                        (teleCoef[groupId] * statistics.negativeTeleport[nodeId] + outProbability[nodeId]);
            }else{
                Pg[groupId] += statistics.nodeRecorded[nodeId] *
                        (negativeTeleCoef[groupId] * statistics.negativeTeleport[nodeId] + outProbability[nodeId]);
            }
        }
        return Pg;
    }
}
