package wsdm16.motifs.distributions;

import org.apache.commons.math3.random.RandomGenerator;

public class EnumeratedIntegerDistribution extends BaseEnumeratedDistribution
{	
	private int[] values;
	
	public EnumeratedIntegerDistribution(double[] pmf)
	{
		this(null, pmf, true);
	}

	public EnumeratedIntegerDistribution(int[] values, double[] pmf)
	{
		this(values, pmf, true);
	}
	
	public EnumeratedIntegerDistribution(int[] values, double[] pmf, boolean copy)
	{
		if(pmf.length==0)
			throw new IllegalArgumentException("Distribution contains no values.");
				
		if(copy)
		{
			cdf = new double[pmf.length];
			this.values = (values!=null)?values.clone():null;
		}
		else
		{
			this.values = values;
			cdf = pmf;
		}
		
		for(int i=0; i<cdf.length; i++)
		{
			if(pmf[i]<0)
				throw new IllegalArgumentException("Probabilities cannot be < 0");

			cdf[i]= pmf[i] + ( (i>0)?cdf[i-1]:0 );
		}

		if(cdf[cdf.length-1]==0)
			throw new IllegalArgumentException("Probabilities cannot be all 0");
	}

	/** Samples from the distribution. Thread safe provided random is used by only one thread or is thread-safe.
	 */
	public int sample(RandomGenerator random)
	{
		int idx = sampleFromCDF(random);
		return (values!=null)?values[idx]:idx;		
	}
	
}
