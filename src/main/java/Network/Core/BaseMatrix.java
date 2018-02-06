package Network.Core;

abstract public class BaseMatrix {
    /**
     * Matrix identifier to distinguish multiple instances
     */
    private int id;

    abstract public BaseMatrix transpose(boolean clone);
    abstract public BaseMatrix newInstance();
    public BaseMatrix[] decompose(int[] partition){
        return null;
    }
    public BaseMatrix fold(int[] partition){
        return null;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
