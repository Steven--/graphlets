package wsdm16.motifs;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.Transform;
import it.unimi.dsi.webgraph.algo.ConnectedComponents;

public class GraphCleanup {

	public static void main(String[] args)
	{
		String basename = null;
		boolean largestCC = false;
		String outBasename = null;
		Options options = new Options();
		options.addOption("b", true, "basename of the host graph (stored in WebGraph format)");
		options.addOption("c", false, "only save the largest connected component of the graph");
		options.addOption("O", true, "output graph file basename");

		CommandLineParser parser = new PosixParser();

		try 
		{
		    CommandLine cmd = parser.parse(options, args);
		    basename = cmd.hasOption("b") ? cmd.getOptionValue("b") : basename;
		    largestCC = cmd.hasOption("c");	
		    outBasename = cmd.hasOption("O") ? cmd.getOptionValue("O") : basename + "-clean";
		}
		catch (ParseException e)
		{
			e.printStackTrace();
			return;
		}

		if (basename == null)
		{
		    HelpFormatter formatter = new HelpFormatter();
		    formatter.setWidth(120);
		    formatter.printHelp(GraphCleanup.class.getSimpleName() + " -b basename [options] ...", options);
		    System.exit(0);
		}

		ProgressLogger pl = new ProgressLogger();
		ImmutableGraph G = null;
		try
		{
		    G = ImmutableGraph.load(basename, pl);
			pl.updateAndDisplay();
		}
		catch (IOException e)
		{
		    e.printStackTrace();
		    return;
		}
		
	    pl.logger().info("Graph loaded.");
	    
	    G = Transform.symmetrize(G);
	    G = wsdm16.graphutils.Transform.removeSelfLoops(G);
	    
	    if(largestCC)
	    {
	    	pl.logger().info("Saving only the largest connected component.");
	    	G = ConnectedComponents.getLargestComponent(G, 0, pl);
	    }

	    pl.logger().info("Writing graph. Basename: " + outBasename);
	    try
	    {
	    	//Store the graph with all the default values except for maxRefcount 
	    	//which is set to 1 to slightly improve performance
	    	//See: http://law.di.unimi.it/tutorial.php
			BVGraph.store(G, outBasename, -1, 1, -1, -1, 0, pl);
		} catch (IOException e)
		{
			e.printStackTrace();
			return;
		}
	    
	    pl.logger().info("Done.");
	}

}
