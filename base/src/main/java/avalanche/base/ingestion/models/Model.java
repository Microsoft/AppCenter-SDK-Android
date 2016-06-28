package avalanche.base.ingestion.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public interface Model {

    void read(JSONObject object) throws JSONException;

    void write(JSONStringer writer) throws JSONException;

    void validate() throws IllegalArgumentException;
}
