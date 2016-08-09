package wsdm16.motifs;

import java.util.Base64;
import java.util.BitSet;

import it.unimi.dsi.webgraph.ArrayListMutableGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;

public abstract class BaseGraphIsomorphisms
{


	protected int n;
	protected int bits_per_graph;

	protected BaseGraphIsomorphisms(int n)
	{
		this.n = n;
		bits_per_graph = n*(n-1)/2; 
	}

	/** Returns the representant for G. The implementation must be thread safe.
	 *  
	 * @param G
	 * @return
	 */
	protected abstract BitSet getRepresentantBitSet(ImmutableGraph G);
	
	protected ImmutableGraph getRepresentant(ImmutableGraph G)
	{
		return fromBitSet(getRepresentantBitSet(G), 0);
	}
	
	protected BitSet toBitSet(ImmutableGraph G)
	{
		assert(G.numNodes()==n);
		
		BitSet b = new BitSet(bits_per_graph);
		for(int u=0; u<n; u++)
		{
			LazyIntIterator it = G.successors(u);
	    	for(int d = G.outdegree(u); d > 0; d--)
	    	{
	    		int v = it.nextInt();
	    		if(v>=u)
	    			continue;
	    		
	    		int idx = u*(u-1)/2 + v;
	    		b.set(idx);
	    	}
		}
		
		return b;
	}
	
	protected ImmutableGraph fromBitSet(BitSet b, int start)
	{
		ArrayListMutableGraph G = new ArrayListMutableGraph(n);

		for(int u=0; u<n; u++)
		{
			for(int v=0; v<u; v++)
			{
				int idx = start + u*(u-1)/2 + v;
				if(b.get(idx))
				{
					G.addArc(u, v);
					G.addArc(v, u);
				}
			}
		}
		
		return G.immutableView();
	}

	protected BitSet isomorphism(BitSet b, int[] idMap)
	{
		BitSet b2 = new BitSet(bits_per_graph);
		for(int u=0; u<n; u++)
		{
			for(int v=0; v<u; v++)
			{
				int idx = u*(u-1)/2 + v;
				int u2 = (idMap[u]>idMap[v])?idMap[u]:idMap[v];
				int v2 = (idMap[u]>idMap[v])?idMap[v]:idMap[u];

				int idx2 = u2*(u2-1)/2 + v2;
				
				b2.set(idx2, b.get(idx));
			}
		}
		return b2;
	}

	private Base64.Encoder encoder = Base64.getEncoder();

	/** Returns a long signature of an undirected loop-free graph G.
	 * This is a base-64 encoded string of the representation described in "long_signature()"
	 * This signature uniquely identifies G.
	 * 
	 */
	public String string_signature(ImmutableGraph G)
	{
		BitSet b = getRepresentantBitSet(G);
		
		return encoder.encodeToString(b.toByteArray());
	}
	
	/** Returns a long signature of an undirected loop-free graph G. The bits from the least
	 * significant to the most significant correspond to the elements of the lower 
	 * triangular adjacency matrix of G (excluding the diagonal itself).
	 * Since n*(n-1)/2 bits are needed to store a n-vertex graph in this way,
	 * the signature is unique for graphs having up to 11 vertices.
	 * Thread safe.
	 * 
	 * @param G
	 * @return The graph signature
	 */
	public long long_signature(ImmutableGraph G)
	{
		if(bits_per_graph>64)
			throw new UnsupportedOperationException("Signature does not fit in 64 bits");
		
		BitSet b = getRepresentantBitSet(G);
		long[] sig = b.toLongArray();

		return (sig.length!=0)?sig[0]:0;
	}
}