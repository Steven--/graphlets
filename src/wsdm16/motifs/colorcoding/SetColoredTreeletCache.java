package wsdm16.motifs.colorcoding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class SetColoredTreeletCache 
{
	private Map<TreeletPair<SetColoredTreelet>, SetColoredTreelet> merges = new ConcurrentHashMap<>();
	private Map<SetColoredTreelet, List< TreeletPair<SetColoredTreelet> > > splits = new ConcurrentHashMap<>();
	private SetColoredTreelet[] singletons;
	
	private Map<SetColoredTreelet, SetColoredTreelet> equivalences = new ConcurrentHashMap<SetColoredTreelet, SetColoredTreelet>();
	
	public SetColoredTreeletCache(int numColors)
	{
		singletons = new SetColoredTreelet[numColors];
		for(int i=0; i<numColors; i++)
			singletons[i] = new SetColoredTreelet(i);
	}

	public SetColoredTreelet getSingletonTreelet(int c)
	{
		return singletons[c];
	}
	
	public SetColoredTreelet mergeTreelets(SetColoredTreelet t1, SetColoredTreelet t2)
	{
		TreeletPair<SetColoredTreelet> ctp = new TreeletPair<>(t1, t2);
		SetColoredTreelet t = merges.getOrDefault(ctp, null);

		if(t!=null)
			return t;
		
		t = (SetColoredTreelet)t1.merge(t2);
		
		SetColoredTreelet eq = equivalences.putIfAbsent(t, t);
		if(eq==null) //t was not in equivalences, and has just been inserted
			eq=t;
		
		SetColoredTreelet old = merges.putIfAbsent(ctp, eq);
		if(old==null) //it's our job to add ctp to the list of splits
		{
			splits.compute(eq, new BiFunction<SetColoredTreelet, List<TreeletPair<SetColoredTreelet>>, List<TreeletPair<SetColoredTreelet>>>() {

				@Override
				public List<TreeletPair<SetColoredTreelet>> apply(SetColoredTreelet t, List<TreeletPair<SetColoredTreelet>> L)
				{
					if(L==null)
						L = new ArrayList<>();
					
					L.add(ctp);
					return L;
				}
			});
		}
		
		return eq;
	}
	
/*	public synchronized SetColoredTreelet mergeTreelets(SetColoredTreelet t1, SetColoredTreelet t2) 
	{
		TreeletPair<SetColoredTreelet> ctp = new TreeletPair<>(t1, t2);
	
		SetColoredTreelet t = merges.getOrDefault(ctp, null);
		if(t==null)
		{
			t = (SetColoredTreelet)t1.merge(t2);
			t = equivalences.getOrDefault(t, t);
			equivalences.putIfAbsent(t, t);
			
			SetColoredTreelet old = merges.putIfAbsent(ctp, t);
			
			//merges.computeIfAbsent(ctp, k -> (SetColoredTreelet)k.t1.merge(k.t2) );
			
			if(old==null)
			{
				splits.compute(t, new BiFunction<SetColoredTreelet, List<TreeletPair<SetColoredTreelet>>, List<TreeletPair<SetColoredTreelet>>>()
				{
					@Override
					public List<TreeletPair<SetColoredTreelet>> apply(SetColoredTreelet t, List<TreeletPair<SetColoredTreelet>> v )
					{
						if(v==null)
							v=new ArrayList<>();
	
						v.add(ctp);
						return v;
					}
				});
			}
			else
				t=old;
		}
		
		return t;
	}
	*/
	
	//FIXME?: This assumes the two treelets have been merged with mergeTreelets
	public List<TreeletPair<SetColoredTreelet>> splitTreelet(SetColoredTreelet t)
	{
		return splits.getOrDefault(t, null); 
	}

}
