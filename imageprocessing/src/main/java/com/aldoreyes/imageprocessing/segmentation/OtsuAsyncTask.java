package com.aldoreyes.imageprocessing.segmentation;

import java.lang.ref.WeakReference;

import com.aldoreyes.imageprocessing.util.BitmapUtils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;

public class OtsuAsyncTask extends AsyncTask<Bitmap, Void, Bitmap> {

	private WeakReference<IBitmapResult> mHandler;

	public OtsuAsyncTask(IBitmapResult handler){
		mHandler = new WeakReference<IBitmapResult>(handler);
	}
	
	public OtsuAsyncTask(){
		this(null);
	}
	
	private static int getThreshold(Bitmap source){
		int[] histogram = BitmapUtils.getGrayscaleHistogram(source);
		int total = source.getWidth() * source.getHeight();
		float sum = 0;
		for (int t=0 ; t<256 ; t++) sum += t * histogram[t];

		float sumB = 0;
		int wB = 0;
		int wF = 0;

		float varMax = 0;
		int threshold = 0;

		for (int t=0 ; t<256 ; t++) {
		   wB += histogram[t];               // Weight Background
		   if (wB == 0) continue;

		   wF = total - wB;                 // Weight Foreground
		   if (wF == 0) break;

		   sumB += (float) (t * histogram[t]);

		   float mB = sumB / wB;            // Mean Background
		   float mF = (sum - sumB) / wF;    // Mean Foreground

		   // Calculate Between Class Variance
		   float varBetween = (float)wB * (float)wF * (mB - mF) * (mB - mF);

		   // Check if new maximum found
		   if (varBetween > varMax) {
		      varMax = varBetween;
		      threshold = t;
		   }
		}
		
		return threshold;
	}
	
	public static Bitmap fetchResult(Bitmap... params){
		Bitmap source = params[0];
		Bitmap target = Bitmap.createBitmap(source.getWidth(), source.getHeight(), source.getConfig());
		Canvas c = new Canvas(target);
		c.drawColor(0xff000000);
		int threshold = getThreshold(source);
		int lenh = source.getWidth();
		int lenv = source.getHeight();
		for (int i = 0; i < lenv; i++) {
			for (int j = 0; j < lenh; j++) {
				if((source.getPixel(j, i) & 0xff) >= threshold){
					target.setPixel(j, i, 0xffffffff);
				}
			}
		}
		
		return target;
	}
	
	@Override
	protected Bitmap doInBackground(Bitmap... params) {
		return fetchResult(params);
	}
	
	@Override
	protected void onPostExecute(Bitmap result) {
		super.onPostExecute(result);
		if(mHandler.get() != null){
			mHandler.get().onResult(result);
		}
	}
	
}
