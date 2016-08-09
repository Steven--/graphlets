package wsdm16.motifs.colorcoding;

import java.util.List;
import java.util.Random;

import it.unimi.dsi.webgraph.ImmutableGraph;

public abstract class BaseColorCoding {

	protected ImmutableGraph G;

	public abstract IColorCodingSampler newSampler(int size);

	public abstract void printStats();

	public abstract void buildStructures(int size);

	public abstract void buildStructures();

	public abstract void run(int no_threads) throws InterruptedException;

	public abstract void run() throws InterruptedException;

	protected int k;
	protected int colors[];

	public BaseColorCoding(ImmutableGraph G, int k)
	{
		if(!G.randomAccess())
			throw new IllegalArgumentException("G must allow random access");
			
		this.G = G;
		this.k = k;
		
		colors = new int[G.numNodes()];
	}

	/** Colors the vertices of G with the colors {0, 1, ..., k-1}
	 * 
	 */
	public void color()
	{
		Random r = new Random(); //FIXME: Quality?
		
		for(int i=G.numNodes()-1; i>=0; i--)
			colors[i] = r.nextInt(k);
	}

	/**
	 * 
	 * @param u A vertex
	 * @return the color of vertex u
	 */
	public int getColorOf(int u) {
		return colors[u];
	}
	
	
	public interface IColorCodingSampler
	{
		public List<Integer> sample();
		public ImmutableGraph getGraph();
	}

}