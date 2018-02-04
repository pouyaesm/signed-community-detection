package Network.Core;

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
     * Sort descending or ascending
     */
    private boolean descending;

    /**
     * Start of interval to apply sorting
     */
    private int start;

    /**
     * End of interval to apply sorting (inclusive)
     */
    private int end;

    public int[] sort(int[] values, boolean descending, boolean clone){
        return sort(values, 0, values.length - 1, descending, clone);
    }

    public int[] sort(int[] values, int start, int end, boolean descending, boolean clone){
        this.start = start;
        this.end = end;
        this.descending = descending;
        this.indices = new int[values.length];
        // initialized to not sorted
        for(int i = 0; i < indices.length ; i++){
            indices[i] = i;
        }
        if(clone){
            this.values = values.clone();
        }else{
            this.values = values;
        }
        quickSort(this.start, this.end);
        return this.values;
    }

    private void quickSort(int start, int end){
        if(start >= end) return;
        if(start < 0) return;
        if(end > indices.length - 1) return;

        int pivot = partition(start, end);
        quickSort(start, pivot - 1);
        quickSort(pivot + 1, end);
    }

    private int partition(int start, int end){

        //Get a random pivot between start and end
        int random = start + (int) (Math.random() * (end - start + 1));

        //New position of pivot element
        int last = end;

        //Move the pivot element to right edge of the array
        swap(random, end);
        end--;

        while(start <= end){
            boolean isDescending = values[start] > values[last];
            if((descending && !isDescending) || (!descending && isDescending)) {
                swap(start, end);
                end--;
            }else{
                start++; // go to next element
            }
        }

        //Move pivot element to its correct position
        swap(start, last);

        return start;
    }

    private void swap(int i, int j){
        int iIndex = indices[i];
        indices[i] = indices[j];
        indices[j] = iIndex;
        int iValue = values[i];
        values[i] = values[j];
        values[j] = iValue;
    }

    /**
     * Return new permuted array of values where values[i] <- values[sortedIndices[i]]
     * @param values
     * @return
     */
    public int[] permute(int[] values, boolean clone){
        int[] permutedValues = new int[end - start + 1];
        for(int i = 0 ; i < permutedValues.length ; i++){
            permutedValues[i] = values[indices[start + i]];
        }
        int[] fullPermutedValues = clone ? values.clone() : values;
        for(int i = 0 ; i < permutedValues.length ; i++){
            fullPermutedValues[start + i] = permutedValues[i];
        }
        return fullPermutedValues;
    }

    public float[] permute(float[] values, boolean clone){
        float[] permutedValues = new float[end - start + 1];
        for(int i = 0 ; i < permutedValues.length ; i++){
            permutedValues[i] = values[indices[start + i]];
        }
        float[] fullPermutedValues = clone ? values.clone() : values;
        for(int i = 0 ; i < permutedValues.length ; i++){
            fullPermutedValues[start + i] = permutedValues[i];
        }
        return fullPermutedValues;
    }


    public int[] getValues() {
        return values;
    }

    public int[] getIndices() {
        return indices;
    }
}