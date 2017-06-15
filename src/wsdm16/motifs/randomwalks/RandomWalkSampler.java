package wsdm16.motifs.randomwalks;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.math3.distribution.GeometricDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import wsdm16.motifs.BaseGraphIsomorphisms;
import wsdm16.motifs.Graphlet;
import wsdm16.motifs.LazyGraphIsomorphisms;
import wsdm16.motifs.MapUtil;
import wsdm16.motifs.randomwalks.FullGraphletRandomWalk.InvalidStartingNodeException;

/**
 * A random-walk based sampler for graphlets in a graph.
 * 
 * The most simple usage is a follows:
 *   java RandomWalkSampler -b GRAPH -k GRAPHLET_SIZE -n SAMPLES -t WALK_LENGTH
 * For instance,
 *   java RandomWalkSampler -b wordassociation-2011 -k 5 -n 100 -t 10000
 *   
 * RandomWalkSampler.java - created on 06 lug 2016
 * 
 * @author anon
 */
public class RandomWalkSampler
{
	public static void main(String[] args)
	{
		String basename = null, basenameT = null;
		String outFile = null; // for storing a walk
		String historyFile = null; // for loading a previous walk
		String sampleFile = null; // for printing out samples
		String saveFinalStateFile = null; // for saving and then restoring the random walk state
		boolean graphletWalk = false; // no self-loops?
		boolean smart = false; // smart random walk i.e. reweight transitions?
		int k = -1;
		int numSamples = -1;
		int numSteps = -1;
		int verboseLevel = 0;
		int timingInterval = 1000; // print a log line each timingInterval graphlets (active with -g)
		int discard = 0;
		int samplingInterval = 0; // print a graphlet each samplingInterval steps
		
		// 1. PARSE THE COMMAND LINE OPTIONS
		Options options = new Options();
		options.addOption("b", true, "basename of the host graph (stored in WebGraph format)");
		options.addOption("k", true, "graphlet size (number of nodes)");
		options.addOption("n", true, "number of samples to take");
		options.addOption("t", true, "number of steps per sample");
		options.addOption("g", false,"no self-loops, each step takes to a new graphlet");
		options.addOption("O", true, "record the walk history to this file");
		options.addOption("S", true, "simulate a walk (including self-loops) from a walk history file");
		options.addOption("d", true, "discard a given number of samples at the beginning");
		options.addOption("i", true, "sampling interval (print out the graphlet each given number of steps)");
		options.addOption("o", true, "save samples (hash codes of graphlets) to this text file");
		options.addOption("s", true, "save the final set of vertices of each walk to this text file");
		options.addOption("m", false, "smart -- reweight transitions to converge faster");
		
		CommandLineParser parser = new PosixParser();
		try
		{
			CommandLine cmd = parser.parse(options, args);
			basename = cmd.hasOption("b") ? cmd.getOptionValue("b") : basename;
			k = cmd.hasOption("k") ? Integer.parseInt(cmd.getOptionValue("k"))
					: k;
			numSamples = cmd.hasOption("n")
					? Integer.parseInt(cmd.getOptionValue("n")) : numSamples;
			numSteps = cmd.hasOption("t") ? Integer.parseInt(cmd.getOptionValue("t"))
					: numSteps;
			basenameT = cmd.hasOption("T") ? cmd.getOptionValue("T")
					: basenameT;
			verboseLevel = cmd.hasOption("v")
					? Integer.parseInt(cmd.getOptionValue("v")) : verboseLevel;
			outFile = cmd.hasOption("O") ? cmd.getOptionValue("O")
					: outFile;
			historyFile = cmd.hasOption("S") ? cmd.getOptionValue("S")
					: historyFile;
			graphletWalk = cmd.hasOption("g");
			discard = cmd.hasOption("d") ? Integer.parseInt(cmd.getOptionValue("d")) : discard;
			samplingInterval = cmd.hasOption("i") ? Integer.parseInt(cmd.getOptionValue("i")) : numSteps;
			sampleFile = cmd.hasOption("o") ? cmd.getOptionValue("o") : sampleFile;
			saveFinalStateFile = cmd.hasOption("s") ? cmd.getOptionValue("s") : saveFinalStateFile;
			smart = cmd.hasOption("m");
		}
		catch (ParseException e)
		{
			System.err.println(e.toString());
		}
		
		if (basename == null)
		{
			HelpFormatter formatter = new HelpFormatter();
			formatter.setWidth(120);
			formatter.printHelp(RandomWalkSampler.class.getSimpleName()
					+ " -b basename [options] ...", options);
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

		//Check that the graph is loop-free and symmetric (edges are reciprocated)
		/*if(!G.equals(wsdm16.graphutils.Transform.removeSelfLoops(Transform.symmetrize(G))))
		{
			System.out.println("Graph has selfloops or has unreciprocated edges.");
			return;
		}*/

		// compute the graph's maximum degree
		int maxDegree = 0;
		IntIterator odegs = G.outdegrees();
		for (int i = G.numNodes() - 1; i >= 0; i--)
			maxDegree = Math.max(maxDegree, odegs.nextInt());

		pl.logger().info("Graph " + basename + " has " + G.numNodes()
				+ " nodes, " + G.numArcs() / 2
				+ " undirected edges, and maximum degree: " + maxDegree);
		pl.logger.info("Sampling motifs of size " + k);

		// 3. SAMPLE
		pl.logger().info("Sampling...");
		BaseGraphIsomorphisms isomorphisms = new LazyGraphIsomorphisms(k);
		long start = new Date().getTime();
		RandomGenerator rnd = new Well19937c(0); // this is for starting from random nodes
		Long2LongOpenHashMap hashCount = new Long2LongOpenHashMap();
		long realSteps = 0;

		/**
		 * Simulation mode: load data from a previous graphlet walk, and use it to do a walk with self-loops
		 */
		if (historyFile != null)
		{ 
			String degHistoryFile = historyFile + ".degs";
			try {
				// read state degree list
				FileInputStream degFis = new FileInputStream(degHistoryFile);
				ObjectInputStream degOis = new ObjectInputStream(degFis);
				
				@SuppressWarnings("unchecked")
				List<Integer> degs = (List<Integer>)degOis.readObject();
				pl.logger().info("Loaded degree history of length " + degs.size());
				
				// prepare to read states i.e. (graphlets)
				FileInputStream stateFis = new FileInputStream(historyFile);
				ObjectInputStream stateOis = new ObjectInputStream(stateFis);
				Set<Integer> vH = null;				
				double ub = 1.0 + Collections.max(degs); // normalization constant for self-loops
				int g = -1; // graphlet step count
				//Number of samples taken.
				//Might be less than numSamples if there is not enough data in the history file
				int realSamples = 0; 
				for (int i = 0; i < numSamples; i++)
				{
					int s = 0; // virtual step count
					
					while (s < numSteps && g < degs.size()-1)  // do the virtual walk!
					{ 
						g++;
						vH = (Set<Integer>) stateOis.readObject();
						s += new GeometricDistribution(rnd, degs.get(g)/ub).sample();
					}
					
					if (g >= degs.size()-1) // we don't have enough data to take the last sample
					{
						pl.logger().warn("Not enough data to take all the requested samples. Requested: "+ numSamples + " Took:" + realSamples);
						break;
					}
					
					realSamples++;
					if (realSamples > discard)
						hashCount.addTo(isomorphisms.long_signature(new Graphlet(G, vH).asGraph()),	1);
				}
				realSteps = g;
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * "Real" random walk -- only graphlets, no self-loops
		 */
		if (graphletWalk && historyFile == null)
		{
			// prepare for output
			FileOutputStream fos = null;
			ObjectOutputStream oos = null;
			if (outFile != null) {
    			try {
					fos = new FileOutputStream(outFile);
					oos = new ObjectOutputStream(fos);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			List<Integer> stateDegs = new ArrayList<>(numSamples * numSteps);
			for (int i = 0; i < numSamples; i++) // random walk!
			{
				int u = rnd.nextInt(G.numNodes());
				FullGraphletRandomWalk randomWalk = null;
				try {
					randomWalk = new FullGraphletRandomWalk(G, k, u, maxDegree, rnd);
				} catch (InvalidStartingNodeException c) {
					continue;
				}
				double startTime = System.nanoTime();
				double lastTime = startTime;
				for (int step = 0; step < numSteps; step++)
				{
					Graphlet H = randomWalk.getGraphlet();
					try {
						oos.writeObject(H.getNodes());
						oos.reset();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					randomWalk.updateSwitchables();
					stateDegs.add(randomWalk.stateDegree());
					// System.out.println(H.getNodes()+ " : " +
					// randomWalk.stateDegree());
					randomWalk.stepReally();
					realSteps++;
					if (((step + 1) % timingInterval) == 0) { // update the log
						double elapsed = (System.nanoTime() - lastTime)/1e9;
						pl.logger().info(step + 1
								+ " graphlets  \t"
								+ String.format("%8.2f", timingInterval / elapsed)
								+ " graphlets/s \t"
								+ String.format("%8.2f", 1e3 * elapsed / timingInterval)
								+ " msec/graphlet");
						lastTime = System.nanoTime();
					}
				}
			}
			if (outFile != null) { // close output files
				try {
					oos.close();
					fos.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			// now write out the state degree list
			try {
				fos = new FileOutputStream(outFile + ".degs");
				oos = new ObjectOutputStream(fos);
				oos.writeObject(stateDegs);
				oos.close();
				fos.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		/**
		 * Ordinary (i.e. with self-loops) random walk, done from scratch
		 */
		if (!graphletWalk && historyFile == null)
		{
	        BufferedWriter writer = null;
			if (sampleFile != null) 
			{
				try {
					writer = new BufferedWriter(new FileWriter(sampleFile));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			BufferedWriter finalStateWriter = null;
			if (saveFinalStateFile != null)
			{
				try
				{
					finalStateWriter = new BufferedWriter(new FileWriter(saveFinalStateFile));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			long virtualSteps=0; //The number of steps taken so far
			double startTime = System.nanoTime()/1e9;
			double lastTime = startTime;
			int i = 0;
			Graphlet H = null;
			while (i < numSamples)
			{
				int u = rnd.nextInt(G.numNodes());
				FullGraphletRandomWalk randomWalk = null;
				try {
					if (smart) {
						randomWalk = new SmartGraphletRandomWalk(G, k, u, 2, rnd);
					}
					else
						randomWalk = new FullGraphletRandomWalk(G, k, u, maxDegree,	rnd);
				} catch (InvalidStartingNodeException c) {
					continue;
				}

				int numIntervals = (int) Math.ceil(1.0 * numSteps / samplingInterval);
				for (int t = 0; t < numIntervals; t++)
				{
					randomWalk.walk(samplingInterval);	
					virtualSteps += samplingInterval;
					H = randomWalk.getGraphlet();
					long sign = isomorphisms.long_signature(H.asGraph());
					if (sampleFile != null) 
						try {
    						writer.write(sign + " ");
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
//					else
//						System.out.print(sign + " ");
				}
				
				if (sampleFile != null) 
					try {
						writer.newLine();
					} catch (IOException e) {
						e.printStackTrace();
					}
//				else
//					System.out.println();
				
				if(saveFinalStateFile != null)
				{
					try
					{
						for(int v : H.getNodes())
							finalStateWriter.write(v + " ");
						finalStateWriter.newLine();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				realSteps += randomWalk.realSteps(); 
				i++;

				hashCount.addTo(isomorphisms.long_signature(H.asGraph()), 1);
				
				//Print speed and ETA
				double currTime =  System.nanoTime()/1e9;
				if(currTime >= lastTime+60*5) //5 minutes
				{
					double elapsed = currTime - startTime;
					double virtualSpeed = elapsed/virtualSteps;
					double secsLeft = (numSamples*numSteps-virtualSteps)*virtualSpeed;
					int d = (int)(secsLeft/86400);
					secsLeft%=86400;
					int h=(int)(secsLeft/3600);
					secsLeft%=3600;
					int m=(int)(secsLeft/60);
					secsLeft%=60;
					int s=(int)(secsLeft);
					
					DecimalFormat fmt3d = new DecimalFormat("#.###");
					DecimalFormat fmtInt = new DecimalFormat("00");
					System.out.println("TIME: " + fmt3d.format(elapsed) + "s. Virtual steps: "
							+ virtualSteps + " (" + fmt3d.format(1000*virtualSpeed)  + "ms/step) Real steps: " 
							+ realSteps +  " (" + fmt3d.format(1000*elapsed/realSteps)  + "ms/step). ETA: "
							+ d+"d "+ h+":"+ fmtInt.format(m)+":"+ fmtInt.format(s));
					
					lastTime = currTime;
				}


			}
			
			if (sampleFile != null)
			{
				try {
					writer.flush();
					writer.close();
					pl.logger.info("Samples written to " + sampleFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if(saveFinalStateFile != null)
			{
				try
				{
					finalStateWriter.flush();
					finalStateWriter.close();
					pl.logger.info("Final states written to " + saveFinalStateFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		
		numSamples = 0;
		for (Long count : hashCount.values())
			numSamples += count;

		pl.logger().info("Done");
		pl.logger().info("Heap used: " + Runtime.getRuntime().totalMemory()
				+ " max: " + Runtime.getRuntime().maxMemory());

		double duration = (new Date().getTime() - start) / 1000.0;
		pl.logger().info("Sampled " + numSamples + " motif occurrences in "
				+ duration + " seconds ("
				+ new DecimalFormat("#.##").format(numSamples / duration)
				+ "occ/s))");
		pl.logger()
				.info(new DecimalFormat("#.##")
						.format(((double) realSteps) / duration)
						+ " states/s, i.e. "
						+ new DecimalFormat("#.###")
								.format(((double) 1e3 * duration / realSteps))
						+ " msec/state");
		pl.logger()
				.info("Average transitions between different graphlets, per sample taken: "
						+ ((double) realSteps) / numSamples);

		/*
		 * System.out.println("Vertex occurrences vector: "); for(int u=0;
		 * u<G.numNodes(); u++) System.out.print(counts[u]+ " ");
		 * System.out.println();
		 */

		if (historyFile != null || !graphletWalk)
		{
    		System.out.println("== SAMPLES FOLLOW ==");
    
    		Map<Long, Long> sortedHashCount = MapUtil.sortByValue(hashCount, true);
    		for (Map.Entry<Long, Long> e : sortedHashCount.entrySet())
    		{
    			System.out.println(e.getKey() + ": " + e.getValue() + " ("
    					+ String.format("%5.2f", e.getValue() * 100.0 / numSamples) + "%)");
    		}

		}
	}

}
