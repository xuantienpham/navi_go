package navi_go.model;

public interface RequestCallback<T> {
    void onComplete(Result<T> result);
}

