package wsdm16.graphutils;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates all permutations of the first n integers.
 * @author anon
 */
public class PermutationGenerator {

    private int[] state;
    private int n;
    private boolean firstRun;
    
    private void init(List<Integer> list) {
	state = new int[list.size()];
	for (int i = 0; i < state.length; i++)
	    state[i] = list.get(i);
	n = state.length;
	firstRun = true;
    }
    
    public PermutationGenerator(List<Integer> list) {
	init(list);
    };
    
    public PermutationGenerator(int n) {
	List<Integer> list = new ArrayList<Integer>(n);
	for (int i = 0; i < n; i++)
	    list.add(i);
	init(list);
    }
    
    private void swap(int i, int j) {
	int tmp = state[i];
	state[i] = state[j];
	state[j] = tmp;
    }
    
    public int[] next() {
	if (!firstRun) {
	    int i = n - 1;
	    while (i > 0 && state[i-1] > state[i])
		i--;
	    int j = n;
	    while (j > 0 && state[j-1] < state[i-1])
		j--;
	    swap(i-1, j-1);
	    i++;
	    j = n;
	    while (i < j) {
		swap(i-1, j-1);
		i++;
		j--;
	    }
	} else
	    firstRun = false;
	return state;
    }
    
    public boolean hasNext() {
	int i = n - 1;
	while (i > 0 && state[i-1] > state[i])
	    i--;
	if (i == 0)
	    return false;
	return true;
    }
    
}
