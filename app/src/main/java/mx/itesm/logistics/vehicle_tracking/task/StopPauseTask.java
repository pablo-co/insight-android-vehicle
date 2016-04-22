/*
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * Created by Pablo Cárdenas on 25/10/15.
 */

package mx.itesm.logistics.vehicle_tracking.task;

import android.util.Log;

import edu.mit.lastmite.insight_library.http.APIResponseHandler;
import edu.mit.lastmite.insight_library.model.Pause;
import edu.mit.lastmite.insight_library.task.NetworkTask;
import mx.itesm.logistics.vehicle_tracking.util.Preferences;

public class StopPauseTask extends NetworkTask {
    protected Pause mPause;

    public StopPauseTask(Pause Pause) {
        mPause = Pause;
    }

    @Override
    public void execute(Callback callback) {
        mCallback = callback;
        updatePause();
        Log.d("PauseEND", mPause.buildParams().toString());
        mAPIFetch.post("pauses/postEndpause", mPause.buildParams(), new APIResponseHandler(mApplication, null, false) {
            @Override
            public void onFinish(boolean success) {
                activateCallback(success);
            }
        });
    }

    @Override
    public Object getModel() {
        return mPause;
    }

    protected void updatePause() {
        mPause.setId(getPauseId());
    }

    protected long getPauseId() {
        return getGlobalLong(Preferences.PREFERENCES_PAUSE_ID);
    }
}
