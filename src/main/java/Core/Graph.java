package Core;

public class Graph extends SparseMatrix {
    /**
     * Attributes per node
     */
    float[][] attributes;

    public Graph(){

    }

    public Graph(ListMatrix listMatrix){
        super(listMatrix);
    }

    /**
     * Decompose graph based on the partition, normalize node ids of each sub-graph
     * @param partitions
     * @return
     */
    public Graph[] decompose(int[] partitions){
        ListMatrix[] lists = getListMatrix().decompose(partitions);
        Graph[] graphs = new Graph[lists.length];
        for(int pr = 0 ; pr < lists.length; pr++){
            ListMatrix list = lists[pr].normalize(false, true);
            if(!hasAttributes()){
                continue; // no node attributes to copy
            }
            // Copy node attributes to new partitions
            int nodeCount = list.getRowCount();
            float[][] attributes = new float[nodeCount][];
            for(int normalizedId = 0 ; normalizedId < nodeCount ; normalizedId++){
                int rawId = list.getToRaw()[0][normalizedId];
                attributes[normalizedId] = getAttributes()[rawId].clone();
            }
            graphs[pr] = new Graph().setAttributes(attributes);
        }
        return graphs;
    }

    /**
     * Fold the nodes and edges inside each partition, into one node,
     * aggregate the attribute of members accordingly
     * @param partitions
     * @return
     */
    public Graph fold(int[] partitions){

        return null;
    }

    public Graph setAttributes(float[][] attributes) {
        this.attributes = attributes;
        return this;
    }

    public float[][] getAttributes() {
        return attributes;
    }

    public boolean hasAttributes(){
        return attributes != null;
    }
}
