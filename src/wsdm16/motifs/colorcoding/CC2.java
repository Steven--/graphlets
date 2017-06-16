package wsdm16.motifs.colorcoding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

import it.unimi.dsi.fastutil.objects.Reference2LongMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import wsdm16.motifs.distributions.EnumeratedDistribution;
import wsdm16.motifs.distributions.EnumeratedIntegerDistribution;

//FIXME: Reference2???HashMaps rely on the cache returning the same instance when two treelets are equal
public class CC2 extends BaseColorCoding
{	
	private class ColorCodingSlice 
	{
		private final int sliceSize;
		private AtomicInteger processed;
		private Reference2LongOpenHashMap<SetColoredTreelet>[] counts;
		private EnumeratedIntegerDistribution rootDistribution;
		private EnumeratedDistribution<SetColoredTreelet>[] setColoredTreeletDistribution;


		@SuppressWarnings("unchecked")
		public ColorCodingSlice(int size)
		{
			sliceSize = size;
			processed = new AtomicInteger(0);
			
			counts = (Reference2LongOpenHashMap<SetColoredTreelet>[])new Reference2LongOpenHashMap<?>[G.numNodes()];
			for(int u=G.numNodes()-1; u>=0; u--)
				counts[u] = new Reference2LongOpenHashMap<SetColoredTreelet>(); 			
		}
		
		protected void runStep(ImmutableGraph H)
		{
			int n=H.numNodes();
			while(true)
			{
				int u = processed.getAndIncrement();
				if(u>=n)
					return;
				
				LazyIntIterator it = H.successors(u);
				for(int d=H.outdegree(u); d>0; d--)
				{
					int v = it.nextInt();
					assert(v!=-1);

					combine(u, v);
				}

				normalize(u);
				
				counts[u].trim();
			}
		}
		
		protected void runBase(ImmutableGraph H)
		{		
			int n=H.numNodes();
			while(true)
			{
				int u = processed.getAndIncrement();
				if(u>=n)
					return;
	
				SetColoredTreelet t = cache.getSingletonTreelet(colors[u]);
				counts[u].put(t, 1L);
				counts[u].trim();
			}
		}
		
		private void combine(int u, int v)
		{
			for(int size1=1; size1<sliceSize; size1++)
			{
				int size2 = sliceSize-size1;
				for(Reference2LongMap.Entry<SetColoredTreelet> e1 : slices[size1].counts[u].reference2LongEntrySet() )
				{
					for(Reference2LongMap.Entry<SetColoredTreelet> e2 : slices[size2].counts[v].reference2LongEntrySet() )
					{
						if(e1.getKey().is_mergeable(e2.getKey()))
						{
							SetColoredTreelet t = cache.mergeTreelets(e1.getKey(), e2.getKey());
							counts[u].addTo(t, e1.getLongValue() * e2.getLongValue());
							
							assert(t.getSize()==sliceSize);
						}
					}
				}
			}
		}
		
		private void normalize(int u)
		{
			for(Reference2LongMap.Entry<SetColoredTreelet> e : counts[u].reference2LongEntrySet() )
			{
				assert(e.getKey().getSize()==sliceSize);				
				assert( (e.getLongValue() % e.getKey().num_children_isomorphic_to_other) == 0 );

				e.setValue(e.getLongValue()/e.getKey().num_children_isomorphic_to_other);
			}
		}
		
		private class StepRunnable implements Runnable
		{
			private ImmutableGraph H;
			
			public StepRunnable(ImmutableGraph H)
			{
				this.H = H; 
			}
			
			@Override
			public void run()
			{
				if(sliceSize==1)
					runBase(H);
				else
					runStep(H);
			}	
		}
		
		public void run(int num_threads) throws InterruptedException
		{	
			if(num_threads>1)
			{
				Thread[] threads = new Thread[num_threads];
	
				for(int i=0; i<num_threads; i++)
				{
					//Iterators on G are not thread safe. 
					//the copy() method is thread-safe and will return a lightweight copy of the graph
					//Concurrent access to different copies is safe.
					//Note that by contract copy() is guaranteed to work only if randomAccess() returns true.
					threads[i] = new Thread(new StepRunnable(G.copy()), "cc-slice"+sliceSize+"-thread"+i);
					threads[i].start();
				}
				
				for(Thread t : threads)
					t.join();
			}
			else
			{
				new StepRunnable(G).run();
			}
		}

		
		public void buildDistributions()
		{
			buildColoredTreeletDistribution();
			buildRootDistribution();
		}
		
		
		public void buildRootDistribution()
		{
			double[] pmf = new double[G.numNodes()];
			for(int u=G.numNodes()-1; u>=0; u--)
			{
				for(long x : counts[u].values())
					pmf[u] += x;
			}
			
			rootDistribution = new EnumeratedIntegerDistribution(null, pmf, false);
		}
		
		@SuppressWarnings("unchecked")
		private void buildColoredTreeletDistribution()
		{
			setColoredTreeletDistribution = (EnumeratedDistribution<SetColoredTreelet>[])new EnumeratedDistribution<?>[G.numNodes()];

			for(int u=G.numNodes()-1; u>=0; u--)
			{
				int s=0;
				SetColoredTreelet[] values = new SetColoredTreelet[counts[u].size()];
				double[] pmf = new double[counts[u].size()];
				for(Reference2LongMap.Entry<SetColoredTreelet> e : counts[u].reference2LongEntrySet())
				{
					values[s]=e.getKey();
					pmf[s]=e.getLongValue(); 
					s++;
				}
				
				if(s!=0)
					setColoredTreeletDistribution[u] = new EnumeratedDistribution<>(values, pmf, false);
			}
		}
		
		/** Samples a treelet from the given root u.a.r. Thread safe.
		 * 
		 * @return The treelet.
		 */
		public SetColoredTreelet sampleSetColoredTreeletFromRoot(RandomGenerator random, int u)
		{
			if(setColoredTreeletDistribution[u]==null)
				return null;
			
			return setColoredTreeletDistribution[u].sample(random);
		}
		
		/** Samples a treelet root u.a.r. Thread safe.
		 * 
		 * @return The root.
		 */
		public int sampleRoot(RandomGenerator random)
		{
			return rootDistribution.sample(random);
		}
		
		public void printStats()
		{
			//NOOP
		}
		
		public long numberOfColoredTreeletsFrom(int root, SetColoredTreelet t)
		{
			return counts[root].getLong(t);
		}
		
	}

	private class Sampler implements IColorCodingSampler
	{
		private ImmutableGraph graph;
		private int size;
		private RandomGenerator random;
		
		public Sampler(ImmutableGraph graph, int size)
		{
			this.graph = graph.copy();
			this.size = size;
			this.random = new Well19937c();
		}
		
		@Override
		public ImmutableGraph getGraph() 
		{
			return graph;
		}
		
		private class NeighborTPPair
		{
			public final int neighbor;
			public final TreeletPair<SetColoredTreelet> treeletPair;
			
			public NeighborTPPair(int neighbor, TreeletPair<SetColoredTreelet> treelet)
			{
				this.neighbor = neighbor;
				this.treeletPair = treelet;
			}
		}
		
		public List<Integer> sample()
		{
			int root = slices[size].sampleRoot(random);
			SetColoredTreelet t = slices[size].sampleSetColoredTreeletFromRoot(random, root);
			return sampleFromRoot(root, t);
		}
		
		/** Uniformly samples an occurrence of t from the set of occurrences of t rooted in r
		 * Note: this is not thread safe
		 * 
		 * @param r
		 * @param t
		 * @return The list of vertices of G contained in the occurrence, in preorder w.r.t. t
		 */
		private List<Integer> sampleFromRoot(int r, SetColoredTreelet t)
		{
			if(r<0 ||  slices[t.getSize()].numberOfColoredTreeletsFrom(r, t)<1) 
				return null;
			
			ArrayList<Integer> L = new ArrayList<>(t.getSize());
			do_sampleFromRoot(r, t, L);
			
			assert(L.size()==t.getSize());
			
			return L;
		}
		
		private void do_sampleFromRoot(int root, SetColoredTreelet t, List<Integer> L)
		{
			if(t.getSize()==1)
			{
				L.add(root);
				return;
			}
			
			List<TreeletPair<SetColoredTreelet>> sctps = cache.splitTreelet(t);		
			assert(sctps!=null);
			assert(sctps.size()>0);
			
			int s=0;
			int rootOutdeg = graph.outdegree(root);
			NeighborTPPair[] values = new NeighborTPPair[rootOutdeg*sctps.size()];
			double[] probs = new double[rootOutdeg*sctps.size()];
			for(TreeletPair<SetColoredTreelet> sctp : sctps)
			{
				long nt1 = slices[sctp.t1.getSize()].numberOfColoredTreeletsFrom(root, sctp.t1);
				
				int t2_size = sctp.t2.getSize();
				LazyIntIterator it = graph.successors(root);
				for(int d = rootOutdeg; d>0; d--)
				{
					int u = it.nextInt();
					long nt2 = slices[t2_size].numberOfColoredTreeletsFrom(u, sctp.t2);
					
					values[s] = new NeighborTPPair(u, sctp);
					probs[s] = nt1*nt2;
					s++;
				}
			}
			
			EnumeratedDistribution<NeighborTPPair> dist = new EnumeratedDistribution<>(values, probs, false);
			NeighborTPPair nsctp = dist.sample(random);

			do_sampleFromRoot(root, nsctp.treeletPair.t1, L);
			do_sampleFromRoot(nsctp.neighbor, nsctp.treeletPair.t2, L);
		}

		
	}
	
	private SetColoredTreeletCache cache;
	private ColorCodingSlice slices[];
	
	public CC2(ImmutableGraph G, int k)
	{
		super(G, k);

		cache = new SetColoredTreeletCache(k);
		
		slices = new ColorCodingSlice[k+1];
		for(int i=1; i<=k; i++)
			slices[i] = new ColorCodingSlice(i);
	}

	public void run() throws InterruptedException
	{
		run(1);
	}

	public void run(int no_threads) throws InterruptedException
	{
		for(int j=1; j<=k; j++)
			slices[j].run(no_threads);
	}
	
	public void buildStructures()
	{
		for(int i=1; i<=k; i++)
			slices[i].buildDistributions();
	}

	public void buildStructures(int size)
	{
		slices[size].buildDistributions();
	}

	
	public void printStats()
	{
		for(int i=1; i<=k; i++)
		{
			System.out.println("Stats for slice " + i);
			slices[i].printStats();
			System.out.println();
		}
	}
	
/*	public int sampleRoot(int size)
	{
		return slices[size].sampleRoot();
	}
*/
	
	@Override
	public IColorCodingSampler newSampler(int size) 
	{
		return new Sampler(G, size);
	}
}
