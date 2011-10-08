package org.mozilla.fennecfaststart;

import org.mozilla.fennecfaststart.MainUIController;
import android.app.Activity;
import android.os.Bundle;

public class FennecFaststartActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        MainUIController controller = new MainUIController(this);
        setContentView(controller.getOuterView());
        controller.start();
    }
}
