package network.signedmapequation;

import network.core.Graph;

public class SiMapStatistics {

    /**
     * inWeight[nodeId] is the total weight of nodeId toward the inside of its group
     */
    public double[] inWeight;

    /**
     * outWeight[nodeId] is the total weight of nodeId toward the outside of its group
     */
    public double[] outWeight;
    /**
     * negativeTeleport[nodeId] is the probability of random jump from nodeId calculated after re-weighting
     */
    public double[] negativeTeleport;

    /**
     * teleport[nodeId] is the probability of random jump from nodeId
     * to guarantee stationary state of G * p = p
     */
    public double[] teleport;

    /**
     * Re-weighted transition probability graph
     */
    public Graph transition;

    /**
     * Visiting probability of nodeId using nodeRecorded teleport
     */
    public double[] nodeRecorded;

    /**
     * Visiting probability of nodeId using unRecorded teleport
     */
    public double[] nodeUnRecorded;

    /**
     * Visiting (entering or existing) probability of groupId using nodeRecorded teleport
     */
    public double[] groupRecorded;

    /**
     * Visiting (entering or existing) probability of groupId using unRecorded teleport
     */
    public double[] groupUnRecorded;
}
