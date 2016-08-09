package wsdm16.graphutils.test;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.webgraph.ImmutableGraph;
import wsdm16.graphutils.GraphGenerator;

public class TestGraphGenerator {

    public static void main(String[] args) {
	ImmutableGraph G;
	
	for (int n = 10; n < 1000; n = n * 2 + 3) {
	    // Circle
	    G = GraphGenerator.circleGraph(n);
	    assert (G.numNodes() == n);
	    assert (G.numArcs() == n);
	    // Clique
	    G = GraphGenerator.cliqueGraph(n);
	    assert (G.numNodes() == n);
	    assert (G.numArcs() == n * n);
	    // G(n,p)
	    G = GraphGenerator.erdosRenyiGraph(n, 1.0);
	    assert (G.numNodes() == n);
	    assert (G.numArcs() == n * n);
	    for (double p = 0; p < 1; p += 0.2) {
		G = GraphGenerator.erdosRenyiGraph(n, p);
		assert (G.numNodes() == n);
		assert (G.numArcs() <= 2 * n * n * p);
		assert (G.numArcs() >= 0.5 * n * n * p);
	    }
	    // G(n,m)
	    for (int m = 1; m < n / 10; m *= 2) {
		G = GraphGenerator.erdosRenyiGraph(n, m);
		assert (G.numNodes() == n);
		assert (G.numArcs() == m);
	    }
	    // Star
	    G = GraphGenerator.starGraph(n);
	    assert (G.numNodes() == n);
	    assert (G.numArcs() == n - 1);
	    G = GraphGenerator.starGraph(n, true);
	    assert (G.numNodes() == n);
	    assert (G.numArcs() == n);
	}
	
	// Tree
	for (int w = 2; w <= 10; w += 1) {
	    for (int h = 0; h < 6; h++) {
		int n = h > 0 ? ((int) Math.pow(w, h + 1) - 1) / (w - 1) : 1;
		// Without loop on root node
		G = GraphGenerator.treeGraph(w, h);
		assert (G.numNodes() == n) : "treeGraph(" + w + "," + h + ") should contain " + n
			+ " nodes, actually contains " + G.numNodes();
		assert (G.numArcs() == n - 1) : "treeGraph(" + w + "," + h + ") should contain " + (n - 1)
			+ " arcs, actually contains " + G.numArcs();
		IntIterator outDegs = G.outdegrees();
		assert (outDegs.next() == 0);
		while (outDegs.hasNext())
		    assert (outDegs.next() == 1);
		// With loop on root node
		G = GraphGenerator.treeGraph(w, h, true);
		assert (G.numNodes() == n) : "treeGraph(" + w + "," + h + ") should contain " + n
			+ " nodes, actually contains " + G.numNodes();
		assert (G.numArcs() == n) : "treeGraph(" + w + "," + h + ") should contain " + (n - 1)
			+ " arcs, actually contains " + G.numArcs();
		outDegs = G.outdegrees();
		while (outDegs.hasNext())
		    assert (outDegs.next() == 1);
	    }
	}
    }

}
