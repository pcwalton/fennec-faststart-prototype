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

import org.mozilla.fennecfaststart.GeckoView;
import org.mozilla.fennecfaststart.ImageLayer;
import org.mozilla.fennecfaststart.Layer;
import org.mozilla.fennecfaststart.PLayer;
import org.mozilla.fennecfaststart.PLayers;
import org.mozilla.fennecfaststart.PLayers.Edit;
import org.mozilla.fennecfaststart.PLayers.EditReply;
import org.mozilla.fennecfaststart.PLayers.OpCreateImageLayer;
import org.mozilla.fennecfaststart.PLayers.OpPaintImage;
import org.mozilla.fennecfaststart.PLayers.SharedImage;
import android.app.Activity;
import android.util.Log;
import java.util.HashMap;

/*
 * A Java layer manager. Implements the PLayers protocol.
 */
public class LayerController extends PLayers {
    // A mapping from each client shadow layer to the layer on our side.
    private HashMap<PLayer,Layer> mShadowLayers;
    // The root layer.
    private Layer mRootLayer;
    // The main Gecko rendering view.
    private GeckoView mGeckoView;
    // The current activity.
    private Activity mActivity;

    public LayerController(Activity activity) {
        mShadowLayers = new HashMap<PLayer,Layer>();
        mGeckoView = new GeckoView(activity, this);
        mActivity = activity;
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
}

