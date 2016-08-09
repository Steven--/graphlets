package wsdm16.motifs.randomwalks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/** A random walk on the space of graphlets of a host graph.
 * This walk is "coward" in the sense that it only steps to graphlets that can be obtained by removing nodes that do not disconnect the current graphlet; in other words, it can move from graphlet A to graphlet B only if their intersection is also connected graphlet.
 * @author anon
 */
import java.util.Set;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.distribution.GeometricDistribution;
import org.apache.commons.math3.random.Well19937c;

import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;
import wsdm16.graphutils.BreadthFirstSearch;
import wsdm16.motifs.Graphlet;

public class CowardGraphletRandomWalk extends GraphletRandomWalk {
    /** Constructor.
     * 
     * @param G host graph
     * @param k size of the graphlet (number of nodes)
     */
    public CowardGraphletRandomWalk(ImmutableGraph G, int k) {
	this.G = G;
	this.k = k;
	this.H = null;
	init();
    }

    /** Constructor.
     * 
     * @param G host graph
     * @param k size of the graphlet (number of nodes)
     * @param u a node to start from to search for a graphlet
     */
    public CowardGraphletRandomWalk(ImmutableGraph G, int k, int u) {
	this.G = G;
	this.k = k;
	this.H = buildGraphlet(u);
	init();
    }

    /** Constructor.
     * 
     * @param G host graph
     * @param k size of the graphlet (number of nodes)
     * @param u a node to start from to search for a graphlet
     * @param maxDeg the maximum degree of G
     */
    public CowardGraphletRandomWalk(ImmutableGraph G, int k, int u, int maxDeg)
    {
		this.G = G;
		this.k = k;
		this.H = buildGraphlet(u);
		this.maxDegree = maxDeg;
		init();
    }

    
    /** Constructor.
     * 
     * @param G host graph
     * @param k size of the graphlet (number of nodes)
     * @param H the initial graphlet
     */
    public CowardGraphletRandomWalk(ImmutableGraph G, Graphlet H) {
	this.G = G;
	this.k = H.size();
	this.H = H;
	init();
    }
    

    /** Constructor.
     * 
     * @param G host graph
     * @param k size of the graphlet (number of nodes)
     * @param H the initial graphlet
     * @param maxDeg the maximum degree of G
     */
    public CowardGraphletRandomWalk(ImmutableGraph G, Graphlet H, int maxDeg) {
	this.G = G;
	this.k = H.size();
	this.H = H;
	this.maxDegree = maxDeg;
	init();
    }

    
    /** Tries to build a graphlet around the given node through a breadth-first search. 
     * 
     * @param u the starting node
     * @return
     */
    private Graphlet buildGraphlet(int u) {
	BreadthFirstSearch.Result result = new BreadthFirstSearch(G, u).setMaxNodes(k).visit();
	    if (result.getReachedNodes() == k)
		return new Graphlet(G, result.getDistanceMap().keySet());
	    else
		return null;
    }
    
    /** Initialize the random walk.
     * 
     */
    private void init()
    {
    	if(maxDegree==-1)
    	{
    		NodeIterator it = G.nodeIterator();
    		for(int i=G.numNodes(); i>0; i--)
	    	{
    			int u = it.nextInt();
	    		if(maxDegree<G.outdegree(u))
	    			maxDegree=G.outdegree(u);
	    	}
    	}
    	
		// Find a first graphlet
		int u = 0;
		while (H == null && u < G.numNodes()) {
		    H = buildGraphlet(u);
		    u++;
		}
		//stateHistory = new ArrayList<>();
		//degreeHistory = new ArrayList<>();
		steps = 0;
		random = new Well19937c();
    }
        
    /** Perform one step of the walk.
     * 
     */
    public boolean step()
    {
    	stepMany(1);
    	return true;
    }
    

    private int stepMany(int howMany) 
    {
		Map<Integer, Set<Integer>> switchables = H.switchableNodes();
		int[] vNodes = new int[switchables.size()];
		double[] vProbs = new double[switchables.size()];
		int i = 0, totalSize = 0;
		for (Integer u : switchables.keySet())
		{
		    vNodes[i] = u;
		    vProbs[i] = switchables.get(u).size();
		    totalSize += vProbs[i];
		    i++;
		}

		int numFailures = Integer.MAX_VALUE;
		if(totalSize!=0)
		{
			int ub = (k-1)*k*maxDegree; //For each vertex of the motif, we have at most (k-1) * maxDeg possible candidates

			GeometricDistribution geometric = new GeometricDistribution( ((double)totalSize)/ub );
			numFailures = geometric.sample();
		}
		
		if(numFailures>=howMany)
		{
			steps+=howMany;
			return howMany;
		}

		steps += numFailures+1;
		
		//stateHistory.add(new HashSet<Integer>(H.getNodes()));
		//degreeHistory.add(totalSize);
		
		for (int j = 0; j < vProbs.length; j++)
		    vProbs[j] /= totalSize;
		
		EnumeratedIntegerDistribution dist = new EnumeratedIntegerDistribution(vNodes, vProbs);
		int u = dist.sample();
		List<Integer> uList = new ArrayList<>(switchables.get(u));
		int v = uList.get(random.nextInt(uList.size()));
		H.removeNode(u);
		H.addNode(v);
		return numFailures+1;
    }
    
    /** Perform multiple steps of the walk.
     * 
     */
    public int walk(int steps)
    {
		int end = this.steps + steps;
		while (this.steps < end)
			stepMany(end-this.steps);
		
		return steps; 
    }
    
}
