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

import org.mozilla.fennec.gfx.LayerController;
import org.mozilla.fennec.ipdl.nsIntRect;
import org.mozilla.fennec.ipdl.nsIntSize;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import java.util.Timer;

/*
 * Handles the kinetic scrolling and zooming physics for a layer controller.
 *
 * Joe Hewitt's Scrollability was used as a reference for much of this:
 *   https://github.com/joehewitt/scrollability/
 *
 * Thanks!
 */
public class PanZoomController {
    private LayerController mController;

    // The friction applied when decelerating.
    private final float FRICTION = 0.9925f;
    // If the velocity is below this threshold when the finger is released, animation stops.
    private final int STOPPED_THRESHOLD = 4;
    // The number of pixels the finger must move to determine horizontal or vertical motion.
    private final int LOCK_THRESHOLD = 10;
    // The percentage of the page which can be overscrolled before it must bounce back.
    private final float BOUNCE_LIMIT = 0.75f;
    // The rate of deceleration when the page has overscrolled and is slowing down before bouncing
    // back.
    private final float BOUNCE_DECEL_RATE = 0.01f;
    // The duration of animation when bouncing back.
    private final int BOUNCE_TIME = 240;
    private final int PAGE_BOUNCE_TIME = 160;

    private float mTouchX, mTouchY, mStartX, mStartY, mVelocityX, mVelocityY;
    private boolean mTouchMoved, mStopped;
    private long mLastTimestamp;
    private Timer mTakeoffTimer;

    public PanZoomController(LayerController controller) {
        mController = controller;
        mTakeoffTimer = new Timer("Takeoff");
    }

    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            return (event.getPointerCount() == 1) ? onTouchStart(event) : false;
        case MotionEvent.ACTION_MOVE:
            return (event.getPointerCount() == 1) ? onTouchMove(event) : false;
        case MotionEvent.ACTION_UP:
            return (event.getPointerCount() == 1) ? onTouchEnd(event) : false;
        default:
            return false;
        }
    }

    /*
     * Panning/scrolling
     */

    private boolean onTouchStart(MotionEvent event) {
        mTouchX = mStartX = event.getX(0); mTouchY = mStartY = event.getY(0);
        mTouchMoved = mStopped = false;
        // TODO: Hold timeout
        return true;
    }

    private boolean onTouchMove(MotionEvent event) {
        mTouchMoved = true;
        // TODO: Clear hold timeout
        track(event, System.currentTimeMillis());
        return true;
    }

    private boolean onTouchEnd(MotionEvent event) {
        takeoff(System.currentTimeMillis());
        return true;
    }

    private void track(MotionEvent event, long timestamp) {
        long timeStep = timestamp - mLastTimestamp;
        mLastTimestamp = timestamp;

        mVelocityX = mTouchX - event.getX(0); mVelocityY = mTouchY - event.getY(0);
        mTouchX = event.getX(0); mTouchY = event.getY(0);

        float absoluteVelocity = (float)Math.sqrt(mVelocityX * mVelocityX + mVelocityY * mVelocityY);
        mStopped = absoluteVelocity < STOPPED_THRESHOLD;

        nsIntSize pageSize = mController.getPageSize();
        nsIntRect visibleRect = mController.getVisibleRect();

        // Apply resistance along the edges.
        int excessX = 0, excessY = 0;
        if (visibleRect.x < 0)
            excessX = -visibleRect.x;
        else if (visibleRect.x + visibleRect.width > pageSize.width)
            excessX = visibleRect.x + visibleRect.width - pageSize.width;
        if (visibleRect.y < 0)
            excessY = -visibleRect.y;
        else if (visibleRect.y + visibleRect.height > pageSize.height)
            excessY = visibleRect.y + visibleRect.height - pageSize.height;

        if (excessX > 0)
            mVelocityX *= BOUNCE_LIMIT - (float)excessX / visibleRect.width;
        if (excessY > 0)
            mVelocityY *= BOUNCE_LIMIT - (float)excessY / visibleRect.height;

        displace();
    }

    private void takeoff(long timestamp) {
        long timeStep = timestamp - mLastTimestamp;
        mLastTimestamp = timestamp;

        if (mStopped)
            mVelocityX = mVelocityY = 0.0f;

        displace();

        // TODO
    }

    private void displace() {
        nsIntRect visibleRect = mController.getVisibleRect();
        mController.scrollTo((int)Math.round(visibleRect.x + mVelocityX),
                             (int)Math.round(visibleRect.y + mVelocityY));
    }

    /*
     * Zooming
     */

    public boolean onScale(ScaleGestureDetector detector) {
        // TODO
        return true;
    }

    public boolean onScaleBegin(ScaleGestureDetector detector) {
        // TODO
        return true;
    }

    public void onScaleEnd(ScaleGestureDetector detector) {
        // TODO
    }
}

