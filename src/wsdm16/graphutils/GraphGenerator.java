package wsdm16.graphutils;

import java.util.Random;

import it.unimi.dsi.webgraph.ArrayListMutableGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.Transform;

/**
 * A generator of common graphs such as stars, trees, circles and more.
 * 
 * @author anon
 */
public class GraphGenerator {

    /**
     * The available types of graph.
     */
    public enum GraphType {
	GNP, GNM, CIRCLE, STAR, TREE, CLIQUE;
    }

    /**
     * The available types of transformations.
     */
    public enum Transformation {
	TRANSPOSE;
    }

    /**
     * Generates an Erdos-Renyi graph G(n,m) using {@link #erdosRenyiGraph(int, int)}, and optionally transpose it.
     * 
     * @param n
     * @param m
     * @return
     */
    public static ImmutableGraph erdosRenyiGraph(int n, int m, Transformation transform) {
	ImmutableGraph G = erdosRenyiGraph(n, m);
	return transform == Transformation.TRANSPOSE ? it.unimi.dsi.webgraph.Transform.transpose(G) : G;
    }

    /**
     * Generates an Erdos-Renyi graph G(n,m). Generates an Erdos-Renyi graph G(n,m) with n nodes and m arcs, where each
     * arc is chosen independently at random among all possible arcs (including loops).
     * 
     * @param n
     * @param m
     * @return
     */
    public static ImmutableGraph erdosRenyiGraph(int n, int m) {
	ArrayListMutableGraph MG = new ArrayListMutableGraph();
	MG.addNodes(n);
	Random rnd = new Random();
	while (MG.numArcs() < m) {
	    int source = rnd.nextInt(n);
	    int end = rnd.nextInt(n);
	    try {
		MG.addArc(source, end);
	    } catch (IllegalArgumentException e) {
	    }
	}
	return MG.immutableView();
    }

    /**
     * @param n
     * @param p
     * @param transform
     * @return
     */
    public static ImmutableGraph erdosRenyiGraph(int n, double p, Transformation transform) {
	ImmutableGraph G = erdosRenyiGraph(n, p);
	return transform == Transformation.TRANSPOSE ? it.unimi.dsi.webgraph.Transform.transpose(G) : G;
    }

    /**
     * Generates an Erdos-Renyi graph G(n,p). Generates an Erdos-Renyi graph G(n,p) with n nodes, where each possible
     * arc independently exists with probability p.
     * 
     * @param n
     * @param p
     * @return
     */
    public static ImmutableGraph erdosRenyiGraph(int n, double p) {
	ArrayListMutableGraph MG = new ArrayListMutableGraph();
	MG.addNodes(n);
	Random rnd = new Random();
	for (int i = 0; i < n; i++)
	    for (int j = 0; j < n; j++)
		if (rnd.nextFloat() <= p)
		    MG.addArc(i, j);
	return MG.immutableView();
    }

    /**
     * @param n
     * @param transform
     * @return
     */
    public static ImmutableGraph circleGraph(int n, Transformation transform) {
	ImmutableGraph G = circleGraph(n);
	return transform == Transformation.TRANSPOSE ? it.unimi.dsi.webgraph.Transform.transpose(G) : G;
    }

    /**
     * Generates a circle graph. Generate a graph on n nodes with arcs (0,1), (1,2), ..., (n-1,0).
     * 
     * @param n
     * @return
     */
    public static ImmutableGraph circleGraph(int n) {
	ArrayListMutableGraph MG = new ArrayListMutableGraph();
	MG.addNodes(n);
	for (int i = 0; i < n; i++) {
	    MG.addArc(i, (i + 1) % n);
	}
	return MG.immutableView();
    }

    /**
     * @param n
     * @param transform
     * @return
     */
    public static ImmutableGraph cliqueGraph(int n, Transformation transform) {
	ImmutableGraph G = cliqueGraph(n);
	return transform == Transformation.TRANSPOSE ? it.unimi.dsi.webgraph.Transform.transpose(G) : G;
    }

    /**
     * Generates a clique graph. Generate a clique graph on n nodes, including loops.
     * 
     * @param n
     * @return
     */
    public static ImmutableGraph cliqueGraph(int n) {
	ArrayListMutableGraph MG = new ArrayListMutableGraph();
	MG.addNodes(n);
	for (int i = 0; i < n; i++)
	    for (int j = 0; j < n; j++)
		MG.addArc(i, j);
	return MG.immutableView();
    }

    /**
     * @param d
     * @param h
     * @param transformation
     * @return
     */
    public static ImmutableGraph treeGraph(int d, int h, Transformation transformation) {
	return treeGraph(d, h, false, transformation);
    }

    /**
     * Generates a tree graph with no loop on the root node. Generates a d-ary tree of depth w. Node 0 is linked by
     * nodes 1, ..., d, node 1 is linked by nodes d+1, ..., d+d; in general, node x is linked by nodes d*x+1, ...,
     * d*x+d. It does not add a self loop on the root node 0.
     * 
     * @param d
     * @param h
     * @return
     */
    public static ImmutableGraph treeGraph(int d, int h) {
	return treeGraph(d, h, false);
    }

    /**
     * @param d
     * @param h
     * @param loopOnRoot
     * @param transformation
     * @return
     */
    public static ImmutableGraph treeGraph(int d, int h, boolean loopOnRoot, Transformation transformation) {
	ImmutableGraph G = treeGraph(d, h, loopOnRoot);
	return transformation == Transformation.TRANSPOSE ? Transform.transpose(G) : G;
    }

    /**
     * Generates a tree graph. Generates a d-ary tree of depth w. Node 0 is linked by nodes 1, ..., d, node 1 is linked
     * by nodes d+1, ..., d+d; in general, node x is linked by nodes d*x+1, ..., d*x+d.
     * 
     * @param d
     * @param h
     * @param loopOnRoot
     *            if true, adds a self-loop to the root node 0.
     * @return
     */
    public static ImmutableGraph treeGraph(int d, int h, boolean loopOnRoot) {
	ArrayListMutableGraph MG = new ArrayListMutableGraph();
	MG.addNodes(1);
	if (loopOnRoot)
	    MG.addArc(0, 0);
	int target = 0; // start from a single node
	for (int l = 0; l < h; l++) {
	    MG.addNodes((int) Math.pow(d, l + 1)); // add the next level of nodes
	    for (int i = 0; i < (int) Math.pow(d, l); i++) { // and link them to the nodes at level l
		for (int j = 0; j < d; j++)
		    MG.addArc(target * d + j + 1, target);
		target++;
	    }
	}
	return MG.immutableView();
    }

    /**
     * @param n
     * @param transformation
     * @return
     */
    public static ImmutableGraph starGraph(int n, Transformation transformation) {
	ImmutableGraph G = starGraph(n);
	return transformation == Transformation.TRANSPOSE ? Transform.transpose(G) : G;
    }

    /**
     * Generates a star graph with no loop on the root node. Generates a graph on n nodes where nodes 1, ..., n-1 link
     * to node 0, with no loop on the root node.
     * 
     * @param n
     * @return
     */
    public static ImmutableGraph starGraph(int n) {
	return starGraph(n, false);
    }

    /**
     * @param n
     * @param loopOnRoot
     * @param transformation
     * @return
     */
    public static ImmutableGraph starGraph(int n, boolean loopOnRoot, Transformation transformation) {
	ImmutableGraph G = starGraph(n, loopOnRoot);
	return transformation == Transformation.TRANSPOSE ? Transform.transpose(G) : G;
    }

    /**
     * Generates a star graph. Generates a graph on n nodes where nodes 1, ..., n-1 link to node 0.
     * 
     * @param n
     * @param loopOnRoot
     *            if true, add a self-loop to the root node 0.
     * @return
     */
    public static ImmutableGraph starGraph(int n, boolean loopOnRoot) {
	ArrayListMutableGraph MG = new ArrayListMutableGraph();
	MG.addNodes(n);
	if (loopOnRoot)
	    MG.addArc(0, 0);
	for (int i = 1; i < n; i++)
	    MG.addArc(i, 0);
	return MG.immutableView();
    }

}
