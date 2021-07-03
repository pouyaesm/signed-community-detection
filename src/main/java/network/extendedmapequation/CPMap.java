package network.extendedmapequation;

import network.Shared;
import network.core.*;
import network.optimization.CPM;
import network.optimization.CPMapParameters;
import network.optimization.ObjectiveParameters;

public class CPMap {

    public static int[] detect(Graph graph, ObjectiveParameters CPMapParameters){
        CPMapParameters parameters = (CPMapParameters) CPMapParameters;
        float start = parameters.resolutionStart;
        float length = parameters.resolutionEnd - start;
        float accuracy = parameters.resolutionAccuracy;
        int refineCount = parameters.refineCount;
        int threadCount = parameters.threadCount;
        // number of resolutions chosen from [start, start + length] to be evaluated
        // only two (4 and 5) needs to be recalculated on each solution refinement:
        // [1] [2] [3] -> [1] 4 [2] 5 [3]
        int count = 5;
        double[] mdl = Util.initArray(count, -1.0);// quality based on extended map equation
        double[] hamil = Util.initArray(count, -1.0);// quality based on CPM
        double bestMdl = Double.POSITIVE_INFINITY;
        float bestResolution = -1;
        int bestIndex = -1; // index of best resolution in array
        int[] bestPartition = null;
        CPM cpmDetector = (CPM) new CPM().setRefineCount(refineCount)
                .setThreadCount(threadCount);
        SiGraph siGraph = new SiGraph(graph);
        Shared.log("CPMap started");
        while(length > accuracy){
            float[] resolutions = Util.split(start, start + length, count);
            Shared.log("Search in [" + start + ", " + (start + length) + "]");
            for(int r = 0 ; r < mdl.length ; r++){
                if(mdl[r] >= 0) continue; // mdl has been calculated and compared before
                parameters.resolution = resolutions[r];
                parameters.alpha = 0.5f;
                Shared.log("---------------------------");
                int[] partition = cpmDetector.setResolution(resolutions[r]).detect(siGraph);
                mdl[r] = CPMap.evaluate(graph, partition, parameters);
                Shared.log(" Resolution: " + resolutions[r]);
                Shared.log(" MDL: " + mdl[r]);
                if(Shared.isVerbose()) {
                    hamil[r] = cpmDetector.evaluate(graph, partition, parameters);
                    Shared.log(" Hamiltonian(alpha="  + parameters.alpha + "): " + hamil[r]);
                }
                if(mdl[r] < bestMdl){
                    bestPartition = partition;
                    bestResolution = resolutions[r];
                    bestMdl = mdl[r];
                    bestIndex = r;
                }
            }
            if(bestIndex == 0) { // one interval ends was the best
                length = resolutions[1] - start; // refine the first sub-interval
                double secondMdl = mdl[1];
                for (int r = 0 ; r < mdl.length ; r++) mdl[r] = -1;
                mdl[0] = bestMdl;
                mdl[mdl.length - 1] = secondMdl;
            }else if (bestIndex == count - 1){
                length = resolutions[1] - start;
                double secondLastMdl = mdl[mdl.length - 2];
                for (int r = 0 ; r < mdl.length ; r++) mdl[r] = -1;
                mdl[0] = secondLastMdl;
                mdl[mdl.length - 1] = bestMdl;
            } else { // best one is neither of both ends
                // refine the solution from one step behind to one step after the best solution
                start = resolutions[bestIndex - 1]; // start one step before best one
                length = resolutions[bestIndex + 1] - start;
                double firstMdl = mdl[bestIndex - 1];
                double lastMdl = mdl[bestIndex + 1];
                for (int r = 0 ; r < mdl.length ; r++) mdl[r] = -1;
                // replace the three evaluated resolutions again
                bestIndex = count / 2;
                mdl[0] = firstMdl;
                mdl[bestIndex] = bestMdl;
                mdl[count - 1] = lastMdl;
            }
        }
        Shared.log("Best resolution: " + bestResolution);
        Shared.log("Best MDL: " + bestMdl);
        return bestPartition != null ? bestPartition : Util.ramp(graph.getNodeMaxId() + 1);
    }

    public static double evaluate(Graph graph, int[] partition, ObjectiveParameters CPMapParameters) {
        CPMapParameters parameters = (CPMapParameters) CPMapParameters;
        CPMapStatistics statistics = reWeight(graph, partition);
        // Teleport probabilities from each node to guarantee stationary state of G * p = p
        int nodeIdRange = statistics.transition.getNodeMaxId() + 1;
        int nodeCount = statistics.transition.getNodeCount();
        statistics.teleport = new double[nodeIdRange];
        if(parameters.TELEPORT_TO_NODE){
            double probability = 1.0f / nodeIdRange;
            for(int nodeId = 0 ; nodeId < nodeIdRange ; nodeId++){
                statistics.teleport[nodeId] = probability;
            }
        }else{
            if(parameters.USE_RECORDED){
                double totalInWeight = Util.sum(statistics.inWeight);
                for(int nodeId = 0 ; nodeId < nodeIdRange ; nodeId++){
                    statistics.teleport[nodeId] = statistics.inWeight[nodeId] / totalInWeight;
                }
            }else{
                double totalOutWeight = Util.sum(statistics.outWeight);
                for(int nodeId = 0 ; nodeId <  nodeIdRange; nodeId++){
                    statistics.teleport[nodeId] = statistics.outWeight[nodeId] / totalOutWeight;
                }
            }
        }
        statistics = new Stationary(parameters.threadCount)
                .visitProbabilities(statistics, partition, parameters.TAU);
        // Calculate the description length of random step
        // based visiting probabilities of nodes and groups
        double descriptionLength;
        if(parameters.USE_RECORDED){
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
        double[] pTotal = Util.sum(pGroup, pSum, false);
        double groupSum = Util.sum(pGroup);
        // Avoid log(0) by replacing 0's with 1's resulting in log(1) = 0
        if(groupSum == 0.0) groupSum = 1; // e.g. when there is no links and no teleportation
        double distanceToZero = 0.00000000001; // 1 to 100 billion
        Util.replace(pGroup, 0, 1, distanceToZero);
        Util.replace(pTotal, 0, 1, distanceToZero);
        Util.replace(pNode, 0, 1, distanceToZero);
        // Description Length
        double descriptionLength =
                groupSum * Util.log2(groupSum)
                - 2 * Util.dot(pGroup, Util.log2(pGroup))
                + Util.dot(pTotal, Util.log2(pTotal))
                - Util.dot(pNode, Util.log2(pNode));
        return descriptionLength;
    }
    /**
     * Re-weight the graph links based on the extended maps equation
     * @param graph
     * @param partition
     * @return re-weighted transition probability graph, negative teleport, in/out weight per nodeId
     */
    public static CPMapStatistics reWeight(Graph graph, int[] partition){
        CPMapStatistics statistics = new CPMapStatistics();
        float[][] weights = graph.getSparseValues();
        Graph transition = graph.getTransitionProbability();
        int nodeCount = transition.getNodeCount();
        int groupRangeId = Util.max(partition) + 1;
        /*
            Queue of neighbor groups and their statistics for a specific nodeId
            (groupId, positiveLink, negativeLink, outCoefficient, inCoefficient)
            outCoefficient: positive external re-weight coefficients from nodeId to neighborGroupId
            Node: this queue will be reset after processing each nodeId, for the next nodeId
        */
        double[][] groupQueue = new double[groupRangeId][];
        int queueHead = 0; // Head of queue indicating the first empty cell of queue array to insert
        /*
            neighborGroupQueueIndex[ng] = q means group ng is a neighbor of current group g
            and it is placed in position q of groupQueue
         */
        int[] neighborGroupQueueIndex = Util.initArray(groupRangeId, -1);
        double[] inPositive = new double[nodeCount]; // node's positive weights inside its group
        double[] inNegative = new double[nodeCount]; // node's negative weights inside its group
        double[] totalPositive = new double[nodeCount]; // node's total positive weight
        double[] backProbability = new double[nodeCount]; // backward probability of each node toward its group
        double[] negativeTeleport = new double[nodeCount]; // negative teleport probability emitted from each node
        double[] inWeight = new double[nodeCount]; // total weight toward nodeId after re-weight
        double[] outWeight = new double[nodeCount]; // total weight from nodeId after re-weight
        // inCoefficient: positive internal re-weight coefficients
        double[] inCoefficient = new double[nodeCount];
        // Use the transition weights for re-weighting and generation of re-weighted graph
        float[][] reWeights = transition.getSparseValues();
        for(int nodeId = 0 ; nodeId < nodeCount ; nodeId++){
            int groupId = partition[nodeId];
            int[] neighbors = transition.getColumns(nodeId);
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
                        if(groupQueue[queueHead] == null) groupQueue[queueHead] = new double[4];
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
                double[] neighborStatistics = groupQueue[queueIndex];
                double positiveWeight = neighborStatistics[1];
                double negativeWeight = neighborStatistics[2];
                if(positiveWeight > 0){
                    backProbability[nodeId] += Math.min(positiveWeight, negativeWeight) / totalPositive[nodeId];
                    neighborStatistics[3] = Math.max(1 - negativeWeight / positiveWeight, 0);
                }else{
                    // node negative link toward neighbor group has coefficient 0
                    neighborStatistics[3] = 0;
                }
            }
            // Calculate negative teleport emitted from each node
            negativeTeleport[nodeId] = backProbability[nodeId];
            // This coefficient is used to re-weight each positive link of nodeId toward inside its group
            if(inPositive[nodeId] > 0){
                inCoefficient[nodeId] = (1 + totalPositive[nodeId]
                        * backProbability[nodeId] / inPositive[nodeId])
                        * Math.max(1 - inNegative[nodeId] / inPositive[nodeId], 0);
                double internalProbability = inPositive[nodeId] / totalPositive[nodeId];
                negativeTeleport[nodeId] += (1 - inCoefficient[nodeId]) * internalProbability;
            }
            // Re-weight the transition probability for (nodeId, neighborId) transitions
            float[] reWeight = reWeights[nodeId];
            for(int n = 0 ; n < neighbors.length ; n++){
                float weight = reWeight[n]; // original probability (nodeId, neighborId)
                if(weight <= 0) continue; // only positive weights are re-weighted
                int neighborId = neighbors[n];
                int neighborGroupId = partition[neighborId];
                double probability;
                if(groupId == neighborGroupId){ // internal positive
                    probability = weight * inCoefficient[nodeId];
                }else{ // external positive
                    double outCoefficient = groupQueue[neighborGroupQueueIndex[neighborGroupId]][3];
                    probability =  weight * outCoefficient;
                }
                reWeight[n] = (float) probability; // re-weighted probability (nodeId, neighborId)
                double newWeight = probability * totalPositive[nodeId];
                outWeight[nodeId] += newWeight;
                inWeight[neighborId] += newWeight;
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
        double totalNegativeTeleport = 0;
        for(int nodeId = 0 ; nodeId < nodeCount ; nodeId++){
            //weight of  negative teleport from nodeId
            double teleportWeight = totalPositive[nodeId] * negativeTeleport[nodeId];
            if(teleportWeight > 0) {
                totalNegativeTeleport += teleportWeight;
                // Add negative teleport to out-weights of nodeId
                outWeight[nodeId] += teleportWeight;
            }
        }
        // Add negative teleports to in-weight of nodes
        for(int nodeId = 0 ; nodeId < nodeCount ; nodeId++){
            inWeight[nodeId] += totalNegativeTeleport / nodeCount;
        }
        // remove zero weights from the re-weighted matrix (all negative and some positive weights)
        ListMatrix transitionList = new ListMatrix()
                .init(reWeights, transition.getSparseColumns(), true, graph.isNormalized());
        statistics.negativeTeleport = negativeTeleport;
        statistics.inWeight = inWeight;
        statistics.outWeight = outWeight;
        statistics.transition = new Graph(transitionList);
        return statistics;
    }


}
