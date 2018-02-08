package network.core;

import cern.colt.map.OpenIntIntHashMap;

/**
 * Holder of normalization maps data structure,
 * and the maximum normalized id used to build the reverse taw to normal maps
 */
public class NormalizeMap {

    public OpenIntIntHashMap map;
    public int maxNormalId;

    public NormalizeMap(){
        this.maxNormalId = -1;
    }

    public NormalizeMap(OpenIntIntHashMap map, int maxNormalId){
        this.map = map;
        this.maxNormalId = maxNormalId;
    }
}
