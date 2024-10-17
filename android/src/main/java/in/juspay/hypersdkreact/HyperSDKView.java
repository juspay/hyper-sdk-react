package in.juspay.hypersdkreact;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.uimanager.ThemedReactContext;

import in.juspay.services.HyperServices;

public class HyperSDKView extends FrameLayout {
    private HyperServices hyperServices;

    public HyperSDKView(@NonNull ThemedReactContext context) {
        super(context);
        this.hyperServices = new HyperServices(context);
    }

    public HyperSDKView(@NonNull Context context) {
        super(context);
        this.hyperServices = new HyperServices(context);
    }

    public HyperSDKView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.hyperServices = this.hyperServices = new HyperServices(context);;
    }

    public HyperSDKView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.hyperServices = new HyperServices(context);
    }

    public HyperSDKView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.hyperServices = new HyperServices(context);
    }

    public HyperServices getHyperServices() {
        return hyperServices;
    }
}
