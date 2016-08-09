package wsdm16.motifs.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import it.unimi.dsi.webgraph.ImmutableGraph;
import wsdm16.graphutils.GraphGenerator;
import wsdm16.graphutils.Transform;
import wsdm16.motifs.Graphlet;
import wsdm16.motifs.SpanningTrees;
import wsdm16.motifs.colorcoding.BaseColorCoding;
import wsdm16.motifs.colorcoding.BaseColorCoding.IColorCodingSampler;
import wsdm16.motifs.colorcoding.ColorCoding;

public class TestColorCoding {
/*
	@Test
	public void testStar() throws InterruptedException
	{
    	int n=10000;
    	ImmutableGraph G = Transform.symmetrize(GraphGenerator.starGraph(n) ).immutableView();

    	C2Treelet t1 = new C2Treelet(0);
    	C2Treelet t2 = new C2Treelet(1);
    	C2Treelet t = t1.merge(t2);

    	int count = 0;
    	for(int i=0; i<100; i++)
    	{
    		ColorCoding C = new ColorCoding(G, 4);
    		C.color();
    		C.run();
    		C.buildStructures();
    		int r = C.sampleRoot(t);
    		count += (r!=-1)?1:0;
    		
    		if(C.getColorOf(0)==0)
    		{
    			C.sampleRoot(t);
    			assertEquals(0, r);
    		}
    		else if(C.getColorOf(0)==1)
    		{
    			assertNotEquals(-1, r);
    			assertNotEquals(0, r);
    		}
    		else
    			assertEquals(-1, r);
    	}
    	
    	assertTrue("Count is "+count, count>=40);
	}

	@Test
	public void testStar2() throws InterruptedException
	{
    	int n=10000;
    	ImmutableGraph G = Transform.symmetrize(GraphGenerator.starGraph(n) ).immutableView();

		ColorCoding C = new ColorCoding(G, 4);    	
		C.color();

		int c = C.getColorOf(0);
		
    	C2Treelet t1 = new C2Treelet(c);
    	int c1 = (c<=1)?2:0;
    	C2Treelet t2 = new C2Treelet( c1 ); 
    	int c2 = (c<=1)?3:1;
    	C2Treelet t3 = new C2Treelet( c2 );

    	C2Treelet t = t1.merge(t2).merge(t3);
    	
		C.run();
		C.buildStructures(3);
		
    	for(int i=0; i<100; i++)
    	{
    		List<Integer> occ= C.sample(t);

    		if(occ!=null)
    		{
    			//for(int u : occ)
    			//	System.out.print(u+" ");
    			//System.out.println();
    			
    			assertEquals(c, C.getColorOf(occ.get(0)));
    			assertEquals(c1, C.getColorOf(occ.get(1)));
    			assertEquals(c2, C.getColorOf(occ.get(2)));
    		}
    	}
	}
	*/

	
	@Test
	public void testGnp() throws InterruptedException
	{
    	int n=10000;
    	int k=5;
    	ImmutableGraph G = Transform.symmetrize(GraphGenerator.erdosRenyiGraph(n, n*10) ).immutableView();
    	G = Transform.removeSelfLoops(G);
 	
    	BaseColorCoding C = new ColorCoding(G, k);
    	C.color();
    	C.run(4);
		
    	C.buildStructures();
		
		Random r = new Random();
		int sampled = 0;
		long start = new Date().getTime();
		IColorCodingSampler sampler = C.newSampler(k);
    	for(int i=0; i<1000; i++)
    	{
    		List<Integer> L= sampler.sample();
    		assertEquals(k, L.size() );

    		Graphlet occ = new Graphlet(G, L);
    		ImmutableGraph H = occ.asGraph();
    		
    		//assertEquals(k, new BreadthFirstSearch(H, 0).count().getReachedNodes());
    		
    		long spanning_trees = SpanningTrees.KirchhoffCount(H);
    		//System.out.println(L.toString() + " " + spanning_trees+ " " + H.numArcs());

    		assertTrue(spanning_trees>0);
    		assertTrue(spanning_trees!=1 || H.numArcs()==2*(H.numNodes()-1));
    		
    		if(r.nextDouble()>1.0/spanning_trees)
    			continue;
    		
    		sampled++;
    	}
    	
    	double duration = (new Date().getTime() - start)/1000.0;
    	System.out.println("Sampled " + sampled + " occurrences in " + duration + " seconds ("+ new DecimalFormat("#.##").format(sampled/duration) + "occ/s))");
	}
}
