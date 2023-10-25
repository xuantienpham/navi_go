package navi_go.osrm;

import android.os.Handler;

public class OSRMViewModel {
    private final OSRMRepository mOSRMRepository;
    private Handler resultHandler;
    public OSRMViewModel(OSRMRepository osrmConnection) {
        this.mOSRMRepository = osrmConnection;
    }

    public void makeOSRMRequest() {
        mOSRMRepository.makeOSRMRequest(new OSRMCallback<String>() {
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
        }, resultHandler);
    }

};
