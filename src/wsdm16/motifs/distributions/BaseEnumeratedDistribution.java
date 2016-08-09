package wsdm16.motifs.distributions;

import org.apache.commons.math3.random.RandomGenerator;

public abstract class BaseEnumeratedDistribution
{
	protected double[] cdf;

	protected int sampleFromCDF(RandomGenerator random)
	{
		double r = random.nextDouble()*cdf[cdf.length-1];
				
        int low = 0;
        int high = cdf.length - 1;

        while (low <= high)
        {
        	int mid = (low + high) >>> 1;
        	double midVal = cdf[mid];
        		
        	if (midVal < r)
				low = mid + 1;
			else if (midVal > r || (mid>low && cdf[mid-1]==midVal) )
				high = mid - 1;
			else
				return mid; 
        }
		
        return low;
	}
}