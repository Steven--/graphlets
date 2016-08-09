package wsdm16.graphutils.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import it.unimi.dsi.webgraph.ArrayListMutableGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import wsdm16.graphutils.GraphGenerator;
import wsdm16.graphutils.Transform;

public class TestTransform {

    @Test
    public void test() {
	testIsIsomorphic();
	testHashCodeIm();
    }

//    private void testIsMapped() {
//
//    }

    private void testIsIsomorphic() {
	ImmutableGraph G = ArrayListMutableGraph.newCompleteGraph(10, false).immutableView();
	ImmutableGraph H = ArrayListMutableGraph.newCompleteGraph(9, false).immutableView();
	assertEquals(null, Transform.isIsomorphic(G,H));
	H = G.copy();
	assertTrue(Transform.isIsomorphic(G,H).length == G.numNodes());
    }

    private void testHashCodeIm() {
	//	ImmutableGraph G = ArrayListMutableGraph.newCompleteGraph(4, false).immutableView();
	ImmutableGraph G = GraphGenerator.erdosRenyiGraph(6, 10);
	int n = G.numNodes();
	int hcG = Transform.hashCodeIm(G);
	System.out.println("the family hash code of G is " + hcG);
	List<Integer> map = new ArrayList<>(n);
	for (int i = 0; i < n; i++)
	    map.add(i);
//	System.out.println("hash codes are " + G.hashCode() + " " + H.hashCode());
	for (int k = 0; k < 10; k++) {
	    Collections.shuffle(map);
//	    System.out.println(map);
	    ImmutableGraph H = Transform.relabelGraph(G, map);
	    int hcH = Transform.hashCodeIm(H);
	    assertEquals(hcG, hcH);
	    System.out.println("G relabeled as " + map + " has family hash code " + hcH);
	}
    }

}
