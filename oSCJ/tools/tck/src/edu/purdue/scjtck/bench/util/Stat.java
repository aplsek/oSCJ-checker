package edu.purdue.scjtck.bench.util;

import javax.safetycritical.Terminal;

public class Stat {

	public static void main(String args[]) {
		long[] a = new long[] { 3, 4, 2, 44, 56, 2, 3, 445 };
		sort(a);
		for (int i = 0; i < a.length; i++)
			Terminal.getTerminal().writeln(a[i] + "\n");
	}

	public static long max(long[] array) {
		return max(array, 0);
	}

	public static long max(long[] array, int startIndex) {
		if (array == null || array.length == 0 || array.length <= startIndex)
			return -1;
		long candidate = array[startIndex];
		for (int i = startIndex + 1; i < array.length; i++)
			if (array[i] > candidate)
				candidate = array[i];
		return candidate;
	}

	public static long min(long[] array) {
		return min(array, 0);
	}

	public static long min(long[] array, int startIndex) {
		if (array == null || array.length == 0 || array.length <= startIndex)
			return -1;
		long candidate = array[startIndex];
		for (int i = startIndex + 1; i < array.length; i++)
			if (array[i] < candidate)
				candidate = array[i];
		return candidate;
	}

	public static double avg(long[] array) {
		return avg(array, 0);
	}
	
	public static double avg(long[] array, int startIndex) {
		if (array == null || array.length == 0 || array.length <= startIndex)
			return -1;
		long sum = array[startIndex];
		for (int i = startIndex + 1; i < array.length; i++)
			sum += array[i];
		return (double) sum / (array.length - startIndex);
	}

	public static double stddev(long[] array) {
		return stddev(array, 0);
	}
	
	public static double stddev(long[] array, int startIndex) {
		if (array == null || array.length == 0 || array.length <= startIndex)
			return -1;
		double avg = avg(array, startIndex);
		double sqrSum = 0;
		for (int i = startIndex; i < array.length; i++)
			sqrSum = (array[i] - avg) * (array[i] - avg);
		return Math.sqrt(sqrSum / (array.length - startIndex));
	}

	public static long[] copyOfRange(long[] original, int from, int to) {
		long[] r = new long[to - from];
		for (int i = from; i < to; i++)
			r[i - from] = original[i];
		return r;
	}

	// TODO: do we need this?
	public static long dist(long[] array, int percentage) {
		if (array == null || array.length == 0)
			return -1;
		return 0;
	}

	public static void sort(long[] a) {
		if (a == null || a.length == 0)
			return;
		sort1(a, 0, a.length);
	}

	private static void sort1(long[] x, int off, int len) {
		// Insertion sort on smallest arrays
		if (len < 7) {
			for (int i = off; i < len + off; i++)
				for (int j = i; j > off && x[j - 1] > x[j]; j--)
					swap(x, j, j - 1);
			return;
		}

		// Choose a partition element, v
		int m = off + (len >> 1); // Small arrays, middle element
		if (len > 7) {
			int l = off;
			int n = off + len - 1;
			if (len > 40) { // Big arrays, pseudomedian of 9
				int s = len / 8;
				l = med3(x, l, l + s, l + 2 * s);
				m = med3(x, m - s, m, m + s);
				n = med3(x, n - 2 * s, n - s, n);
			}
			m = med3(x, l, m, n); // Mid-size, med of 3
		}
		long v = x[m];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = off, b = a, c = off + len - 1, d = c;
		while (true) {
			while (b <= c && x[b] <= v) {
				if (x[b] == v)
					swap(x, a++, b);
				b++;
			}
			while (c >= b && x[c] >= v) {
				if (x[c] == v)
					swap(x, c, d--);
				c--;
			}
			if (b > c)
				break;
			swap(x, b++, c--);
		}

		// Swap partition elements back to middle
		int s, n = off + len;
		s = Math.min(a - off, b - a);
		vecswap(x, off, b - s, s);
		s = Math.min(d - c, n - d - 1);
		vecswap(x, b, n - s, s);

		// Recursively sort non-partition-elements
		if ((s = b - a) > 1)
			sort1(x, off, s);
		if ((s = d - c) > 1)
			sort1(x, n - s, s);
	}

	private static void swap(long x[], int a, int b) {
		long t = x[a];
		x[a] = x[b];
		x[b] = t;
	}

	private static int med3(long x[], int a, int b, int c) {
		return (x[a] < x[b] ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a)
				: (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
	}

	private static void vecswap(long x[], int a, int b, int n) {
		for (int i = 0; i < n; i++, a++, b++)
			swap(x, a, b);
	}
}
