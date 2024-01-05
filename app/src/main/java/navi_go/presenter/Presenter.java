package navi_go.presenter;

import navi_go.Contract;

public class Presenter {
    private Contract.View mMainView;
    private Contract.Model mModel;

    public Presenter(Contract.View mainView, Contract.Model model) {
        mMainView = mainView;
        mModel = model;
    }
}
