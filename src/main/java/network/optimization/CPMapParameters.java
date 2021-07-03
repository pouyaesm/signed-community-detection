package network.optimization;

/**
 * Parameters for the (CPM + Signed Map Equation) community detection
 */
public class CPMapParameters extends CPMParameters {

    /**
     * Probability of choosing to teleport instead of following the transition probability matrix
     * to guarantee convergence of G * p = p
     */
    public float TAU;

    /**
     * Teleport to links (better) or nodes?
     */
    public boolean TELEPORT_TO_NODE;

    /**
     * Use nodeRecorded teleport (uniform landing) or unrecorded (better)
     * Recorded: consider teleportation steps in code length
     */
    public boolean USE_RECORDED;

    /**
     * Number of refinements over Louvain output by
     * Rosvall-Bergstrom method. Leads to a more reliable detection
     */
    public int refineCount;

    /**
     * Number of threads used for matrix multiplication
     */
    public int threadCount;

    /**
     * Start resolution to search for the best resolution
     */
    public float resolutionStart;
    /**
     * End resolution
     */
    public float resolutionEnd;

    /**
     * Accuracy of the best solution, e.g. when accuracy is 0.1,
     * the solution is refined util this close to the best resolution found so far
     */
    public float resolutionAccuracy;

    /**
     * Constructor
     * @param tau
     * @param teleportToNode
     * @param useRecorded
     * @param refineCount
     * @param threadCount
     */
    public CPMapParameters(float tau, boolean teleportToNode, boolean useRecorded,
                           int refineCount, int threadCount){
        this.TAU = tau;
        this.TELEPORT_TO_NODE = teleportToNode;
        this.USE_RECORDED = useRecorded;
        this.refineCount = refineCount;
        this.threadCount = threadCount;
    }

    public CPMapParameters(float tau, boolean teleportToNode, boolean useRecorded,
                           float resolutionAccuracy, float resolutionStart, float resolutionEnd,
                           int refineCount, int threadCount){
        this.TAU = tau;
        this.TELEPORT_TO_NODE = teleportToNode;
        this.USE_RECORDED = useRecorded;
        this.refineCount = refineCount;
        this.threadCount = threadCount;
        this.resolutionStart = resolutionStart;
        this.resolutionEnd = resolutionEnd;
        this.resolutionAccuracy = resolutionAccuracy;
    }
}
