package wsdm16.motifs.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import wsdm16.motifs.EagerGraphIsomorphisms;

public class TestGraphIsomorphisms
{
    @Test
    public void test() 
    {
    	int expected[] = {0, 1, 1, 2, 6, 21, 112, 853, 11117, 261080};
    	
    	for(int i=2; i<=7; i++)
    	{
    		EagerGraphIsomorphisms gi = new EagerGraphIsomorphisms(i);
    		assertEquals(expected[i], gi.getNumberOfClasses());
    	}    
    }
}
