package com.aldoreyes.imageprocessing.segmentation;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import android.graphics.Bitmap;
import android.os.AsyncTask;

public class SeqSegmAsyncTask extends AsyncTask<Bitmap, Void, Integer> {

	/*
	 * private static final int[] colors = new int[]{ 0xffffffff, 0xffffff00,
	 * 0xff00ffff, 0xffff00ff, 0xff330000, 0xff003300, 0xff000055, 0xff550000,
	 * 0xff005500, 0xff000099, 0xff990000, 0xff009900, 0xff0000CC, 0xffCC0000,
	 * 0xff00CC00, 0xff0000ff, 0xffff0000, 0xff00ff00 };
	 */

	private WeakReference<IIntegerResult> mHandler;
	private Bitmap mTaggedBM;
	private Set<SegmentInfo> mEqTable;

	public SeqSegmAsyncTask(IIntegerResult handler) {
		mHandler = new WeakReference<IIntegerResult>(handler);
	}

	public SeqSegmAsyncTask() {
		this(null);
	}

	private static void getEqTable(Bitmap data, Set<SegmentInfo> outSet) {
		TreeMap<Integer, Integer> labels = new TreeMap<Integer, Integer>();
		int lenh = data.getWidth(), lenv = data.getHeight();
		int currentTag = 0;
		int prevTop, prevLeft;
		for (int y = 0; y < lenv; y++) {
			for (int x = 0; x < lenh; x++) {
				if ((data.getPixel(x, y) & 0xff) == 0xff) {
					// check above
					prevTop = y > 0 ? data.getPixel(x, y - 1) & 0xff : 0;
					prevLeft = x > 0 ? data.getPixel(x - 1, y) & 0xff : 0;
					if (prevTop + prevLeft > 0) {
						if (prevTop > 0) {
							if (prevLeft > 0) {
								// both have a tag
								if (prevTop == prevLeft) {
									data.setPixel(x, y, 0xff000000 + prevTop);
								} else {
									data.setPixel(x, y, 0xff000000 + Math.min(
											prevLeft, prevTop));
									// add equivalence
									union(labels, prevLeft, prevTop);
								}
							} else {
								// prevTop > 0
								data.setPixel(x, y, 0xff000000 + prevTop);
							}

						} else {
							// prev left > 0
							data.setPixel(x, y, 0xff000000 + prevLeft);
						}
					} else {
						currentTag++;
						data.setPixel(x, y, 0xff000000 + currentTag);
						labels.put(currentTag, currentTag);
					}
				}
			}
		}

		consolidateTags(labels, data);

		// filter labels
		Iterator<Integer> iterator = labels.keySet().iterator();
		while (iterator.hasNext()) {
			int i = iterator.next();
			if (i == labels.get(i)) {
				outSet.add(new SegmentInfo(i, data));
			}
		}
	}

	private static void union(Map<Integer, Integer> labels, int a, int b) {
		if (a > b) {
			union(labels, b, a);
			return;
		}
		if ((a == b) || (labels.get(b) == a))
			return;

		if (labels.get(b) == b) {
			labels.put(b, a);
		} else {
			union(labels, labels.get(b), a);

			if (labels.get(b) > a) {
				// ***rbf new
				labels.put(b, a);
			}
		}
	}

	private static int find(Map<Integer, Integer> labels, int a) {
		if (labels.get(a) == a) {
			return a;
		} else {
			return find(labels, labels.get(a));
		}
	}

	/**
	 * All equivalent tags should be set to a single number, no contiguous non
	 * zero pixels should have different values
	 * 
	 * @param labels
	 * @param data
	 */
	private static void consolidateTags(Map<Integer, Integer> labels,
			Bitmap data) {
		int lenh = data.getWidth(), lenv = data.getHeight(), pixel;
		for (int y = 0; y < lenv; y++) {
			for (int x = 0; x < lenh; x++) {
				if ((pixel = data.getPixel(x, y) & 0xffffff) > 0) {
					// int i = find(labels, labels.get(pixel)) % colors.length;
					// data.setPixel(x, y, colors[i]);
					data.setPixel(x, y,
							0xff000000 + find(labels, labels.get(pixel)));
				}
			}
		}
	}

	/**
	 * 
	 * @param outSet
	 *            Out parameter, where the equality table is saved
	 * @param params
	 *            Only the first bitmap of the array is used.
	 * @return Returns the tagged bitmap
	 */
	public static Bitmap fetchResult(Set<SegmentInfo> outSet, Bitmap... params) {
		Bitmap bitmap = params[0];

		/*
		 * int[][] seErosion = new int[][]{ {1,1,1,1,1,1}, {1,1,1,1,1,1},
		 * {1,1,1,1,1,1}, {1,1,1,1,1,1}, {1,1,1,1,1,1}, {1,1,1,1,1,1} };
		 * 
		 * int[][] se = new int[][]{ {1,1,1,1}, {1,1,1,1}, {1,1,1,1}, {1,1,1,1}
		 * };
		 */

		Bitmap toReturn = bitmap.copy(bitmap.getConfig(), true); // clone to not
																	// override
																	// original
																	// bitmap

		getEqTable(toReturn, outSet);
		return toReturn;
	}

	@Override
	protected Integer doInBackground(Bitmap... params) {
		mTaggedBM = fetchResult(mEqTable = new TreeSet<SegmentInfo>(), params);

		return mEqTable.size();

	}

	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
		if (mHandler.get() != null) {
			mHandler.get().onResult(result, getTaggedBM(), mEqTable);
		}
	}

	public Set<SegmentInfo> getEqTable() {
		return mEqTable;
	}

	public Bitmap getTaggedBM() {
		return mTaggedBM;
	}

}
