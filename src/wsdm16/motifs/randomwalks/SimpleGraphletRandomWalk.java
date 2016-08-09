package wsdm16.motifs.randomwalks;
/** A random walk on the space of graphlets of a host graph.
 * 
 * @author anon
 */
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.random.Well19937c;

import it.unimi.dsi.webgraph.ImmutableGraph;
import wsdm16.graphutils.BreadthFirstSearch;
import wsdm16.motifs.Graphlet;

public class SimpleGraphletRandomWalk extends GraphletRandomWalk {
    /** Constructor.
     * 
     * @param G host graph
     * @param k size of the graphlet (number of nodes)
     */
    public SimpleGraphletRandomWalk(ImmutableGraph G, int k) {
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
    public SimpleGraphletRandomWalk(ImmutableGraph G, int k, int u) {
	this.G = G;
	this.k = k;
	this.H = buildGraphlet(u);
	init();
    }

    /** Constructor.
     * 
     * @param G host graph
     * @param k size of the graphlet (number of nodes)
     * @param H the initial graphlet
     */
    public SimpleGraphletRandomWalk(ImmutableGraph G, Graphlet H) {
	this.G = G;
	this.k = H.size();
	this.H = H;
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
    private void init() {
	// Find a first graphlet
	int u = 0;
	while (H == null && u < G.numNodes()) {
	    H = buildGraphlet(u);
	    u++;
	}
	//stateHistory = new ArrayList<>();
	//degreeHistory = new ArrayList<>();
	steps = 0;
	this.random = new Well19937c();
    }
        
    /** Perform one step of the walk.
     * @return false if and only if no step could be done (no adjacent graphlet exists).
     * 
     */
    public boolean step() {
	List<Integer> neighs = new ArrayList<>(H.neighbors());
	if (neighs.size() == 0)
	    return false;
	Graphlet nextH = null;
	List<Integer> nodes = new ArrayList<>(H.getNodes());
	while (nextH == null) { // simply try to switch a graphlet node with a neighbour
	    nextH = new Graphlet(G, nodes);
	    nextH.removeNode(nodes.get(random.nextInt(nodes.size())));
	    nextH.addNode(neighs.get(random.nextInt(neighs.size())));
	    if (!nextH.isConnected())
		nextH = null;
	}
	H.setNodes(nextH.getNodes());
	//stateHistory.add(new HashSet<Integer>(H.getNodes()));
	steps++;
	return true;
    }
    
    /** Perform multiple steps of the walk.
     * @return false if and only if some step could not be done (no adjacent graphlet existed).
     * 
     */
    public int walk(int steps) {
	int i = 0;
	while (step() && i < steps)
	    i++;
	return i; 
    }
    
}
