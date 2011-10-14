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

import org.mozilla.fennecfaststart.R;
import org.mozilla.fennec.gfx.GeckoView;
import org.mozilla.fennec.gfx.ImageLayer;
import org.mozilla.fennec.gfx.Layer;
import org.mozilla.fennec.ipdl.PLayer;
import org.mozilla.fennec.ipdl.PLayers;
import org.mozilla.fennec.ipdl.PLayers.Edit;
import org.mozilla.fennec.ipdl.PLayers.EditReply;
import org.mozilla.fennec.ipdl.PLayers.OpCreateImageLayer;
import org.mozilla.fennec.ipdl.PLayers.OpPaintImage;
import org.mozilla.fennec.ipdl.PLayers.SharedImage;
import org.mozilla.fennec.ipdl.nsIntRect;
import org.mozilla.fennec.ipdl.nsIntSize;
import org.mozilla.fennec.ui.PanZoomController;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import java.util.HashMap;

/*
 * The layer controller manages a tile that represents the visible page. It does panning and zooming
 * natively by delegating to a panning/zooming controller.
 */
public class LayerController implements ScaleGestureDetector.OnScaleGestureListener {
    // A mapping from each client shadow layer to the layer on our side.
    private HashMap<PLayer,Layer> mShadowLayers;
    // The root layer.
    private Layer mRootLayer;
    // The main Gecko rendering view.
    private GeckoView mGeckoView;
    // The current activity.
    private Activity mActivity;
    // The current page size in pixels.
    private nsIntSize mPageSize;
    // The current visible region.
    private nsIntRect mVisibleRect;
    // The natural size of the visible region, without any zoom applied.
    private nsIntSize mNaturalViewportSize;
    // The panning and zooming controller, which interprets pan and zoom gestures for us and
    // updates our visible rect appropriately.
    private PanZoomController mPanZoomController;

    public LayerController(Activity activity) {
        mShadowLayers = new HashMap<PLayer,Layer>();
        mGeckoView = new GeckoView(activity, this);
        mActivity = activity;
        mPageSize = new nsIntSize(970, 1024);       // TODO: Make this real.
        mVisibleRect = new nsIntRect(0, 0, 1, 1);   // Gets filled in when the surface changes.
        mNaturalViewportSize = new nsIntSize(1, 1);
        mPanZoomController = new PanZoomController(this);
    }

    /*
     * Editing operations
     */

    public void createImageLayer(PLayer layer) {
        assert (!mShadowLayers.containsKey(layer));
        mShadowLayers.put(layer, new ImageLayer(mActivity));
    }

    public void setRoot(PLayer layer) {
        assert (mShadowLayers.containsKey(layer));
        mRootLayer = mShadowLayers.get(layer);
    }

    public void paintImage(PLayer layer, SharedImage image) {
        assert (mShadowLayers.containsKey(layer));
        assert (mShadowLayers.get(layer) instanceof ImageLayer);

        ImageLayer imageLayer = (ImageLayer)mShadowLayers.get(layer);
        imageLayer.paintImage(image);
    }

    public Layer getRoot() { return mRootLayer; }
    public GeckoView getView() { return mGeckoView; }
    public Activity getActivity() { return mActivity; }
    public nsIntSize getPageSize() { return mPageSize; }
    public nsIntRect getVisibleRect() { return mVisibleRect; }
    public nsIntSize getNaturalViewportSize() { return mNaturalViewportSize; }

    public Bitmap getBackgroundPattern() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        return BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.pattern, options);
    }

    public float getZoomFactor() {
        return (float)mNaturalViewportSize.width / (float)mVisibleRect.width;
    }

    public void onViewportSizeChanged(int newWidth, int newHeight) {
        float zoomFactor = getZoomFactor();
        mNaturalViewportSize.width = newWidth; mNaturalViewportSize.height = newHeight;
        mVisibleRect.width = (int)Math.round((float)newWidth / zoomFactor);
        mVisibleRect.height = (int)Math.round((float)newHeight / zoomFactor);
    }

    public void setNeedsDisplay() {
        // TODO
    }

    public void scrollTo(int x, int y) {
        mVisibleRect.x = x; mVisibleRect.y = y;
        setNeedsDisplay();
    }

    public void setVisibleRect(int x, int y, int width, int height) {
        mVisibleRect.x = x; mVisibleRect.y = y;
        mVisibleRect.width = width; mVisibleRect.height = height;
        setNeedsDisplay();
    }

    public boolean post(Runnable action) { return mGeckoView.post(action); }

    /*
     * Gesture detection. This is handled only at a high level in this class; we dispatch to the
     * pan/zoom controller to do the dirty work.
     */

    public boolean onTouchEvent(MotionEvent event) {
        return mPanZoomController.onTouchEvent(event);
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

