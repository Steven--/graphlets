package wsdm16.motifs.colorcoding;


import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap.FastEntrySet;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import wsdm16.motifs.BaseGraphIsomorphisms;
import wsdm16.motifs.Graphlet;
import wsdm16.motifs.LazyGraphIsomorphisms;
import wsdm16.motifs.SpanningTrees;
import wsdm16.motifs.colorcoding.BaseColorCoding.IColorCodingSampler;

/** An exhaustive based sampler for graphlets in a graph.
 * 
 * The synopsis is: java wsdm16.motifs.ExhaustiveSampler -b GRAPH -k GRAPHLET_SIZE 
 *  
 * RandomWalkSampler.java - created on 06 lug 2016
 * @author anon + anon
 */
public class ColorCodingSampler 
{

	AtomicInteger samplesTaken = new AtomicInteger(0);
	ConcurrentHashMap<Long, Long> spanning_trees;
	BaseGraphIsomorphisms isomorphisms;
	int numSamples = -1;
	int k = -1;

    public static void main(String[] args) throws InterruptedException
    {
    	new ColorCodingSampler().entryPoint(args);
    }
    
    public void entryPoint(String[] args) throws InterruptedException
    {	
	    String basename = null, basenameT = null;
		int numThreads=-1;
		int verboseLevel = 0;
		boolean alon = false;
		// 1. PARSE THE COMMAND LINE OPTIONS
		Options options = new Options();
		options.addOption("a", false, "use Alon's color coding");
		options.addOption("b", true, "basename of the host graph (stored in WebGraph format)");
		options.addOption("k", true, "graphlet size (number of nodes)");
		options.addOption("n", true, "number of samples to take");
		options.addOption("p", true, "number of threads to run in parallel or -1 to use all available cores");

		options.addOption(
			"T",
			true,
			"load transposed graph from this file (otherwise it will be transposed on-line).");
		CommandLineParser parser = new PosixParser();
		try {
		    CommandLine cmd = parser.parse(options, args);
		    basename = cmd.hasOption("b") ? cmd.getOptionValue("b") : basename;
		    k = cmd.hasOption("k") ? Integer.parseInt(cmd.getOptionValue("k")) : k;
		    numThreads = cmd.hasOption("p") ? Integer.parseInt(cmd.getOptionValue("p")) : 1;
		    if(numThreads==-1) numThreads = Runtime.getRuntime().availableProcessors();
		    numSamples = cmd.hasOption("n") ? Integer.parseInt(cmd.getOptionValue("n")) : numSamples;
		    basenameT = cmd.hasOption("T") ? cmd.getOptionValue("T") : basenameT;
		    verboseLevel = cmd.hasOption("v") ? Integer.parseInt(cmd.getOptionValue("v")) : verboseLevel;
		    alon = cmd.hasOption("a");
		} catch (ParseException e) {
		    System.err.println(e.toString());
		}
		if (basename == null) {
		    HelpFormatter formatter = new HelpFormatter();
		    formatter.setWidth(120);
		    formatter.printHelp(ColorCodingSampler.class.getSimpleName() + " -b basename [options] ...", options);
		    System.exit(0);
		}
		
		// 2. BUILD THE GRAPH, OR READ IT FROM FILE
		ProgressLogger pl = new ProgressLogger();
		ImmutableGraph G = null;
		try {
		    G = ImmutableGraph.load(basename, pl);
		    if (verboseLevel > 0)
			pl.updateAndDisplay();
		} catch (IOException e) {
		    e.printStackTrace();
		}

		/*if(!G.equals(wsdm16.graphutils.Transform.removeSelfLoops(Transform.symmetrize(G))))
		{
			System.out.println("Graph has selfloops or has unreciprocated edges.");
			return;
		}*/
		
		// 3. SAMPLE
		isomorphisms = new LazyGraphIsomorphisms(k);
		BaseColorCoding C = null;
		if(alon)
		{
			pl.logger().info("Using Alon's Color Coding");
			C = new AlonColorCoding(G, k);
		}
		else
			C = new ColorCoding(G, k);
		
		pl.logger().info("Graph " + basename + " has " + G.numNodes() + " nodes and " + G.numArcs()/2 + " undirected edges.");
		pl.logger.info("Sampling motifs of size " + k);
		
		pl.logger().info("Heap used: " + Runtime.getRuntime().totalMemory() + " max: "+ Runtime.getRuntime().maxMemory());

		pl.logger().info("Coloring...");

		C.color();
		
		pl.logger().info("Filling tables using "+ numThreads +" threads...");
		
		try
		{
			C.run(numThreads);
		}
		catch (InterruptedException e1)
		{
			e1.printStackTrace();
			System.exit(1);
		}

		pl.logger().info("Heap used: " + Runtime.getRuntime().totalMemory() + " max: "+ Runtime.getRuntime().maxMemory());

		pl.logger().info("Building structures");
		C.buildStructures(k);

		pl.logger().info("Heap used: " + Runtime.getRuntime().totalMemory() + " max: "+ Runtime.getRuntime().maxMemory());

		C.printStats();
		
		pl.logger().info("Sampling...");
		 
		spanning_trees = new ConcurrentHashMap<>(numThreads);
		long start = new Date().getTime();

		List<Thread> threads = new ArrayList<>();
		List<SamplerRunnable> runnables = new ArrayList<>();
		for(int i=0; i<numThreads; i++)
		{
			IColorCodingSampler sampler = C.newSampler(k);

			SamplerRunnable sr = new SamplerRunnable(sampler);
			Thread t = new Thread(sr);
			runnables.add(sr);
			threads.add(t);
			t.start();
		}

		for(Thread t : threads)
			t.join();

		pl.logger().info("Merging results");

		int accepted=0, rejected=0;
		Long2LongOpenHashMap hashCount = new Long2LongOpenHashMap();
		for(SamplerRunnable sr : runnables)
		{
			rejected += sr.getRejected();
			accepted += sr.getAccepted();
			
			for(Long2LongMap.Entry e : sr.getSamples())
				hashCount.addTo(e.getLongKey(), e.getLongValue());
		}

		assert(accepted==numSamples);
		
		double duration = (new Date().getTime() - start)/1000.0;
		
		pl.logger().info("Done");
		pl.logger().info("Heap used: " + Runtime.getRuntime().totalMemory() + " max: "+ Runtime.getRuntime().maxMemory());
		
		pl.logger().info("Sampled " + numSamples + " motif occurrences in " + duration + " seconds ("+ new DecimalFormat("#.##").format(numSamples/duration) + "occ/s))");
		pl.logger().info("Sampled " + (numSamples + rejected) + " treelet occurrences in " + duration + " seconds ("+ new DecimalFormat("#.##").format((numSamples+rejected)/duration) + "occ/s))");
		pl.logger().info("Rejected " + rejected + " treelets (" + new DecimalFormat("#.##").format(100*((double)rejected)/(numSamples+rejected)) + "%)");
		
		System.out.println("== SAMPLES FOLLOW ==");
		for(Map.Entry<Long, Long> e : hashCount.entrySet())
			System.out.println(e.getKey() + ": " + e.getValue() + " ("+ e.getValue()*100.0/numSamples +"%)");

    }
    
    private class SamplerRunnable implements Runnable
    {
    	IColorCodingSampler sampler;
    	private int rejected;
    	private int accepted;
    	private RandomGenerator random;
		private Long2LongOpenHashMap hashCount;
		private ImmutableGraph graph;
		
    	public SamplerRunnable(IColorCodingSampler sampler)
    	{
    		this.sampler = sampler;
    		rejected = 0;
    		random = new Well19937c();
    		hashCount = new Long2LongOpenHashMap();
    		graph = sampler.getGraph();
    	}

    	public int getAccepted() 
    	{
			return accepted;
		}
    	
    	public int getRejected() 
    	{
			return rejected;
		}

    	public FastEntrySet getSamples()
    	{
    		return hashCount.long2LongEntrySet();
    	}
    	
		@Override
		public void run()
		{
			while(true)
			{
				int taken = samplesTaken.getAndIncrement();
				if(taken >= numSamples)
					return;

				//Loop while we generate one sample
				while(true)
				{
		    		List<Integer> L= sampler.sample();
		    		assert(L.size() == k);
	
		    		Graphlet occ = new Graphlet(graph, L);
		    		assert(occ.isConnected());
	
		    		assert(occ.size()==k);
		    
		    		ImmutableGraph H = occ.asGraph();
		    		assert(H.numNodes()==k);
		    		
					long hash = isomorphisms.long_signature(H);
		    		
					
		    		Long boxedSt = spanning_trees.getOrDefault(hash, null);
		    		long st;
		    		if(boxedSt!=null)
		    			st = boxedSt.longValue(); //Unbox
		    		else
		    		{
		        		st = SpanningTrees.KirchhoffCount(H);
		        		assert(st>0);
		        		spanning_trees.putIfAbsent(hash, st);
		    		}
		    		
		    		if(random.nextDouble()<=1.0/st) //Rejection
		    		{    					
					    hashCount.addTo(hash, 1);
					    accepted++;
					    break;
		    		}
		    		
		    		rejected++;
				}
			}
			
		}
    }
}
