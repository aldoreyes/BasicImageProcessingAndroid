package com.aldoreyes.imageprocessing.segmentation;

import java.lang.ref.WeakReference;

import com.aldoreyes.imageprocessing.util.BitmapUtils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;

public class SahooAsyncTask extends AsyncTask<Bitmap, Void, Bitmap> {
	
	private WeakReference<IBitmapResult> mHandler;
	
	public SahooAsyncTask(IBitmapResult handler){
		mHandler = new WeakReference<IBitmapResult>(handler);
	}

	private int getThreshold(Bitmap source){
		int[] data = BitmapUtils.getGrayscaleHistogram(source);
		int threshold = -1;
		
		int ih, it;
		int first_bin;
		int last_bin;
		double tot_ent;  /* total entropy */
		double max_ent;  /* max entropy */
		double ent_back; /* entropy of the background pixels at a given threshold */
		double ent_obj;  /* entropy of the object pixels at a given threshold */
		double [] norm_histo = new double[256]; /* normalized histogram */
		double [] P1 = new double[256]; /* cumulative normalized histogram */
		double [] P2 = new double[256]; 

		int total =0;
		for (ih = 0; ih < 256; ih++ ) 
			total+=data[ih];

		for (ih = 0; ih < 256; ih++ )
			norm_histo[ih] = (double)data[ih]/total;

		P1[0]=norm_histo[0];
		P2[0]=1.0-P1[0];
		for (ih = 1; ih < 256; ih++ ){
			P1[ih]= P1[ih-1] + norm_histo[ih];
			P2[ih]= 1.0 - P1[ih];
		}

		/* Determine the first non-zero bin */
		first_bin=0;
		for (ih = 0; ih < 256; ih++ ) {
			if ( !(Math.abs(P1[ih])<2.220446049250313E-16)) {
				first_bin = ih;
				break;
			}
		}

		/* Determine the last non-zero bin */
		last_bin=255;
		for (ih = 255; ih >= first_bin; ih-- ) {
			if ( !(Math.abs(P2[ih])<2.220446049250313E-16)) {
				last_bin = ih;
				break;
			}
		}

		// Calculate the total entropy each gray-level
		// and find the threshold that maximizes it 
		max_ent = Double.MIN_VALUE;

		for ( it = first_bin; it <= last_bin; it++ ) {
			/* Entropy of the background pixels */
			ent_back = 0.0;
			for ( ih = 0; ih <= it; ih++ )  {
				if ( data[ih] !=0 ) {
					ent_back -= ( norm_histo[ih] / P1[it] ) * Math.log ( norm_histo[ih] / P1[it] );
				}
			}

			/* Entropy of the object pixels */
			ent_obj = 0.0;
			for ( ih = it + 1; ih < 256; ih++ ){
				if (data[ih]!=0){
				ent_obj -= ( norm_histo[ih] / P2[it] ) * Math.log ( norm_histo[ih] / P2[it] );
				}
			}

			/* Total entropy */
			tot_ent = ent_back + ent_obj;

			// IJ.log(""+max_ent+"  "+tot_ent);
			if ( max_ent < tot_ent ) {
				max_ent = tot_ent;
				threshold = it;
			}
		}
		return threshold;

	}
	
	@Override
	protected Bitmap doInBackground(Bitmap... params) {
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
	protected void onPostExecute(Bitmap result) {
		super.onPostExecute(result);
		if(mHandler.get() != null){
			mHandler.get().onResult(result);
		}
	}

}
