package wsdm16.motifs;
import java.util.HashMap;
import java.util.Map;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.NodeIterator;

public class ImmutableGraphWrapper {
	
	protected Map<Integer, IntOpenHashSet> bigMap;
	protected Map<Integer, IntArraySet> smallMap;
	protected Map<Integer, IntSet> neighMap;

	public ImmutableGraphWrapper(ImmutableGraph G) {
		this(G, 100);
	}
	
	public ImmutableGraphWrapper(ImmutableGraph G, int thr) {
		neighMap = new HashMap<>();
		NodeIterator itr = G.nodeIterator();
		while (itr.hasNext()) {
			int u = itr.nextInt();
			if (G.outdegree(u) > thr)
				neighMap.put(u, new IntOpenHashSet(G.successorArray(u), 0, G.outdegree(u)));
			else
				neighMap.put(u, new IntArraySet(G.successorArray(u).clone(), G.outdegree(u)));
		}
	}
	
	/**
	 * Check if v is an out-neighbor of u.
	 * @param u
	 * @param v
	 * @return
	 */
	public boolean isArc(int u, int v) {
		return neighMap.get(u).contains(v);
	}
	
	/**
	 * Check if u and v are neighbors.
	 * @param u
	 * @param v
	 * @return
	 */
	public boolean areNeighbors(int u, int v) {
		return isArc(u, v) || isArc(v, u);
	}
}
