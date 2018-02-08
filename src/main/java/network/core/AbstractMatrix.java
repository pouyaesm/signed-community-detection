package network.core;

import cern.colt.map.OpenIntIntHashMap;

abstract public class AbstractMatrix {
    /**
     * Matrix identifier to distinguish multiple instances
     */
    private int id;

    abstract public AbstractMatrix transpose(boolean clone);
    abstract public AbstractMatrix newInstance();
    abstract public void onMatrixBuilt(); // called when data structure is built
    public AbstractMatrix[] decompose(int[] partition){
        return null;
    }
    public SparseMatrix[] decompose(int[] partition, OpenIntIntHashMap[] mapToNormal){return null;}
    public AbstractMatrix fold(int[] partition){
        return null;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
