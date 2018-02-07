package network.signedmapequation;

import network.core.Graph;
import network.core.ListMatrix;
import network.core.Util;
import network.optimization.CPMapParameters;
import network.optimization.ObjectiveParameters;

public class SiMap {

    public static int[] detect(Graph graph, ObjectiveParameters parameters){
        CPMapParameters cpMapParameters = (CPMapParameters) parameters;

        return null;
    }

    public static double evaluate(Graph graph, int[] partition, ObjectiveParameters parameters) {
        CPMapParameters cpMapParameters = (CPMapParameters) parameters;
        SiMapStatistics statistics = reWeight(graph, partition);
        // Teleport probabilities from each node to guarantee stationary state of G * p = p
        int nodeCount = statistics.transition.getNodeCount();
        statistics.teleport = new float[nodeCount];
        if(cpMapParameters.TELEPORT_TO_NODE){
            float probability = 1.0f / nodeCount;
            for(int nodeId = 0 ; nodeId < nodeCount ; nodeId++){
                statistics.teleport[nodeId] = probability;
            }
        }else{
            if(cpMapParameters.USE_RECORDED){
                float totalInWeight = Util.sum(statistics.inWeight);
                for(int nodeId = 0 ; nodeId < nodeCount ; nodeId++){
                    statistics.teleport[nodeId] = statistics.inWeight[nodeId] / totalInWeight;
                }
            }else{
                float totalOutWeight = Util.sum(statistics.outWeight);
                for(int nodeId = 0 ; nodeId <  nodeCount; nodeId++){
                    statistics.teleport[nodeId] = statistics.outWeight[nodeId] / totalOutWeight;
                }
            }
        }
        statistics = Stationary.visitProbabilities(statistics, partition, cpMapParameters.TAU);
        // Calculate the description length of random step
        // based visiting probabilities of nodes and groups
        double descriptionLength;
        if(cpMapParameters.USE_RECORDED){
            descriptionLength = getDescriptionLength(
                    statistics.nodeRecorded, statistics.groupRecorded, partition);
        }else{
            descriptionLength = getDescriptionLength(
                    statistics.nodeUnRecorded, statistics.groupUnRecorded, partition);
        }
        return descriptionLength;
    }

    /**
     * Calculate discription length for given visiting probabilities and partitioning
     * @param pNode
     * @param pGroup
     * @param partition
     * @return
     */
    private static double getDescriptionLength(
            double[] pNode, double[] pGroup, int[] partition){
        // Aggregate node visit probabilities based on their group ids
        double[] pSum = Util.aggregate(pNode, partition);
        double[] pTotal = Util.sum(pGroup, pSum);
        double groupSum = Util.sum(pGroup);
        // Avoid log(0) by replacing 0's with 1's resulting in log(1) = 0
        if(groupSum == 0.0) groupSum = 1; // e.g. when there is no links and no teleportation
        Util.replace(pGroup, 0, 1);
        Util.replace(pTotal, 0, 1);
        Util.replace(pNode, 0, 1);
        // Description Length
        double descriptionLength =
                groupSum * Util.log2(groupSum)
                - 2 * Util.dot(pGroup, Util.log2(pGroup))
                + Util.dot(pTotal, Util.log2(pTotal))
                - Util.dot(pNode, Util.log2(pNode));
        return descriptionLength;
    }
    /**
     * Re-weight the graph links based on the extended map equation
     * @param graph
     * @param partition
     * @return re-weighted transition probability graph, negative teleport, in/out weight per nodeId
     */
    public static SiMapStatistics reWeight(Graph graph, int[] partition){
        SiMapStatistics statistics = new SiMapStatistics();
        float[][] weights = graph.getValues();
        statistics.transition = graph.getTransitionProbability();
        int nodeCount = statistics.transition.getNodeCount();
        int groupRangeId = Util.max(partition) + 1;
        /*
            Queue of neighbor groups and their statistics for a specific nodeId
            (groupId, positiveLink, negativeLink, outCoefficient, inCoefficient)
            outCoefficient: positive external re-weight coefficients from nodeId to neighborGroupId
            Node: this queue will be reset after processing each nodeId, for the next nodeId
        */
        float[][] groupQueue = new float[groupRangeId][];
        int queueHead = 0; // Head of queue indicating the first empty cell of queue array to insert
        /*
            neighborGroupQueueIndex[ng] = q means group ng is a neighbor of current group g
            and it is placed in position q of groupQueue
         */
        int[] neighborGroupQueueIndex = Util.intArray(groupRangeId, -1);
        float[] inPositive = new float[nodeCount]; // node's positive weights inside its group
        float[] inNegative = new float[nodeCount]; // node's negative weights inside its group
        float[] totalPositive = new float[nodeCount]; // node's total positive weight
        float[] backProbability = new float[nodeCount]; // backward probability of each node toward its group
        statistics.negativeTeleport = new float[nodeCount]; // negative teleport probability emitted from each node
        statistics.inWeight = new float[nodeCount]; // total weight toward nodeId after re-weight
        statistics.outWeight = new float[nodeCount]; // total weight from nodeId after re-weight
        // inCoefficient: positive internal re-weight coefficients
        float[] inCoefficient = new float[nodeCount];
        // Use the transition weights for re-weighting and generation of re-weighted graph
        float[][] reWeights = statistics.transition.getValues();
        for(int nodeId = 0 ; nodeId < nodeCount ; nodeId++){
            int groupId = partition[nodeId];
            int[] neighbors = statistics.transition.getColumns(nodeId);
            float[] neighborWeights = weights[nodeId];
            for(int n  = 0 ; n < neighbors.length ; n++){
                int neighborId = neighbors[n];
                float weight = neighborWeights[n];
                int neighborGroupId = partition[neighborId];
                if(weight > 0) totalPositive[nodeId] += weight; // total positive weight from nodeId
                if(groupId == neighborGroupId){ // link inside nodeId's group
                    if(weight > 0) inPositive[nodeId] += weight;
                    else inNegative[nodeId] -= weight;
                }else{ // nodeId link toward neighbor groups
                    // first time this neighbor is visited ?
                    int neighborQueueIndex;
                    if(neighborGroupQueueIndex[neighborGroupId] == -1){
                        if(groupQueue[queueHead] == null) groupQueue[queueHead] = new float[4];
                        groupQueue[queueHead][0] = neighborGroupId;
                        neighborQueueIndex = neighborGroupQueueIndex[neighborGroupId] = queueHead;
                        queueHead++;
                    }else{
                        neighborQueueIndex = neighborGroupQueueIndex[neighborGroupId];
                    }
                    if(weight > 0) groupQueue[neighborQueueIndex][1] += weight;
                    else groupQueue[neighborQueueIndex][2] -= weight;
                }
            } // for nodeId's all neighbors
            // Calculate backward probability of nodeId and outCoefficients of its neighbor groups
            for(int queueIndex = 0 ; queueIndex < queueHead ; queueIndex++){
                float[] neighborStatistics = groupQueue[queueIndex];
                float positiveWeight = neighborStatistics[1];
                float negativeWeight = neighborStatistics[2];
                if(positiveWeight > 0){
                    backProbability[nodeId] += Math.min(positiveWeight, negativeWeight) / totalPositive[nodeId];
                    neighborStatistics[3] = Math.max(1 - negativeWeight / positiveWeight, 0);
                }else{
                    // node negative link toward neighbor group has coefficient 0
                    neighborStatistics[3] = 0;
                }
            }
            // Calculate negative teleport emitted from each node
            statistics.negativeTeleport[nodeId] = backProbability[nodeId];
            // This coefficient is used to re-weight each positive link of nodeId toward inside its group
            if(inPositive[nodeId] > 0){
                inCoefficient[nodeId] = (1 + totalPositive[nodeId]
                        * backProbability[nodeId] / inPositive[nodeId])
                        * Math.max(1 - inNegative[nodeId] / inPositive[nodeId], 0);
                float internalProbability = inPositive[nodeId] / totalPositive[nodeId];
                statistics.negativeTeleport[nodeId] += (1 - inCoefficient[nodeId]) * internalProbability;
            }
            // Re-weight the transition probability for (nodeId, neighborId) transitions
            float[] reWeight = reWeights[nodeId];
            for(int n = 0 ; n < neighbors.length ; n++){
                float weight = reWeight[n]; // original probability (nodeId, neighborId)
                if(weight <= 0) continue; // only positive weights are re-weighted
                int neighborId = neighbors[n];
                int neighborGroupId = partition[neighborId];
                float probability;
                if(groupId == neighborGroupId){ // internal positive
                    probability = weight * inCoefficient[nodeId];
                }else{ // external positive
                    float outCoefficient = groupQueue[neighborGroupQueueIndex[neighborGroupId]][3];
                    probability =  weight * outCoefficient;
                }
                reWeight[n] = probability; // re-weighted probability (nodeId, neighborId)
                float newWeight = probability * totalPositive[nodeId];
                statistics.outWeight[nodeId] += newWeight;
                statistics.inWeight[neighborId] += newWeight;
            }
            // Clear the data structures for tracking the neighbor groups of next node
            for(int queueIndex = 0 ; queueIndex < queueHead ; queueIndex++){
                neighborGroupQueueIndex[(int) groupQueue[queueIndex][0]] = -1;
                for(int statisticsIndex = 0 ; statisticsIndex < 4 ; statisticsIndex++){
                    groupQueue[queueIndex][statisticsIndex] = 0;
                }
            }
            queueHead = 0; //reset queue header for next nodeId
        } // for each node
        // Total weight of negative teleports emitted
        float negativeTeleport = 0;
        for(int nodeId = 0 ; nodeId < nodeCount ; nodeId++){
            //weight of  negative teleport from nodeId
            float teleportWeight = totalPositive[nodeId] * statistics.negativeTeleport[nodeId];
            if(teleportWeight > 0) {
                negativeTeleport += teleportWeight;
                // Add negative teleport to out-weights of nodeId
                statistics.outWeight[nodeId] += teleportWeight;
            }
        }
        // Add negative teleports to in-weight of nodes
        for(int nodeId = 0 ; nodeId < nodeCount ; nodeId++){
            statistics.inWeight[nodeId] += negativeTeleport / nodeCount;
        }
        // remove zero weights from the re-weighted matrix (all negative and some positive weights)
        ListMatrix listMatrix = new ListMatrix()
                .init(reWeights, statistics.transition.getColumns(), true, false)
                .normalize();
        statistics.transition = new Graph(listMatrix);
        return statistics;
    }


}
