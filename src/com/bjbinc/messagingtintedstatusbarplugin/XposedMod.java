package com.bjbinc.messagingtintedstatusbarplugin;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class XposedMod implements IXposedHookZygoteInit {
	private Activity act;
	Handler handler;
	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {

		XposedHelpers.findAndHookMethod(Activity.class, "onWindowFocusChanged", boolean.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {

				Activity activity = (Activity) param.thisObject;
				String packageName = activity.getPackageName();

				if((Boolean) param.args[0] && packageName.equals("com.google.android.apps.messaging")){
					try{
						int color = getColor(activity);
						if(color==-1){
							color=0;
							StatusBarTintApi.sendColorChangeIntent(color, Color.WHITE, -3, -3, activity.getApplicationContext());
							act = activity;
							handler=new Handler();
							final Runnable r = new Runnable()
							{
								public void run() 
								{
									int color = getColor(act);
									if(color==-1){
										handler.postDelayed(this, 500);
									}
									else{
										StatusBarTintApi.sendColorChangeIntent(color, Color.WHITE, -3, -3, act.getApplicationContext());
									}
								}
							};
							handler.postDelayed(r, 500);
						}
						else{
							StatusBarTintApi.sendColorChangeIntent(color, Color.WHITE, -3, -3, activity.getApplicationContext());
						}
					}
					catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		});
	}

	public int getColor(Activity activity){
		Bitmap bm = takeScreenshot(activity);
		int width = bm.getWidth();
		int height = getStatusBarHeight(activity);
		BitmapDrawable bd = new BitmapDrawable(activity.getResources(),Bitmap.createBitmap(bm, 0, height, width, height));
		return getMainColorFromActionBarDrawable(bd);
	}

	public Bitmap takeScreenshot(Activity activity) {
		View rootView = activity.findViewById(android.R.id.content).getRootView();
		rootView.setDrawingCacheEnabled(false);
		rootView.invalidate();
		rootView.setDrawingCacheEnabled(true);
		return rootView.getDrawingCache();
	}
	
	public int getStatusBarHeight(Activity activity) {
		int result = 0;
		int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = activity.getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	public static int getMainColorFromActionBarDrawable(Drawable drawable) throws IllegalArgumentException {
		/* This should fix the bug where a huge part of the ActionBar background is drawn white. */
		Drawable copyDrawable = drawable.getConstantState().newDrawable();

		if (copyDrawable instanceof ColorDrawable) {
			return ((ColorDrawable) drawable).getColor();
		}

		Bitmap bitmap = drawableToBitmap(copyDrawable);
		int pixel = bitmap.getPixel(0, 40);
		int red = Color.red(pixel);
		int blue = Color.blue(pixel);
		int green = Color.green(pixel);
		int alpha = Color.alpha(pixel);
		copyDrawable = null;
		return Color.argb(alpha, red, green, blue);
	}

	public static Bitmap drawableToBitmap(Drawable drawable) throws IllegalArgumentException {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable)drawable).getBitmap();
		}
		Bitmap bitmap;

		try {
			bitmap = Bitmap.createBitmap(1, 80, Config.ARGB_8888);
			bitmap.setDensity(480);
			Canvas canvas = new Canvas(bitmap); 
			drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
			drawable.draw(canvas);
		} catch (IllegalArgumentException e) {
			throw e;
		}

		return bitmap;
	}

	public static int getIconColorForColor(int color) {
		float hsvMaxValue = 0.7f;
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		float value = hsv[2];

		if (value > hsvMaxValue) {
			return Color.BLACK;
		} else {
			return Color.WHITE;
		}
	}
}
