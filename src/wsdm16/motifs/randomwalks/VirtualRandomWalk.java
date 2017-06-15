package wsdm16.motifs.randomwalks;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.distribution.GeometricDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;

/**
 * A virtual random walk performed using a sequence of pre-computed graphlet transitions and emulating self-loops.
 * It takes care of loading data from two sequences of files:
 *    each file in the first sequence is a "state" file storing serialized Set<Integer> representing graphlet nodes
 *    each file in the second sequence is a "degree" file storing a List<Integer> of corresponding state degrees
 * The file names are in the form
 * 	  basename.0, basename.1, ...		for the states
 *    basename.0.degs, basename.1.degs, ...			for the degrees
 * 
 * VirtualRandomWalk.java - created on 30 lug 2016
 * @author anon
 */
public class VirtualRandomWalk {
    private Set<Integer> nodes; // the current set of nodes
    private String walkBasename; // basename of the files where to pick the pre-stored walk
    private String degBasename; // basename of the files where to pick the pre-stored walk
    private List<Integer> degs; // the list of degrees
    private int regularDegree; // the degree that all states should have in the chain 
    private int chunkCount;
    private int stateCount;
    private ProgressLogger pl;
	private FileInputStream degFis;
	private ObjectInputStream degOis;
	private FileInputStream stateFis;
	private ObjectInputStream stateOis;
	private RandomGenerator rnd;

	private class DataOverException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
	};
	
	/**
	 * Constructor.
	 * @param basename the basename of the files (e.g. "ljournal-2008" or "walks/mygraph-ABC")
	 * @param G the graph where the graphlets were taken
	 * @param degree the degree of the graphlets in the virtual Markov Chain. This must be no smaller than the maximum degree encountered in the degree files.
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws DataOverException in case the walk gets over the end of the available data files.
	 */
	public VirtualRandomWalk(String basename, ImmutableGraph G, int degree) throws ClassNotFoundException, IOException, DataOverException {
    	chunkCount = 0;
    	walkBasename = basename;
    	regularDegree = degree;
    	pl = new ProgressLogger();
    	initInputStreams();
		rnd = new Well19937c();
    }
    
	/**
	 * Return the name of the current file holding states.
	 * @return
	 */
    public String getCurrentWalkFilename() {
    	return walkBasename + "." + chunkCount;
    }

    /**
     * Return the name of the current file holding state degreess.
     * @return
     */
    public String getCurrentDegFilename() {
    	return degBasename + "." + chunkCount + ".degs";
    }

    /**
     * Init the input streams on the current files. This gets called before reading any new state.
     * @throws DataOverException
     */
    @SuppressWarnings("unchecked")
	private void initInputStreams() throws DataOverException {
		try {
			stateFis = new FileInputStream(getCurrentWalkFilename());
			stateOis = new ObjectInputStream(stateFis);
			pl.logger().info("Loaded " + getCurrentWalkFilename());
			degFis = new FileInputStream(getCurrentDegFilename());
			degOis = new ObjectInputStream(degFis);
			pl.logger().info("Loaded " + getCurrentDegFilename());
			degs = (List<Integer>)degOis.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			pl.logger().info("Failed to load "+ getCurrentWalkFilename() + " or " + getCurrentDegFilename() + ". Assuming data is over, thus exiting.");
			throw new DataOverException();
		}
    }

    /**
     * Close the input streams.
     */
    private void closeInputStreams() {
    	try {
			degOis.close();
	    	degFis.close();
	    	stateOis.close();
	    	stateFis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * Read from file the next state in the walk, throwing an exception if no more states are available.
     * @return
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws DataOverException if the current file is over and no next file is found.
     */
    @SuppressWarnings("unchecked")
    private Set<Integer> readNextState() throws ClassNotFoundException, IOException, DataOverException {
    	if (stateOis.available() == 0) {
    		closeInputStreams();
    		chunkCount++;
    		initInputStreams();
    	}
		return (Set<Integer>) stateOis.readObject();
    }
    
    /**
     * Return the current state.
     * @return
     */
    public Set<Integer> getState() {
    	return nodes;
    }
    
    /**
     * Perform a given number of steps in the virtual walk, throwing an exception if not enough data was available.
     * @param steps
     * @return
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws DataOverException if the walk need more data, but the current file is over and no next file is found.
     */
    public int walk(int steps) throws ClassNotFoundException, IOException, DataOverException {
		int s = 0;
		while (s < steps) {
			nodes = readNextState();
			s += new GeometricDistribution(rnd, degs.get(stateCount)/regularDegree).sample();
		}
		return steps < s ? steps : s;
    }
    
}
