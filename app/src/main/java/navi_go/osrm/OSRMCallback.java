package navi_go.osrm;

public interface OSRMCallback<T> {
    void onComplete(Result<T> result);
}

