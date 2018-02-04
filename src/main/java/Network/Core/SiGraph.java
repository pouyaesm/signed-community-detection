package Network.Core;

public class SiGraph extends MultiGraph{
    /**
     * Constants used for id of graphs to add and retrieve
     */
    public final static int POSITIVE = 1;
    public final static int NEGATIVE = 2;

    @Override
    public SiGraph newInstance() {
        return new SiGraph();
    }
}
