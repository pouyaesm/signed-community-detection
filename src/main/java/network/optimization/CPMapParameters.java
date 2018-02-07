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
}
