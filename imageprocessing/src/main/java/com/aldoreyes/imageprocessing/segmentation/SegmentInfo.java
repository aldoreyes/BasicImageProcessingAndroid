package com.aldoreyes.imageprocessing.segmentation;


import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.SparseArray;

public class SegmentInfo implements Comparable<SegmentInfo> {
	private Point mStartPoint;
	private Point mEndPoint;
	public int Label;
	private Bitmap mSource;
	private Point mCenter;

	private SparseArray<Double> mCache;
	private SparseArray<Double> mCentralMomentCache;
	private SparseArray<Double> mNCache;
	private double mHu1;
	private double mHu2;
	private double mFlusser1;
	private double mFlusser2;
	private double mFlusser3;
	private double mFlusser4;

	public Point getStartPoint() {
		return mStartPoint;
	}

	public void setStartPoint(Point mStartPoint) {
		this.mStartPoint = mStartPoint;
	}

	public Point getEndPoint() {
		return mEndPoint;
	}

	public void setEndPoint(Point mEndPoint) {
		this.mEndPoint = mEndPoint;
	}

	public SegmentInfo(int label, Bitmap source) {
		Label = label;
		mSource = source;
		mCache = new SparseArray<Double>();
		mCentralMomentCache = new SparseArray<Double>();
		mNCache = new SparseArray<Double>();
		
		init();
	}

	private void init() {
		int minX = mSource.getWidth(), minY = mSource.getHeight(), maxX = -1, maxY = -1;

		// get min point Y
		for (int y = 0; y < minY; y++) {
			for (int x = 0; x < minX; x++) {

				if ((mSource.getPixel(x, y) & 0xffffff) == Label) {
					minY = y;
					break;
				}
			}
		}

		// get min point X
		for (int y = 0; y < mSource.getHeight(); y++) {
			for (int x = 0; x < minX; x++) {
				if ((mSource.getPixel(x, y) & 0xffffff) == Label) {
					minX = x;
					break;
				}
			}
		}
		setStartPoint(new Point(minX, minY));

		// get max point Y
		for (int y = mSource.getHeight() - 1; y > maxY; y--) {
			for (int x = 0; x < mSource.getWidth(); x++) {
				if ((mSource.getPixel(x, y) & 0xffffff) == Label) {
					maxY = y;
					break;
				}
			}
		}
		// get max point X
		for (int y = 0; y < mSource.getHeight(); y++) {
			for (int x = mSource.getWidth() - 1; x > maxX; x--) {
				if ((mSource.getPixel(x, y) & 0xffffff) == Label) {
					maxX = x;
					break;
				}
			}
		}
		setEndPoint(new Point(maxX, maxY));
	}

	public double getM(int p, int q) {
		if (mCache.indexOfKey(p * 10 + q) >= 0) {
			return mCache.get(p * 10 + q);
		}

		double m = 0;

		for (int y = mStartPoint.y; y < mEndPoint.y; y++) {
			for (int x = mStartPoint.x; x < mEndPoint.x; x++) {
				if ((mSource.getPixel(x, y) & 0xffffff) == Label) {
					m += Math.pow(x, p) * Math.pow(y, q);
				}
			}
		}
		
		mCache.put(p * 10 + q, m);

		return m;

	}

	public double getCentralM(int p, int q) {
		if (mCentralMomentCache.indexOfKey(p * 10 + q) >= 0) {
			return mCentralMomentCache.get(p * 10 + q);
		}
		Point centroid = getCenter();
		double m = 0;
		for (int y = mStartPoint.y; y < mEndPoint.y; y++) {
			for (int x = mStartPoint.x; x < mEndPoint.x; x++) {
				if ((mSource.getPixel(x, y) & 0xffffff) == Label) {
					m += Math.pow(x - centroid.x, p)
							* Math.pow(y - centroid.y, q);
				}
			}
		}

		mCentralMomentCache.put(p * 10 + q, m);

		return m;
	}

	public double getN(int i, int j) {
		if (mNCache.indexOfKey(i * 10 + j) >= 0) {
			return mNCache.get(i * 10 + j);
		}

		double n = getCentralM(i, j);
		double div = getCentralM(0, 0);

		n = n / Math.pow(div, 1 + (i + j) / 2);
		mNCache.put(i * 10 + j, n);
		return n;
	}

	public Point getCenter() {
		if (mCenter != null) {
			return mCenter;
		}

		return mCenter = new Point((int) (getM(1, 0) / getM(0, 0)),
				(int) (getM(0, 1) / getM(0, 0)));
	}

	public double getHuInvariant1() {
		if (mHu1 != 0) {
			return mHu1;
		}

		return mHu1 = getN(2, 0) + getN(0, 2);
	}

	public double getHuInvariant2() {
		if (mHu2 != 0) {
			return mHu2;
		}

		return mHu2 = Math.pow(getN(2, 0) - getN(0, 2), 2) + 4
				* Math.pow(getN(1, 1), 2);
	}

	public double getFlusser1() {
		if (mFlusser1 != 0) {
			return mFlusser1;
		}

		return mFlusser1 = getN(2, 0) + getN(0, 2);
	}

	public double getFlusser2() {
		if (mFlusser2 != 0) {
			return mFlusser2;
		}
		return mFlusser2 = getN(3,0); //Math.pow(getN(3, 0) + getN(1, 2), 2)
				//+ Math.pow(getN(2, 1) + getN(0, 3), 2);
	}

	public double getFlusser3() {
		if (mFlusser3 != 0) {
			return mFlusser3;

		}

		return mFlusser3 = (getN(2, 0) - getN(0, 2))
				* (Math.pow(getN(3, 0) + getN(1, 2), 2) - Math.pow(getN(2, 1)
						+ getN(0, 3), 2)) + 4 * getN(1, 1)
				* (getN(3, 0) + getN(1, 2)) * (getN(2, 1) + getN(0, 3));
	}
	
	public double getFlusser4(){
		if(mFlusser4 != 0){
			return mFlusser4;
		}
		
		return mFlusser4 = getN(1, 1) * (Math.pow(getN(3,0) + getN(1, 2), 2) - Math.pow(getN(0, 3) + getN(2, 1), 2)) -
				(getN(2, 0) - getN(0, 2))*(getN(3, 0) + getN(1, 2))*(getN(0, 3) + getN(2, 1));
	}

	@Override
	public int compareTo(SegmentInfo another) {
		return Label - another.Label;
	}
}
