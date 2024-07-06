# Sabre Heuristic Data Generator

This tool uses the [Sabre Narrative Planner](https://github.com/sgware/sabre) to
perform an exhaustive
[explanation-first search](https://htmlpreview.github.io/?https://github.com/sgware/sabre/blob/main/doc/edu/uky/cs/nil/sabre/prog/ExplanationFirstSearch.html)
of a [benchmark problem](https://github.com/sgware/sabre-benchmarks) and then
print out a CSV file with information about all the search nodes it discovered
that were solutions or the ancestors of solutions. The data can be used to
evaluate Sabre's search heuristics and develop new heuristic techniques.

## Usage

To clone this project (including Sabre and a collection of Sabre benchmark
problems as submodules) and run it on the `macguffin` problem:

```
git clone --recurse-submodules https://github.com/sgware/sabre-heuristic-data-generator
cd sabre-heuristic-data-generator
java -Xms10g -Xmx10g -jar lib/hdg.jar -p sabre-benchmarks/problems/macguffin.txt -atl 2 -ctl 2 -el 1 -out output/macguffin.csv
```

The `-p` argument specifies the Sabre problem. `-atl` and `-ctl` at the author
and character temporal limits that define how long a solution plan can be and
how long an individual character's plan can be. `-el` is the epistemic limit
that controls how deeply the search will reason about each character's theory of
mind. `-out` sets the file where the output will be written.

It is important to set the author and character temporal limits and the
epistemic limit to something that can be exhaustively searched. The
[benchmark problem report](https://github.com/sgware/sabre-benchmarks/blob/main/report.pdf)
has recommended settings for each problem.

The `-Xms10g` and `-Xmx10g` arguments set the minimum and maximum Java virtual
machine memory to 10 gigabytes, which is overkill for such a small problem, but
when using this tool on larger problems Java will need lot of memory.

## Output

After completing an exhaustive search, this tool will explore the
[progression tree](https://htmlpreview.github.io/?https://github.com/sgware/sabre/blob/v0.8/doc/edu/uky/cs/nil/sabre/ptree/ProgressionTree.html)
that was created to find all nodes that are either solutions or ancestors of
solutions (i.e. nodes which can eventually lead to solutions). Data for these
nodes, along with estimates for three of Sabre's search heuristics, is written
out to file in Comma Separated Value format.

The meaning of each column in the CSV file is:
- *epistemic*: The epistemic path to the node with characters separated by
semicolons. An empty value means the node is an author node. A value like
`Tom; Merchant` means the node represents Tom's beliefs about the Merchant's
beliefs.
- *plan*: The sequence of actions that led to the node's state. This sequence
always starts at the initial state. It may omit actions that have occurred if
the node's character did not observe them. It may include dummy belief update
actions if a node's state was directly updated as a result of an action effect
or trigger.
- *state*: The node's current state, given as an assignment of a value to every
fluent. Note that some fluents may represent beliefs as a result of how Sabre
compiles problems for a tree-based search.
- *utility*: The utility of the current node's character (or the author's
utility if the node is an author node). A node's character is the last on the
epistemic path. So if the epistemic path is `Tom; Merchant`, then the node's
character is the Merchant.
- *solution*: Whether or not the node is a solution, meaning its most recent
action improved the utility of the node's character.
- *distance*: The distance to a node's nearest solution descendant. If a node's
plan is 1 action away from being a solution, this value will be 1. Note that
solutions may have a value greater than 0 because sometimes it is possible to
keep improving a character's utility. This value is the ground truth against
which the heuristic estimates can be compared. A perfect heuristic would always
estimate this number accurately.
- *hmax*: The heuristic estimate generated by the
[max graph heuristic](https://htmlpreview.github.io/?https://github.com/sgware/sabre/blob/main/doc/edu/uky/cs/nil/sabre/prog/GraphHeuristic.MaxGraphHeuristic.html)
for this node. This is similar to Bonet and Geffner's max heuristic, which
defines the costs of a conjunction as the maximum cost of any of its conjuncts.
This value will be blank if the heuristic returned positive infinity.
- *hadd*: The heuristic estimate generated by the
[sum graph heuristic](https://htmlpreview.github.io/?https://github.com/sgware/sabre/blob/main/doc/edu/uky/cs/nil/sabre/prog/GraphHeuristic.SumGraphHeuristic.html)
for this node. This is similar to Bonet and Geffner's additive heuristic, which
defines the costs of a conjunction as the sum of the costs of its conjuncts.
This value will be blank if the heuristic returned positive infinity.
- *hrp*: The heuristic estimate generated by the
[relaxed plan heuristic](https://htmlpreview.github.io/?https://github.com/sgware/sabre/blob/main/doc/edu/uky/cs/nil/sabre/prog/RelaxedPlanHeuristic.html)
for this node. This is based on Hoffmann's Fast Forward heuristic, which uses a
plan graph to find a plan for an easier version of the planning problem and uses
the length of that plan as its estimate. This value will be blank if the
heuristic failed to find a relaxed plan.
- *relaxed*: The relaxed plan found by the relaxed plan heuristic, if one was
found.

## Ownership and License

This tool and the Sabre Narrative Planner was developed by Stephen G. Ware PhD,
Associate Professor of Computer Science at the University of Kentucky. Sabre is
released under the Creative Commons Attribution-NonCommercial 4.0 International
license, which allows you to share, remix, and add to the software for
non-commercial projects as long as you give credit to the original creators.
This tool is not released under any particular license.

## Version History

- Version 1.0: First public release, using a pre-release version of Sabre 0.8.

## Citation

You can cite this tool and the data generated by it with the following BibTeX
entry:

```
@misc{ware2024sabreheuristicdata,
  author={Ware, Stephen G.},
  title={Sabre Heuristic Data Generator},
  year={2024},
  howpublished={\url{https://github.com/sgware/sabre-heuristic-data-generator}}
}
```