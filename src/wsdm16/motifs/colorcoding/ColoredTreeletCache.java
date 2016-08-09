package wsdm16.motifs.colorcoding;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ColoredTreeletCache
{	
	private Map<TreeletPair<ColoredTreelet>, ColoredTreelet> merges = new ConcurrentHashMap<>();
	private Map<ColoredTreelet, TreeletPair<ColoredTreelet>> splits = new ConcurrentHashMap<>();
	protected BaseTreelet[] singletons;

	public ColoredTreeletCache(int numColors)
	{
		singletons = new ColoredTreelet[numColors];
		for(int i=0; i<numColors; i++)
			singletons[i] = new ColoredTreelet(i);
	}

	public ColoredTreelet getSingletonTreelet(int c)
	{
		return (ColoredTreelet)singletons[c];
	}
	
	public ColoredTreelet mergeTreelets(ColoredTreelet t1, ColoredTreelet t2) 
	{
		TreeletPair<ColoredTreelet> ctp = new TreeletPair<ColoredTreelet>(t1, t2);
	
		ColoredTreelet t = merges.getOrDefault(ctp, null);
		if(t==null)
		{
			t = (ColoredTreelet)t1.merge(t2);
			ColoredTreelet old = merges.putIfAbsent(ctp, t);
			if(old==null)
				splits.put(t, ctp);
			else
				t=old;
		}
		
		return t;
	}
	
	//FIXME?: This assumes the two treelets have been merged with mergeTreelets
	public TreeletPair<ColoredTreelet> splitTreelet(ColoredTreelet t)
	{
		return splits.getOrDefault(t, null); 
	}
}
