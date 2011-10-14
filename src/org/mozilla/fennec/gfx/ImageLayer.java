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
import org.mozilla.fennec.gfx.Layer;
import org.mozilla.fennec.gfx.Tile;
import android.content.Context;
import android.util.Log;
import javax.microedition.khronos.opengles.GL10;

public class ImageLayer extends Layer {
    private CairoImage mImage;
    private boolean mSurfaceDirty;
    private Tile mTile;

    public ImageLayer() {
        mSurfaceDirty = true;
        mTile = new Tile();
    }

    public void draw(GL10 gl) {
        retileIfNecessary(gl);
        mTile.draw(gl);
    }

    private void retileIfNecessary(GL10 gl) {
        if (!mSurfaceDirty || mImage == null)
            return;

        Log.e("Fennec", "Retiling, width=" + mImage.width + ", height=" + mImage.height +
              ", format=" + mImage.format);
        mTile.setImage(gl, mImage);
        mSurfaceDirty = false;
    }
    
    public void paintImage(CairoImage image) {
        if (mImage != image) {
            mImage = image;
            mSurfaceDirty = true;
        }
    }
}

