package wsdm16.motifs.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import it.unimi.dsi.webgraph.ImmutableGraph;
import wsdm16.graphutils.GraphGenerator;
import wsdm16.graphutils.Transform;
import wsdm16.motifs.colorcoding.CC2;
import wsdm16.motifs.colorcoding.BaseColorCoding.IColorCodingSampler;
import wsdm16.motifs.colorcoding.SetColoredTreelet;

public class TestAlonColorCoding {

	@Test
	public void testSetColoredTreelet()
	{
		SetColoredTreelet t1_0 = new SetColoredTreelet(0);
		SetColoredTreelet t1_1 = new SetColoredTreelet(1);
		SetColoredTreelet t1_2 = new SetColoredTreelet(2);
		
		SetColoredTreelet t2_01 = (SetColoredTreelet)t1_0.merge(t1_1);
		SetColoredTreelet t2_12 = (SetColoredTreelet)t1_1.merge(t1_2);
		
		SetColoredTreelet t3_012 = (SetColoredTreelet)t2_01.merge(t1_2);
		SetColoredTreelet t3_120 = (SetColoredTreelet)t2_12.merge(t1_0);
		
		//assertTrue(t2_01.equals(t2_12));
		//assertTrue(t2_12.equals(t2_01));

		assertTrue(t3_012.equals(t3_120));
		assertTrue(t3_120.equals(t3_012));

		assertTrue(t3_012.hashCode() ==  t3_120.hashCode());

	}
	
	@Test
	public void testGnp() throws InterruptedException
	{
    	int n=10000;
    	int k=5;
    	ImmutableGraph G = Transform.symmetrize(GraphGenerator.erdosRenyiGraph(n, n*10) ).immutableView();
    	G = Transform.removeSelfLoops(G);
 	
    	CC2 C = new CC2(G, k);
    	C.color();
    	C.run(4);
		
    	C.buildStructures(); 
		IColorCodingSampler sampler = C.newSampler(k);
    	for(int i=0; i<100; i++)
    	{
//    		System.out.println(C.sampleRoot(4));
    		System.out.println(sampler.sample());
    	}
    	
	}
}
