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

import org.mozilla.fennec.gfx.CairoImage;
import org.mozilla.fennec.gfx.GeckoRenderer;
import org.mozilla.fennec.gfx.ImageLayer;
import org.mozilla.fennec.gfx.LayerController;
import org.mozilla.fennecfaststart.R;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.nio.ByteBuffer;

/*
 * A stand-in for Gecko that renders a static image (cached content of the previous page) using the
 * layer manager. We use this as a placeholder until Gecko is up.
 */
public class StaticImageLayerClient {
    private Activity mActivity;
    private LayerController mLayerController;
    private int mWidth, mHeight, mFormat;
    private ByteBuffer mBuffer;

    public StaticImageLayerClient(Activity activity, LayerController layerController) {
        mActivity = activity;
        mLayerController = layerController;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.page,
                                                     options);

        mWidth = bitmap.getWidth();
        mHeight = bitmap.getHeight();
        mFormat = GeckoRenderer.bitmapConfigToCairoFormat(bitmap.getConfig());
        mBuffer = ByteBuffer.allocateDirect(mWidth * mHeight * 4);
        bitmap.copyPixelsToBuffer(mBuffer.asIntBuffer());

        Log.e("Fennec", "Static image layer client uploaded");
    }

    public void init() {
        ImageLayer imageLayer = new ImageLayer(mActivity);
        mLayerController.setRoot(imageLayer);
        imageLayer.paintImage(new CairoImage(mBuffer, mWidth, mHeight, mFormat));
    }
}

