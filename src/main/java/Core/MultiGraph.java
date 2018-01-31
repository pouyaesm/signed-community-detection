package Core;

import java.util.ArrayList;

/**
 * A graph with multiple types of edges
 */
public class MultiGraph {
    private ArrayList<Graph> graphs;

    public MultiGraph(){
        graphs =  new ArrayList<>();
    }

    public MultiGraph addGraph(Graph graph){
        graphs.add(graph);
        return this;
    }

    public Graph getGraph(int index) {
        return graphs.get(index);
    }
}
