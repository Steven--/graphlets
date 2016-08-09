package wsdm16.motifs;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.Transform;

/** An exhaustive based sampler for graphlets in a graph.
 * 
 * The synopsis is: java wsdm16.motifs.ExhaustiveSampler -b GRAPH -k GRAPHLET_SIZE 
 *  
 * RandomWalkSampler.java - created on 06 lug 2016
 * @author anon + anon
 */
public class ExhaustiveSampler {

    public static void main(String[] args)
    {	
	    String basename = null;
		int k=-1;
		int verboseLevel = 0;
		// 1. PARSE THE COMMAND LINE OPTIONS
		Options options = new Options();
		options.addOption("b", true, "basename of the host graph (stored in WebGraph format)");
		options.addOption("k", true, "graphlet size (number of nodes)");
		CommandLineParser parser = new PosixParser();
		try {
		    CommandLine cmd = parser.parse(options, args);
		    basename = cmd.hasOption("b") ? cmd.getOptionValue("b") : basename;
		    k = cmd.hasOption("k") ? Integer.parseInt(cmd.getOptionValue("k")) : k;
		    verboseLevel = cmd.hasOption("v") ? Integer.parseInt(cmd.getOptionValue("v")) : verboseLevel;
		} catch (ParseException e) {
		    System.err.println(e.toString());
		}
		if (basename == null) {
		    HelpFormatter formatter = new HelpFormatter();
		    formatter.setWidth(120);
		    formatter.printHelp(ExhaustiveSampler.class.getSimpleName() + " -b basename [options] ...", options);
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

		if(!G.equals(wsdm16.graphutils.Transform.removeSelfLoops(Transform.symmetrize(G))))
		{
			System.out.println("Graph has selfloops or has unreciprocated edges.");
			return;
		}		

		pl.logger().info("Graph " + basename + " has " + G.numNodes() + " nodes and" + G.numArcs()/2 + " undirected edges.");
		pl.logger().info("Sampling...");

		//pl.logger().info("Precomputing isomorphisms...");
		BaseGraphIsomorphisms isomorphisms = new LazyGraphIsomorphisms(k); //new EagerGraphIsomorphisms(k);

		// 3. SAMPLE
		
		Long2LongOpenHashMap hashCount = new Long2LongOpenHashMap();
		SubgraphEnumerator enumerator = new SubgraphEnumerator(G, k);
		double count=0;
		while(true) 
		{
			Graphlet H = enumerator.nextSubgraph();
			
			if(H==null)
				break;
			
			long hash = isomorphisms.long_signature(H.asGraph());
		    hashCount.addTo(hash, 1);
		    count+=1;
		}

		pl.logger().info("Done");

		System.out.println("== SAMPLES FOLLOW ==");

		for(Map.Entry<Long, Long> e : hashCount.entrySet())
			System.out.println(e.getKey() + ": " + e.getValue() + " ("+ e.getValue()*100.0/count +"%)");

    }
}
