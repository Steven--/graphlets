/**
 * Performs a breadth-first visits of a graph. It keeps trace of the distances, stops at the desired distance,
 * and can perform multiple searches (starting from arbitrary nodes) with multiple threads.
 */
package wsdm16.graphutils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ASCIIGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.Transform;

/**
 * Implementation of breadth-first search, with or without distance mapping.
 * 
 * @author anon
 */
public class BreadthFirstSearch {
    private ImmutableGraph G;
    private int source;
    private List<Integer> sourceList;
    private int maxDistance;
    private int maxNodes;

    enum GraphFormat {
	BV, ASCII
    }

    /**
     * Terminating conditions for a visit
     */
    public enum VisitTerminatingCondition {
	FRONTIER_EMTPY, MAX_DISTANCE_REACHED, MAX_NODES_REACHED
    }

    /**
     * Returns the number of children of a node, not counting itself in case a self-loop exists.
     * 
     * @param G
     * @param node
     * @return
     */
    public static int numChildren(ImmutableGraph G, int node) {
	LazyIntIterator itr = G.successors(node);
	boolean hasLoop = false;
	for (int i = 0; i < G.outdegree(node); i++)
	    hasLoop |= itr.nextInt() == node;
	return G.outdegree(node) - (hasLoop == true ? 1 : 0);
    }

    /**
     * Checks if v has a self-loop
     * 
     * @param G
     * @param v
     * @return
     */
    public static boolean hasLoop(ImmutableGraph G, int v) {
	return numChildren(G, v) == G.outdegree(v);
    }

    /**
     * The result of a breadth-first search.
     * 
     * BreadthFirstSearch.java - created on 12/set/2014
     * 
     * @author anon
     */
    public static class Result implements Serializable {
	private static final long serialVersionUID = -6395514494857039846L;
	private int sourceNode = -1;
	private double elapsedSeconds = -1;
	private VisitTerminatingCondition terminatingCondition = null;
	private long processedArcs = 0;
	private int maxNodes = 0;
	private int reachedNodes = 0;
	private int maxDistance = 0;
	private int reachedDistance = 0;
	private Map<Integer, Integer> distanceMap = null;

	/**
	 * @return the sourceNode
	 */
	public int getSourceNode() {
	    return sourceNode;
	}

	/**
	 * @param sourceNode
	 *            the sourceNode to set
	 */
	public Result setSourceNode(int sourceNode) {
	    this.sourceNode = sourceNode;
	    return this;
	}

	/**
	 * @return the reachedNodes
	 */
	public int getReachedNodes() {
	    return reachedNodes;
	}

	/**
	 * @param reachedNodes
	 *            the reachedNodes to set
	 */
	public Result setReachedNodes(int reachedNodes) {
	    this.reachedNodes = reachedNodes;
	    return this;
	}

	public double getElapsedSeconds() {
	    return elapsedSeconds;
	}

	public Result setElapsedSeconds(double elapsedSeconds) {
	    this.elapsedSeconds = elapsedSeconds;
	    return this;
	}

	public VisitTerminatingCondition getTerminatingCondition() {
	    return terminatingCondition;
	}

	public Result setTerminatingCondition(VisitTerminatingCondition terminatingCondition) {
	    this.terminatingCondition = terminatingCondition;
	    return this;
	}

	public long getProcessedArcs() {
	    return processedArcs;
	}

	public Result setProcessedArcs(long processedArcs) {
	    this.processedArcs = processedArcs;
	    return this;
	}

	public int getMaxDistance() {
	    return maxDistance;
	}

	public Result setMaxDistance(int maxDistance) {
	    this.maxDistance = maxDistance;
	    return this;
	}

	public int getMaxNodes() {
	    return maxNodes;
	}

	public Result setMaxNodes(int maxNodes) {
	    this.maxNodes = maxNodes;
	    return this;
	}

	public int getReachedDistance() {
	    return reachedDistance;
	}

	public Result setReachedDistance(int reachedDistance) {
	    this.reachedDistance = reachedDistance;
	    return this;
	}

	public Map<Integer, Integer> getDistanceMap() {
	    return distanceMap;
	}

	public Result setDistanceMap(Map<Integer, Integer> distanceMap) {
	    this.distanceMap = distanceMap;
	    return this;
	}

	/**
	 * Validates the state of the object after it has been deserialized
	 */
	private void validateState() {
	}

	/**
	 * Reads the object from an input stream
	 * 
	 * @param inputStream
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private void readObject(ObjectInputStream inputStream) throws ClassNotFoundException, IOException {
	    inputStream.defaultReadObject();
	    validateState();
	}

	/**
	 * Writes the object to an output stream
	 * 
	 * @param outputStream
	 * @throws IOException
	 */
	private void writeObject(ObjectOutputStream outputStream) throws IOException {
	    outputStream.defaultWriteObject();
	}

    }

    public BreadthFirstSearch(ImmutableGraph G, int source) {
	this(G, source, Integer.MAX_VALUE);
    }

    public BreadthFirstSearch(ImmutableGraph G, int source, int maxDist) {
	this(G, null, maxDist);
	this.source = source;
    }

    public BreadthFirstSearch(ImmutableGraph G, List<Integer> sourceList) {
	this(G, sourceList, Integer.MAX_VALUE);
    }

    public BreadthFirstSearch(ImmutableGraph G, List<Integer> sourceList, int maxDist) {
	this.G = G;
	this.sourceList = sourceList;
	this.maxDistance = maxDist;
	this.maxNodes = Integer.MAX_VALUE;
    }

    /**
     * Counts the number of reachable nodes, using the parameters passed to the contructor. This is identical to an
     * execution of visit(), except that the distance of each node is not stored, thus no distance map is kept.
     * 
     * @return a {@link Result} object containing the result of the visit.
     */
    public Result count() {
	long startTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
	IntArrayFIFOQueue lastFrontier = new IntArrayFIFOQueue();
	lastFrontier.enqueue(source);
	BitSet visited = new BitSet();
	visited.set(source);
	int currentDistance = 0;
	long processedArcs = 0;
	while (currentDistance < maxDistance && visited.cardinality() < maxNodes && !lastFrontier.isEmpty()) {
	    IntArrayFIFOQueue nextFrontier = new IntArrayFIFOQueue();
	    while (visited.cardinality() < maxNodes && !lastFrontier.isEmpty()) {
		Integer u = lastFrontier.dequeueInt();
		LazyIntIterator neighs = G.successors(u);
		int deg = G.outdegree(u);
		processedArcs += deg;
		while (visited.cardinality() < maxNodes && deg-- != 0) {
		    Integer w = neighs.nextInt();
		    if (!visited.get(w)) { // w has never been seen before
			visited.set(w);
			nextFrontier.enqueue(w.intValue());
		    }
		}
	    }
	    if (visited.cardinality() < maxNodes && !nextFrontier.isEmpty()) // if there is still something to explore, we can increase the distance
		currentDistance++;
	    lastFrontier = nextFrontier;
	}
	long stopTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
	VisitTerminatingCondition cond;
	if (lastFrontier.isEmpty())
	    cond = VisitTerminatingCondition.FRONTIER_EMTPY;
	else if (visited.cardinality() == maxNodes)
	    cond = VisitTerminatingCondition.MAX_NODES_REACHED;
	else
	    cond = VisitTerminatingCondition.MAX_DISTANCE_REACHED;
	return new Result()
		.setElapsedSeconds((double) (stopTime - startTime) / 1E9)
		.setSourceNode(source)
		.setDistanceMap(null)
		.setMaxNodes(maxNodes)
		.setReachedNodes(visited.cardinality())
		.setMaxDistance(maxDistance)
		.setReachedDistance(currentDistance)
		.setProcessedArcs(processedArcs)
		.setTerminatingCondition(cond);
    }

    /**
     * Performs the actual visit, using the parameters passed to the constructor.
     * 
     * @return a {@link Result} object containing the result of the visit.
     */
    public Result visit() {
	long startTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
	Map<Integer, Integer> distanceMap = new Int2IntOpenHashMap();
	distanceMap.put(source, 0);
	IntArrayFIFOQueue lastFrontier = new IntArrayFIFOQueue(); // nodes visited in the last iteration
	lastFrontier.enqueue(source);
	BitSet visited = new BitSet();
	visited.set(source);
	int currentDistance = 0;
	long processedArcs = 0;
	while (currentDistance < maxDistance && visited.cardinality() < maxNodes && !lastFrontier.isEmpty()) {
	    IntArrayFIFOQueue nextFrontier = new IntArrayFIFOQueue();
	    while (visited.cardinality() < maxNodes && !lastFrontier.isEmpty()) {
		Integer u = lastFrontier.dequeueInt();
		LazyIntIterator neighs = G.successors(u);
		int deg = G.outdegree(u);
		processedArcs += deg;
		while (visited.cardinality() < maxNodes && deg-- != 0) {
		    Integer w = neighs.nextInt();
		    if (!visited.get(w)) { // w has never been seen before
			visited.set(w);
			distanceMap.put(w, currentDistance + 1);
			nextFrontier.enqueue(w.intValue());
		    }
		}
	    }
	    if (visited.cardinality() < maxNodes && !nextFrontier.isEmpty()) // if there is still something to explore, we can increase the distance
		currentDistance++;
	    lastFrontier = nextFrontier;
	}
	long stopTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
	VisitTerminatingCondition cond;
	if (lastFrontier.isEmpty())
	    cond = VisitTerminatingCondition.FRONTIER_EMTPY;
	else if (visited.cardinality() == maxNodes)
	    cond = VisitTerminatingCondition.MAX_NODES_REACHED;
	else
	    cond = VisitTerminatingCondition.MAX_DISTANCE_REACHED;
	return new Result()
		.setElapsedSeconds((double) (stopTime - startTime) / 1E9)
		.setSourceNode(source)
		.setDistanceMap(distanceMap)
		.setReachedNodes(visited.cardinality())
		.setMaxDistance(maxDistance)
		.setReachedDistance(currentDistance)
		.setProcessedArcs(processedArcs)
		.setTerminatingCondition(cond);
    }

    /**
     * Performs multiple breadth-first searches in parallel.
     * 
     * @param numThreads
     * @param oneFilePerNode
     * @param outFile
     * @param maxQueuedResults
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public List<Pair<Integer, BreadthFirstSearch.Result>> multiThreadedVisit(int numThreads, boolean oneFilePerNode,
	    String outFile, int maxQueuedResults) throws IOException, InterruptedException {
	final ConcurrentLinkedQueue<Integer> nodeQueue = new ConcurrentLinkedQueue<>();
	final LinkedBlockingQueue<Thread> doneQueue = new LinkedBlockingQueue<>();
	List<Pair<Integer, BreadthFirstSearch.Result>> outputList = new ArrayList<Pair<Integer, BreadthFirstSearch.Result>>();
	final LinkedBlockingQueue<Pair<Integer, BreadthFirstSearch.Result>> resultQueue = new LinkedBlockingQueue<>(
		outFile == null ? sourceList.size() : maxQueuedResults);
	ProgressLogger mainLogger = new ProgressLogger(LoggerFactory.getLogger("SOLVER"));

	/**
	 * The control loop in each thread. This object polls the input node list for new source nodes, invokes the
	 * visit, and puts the result back into a queue.
	 */
	class ParallelBFSWorker implements Runnable {
	    private final ImmutableGraph Gview;

	    public ParallelBFSWorker() {
		super();
		this.Gview = G.copy();
	    }

	    public void run() {
		Integer v;
		while ((v = nodeQueue.poll()) != null) {
		    BreadthFirstSearch.Result out = new BreadthFirstSearch(Gview, v, maxDistance).visit();
		    try {
			resultQueue.put(new ImmutablePair<Integer, BreadthFirstSearch.Result>(v, out));
		    } catch (InterruptedException e) {
			e.printStackTrace();
		    }
		}
		try {
		    doneQueue.put(Thread.currentThread());
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
	    }
	}
	mainLogger.start();
	Iterator<Integer> nodeItr = sourceList.iterator();
	while (nodeItr.hasNext())
	    nodeQueue.add(nodeItr.next());
	Thread[] runners = new Thread[numThreads];
	/* Create, start, and join threads */
	for (int i = 0; i < numThreads; i++) {
	    runners[i] = new Thread(new ParallelBFSWorker());
	    // mainLogger.logger().info("Thread " + i + " started");
	}
	for (int i = 0; i < numThreads; i++)
	    runners[i].start();
	if (outFile != null) { // writes out a pair as soon as it arrives
	    if (oneFilePerNode) {
		for (int i = 0; i < sourceList.size(); i++) {
		    Pair<Integer, BreadthFirstSearch.Result> p = resultQueue.take();
		    FileOutputStream fos = new FileOutputStream(outFile + "-node=" + p.getLeft());
		    ObjectOutputStream oos = new ObjectOutputStream(fos);
		    oos.writeObject(p);
		    oos.close();
		    fos.close();
		}
	    } else {
		FileOutputStream fos = new FileOutputStream(outFile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		for (int i = 0; i < sourceList.size(); i++) {
		    Pair<Integer, BreadthFirstSearch.Result> p = resultQueue.take();
		    oos.writeObject(p);
		}
		oos.close();
		fos.close();
	    }
	}
	for (int i = 0; i < numThreads; i++) { // wait for threads to finish
	    try {
		Thread joining = doneQueue.take();
		joining.join();
		// mainLogger.logger().info("Thread joined");
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}
	if (outFile == null) { // return the actual result
	    outputList = new LinkedList<>();
	    resultQueue.drainTo(outputList);
	}
	// mainLogger.done();
	return outputList;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
	String graphFile = null;
	int maxDistance = 0;
	int verboseLevel = 0;
	boolean transpose = false;
	int numThreads = 1, maxQueuedResults = numThreads;
	String outputFile = null;
	GraphFormat format = null;
	List<Integer> sources = new ArrayList<Integer>();
	boolean oneFilePerNode = false;
	Options options = new Options();
	options.addOption("F", "input-file-webgraph", true, "basename of the file, stored in Webgraph's BVGraph format");
	options.addOption("A", "input-file-ascii", true, "basename of the graph, stored in Webgraph's ASCII format");
	options.addOption("d", "max-distance", true, "max depth of the BFS visit");
	options.addOption("q", "max-queued-results", true, "max queued results for the multithreaded visit.");
	options.addOption("o", "output-file", true, "store output into this file");
	options.addOption("t", "threads", true, "number of concurrent threads (default = 1)");
	options.addOption("v", "verbose", true, "verbose level; 0 = no output (default), 1 = basic output");
	options.addOption("r", "reverse", false, "reverse (= transpose) the input graph");
	options.addOption("i", "individual", false, "save output on a file per each node");
	CommandLineParser parser = new PosixParser();
	try {
	    CommandLine cmd = parser.parse(options, args);
	    verboseLevel = cmd.hasOption("v") ? Integer.parseInt(cmd.getOptionValue("v")) : verboseLevel;
	    format = cmd.hasOption("A") ? GraphFormat.ASCII : GraphFormat.BV;
	    graphFile = cmd.hasOption("F") ? cmd.getOptionValue("F") : graphFile;
	    graphFile = cmd.hasOption("A") ? cmd.getOptionValue("A") : graphFile;
	    maxDistance = cmd.hasOption("d") ? Integer.parseInt(cmd.getOptionValue("d")) : Integer.MAX_VALUE;
	    outputFile = cmd.hasOption("o") ? cmd.getOptionValue("o") : outputFile;
	    numThreads = cmd.hasOption("t") ? Integer.parseInt(cmd.getOptionValue("t")) : numThreads;
	    maxQueuedResults = cmd.hasOption("q") ? Integer.parseInt(cmd.getOptionValue("q")) : numThreads;
	    transpose = cmd.hasOption("r");
	    oneFilePerNode = cmd.hasOption("i");
	} catch (ParseException e) {
	    System.err.println(e.toString());
	    System.exit(1);
	}
	if (graphFile == null) {
	    HelpFormatter formatter = new HelpFormatter();
	    formatter.setWidth(120);
	    formatter.printHelp(BreadthFirstSearch.class.getName()
		    + " (-F basename|-A basename) [options] ... < nodelist", options);
	    System.exit(0);
	}
	ImmutableGraph G = null;
	ProgressLogger pl = new ProgressLogger();
	if (format == GraphFormat.BV) {
	    G = ImmutableGraph.load(graphFile, pl);
	} else if (format == GraphFormat.ASCII) {
	    G = ASCIIGraph.load(graphFile, pl);
	}
	if (verboseLevel > 0)
	    pl.updateAndDisplay();
	if (verboseLevel > 0)
	    System.out.println("Graph loaded.");
	if (transpose) {
	    if (verboseLevel > 0)
		System.out.println("Transposing graph ...");
	    G = Transform.transpose(G);
	    if (verboseLevel > 0)
		System.out.println("Graph transposed.");
	}
	// read nodes from stdin
	Scanner stdin = new Scanner(System.in);
	while (stdin.hasNext()) {
	    Integer v = Integer.parseInt(stdin.next());
	    sources.add(v);
	}
	stdin.close();
	// do a parallel visit
	numThreads = Math.min(sources.size(), numThreads);
	long start = System.nanoTime();
	List<Pair<Integer, Result>> result = new BreadthFirstSearch(G, sources, maxDistance).multiThreadedVisit(
		numThreads, oneFilePerNode, outputFile, maxQueuedResults);
	long elapsed = System.nanoTime() - start;
	if (verboseLevel > 0) {
	    System.out.println("Elapsed in MAIN: " + (double) elapsed / 1E9);
	}
	if (verboseLevel > 1) {
	    if (result != null) {
		Iterator<Pair<Integer, Result>> itr = result.iterator();
		while (itr.hasNext()) {
		    Pair<Integer, Result> r = itr.next();
		    System.out.println(r.getLeft() + "  " + r.getRight().getDistanceMap().size());
		}
	    }
	}
    }

    public int getMaxNodes() {
	return this.maxNodes;
    }

    public BreadthFirstSearch setMaxNodes(int maxNodes) {
	this.maxNodes = maxNodes;
	return this;
    }

}
