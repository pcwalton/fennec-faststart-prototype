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

import org.mozilla.fennec.gfx.CairoImage;
import org.mozilla.fennec.gfx.IntSize;
import org.mozilla.fennec.gfx.Layer;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Base class for tile layers, which encapsulate the logic needed to draw textured tiles in OpenGL
 * ES.
 */
public abstract class TileLayer extends Layer {
    private CairoImage mImage;
    private boolean mRepeat, mTextureUploadNeeded;
    private IntSize mSize;
    private int[] mTextureIDs;

    public TileLayer(boolean repeat) {
        super();
        mRepeat = repeat;
        mTextureUploadNeeded = false;
    }

    public IntSize getSize() { return mSize; }

    protected boolean repeats() { return mRepeat; }
    protected int getTextureID() { return mTextureIDs[0]; }

    /**
     * Subclasses implement this method to perform tile drawing.
     *
     * Invariant: The current matrix mode must be GL_MODELVIEW both before and after this call.
     */
    protected abstract void onTileDraw(GL10 gl);

    @Override
    protected void onDraw(GL10 gl) {
        if (mImage == null)
            return;
        if (mTextureUploadNeeded)
            uploadTexture(gl);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glPushMatrix();

        onTileDraw(gl);

        gl.glPopMatrix();
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }

    public void paintImage(CairoImage image) {
        if (mImage == image)
            return;

        mImage = image;
        mTextureUploadNeeded = true;

        /*
         * Assert that the image has a power-of-two size. Phones tend not to support NPOT textures,
         * and OpenGL ES doesn't seem to let us efficiently slice up a NPOT bitmap.
         */
        assert (mImage.width & (mImage.width - 1)) == 0;
        assert (mImage.height & (mImage.height - 1)) == 0;
    }

    private void uploadTexture(GL10 gl) {
        if (mTextureIDs == null) {
            mTextureIDs = new int[1];
            gl.glGenTextures(mTextureIDs.length, mTextureIDs, 0);
        }

        mSize = new IntSize(mImage.width, mImage.height);

        int internalFormat = CairoUtils.cairoFormatToGLInternalFormat(mImage.format);
        int format = CairoUtils.cairoFormatToGLFormat(mImage.format);
        int type = CairoUtils.cairoFormatToGLType(mImage.format);

        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIDs[0]);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

        int repeatMode = mRepeat ? GL10.GL_REPEAT : GL10.GL_CLAMP_TO_EDGE;
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, repeatMode);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, repeatMode);

        gl.glTexImage2D(gl.GL_TEXTURE_2D, 0, internalFormat, mSize.width, mSize.height, 0, format,
                        type, mImage.buffer);

        mTextureUploadNeeded = false;
    }

    protected static FloatBuffer createBuffer(float[] values) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(values.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());

        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.put(values);
        floatBuffer.position(0);
        return floatBuffer;
    }
    
    protected static void drawTriangles(GL10 gl, FloatBuffer vertexBuffer,
                                        FloatBuffer texCoordBuffer, int count) {
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texCoordBuffer);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, count);
    }
}

