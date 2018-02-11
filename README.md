### Introduction 
==============

This package is a tool for community detection in signed, directed, and weighted networks.
This is the implementation of [our paper on community detection]([https://www.nature.com/articles/srep14339])
, which extends [Constant Potts Model](https://journals.aps.org/pre/abstract/10.1103/PhysRevE.84.016114) objective function
 that is optimized using [Louvain algorithm](https://arxiv.org/abs/0803.0476), and finds the scale parameter of constant potts model
 using the extended [Map Equation](http://www.pnas.org/content/105/4/1118) to signed networks.

#### Community detection
---

Input graph format is required to be:
```
id1     id2     weight
...
```
where each line represents a link from `id1` to `id2`.

To detect the communities of `graph.txt`, run the command below:
```
mdl --verbose -g graph.txt -o partition.txt
```
If a graph is undirected add `--undirected` parameter.

To detect a community at a specific scale (resolution)  value `0.001`, run:
```
mdl --verbose -r 0.001 -g graph.txt -o partition.txt
```

#### Community Evaluation
---

If you want to evaluate the quality of partition `partition.txt` 
and write the evaluation result to `mdl.txt`, run:

```
mdl -g graph.txt -p partition.txt -o mdl.txt
```

If you want to evaluate a group of partitions `partition-*.txt`, run:

```
mdl -g graph.txt -p partition-*.txt -o mdl.txt
```

Other options can be viewed by:
```
mdl -h
```

There is a set of tools for pre-processing a graph, see the options using:

```
preprocess -h
```

#### Performance
---

This algorithm requires `O(E)` memory space and `O(ElogE)` execution time for the detection
of communities in a signed network having `O(E)` links.

#### Citation
---

If you find this project useful, please cite the paper as follows:

```
Esmailian, P. and Jalili, M., 2015. Community detection in signed networks: the role of negative ties in different scales. Scientific reports, 5, p.14339.
```