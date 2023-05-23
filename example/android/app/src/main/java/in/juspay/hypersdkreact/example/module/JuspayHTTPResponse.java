/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package in.juspay.hypersdkreact.example.module;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

public class JuspayHTTPResponse {
    private final int responseCode;
    private final Map<String, List<String>> headers;
    private final String responsePayload;

    public JuspayHTTPResponse(int responseCode, @NonNull String responsePayload, Map<String, List<String>> headers) {
        this.responseCode = responseCode;
        this.responsePayload = responsePayload;
        this.headers = headers;
    }

    public JuspayHTTPResponse(HttpURLConnection connection) throws IOException {
        this.responseCode = connection.getResponseCode();
        this.headers = connection.getHeaderFields();
        InputStreamReader responseReader;
        if ((this.responseCode < 200 || this.responseCode >= 300) && this.responseCode != 302) {
            responseReader = new InputStreamReader(connection.getErrorStream());
        } else {
            responseReader = new InputStreamReader(connection.getInputStream());
        }

        BufferedReader in = new BufferedReader(responseReader);
        StringBuilder response = new StringBuilder();

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }

        in.close();
        this.responsePayload = response.toString();
        connection.disconnect();
    }

    @NonNull
    public String toString() {
        JSONObject object = new JSONObject();

        try {
            object.put("responseCode", this.responseCode);
            object.put("responsePayload", this.responsePayload);
            object.put("headers", this.headers);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return object.toString();
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponsePayload() {
        return responsePayload;
    }
}
