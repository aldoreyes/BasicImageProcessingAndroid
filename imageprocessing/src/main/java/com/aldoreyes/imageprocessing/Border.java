package com.aldoreyes.imageprocessing;

import java.util.ArrayList;
import java.util.Iterator;

import android.graphics.Point;
import android.os.Handler;
import android.util.Log;

public class Border {
	public ArrayList<BorderPoint> points;
	public boolean isExternal;
	//if counter clock -1 else +1 on add
	private int turnCount;

	public Border() {
		points = new ArrayList<BorderPoint>();
		turnCount = 0;
		isExternal = false;
	}
	
	public void add(BorderPoint point){
		if(points.size()>0){
			turnCount+= point.counterClockDir?-1:1;
			isExternal = turnCount >= 0;
		}
		points.add(point);
	}
	
	@Override
	public String toString() {
		Iterator<BorderPoint> iterator = points.iterator();
		String toReturn = (isExternal?"external::":"internal::")+turnCount+"::";
		while(iterator.hasNext()){
			toReturn+=iterator.next().toString()+" , ";
		}
		return toReturn;
	}

	
	/**
	 * Returns 
	 * @param source 
	 * @return A Border if there is one, null otherwise
	 */
	public static Border getBorder(int[][] source) {
		Border toReturn = null;
		int xLen = source[0].length;
		int yLen = source.length;
		BorderPoint start = null;

		// search for the beginning of the border
		int prev = 0;
		for (int y = 0; y < yLen; y++) {
			for (int x = 0; x < xLen; x++) {
				// check that is a pixel next to an empty pixel
				if (source[y][x] == 1 && prev == 0) {
					start = new BorderPoint(x, y);
					break;
				} else {
					prev = source[y][x];
				}
			}
			if(start != null){
				break;
			}
		}
		
		if (start != null) {
			toReturn = new Border();
			BorderPoint current = new BorderPoint(start);
			do {
				
				toReturn.add(current.clone());
				BorderPoint.gotoNext(source, current);
			} while (!(start.equals(current)));
		}

		return toReturn;
	}
	
	public static void markBorderOnSource(int[][] source, Border border){
		Iterator<BorderPoint> iterator = border.points.iterator();
		int value = border.isExternal?2:3;
		BorderPoint point = null;
		while(iterator.hasNext()){
			point = iterator.next();
			source[point.y][point.x] = value;
		}
	}
	
	public static void spreadInternalBorder(int[][] source, Border border, Handler handler){
		spreadBorder(source,border, handler, 3);
	}
	
	public static void spreadExternalBorder(int[][] source, Border border, Handler handler){
		spreadBorder(source,border, handler, 2);
	}
	
	public static void spreadBorder(int[][] source, Border border, Handler handler, int value){
		Iterator<BorderPoint> iterator = border.points.iterator();
		while(iterator.hasNext()){
			BorderPoint point = iterator.next();
			Thread t1 = new Thread(new SpreadRunnable(handler, source, point.x-1, point.y, value));
			Thread t2 = new Thread(new SpreadRunnable(handler, source, point.x, point.y-1, value));
			Thread t3 = new Thread(new SpreadRunnable(handler, source, point.x+1, point.y, value));
			Thread t4 = new Thread(new SpreadRunnable(handler, source, point.x, point.y+1, value));
			t1.start();
			t2.start();
			t3.start();
			t4.start();
		}
	}
	
	private static void spreadPixel(int[][] source, int x, int y, int value){
		
	}
	
	private static class SpreadRunnable implements Runnable{

		private int[][] source;
		private int x,y,value;
		private Handler handler;
		
		SpreadRunnable(Handler handler, int[][] source, int x, int y, int value){
			handler.sendEmptyMessage(1);
			this.source = source;
			this.x = x;
			this.y = y;
			this.value = value;
			this.handler = handler;
			
		}
		
		@Override
		public void run() {
			
			synchronized (source) {
				int val = source[y][x];
				if(val > 0 && val != this.value){
					source[y][x] = value;
				}else{
					handler.sendEmptyMessage(-1);
					return;
				}
			}
			
			Thread t1 = new Thread(new SpreadRunnable(handler, source, x-1, y, value));
			Thread t2 = new Thread(new SpreadRunnable(handler, source, x, y-1, value));
			Thread t3 = new Thread(new SpreadRunnable(handler, source, x+1, y, value));
			Thread t4 = new Thread(new SpreadRunnable(handler, source, x, y+1, value));
			t1.start();
			t2.start();
			t3.start();
			t4.start();
			handler.sendEmptyMessage(-1);
		}
		
	}

	public static class BorderPoint extends Point {
		int dir;
		boolean counterClockDir = false;

		public BorderPoint(int x, int y) {
			super(x, y);
			dir = 0;
			
		}
		
		public BorderPoint(BorderPoint p) {
			super(p);
			dir = p.dir;
			counterClockDir = p.counterClockDir;
		}
		/**
		 * 		 - 1 -
		 * dir = 2 X 0
		 * 		 - 3 -
		 * @param source
		 * @param current
		 */
		private static void gotoNext(int[][] source, BorderPoint current) {
			int maxX = source[0].length;
			int maxY = source.length;
			int dir = current.dir;
			int startDir = dir = (dir + 3) % 4;
			int x, y;
			current.counterClockDir = true;
			do {
				x = current.x + (dir % 2 == 1 ? 0 : dir == 0 ? 1 : -1);
				y = current.y + (dir % 2 == 0 ? 0 : dir == 1 ? -1 : 1);
				if (x > 0 && x < maxX && y > 0 && y < maxY && source[y][x] > 0) {
					current.x = x;
					current.y = y;
					current.dir = dir;
					break;
				}
				current.counterClockDir = false;
				dir = (dir+1) % 4;
			}while (dir != startDir);

		}
		
		public BorderPoint clone(){
			return new BorderPoint(this);
		}

	}
}
