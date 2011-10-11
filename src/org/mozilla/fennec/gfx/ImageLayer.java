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

import org.mozilla.fennec.gfx.Layer;
import org.mozilla.fennec.ipdl.PLayers.SharedImage;
import org.mozilla.fennec.ipdl.PLayers.SharedImageShmem;
import org.mozilla.fennec.ipdl.PLayers.SurfaceDescriptor;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

public class ImageLayer extends Layer {
    private final int TILE_SIZE = 256;

    private SharedImageShmem mShmem;
    private boolean mSurfaceDirty;
    private FloatBuffer mVertexBuffer, mTexCoordBuffer;
    private Activity mActivity;
    private int[] mTextureIDs;
    private int mTilesAcross, mTilesDown;

    final float[] VERTICES = {
        -1.0f, -1.0f, 0.0f,
         1.0f, -1.0f, 0.0f,
        -1.0f,  1.0f, 0.0f,
         1.0f,  1.0f, 0.0f
    };

    final float[] TEX_COORDS = {
        0.0f, 1.0f,
        1.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 0.0f
    };

    public ImageLayer(Activity activity) {
        mActivity = activity;
    
        ByteBuffer vertexBuffer = ByteBuffer.allocateDirect(VERTICES.length * 4);
        vertexBuffer.order(ByteOrder.nativeOrder());
        mVertexBuffer = vertexBuffer.asFloatBuffer();
        mVertexBuffer.put(VERTICES);
        mVertexBuffer.position(0);

        ByteBuffer texCoordBuffer = ByteBuffer.allocateDirect(TEX_COORDS.length * 4);
        texCoordBuffer.order(ByteOrder.nativeOrder());
        mTexCoordBuffer = texCoordBuffer.asFloatBuffer();
        mTexCoordBuffer.put(TEX_COORDS);
        mTexCoordBuffer.position(0);

        mSurfaceDirty = true;
    }

    public void draw(GL10 gl) {
        //Log.e("Fennec", "in ImageLayer::draw");

        retileIfNecessary(gl);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        int k = 0;
        for (int i = 0; i < mTilesDown; i++) {
            for (int j = 0; j < mTilesAcross; j++) {
                gl.glLoadIdentity();
                gl.glTranslatef((float)j, (float)i, -6.0f);

                if (k == 0 || k == 3) {
                    gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIDs[k]);
                    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
                    gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexCoordBuffer);
                    gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
                }

                k++;
            }
        }

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }

    private void retileIfNecessary(GL10 gl) {
        if (!mSurfaceDirty || mShmem == null)
            return;

        mTilesAcross = (int)Math.ceil((double)mShmem.width / (double)TILE_SIZE);
        mTilesDown = (int)Math.ceil((double)mShmem.height / (double)TILE_SIZE);

        Log.e("Fennec", "Retiling, width=" + mShmem.width + ", height=" + mShmem.height +
              ", format=" + mShmem.format + ", tilesAcross=" + mTilesAcross);

        mTextureIDs = new int[mTilesAcross * mTilesDown];
        gl.glGenTextures(mTextureIDs.length, mTextureIDs, 0);

        int k = 0;
        for (int i = 0; i < mTilesDown; i++) {
            for (int j = 0; j < mTilesAcross; j++) {
                gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIDs[k]);
                gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
                                   GL10.GL_NEAREST);
                gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
                                   GL10.GL_LINEAR);
                gl.glTexSubImage2D(gl.GL_TEXTURE_2D, 0, j * TILE_SIZE, i * TILE_SIZE,
                                   TILE_SIZE, TILE_SIZE, cairoFormatToGLFormat(mShmem.format),
                                   cairoFormatToGLType(mShmem.format), mShmem.buffer);
                Log.e("Fennec", "glTexSubImage2D() error == " + gl.glGetError());
                k++;
            }
        }

        mSurfaceDirty = false;
    }
    
    public void paintImage(SharedImage image) {
        SharedImageShmem shmem = ((SurfaceDescriptor)image).shmem;
        if (mShmem != shmem) {
            mShmem = shmem;
            mSurfaceDirty = true;
        }
    }

    private static int cairoFormatToGLInternalFormat(int cairoFormat) {
        switch (cairoFormat) {
        case SharedImageShmem.FORMAT_ARGB32:
            return GL10.GL_RGBA;
        case SharedImageShmem.FORMAT_RGB24:
        case SharedImageShmem.FORMAT_RGB16_565:
            return GL10.GL_RGB;
        case SharedImageShmem.FORMAT_A8:
        case SharedImageShmem.FORMAT_A1:
            throw new RuntimeException("Cairo FORMAT_A1 and FORMAT_A8 unsupported");
        default:
            throw new RuntimeException("Unknown Cairo format");
        }
    }

    private static int cairoFormatToGLFormat(int cairoFormat) {
        switch (cairoFormat) {
        case SharedImageShmem.FORMAT_ARGB32:
            return GL10.GL_RGBA;
        case SharedImageShmem.FORMAT_RGB24:
        case SharedImageShmem.FORMAT_RGB16_565:
            return GL10.GL_RGB;
        case SharedImageShmem.FORMAT_A8:
        case SharedImageShmem.FORMAT_A1:
            return GL10.GL_ALPHA;
        default:
            throw new RuntimeException("Unknown Cairo format");
        }
    }

    private static int cairoFormatToGLType(int cairoFormat) {
        switch (cairoFormat) {
        case SharedImageShmem.FORMAT_ARGB32:
        case SharedImageShmem.FORMAT_RGB24:
        case SharedImageShmem.FORMAT_A8:
            return GL10.GL_UNSIGNED_BYTE;
        case SharedImageShmem.FORMAT_A1:
            throw new RuntimeException("Cairo FORMAT_A1 unsupported in Android OpenGL");
        case SharedImageShmem.FORMAT_RGB16_565:
            return GL10.GL_UNSIGNED_SHORT_5_6_5;
        default:
            throw new RuntimeException("Unknown Cairo format");
        }
    }
}

