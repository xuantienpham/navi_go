package navi_go.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;

public class SpeederView extends View {
    // ===========================================================
    // Constants
    // ===========================================================

    public static final int NO_SPEED = -1;
    // ===========================================================
    // Fields
    // ===========================================================

    private int mSpeed;
    private int mMaxSpeed;

    /**
     * Paint settings.
     */
    //protected Paint mPaint = new Paint();

    // ===========================================================
    // Constructors
    // ===========================================================

    public SpeederView(Context context) {
        super(context);
    }

    public SpeederView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
    }


    public SpeederView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }


    public SpeederView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    private void initSpeeder() {
        mSpeed = SpeederView.NO_SPEED;
        mMaxSpeed = SpeederView.NO_SPEED;
    }
}
