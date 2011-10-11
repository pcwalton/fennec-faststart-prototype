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

package org.mozilla.fennecfaststart;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;

class FakeViewParent implements ViewParent {
    public void bringChildToFront(View child) {}
    public void childDrawableStateChanged(View child) {}
    public void clearChildFocus(View child) {}
    public void createContextMenu(ContextMenu menu) {}
    public View focusSearch(View v, int direction) { return null; }
    public void focusableViewAvailable(View v) {}
    public boolean getChildVisibleRect(View child, Rect r, Point offset) { return false; }
    public ViewParent getParent() { return null; }
    public void invalidateChild(View child, Rect r) {}
    public ViewParent invalidateChildInParent(int[] location, Rect r) { return null; }
    public boolean isLayoutRequested() { return false; }
    public void recomputeViewAttributes(View child) {}
    public void requestChildFocus(View child, View focused) {}
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        return false;
    }
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
    public void requestLayout() {}
    public void requestTransparentRegion(View child) {}
    public boolean showContextMenuForChild(View originalView) { return false; }
    public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback) {
        return null;
    }
}

class SurfaceTestView extends SurfaceView implements SurfaceHolder.Callback {
    private Activity mActivity;
    private Bitmap mBitmap;

    static final int SIZE = 128;
    int mRealWidth, mRealHeight;
    float mDensity;

    public SurfaceTestView(Activity activity) {
        super(activity);
        mActivity = activity;

        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mDensity = metrics.density;

        mBitmap = BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.star);
        getHolder().addCallback(this);

        Log.e("FennecFastStart", "SurfaceTestView constructor");
    }

    // The main drawing routine.
    private void redraw() {
        Log.e("FennecFastStart", "painting surfacetestview");

        SurfaceHolder holder = getHolder();
        holder.setFixedSize(mRealWidth, mRealHeight);

        Canvas canvas = holder.lockCanvas();
        canvas.drawRGB(0, 0, 255);
        canvas.drawBitmap(mBitmap, new Rect(0, 0, 200, 200),
                          new RectF(0.0f, 0.0f, SurfaceTestView.SIZE, SurfaceTestView.SIZE),
                          new Paint());
        holder.unlockCanvasAndPost(canvas);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        redraw();
    }

    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        mRealWidth = (int)(Math.round(width * mDensity));
        mRealHeight = (int)(Math.round(height * mDensity));
    }

    @Override
    protected void onAttachedToWindow() {
        // HACK
        try {
            Class surfaceClass = View.class;
            Field mParentField = surfaceClass.getDeclaredField("mParent");
            Object originalParent = mParentField.get(this);
            mParentField.set(this, new FakeViewParent());

            super.onAttachedToWindow();

            mParentField.set(this, originalParent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Surface holder interface
     */

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e("FennecFastStart", "surfaceChanged");
        //invalidate();
        redraw();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.e("FennecFastStart", "surfaceCreated");
        invalidate();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {}
}

class CircleLayout extends ViewGroup {
    public float startTheta;
    private int mRealSize;

    public CircleLayout(Context context) {
        super(context);
        startTheta = 0.0f;

        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity)getContext()).getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mRealSize = (int)(Math.round(SurfaceTestView.SIZE / metrics.density));
    }

    protected void onAttachedToWindow() {
        getParent().requestTransparentRegion(this);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int nChildren = getChildCount();
        float delta = 2.0f * (float)Math.PI / nChildren;
        float theta = startTheta;

        float centerX = (float)(right - left) / 2.0f;
        float centerY = (float)(bottom - top) / 2.0f;
        float radius = (Math.min(right - left, bottom - top) - mRealSize) / 2.0f;

        for (int i = 0; i < nChildren; i++) {
            View child = getChildAt(i);

            float deltaX = radius * (float)Math.sin(theta);
            float deltaY = radius * (float)Math.cos(theta);
            float childCenterX = centerX + deltaX;
            float childCenterY = centerY + deltaY;

            float childX = childCenterX - mRealSize / 2.0f;
            float childY = childCenterY - mRealSize / 2.0f;

            child.layout((int)Math.round(childX),
                         (int)Math.round(childY),
                         (int)Math.round(childX + mRealSize),
                         (int)Math.round(childY + mRealSize));

            theta += delta; 
        }
    }
}

public class SurfaceTestController {
    private Activity mActivity;
    private CircleLayout mLayout;
    private SurfaceTestView[] mSubviews;
    private int mRealSize;

    public SurfaceTestController(Activity activity) {
        mActivity = activity;

        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mRealSize = (int)(Math.round(SurfaceTestView.SIZE / metrics.density));

        build();
    }

    private void build() {

        mLayout = new CircleLayout(mActivity);
        mSubviews = new SurfaceTestView[5];
        for (int i = 0; i < mSubviews.length; i++) {
            SurfaceTestView subview = new SurfaceTestView(mActivity);
            mLayout.addView(subview);
            mSubviews[i] = subview;
        }
    }

    public void start() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                mLayout.post(new Runnable() {
                    public void run() {
                        mLayout.startTheta -= (float)(2.0 * Math.PI / 200.0);
                        mLayout.requestLayout();
                    }
                });
            }
        }, 0L, 1000L / 60L);
    }
    
    public View getLayout() { return mLayout; }
}

