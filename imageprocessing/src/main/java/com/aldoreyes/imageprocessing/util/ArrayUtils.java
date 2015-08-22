package com.aldoreyes.imageprocessing.util;

public class ArrayUtils {
	public static int neighbors(int[][] source, int x, int y){
		int maxX = Math.min(source[0].length-1, x+1);
		int maxY = Math.min(source.length-1, y+1);
		int counter = 0;
		for(int i=Math.max(y-1, 0);i<=maxY; i++){
			for(int j=Math.max(x-1, 0);j<=maxX; j++){
					
					if(j!=x || i!=y){
						counter+=source[i][j];
					}
			}
		}
		
		return counter;
	}
	
	public static double dotProd(double[] a, double[] b){
		if(a.length != b.length){
			throw new IllegalArgumentException("The dimensions have to be equal!");
		}
		double sum = 0;
		for(int i = 0; i < a.length; i++){
			sum += a[i] * b[i];
		}
		return sum;
	}
	
	public static double average(double[] a){
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			sum+= a[i];
		}
		
		return sum/a.length;
	}
	
	
}
