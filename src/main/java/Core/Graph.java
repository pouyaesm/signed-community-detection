package Core;

import static Core.ListMatrix.ROW;

public class Graph extends SparseMatrix {
    /**
     * Attributes per node
     */
    float[][] attributes;

    public Graph(ListMatrix listMatrix){
        super(listMatrix);
    }

    /**
     * Decompose graph based on the partition, normalize node ids of each sub-graph
     * @param partition
     * @return
     */
    @Override
    public Graph[] decompose(int[] partition){
        Graph[] graphs = (Graph[]) super.decompose(partition);
        for(int pr = 0 ; pr < graphs.length; pr++){
            ListMatrix list = graphs[pr].getListMatrix().normalize(false, true);
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
            graphs[pr].setAttributes(attributes);
        }
        return graphs;
    }

    /**
     * Fold the graph based on the partition, aggregate node attributes
     * @param partition
     * @return
     */
    @Override
    public Graph fold(int[] partition){
        Graph folded = (Graph) super.fold(partition);
        if (attributes == null) {
            return folded; // no attributes to aggregate
        }
        // Aggregate the attributes of nodes into their superNode
        ListMatrix listMatrix = folded.getListMatrix();
        int groupCount = listMatrix.getRowCount();// node count after fold = number of groups
        int attributeCount = this.attributes[0].length;
        float[][] attributes = new float[groupCount][attributeCount];
        // Aggregate attribute of nodes into their group node
        // Assumption: superNodes are normalized version of their groupIds in partition
        int[] groupToSuperGroupId = listMatrix.getToNormal()[ROW];
        for(int nodeId = 0 ; nodeId < partition.length ; nodeId++){
            int superGroupId = groupToSuperGroupId[partition[nodeId]];
            for(int attr = 0 ; attr < attributeCount ; attr++){
                attributes[superGroupId][attr] += this.attributes[nodeId][attr];
            }
        }
        folded.setAttributes(attributes);
        return folded;
    }

    /**
     * Number of graph nodes (row count = column count)
     * @return
     */
    public int getNodeCount(){
        return hasList() ? getListMatrix().getRowCount() : 0;
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

    /**
     * Does graph have edge?
     * @return
     */
    public boolean hasEdge(){
        // Each member of the list matrix is an edge
        return hasList();
    }

    @Override
    public Graph newInstance() {
        return new Graph(null);
    }
}
