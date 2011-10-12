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

import org.mozilla.fennec.gfx.LayerController;
import org.mozilla.fennec.gfx.Tile;
import org.mozilla.fennec.ipdl.PLayers.SharedImageShmem;
import org.mozilla.fennec.ipdl.nsIntRect;
import org.mozilla.fennec.ipdl.nsIntSize;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.util.DisplayMetrics;
import android.util.Log;
import java.nio.ByteBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GeckoRenderer implements GLSurfaceView.Renderer {
    private Tile mBackgroundTile;
    private LayerController mLayerController;

    public GeckoRenderer(LayerController layerController) {
        mBackgroundTile = new Tile();
        mLayerController = layerController;
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glClearDepthf(1.0f);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glDisable(GL10.GL_DITHER);
        gl.glEnable(GL10.GL_TEXTURE_2D);

        recreateBackgroundTile(gl);
    }

    public void onDrawFrame(GL10 gl) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        Layer rootLayer = mLayerController.getRoot();
        if (rootLayer == null)
            return;

        gl.glLoadIdentity();
        mBackgroundTile.draw(gl);

        nsIntSize pageSize = mLayerController.getPageSize();
        nsIntRect visibleRect = mLayerController.getVisibleRect();
        gl.glLoadIdentity();
        gl.glTranslatef(-visibleRect.x, -visibleRect.y, 0.0f);

        rootLayer.draw(gl);
        // TODO: Recurse down, draw children.
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //int realWidth = dipsToRealPixels(width), realHeight = dipsToRealPixels(height);
        int realWidth = width, realHeight = height;
        Log.e("Fennec", "realWidth=" + realWidth + " realHeight=" + realHeight);
        gl.glViewport(0, 0, realWidth, realHeight);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrthof(0.0f, (float)realWidth, (float)realHeight, 0.0f, -10.0f, 10.0f);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        recreateBackgroundTile(gl);

        mLayerController.onViewportSizeChanged(realWidth, realHeight);

        // TODO
    }

    private void recreateBackgroundTile(GL10 gl) {
        Bitmap backgroundBitmap = mLayerController.getBackgroundPattern();
        int cairoFormat = bitmapConfigToCairoFormat(backgroundBitmap.getConfig());
        int width = backgroundBitmap.getWidth(), height = backgroundBitmap.getHeight();
        Log.e("Fennec", "background tile width = " + width);
        ByteBuffer backgroundBitmapByteBuffer = ByteBuffer.allocateDirect(width * height * 4);
        backgroundBitmap.copyPixelsToBuffer(backgroundBitmapByteBuffer.asIntBuffer());

        SharedImageShmem backgroundImageShmem = new SharedImageShmem();
        backgroundImageShmem.buffer = backgroundBitmapByteBuffer;
        backgroundImageShmem.width = width;
        backgroundImageShmem.height = height;
        backgroundImageShmem.format = cairoFormat;
        mBackgroundTile.setImage(gl, backgroundImageShmem);
    }

    public static int bitmapConfigToCairoFormat(Bitmap.Config config) {
        switch (config) {
        case ALPHA_8:   return SharedImageShmem.FORMAT_A8;
        case ARGB_4444: throw new RuntimeException("ARGB_444 unsupported");
        case ARGB_8888: return SharedImageShmem.FORMAT_ARGB32;
        case RGB_565:   return SharedImageShmem.FORMAT_RGB16_565;
        default:        throw new RuntimeException("Unknown Skia bitmap config");
        }
    }

    public int dipsToRealPixels(int dips) {
        DisplayMetrics metrics = new DisplayMetrics();
        mLayerController.getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return (int)Math.round(dips * metrics.density);
    }
}

