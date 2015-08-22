package com.aldoreyes.imageprocessing.util;

import java.util.Iterator;

import com.aldoreyes.imageprocessing.Border;
import com.aldoreyes.imageprocessing.Border.BorderPoint;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;

public class BitmapUtils {
	public static Bitmap createBinaryImage(int[][] source, int width, int height) {
		Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bm);
		int vlen = source.length;
		int hlen = source[0].length;
		int rectWidth = width / hlen;
		int rectHeight = height / hlen;
		Rect r = new Rect(0, 0, rectWidth, rectHeight);
		Paint paint = new Paint();
		// reset to white
		paint.setARGB(255, 255, 255, 255);
		canvas.drawRect(new Rect(0, 0, width, height), paint);
		paint.setTextSize(32);
		// black tint
		paint.setARGB(255, 0, 0, 0);
		paint.setStyle(Style.STROKE);

		int midV = (int) (r.bottom * .75);
		int midH = (int) (r.bottom * .4);
		for (int y = 0; y < vlen; y++) {
			for (int x = 0; x < hlen; x++) {
				r.right = (r.left = x * rectWidth) + rectWidth;
				r.bottom = (r.top = y * rectHeight) + rectHeight;
				canvas.drawRect(r, paint);
				canvas.drawText(source[y][x] + "", r.left + midH, r.top + midV,
						paint);
			}
		}
		return bm;
	}

	public static void drawSelect(Bitmap bm, int x, int y, int cellWidth,
			int cellHeight) {
		Canvas canvas = new Canvas(bm);
		canvas.drawColor(Color.TRANSPARENT,
				android.graphics.PorterDuff.Mode.CLEAR);

		Paint paint = new Paint();
		paint.setARGB(255, 255, 0, 0);
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(3f);
		canvas.drawRect(new Rect((x - 1) * cellWidth, (y - 1) * cellHeight,
				(x + 2) * cellWidth, (y + 2) * cellHeight), paint);
	}

	public static void drawBorder(Bitmap bm, Border border, int cellWidth,
			int cellHeight) {
		Iterator<BorderPoint> iterator = border.points.iterator();
		BorderPoint point = null;

		Paint paint = new Paint();
		paint.setARGB(255, 0, 255, 0);

		Canvas canvas = new Canvas(bm);
		canvas.drawColor(Color.TRANSPARENT,
				android.graphics.PorterDuff.Mode.CLEAR);

		Rect rect = new Rect();
		while (iterator.hasNext()) {
			point = iterator.next();
			rect.set((point.x) * cellWidth, (point.y) * cellHeight,
					(point.x + 1) * cellWidth, (point.y + 1) * cellHeight);
			canvas.drawRect(rect, paint);
		}
	}

	public static int[] getGrayscaleHistogram(Bitmap bm) {
		int[] histogram = new int[256];
		int ptr = 0;
		while (ptr < histogram.length)
			histogram[ptr++] = 0;

		int hlen = bm.getWidth(), vlen = bm.getHeight();
		int pixel;
		for (int y = 0; y < vlen; y++) {
			for (int x = 0; x < hlen; x++) {
				pixel = bm.getPixel(x, y);
				histogram[pixel & 0xff]++;
				// histogram[(int)(((pixel & 0xff) + (pixel & 0xff00 >> 8) +
				// (pixel & 0xff0000 >> 16))/3)]++;
			}
		}

		return histogram;
	}

	public static int getMean(int[] data) {
		// C. A. Glasbey,
		// "An analysis of histogram-based thresholding algorithms,"
		// CVGIP: Graphical Models and Image Processing, vol. 55, pp. 532-537,
		// 1993.
		//
		// The threshold is the mean of the greyscale data
		int threshold = -1;
		double tot = 0, sum = 0;
		for (int i = 0; i < 256; i++) {
			tot += data[i];
			sum += (i * data[i]);
		}
		threshold = (int) Math.floor(sum / tot);
		return threshold;
	}

	public static int countSE(Bitmap bitmap, int[][] se) {
		int hlen = bitmap.getWidth(), vlen = bitmap.getHeight();
		int toReturn = 0;
		for (int i = vlen - 1; i >= 0; i--) {
			for (int j = hlen - 1; j >= 0; j--) {
				if (kernelMatch(j, i, hlen, vlen, bitmap, se)) {
					toReturn++;
				}
			}
		}

		return toReturn;
	}
	
	public static void dilate(Bitmap bitmap, int[][] se){
		dilate(bitmap.copy(bitmap.getConfig(), false), bitmap, se);
	}

	public static int dilate(Bitmap source, Bitmap target, int[][] se) {
		int hlen = source.getWidth(), vlen = source.getHeight(), offsetX = (int) -Math
				.floor(se[0].length / 2), offsetY = (int) -Math
				.floor(se.length / 2);
		int toReturn = 0;
		for (int i = 0; i < vlen; i++) {
			for (int j = 0; j < hlen; j++) {
				if ((source.getPixel(j, i) & 0xff) > 0) {
					kernelDilate(j, i, hlen, vlen, target, se, offsetX, offsetY);
					toReturn++;
				}
			}
		}

		return toReturn;
	}

	private static void kernelDilate(int x, int y, int w, int h, Bitmap target,
			int[][] se, int offsetX, int offsetY) {
		int lenh = se[0].length, lenv = se.length, xPos, yPos;
		for (int i = 0; i < lenv; i++) {
			for (int j = 0; j < lenh; j++) {
				if ((yPos = i + y + offsetY) > 0 && (xPos = x + j + offsetX) > 0 && yPos < h && xPos < w
						&& se[i][j] == 1) {
					target.setPixel(xPos, yPos, 0xffffffff);
				}
			}
		}
	}

	public static void erosion(Bitmap bitmap, int[][] se) {

		int hlen = bitmap.getWidth(), vlen = bitmap.getHeight();
		for (int i = 0; i < vlen; i++) {
			for (int j = 0; j < hlen; j++) {
				bitmap.setPixel(j, i,
						kernelMatch(j, i, hlen, vlen, bitmap, se) ? 0xffffffff
								: 0xff000000);
			}
		}
	}

	private static boolean kernelMatch(int x, int y, int w, int h,
			Bitmap bitmap, int[][] se) {
		int lenh = se[0].length, lenv = se.length;
		for (int i = 0; i < lenv; i++) {
			for (int j = 0; j < lenh; j++) {
				int pVal = ((bitmap.getPixel((x + j) % w, (y + i) % h) & 0xff) > 0 ? 1 : 0);
				if (se[i][j] != pVal) {
					return false;
				}
			}
		}

		return true;
	}

	public static int getPixelsCount(Bitmap bitmap) {
		int hlen = bitmap.getWidth(), vlen = bitmap.getHeight();
		int toReturn = 0;
		for (int i = 0; i < vlen; i++) {
			for (int j = 0; j < hlen; j++) {
				if ((bitmap.getPixel(j, i) & 0xff) > 0) {
					toReturn++;
				}
			}
		}

		return toReturn;
	}

	public static int getContactCount(Bitmap bitmap) {
		int toReturn = 0;
		int hlen = bitmap.getWidth(), vlen = bitmap.getHeight();
		for (int i = 0; i < vlen; i++) {
			for (int j = 0; j < hlen; j++) {
				if ((bitmap.getPixel(j, i) & 0xff) > 0) {
					// check right border
					if (j + 1 < hlen && (bitmap.getPixel(j + 1, i) & 0xff) > 0) {
						toReturn++;
					}
					// check bottom border
					if (i + 1 < vlen && (bitmap.getPixel(j, i + 1) & 0xff) > 0) {
						toReturn++;
					}
				}
			}
		}

		return toReturn;
	}
	
	public static void drawCross(Bitmap bm, int x, int y){
		Canvas c = new Canvas(bm);
		Paint p = new Paint();
		p.setColor(0xffff0000);
		c.drawLine(x-4, y, x+4, y, p);
		c.drawLine(x, y-4, x, y+4, p);
	}
}
