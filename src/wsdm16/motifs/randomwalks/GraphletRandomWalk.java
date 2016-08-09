package wsdm16.motifs.randomwalks;

import org.apache.commons.math3.random.RandomGenerator;

import it.unimi.dsi.webgraph.ImmutableGraph;
import wsdm16.motifs.Graphlet;

/**
 * GraphletRandomWalk.java - created on 06 lug 2016
 * 
 * This is an abstract class representing a random walk over the graphlet space of the graph.
 * Different subclasses may for example operate over different Markov Chains.
 * @author anon
 */
public abstract class GraphletRandomWalk {

    protected ImmutableGraph G;
    protected Graphlet H = null;
    protected int steps = -1;
    protected int k = -1;
    protected RandomGenerator random = null;
    //protected List<Set<Integer>> stateHistory;
    //protected List<Integer> degreeHistory;
    protected int maxDegree = -1;

    /** 
     * Return the current graphlet.
     * @return
     */
    public Graphlet getGraphlet() {
        return H;
    }

    /** 
     * Return the state history.
     * @return The list of sets of graphlets the walk went through. Each graphlet is represented by its nodes in the host graph.
     */
    /*public List<Set<Integer>> stateHistory() {
        return stateHistory;
    }*/

    /** 
     * Return the degree history.
     * @return List of the number of distint graphlets that were reachable at each step of the walk.
     */
    /*public List<Integer> degreeHistory() {
        return degreeHistory;
    }*/

    /** Return the number of steps done so far, i.e. the number of graphlets visited after the initial one.
     * 
     * @return
     */
    public int steps() {
        return steps;
    }
    
    /** Perform one step.
     * 
     * @return false if a step is not possible.
     */
    public abstract boolean step();
    
    /** Perform s steps.
     * 
     * @return the actual number of steps it was possible to perform.
     */
    public abstract int walk(int s);

}
