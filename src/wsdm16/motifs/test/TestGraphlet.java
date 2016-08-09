package wsdm16.motifs.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import it.unimi.dsi.webgraph.ArrayListMutableGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import wsdm16.motifs.Graphlet;

public class TestGraphlet {

    @Test
    public void test() {
	Collection<Integer> nodes = Arrays.asList(0,4,2,9);
	int k = nodes.size();
	ImmutableGraph G = ArrayListMutableGraph.newCompleteGraph(20, false).immutableView();
	Graphlet H = new Graphlet(G, nodes);
	assertEquals(H.removableNodes(), H.getNodes());
	assertEquals(H.size(), k);
	assertEquals(H.asGraph().numArcs(), k*(k-1));
	assertEquals(H.neighbors().size(), 20-k);
	// add an already-existing node
	H.addNode(0);
	assertEquals(H.size(), k);
	assertEquals(H.asGraph().numArcs(), k*(k-1));
	// add a new node
	H.addNode(1);
	k++;
	assertEquals(H.size(), k);
	assertEquals(H.asGraph().numArcs(), k*(k-1));
	assertEquals(H.neighbors().size(), 20-k);
	// remove two nodes
	H.removeNode(4);
	H.removeNode(2);
	k -= 2;
	assertEquals(H.size(), k);
	assertEquals(H.asGraph().numArcs(), k*(k-1));
	assertEquals(H.neighbors().size(), 20-k);
	System.out.println(H.getNodes());
    }

}
