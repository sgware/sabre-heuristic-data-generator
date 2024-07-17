package edu.uky.cs.nil.sabre.hdg;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import edu.uky.cs.nil.sabre.Action;
import edu.uky.cs.nil.sabre.Character;
import edu.uky.cs.nil.sabre.Event;
import edu.uky.cs.nil.sabre.Fluent;
import edu.uky.cs.nil.sabre.HeadPlan;
import edu.uky.cs.nil.sabre.Plan;
import edu.uky.cs.nil.sabre.comp.CompiledAction;
import edu.uky.cs.nil.sabre.logic.Assignment;
import edu.uky.cs.nil.sabre.logic.Comparison;
import edu.uky.cs.nil.sabre.logic.False;
import edu.uky.cs.nil.sabre.logic.Value;
import edu.uky.cs.nil.sabre.ptree.ProgressionTree;
import edu.uky.cs.nil.sabre.util.ImmutableList;
import edu.uky.cs.nil.sabre.util.Worker.Status;

/**
 * A wrapper around a {@link ProgressionTree progression tree} which discovers
 * all {@link ProgressionTree#isExplained(long) explained nodes} in the tree
 * and provides other methods for collecting data about those nodes.
 * 
 * @author Stephen G. Ware
 */
public class ProgressionTreeMap {

	/**
	 * The progression tree that has already been expanded by a {@link
	 * edu.uky.cs.nil.sabre.prog.ProgressionSearch progression search}
	 */
	public final ProgressionTree tree;
	
	/**
	 * The key of this map are the explained nodes found in the tree and the
	 * values are that node's minimum distance to a solution
	 */
	private final TreeMap<Long, Integer> distance = new TreeMap<>();
	
	/**
	 * Constructs a new progression tree wrapper and explores the tree to find
	 * all explained nodes.
	 * 
	 * @param tree a progression tree which has already been expanded by a
	 * {@link edu.uky.cs.nil.sabre.prog.ProgressionSearch progression search}
	 * @param status a status object to update while exploring the tree
	 */
	public ProgressionTreeMap(ProgressionTree tree, Status status) {
		this.tree = tree;
		status.setMessage("Finding explained nodes: %d nodes found", 0);
		findExplained(0, status);
		int count = 0;
		status.setMessage("Finding distance to solution: %d nodes processed", count);
		for(long node : distance.keySet()) {
			if(isSolution(node)) {
				if(tree.getEvent(node) instanceof Action)
					setDistance(tree.getBefore(node), 1);
				else
					setDistance(tree.getBefore(node), 0);
			}
			status.update(0, ++count);
		}
		count = 0;
		status.setMessage("Labeling solutions: %d nodes processed", count);
		for(long node : distance.keySet()) {
			if(isSolution(node) && distance.get(node) == Integer.MAX_VALUE)
				distance.put(node, 0);
			status.update(0, ++count);
		}
	}
	
	private final void findExplained(long node, Status status) {
		if(distance.put(node, Integer.MAX_VALUE) == null) {
			status.update(0, distance.size());
			long child = tree.getLastChild(node);
			while(child != -1) {
				if(isExplained(child)) {
					findExplained(tree.getAfterTriggers(child), status);
					CompiledAction action = tree.getAction(child);
					for(Character consenting : action.consenting)
						findExplained(tree.getBranch(child, consenting), status);
				}
				child = tree.getPreviousSibling(child);
			}
		}
	}
	
	private final boolean isExplained(long node) {
		return tree.isExplained(node) && tree.getExplanation(node, tree.getCharacter(node)) != -1;
	}
	
	private final void setDistance(long node, int distance) {
		if(this.distance.containsKey(node))
			this.distance.put(node, Math.min(this.distance.get(node), distance));
		if(tree.getBefore(node) != node)
			setDistance(tree.getBefore(node), distance + (tree.getEvent(node) instanceof Action ? 1 : 0));
	}
	
	/**
	 * Returns the number of explained nodes found in the {@link #tree
	 * progression tree}.
	 * 
	 * @return the number of explained nodes in the tree
	 */
	public int size() {
		return distance.size();
	}
	
	/**
	 * Returns a collection of all explained node IDs in the {@link #tree
	 * progression tree}.
	 * 
	 * @return a collection of all explained node IDs
	 */
	public Iterable<Long> getNodes() {
		return distance.keySet();
	}
	
	/**
	 * Returns the minimum number of {@link Action actions} explained actions
	 * that can be taken from this node to reach a solution in the {@link #tree
	 * progression tree}. A node which is itself a solution may have a value
	 * greater than 0 if it is possible to reach other, better solutions from
	 * that node.
	 * 
	 * @param node the ID of an {@link #getNodes() explained node}
	 * @return the distance between the given node and its nearest solution
	 */
	public Integer getDistance(long node) {
		return distance.get(node);
	}
	
	/**
	 * Checks whether {@link ProgressionTree#getPlan(long) the plan} leading to
	 * a given node is a solution, meaning that it improved the {@link
	 * ProgressionTree#getUtility(long) utility} of {@link
	 * ProgressionTree#getCharacter(long) a node's character} via a sequence of
	 * {@link ProgressionTree#isExplained(long) explained actions}.
	 * 
	 * @param node the node to check
	 * @return true if the node is one of the {@link #getNodes() explained
	 * nodes} whose plan is a solution
	 */
	public boolean isSolution(long node) {
		return distance.containsKey(node) && isSolution(node, false, node);
	}
	
	private final boolean isSolution(long before, boolean action, long after) {
		if(distance.containsKey(before) && action)
			return Comparison.LESS_THAN.test(tree.getUtility(before), tree.getUtility(after));
		else if(tree.getBefore(before) == before)
			return false;
		else {
			Event event = tree.getEvent(before);
			return isSolution(tree.getBefore(before), action || (event instanceof Action && !event.getPrecondition().equals(False.FALSE)), after);
		}
	}
	
	/**
	 * Returns the {@link ProgressionTree#getCharacter(long) character}
	 * associated with the given node.
	 * 
	 * @param node a node in the {@link #tree progression tree}
	 * @return the character associated with that node or null if the node is
	 * associated with the author
	 */
	public Character getCharacter(long node) {
		return tree.getCharacter(node);
	}
	
	/**
	 * Returns the sequence of {@link Character characters} corresponding to
	 * a given node's theory of mind. If the list [X, Y] is returned, it means
	 * the given node represents what X believes Y believes.
	 * 
	 * @param node a node from the {@link #tree progression tree}
	 * @return a sequence of character representing the node's theory of mind
	 * or an empty sequence of the node is associated with the author (i.e. the
	 * actual state)
	 */
	public ImmutableList<Character> getEpistemicPath(long node) {
		ImmutableList<Character> path = ImmutableList.EMPTY.cast(Character.class);
		while(tree.getCharacter(node) != null) {
			path = path.add(tree.getCharacter(node));
			while(tree.getLastTrunk(node) == -1)
				node = tree.getBefore(node);
			node = tree.getLastTrunk(node);
		}
		return path;
	}
	
	/**
	 * Returns the sequence of {@link Action actions} that led to the state
	 * represented by a given node. The plan will always start at the initial
	 * state, which means it may contain {@link
	 * edu.uky.cs.nil.sabre.graph.DummyAction dummy actions} that updated the
	 * beliefs of characters.
	 * 
	 * @param node a node from the {@link #tree progression tree}
	 * @return the sequence of actions that led from the initial state to the
	 * state represented by the node
	 */
	public Plan<Action> getPlan(long node) {
		HeadPlan<Action> plan = HeadPlan.EMPTY;
		while(tree.getBefore(node) != node) {
			Event event = tree.getEvent(node);
			if(event instanceof Action)
				plan = plan.prepend((Action) event);
			node = tree.getBefore(node);
		}
		return plan;
	}
	
	/**
	 * Returns the {@link ProgressionTree#getUtility(long) utility} of the
	 * {@link ProgressionTree#getCharacter(long) node's character} in the state
	 * represented by the given node.
	 * 
	 * @param node a node from the {@link #tree progression tree}
	 * @return the utility of the character, or {@link Double#NaN NaN} if the
	 * utility is {@link edu.uky.cs.nil.sabre.logic.Unknown#UNKNOWN unknown}
	 */
	public double getUtility(long node) {
		Value utility = tree.getUtility(node);
		if(utility instanceof edu.uky.cs.nil.sabre.Number)
			return ((edu.uky.cs.nil.sabre.Number) utility).value;
		else
			return Double.NaN;
	}
	
	/**
	 * Returns an {@link Assignment assignment} of a {@link
	 * edu.uky.cs.nil.sabre.logic.Value value} to every {@link Fluent fluent}
	 * defined in the {@link #tree progression tree's} {@link
	 * ProgressionTree#problem problem} based on the state represented by a
	 * given node.
	 * 
	 * @param node a node from the {@link #tree progression tree} 
	 * @return a list of assignments where each fluent has the value it has in
	 * the state represented by the given node
	 */
	public List<Assignment> getState(long node) {
		List<Assignment> state = new ArrayList<>(tree.problem.fluents.size());
		for(Fluent fluent : tree.problem.fluents)
			state.add(new Assignment(fluent, tree.getValue(node, fluent)));
		return state;
	}
}