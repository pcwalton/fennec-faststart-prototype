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

package org.mozilla.fennec.ui;

import org.mozilla.fennec.gfx.IntPoint;
import org.mozilla.fennec.gfx.IntRect;
import org.mozilla.fennec.gfx.IntSize;
import org.mozilla.fennec.gfx.LayerController;

/** Manages the dimensions of the page viewport. */
public class ViewportController {
    private IntSize mPageSize;
    private IntRect mVisibleRect;

    public ViewportController(IntSize pageSize, IntRect visibleRect) {
        mPageSize = pageSize;
        mVisibleRect = visibleRect;
    }

    /** Returns the given rect, clamped to the boundaries of a tile. */
    public IntRect clampRect(IntRect rect) {
        int x = Math.max(0, Math.min(rect.x, mPageSize.width - rect.width));
        int y = Math.max(0, Math.min(rect.y, mPageSize.height - rect.height));
        return new IntRect(x, y, rect.width, rect.height);
    }

    /** Returns the coordinates of a tile centered on the given rect. */
    public static IntRect widenRect(IntRect rect) {
        IntPoint center = rect.getCenter();
        return new IntRect(center.x - LayerController.TILE_WIDTH / 2,
                           center.y - LayerController.TILE_HEIGHT / 2,
                           LayerController.TILE_WIDTH,
                           LayerController.TILE_HEIGHT);
    }

    /**
     * Given the layer controller's visible rect, page size, and screen size, returns the zoom
     * factor.
     */
    public float getZoomFactor(IntRect layerVisibleRect, IntSize layerPageSize,
                               IntSize screenSize) {
        IntRect transformed = transformVisibleRect(layerVisibleRect, layerPageSize);
        return (float)screenSize.width / (float)transformed.width;
    }

    /**
     * Given the visible rectangle that the user is viewing and the layer controller's page size,
     * returns the dimensions of the box that this corresponds to on the page.
     */
    public IntRect transformVisibleRect(IntRect layerVisibleRect, IntSize layerPageSize) {
        float zoomFactor = (float)layerPageSize.width / (float)mPageSize.width;
        return layerVisibleRect.scaleAll(1.0f / zoomFactor);
    }

    /**
     * Given the visible rectangle that the user is viewing and the layer controller's page size,
     * returns the dimensions in layer coordinates that this corresponds to.
     */
    public IntRect untransformVisibleRect(IntRect viewportVisibleRect, IntSize layerPageSize) {
        float zoomFactor = (float)layerPageSize.width / (float)mPageSize.width;
        return viewportVisibleRect.scaleAll(zoomFactor);
    }

    public IntSize getPageSize() { return mPageSize; }
    public void setPageSize(IntSize pageSize) { mPageSize = pageSize; }
    public IntRect getVisibleRect() { return mVisibleRect; }
    public void setVisibleRect(IntRect visibleRect) { mVisibleRect = visibleRect; }
}

