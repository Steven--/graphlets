package wsdm16.graphutils.test;

import java.util.Arrays;

import org.junit.Test;

import wsdm16.graphutils.PermutationGenerator;

public class TestPermutationGenerator {

    @Test
    public void test() {
	PermutationGenerator gen = new PermutationGenerator(Arrays.asList(0,1,2,3));
	while (gen.hasNext()) {
	    int[] perm = gen.next();
	    for (int x : perm)
		System.out.print(x + " ");	
	    System.out.println();
	}
	
	gen = new PermutationGenerator(4);
	while (gen.hasNext()) {
	    int[] perm = gen.next();
	    for (int x : perm)
		System.out.print(x + " ");	
	    System.out.println();
	}
    }
    
}
