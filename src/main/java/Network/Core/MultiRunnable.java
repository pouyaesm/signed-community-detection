package Network.Core;

import java.util.ArrayList;

/**
 * Executes multiple Runnable-s sequentially
 * This is used when number of sub problems are larger than thread groupCount
 * So each thread executes multiple sub problems sequentially
 */
public class MultiRunnable implements Runnable {

    ArrayList<Runnable> runnables;

    public MultiRunnable(){
        runnables = new ArrayList<>();
    }

    /**
     * Add runnable to be executed sequentially
     * @param runnable
     * @return
     */
    public MultiRunnable add(Runnable runnable){
        runnables.add(runnable);
        return this;
    }

    public int size(){
        return runnables.size();
    }

    public Runnable get(int index){
        return runnables.get(index);
    }


    @Override
    public void run() {
        for(Runnable runnable : runnables){
            runnable.run();
        }
    }
}
