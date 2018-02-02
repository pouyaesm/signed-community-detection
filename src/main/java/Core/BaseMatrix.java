package Core;

abstract public class BaseMatrix {
    abstract public BaseMatrix transpose(boolean clone);
    abstract public BaseMatrix newInstance();
    public BaseMatrix[] decompose(int[] partition){
        return null;
    }
    public BaseMatrix fold(int[] partition){
        return null;
    }
}
