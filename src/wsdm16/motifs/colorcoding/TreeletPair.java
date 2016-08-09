package wsdm16.motifs.colorcoding;


public class TreeletPair<T extends BaseTreelet>
{
	public final T t1;
	public final T t2;
	
	public TreeletPair(T t1, T t2)
	{
		this.t1=t1;
		this.t2=t2;
	}

	@Override
	public boolean equals(Object other)
	{
		if(this==other)
			return true;
		
		if(TreeletPair.class != other.getClass())
			return false;
		
		@SuppressWarnings("unchecked")
		TreeletPair<T> ctp = (TreeletPair<T>)other;
		return t1.equals(ctp.t1) && t2.equals(ctp.t2);
	}
	
	@Override
	public int hashCode()
	{
		return t1.hashCode() *31 + t2.hashCode();
	}
	
}