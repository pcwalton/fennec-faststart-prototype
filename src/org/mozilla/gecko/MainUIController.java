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

package org.mozilla.gecko;

import org.mozilla.gecko.FakeGeckoLayerClient;
import org.mozilla.gecko.gfx.FloatPoint;
import org.mozilla.gecko.gfx.IntPoint;
import org.mozilla.gecko.gfx.IntSize;
import org.mozilla.gecko.gfx.LayerClient;
import org.mozilla.gecko.gfx.LayerController;
import org.mozilla.gecko.gfx.PlaceholderLayerClient;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Debug;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import java.io.IOException;

public class MainUIController {
    private Activity mActivity;
    private LayerClient mLayerClient;
    private LayerController mLayerController;
    private View outerView;

    public MainUIController(Activity activity) {
        mActivity = activity;
        build();
    }

    public Context getContext() { return mActivity; }

    /* Constructs the UI. */
    private void build() {
        //mActivity.requestWindowFeature(Window.FEATURE_NO_TITLE);

        AwesomeBarController awesomeBarController = new AwesomeBarController(this);

        PlaceholderLayerClient layerClient = PlaceholderLayerClient.createInstance(mActivity);
        mLayerController = new LayerController(mActivity, layerClient);
        if (layerClient != null)
            layerClient.init();

        View contentView = mLayerController.getView();
        LinearLayout.LayoutParams contentViewLayout =
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                                          ViewGroup.LayoutParams.FILL_PARENT);
        contentViewLayout.weight = 1.0f;
        contentView.setLayoutParams(contentViewLayout);

        mLayerController.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View view, MotionEvent event) {
                IntPoint viewPoint = new IntPoint((int)Math.round(event.getX()),
                                                  (int)Math.round(event.getY()));
                if (viewPoint == null)
                    return false;

                FloatPoint layerPoint = mLayerController.convertViewPointToLayerPoint(viewPoint);
                Log.e("Fennec", "### Layer point: " + layerPoint);
                return true;
            }
        });

        LinearLayout outerLayout = new LinearLayout(mActivity);
        outerLayout.setOrientation(LinearLayout.VERTICAL);
        outerLayout.addView(awesomeBarController.getAwesomeBar());
        outerLayout.addView(contentView);

        outerView = outerLayout;
    }

    public View getOuterView() { return outerView; }

    public void start() { /* TODO */ }

    public void createMenu(Menu menu) {
        menu.add("Load Mock Gecko").setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                FakeGeckoLayerClient geckoLayerClient = new FakeGeckoLayerClient();
                mLayerController.setLayerClient(geckoLayerClient);
                geckoLayerClient.init();
                return true;
            }
        });

        menu.add("Set Page Size").setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final EditText editText = new EditText(mActivity);
                new AlertDialog.Builder(mActivity)
                    .setTitle("Set Page Size")
                    .setMessage("Enter a new page size, space-separated (e.g. 320 240):")
                    .setView(editText)
                    .setPositiveButton("OK", new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String[] strings = editText.getText().toString().split("\\s+");
                            IntSize newSize = new IntSize(Integer.parseInt(strings[0]),
                                                          Integer.parseInt(strings[1]));
                            mLayerClient.setPageSize(newSize);
                        }
                    })
                    .show();

                return true;
            }
        });

        menu.add("Dump Heap").setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                try {
                    Debug.dumpHprofData("/sdcard/fennec.hprof");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
        });

        menu.add("Quit").setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                System.exit(0);
                return true;
            }
        });
    }
}

