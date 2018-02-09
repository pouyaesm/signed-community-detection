package network.core;

import cern.colt.map.OpenIntIntHashMap;

abstract public class AbstractMatrix {
    /**
     * Matrix identifier to distinguish multiple instances
     */
    private int id;

    abstract public AbstractMatrix transpose(boolean clone);
    public AbstractMatrix[] decompose(int[] partition){
        return null;
    }
    public SparseMatrix[] decompose(int[] partition, OpenIntIntHashMap[] mapToNormal){return null;}
    public AbstractMatrix fold(int[] partition){
        return null;
    }
    abstract public boolean isEmpty();
    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
