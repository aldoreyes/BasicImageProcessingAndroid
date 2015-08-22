package com.aldoreyes.imageprocessing.segmentation;

import java.lang.ref.WeakReference;

import com.aldoreyes.imageprocessing.util.BitmapUtils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.util.Log;

public class KittlerAsyncTask extends AsyncTask<Bitmap, Void, Bitmap> {
	private WeakReference<IBitmapResult> mHandler;

	public KittlerAsyncTask(IBitmapResult handler){
		mHandler = new WeakReference<IBitmapResult>(handler);
	}
	private int getThreshold(Bitmap source){
		int[] histogram = BitmapUtils.getGrayscaleHistogram(source);
		int threshold = BitmapUtils.getMean(histogram);
		
		int Tprev =-2;
		double mu, nu, p, q, sigma2, tau2, w0, w1, w2, sqterm, temp;
		
		double sumTotal = getHistSum(histogram, 255);
		double prodTotal = getHistProd(histogram, 255);
		double squaredTotal = getSquaredProd(histogram, 255);
		//int counter=1;
		while (threshold!=Tprev){
			//Calculate some statistics.
			double iSum = getHistSum(histogram, threshold);
			double iProd = getHistProd(histogram, threshold);
			double iSquared = getSquaredProd(histogram, threshold);
			
			mu = iProd/iSum;
			nu = (prodTotal-getHistProd(histogram, threshold))/(sumTotal-getHistSum(histogram, threshold));
			p = getHistSum(histogram, threshold)/sumTotal;
			q = (sumTotal-iSum) / sumTotal;
			sigma2 = getSquaredProd(histogram, threshold)/iSum-(mu*mu);
			tau2 = (squaredTotal-iSquared) / (sumTotal-iSum) - (nu*nu);

			//The terms of the quadratic equation to be solved.
			w0 = 1.0/sigma2-1.0/tau2;
			w1 = mu/sigma2-nu/tau2;
			w2 = (mu*mu)/sigma2 - (nu*nu)/tau2 + Math.log10((sigma2*(q*q))/(tau2*(p*p)));

			//If the next threshold would be imaginary, return with the current one.
			sqterm = (w1*w1)-w0*w2;
			if (sqterm < 0) {
				Log.d("ip","MinError(I): not converging. Try \'Ignore black/white\' options");
				return threshold;
			}

			//The updated threshold is the integer part of the solution of the quadratic equation.
			Tprev = threshold;
			temp = (w1+Math.sqrt(sqterm))/w0;

			if ( Double.isNaN(temp)) {
				Log.d("ip","MinError(I): NaN, not converging. Try \'Ignore black/white\' options");
				threshold = Tprev;
			}
			else{
				
				threshold =(int) Math.floor(temp);
				Log.d("ip","MinError(I): calculating... " +threshold);
			}
		}
		return threshold;
	}
	
	private double getHistSum(int [] y, int j) {
		double x = 0;
		for (int i=0;i<=j;i++)
			x+=y[i];
		return x;
	}
	
	private double getHistProd(int [] y, int j) {
		double x = 0;
		for (int i=0;i<=j;i++)
			x+=i*y[i];
		return x;
	}
	
	private double getSquaredProd(int [] y, int j) {
		double x = 0;
		for (int i=0;i<=j;i++)
			x+=i*i*y[i];
		return x;
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
