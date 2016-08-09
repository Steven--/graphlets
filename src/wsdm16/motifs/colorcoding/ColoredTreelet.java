package wsdm16.motifs.colorcoding;

/** Represents a colored treelet. Can be used for treelets of up to 32 nodes/colors.
 * 
 * @author anon
 *
 */
public class ColoredTreelet extends BaseTreelet
{
	private int size;
	private int root_color;

	private int colors;
	
	private ColoredTreelet master;
	private ColoredTreelet other;
	
	private boolean has_hash = false;
	private int hash;
	
	/** Constructs a singleton treelet where the root has the given color
	 * 
	 * @param color
	 */
	public ColoredTreelet(int color)
	{
		this.size = 1;
		this.root_color=color;
		
		colors = (1 << color);
		master = null;
		other = null;
	}

	/** Constructs a the colored treelet resulting from merging t1 with t2.
	 * The colored treelets t1 and t2 must be mergeable but this is not checked.
	 * 
	 * @param t1
	 * @param t2
	 */
	
	private ColoredTreelet(ColoredTreelet t1, ColoredTreelet t2)
	{
		size=t1.size+t2.size;
		
		master = t1;
		other = t2;
		
		root_color = t1.root_color;
		colors = t1.colors | t2.colors;
	}

	

	
	/** Checks if this is mergeable with t2 
	 * 
	 * @param t2
	 * @return true iff this treelet and t2 have disjoint color sets and the root of t2 has a bigger color than all the children of the root of this
	 */
	public boolean is_mergeable(BaseTreelet t2)
	{		
		if(ColoredTreelet.class != t2.getClass())
			throw new UnsupportedOperationException();
	
		ColoredTreelet ct2 = (ColoredTreelet)t2;
		
		return  ((colors & ct2.colors)==0) && (other==null || other.root_color < ct2.root_color);
	}
	
	/**
	 * @param other
	 * @return The colored treelet resulting form merging this with t2
	 */
	public BaseTreelet merge(BaseTreelet t2)
	{
		if(!is_mergeable(t2))
			throw new IllegalArgumentException("The treelet must be mergeable with other");

		return new ColoredTreelet(this, (ColoredTreelet)t2);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(this==obj)
			return true;
		
		if(this.getClass() != obj.getClass())
			return false;
		
		ColoredTreelet t2 = (ColoredTreelet)obj;
		
		if(size!=t2.size || root_color!=t2.root_color)
			return false;
		
		if(master==null)
			return true;
		
		return master.equals(t2.master) && other.equals(t2.other);
	}
	
	@Override
	public int hashCode()
	{
		if(!has_hash)
		{
			if(master!=null)
			{
				if(size>=6)
				{
					hash = master.hashCode() * 31 + other.hashCode();
				}
				else
				{
					int mhash = master.hashCode();
					hash = ((mhash & ~0b111) << (other.size*6)) + (other.hashCode() << 3) + (mhash & 0b111);
				}
			}
			else
			{
				hash = (root_color << 3) + root_color;
			}
	
			//hash = Objects.hash(root_color, master, other);
			has_hash=true;
		}
		
		return hash;
	}
	
	/**
	 * @return the number of nodes of this treelet
	 */
	public int getSize()
	{
		return size;
	}
}
