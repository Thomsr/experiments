# Witty

Here we describe how to run our solver Witty. The results of our experiments are inside
the folder `experiments/results/`. Each version of the algorithm has its own result file. The names of the
different algorithms are listed below. The format of these result files corresponds to the output format
described below.

## Requirements

- Java 11 or higher
- Gurobi 10.0.3 (used for the calculation of the pairLB)

## Installation

A precompiled JAR called `Code.jar` is included.
You can compile a JAR yourself by installing Gradle and running `./gradlew jar`.

## How to Use

Run the program with the command `java -jar Code.jar args` where `args` are the
input arguments detailed in the section "Input Arguments".

## Input Arguments

The program requires 11 input arguments to run:

1. The path to the directory containing the input data file.
2. The name of the input data file.
3. The path to an output file.
4. The ratio of examples that make up the input data file.
This argument has no effect on the program and just exists
as additional information for the output.
5. The seed used to randomly sample the examples in the input data set.
This argument has no effect on the program and just exists
as additional information for the output.
6. The maximum size of the decision tree.
This argument is only used by one of the algorithms.
7. The maximum number of seconds that this program is allowed to run.
8. A number identifying this problem instance.
9. The id of the algorithm that should be used. See section "Algorithms".
10. An upper bound for the size of the smallest optimal decision tree.
11. The time in milliseconds required to calculate the upper bound.

## Algorithms

The program supports different algorithms. Most algorithms try to calculate
a smallest optimal decision tree (marked as optimize). Only one of the algorithms
instead checks if an optimal decision tree of a given maximum size exists
(marked as decide).
Besides this, the main differences between the algorithms are the improvements
that they use.

The following table gives an overview of the different algorithms:

| id | name           | problem  | optimization <br/> strategy | uses dirty <br/> example priority | uses <br/> reduction rules | uses <br/> lower bounds | uses <br/> subset constraints | uses <br/> subset caching |
|----|----------------|----------|:---------------------------:|:---------------------------------:|:--------------------------:|:-----------------------:|:-----------------------------:|:-------------------------:|
| 0  | Naive          | optimize |              1              |                No                 |             No             |           No            |              No               |            No             |
| 1  | DirtyEPriority | optimize |              1              |                Yes                |             No             |           No            |              No               |            No             |
| 2  | Basic          | optimize |              1              |                Yes                |            Yes             |           No            |              No               |            No             |
| 3  | LB             | optimize |              1              |                Yes                |            Yes             |           Yes           |              No               |            No             |
| 4  | SubConst       | optimize |              1              |                Yes                |            Yes             |           Yes           |              Yes              |            No             |
| 5  | Witty          | optimize |              1              |                Yes                |            Yes             |           Yes           |              Yes              |            Yes            |
| 6  | Strategy 2     | optimize |              2              |                Yes                |            Yes             |           Yes           |              Yes              |            Yes            |
| 7  | Strategy 3     | optimize |              3              |                Yes                |            Yes             |           Yes           |              Yes              |            Yes            |
| 8  | Decision       | decide   |            None             |                Yes                |            Yes             |           Yes           |              Yes              |            Yes            |

## Input Format

An input file representing a dataset with `n` examples, `d` dimensions and `2` classes
should be formatted in the following way:

- The file represents a table in CSV-format.
- The first row represents the name of the columns.
- The `n` remaining rows represent the `n` examples of the dataset.
- The first `d` columns represent the `d` dimensions of the dataset and
should be filled with numeric values.
- The last column represents the class of each example and should only contain
the values `0` or `1`.

## Output Format

Once the program is done a single line will be added to the end of the
output file containing 36 values separated by a `;`. In the following, the
meaning of these values is described:

1. The problem id given as part of the input arguments.
2. The id of the used algorithm.
3. The name of the dataset.
4. The number of examples in the dataset.
5. The subset ratio given as part of the input arguments.
6. The subset seed given as part of the input arguments.
7. The number of dimensions in the dataset.
8. The maximum size of the decision tree given as part of the input arguments.
9. The maximum number of seconds that the program was allowed to run.
10. The runtime of the program in milliseconds.
11. The maximum memory in MiB that was used by the program during its runtime.
12. `true` if a timeout happened. `false` otherwise.
13. `true` if the program could find an optimal tree. `false` otherwise.
14. The size of the optimal tree.
15. The ratio of examples that are correctly classified by the optimal tree.
Should always be `1.0`.
16. The number of search tree nodes checked by the algorithm.
17. The number of times the ImpLB caused the algorithm to return from a
search tree node.
18. The number of times the Subset Constraints caused the algorithm to return
from a search tree node.
19. The number of sets saved in the setTrie.
20. The number of times that a subset of an example set was present in the
setTrie and caused the algorithm to return from a search tree node.
21. The number of vertices in the setTrie.
22. The upper bound for the size of the smallest optimal decision tree
given as part of the input arguments.
23. The sum of all leaf depths in the optimal tree.
24. The minimum depth of any leaf in the optimal tree.
25. The maximum depth of any leaf in the optimal tree.
26. The maximum number of dimensions in which two examples differ.
27. The number of cuts in the dataset.
28. The maximum number of unique values in a dimension.
29. How many times the pairLB was calculated.
30. How many times the pairLB caused the algorithm to return from a
search tree node.
31. How many additional search tree nodes were checked due to the subset caching.
32. The amount of time in milliseconds used to calculate pairLBs that caused
the algorithm to return from a search tree node.
33. The amount of time in milliseconds used to calculate pairLBs that did not
cause the algorithm to return from a search tree node.
34. How many times the greedy heuristic caused the program to skip the
calculation of a pairLB.
35. The amount of time in milliseconds used to calculate greedy heuristic.
36. The calculated decision tree as a string. The format of this string is
explained in the section "Tree Output Format".

## Tree Output Format

The string consists of several segments s0 to s(n-1) where n is the number of vertices in this Decision Tree. 
The segments are separated by the character "|". 

Each segment si corresponds to vertex i in the tree and contains 5 values separated by the character ":".
The first (n - 1) / 2 segments are the inner vertices of the tree and the remaining segments are the leafs.

The first value is the parent p of vertex i. If i is the root this value will be -1.

The second value is either l or r and shows whether i is the left (l) or right (r) child of its parent.
If i is the root this value will be -1.

The third value is the class of vertex i; either true or false. If i is an inner vertex this value will be -1.

The fourth and fifth values are the dimension and threshold of the cut of i. If i is a leaf both values
will be -1.

# Other Solvers

We compared Witty with MurTree, dtfinder and dtfinder-DT1.
The result files of these algorithms are located in the folder `experiments/results/`.
The result files for dtfinder and dtfinder-DT1 are in CSV format and the columns have the following meaning:

1. A unique ID for the instance.
2. The relative path of the input instance inside the `data/` folder.
3. The sampling ratio.
4. The seed used for the random sampling.
5. The timeout in seconds.
6. The running time of the instance in milliseconds.
7. true if a timeout happened, false otherwise.

The result files for MurTree with a size constraint and MurTree with a depth constraint are also in CSV format
and the columns have the following meaning:

1. The relative path of the input instance inside the `data/` folder.
2. The optimal size or depth of the instance depending on which algorithm was used.
3. The running time of the instance in milliseconds. It is 3600000 if a timeout happened.
