package mx.itesm.logistics.vehicle_tracking.model;

import android.os.Parcel;

import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import edu.mit.lastmite.insight_library.model.Location;
import edu.mit.lastmite.insight_library.model.User;

public class Driver extends User {
    public static final String JSON_WRAPPER = "driver";
    public static final String JSON_ID = "id_driver";

    public Driver() {

    }

    public Driver(JSONObject json) throws JSONException {
        JSONObject object = json;
        if (json.has(JSON_WRAPPER)) {
            object = json.getJSONObject(JSON_WRAPPER);
        }

        if (object.has(JSON_EMAIL) && !object.isNull(JSON_EMAIL)) {
            mEmail = object.getString(JSON_EMAIL);
        }

        if (object.has(JSON_ID) && !object.isNull(JSON_ID)) {
            mId = object.getLong(JSON_ID);
        }

        if (object.has(JSON_COMPANY_ID) && !object.isNull(JSON_COMPANY_ID)) {
            mCompanyId = object.getLong(JSON_COMPANY_ID);
        }

        if (object.has(JSON_ACCESS_TOKEN) && !object.isNull(JSON_ACCESS_TOKEN)) {
            mAccessToken = object.getString(JSON_ACCESS_TOKEN);
        }

        if (object.has(JSON_PASSWORD)  && !object.isNull(JSON_PASSWORD)) {
            mPassword = object.getString(JSON_PASSWORD);
        }
    }

    public Driver(Parcel in) {
        mId = in.readLong();
        mAccessToken = in.readString();
        mEmail = in.readString();
        mCompanyId = in.readLong();
        mPassword = in.readString();
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject wrapper = new JSONObject();
        wrapper.put(JSON_WRAPPER, toJSONWithoutWrapper());
        return wrapper;
    }

    public JSONObject toJSONWithoutWrapper() throws JSONException {
        JSONObject object = new JSONObject();
        object.put(JSON_ID, mId);
        object.put(JSON_ACCESS_TOKEN, mAccessToken);
        object.put(JSON_EMAIL, mEmail);
        object.put(JSON_COMPANY_ID, mCompanyId);
        object.put(JSON_PASSWORD, mPassword);

        return object;
    }

    public RequestParams buildParams() {
        HashMap<String, Object> object = new HashMap<>();
        object.put(JSON_ID, mId);
        object.put(JSON_ACCESS_TOKEN, mAccessToken);
        object.put(JSON_EMAIL, mEmail);
        object.put(JSON_COMPANY_ID, mCompanyId);
        object.put(JSON_PASSWORD, mPassword);

        RequestParams params = new RequestParams();
        params.put(JSON_WRAPPER, object);
        return params;
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(mId);
        out.writeString(mAccessToken);
        out.writeString(mEmail);
        out.writeLong(mCompanyId);
        out.writeString(mPassword);
    }

    public static final Creator CREATOR = new Creator() {
        public Driver createFromParcel(Parcel in) {
            return new Driver(in);
        }

        public Driver[] newArray(int size) {
            return new Driver[size];
        }
    };
}