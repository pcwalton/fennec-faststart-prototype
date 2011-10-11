/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Mozilla Android code.
 *
 * The Initial Developer of the Original Code is Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2009-2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Patrick Walton <pcwalton@mozilla.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.fennec;

import org.mozilla.fennecfaststart.R;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Debug;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.FileDescriptor;

public class GeckoSurfaceView extends SurfaceView implements SurfaceHolder.Callback,
                                                             GestureDetector.OnGestureListener {
    private float mX, mY;
    private Activity mActivity;
    private int mPageWidth, mPageHeight;
    private int mRealWidth, mRealHeight;
    private Bitmap mPageBitmap;
    private GestureDetector mGestureDetector;

    // FIXME: Use a Context instead of this Activity?
    public GeckoSurfaceView(Activity activity) {
        super(activity);

        mActivity = activity;
        mX = mY = 0.0f;

        mPageBitmap = BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.page);
        //mPageBitmap.setDensity(Bitmap.DENSITY_NONE);
        mPageWidth = mPageBitmap.getWidth();
        mPageHeight = mPageBitmap.getHeight();

        getHolder().addCallback(this);

        mGestureDetector = new GestureDetector(this);
    }

    // The main drawing routine. This is performance critical.
    private void redraw() {
        Log.e("FennecFastStart", "painting view width " + getWidth() + " real width " +
              mRealWidth);

        SurfaceHolder holder = getHolder();
        holder.setFixedSize(mRealWidth, mRealHeight);
        Canvas canvas = holder.lockCanvas();
        //canvas.setDensity(Bitmap.DENSITY_NONE);

        canvas.drawRGB(0, 0, 255);

        canvas.drawBitmap(mPageBitmap, new Rect(0, 0, mPageWidth, mPageHeight),
                          new RectF(mX, mY, mX + mPageWidth, mY + mPageHeight),
                          new Paint());

        Paint paint = new Paint();
        paint.setColor(Color.RED);
        canvas.drawText("xpos " + mX + " ypos " + mY, 0.0f, 12.0f, paint);

        holder.unlockCanvasAndPost(canvas);
    }

    protected void onDraw(Canvas canvas) {
        Log.e("FennecFastStart", "onDraw");
        super.onDraw(canvas);
        redraw();
    }

    // Called whenever the view changes size.
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        Log.e("FennecFastStart", "onSizeChanged " + width + " " + height);

        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mRealWidth = (int)(Math.round(width * metrics.density));
        mRealHeight = (int)(Math.round(height * metrics.density));
    }

    // Called whenever the view receives a touch event.
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    /*
     * Surface holder interface
     */

    // Called whenever the surface changes size.
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e("FennecFastStart", "surfaceChanged " + width + " " + height);
        invalidate();
    }

    // Called when the surface gets created.
    public void surfaceCreated(SurfaceHolder holder) {
        /* GeckoEvent e = new GeckoEvent(GeckoEvent.SURFACE_CREATED);
        GeckoAppShell.sendEventToGecko(e); */
        invalidate();
    }

    // Called when the surface is about to be destroyed.
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO
    }

    /*
     * Gesture detector interface
     */

    public boolean onDown(MotionEvent event) {
        Debug.dumpService("SurfaceFlinger", FileDescriptor.err, new String[0]);
        return true;
    }

    public boolean onFling(MotionEvent event0, MotionEvent event1, float velocityX,
                           float velocityY) {
        // TODO
        Log.e("FennecFastStart", "fling " + velocityX + " " + velocityY);
        return true;
    }

    public void onLongPress(MotionEvent event) {
        // TODO
    }

    public boolean onScroll(MotionEvent event0, MotionEvent event1, float deltaX, float deltaY) {
        // TODO
        mX -= (int)deltaX;
        mY -= (int)deltaY;
        invalidate();
        Log.e("FennecFastStart", "scroll " + deltaX + " " + deltaY);
        return true;
    }

    public void onShowPress(MotionEvent event) {
        // TODO
    }

    public boolean onSingleTapUp(MotionEvent event) {
        // TODO
        return true;
    }
}

