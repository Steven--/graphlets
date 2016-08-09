package wsdm16.motifs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.NodeIterator;

/** Enumerates all the connected subgraphs of G of size K.
 * Optionally only the subgraphs containing a specific node can be enumerated.
 * Each distinct subgraph is returned exactly one time.
 * 
 * @author anon
 *
 */
public class SubgraphEnumerator
{    
    private class State
    {
    	int node;
    	HashSet<Integer> candidates;
    	Iterator<Integer> candidates_iterator;
    	
    	HashSet<Integer> forbidden = null;
    	HashSet<Integer> motif = null;
    	HashSet<Integer> candidates_next = null;
    	
    	ArrayList<Integer> new_candidates  = new ArrayList<>();
    	ArrayList<Integer> new_forbidden = new ArrayList<>();
    	
    	boolean initialized = false;
    	int num_candidates_processed = 0;
    }
    
    private Stack<State> CallStack = new Stack<>();
    private ImmutableGraph G;
    private int K;
    private boolean single_node;
    private int node;
    private NodeIterator nodeIterator;

    private HashSet<Integer> processed_nodes = new HashSet<>();

    /** Creates an object that enumerates all the connected subgraphs of G of size K
     * 
     * @param G The host graph
     * @param K The size of the returned subgraphs
     */
    public SubgraphEnumerator(ImmutableGraph G, int K)
    {
    	this.G = G;
    	this.K = K;
    	nodeIterator = G.nodeIterator();
    	single_node = false;
    }

    /** Creates an object that enumerates all the connected subgraphs of G of size K containing node u
     * 
     * @param G The host graph
     * @param K The size of the returned subgraphs
     * @param u A vertex that will belong to all returned subgraphs
     */
    public SubgraphEnumerator(ImmutableGraph G, int K, int u)
    {
    	this.G = G;
    	this.K = K;
    	single_node = true;
    	node = u;
	}

    /**
     * 
     * @return The next subgraph of G or null of there are no more subgraphs
     */
    public Graphlet nextSubgraph()
    {
    	if(single_node)
    	{
    		if(processed_nodes.size()==0)
    		{
    	    	processed_nodes.add(node);

    	    	State s = new State();
    	    	s.node = node;
    	    	s.candidates = new HashSet<Integer>();
    	    	s.forbidden = processed_nodes;
    	    	s.motif = new HashSet<Integer>();
    	    	
    	    	CallStack.add(s);
    		}
    		
    		return process_callstack();
    	}
    	else
    	{
    		Graphlet r = null;
    		while(true)
    		{
    			r = process_callstack();
    			if(r!=null)
    				return r;

    			if(!nodeIterator.hasNext())
    				return null;
				
    			int v=nodeIterator.nextInt();
    			
				processed_nodes.add(v);
				
    	    	State s = new State();
    	    	s.node = v;
    	    	s.candidates = new HashSet<Integer>();
    	    	s.forbidden = processed_nodes;
    	    	s.forbidden.add(node);
    	    	s.motif = new HashSet<Integer>();
    	    	
    	    	CallStack.add(s); 
    		}
    	}
    }
    
    private Graphlet process_callstack()
    {
    	while(!CallStack.empty())
    	{
	    	State s = CallStack.peek();
	    	
	    	if(!s.initialized)
	    	{
	    		s.motif.add(s.node);
	    		
	    		if(s.motif.size()==K)
	    		{
	    			CallStack.pop();
	    			Graphlet graphlet = new Graphlet(G, s.motif);
	    			s.motif.remove((Integer)s.node);
	    			
	    			return graphlet;
	    		}
	    		
		    	LazyIntIterator sIt = G.successors(s.node);
		    	for(int d = G.outdegree(s.node); d > 0; d--)
		    	{
		    		int v = sIt.nextInt();
		    		if(!s.forbidden.contains(v) && !s.candidates.contains(v))
		    		{
		    			s.candidates.add(v);
		    			s.new_candidates.add(v);
		    		}
		    	}
	    		s.candidates_iterator = s.candidates.iterator();
		    	s.candidates_next = new HashSet<Integer>(s.candidates);
		    	s.initialized = true;
	    	}
	    	
	    	if(s.num_candidates_processed < s.candidates.size())
	    	{
	    		int v = s.candidates_iterator.next();
	    		s.num_candidates_processed++;
	
	    		s.forbidden.add(v);	 
	    		s.new_forbidden.add(v);
	    		s.candidates_next.remove((Integer)v);
	
	    		State s2 = new State();
	    		s2.motif = s.motif;
	    		s2.candidates = s.candidates_next;
	    		s2.forbidden = s.forbidden;
	    		s2.node=v;
	    		
	    		CallStack.push(s2);
	    	}
	    	else
	    	{
		    	for(int v : s.new_candidates)
		    		s.candidates.remove((Integer)v);
	
		    	for(int v : s.new_forbidden)
		    		s.forbidden.remove((Integer)v);
		    	
		    	s.motif.remove((Integer)s.node);
		    	CallStack.pop();
	    	}
    	}
    	
    	return null;
    }
    
//    public void Enumerate(int u)
//    {
///*    	ArrayList<Integer> candidates = new ArrayList<>();
//    	NodeIterator nIt = G.nodeIterator();
//    	for(int n = G.numNodes(); n > 0; n--)
//    		candidates.add(nIt.nextInt());
//*/   	
//    	
//    	ArrayList<Integer> motif = new ArrayList<>();
//    	ArrayList<Integer> candidates = new ArrayList<>();
//    	ArrayList<Integer> forbidden = new ArrayList<>();
//    	
//    	forbidden.add(u);
//    	doEnumerate(u, motif, candidates, forbidden);
//    }
//
//	private void doEnumerate(int u, ArrayList<Integer> motif, ArrayList<Integer> candidates, ArrayList<Integer> forbidden)
//	{
//		motif.add(u);
//    	
//		if(motif.size()==K)
//		{
//			for(int v : motif)
//			{
//				System.out.print(v);
//				System.out.print(" ");
//			}
//			System.out.println();
//		}
//		else
//		{	
//	    	
//	    	ArrayList<Integer> new_candidates = new ArrayList<>();
//	    	LazyIntIterator sIt = G.successors(u);
//	    	for(int d = G.outdegree(u); d > 0; d--)
//	    	{
//	    		int v = sIt.nextInt();
//	    		if(!motif.contains(v) && !forbidden.contains(v) && !candidates.contains(v))
//	    		{
//	    			candidates.add(v);
//	    			new_candidates.add(v);
//	    		}
//	    	}
//
//	    	ArrayList<Integer> new_forbidden = new ArrayList<>();
//	    	ArrayList<Integer> candidates2 = new ArrayList<>(candidates);
//	    	for(int v : candidates )
//	    	{
//	    		forbidden.add(v);	    		
//	    		new_forbidden.add(v);
//	    		candidates2.remove((Integer)v);
//	    		doEnumerate(v, motif, candidates2, forbidden);
//	    	}
//	    	
//	    	for(int v : new_candidates)
//	    		candidates.remove((Integer)v);
//
//	    	for(int v : new_forbidden)
//	    		forbidden.remove((Integer)v);
//
//		}
//		
//    	motif.remove((Integer)u);    	
//	}
}
