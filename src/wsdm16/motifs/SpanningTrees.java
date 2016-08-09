package wsdm16.motifs;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;

public class SpanningTrees 
{
	/** Computes the number of spanning trees of G using Kirchhoff's theorem
	 * @see: https://en.wikipedia.org/wiki/Kirchhoff%27s_theorem
	 * 
	 * @param G An undirected (reciprocated edges), loop-free graph
	 * @return The number of spanning trees of G
	 */
	public static long KirchhoffCount(ImmutableGraph G)
	{
		int n = G.numNodes();
		
		//The Laplacian matrix of G with the last row/column removed
		RealMatrix L = new Array2DRowRealMatrix(n-1,n-1);
		
		for(int u=0; u<n-1; u++)
		{
			L.setEntry(u, u, G.outdegree(u));
			
			LazyIntIterator it = G.successors(u);
			for(int d = G.outdegree(u); d>0; d--)
			{
				int v = it.nextInt();
				
				if(v!=n-1)
					L.setEntry(u, v, -1);
			}
		}

		//System.err.println(L.toString());
		
		return Math.round(new LUDecomposition(L).getDeterminant());
	}
}
