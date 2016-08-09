package wsdm16.motifs.distributions;

public class IntegerDistributionValue
{
	public final int value;
	public final double pmf;
	
	public IntegerDistributionValue(int value, double pmf)
	{
		this.value=value;
		this.pmf=pmf;
	}
}