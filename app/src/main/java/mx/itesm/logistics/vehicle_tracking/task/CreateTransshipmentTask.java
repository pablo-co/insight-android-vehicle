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

import org.apache.http.Header;
import org.json.JSONObject;

import edu.mit.lastmite.insight_library.http.APIResponseHandler;
import edu.mit.lastmite.insight_library.task.NetworkTask;
import mx.itesm.logistics.vehicle_tracking.model.Transshipment;
import mx.itesm.logistics.vehicle_tracking.util.Preferences;

public class CreateTransshipmentTask extends NetworkTask {
    protected Transshipment mTransshipment;

    public CreateTransshipmentTask(Transshipment transshipment) {
        mTransshipment = transshipment;
    }

    @Override
    public void execute(Callback callback) {
        mCallback = callback;
        updateTransshipment();

        Log.d("CREATEtrans", mTransshipment.buildParams().toString());
        mAPIFetch.post("transshipments/postTransshipment", mTransshipment.buildParams(), new APIResponseHandler(mApplication, null, false) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Transshipment transshipment = new Transshipment(response);
                    saveTransshipmentId(transshipment.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onFinish(boolean success) {
                activateCallback(success);
            }
        });
    }

    @Override
    public Object getModel() {
        return mTransshipment;
    }

    protected void updateTransshipment() {
        mTransshipment.setRouteId(getRouteId());
    }

    protected long getRouteId() {
        return getGlobalLong(Preferences.PREFERENCES_ROUTE_ID);
    }

    protected void saveTransshipmentId(long transshipmentId) {
        putGlobalLong(Preferences.PREFERENCES_TRANSSHIPMENT_ID, transshipmentId);
    }
}
