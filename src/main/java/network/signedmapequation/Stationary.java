package network.signedmapequation;

import network.core.Graph;
import network.core.Util;

public class Stationary {

    /**
     * Calculate visit probabilities of nodes and group of nodes, add the calculations to statistics
     * @param statistics
     * @param partition
     * @param tau
     * @return
     */
    public static SiMapStatistics visitProbabilities(SiMapStatistics statistics, int[] partition, float tau){
        statistics.nodeRecorded = nodeRecorded(statistics.transition
                , statistics.teleport, statistics.negativeTeleport, tau, 0.000000000000001f);
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
    public static double[] nodeUnRecorded(Graph transitionMatrix, double[] recorded,
                                          float[] negativeTeleport){
        double[] unrecorded = new double[recorded.length]; // unRecorded node visit probability
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
        int nodeCount = transitionMatrix.getNodeCount();
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
    public static double[] nodeRecorded(Graph transitionMatrix, float[] teleport,
                                        float[] negativeTeleport, double tau, double minDistance){
        int nodeCount = transitionMatrix.getNodeCount();
        double[] Pt = Util.doubleArray(nodeCount, 1.0f / transitionMatrix.getNodeCount()); // distribution at t-th step
        double[] Pt_1 = Pt.clone(); // distribution at (t-1)-th step
        double[] multiply = new double[nodeCount]; // holds the multiplication Pt_1 * G
        double distance = 0;
        while(distance > minDistance){
            double totalNegativeTeleport = 0;
            // Calculate sum(P(n) * Ptele(n)) (used in part of calculations)
            for(int nodeId = 0; nodeId < nodeCount ; nodeId++){
                totalNegativeTeleport += Pt[nodeId] * negativeTeleport[nodeId];
            }
            // Calculate P = P(t - 1) * G
            float[][] transitions = transitionMatrix.getValues();
            for(int nodeId = 0 ; nodeId < nodeCount; nodeId++){
                int[] neighbors = transitionMatrix.getColumns(nodeId);
                float[] transitionToNeighbor = transitions[nodeId];
                double visitingNodeId = Pt[nodeId];
                for(int n = 0 ; n < neighbors.length ; n++){
                    multiply[neighbors[n]] += visitingNodeId * transitionToNeighbor[n];
                }
            }
            // Update Pt to Pt+1, and calculate the distribution distance
            distance = 0;
            for(int nodeId = 0 ; nodeId < nodeCount ; nodeId++){
                Pt[nodeId] = tau * teleport[nodeId]
                        + (1 - tau) * multiply[nodeId]
                        + ((1 - tau) / nodeCount) * totalNegativeTeleport;
                // distance of Pt to Pt_1
                distance += Math.pow(Pt[nodeId] - Pt_1[nodeId], 2);
                Pt_1[nodeId] = Pt[nodeId];
            }
            distance = Math.sqrt(distance);
        } // while convergence
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
    public static double[] group(SiMapStatistics statistics, int[] partition, double tau, boolean useRecorded){
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
