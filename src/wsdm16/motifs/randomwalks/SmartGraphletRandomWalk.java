package wsdm16.motifs.randomwalks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
/** 
 * A random walk on the space of graphlets of a host graph.
 * This walk is similar to @FullGraphletRandomWalk. The possible transitions from each graphlet are the
 * same as in @FullGraphletRandomWalk, but the weights are different. In particular, this walk reduces the
 * probability of walking towards graphlets that have many "twins", such as towards stars centered in
 * high-degree nodes. This should speed up the walk in terms of actual graphlets transitions (i.e. if one
 * does not count transitions along self-loops, it should converge faster).
 * @author anon
 */
import java.util.Set;

import org.apache.commons.math3.distribution.GeometricDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.NodeIterator;
import it.unimi.dsi.fastutil.ints.Int2FloatArrayMap;
import wsdm16.graphutils.BreadthFirstSearch;
import wsdm16.motifs.BaseGraphIsomorphisms;
import wsdm16.motifs.Graphlet;
import wsdm16.motifs.LazyGraphIsomorphisms;
import wsdm16.motifs.distributions.EnumeratedIntegerDistribution;

public class SmartGraphletRandomWalk extends FullGraphletRandomWalk {
	private int realSteps;
	private Map<Integer, Set<Integer>> neighCache; // store neighbors of each graphlet node
	private Map<Integer, Set<Integer>> switchables; // store nodes that can be swapped with H's nodes
	private Map<Integer, Map<Integer, Float>> arcWeights; // store nodes that can be swapped with H's nodes
	private Map<Set<Integer>, Set<Integer>> neighUnionCache; // store union of nodes neighbors

	/** Constructor.
	 * 
	 * @param G host graph
	 * @param k size of the graphlet (number of nodes)
	 * @param u a node to start from to search for a graphlet
	 * @param maxDeg the maximum degree of G
	 * @throws wsdm16.motifs.randomwalks.FullGraphletRandomWalk.InvalidStartingNodeException 
	 */
	public SmartGraphletRandomWalk(ImmutableGraph G, int k, int u, int maxDeg, RandomGenerator random) throws FullGraphletRandomWalk.InvalidStartingNodeException
	{
		super(G, k, u, maxDeg, random);		
	}

	public SmartGraphletRandomWalk(ImmutableGraph G, int k)
	{
		super(G, k);
	}

	/**
	 * Updates the nodes that can be switched with the graphlet's nodes, and their weights.
	 */
	public void updateSwitchables() {
		List<Integer> nodes = new ArrayList<>(H.getNodes());
		switchables = new HashMap<>(nodes.size());
		arcWeights = new HashMap<Integer, Map<Integer, Float>>(H.size());
		for (int u : H.getNodes()) { // see what can be done with H\{u}
			Int2FloatArrayMap map = new Int2FloatArrayMap();
			map.defaultReturnValue(1f);
			Graphlet H1 = new Graphlet(H);
			H1.removeNode(u);
			Set<Integer> nodes1 = new HashSet<Integer>(H1.getNodes());
			for (int v : nodes1) {
				int c = 0;
				Set<Integer> keepConn = new IntArraySet(); // neighbors w of v such that (H-u+w) is a graphlet 
				for (int w : G.successorArray(v)) {
					if (!H.getNodes().contains(w)) {
    					H1.addNode(w);
    					if (H1.isConnected()) {
    						c += 1;
    						keepConn.add(w);
    					}
    					H1.removeNode(w);
					}
				}
				for (int w : keepConn)
					map.put(w, (float)Math.min(map.get(w), 1.0/c));
			}
			switchables.put(u, map.keySet());
			arcWeights.put(u, map);
		}	
	}

	/**
	 * Return the current state's degree (number of neighboring graphlets in the random walk).
	 * @return just the sum of the sizes of the sets in this.switchables
	 */
	public int stateDegree() {
		int totalSize = 0;
		for (Integer u : switchables.keySet())
			totalSize += switchables.get(u).size();
		return totalSize;
	}

	/**
	 * Return the current state's weight (sum of the transitions' weights, excluding self-loops.
	 * @return the sum of the edge weights.
	 */
	public float stateWeight() {
		float wgt = 0;
		for (Integer u : arcWeights.keySet())
			for (Map.Entry<Integer, Float> e : arcWeights.get(u).entrySet())
				wgt += e.getValue();
		return wgt;
	}

	/**
	 * Draw a pair of nodes to be switched, i.e. a neighboring graphlet, according to the weights.
	 * Draw a pair (u,v). from all the pairs of nodes in G where u is in H and v is in G\H and
	 * such that replacing u with v gives again a graphlet. The pair (u,v) is chosen with probability
	 * proportional to the weight assigned to (u,v) -- see {@link SmartGraphletRandomWalk#updateSwitchables()}.
	 * @param switchables a map from the nodes in H to the nodes that can replace them
	 * @param weights a two-level map; for each node u of H, a weight map from each node that can replace u to its weight
	 * @return a 2-element array with entries [u, v]
	 */
	private int[] drawSwitch(Map<Integer, Set<Integer>> switchables, Map<Integer, Map<Integer, Float>> weights) {
		int[] uv = new int[2];
		
		// first, select the node u to be removed from H
		int[] vNodes = new int[k];
		double[] vProbs = new double[k];
		int i = 0;
		float totW = 0f;
		for (Integer u : switchables.keySet()) {
			vNodes[i] = u;
			vProbs[i] = 0;
			for (Map.Entry<Integer, Float> e : arcWeights.get(u).entrySet()) {
				vProbs[i] += e.getValue();
//				System.out.print(e.getKey() + ":" + e.getValue() + "; ");
			}
			totW += vProbs[i];
			i++;
		}
		for (int j = 0; j < i; j++) {
			vProbs[j] /= totW;
			// System.out.print(vNodes[j] + ":" + vProbs[j] + "; ");
		}
		//System.out.println();
		uv[0] = new EnumeratedIntegerDistribution(vNodes, vProbs, false).sample(random);

		// then, pick the node v to replace u, chosen among u's switches
		Set<Integer> switches = switchables.get(uv[0]);
		Integer[] swI = switches.toArray(new Integer[switches.size()]);
		int[] sw = new int[swI.length];
		Float[] probsF = arcWeights.get(uv[0]).values().toArray(new Float[switches.size()]);
		double[] probs = new double[probsF.length];
		totW = 0;
		for (int j = 0; j < sw.length; j++) {
			sw[j] = swI[j];
			probs[j] = probsF[j];
			totW += probs[j];
		}
		for (int j = 0; j < sw.length; j++) // normalize the probs
			probs[j] /= totW;
		uv[1] = (new EnumeratedIntegerDistribution(sw, probs, false)).sample(random);
		return uv;
	}

	/**
	 * Makes many steps of the random walk, including self-loops around states.
	 * @param howMany how many steps to perform, including following self-loops
	 * @return the number of steps performed
	 */
	private int stepMany(int howMany)
	{
		// get the switchables
		updateSwitchables();
		// follow a number X~Geom(..) of self-loops
		int numFailures = Integer.MAX_VALUE;
		if (!switchables.isEmpty())
		{
			int ub = (k-1)*k*maxDegree; //For each vertex of the motif, we have at most (k-1) * maxDeg possible candidates
//			System.out.println("State weight = "+ stateWeight());
			GeometricDistribution geometric = new GeometricDistribution(random, ((double)stateWeight())/ub);
			numFailures = geometric.sample();
		}

		if (numFailures>=howMany)
		{
			steps+=howMany;
			return howMany;
		}

		steps += numFailures+1;
		realSteps += 1;

		// switch!
		int[] uv = drawSwitch(switchables, arcWeights);
//		System.out.println("replacing " + uv[0] + " -> " + uv[1]);
		H.removeNode(uv[0]);
		H.addNode(uv[1]);
//		System.out.println(H.getNodes());
//		System.out.println(H.getNodes().size());
//		updateNeighCache(uv[1]);
		assert(H.size()==k);
		assert(H.isConnected());
//		BaseGraphIsomorphisms isomorphisms = new LazyGraphIsomorphisms(k);
//		isomorphisms.long_signature(H.asGraph());
		return numFailures+1;
	}

	/** Perform multiple steps of the walk.
	 * 
	 */
	@Override
	public int walk(int steps)
	{
		int end = this.steps + steps;
		while (this.steps < end)
			stepMany(end-this.steps);

		return steps; 
	}

	public int realSteps()
	{
		return realSteps;
	}

}
