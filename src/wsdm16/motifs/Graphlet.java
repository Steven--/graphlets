package wsdm16.motifs;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.webgraph.ArrayListMutableGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import wsdm16.graphutils.BreadthFirstSearch;

/**
 * A graphlet (connected induced subgraph) of a graph. This class represents a
 * graphlet, that is, a connected induced subgraph of a graph (called the host
 * graph). It stores information about the vertex set of the graphlet and their
 * neighbours. It is your responsibility to pass a symmetric host graph,
 * otherwise results can be meaningless or unpredictable.
 * 
 * @author anon Graphlet.java - created on 30 giu 2016 -
 */
public class Graphlet {

	private ImmutableGraph G;
	private Set<Integer> nodes;
	private Set<Integer> neighbors;
	private boolean upToDate;

	/**
	 * Copy constructor.
	 * 
	 * @param copy
	 */
	public Graphlet(Graphlet copy) {
		this(copy.getG(), copy.getNodes());
	}

	/**
	 * Constructor.
	 * 
	 * @param g
	 *            the host graph
	 * @param nodes
	 *            vertices of the graphlet
	 */
	public Graphlet(ImmutableGraph g, Collection<Integer> nodes)
			throws ArrayIndexOutOfBoundsException {
		G = g;
		setNodes(nodes);
		for (int u : this.nodes)
			checkNode(u);
		neighbors = new IntOpenHashSet();
		upToDate = false;
	}

	/**
	 * Returns the number of nodes in the graphlet.
	 * 
	 * @return
	 */
	public int size() {
		return nodes.size();
	}

	/**
	 * Set the nodes of the graphlet.
	 * 
	 * @param nodes
	 *            a set of nodes. The set is copied, not referenced.
	 */
	public void setNodes(Collection<Integer> nodes) {
		this.nodes = new HashSet<Integer>(nodes);
		upToDate = false;
	}

	/**
	 * Return the nodes of the graphlet.
	 * @return the set of nodes of the host graph which form the graphlet.
	 */
	public Set<Integer> getNodes() {
		return nodes;
	}

	/**
	 * Checks if a node is in the host graph. If the node is not in the range
	 * 0,...,n-1 then an exception is thrown.
	 * 
	 * @param u
	 * @throws ArrayIndexOutOfBoundsException
	 */
	private void checkNode(int u) throws ArrayIndexOutOfBoundsException {
		if (u < 0 || u >= G.numNodes())
			throw new ArrayIndexOutOfBoundsException(
					"Node " + u + " is out of range.");
	}

	/**
	 * Add a node to the graphlet's vertex set.
	 * 
	 * @param node
	 */
	public void addNode(Integer node) {
		nodes.add(node);
		upToDate = false;
	}

	/**
	 * Remove a node from the graphlet's vertex set.
	 * 
	 * @param node
	 * @return true if the node was in the node set of the graphlet.
	 */
	public boolean removeNode(Integer node) {
		boolean hadit = nodes.remove(node);
		upToDate = false;
		return hadit;
	}

	/**
	 * Return the set of neighbors of the graphlet's nodes in the host graph
	 * (excluding the graphlet's nodes).
	 * 
	 * @return
	 */
	public Set<Integer> neighbors() {
		if (!upToDate) {
			neighbors.clear();
			for (int u : nodes) {
				neighbors.addAll(new IntArrayList(G.successorArray(u)));
			}
			neighbors.removeAll(nodes);
			upToDate = true;
		}
		return neighbors;
	}

	/**
	 * Returns the graphlet as a new graph. The indices of the nodes will be in
	 * the range 0, ..., size-1, thererefore the IDs of the original nodes will
	 * be lost.
	 * 
	 * @return the graphlet graph
	 */
	public ImmutableGraph asGraph() {
		return asMutableGraph().immutableView();
	}

	/**
	 * Checks if the graphlet is indeed connected.
	 * 
	 * @return
	 */
	public boolean isConnected() {
		if (new BreadthFirstSearch(asGraph(), 0).count()
				.getReachedNodes() == size())
			return true;
		else
			return false;
	}

	/**
	 * Returns the graphlet as a new graph. The indices of the nodes will be in
	 * the range 0, ..., size-1, thererefore the IDs of the original nodes will
	 * be lost.
	 * 
	 * @return the graphlet graph
	 */
	public ArrayListMutableGraph asMutableGraph() {
		ArrayListMutableGraph h = new ArrayListMutableGraph();

		// Int2IntOpenHashMap hashMap = new Int2IntOpenHashMap(nodes.size());
		// int i=0;
		// for (int u : nodes)
		// hashMap.put(u, i++);

		IntArrayList a = new IntArrayList(nodes);
		h.addNodes(nodes.size());

		for (int u : nodes) {
			LazyIntIterator succ = G.successors(u);
			for (int d = G.outdegree(u); d > 0; d--) {
				int v = succ.nextInt();
				if (a.contains(v))
					h.addArc(a.indexOf(u), a.indexOf(v));

				// if (hashMap.containsKey(v))
				// h.addArc(hashMap.get(u), hashMap.get(v));
			}
		}
		return h;
	}

	/**
	 * Returns the set of nodes that can be removed (one at a time) without
	 * disconnecting the graphlet.
	 * 
	 * @return
	 */
	public Set<Integer> removableNodes() {
		ImmutableGraph H = asGraph();
		IntArrayList a = new IntArrayList(nodes);
		Set<Integer> rn = new HashSet<Integer>();
		for (int i = 0; i < H.numNodes(); i++) { // remove each node in turn,
												 // count CC size via BFS
			ArrayListMutableGraph F = new ArrayListMutableGraph(H);
			F.removeNode(i);
			if (new BreadthFirstSearch(F.immutableView(), 0).count()
					.getReachedNodes() == F.numNodes()) // still connected!
				rn.add(a.getInt(i));
		}
		return rn;
	}

	/**
	 * Computes pairs of switchable nodes between the graphlet and the rest of
	 * the graph. For each removable node of the graphlet returns a set of nodes
	 * that can be switched with it to obtain a new graphlet of the same size.
	 * 
	 * @return
	 */
	public Map<Integer, Set<Integer>> switchableNodes() {
		Map<Integer, Set<Integer>> map = new HashMap<Integer, Set<Integer>>();
		Set<Integer> removables = removableNodes();
		for (int u : removables)
			map.put(u, new HashSet<Integer>());

		for (int u : nodes) {
			// for each of the neighbors of nodes, add it in the switchable set
			// of others
			LazyIntIterator succ = G.successors(u);
			for (int d = G.outdegree(u); d > 0; d--) {
				int v = succ.nextInt();
				if (!nodes.contains(v)) // cannot be a node already in the
										// graphlet
					for (int u1 : removables) // add it as switchable to all
											  // other removables
						if (u1 != u)
							map.get(u1).add(v);
			}
		}
		return map;
	}

	/**
	 * Return the host graph.
	 * @return the host graph of the graphlet.
	 */
	public ImmutableGraph getG() {
		return G;
	}

}
