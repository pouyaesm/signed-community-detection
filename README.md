# Introduction 

This package is a tool for community detection and evaluation in signed and weighted networks.
This is an implementation of [our paper on community detection](https://www.nature.com/articles/srep14339), which extends [Constant Potts Model](https://journals.aps.org/pre/abstract/10.1103/PhysRevE.84.016114) (CPM) objective function optimized using [Louvain algorithm](https://arxiv.org/abs/0803.0476), and 
 uses the extended [Map Equation](http://www.pnas.org/content/105/4/1118) for signed networks to find the scale parameter of CPM. 
 Quality of arbitrary partitions can be evaluated using the Map Equation as a more robust variant of Modularity; see this [experimental](https://arxiv.org/abs/0908.1062) 
 and [theoretical](https://arxiv.org/abs/1402.4385) results. 

## Installation

To build the jar file, install [Apache Maven](https://maven.apache.org/download.cgi),
then run `mvn package` in the root directory of project.  
You can also download the jar file from [here (v1.1.3, 177kb)](https://drive.google.com/file/d/1P9i_EyMH9w0YLyhqXLrzS198KFwLXDPH/view).

Start using the program by running `java -jar <filename> -h`.



## Community detection

Format of input graph should be:
```
id1     id2     weight
...
```
where each line represents a link from node `id1` to `id2`; 
graph is considered undirected by default.

To detect the communities of `graph.txt`, run the command below:
```
mdl --verbose -g graph.txt -o partition.txt
```
each line of `partition.txt` will be `node_id    partition_id`.

To detect communities at a specific resolution (a.k.a. scale) `0.001`, run:
```
mdl --verbose -r 0.001 -g graph.txt -o partition.txt
```

To detect communities at a specific interval-accuracy of resolution, run:
```
mdl --verbose -i 0.01 0.05 -a 0.01 -g graph.txt -o partition.txt
```

Generally, by sliding the resolution from 0 to 1, detected communities become smaller and denser.

Setting r = 0 is equivalent to [Correlation Clustering](https://link.springer.com/article/10.1023/B:MACH.0000033116.57574.95)
that aims to minimize the number of negative (positive) edges inside (between) clusters regardless of the edge density.

## Community evaluation

To evaluate the quality of an arbitrary partition `partition.txt` 
and write the evaluation result to `mdl.txt`, run:

```
mdl -g graph.txt -p partition.txt -o mdl.txt
```
If a graph is directed add `--directed` parameter. Evaluation is based on extended Map Equation (MDL) which supports directed links.

To evaluate a group of partition files `partition-*.txt` at once, run:

```
mdl -g graph.txt -p partition-*.txt -o mdl.txt
```

Other options can be viewed by:
```
mdl -h
```

For pre-processing a graph, such as extracting its positive or negative subgraphs, run:

```
preprocess -h
```

## Performance

This algorithm requires `O(E)` memory space and `O(ElogE)` execution time for the detection
of communities in a signed network having `E` links.

## Side note

This implementation reproduces the reported results of our Matlab-MEX version for three real-world networks.
In that version, a variant of Louvain algorithm is used (proposed by [M. Rosvall and C. T. Bergstrom](http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.182.8134)), 
which basically refines the output of Louvain. 
You can use this variant by setting the refine-count `--refine` to non-zero; we suggest 3 to 4 refinements. On large graphs, each refinement takes 2-2.5x the vanilla Louvain, therefore refine-count is set to zero by default.

## Citation

If you find this project useful, please cite the paper as follows:

```
Esmailian, P. and Jalili, M., 2015. Community detection in signed networks: the role of negative ties in different scales. Scientific reports, 5, p.14339.
```