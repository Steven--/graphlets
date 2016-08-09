package wsdm16.motifs.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import it.unimi.dsi.webgraph.ArrayListMutableGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import wsdm16.graphutils.GraphGenerator;
import wsdm16.graphutils.Transform;
import wsdm16.motifs.Graphlet;
import wsdm16.motifs.SubgraphEnumerator;

public class TestSubgraphEnumerator {

    @Test
    public void test_clique() 
    {
    	int n=50;
    	ImmutableGraph G = ArrayListMutableGraph.newCompleteGraph(n, false).immutableView();
    	
    	for(int k=2; k<=4; k++)
    	{
	    	SubgraphEnumerator enumerator = new SubgraphEnumerator(G, k);
	    	
	    	int count = count(enumerator);

        	int expected = 1;
        	for(int j=G.numNodes(); j>n-k; j--)
        		expected *= j;
        	for(int j=2; j<=k; j++)
        		expected /= j;
        	
	    	assertEquals(expected, count);
    	}
    }

    @Test
    public void test_cycle() 
    {
    	int n=100;
    	
    	ImmutableGraph G = Transform.symmetrize(GraphGenerator.circleGraph(n)).immutableView();
    	
    	for(int k=1; k<=n; k++)
    	{        	        	
	    	SubgraphEnumerator enumerator = new SubgraphEnumerator(G, k);
	    	
	    	int count = count(enumerator);
	    	
	    	if(k==101)
	    		assertEquals(0, count);
	    	else if(k==100)
	    		assertEquals(1, count);
	    	else
	    		assertEquals(n, count);
    	}
    }
    
    @Test
    public void test_star() 
    {
    	int n=100;
    	ImmutableGraph G = Transform.symmetrize(GraphGenerator.starGraph(n) ).immutableView();
    	
    	for(int k=1; k<=4; k++)
    	{        	        	
	    	SubgraphEnumerator enumerator = new SubgraphEnumerator(G, k, 0);
	    	
	    	int count = count(enumerator);

        	int expected = 1;
        	for(int j=G.numNodes()-1; j>n-k; j--)
        		expected *= j;
        	for(int j=2; j<=k-1; j++)
        		expected /= j;
	        	
    		assertEquals(expected, count);
    	}

    	for(int k=1; k<=4; k++)
    	{        	        	
	    	SubgraphEnumerator enumerator = new SubgraphEnumerator(G, k, 1);
	    	int count = count(enumerator);

        	int expected = 1;
        	for(int j=G.numNodes()-2; j>n-k; j--)
        		expected *= j;
        	for(int j=2; j<=k-2; j++)
        		expected /= j;
	        	
    		assertEquals(expected, count);
    	}

    	for(int k=1; k<=4; k++)
    	{        	        	
	    	SubgraphEnumerator enumerator = new SubgraphEnumerator(G, k);
	    	int count = count(enumerator);

        	int expected = 100;
        	
        	if(k!=1)
        	{
        		expected=1;
	        	for(int j=G.numNodes()-1; j>n-k; j--)
	        		expected *= j;
	        	for(int j=2; j<=k-1; j++)
	        		expected /= j;
        	}	
    		assertEquals(expected, count);
    	}
    
    }

	private int count(SubgraphEnumerator enumerator) {
		int count=0;
		while(true)
		{    		
			Graphlet H = enumerator.nextSubgraph();
			if(H==null)
				break;

			assertTrue(H.isConnected());
			count++;    		
		}
		return count;
	}
    
}
