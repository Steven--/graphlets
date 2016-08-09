package wsdm16.graphutils;

import java.util.Arrays;
import java.util.List;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.webgraph.ArrayListMutableGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.NodeIterator;

public class Transform {

    /** Merge two graphs.
     * 
     * @param G
     * @param H
     * @return
     */
    public static ArrayListMutableGraph merge(ImmutableGraph G, ImmutableGraph H) {
	ArrayListMutableGraph M = new ArrayListMutableGraph(G);
	M.addNodes(Math.max(H.numNodes() - G.numNodes(), 0));
	NodeIterator itr = H.nodeIterator();
	for (int i = 0; i < H.numNodes(); i++) {
	    int u = itr.nextInt();
	    LazyIntIterator succ = itr.successors();
	    for (int d = itr.outdegree(); d > 0; d--) {
		try {
		    M.addArc(u, succ.nextInt());
		} catch (IllegalArgumentException e) {};
	    }
	}
	return M;
    }

    /** Reciprocates all the arcs of a graph.
     * 
     * @param G
     * @return
     */
    public static ArrayListMutableGraph symmetrize(ImmutableGraph G) {
	return merge(G, it.unimi.dsi.webgraph.Transform.transpose(G));
    }

    /** Remove all self-loops of a graph
     * 
     * @param G
     * @return
     */
    public static ImmutableGraph removeSelfLoops(ImmutableGraph G)
    {	
    	return it.unimi.dsi.webgraph.Transform.filterArcs(G,it.unimi.dsi.webgraph.Transform.NO_LOOPS);
    }
    
    /** Check if the specified map is an isomorphism between G and H
     * 
     * @param G
     * @param H
     * @param idMap an int array specifying how the nodes of G are mapped on H
     * @return true if and only if for each arc (i,j) in G the arc (idMap[i],idMap[j]) exists in H
     */
    public static boolean isMapped(ImmutableGraph G, ImmutableGraph H, int[] idMap) {
	NodeIterator itr = G.nodeIterator();
	while (itr.hasNext()) {
	    int uG = itr.nextInt();
	    int uH = idMap[uG];
	    if (G.outdegree(uG) != H.outdegree(uH))
		return false;
	    LazyIntIterator succG = G.successors(uG);
	    LazyIntIterator succH = H.successors(uH);
	    for (int d = G.outdegree(uG); d > 0; d--) {
		if (idMap[succG.nextInt()] != succH.nextInt())
		    return false;
	    }
	}
	return true;
    }

    /** Check if two graphs are isomorphic.
     * 
     * @param G
     * @param H
     * @return an array giving an isomorphism from G to H if the two are isomorphic, or null otherwise
     */
    public static int[] isIsomorphic(ImmutableGraph G, ImmutableGraph H) {
	// check number of nodes and arcs
	if (G.numArcs() != H.numArcs() || G.numNodes() != H.numNodes())
	    return null;
	// check degree sequence
	int[] degG = new int[G.numNodes()];
	int[] degH = new int[H.numNodes()];
	IntIterator degItrG = G.outdegrees();
	IntIterator degItrH = H.outdegrees();
	for (int i = 0; i < G.numNodes(); i++) {
	    degG[i] = degItrG.nextInt();
	    degH[i] = degItrH.nextInt();
	}
	Arrays.sort(degG);
	Arrays.sort(degH);
	if (!Arrays.equals(degG, degH))
	    return null;
	// try bruteforce
	PermutationGenerator permGen = new PermutationGenerator(G.numNodes());
	while (permGen.hasNext()) {
	    int[] perm = permGen.next();
	    if (isMapped(G, H, perm))
		return perm;	    
	}
	return null;
    }

    /** Relabel a graph.
     * 
     * @param G the original graph
     * @param idMap a map from [0, ..., n-1] to [0, ..., n-1]
     * @return a graph where the arc (idMap[u],idMap[v]) exists if (u,v) exists in G
     */
    public static ImmutableGraph relabelGraph(ImmutableGraph G, int[] idMap) {
	ArrayListMutableGraph H;
	H = new ArrayListMutableGraph(G.numNodes());
	NodeIterator itr = G.nodeIterator();
	while (itr.hasNext()) {
	    int u = itr.nextInt();
	    LazyIntIterator succs = G.successors(u);
	    for (int d = G.outdegree(u); d > 0; d--)
		H.addArc(idMap[u], idMap[succs.nextInt()]);
	}
	return H.immutableView();
    }

    /** Relabel a graph.
     * 
     * @param G
     * @param idMap
     * @return
     */
    public static ImmutableGraph relabelGraph(ImmutableGraph G, List<Integer> idMap) {
	int[] map = new int[idMap.size()];
	for (int i = 0; i < map.length; i++)
	    map[i] = idMap.get(i);
	return relabelGraph(G, map);
    }
    
    /** Return a hash code of the family of graphs isomorphic to G.
     * 
     * @param G
     * @return the minimum hash code over all relabelings of G.
     */
    public static int hashCodeIm(ImmutableGraph G) {
	PermutationGenerator permGen = new PermutationGenerator(G.numNodes());
	int idMap[];
	int result = Integer.MAX_VALUE;
	while (permGen.hasNext()) { // find the minimum ImmutableGraph.hashCode() over all relabelings
	    idMap = permGen.next();
	    int hc = relabelGraph(G, idMap).hashCode();
	    result = hc < result? hc : result;
	}
	return result;
    }

}
