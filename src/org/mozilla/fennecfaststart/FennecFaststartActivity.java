package org.mozilla.fennecfaststart;

import org.mozilla.fennec.MainUIController;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

public class FennecFaststartActivity extends Activity {
    private MainUIController mController;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mController = new MainUIController(this);
        setContentView(mController.getOuterView());
        mController.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mController.createMenu(menu);
        return true;
    }
}
