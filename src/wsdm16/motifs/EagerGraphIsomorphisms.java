package wsdm16.motifs;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

import it.unimi.dsi.webgraph.ImmutableGraph;
import wsdm16.graphutils.BreadthFirstSearch;
import wsdm16.graphutils.PermutationGenerator;

/** Represent the Equivalence Classes of Graph Ismomrphisms
 * 
 * @author steven
 *
 */
public class EagerGraphIsomorphisms extends BaseGraphIsomorphisms
{
	private HashMap<BitSet, BitSet> representant_map = new HashMap<>();
	private HashMap<BitSet, BitSet> equivalence_classes = new HashMap<>();
	private ArrayList<BitSet> representants_list = new ArrayList<>();
	
	
	/**
	 * 
	 * @param n The number of nodes of the graph. Must be >= 2.
	 */
	public EagerGraphIsomorphisms(int n)
	{
		super(n);

		boolean edges[] = new boolean[bits_per_graph]; //Elements of a n x n lower triangular matrix (without the diagonal)
		while(true)
		{
			int i=0;
			while(i<bits_per_graph && edges[i])
			{
				edges[i] = false;
				i++;
			}
			
			if(i==bits_per_graph)
				break;
			
			edges[i]=true;
			
			BitSet b =  new BitSet(bits_per_graph);
			for(i=0; i< bits_per_graph; i++)
				b.set(i, edges[i]);
				
			ImmutableGraph G = fromBitSet(b, 0);

			if(new BreadthFirstSearch(G, 0).count().getReachedNodes()!=n)
				continue;
			
			if(representant_map.containsKey(b))
				continue;
						
			representants_list.add(b);
			BitSet classBS = new BitSet();
			equivalence_classes.put(b, classBS);
			
			PermutationGenerator permGen = new PermutationGenerator(n);
			int idMap[];
			int count=0;
			while (permGen.hasNext())
			{
				idMap = permGen.next();
				BitSet b2 = isomorphism(b, idMap);
				
				representant_map.put(b2, b);
				int pos = count*bits_per_graph;
				for(i=0; i<bits_per_graph; i++)
					classBS.set(pos+i, b2.get(i) );
				
				count++;
			}
		}
	}
			
	protected BitSet getRepresentantBitSet(ImmutableGraph G)
	{
		BitSet b = toBitSet(G);
		return representant_map.get(b);
	}
	
	public int getNumberOfClasses()
	{
		return representants_list.size();
	}
	
	public ImmutableGraph getRepresentantForClass(int i)
	{
		return fromBitSet(representants_list.get(i), 0);
	}
	
/*	public Iterator<ImmutableGraph> getGraphsIsomorphicTo(ImmutableGraph G)
	{
		return equivalence_classes.get(G).iterator();
	}*/
}
