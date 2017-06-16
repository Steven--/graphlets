package wsdm16.motifs.colorcoding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.random.BitsStreamGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

import it.unimi.dsi.fastutil.objects.Reference2LongMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import wsdm16.motifs.distributions.EnumeratedDistribution;
import wsdm16.motifs.distributions.EnumeratedIntegerDistribution;

//FIXME: Reference2???HashMaps rely on the cache returning the same instance when two treelets are equal
public class ColoredTreeletColorCoding extends BaseColorCoding 
{	
	private class ColorCodingSlice 
	{
		private final int sliceSize;
		private AtomicInteger processed;
		private Reference2LongOpenHashMap<ColoredTreelet>[] counts;
		
		private EnumeratedIntegerDistribution rootDistribution;
		private EnumeratedDistribution<ColoredTreelet>[] coloredTreeletDistribution;
		
		public ColorCodingSlice(int size)
		{
			sliceSize = size;
			processed = new AtomicInteger(0);
			
			counts = (Reference2LongOpenHashMap<ColoredTreelet>[])new Reference2LongOpenHashMap<?>[G.numNodes()];
			for(int u=G.numNodes()-1; u>=0; u--)
				counts[u] = new Reference2LongOpenHashMap<ColoredTreelet>(); 			
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
					combine(u, v);
					
					assert(v!=-1);
				}
				
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
	
				ColoredTreelet t = cache.getSingletonTreelet(colors[u]);
				counts[u].put(t, 1L);
				
				counts[u].trim();
			}
		}
		
		private void combine(int u, int v)
		{
			for(int size1=1; size1<sliceSize; size1++)
			{
				int size2 = sliceSize-size1;
				for(Reference2LongMap.Entry<ColoredTreelet> e1 : slices[size1].counts[u].reference2LongEntrySet() )
				{
					for(Reference2LongMap.Entry<ColoredTreelet> e2 : slices[size2].counts[v].reference2LongEntrySet() )
					{
						if(e1.getKey().is_mergeable(e2.getKey()))
						{
							ColoredTreelet t = cache.mergeTreelets(e1.getKey(), e2.getKey());
							counts[u].addTo(t, e1.getLongValue() * e2.getLongValue());
							
							assert(t.getSize()==sliceSize);
						}
					}
				}
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
			//buildTreeletDistribution();
			buildColoredTreeletDistribution();
			buildRootDistribution();
		}
		
		private void buildRootDistribution()
		{
			double[] pmf = new double[G.numNodes()];
			for(int u=G.numNodes()-1; u>=0; u--)
			{
				for(long x : counts[u].values())
					pmf[u] += x;
			}
			
			rootDistribution = new EnumeratedIntegerDistribution(null, pmf, false);
		}
		
		/* Samples a root with a probability proportional to the number of colored treelet rooted in it.
		 * Thread safe.
		 */
		public int sampleRoot(RandomGenerator random)
		{
			return rootDistribution.sample(random);
		}

		private void buildColoredTreeletDistribution()
		{
			coloredTreeletDistribution = (EnumeratedDistribution<ColoredTreelet>[])new EnumeratedDistribution<?>[G.numNodes()];
			//List<DistributionValue<C2Treelet>> pmf = new ReferenceArrayList<>();
			for(int u=G.numNodes()-1; u>=0; u--)
			{
				int s=0;
				ColoredTreelet[] values = new ColoredTreelet[counts[u].size()];
				//DistributionValue<C2Treelet>[] pmf = (DistributionValue<C2Treelet>[])new DistributionValue<?>[counts[u].size()];
				double[] pmf = new double[counts[u].size()];
				for(Reference2LongMap.Entry<ColoredTreelet> e : counts[u].reference2LongEntrySet())
				{
					values[s]=e.getKey();
					pmf[s]=e.getLongValue(); //new DistributionValue<C2Treelet>(e.getKey(), e.getLongValue());
					s++;
				}
				
				if(s!=0)
					coloredTreeletDistribution[u] = new EnumeratedDistribution<>(values, pmf, false);
			}
		}
		
		/* Samples a a treelet rooted in u u.a.r.
		 * Thread safe.
		 */
		public ColoredTreelet sampleColoredTreeletFromRoot(RandomGenerator random, int u)
		{
			if(coloredTreeletDistribution[u]==null)
				return null;
			
			return coloredTreeletDistribution[u].sample(random);
		}
		
		/** Returns the number of occurrences of t from root.
		 *  Thread safe. 
		 */
		public long numberOfColoredTreeletsFrom(int root, ColoredTreelet t)
		{
			return counts[root].getLong(t);
		}
		
		public void printStats()
		{
			long max_treelet_types_from_root = 0;
			long max_treelets_from_root = 0;
			long max_treelet_count = 0;
			long total_num_treelets = 0;
			
			for(int u=G.numNodes()-1; u>=0; u--)
			{
				int size = counts[u].size();
				
				if(max_treelet_types_from_root<size)
					max_treelet_types_from_root = size;

				long sum = 0;
				for(long s : counts[u].values())
				{
					if(max_treelet_count<s)
						max_treelet_count=s;
					
					sum += s;
				}
				
				if(max_treelets_from_root<size)
					max_treelets_from_root = size;
				
				total_num_treelets += sum;
			}
			
			System.out.println("Maximum number of treelet types from a single root: " + max_treelet_types_from_root);
			System.out.println("Maximum number of treelets rooted in a single vertex: " + max_treelets_from_root);
			System.out.println("Maximum number of occurrences of a rooted treelet: " + max_treelet_count);	
			System.out.println("Overall number of counted treelets: " + total_num_treelets);
		}

	}
	
	private class Sampler implements IColorCodingSampler
	{
		private ImmutableGraph graph;
		private int size;
		private BitsStreamGenerator random;
		
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
		
		public List<Integer> sample()
		{
			int root = slices[size].sampleRoot(random);
			ColoredTreelet t = slices[size].sampleColoredTreeletFromRoot(random, root);
			return sampleFromRoot(root, t);
		}
		
		/** Uniformly samples an occurrence of t from the set of occurrences of t rooted in r
		 * Note: this is not thread safe
		 * 
		 * @param r
		 * @param t
		 * @return The list of vertices of G contained in the occurrence, in preorder w.r.t. t
		 */
		private List<Integer> sampleFromRoot(int r, ColoredTreelet t)
		{
			if(r<0 ||  slices[t.getSize()].numberOfColoredTreeletsFrom(r, t)<1) // counts[r].getOrDefault(t, 0)<1)
				return null;
			
			ArrayList<Integer> L = new ArrayList<>(t.getSize());
			do_sampleFromRoot(r, t, L);
			
			assert(L.size()==t.getSize());
			
			return L;
		}
		
		//FIXME: Linear scan or binary search?
		private void do_sampleFromRoot(int r, ColoredTreelet t, List<Integer> L)
		{
			int size = t.getSize();
			if(size==1)
			{
				assert(!L.contains(r));
				L.add(r);
				return;
			}
			
			TreeletPair<ColoredTreelet> split = cache.splitTreelet(t);

			//THIS SHOULD NEVER HAPPEN
			//if(split==null)
			//	return;
			assert(split!=null);
			assert(split.t1.is_mergeable(split.t2));
			
			int t2_size = split.t2.getSize();
			//int[] values = new int[G.outdegree(r)];
			double[] probs = new double[graph.outdegree(r)];
			long sum = 0;
			int d = graph.outdegree(r);
			LazyIntIterator it = graph.successors(r);
			for(int i=0; i<d; i++)
			{
				int v = it.nextInt();
				//values[i] = v;
				long c = slices[t2_size].numberOfColoredTreeletsFrom(v, split.t2);// counts[v].getOrDefault(split.t2, 0);
				probs[i] = c;
				sum += c;
			}

			assert(sum>0);
			
			long rnd = random.nextLong(sum); //the last value is exclusive
			it = graph.successors(r);
			int r2=-1;
			for(int i=0; i<d; i++)
			{
				r2 = it.nextInt();
				if(rnd<probs[i])
					break;
				else
					rnd-=probs[i];
			}
			
			assert(r2!=-1);
			
			/*
			for(int d=graph.outdegree(r)-1; d>=0; d--)
				probs[d]/=sum;
		
			EnumeratedIntegerDistribution dist = new EnumeratedIntegerDistribution(values, probs);
			int r2 = dist.sample();*/
			
			do_sampleFromRoot(r, split.t1, L);
			do_sampleFromRoot(r2, split.t2, L);		
		}
	}
	
	private ColoredTreeletCache cache;
	private ColorCodingSlice slices[];
	
	public ColoredTreeletColorCoding(ImmutableGraph G, int k)
	{
		super(G, k);
		
		cache = new ColoredTreeletCache(k);
		
		slices = new ColorCodingSlice[k+1];
		for(int i=1; i<=k; i++)
			slices[i] = new ColorCodingSlice(i);
	}

	@Override
	public void run() throws InterruptedException
	{
		run(1);
	}

	@Override
	public void run(int no_threads) throws InterruptedException
	{
		for(int j=1; j<=k; j++)
			slices[j].run(no_threads);
	}
	
	@Override
	public void buildStructures()
	{
		for(int i=1; i<=k; i++)
			slices[i].buildDistributions();
	}

	@Override
	public void buildStructures(int size)
	{
		slices[size].buildDistributions();
	}

	
	@Override
	public void printStats()
	{
		for(int i=1; i<=k; i++)
		{
			System.out.println("Stats for slice " + i);
			slices[i].printStats();
			System.out.println();
		}
	}

	@Override
	public IColorCodingSampler newSampler(int size)
	{
		return new Sampler(G, size);
	}
}
