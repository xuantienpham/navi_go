package navi_go.view;

import android.content.Context;
import android.widget.RelativeLayout;

import navi_go.model.RequestCallback;
import navi_go.model.Result;

public class SearchRoutesView extends RelativeLayout implements RequestCallback<String> {
    public SearchRoutesView(Context context) {
        super(context);
    }

    @Override
    public void onComplete(Result<String> result) {
        if(result instanceof Result.Success) {
            // Happy path

            String s = ((Result.Success<String>) result).data;
            //mainActivity.mTxtViewLog.setText(s);
            //Toast.makeText(mainActivity.getBaseContext(), s, Toast.LENGTH_SHORT).show();

        } else {
            // Show error in UI
            Exception exc = ((Result.Error<String>) result).exception;
            String s = exc.toString();
            //String s = ((Result.Error<String>) result).exception.toString();
            //Toast.makeText(mainActivity.ctx, s, Toast.LENGTH_SHORT).show();
        }
    }
}
