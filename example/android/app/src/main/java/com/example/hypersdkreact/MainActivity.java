package com.example.hypersdkreact;

import android.os.Bundle;
import android.webkit.WebView;

import com.facebook.react.ReactActivity;

import in.juspay.hypersdkreact.HyperSdkReactModule;

public class MainActivity extends ReactActivity {

  /**
   * Returns the name of the main component registered from JavaScript. This is used to schedule
   * rendering of the component.
   */
  @Override
  protected String getMainComponentName() {
    return "HyperSdkReactExample";
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    WebView.setWebContentsDebuggingEnabled(true);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    HyperSdkReactModule.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }
}
