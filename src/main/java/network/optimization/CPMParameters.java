package network.optimization;

public class CPMParameters extends ObjectiveParameters {
    public double KC; // weight from node set K to group C, where group(K) = C
    public double  CK;
    public double KCp; // weight from node set K to group Cp, where group(K) <> C
    public double CpK;
    public int Nk; // size of node set K
    public int NC; // size of group C (including K)
    public int NCp; // size of group Cp (including K)
    public double resolution; // resolution parameter of CPM objective function
    public double alpha; // relative importance of positive links to negative links [0, 1]

    public double Kin; // weight toward node set K
    public double Kout; // weight from node set K
    public double Kself; // self-loop weigh of node set K

    /**
     * Number of refinements over Louvain output by
     * Rosvall-Bergstrom method. Leads to a more reliable detection
     */
    public int refineCount;

    /**
     * Random seed for reproducibility (deterministic results)
     */
    public int randomSeed;

    public CPMParameters(){
        this.alpha = 0.5; // same weight for negative and positive edges
        this.randomSeed = -1;   // seed will be selected randomly
    }

    public CPMParameters setResolution(double resolution) {
        this.resolution = resolution;
        return this;
    }

    public CPMParameters clone(){
        CPMParameters parameters = new CPMParameters();
        parameters.KC = KC;
        parameters.CK = CK;
        parameters.KCp = KCp;
        parameters.CpK = CpK;

        parameters.Nk = Nk;
        parameters.NC = NC;
        parameters.NCp = NCp;

        parameters.Kin = Kin;
        parameters.Kout = Kout;
        parameters.Kself = Kself;

        parameters.resolution = resolution;
        parameters.alpha = alpha;
        parameters.refineCount = refineCount;
        parameters.randomSeed = randomSeed;
        return parameters;
    }
}
