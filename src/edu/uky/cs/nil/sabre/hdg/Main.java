package edu.uky.cs.nil.sabre.hdg;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.time.ZonedDateTime;

import edu.uky.cs.nil.sabre.Session;
import edu.uky.cs.nil.sabre.State;
import edu.uky.cs.nil.sabre.comp.CompiledAction;
import edu.uky.cs.nil.sabre.prog.GraphHeuristic;
import edu.uky.cs.nil.sabre.prog.ProgressionCostFactory;
import edu.uky.cs.nil.sabre.prog.ProgressionPlanner.Method;
import edu.uky.cs.nil.sabre.prog.ProgressionSearch;
import edu.uky.cs.nil.sabre.prog.RelaxedPlanHeuristic;
import edu.uky.cs.nil.sabre.prog.RepeatedNodeHeuristic;
import edu.uky.cs.nil.sabre.ptree.ProgressionTree;
import edu.uky.cs.nil.sabre.ptree.ProgressionTreeSpace;
import edu.uky.cs.nil.sabre.search.Result;
import edu.uky.cs.nil.sabre.util.CommandLineArguments;
import edu.uky.cs.nil.sabre.util.Worker;
import edu.uky.cs.nil.sabre.util.Worker.Status;

/**
 * Runs a complete {@link ProgressionSearch Sabre progression heuristic search}
 * based on the provided command line arguments and then writes relevant
 * information for all explained nodes in the search's {@link ProgressionTree
 * tree} to file along with {@link
 * edu.uky.cs.nil.sabre.prog.ProgressionCost heuristic estimates} for several
 * of Sabre's heuristics.
 * 
 * @author Stephen G. Ware
 */
public class Main {
	
	/** The command line key for the usage message */
	public static final String HELP_KEY = edu.uky.cs.nil.sabre.Main.HELP_KEY;
	
	/** The command line key for the problem file URL */
	public static final String PROBLEM_KEY = edu.uky.cs.nil.sabre.Main.PROBLEM_KEY;
	
	/** The command line key for the author temporal limit setting */
	public static final String AUTHOR_TEMPORAL_LIMIT_KEY = edu.uky.cs.nil.sabre.Main.AUTHOR_TEMPORAL_LIMIT_KEY;
	
	/** The command line key for the character temporal limit setting */
	public static final String CHARACTER_TEMPORAL_LIMIT_KEY = edu.uky.cs.nil.sabre.Main.CHARACTER_TEMPORAL_LIMIT_KEY;
	
	/** The command line key for the epistemic limit setting */
	public static final String EPISTEMIC_LIMIT_KEY = edu.uky.cs.nil.sabre.Main.EPISTEMIC_LIMIT_KEY;
	
	/** The command line key for the output file URL */
	public static final String OUTPUT_KEY = "-out";
	
	private static final String pad(String string) {
		return String.format("%-13s", string);
	}
	
	/** A string explaning the command line arguments that can be passed to this program */
	public static final String USAGE =
		Settings.CREDITS + "\n" +
		pad(HELP_KEY) + 								"print this message and halt\n" +
		pad(PROBLEM_KEY + " PATH") +					"problem file to parse\n" +
		pad(AUTHOR_TEMPORAL_LIMIT_KEY + " NUMBER") + 	"max actions in a plan\n" +
		pad(CHARACTER_TEMPORAL_LIMIT_KEY + " NUMBER") + "max actions in a character's explanation for an action\n" +
		pad(EPISTEMIC_LIMIT_KEY + " NUMBER") + 			"max depth to explore theory of mind\n" +
		pad(OUTPUT_KEY + " PATH") + 					"output file in CSV format (defaluts to problem name)";
	
	private static final String[] HEADERS = new String[] {"epistemic", "plan", "state", "utility", "solution", "distance", "hmax", "hadd", "hrp", "relaxed"};
	private static GraphHeuristic hmax = null;
	private static GraphHeuristic hadd = null;
	private static RelaxedPlanHeuristic hrp = null;
	
	/**
	 * Runs the Sabre Heuristic Data Generator tool based on the giving {@link
	 * #USAGE line arguments}.
	 * 
	 * @param args the command line arguments
	 * @throws Exception if an exception occurs while the tool is running
	 */
	public static void main(String[] args) throws Exception {
		CommandLineArguments arguments = new CommandLineArguments(args);
		if(args.length == 0 || arguments.contains(HELP_KEY))
			System.out.println(USAGE);
		else {
			ZonedDateTime start = ZonedDateTime.now();
			Session session = new Session();
			Result<CompiledAction> result = Worker.get(status -> main(arguments, session, status));
			System.out.println("Problem:      " + result.problem.name);
			System.out.println("  Characters: " + result.problem.universe.characters.size());
			System.out.println("  Fluents:    " + result.problem.fluents.size());
			System.out.println("  Actions:    " + result.problem.actions.size());
			System.out.println("  Triggers:   " + result.problem.triggers.size());
			System.out.println("Search:");
			System.out.println("  Sabre Version:            " + edu.uky.cs.nil.sabre.Settings.VERSION_STRING);
			System.out.println("  State Date / Time:        " + start.toEpochSecond() + " (" + start + ")");
			System.out.println("  Method:                   " + session.getMethod());
			System.out.println("  Author Temporal Limit:    " + session.getAuthorTemporalLimit());
			System.out.println("  Character Temporal Limit: " + session.getCharacterTemporalLimit());
			System.out.println("  Epistemic Limit:          " + session.getEpistemicLimit());
			System.out.println("  Visited:                  " + result.visited + " nodes");
			System.out.println("  Generated:                " + result.generated + " nodes");
			System.out.println("  Time:                     " + result.time + "ms (" + edu.uky.cs.nil.sabre.Utilities.time(result.time) + ")");
			
		}
	}
	
	private static final Result<CompiledAction> main(CommandLineArguments arguments, Session session, Status status) throws Exception {
		// Configure and run a full explanation-first search.
		session.setStatus(status);
		session.setProblem(new File(arguments.require(PROBLEM_KEY)));
		session.setAuthorTemporalLimit(Integer.parseInt(arguments.require(AUTHOR_TEMPORAL_LIMIT_KEY)));
		session.setCharacterTemporalLimit(Integer.parseInt(arguments.require(CHARACTER_TEMPORAL_LIMIT_KEY)));
		session.setEpistemicLimit(Integer.parseInt(arguments.require(EPISTEMIC_LIMIT_KEY)));
		session.setMethod(Method.EXPLANATION_FIRST);
		session.setCost(ProgressionCostFactory.TEMPORAL);
		session.setHeuristic(new RepeatedNodeHeuristic.Factory(ProgressionCostFactory.ZERO));
		session.setGoal(Double.MAX_VALUE);
		ProgressionSearch search = (ProgressionSearch) session.getSearch();
		// Create the heuristics.
		hmax = (GraphHeuristic) GraphHeuristic.MAX.getCost(search.problem, status);
		hadd = (GraphHeuristic) GraphHeuristic.SUM.getCost(search.problem, status);
		hrp = (RelaxedPlanHeuristic) RelaxedPlanHeuristic.FACTORY.getCost(search.problem, status);
		// Do the search.
		Result<CompiledAction> result = search.get(status);
		// Use Java reflection to access the search's progression tree.
		Field spaceField = ProgressionSearch.class.getDeclaredField("space");
		spaceField.setAccessible(true);
		ProgressionTreeSpace space = (ProgressionTreeSpace) spaceField.get(search);
		Field treeField = ProgressionTreeSpace.class.getDeclaredField("tree");
		treeField.setAccessible(true);
		ProgressionTreeMap map = new ProgressionTreeMap((ProgressionTree) treeField.get(space), status);
		// Write the nodes out to file.
		File output = new File(search.problem.name + ".csv");
		if(arguments.contains(OUTPUT_KEY))
			output = new File(arguments.get(OUTPUT_KEY));
		int count = 0;
		status.setMessage("Writing data to \"" + output + "\": %d nodes written", count);
		try(BufferedWriter out = new BufferedWriter(new FileWriter(output))) {
			for(int i=0; i<HEADERS.length; i++) {
				if(i > 0)
					out.append(",");
				out.append("\"" + HEADERS[i] + "\"");
			}
			out.append("\n");
			for(long node : map.getNodes()) {
				if(map.getDistance(node) < Integer.MAX_VALUE) {
					write(map, node, out);
					status.update(0, ++count);
				}
			}
		}
		status.setMessage(count + " nodes written to \"" + output + "\"");
		return result;
	}
	
	private static final void write(ProgressionTreeMap map, long node, Writer out) throws IOException {
		write(map.getEpistemicPath(node), out);
		out.append(",");
		write(map.getPlan(node), out);
		out.append(",");
		write(map.getState(node), out);
		out.append(",");
		write(map.getUtility(node), out);
		out.append(",");
		write(map.isSolution(node), out);
		out.append(",");
		write(map.getDistance(node), out);
		out.append(",");
		State state = fluent -> map.tree.getValue(node, fluent);
		write(hmax.evaluate(state, map.getCharacter(node)), out);
		out.append(",");
		write(hadd.evaluate(state, map.getCharacter(node)), out);
		out.append(",");
		write(hrp.evaluate(state, map.getCharacter(node)), out);
		out.append(",");
		write(hrp.getRelaxedPlan(), out);
		out.append("\n");
	}
	
	@SuppressWarnings("unchecked")
	private static final void write(Object object, Writer out) throws IOException {
		if(object == null || object.equals(Double.POSITIVE_INFINITY))
			return;
		else if(object instanceof Boolean || object instanceof Number)
			out.append(object.toString());
		else if(object instanceof Iterable) {
			out.append("\"");
			Iterable<Object> iterable = (Iterable<Object>) object;
			boolean first = true;
			for(Object element : iterable) {
				if(first)
					first = false;
				else
					out.append("; ");
				out.append(element.toString());
			}
			out.append("\"");
		}
		else
			out.append("\"" + object + "\"");
	}
}