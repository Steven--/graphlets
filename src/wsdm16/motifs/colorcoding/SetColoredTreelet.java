package wsdm16.motifs.colorcoding;

public class SetColoredTreelet extends BaseTreelet
{
	private int size;
	final int num_children_isomorphic_to_other;
	
	private SetColoredTreelet master;
	private SetColoredTreelet other; //other is the last child
	
	private int colors;
	private int structureHash;
	
	/** Constructs a singleton treelet
	 * 
	 * @param color
	 */	
	public SetColoredTreelet(int color)
	{
		this.size = 1;
		num_children_isomorphic_to_other=0; //not defined

		colors = (1 << color);
		master = null;
		other = null;
		
		setStructureHashCode();
	}
	
	private SetColoredTreelet(SetColoredTreelet t1, SetColoredTreelet t2)
	{
		size=t1.size+t2.size;
		
		master = t1;
		other = t2;
		colors = t1.colors | t2.colors;
		
		//FIXME: this check is essentially duplicated in merge()
		if(master.size==1)
		{
			num_children_isomorphic_to_other=1; //t2 itself
		}
		else
		{
			if(t2.compareStructureTo(master.other)==0)
				num_children_isomorphic_to_other = master.num_children_isomorphic_to_other + 1;
			else
				num_children_isomorphic_to_other = 1; //t2 itself
		}
		
		setStructureHashCode();
	}

	
	/** Checks if this can be merges with t2
	 * @param t2
	 * @return true iff the colors sets of t1 and t2 are disjoint, and t2 is "greater or equal" than all of the children of the root of this, w.r.t. our ordering 
	 */
	public boolean is_mergeable(BaseTreelet t2)
	{
		SetColoredTreelet ct2 = (SetColoredTreelet)t2;
		
		if((colors & ct2.colors)!=0)
			return false;
		
		if(master==null)
			return true;
		
		//other is the largest child
		return ct2.compareStructureTo(other)>=0; 
	}
	
	/**
	 * @param other
	 * @return The colored treelet resulting form merging this with other
	 */
	public BaseTreelet merge(BaseTreelet t2)
	{
		if(!is_mergeable(t2))
			throw new IllegalArgumentException("The treelet must be mergeable with other");

		return new SetColoredTreelet(this, (SetColoredTreelet)t2);
	}

	/** Hashcode is split into 21 + 11 bits.
	 *  The first 21 bits are the structureHashCode while the last 11 are a bitset encoding the collors
	 *  The returned hashcode is unique up to size 11 (using 2*11-1 + 11 = 32 bits)
	 */
	@Override
	public int hashCode() 
	{
		return structureHash << 21 + colors;
	}
	
	/**
	 * Encodes the structure of the tree as DFS traversal, in binary.
	 * 1 means that we entered a new vertex and 0 means we are leaving a vertex and its subtree
	 * The last 0 is omitted. The bits needed for the hashcode to be unique are 2*size-1,
	 * hence hashcodes are unique for all trees up to size 16.
	 * I.e: a star with 3 leavers is 1101010, a path with 4 nodes is 1111000.
	 */
	private void setStructureHashCode()
	{
		if(master==null)
			structureHash=1; //enter and exit node, last 0 is omitted
		else
		{
			//Notice that we are implicitly adding a 0 between master and other 
			structureHash = (master.structureHash << 2*other.size) + other.structureHash;
		}
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(this==obj)
			return true;
		
		if(SetColoredTreelet.class != obj.getClass())
			return false;

		SetColoredTreelet t2 = (SetColoredTreelet)obj;

		if(size!=t2.size || colors!=t2.colors)
			return false;		
				
		if(size==1)
			return true;
		
		return this.compareStructureTo(t2)==0;
	}
	
	private int compareStructureTo(SetColoredTreelet t2)
	{
		if(size!=t2.size)
			return size-t2.size;

		if(size==1)
			return 0;
		
		int c = master.compareStructureTo(t2.master);
		if(c!=0)
			return c;
		
		return other.compareStructureTo(t2.other);
	}

	
	public int getSize()
	{
		return size;
	}
	
	public String toString()
	{
		if(size==1)
			return "+-";

		String m = master.toString();
		
		return m.substring(0, m.length()-1) + other.toString() + "-";
	}
}
