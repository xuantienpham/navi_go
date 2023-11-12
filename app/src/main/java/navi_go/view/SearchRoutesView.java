package navi_go.view;

import android.content.Context;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.osmandroid.R;

import navi_go.model.RequestCallback;
import navi_go.model.Result;

public class SearchRoutesView extends RelativeLayout implements RequestCallback<String> {
    private TextView lblLog;
    public SearchRoutesView(Context context) {
        super(context);
        //lblLog = findViewById(R.id.lblLog);
    }

    @Override
    public void onComplete(Result<String> result) {
        if(result instanceof Result.Success) {
            // Happy path

            String s = ((Result.Success<String>) result).data;
            lblLog.setText(s);

        } else {
            // Show error in UI
            Exception exc = ((Result.Error<String>) result).exception;
            String s = exc.toString();
            //String s = ((Result.Error<String>) result).exception.toString();
            //Toast.makeText(mainActivity.ctx, s, Toast.LENGTH_SHORT).show();
        }
    }
}
