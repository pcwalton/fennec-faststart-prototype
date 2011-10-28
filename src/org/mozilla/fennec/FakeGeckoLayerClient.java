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

import org.mozilla.fennec.gfx.BufferedCairoImage;
import org.mozilla.fennec.gfx.CairoImage;
import org.mozilla.fennec.gfx.IntRect;
import org.mozilla.fennec.gfx.IntSize;
import org.mozilla.fennec.gfx.LayerClient;
import org.mozilla.fennec.gfx.LayerController;
import org.mozilla.fennec.gfx.SingleTileLayer;
import org.mozilla.fennec.ui.ViewportController;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.Log;
import java.nio.ByteBuffer;

public class FakeGeckoLayerClient extends LayerClient {
    private Bitmap mBitmap;
    private ByteBuffer mBuffer;
    private AsyncTask<Object,Object,BufferedCairoImage> mRenderTask;
    private SingleTileLayer mTileLayer;
    private ViewportController mViewportController;

    private static final int PAGE_WIDTH = 980;
    private static final int PAGE_HEIGHT = 2500;

    public FakeGeckoLayerClient() {
        super();
        mViewportController = new ViewportController(new IntSize(PAGE_WIDTH, PAGE_HEIGHT),
                                                     new IntRect(0, 0, 1, 1));
    }

    @Override
    public void init() {
        mViewportController.setVisibleRect(getLayerController().getVisibleRect());

        mTileLayer = new SingleTileLayer();
        getLayerController().setRoot(mTileLayer);

        mBuffer = ByteBuffer.allocateDirect(LayerController.TILE_SIZE * LayerController.TILE_SIZE *
                                            2);
        mBitmap = Bitmap.createBitmap(LayerController.TILE_SIZE, LayerController.TILE_SIZE,
                                      Bitmap.Config.RGB_565);

        render();
    }

    @Override
    protected void render() {
        if (mRenderTask != null) {
            mRenderTask.cancel(true);
            mRenderTask = null;
        }

        mRenderTask = new AsyncTask<Object,Object,BufferedCairoImage>() {
            private IntRect mViewportRect;

            protected BufferedCairoImage doInBackground(Object... args) {
                mViewportRect = mViewportController.clampRect(getTransformedVisibleRect());
                float zoomFactor = getZoomFactor();

                Canvas canvas = new Canvas(mBitmap);
                canvas.drawRGB(255, 255, 255);

                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setColor(Color.BLACK);
                paint.setTextSize(12.0f * zoomFactor);

                for (int i = 0; i < TEXT.length; i++) {
                    canvas.drawText(TEXT[i],
                                    (12.0f - mViewportRect.x) * zoomFactor,
                                    (12.0f + 12.0f + 14.0f * i - mViewportRect.y) * zoomFactor,
                                    paint);

                    if (isCancelled())
                        return null;
                }

                mBitmap.copyPixelsToBuffer(mBuffer.asIntBuffer());
                return new BufferedCairoImage(mBuffer, LayerController.TILE_SIZE,
                                              LayerController.TILE_SIZE,
                                              CairoImage.FORMAT_RGB16_565);
            }

            protected void onPostExecute(BufferedCairoImage image) {
                LayerController controller = getLayerController();
                controller.unzoom();
                controller.notifyViewOfGeometryChange();

                mViewportController.setVisibleRect(mViewportRect);

                IntRect viewportRect = mViewportController.clampRect(mViewportRect);
                IntRect layerRect = mViewportController.untransformVisibleRect(viewportRect,
                                                                               getPageSize());
                mTileLayer.origin = layerRect.getOrigin();
                mTileLayer.paintImage(image);
            }
        };

        mRenderTask.execute();
    }

    @Override
    public IntSize getPageSize() {
        return mViewportController.getPageSize();
    }

    @Override
    public void geometryChanged() {
        mViewportController.setVisibleRect(getTransformedVisibleRect());
        render();
    }

    /* Returns the dimensions of the box in page coordinates that the user is viewing. */
    private IntRect getTransformedVisibleRect() {
        LayerController layerController = getLayerController();
        return mViewportController.transformVisibleRect(layerController.getVisibleRect(),
                                                        layerController.getPageSize());
    }

    private float getZoomFactor() {
        LayerController layerController = getLayerController();
        return mViewportController.getZoomFactor(layerController.getVisibleRect(),
                                                 layerController.getPageSize(),
                                                 layerController.getScreenSize());
    }

    private static final String[] TEXT = {
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Cras ut tortor velit, eget volutpat erat. Duis non leo neque. Maecenas tincidunt, nunc eget dapibus consequat, nisl diam elementum eros, non",
        "bibendum lectus eros eget mi. Nam consequat cursus laoreet. Suspendisse potenti. Curabitur et diam elit. Ut turpis lectus, porta ut eleifend ut, laoreet id nibh. Nam leo metus, egestas a ornare eget,",
        "consectetur quis nulla. Phasellus sit amet enim orci. Suspendisse et risus nec felis faucibus sollicitudin.",
        "",
        "Pellentesque turpis urna, porttitor quis posuere sed, ornare sed tortor. Aliquam erat volutpat. Quisque ullamcorper venenatis orci, id dictum tellus pretium eget. Maecenas laoreet porttitor nisi,",
        "egestas rhoncus libero sodales vel. Donec sit amet euismod diam. Aliquam odio magna, sodales vel interdum nec, faucibus vel velit. Fusce venenatis elit malesuada tellus tristique facilisis. Nullam",
        "scelerisque mi non eros porta eget malesuada est adipiscing. Etiam a urna massa. Curabitur dictum orci id enim vestibulum eget venenatis metus interdum.",
        "",
        "Nulla facilisi. Maecenas vestibulum arcu sed justo commodo sed pretium urna dictum. Aliquam quis ipsum et justo aliquet congue. Pellentesque massa risus, lacinia id consequat vel, laoreet non nisi.",
        "Donec eget sollicitudin odio. Suspendisse potenti. Sed neque quam, ornare quis pellentesque quis, euismod sed diam. Sed mollis tortor sed nisi sagittis non fermentum libero condimentum.",
        "",
        "Nam justo nisi, luctus auctor porttitor in, fringilla ac nisi. Mauris a dolor dolor, pulvinar accumsan lacus. Vivamus lobortis enim eget mauris fermentum vel cursus turpis fringilla. Donec vel felis",
        "diam. Morbi et quam purus, eu semper tortor. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Nulla at faucibus metus. Vivamus rutrum ultricies erat. In volutpat,",
        "nisi et venenatis aliquet, felis orci dignissim leo, id ullamcorper ligula elit eget quam. Mauris gravida lobortis elementum. Fusce ut mauris dolor. Sed libero diam, luctus vitae interdum vel, tempus",
        "ac urna. Aliquam a augue dui, ut laoreet quam. Aliquam diam libero, vestibulum at dictum at, varius eu lorem. Ut lectus risus, aliquet in hendrerit sed, pharetra vel lacus. Sed sed quam arcu, vel",
        "feugiat augue.",
        "",
        "Pellentesque eleifend magna et lacus euismod ac sagittis velit aliquet. Pellentesque vitae dui massa. Ut ac pulvinar magna. Fusce vel odio dolor, in hendrerit diam. Donec ut tellus sem. Donec sapien",
        "sem, elementum ut scelerisque non, aliquet id lacus. Sed non diam metus, eu sodales mauris. Quisque porttitor diam ut metus tempor in tristique quam accumsan. Curabitur pellentesque diam a dolor",
        "lacinia sodales. Sed hendrerit sapien id justo lacinia in dapibus quam elementum. Morbi bibendum, massa eget porttitor tempus, est mauris semper felis, id hendrerit diam quam id purus. Sed lobortis",
        "diam vestibulum mi lobortis pretium. Mauris at dictum ante. Nam nisi dolor, rhoncus ac suscipit et, condimentum eu lorem. Ut ut neque id diam auctor vehicula.",
        "",
        "Vivamus in magna dui. Curabitur aliquam euismod leo. Morbi auctor orci ac quam tincidunt tempor. Integer ut libero id nisl lacinia lobortis. Aliquam erat volutpat. Donec mattis porta nunc, in interdum",
        "ante vehicula dignissim. Nunc in libero rutrum nunc molestie tempus. Proin sagittis dictum felis. Maecenas blandit lectus sed eros sodales porttitor. Donec urna purus, convallis nec hendrerit eget,",
        "sagittis sed libero. Sed in tellus nibh. Nunc placerat lorem sit amet tortor lobortis et rhoncus nulla faucibus. Nunc suscipit ultricies lacus tincidunt ullamcorper. Nunc pretium pharetra arcu eget",
        "ultrices.",
        "",
        "Aliquam at neque lectus, sed pharetra diam. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur ornare faucibus ipsum ac euismod. Phasellus porttitor magna vel lectus vulputate",
        "pulvinar. Etiam vulputate fermentum tellus, et aliquet libero semper vitae. Aliquam vitae nisl sit amet orci sollicitudin varius ac id enim. Nam elit nisl, sodales viverra fringilla a, sollicitudin",
        "vel sem. Curabitur quis urna et enim ultrices dictum id ac dolor. Aenean non tortor in mauris vestibulum lobortis. Pellentesque consequat lectus eget eros mollis aliquam. In odio sapien, scelerisque",
        "rutrum elementum pulvinar, ultricies vel tellus. Vestibulum hendrerit est ac urna sollicitudin vitae feugiat lectus vestibulum. Nam et arcu mi. Nulla tincidunt accumsan fermentum. Sed venenatis justo",
        "orci, et pulvinar ante. Nulla ac enim neque, eu fringilla risus.",
        "",
        "Mauris turpis augue, vestibulum at vehicula quis, condimentum vel elit. Nullam sollicitudin tincidunt risus ut volutpat. Cras sed ligula et dolor accumsan tempus a nec leo. Nulla hendrerit tristique",
        "urna, dignissim condimentum velit pulvinar nec. Donec tristique posuere tellus. Nunc porta venenatis sem, a pretium neque consequat ac. Ut eu nisl lectus. Aenean feugiat, sem adipiscing tincidunt",
        "tincidunt, nisi lectus facilisis felis, sit amet rhoncus mi neque ac justo. Vivamus luctus faucibus facilisis. Praesent ut tortor quis lectus laoreet convallis.",
        "",
        "Donec malesuada tempor vehicula. Etiam eleifend ipsum a neque vulputate quis pretium nunc vehicula. Integer sed rhoncus ante. Cras quis arcu eu nibh dapibus viverra. Vestibulum ante ipsum primis in",
        "faucibus orci luctus et ultrices posuere cubilia Curae; Quisque suscipit pellentesque lacinia. Maecenas porta nunc eget tortor porta posuere. Aenean in eros leo. In urna tortor, suscipit a aliquet ac,",
        "cursus ut eros. Phasellus pellentesque, libero in dignissim accumsan, est quam dictum massa, a tristique nulla enim aliquam nisi. Mauris pulvinar rhoncus odio, vel ultricies velit ultricies non. Nulla",
        "hendrerit, massa eget gravida pellentesque, nisl odio vestibulum magna, vitae rhoncus velit lacus vel nisl.",
        "",
        "Maecenas nisi mi, mollis ornare varius nec, porta vel massa. In pretium volutpat sapien quis interdum. Phasellus euismod, diam ut vulputate vehicula, elit odio ultrices ligula, a sodales nunc purus in",
        "metus. Nam sit amet libero mauris, et sodales orci. Morbi non placerat ligula. Aliquam lectus mi, mollis id porttitor non, adipiscing porta libero. Sed vestibulum posuere quam a molestie. Etiam",
        "gravida interdum molestie. Nam non quam vel ipsum lacinia commodo ut at enim.",
        "",
        "Sed adipiscing tempor purus quis lacinia. Mauris id porta quam. Ut porta, nunc ut aliquam commodo, eros nunc tincidunt elit, id consectetur enim quam eu nibh. Donec sit amet libero ut leo ornare",
        "fringilla vel vel ante. Cras at nisi magna, nec pretium lacus. Maecenas fermentum nisl tincidunt purus blandit eu adipiscing orci volutpat. Sed at accumsan lorem. Nam feugiat magna vel elit semper",
        "scelerisque. Donec metus elit, lacinia quis pellentesque et, rutrum volutpat dui.",
        "",
        "Duis id felis id massa sodales rutrum. Ut est libero, iaculis a bibendum sit amet, commodo a lacus. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed varius nunc eget lorem pretium non",
        "rhoncus est blandit. Morbi velit leo, convallis id posuere facilisis, condimentum ac justo. Praesent at nunc at risus convallis cursus a feugiat elit. In arcu urna, sodales eu molestie faucibus,",
        "vehicula sagittis dolor. Cras in fermentum quam. Curabitur nisi elit, dignissim quis condimentum nec, iaculis non purus. Fusce dui est, fermentum vel convallis in, accumsan ac lorem.",
        "",
        "Nunc fermentum fringilla massa eget suscipit. Donec nec nulla eu dui suscipit porta eget in tellus. Sed ac metus lacus, vitae sagittis nulla. Fusce faucibus cursus nibh eu laoreet. Aliquam eros",
        "lectus, lobortis ac molestie facilisis, interdum a ipsum. Nulla ut magna risus, ut bibendum est. Sed ullamcorper vehicula convallis. Quisque a quam in quam viverra imperdiet. Phasellus gravida orci",
        "sit amet neque vestibulum non fermentum leo varius. Cras vitae lacus in arcu imperdiet molestie. Fusce eget lectus eu magna placerat lacinia. Ut molestie sem pellentesque metus imperdiet vestibulum.",
        "",
        "Pellentesque feugiat, metus at condimentum vulputate, elit odio convallis leo, eget gravida dui dolor sit amet justo. Donec sollicitudin sodales magna, blandit mattis lectus interdum eget. Sed justo",
        "enim, venenatis id varius sit amet, tempus pulvinar arcu. Maecenas ultrices fringilla arcu, at vulputate metus tristique nec. Mauris quis metus sollicitudin erat eleifend lacinia. Integer lacinia",
        "sapien in dolor ornare tempus. Nulla augue augue, bibendum sit amet dictum sed, ultricies eget arcu.",
        "",
        "Mauris sed dapibus neque. Donec est urna, blandit at vestibulum at, sagittis euismod lorem. Aliquam erat volutpat. Nullam vel augue vitae neque tristique venenatis vel interdum turpis. Maecenas mollis",
        "volutpat ullamcorper. Mauris placerat, nisl ut interdum dignissim, massa lectus aliquam sem, vel adipiscing justo diam et tellus. Aenean aliquam nibh non dolor ultrices commodo. Nullam laoreet, lacus",
        "sed sodales tincidunt, metus dui mollis arcu, at porta sapien mi vel tortor. Donec nunc quam, pulvinar vitae tincidunt at, blandit eu dui. Proin nulla tortor, semper vitae convallis vehicula, pharetra",
        "non diam. Curabitur erat quam, bibendum eget porttitor et, viverra in dui.",
        "",
        "Etiam rutrum dui vel nisi faucibus fermentum. Curabitur porta diam ut purus faucibus fringilla. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Morbi nunc",
        "nulla, viverra at accumsan a, eleifend quis massa. Donec pretium hendrerit tempus. Ut pellentesque ipsum quis sapien consectetur consequat. Curabitur ullamcorper, tortor non iaculis tincidunt, turpis",
        "risus aliquet lorem, in tincidunt nulla nunc condimentum orci. Mauris metus lacus, feugiat ac semper vitae, porttitor ut velit. Mauris ultricies diam ac lectus rhoncus convallis. Etiam fringilla dolor",
        "eu ante iaculis non molestie lacus ultricies. Proin sagittis, neque a interdum egestas, metus massa viverra nisl, nec scelerisque nulla nunc eu nisi. Nam scelerisque, sapien vel fringilla pharetra,",
        "ante nisl tristique lacus, ut bibendum enim sem sed leo. Proin varius vestibulum semper. Mauris vel leo nisi.",
        "",
        "Curabitur id laoreet dui. Phasellus pulvinar eros ac urna lacinia quis luctus est cursus. Curabitur sit amet elit magna. Vivamus rhoncus rutrum est eu elementum. Aenean vel lorem non justo blandit",
        "tempus. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Maecenas id orci urna, a malesuada magna. Donec ultrices risus ac nisi malesuada ullamcorper ac in",
        "sapien. Fusce eget massa quis mauris iaculis interdum.",
        "",
        "Fusce vitae lectus ut ante sollicitudin commodo. Phasellus et mauris justo, vel luctus lectus. Maecenas velit eros, adipiscing at feugiat nec, venenatis et velit. Fusce tortor sapien, laoreet sit amet",
        "sollicitudin quis, fringilla quis sem. Nunc ullamcorper pretium fringilla. Phasellus ante eros, pharetra nec commodo vel, posuere sed turpis. Curabitur eu molestie massa. Nullam in massa est.",
        "Pellentesque erat mauris, iaculis sed venenatis id, cursus eget odio. Sed ornare adipiscing lacus vel malesuada. Maecenas suscipit molestie purus, ac eleifend erat luctus eget. Donec tortor velit,",
        "fermentum sed dignissim ac, imperdiet vel metus. Curabitur eget enim libero.",
        "",
        "Phasellus sit amet neque ac odio congue eleifend id non diam. Phasellus porttitor, elit laoreet pretium scelerisque, felis lacus eleifend lorem, in sodales elit enim nec orci. Aenean elementum purus",
        "eget mi eleifend ut dignissim nibh molestie. Aenean non elit ligula, vitae aliquet massa. Nam felis arcu, porttitor ac dapibus id, elementum in elit. Ut elit lorem, rutrum ut ultricies quis, mollis",
        "nec dolor. Duis nec tellus ac magna tempus scelerisque in eu enim. Morbi pharetra quam quis neque facilisis vel viverra nisi ultricies.",
        "",
        "Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Pellentesque odio turpis, condimentum sit amet dapibus sed, sagittis vel libero. Sed consectetur arcu",
        "ullamcorper est sagittis vestibulum. Pellentesque placerat, arcu vel facilisis tempus, ante dui consequat libero, non commodo metus urna nec lorem. Sed magna tellus, fermentum id convallis ultricies,",
        "interdum in nisi. Vestibulum nisi nisl, porta vel fermentum vel, ultricies vitae ligula. Quisque sapien justo, lacinia eu sodales sed, cursus eget enim. Vestibulum ultricies, ipsum vitae scelerisque",
        "laoreet, sapien leo ullamcorper justo, id elementum velit erat ut nisi."
    };
}

