package in.juspay.hypersdkreact;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.Event;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HyperOnEvent extends Event<HyperOnEvent> {

    private final String event;
    private final JSONObject data;
    public static String EVENT_NAME = "topHyperEvent";

    public HyperOnEvent(String event, JSONObject data) {
        this.data = data;
        this.event = event;
    }
    @NonNull
    @Override
    public String getEventName() {
        return EVENT_NAME;
    }

    @Nullable
    @Override
    protected WritableMap getEventData() {
        WritableMap map = Arguments.createMap();
        try {
            map.putMap("data", Arguments.makeNativeMap(jsonToMap(data)));
        } catch (JSONException e) {
            System.out.println("JSON EXCEPTION" + e);
        }
        map.putString("event", event);
        return map;
    }

    private static Map<String, Object> jsonToMap(JSONObject json) throws JSONException {
        Map<String, Object> map = new HashMap<>();

        // Iterate over the keys in the JSONObject
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = json.get(key);

            // Recursively convert if the value is a JSONObject or JSONArray
            if (value instanceof JSONObject) {
                map.put(key, jsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                map.put(key, jsonToList((JSONArray) value));
            } else {
                // For other types, add directly to the map
                map.put(key, value);
            }
        }
        return map;
    }

    private static Object jsonToList(JSONArray array) throws JSONException {
        Object[] list = new Object[array.length()];

        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);

            // Recursively convert if the value is a JSONObject or JSONArray
            if (value instanceof JSONObject) {
                list[i] = jsonToMap((JSONObject) value);
            } else if (value instanceof JSONArray) {
                list[i] = jsonToList((JSONArray) value);
            } else {
                // For other types, add directly to the list
                list[i] = value;
            }
        }
        return list;
    }
}
