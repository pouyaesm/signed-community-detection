package network.core;

public class SiGraph extends MultiGraph {
    // Ids for adding and retrieving positive/negative sub-graphs from multi-graph
    public final static int POSITIVE = 0;
    public final static int NEGATIVE = 1;

    public SiGraph(){

    }

    public SiGraph(MultiGraph multiGraph){
        super(multiGraph);
    }

    public SiGraph(Graph graph){
        Graph positive = graph.filter(0, Integer.MAX_VALUE);
        Graph negative  = graph.filter(Integer.MIN_VALUE, 0);
        addGraph(POSITIVE, positive);
        addGraph(NEGATIVE, negative);
    }

    @Override
    public SiGraph clone() {
        return new SiGraph(super.clone());
    }
}
