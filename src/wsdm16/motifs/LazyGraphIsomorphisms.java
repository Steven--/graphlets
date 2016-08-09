package wsdm16.motifs;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;

import it.unimi.dsi.webgraph.ImmutableGraph;
import wsdm16.graphutils.PermutationGenerator;

public class LazyGraphIsomorphisms extends BaseGraphIsomorphisms
{
	private ConcurrentHashMap<BitSet, BitSet> representant_map = new ConcurrentHashMap<>();

	public LazyGraphIsomorphisms(int n)
	{
		super(n);
	}
	
	protected BitSet getRepresentantBitSet(ImmutableGraph G)
	{
		BitSet b = toBitSet(G);
	
		BitSet repr = representant_map.getOrDefault(b, null);
		if(repr!=null)
			return repr;
		
		repr=b;
		ArrayList<BitSet> list = new ArrayList<>();
		PermutationGenerator permGen = new PermutationGenerator(n);
		int idMap[];
		while (permGen.hasNext())
		{
			idMap = permGen.next();
			BitSet b2 = isomorphism(b, idMap);
			list.add(b2);
			
			BitSet xor = (BitSet)b2.clone();
			xor.xor(repr);
			int msbDifferent = xor.length()-1;
			if(msbDifferent!=-1 && b2.get(msbDifferent)==false) //b2 is the minimum
				repr = b2;
		}
		
		for(BitSet b2 : list)
			representant_map.putIfAbsent(b2, repr);
		
		return repr;
	}
}