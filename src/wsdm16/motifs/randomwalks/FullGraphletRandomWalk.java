package wsdm16.motifs.randomwalks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
/** 
 * A random walk on the space of graphlets of a host graph.
 * This walk is "full", in the sense that it can step from graphlet A to graphlet B if the
 * two graphlets differ by exactly one node, even if their intersection does not induce a connected subgraph of G.
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
import wsdm16.graphutils.BreadthFirstSearch;
import wsdm16.motifs.Graphlet;
import wsdm16.motifs.distributions.EnumeratedIntegerDistribution;

public class FullGraphletRandomWalk extends GraphletRandomWalk {
	public class InvalidStartingNodeException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	}   

	private int realSteps;
	private Map<Integer, Set<Integer>> neighCache; // store neighbors of each graphlet node
	private Map<Integer, Set<Integer>> switchables; // store nodes that can be swapped with H's nodes
	private Map<Set<Integer>, Set<Integer>> neighUnionCache; // store union of nodes neighbors

	/** Constructor.
	 * 
	 * @param G host graph
	 * @param k size of the graphlet (number of nodes)
	 * @param u a node to start from to search for a graphlet
	 * @param maxDeg the maximum degree of G
	 * @throws InvalidStartingNodeException 
	 */
	public FullGraphletRandomWalk(ImmutableGraph G, int k, int u, int maxDeg, RandomGenerator random) throws InvalidStartingNodeException
	{
		this.G = G;
		this.k = k;
		this.random = random;
		this.maxDegree = maxDeg;
		this.H = buildGraphlet(u);
		this.realSteps=0;

		if(H==null)
			throw new InvalidStartingNodeException();

		init();
	}

	public FullGraphletRandomWalk(ImmutableGraph G, int k)
	{
		this.G = G;
		this.k = k;
		this.random = new Well19937c();
		this.realSteps=0;
		init();
	}

	/** Tries to build a graphlet around the given node through a breadth-first search. 
	 * 
	 * @param u the starting node
	 * @return
	 */
	protected Graphlet buildGraphlet(int u)
	{	
		BreadthFirstSearch.Result result = new BreadthFirstSearch(G, u).setMaxNodes(k).visit();
		if (result.getReachedNodes() == k)
			return new Graphlet(G, result.getDistanceMap().keySet());
		else
			return null;
	}

	/** Initialize the random walk.
	 * 
	 */
	protected void init() 
	{
		if(maxDegree==-1) // then compute by ourselves
		{
			NodeIterator it = G.nodeIterator();
			for(int i=G.numNodes(); i>0; i--)
			{
				int u = it.nextInt();
				if(maxDegree<G.outdegree(u))
					maxDegree=G.outdegree(u);
			}
		}

		// find a first graphlet to start from
		int u = 0;
		while (H == null && u < G.numNodes()) {
			H = buildGraphlet(u);
			u++;
		}
		steps = 0;

		// initialize the neighbor cache
		neighCache = new LRUCache<Integer, Set<Integer>>(10 * k);
		for (Integer x : H.getNodes())
			updateNeighCache(x);
		neighUnionCache = new LRUCache<Set<Integer>, Set<Integer>>(10 * k);
	}

	/**
	 * Insert in cache the neighbors of a given node.
	 * @param u
	 */
	protected void updateNeighCache(int u) {
		Set<Integer> neighs = new IntOpenHashSet(G.outdegree(u));
		LazyIntIterator succ = G.successors(u);
		for (int d = G.outdegree(u); d > 0; d--)
			neighs.add(succ.nextInt());
		neighCache.put(u, neighs);
	}

	/**
	 * Return the union of the sets of neighbours of multiple nodes.
	 * 
	 * The method is optimized in that it searches in the cache of neighbors union before recomputing the union,
	 * and in the cache of neighbors before retrieving the neighbors from the graph.
	 * @param who
	 * @return
	 */
	protected Set<Integer> neighborUnion(Collection<Integer> who)
	{
		who = new IntArraySet(who);
		// is it cached?
		if (neighUnionCache.containsKey(who))
			return new IntOpenHashSet(neighUnionCache.get(who));
		// if no, compute it
		Set<Integer> union;
		if (who.size() == 1)
			union = new IntOpenHashSet(neighCache.get(who.iterator().next()));
		else {
			int neigh_estimate = 0;
			// trivial upper estimate of the resulting union's size
			for (int u : who)
				neigh_estimate += G.outdegree(u);
			union = new IntOpenHashSet(neigh_estimate);	
			for (int u : who)
				union.addAll(neighCache.get(u));
		}
		neighUnionCache.put((Set<Integer>)who, union);
		return union;
	}

	/** Perform one step of the walk.
	 * 
	 */
	@Override
	public boolean step()
	{
		stepMany(1);
		return true;
	}

	/**
	 * Transition to a neighboring graphlet (no self-loops).
	 * @return
	 */
	public boolean stepReally()
	{
		int[] uv = drawSwitch(switchables);
		H.removeNode(uv[0]);
		H.addNode(uv[1]);
		updateNeighCache(uv[1]);
		assert(H.size()==k);
		assert(H.isConnected());
		return true;
	}

	/**
	 * A fast BFS implementation for very small graphs.
	 * @param graph
	 * @param from
	 * @param discovered
	 * @return
	 */
	private static int[] BFS(ImmutableGraph graph, int from, boolean[] discovered)
	{
		int[] queue = new int[graph.numNodes()+1];
		int end = 1; //One after the last element
		int top = 0; //The next element to pop
		queue[0]=from;
		discovered[from]=true;

		while(top<end)
		{
			int u = queue[top++];
			LazyIntIterator it = graph.successors(u);
			int v;
			while( (v = it.nextInt())!=-1 )
			{
				if(!discovered[v])
				{
					discovered[v]=true;
					queue[end++]=v;
				}
			}
		}
		queue[end]=-1;
		return queue;
	}

	protected static int[][] connectedComponents(ImmutableGraph graph)
	{
		int[][] components = new int[graph.numNodes()+1][];
		boolean[] discovered = new boolean[graph.numNodes()];

		int numComponents=0;
		for(int u=graph.numNodes()-1; u>=0; u--)
		{
			if(!discovered[u])
				components[numComponents++] = BFS(graph, u, discovered);
		}

		//components[numComponents] = null;
		return components;
	}

	/**
	 * Updates the nodes that can be switched with the graphlet's nodes.
	 */
	public void updateSwitchables() {
		List<Integer> nodes = new ArrayList<>(H.getNodes());
		switchables = new HashMap<>(nodes.size());
		for (int u : H.getNodes()) { // see what can be done with H\{u}
			Graphlet H1 = new Graphlet(H);
			H1.removeNode(u);
			List<Integer> nodes1 = new ArrayList<>(H1.getNodes());

			// get the connected components of H \ {u}
			int[][] connComps = connectedComponents(H1.asGraph());
			@SuppressWarnings("unchecked")
			ArrayList<Integer>[] comps = (ArrayList<Integer>[]) new ArrayList[k - 1];
			int ncomps = 0;
			while (connComps[ncomps] != null) {
				comps[ncomps] = new ArrayList<Integer>();
				for (int i = 0; connComps[ncomps][i] != -1; i++) {
					comps[ncomps].add(nodes1.get(connComps[ncomps][i]));
				}
				ncomps++;
			}

			// compute the intersection of the neighbors of the components, excluding the nodes of H itself
			Set<Integer> intersNeigh = neighborUnion(comps[0]);
			intersNeigh.removeAll(H.getNodes());
			for (int c = 1; c < ncomps; c++)
			{
				// intersect with each component's neighbours
				if (comps[c].size() == 1)
					intersNeigh.retainAll(neighCache.get(comps[c].get(0)));
				else
					intersNeigh.retainAll(neighborUnion(comps[c]));
				if (intersNeigh.isEmpty()) // "fast" fail
					break;
			}
			// the survivors can be switched with u to obtain a new graphlet
			if (intersNeigh.size() > 0)
				switchables.put(u, intersNeigh);
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
	 * Draw a pair of nodes to be switched.
	 * Draw a pair (u,v) u.a.r. from all the pairs of nodes in G where u is in H and v is in G\H and
	 * such that replacing u with v gives again a graphlet. In other words, replacing u with v makes
	 * the walk transition to a neighboring state u.a.r. 
	 * @param switchables a map from the nodes in H to the nodes that can replace them
	 * @return a 2-element array with entries [u, v]
	 */
	private int[] drawSwitch(Map<Integer, Set<Integer>> switchables) {
		// compute the probability that u will be removed -- proportional to its number of switchables
		int[] vNodes = new int[k];
		double[] vProbs = new double[k];
		int i = 0;
		for (Integer u : switchables.keySet()) {
			vNodes[i] = u;
			vProbs[i] = switchables.get(u).size();
			i++;
		}

		// draw the pair of nodes to be switched
		EnumeratedIntegerDistribution dist = new EnumeratedIntegerDistribution(vNodes, vProbs, false);
		// pick the node u to be removed
		int u = dist.sample(random);
		int v = -1;
		Set<Integer> switches = switchables.get(u);
		if (switches.size() > 100) {
			// pick v by iterating over the switchables (avoids converting the set to a list)
			int pos = random.nextInt(switches.size());
			Iterator<Integer> itr = switches.iterator();
			for (int j = 0; j < pos+1; j++)
				v = itr.next();	
		} else {
			// probably faster to just convert into a list
			List<Integer> uList = new ArrayList<>(switches);
			v = uList.get(random.nextInt(switches.size()));
		}
		int[] uv = new int[2];
		uv[0] = u;
		uv[1] = v;
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
			GeometricDistribution geometric = new GeometricDistribution(random, ((double)stateDegree())/ub);
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
		int[] uv = drawSwitch(switchables);
		H.removeNode(uv[0]);
		H.addNode(uv[1]);
		updateNeighCache(uv[1]);
		assert(H.size()==k);
		assert(H.isConnected());
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
