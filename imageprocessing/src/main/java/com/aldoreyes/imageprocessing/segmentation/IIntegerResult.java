package com.aldoreyes.imageprocessing.segmentation;

import java.util.Set;

import android.graphics.Bitmap;

public interface IIntegerResult {
	void onResult(int result, Bitmap tagBM, Set<SegmentInfo> eqTable);
}
