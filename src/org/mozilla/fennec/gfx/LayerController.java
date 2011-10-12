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
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import java.util.HashMap;

/*
 * A Java layer manager implementing the PLayers protocol. Does panning and zooming natively by
 * delegating to a panning/zooming controller so that the UI is usable before Gecko is up.
 */
public class LayerController extends PLayers
        implements GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener {
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
    // The panning and zooming controller, which interprets pan and zoom gestures for us and
    // updates our visible rect appropriately.
    private PanZoomController mPanZoomController;

    public LayerController(Activity activity) {
        mShadowLayers = new HashMap<PLayer,Layer>();
        mGeckoView = new GeckoView(activity, this);
        mActivity = activity;
        mPageSize = new nsIntSize(995, 1250);       // TODO: Make this real.
        mVisibleRect = new nsIntRect(0, 0, 0, 0);   // Gets filled in when the surface changes.
        mPanZoomController = new PanZoomController(this);
    }

    /*
     * Editing operations
     */

    private EditReply createImageLayer(PLayer layer) {
        assert (!mShadowLayers.containsKey(layer));
        mShadowLayers.put(layer, new ImageLayer(mActivity));
        return null;
    }

    private EditReply setRoot(PLayer layer) {
        assert (mShadowLayers.containsKey(layer));
        mRootLayer = mShadowLayers.get(layer);
        return null;
    }

    private EditReply paintImage(PLayer layer, SharedImage image) {
        assert (mShadowLayers.containsKey(layer));
        assert (mShadowLayers.get(layer) instanceof ImageLayer);

        ImageLayer imageLayer = (ImageLayer)mShadowLayers.get(layer);
        imageLayer.paintImage(image);
        return null;
    }

    public EditReply[] update(Edit[] cset) {
        EditReply[] replies = new EditReply[cset.length];
        for (int i = 0; i < cset.length; i++) {
            // TODO: Might want to use a hash table on Java Class objects here or something.
            Edit edit = cset[i];
            if (edit instanceof OpCreateImageLayer) {
                replies[i] = createImageLayer(((OpCreateImageLayer)edit).layer);
            } else if (edit instanceof OpSetRoot) {
                replies[i] = setRoot(((OpSetRoot)edit).root);
            } else if (edit instanceof OpPaintImage) {
                OpPaintImage op = (OpPaintImage)edit;
                replies[i] = paintImage(op.layer, op.newFrontBuffer);
            } else {
                Log.e("Fennec", "unimplemented layer edit");
            }
        }
        return replies;
    }

    public Layer getRoot() { return mRootLayer; }
    public GeckoView getView() { return mGeckoView; }
    public Activity getActivity() { return mActivity; }
    public nsIntSize getPageSize() { return mPageSize; }
    public nsIntRect getVisibleRect() { return mVisibleRect; }

    public Bitmap getBackgroundPattern() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        return BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.pattern, options);
    }

    public void onViewportSizeChanged(int newWidth, int newHeight) {
        mVisibleRect.width = newWidth; mVisibleRect.height = newHeight;
    }

    public void setNeedsDisplay() {
        // TODO
    }

    public void scrollTo(int x, int y) {
        mVisibleRect.x = x; mVisibleRect.y = y;
        setNeedsDisplay();
    }

    /*
     * Gesture detection. This is handled only at a high level in this class; we dispatch to the
     * pan/zoom controller to do the dirty work.
     */

    @Override
    public boolean onDown(MotionEvent event) { return true; }

    @Override
    public boolean onFling(MotionEvent event0, MotionEvent event1, float velocityX,
                           float velocityY) {
        return mPanZoomController.onFling(event0, event1, velocityX, velocityY);
    }

    @Override
    public void onLongPress(MotionEvent event) {
        // TODO: Pop up a menu or something.
    }

    @Override
    public boolean onScroll(MotionEvent event0, MotionEvent event1, float deltaX, float deltaY) {
        return mPanZoomController.onScroll(event0, event1, deltaX, deltaY);
    }

    @Override
    public void onShowPress(MotionEvent event) {
        // TODO
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        // TODO: Forward to Gecko? Might be too high level to be useful to Gecko...
        return true;
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

