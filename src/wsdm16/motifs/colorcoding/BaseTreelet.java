package wsdm16.motifs.colorcoding;

public abstract class BaseTreelet
{
	public abstract boolean is_mergeable(BaseTreelet t2);
	
	public abstract BaseTreelet merge(BaseTreelet t2);
}
