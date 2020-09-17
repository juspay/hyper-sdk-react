package com.example.hypersdkreact;

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
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public class HyperAPIUtils extends ReactContextBaseJavaModule {

  private static final String LOG_TAG = "HyperAPIUtils";
  private ReactContext reactContext;

  HyperAPIUtils(ReactApplicationContext reactContext) {
    this.reactContext = reactContext;
  }

  @NonNull
  @Override
  public String getName() {
    return "HyperAPIUtils";
  }

  @ReactMethod
  public void createCustomer(String customerId, String mobile, String email, String apiKey, Promise promise) {
    try {
      String url = "https://sandbox.juspay.in" + "/customers";
      String auth = Base64.encodeToString((apiKey + ":").getBytes(), Base64.DEFAULT);
      JuspayHTTPResponse response;

      HttpsURLConnection connection = (HttpsURLConnection) (new URL(url).openConnection());
      connection.setRequestMethod("POST");
      connection.setSSLSocketFactory(new TLSSocketFactory());
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
      initializeSSLContext(reactContext);
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

      connection.setSSLSocketFactory(new TLSSocketFactory());
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
      initializeSSLContext(reactContext);
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

  private static String generateQueryString(Map<String, String> queryString) throws UnsupportedEncodingException {
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

  private String toCurlRequest(HttpsURLConnection connection, byte[] body) {
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
    if (body != null)
      builder.append("-d '").append(new String(body)).append("' \\\n  ");


    return builder.toString();
  }

  private static void initializeSSLContext(Context mContext) {
    try {
      SSLContext.getInstance("TLSv1.2");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    try {
      ProviderInstaller.installIfNeeded(mContext.getApplicationContext());
    } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
      e.printStackTrace();
    }
  }

}
