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

package org.mozilla.fennec.gfx;

import org.mozilla.fennec.gfx.GeckoView;
import org.mozilla.fennec.gfx.ImageLayer;
import org.mozilla.fennec.gfx.IntRect;
import org.mozilla.fennec.gfx.IntSize;
import org.mozilla.fennec.gfx.Layer;
import org.mozilla.fennec.gfx.LayerClient;
import org.mozilla.fennec.ui.PanZoomController;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View.OnTouchListener;
import java.util.HashMap;

/*
 * The layer controller manages a tile that represents the visible page. It does panning and
 * zooming natively by delegating to a panning/zooming controller. Touch events can be dispatched
 * to a higher-level view.
 */
public class LayerController implements ScaleGestureDetector.OnScaleGestureListener {
    private Layer mRootLayer;                   /* The root layer. */
    private GeckoView mGeckoView;               /* The main Gecko rendering view. */
    private Context mContext;                   /* The current context. */
    private IntRect mVisibleRect;               /* The current visible region. */
    private IntSize mNaturalViewportSize;
    /* The natural size of the visible region, without any zoom applied. */

    private PanZoomController mPanZoomController;
    /*
     * The panning and zooming controller, which interprets pan and zoom gestures for us and
     * updates our visible rect appropriately.
     */

    private OnTouchListener mOnTouchListener;   /* The touch listener. */
    private LayerClient mLayerClient;           /* The layer client. */

    public static final int TILE_SIZE = 1024;

    public LayerController(Context context, LayerClient layerClient) {
        mLayerClient = layerClient;
        layerClient.setLayerController(this);

        mGeckoView = new GeckoView(context, this);
        mContext = context;
        mVisibleRect = new IntRect(0, 0, 1, 1);     /* Gets filled in when the surface changes. */
        mNaturalViewportSize = new IntSize(1, 1);
        mPanZoomController = new PanZoomController(this);
    }

    public void setRoot(Layer layer) { mRootLayer = layer; }
    public void setLayerClient(LayerClient layerClient) { mLayerClient = layerClient; }

    public Layer getRoot() { return mRootLayer; }
    public GeckoView getView() { return mGeckoView; }
    public Context getContext() { return mContext; }
    public IntRect getVisibleRect() { return mVisibleRect; }
    public IntSize getNaturalViewportSize() { return mNaturalViewportSize; }

    public IntSize getPageSize() { return mLayerClient.getPageSize(); }

    public Bitmap getBackgroundPattern() {
        Resources resources = mContext.getResources();
        int resourceID = resources.getIdentifier("pattern", "drawable", mContext.getPackageName());
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        return BitmapFactory.decodeResource(mContext.getResources(), resourceID, options);
    }

    /*
     * Note that the zoom factor of the layer controller differs from the zoom factor of the layer
     * client (i.e. the page).
     */
    public float getZoomFactor() {
        return (float)mNaturalViewportSize.width / (float)mVisibleRect.width;
    }

    public void onViewportSizeChanged(int newWidth, int newHeight) {
        float zoomFactor = getZoomFactor();     /* Must come first. */

        mNaturalViewportSize = new IntSize(newWidth, newHeight);

        setVisibleRect(mVisibleRect.x, mVisibleRect.y,
                       (int)Math.round((float)newWidth / zoomFactor),
                       (int)Math.round((float)newHeight / zoomFactor));

        mLayerClient.onZoomFactorChanged(zoomFactor);
    }

    public void setNeedsDisplay() {
        // TODO
    }

    public void scrollTo(int x, int y) {
        setVisibleRect(x, y, mVisibleRect.width, mVisibleRect.height);
    }

    public void setVisibleRect(int x, int y, int width, int height) {
        mVisibleRect = new IntRect(x, y, width, height);
        mLayerClient.onVisibleRectChanged(mVisibleRect);
        setNeedsDisplay();
    }

    public boolean post(Runnable action) { return mGeckoView.post(action); }

    public void setOnTouchListener(OnTouchListener onTouchListener) {
        mOnTouchListener = onTouchListener;
    }

    /*
     * Gesture detection. This is handled only at a high level in this class; we dispatch to the
     * pan/zoom controller to do the dirty work.
     */

    public boolean onTouchEvent(MotionEvent event) {
        boolean result = mPanZoomController.onTouchEvent(event);
        if (mOnTouchListener != null)
            result = mOnTouchListener.onTouch(mGeckoView, event) || result;
        return result;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        return mPanZoomController.onScale(detector);
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return mPanZoomController.onScaleBegin(detector);
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        mPanZoomController.onScaleEnd(detector);
    }
}

