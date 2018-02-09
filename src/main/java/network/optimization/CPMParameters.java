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

    public CPMParameters(){

    }

    public CPMParameters(double resolution, double alpha){
        this.resolution = resolution;
        this.alpha = alpha;
    }
}
