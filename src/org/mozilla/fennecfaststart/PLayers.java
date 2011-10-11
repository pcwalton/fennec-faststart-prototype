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

import org.mozilla.fennecfaststart.PLayer;
import android.graphics.Bitmap;
import java.nio.ByteBuffer;

// Converted from the IPDL at gfx/layers/ipc/PLayers.ipdl.
public abstract class PLayers {
    public static class nsIntRect { public int x, y, width, height; }
    public static class nsIntRegion { public nsIntRect rect; /* TODO */ }
    public static class gfx3DMatrix {
        public float _11, _12, _13, _14;
        public float _21, _22, _23, _24;
        public float _31, _32, _33, _34;
        public float _41, _42, _43, _44;
    }

    // The basic IPDL Shmem type.
    public static abstract class Shmem {
        public ByteBuffer buffer;
    }

    public static class SharedImageShmem extends Shmem {
        public int width, height, format;

        // Shmem uses Cairo image formats internally.
        static final int FORMAT_INVALID = -1;
        static final int FORMAT_ARGB32 = 0;
        static final int FORMAT_RGB24 = 1;
        static final int FORMAT_A8 = 2;
        static final int FORMAT_A1 = 3;
        static final int FORMAT_RGB16_565 = 4;
    }

    public static abstract class Edit {}
    public static class OpCreateImageLayer extends Edit { public PLayer layer; }

    public static abstract class SharedImage {}
    public static class SurfaceDescriptor extends SharedImage {
        public SharedImageShmem shmem;
    }

    public static class CommonLayerAttributes {
        nsIntRegion visibleRegion;
        gfx3DMatrix transform;
        int contentFlags;
        float opacity;
        boolean useClipRect;
        nsIntRect clipRect;
        boolean useTileSourceRect;
        nsIntRect tileSourceRect;
        boolean isFixedPosition;
    }

    public static class LayerAttributes {
        public CommonLayerAttributes common;
        // public SpecificLayerAttributes specific;
    }

    public static class OpSetLayerAttributes extends Edit {
        public PLayer layer;
        public LayerAttributes attrs;
    }

    // Monkey with the tree structure
    public static class OpSetRoot extends Edit     { public PLayer root; }
    public static class OpInsertAfter extends Edit { public PLayer container, childLayer, after; }
    public static class OpAppendChild extends Edit { public PLayer container, childLayer; }
    public static class OpRemoveChild extends Edit { public PLayer container, childLayer; }

    // Paint (buffer update)
    public static class OpPaintImage extends Edit {
        PLayer layer;
        SharedImage newFrontBuffer;
    }

    // Replies to operations
    public static abstract class EditReply {}
    public static class OpImageSwap extends EditReply {
        public PLayer layer;
        public SharedImage newBackImage;
    }

    public abstract EditReply[] update(Edit[] cset);
}

