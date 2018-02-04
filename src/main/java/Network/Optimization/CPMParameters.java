package Network.Optimization;

public class CPMParameters extends ObjectiveParameters {
    public float KC; // weight from node set K to group C, where group(K) = C
    public float  CK;
    public float KCp; // weight from node set K to group Cp, where group(K) <> C
    public float CpK;
    public int Nk; // size of node set K
    public int NC; // size of group C (including K)
    public int NCp; // size of group Cp (including K)
    public float resolution; // resolution parameter of CPM objective function
    public float alpha; // relative importance of positive links to negative links [0, 1]

    public float Kin; // weight toward node set K
    public float Kout; // weight from node set K
    public float Kself; // self-loop weigh of node set K
}
