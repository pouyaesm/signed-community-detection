package Utils;

import Network.Core.Util;

import java.util.concurrent.ThreadLocalRandom;

public class QuickSort {

    /**
     * Input values
     */
    private int[] values;

    /**
     * Sorted indices
     */
    private int[] indices;

    /**
     * For keeping track of shifted values based on sorted indices
     */
    private boolean[] visited;

    /**
     * Sort descending or ascending
     */
    private boolean descending;

    private int depth;

    public QuickSort (int[] values){
        indices = Util.ramp(values.length);
        visited = new boolean[values.length];
        this.values = values;
    }

    public QuickSort sort(boolean descending){
        return sort(0, values.length - 1, descending);
    }

    public QuickSort sort(int start, int end, boolean descending){
        this.descending = descending;
        depth = 0;
        quickSort(start, end);
        return this;
    }

    private void quickSort(int start, int end){
        if(start >= end) return;
        depth++;
        if(depth > 500){
            throw new StackOverflowError("WOW!");
        }
        int pivot = partition(start, end);
        quickSort(start, pivot - 1);
        depth--;
        quickSort(pivot + 1, end);
        depth--;
    }

    private int partition(int start, int end){
        //Get a random pivot between start and end
        int pivotIndex =  ThreadLocalRandom.current().nextInt(start, end + 1);
        int pivotValue = values[indices[pivotIndex]];
        //Move the pivot element to right edge of the array
        swap(pivotIndex, end);
        // For descending, place values larger than pivot to the left of swapIndex (closer to 0)
        // Then put the pivot in swapIndex
        pivotIndex = start;
        for(int k = start ; k < end ; k++){
            if(descending){
                if(values[indices[k]] > pivotValue)  swap(pivotIndex++, k);
            }else{
                if(values[indices[k]] < pivotValue)  swap(pivotIndex++, k);
            }
        }
        //Move pivot element to its correct position
        swap(pivotIndex, end);
        return pivotIndex;
    }

    private void swap(int i, int k){
        int iIndex = indices[i];
        indices[i] = indices[k];
        indices[k] = iIndex;
    }

    /**
     * Return the permuted values (no copy)
     * @param values this will be changed
     * @return
     */
    public int[] permute(int[] values){
        for(int i = 0 ; i < indices.length ; i++){
            if(visited[i]) continue;
            int targetId = i;
            int startValue = values[targetId];
            int sourceId = indices[targetId];
            // while the loop start is not revisited
            while(!visited[sourceId]) {
                visited[targetId] = true;
                values[targetId] = values[sourceId];
                targetId = sourceId;
                sourceId = indices[targetId];
            }
            values[targetId] = startValue;
        }
        // reset visited array (to avoid array creation on each permute)
        for(int id = 0 ; id < visited.length ; id++){
            visited[id] = false;
        }
        return values;
    }

    public float[] permute(float[] values){
        for(int i = 0 ; i < indices.length ; i++){
            if(visited[i]) continue;
            int targetId = i;
            float startValue = values[targetId];
            int sourceId = indices[targetId];
            // while the loop start is not revisited
            while(!visited[sourceId]) {
                visited[targetId] = true;
                values[targetId] = values[sourceId];
                targetId = sourceId;
                sourceId = indices[targetId];
            }
            values[targetId] = startValue;
        }
        // reset visited array (to avoid array creation on each permute)
        for(int id = 0 ; id < visited.length ; id++){
            visited[id] = false;
        }
        return values;
    }

    public QuickSort setValues(int[] values) {
        this.values = values;
        return this;
    }

    public int[] getValues() {
        return values;
    }

    public int[] getIndices() {
        return indices;
    }
}