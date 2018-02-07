package network.core;

abstract public class AbstractMatrix {
    /**
     * Matrix identifier to distinguish multiple instances
     */
    private int id;

    abstract public AbstractMatrix transpose(boolean clone);
    abstract public AbstractMatrix newInstance();
    public AbstractMatrix[] decompose(int[] partition){
        return null;
    }
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
