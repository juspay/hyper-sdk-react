package in.juspay.hypersdkreact;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


public class MyFragment extends Fragment {
  CustomView customView;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
    super.onCreateView(inflater, parent, savedInstanceState);
    customView = new CustomView(this.requireContext());
    return customView; // this CustomView could be any view that you want to render
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    // do any logic that should happen in an `onCreate` method, e.g:
    // customView.onCreate(savedInstanceState);
  }

  @Override
  public void onPause() {
    super.onPause();
    // do any logic that should happen in an `onPause` method
    // e.g.: customView.onPause();
  }

  @Override
  public void onResume() {
    super.onResume();
    // do any logic that should happen in an `onResume` method
    // e.g.: customView.onResume();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    // do any logic that should happen in an `onDestroy` method
    // e.g.: customView.onDestroy();
  }
}
