package wsdm16.motifs.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import it.unimi.dsi.webgraph.ArrayListMutableGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import wsdm16.graphutils.GraphGenerator;
import wsdm16.motifs.Graphlet;
import wsdm16.motifs.randomwalks.FullGraphletRandomWalk;
import wsdm16.motifs.randomwalks.GraphletRandomWalk;
import wsdm16.motifs.randomwalks.SimpleGraphletRandomWalk;

public class TestGraphletRandomWalk {
    
    public TestGraphletRandomWalk() {
	// TODO init parameters
    }
    
    @Parameters
    public static Collection<Object[]> data() {
	List<Object[]> data = new ArrayList<Object[]>();
	return data;
    }
    
    /*
     * Check that no two adjacent states are the same
     */
    void checkHistory(List<Set<Integer>> history) {
	Set<Integer> old = new HashSet<>();
	for (Set<Integer> state : history) {
	    assertTrue(state != old);
	    old = state;
	}

    }
    
    /*
     * Test on the clique host graph
     */
//    @Test
    public void testClique() {
	int n = 50;
	int k = 5;

	ImmutableGraph G = ArrayListMutableGraph.newCompleteGraph(n, false).immutableView();
	GraphletRandomWalk walk = new SimpleGraphletRandomWalk(G, k);
	Graphlet H = walk.getGraphlet();
	assertEquals("walk.getGraphlet().size() should equal the constructor parameter.", k, H.size());

	walk.walk(100);
	//assertEquals("History length should match walk length.", 101, walk.stateHistory().size());
	//checkHistory(walk.stateHistory());

	walk.step();
	H = walk.getGraphlet();
//	System.out.println(H.size());
	assertEquals("walk.getGraphlet().size() should equal the constructor parameter.", k, H.size());
    }

    /**
     *  Test on G(n,p)
     */
    @Test
    public void testGnp() {
	int n = 100, d = 10;
	int k = 5;
	
	ImmutableGraph ER = GraphGenerator.erdosRenyiGraph(n, d*n);
	ImmutableGraph G = wsdm16.graphutils.Transform.symmetrize(ER).immutableView();

	GraphletRandomWalk walk = new FullGraphletRandomWalk(G, k);
	Graphlet H = walk.getGraphlet();
	assertEquals("walk.getGraphlet().size() should equal the constructor parameter.", k, H.size());
	assertTrue(H.isConnected());
	
	int walked = walk.walk(100);
	//System.out.println(walk.stateHistory());
	//System.out.println(walk.degreeHistory());
	assertEquals("Should have walked 100 steps", 100, walked);
	//assertEquals("History length should match walk length.", 101, walk.stateHistory().size());
	//checkHistory(walk.stateHistory());

	walk.step();
	H = walk.getGraphlet();
	assertEquals("walk.getGraphlet().size() should equal the constructor parameter.", k, H.size());
	assertTrue(H.isConnected());
    }
    
}
