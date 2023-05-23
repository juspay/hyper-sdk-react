/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package in.juspay.hypersdkreact.example.module;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

@ReactModule(name = HyperAPIUtils.NAME)
public class HyperAPIUtils extends ReactContextBaseJavaModule {
    static final String NAME = "HyperAPIUtils";
    private static final String LOG_TAG = NAME;

    private final ReactContext reactContext;

    HyperAPIUtils(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }


    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void createCustomer(String customerId, String mobile, String email, String apiKey, Promise promise) {
        try {
            String url = "https://sandbox.juspay.in" + "/customers";
            String auth = Base64.encodeToString((apiKey + ":").getBytes(), Base64.DEFAULT);
            JuspayHTTPResponse response;

            HttpsURLConnection connection = (HttpsURLConnection) (new URL(url).openConnection());
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Basic " + auth);
            connection.setRequestProperty("version", "2018-07-01");
            connection.setDoOutput(true);

            Map<String, String> payload = new HashMap<>();
            payload.put("object_reference_id", customerId);
            payload.put("mobile_number", mobile);
            payload.put("email_address", email);
            payload.put("first_name", "Harsh");
            payload.put("last_name", "Garg");
            payload.put("description", "Test Transaction");
            payload.put("options.get_client_auth_token", "true");

            Log.d(LOG_TAG, "cURL:\n" + toCurlRequest(connection, generateQueryString(payload).getBytes()));
            OutputStream stream = connection.getOutputStream();
            stream.write(generateQueryString(payload).getBytes());
            response = new JuspayHTTPResponse(connection);
            Log.d(LOG_TAG, "Customer: " + response.getResponsePayload());
            promise.resolve(response.getResponsePayload());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @ReactMethod
    public void generateOrder(String orderId, String orderAmount, String customerId, String mobile, String email, String apiKey, Promise promise) {
        try {
            String orderUrl = "https://sandbox.juspay.in" + "/order/create";
            String auth = Base64.encodeToString((apiKey + ":").getBytes(), Base64.DEFAULT);

            HttpsURLConnection connection = (HttpsURLConnection) (new URL(orderUrl).openConnection());

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Basic " + auth);
            connection.setRequestProperty("version", "2018-07-01");
            connection.setDoOutput(true);

            Map<String, String> payload = new HashMap<>();
            payload.put("order_id", orderId);
            payload.put("amount", orderAmount);
            payload.put("customer_id", customerId);
            payload.put("customer_email", email);
            payload.put("customer_phone", mobile);
            payload.put("return_url", "https://sandbox.juspay.in/end");
            payload.put("description", "Test Transaction");
            payload.put("options.get_client_auth_token", "true");

            Log.d(LOG_TAG, "cURL:\n" + toCurlRequest(connection, generateQueryString(payload).getBytes()));
            OutputStream stream = connection.getOutputStream();
            stream.write(generateQueryString(payload).getBytes());
            JuspayHTTPResponse response = new JuspayHTTPResponse(connection);
            Log.d(LOG_TAG, "Order: " + response.getResponsePayload());

            promise.resolve(response.getResponsePayload());
        } catch (Exception e) {
            e.printStackTrace();
            promise.reject(e);
        }
    }

    @NonNull
    private static String generateQueryString(@NonNull Map<String, String> queryString) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : queryString.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            String key = entry.getKey();
            String val = entry.getValue();
            sb.append(URLEncoder.encode(key, "UTF-8")).append("=").append(URLEncoder.encode(val, "UTF-8"));
        }
        return sb.toString();
    }

    @ReactMethod
    public void copyToClipBoard(String header, String message) {
        ClipboardManager clipboardManager = (ClipboardManager) reactContext.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(header, message);
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(clipData);
        }
    }

    @ReactMethod
    public void generateSign(String keyString, @NonNull String payload, @NonNull Promise promise) {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.decode(keyString, Base64.DEFAULT));
            PrivateKey privateKey = kf.generatePrivate(keySpecPKCS8);
            Signature privateSignature = Signature.getInstance("SHA256withRSA");
            privateSignature.initSign(privateKey);
            privateSignature.update(payload.getBytes(StandardCharsets.UTF_8));
            byte[] signature = privateSignature.sign();
            promise.resolve(Base64.encodeToString(signature, Base64.DEFAULT));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException |
                 SignatureException e) {
            e.printStackTrace();
            promise.reject("generateSign", "sign-failed");
        }
    }

    @NonNull
    private String toCurlRequest(@NonNull HttpsURLConnection connection, byte[] body) {
        StringBuilder builder = new StringBuilder("curl -v ");

        // Method
        builder.append("-X ").append(connection.getRequestMethod()).append(" \\\n  ");

        // URL
        builder.append("\"").append(connection.getURL()).append("\" \\\n");

        // Headers
        for (Map.Entry<String, List<String>> entry : connection.getRequestProperties().entrySet()) {
            builder.append("-H \"").append(entry.getKey()).append(":");
            for (String value : entry.getValue()) {
                builder.append(" ").append(value);
            }
            builder.append("\" \\\n  ");
        }

        // Body
        if (body != null) {
            builder.append("-d '").append(new String(body)).append("' \\\n  ");
        }

        return builder.toString();
    }
}
