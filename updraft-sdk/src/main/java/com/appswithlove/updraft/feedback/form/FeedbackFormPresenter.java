package com.appswithlove.updraft.feedback.form;

import android.os.Handler;
import com.appswithlove.updraft.Updraft;
import com.appswithlove.updraft.api.ApiWrapper;
import com.appswithlove.updraft.feedback.FeedbackActivity;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.util.concurrent.TimeUnit;

public class FeedbackFormPresenter {

    private FeedbackFormContract.View mView;
    private CompositeDisposable mCompositeDisposable;
    private ApiWrapper mApiWrapper;
    private Disposable mSendFeedbackDisposable;

    FeedbackFormPresenter() {
        mApiWrapper = Updraft.getInstance().getApiWrapper();
    }

    public void attachView(FeedbackFormContract.View view) {
        mView = view;
        mCompositeDisposable = new CompositeDisposable();
    }

    public void detachView() {
        mCompositeDisposable.dispose();
        mView = null;
    }

    public void onSendButtonClicked() {
        mView.showProgress();
        mSendFeedbackDisposable =
                mApiWrapper
                        .sendMobileFeedback(
                                mView.getSelectedChoice(),
                                mView.getDescription(),
                                mView.getEmail(),
                                FeedbackActivity.SAVED_SCREENSHOT
                        )
                        //.concatMap(progress -> Observable.just(progress).delay(1000, TimeUnit.MILLISECONDS))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                response -> mView.updateProgress(response),
                                t -> {
                                    t.printStackTrace();
                                    mView.showErrorMessage(t);
                                },
                                () -> {
                                    mView.showSuccessMessage();
                                    closeAfterDelay();
                                }
                        );
        mCompositeDisposable.add(mSendFeedbackDisposable);
    }

    public void onProgressCancelClicked() {
        if (mSendFeedbackDisposable != null && !mSendFeedbackDisposable.isDisposed()) {
            mSendFeedbackDisposable.dispose();
        }
        mView.hideProgress();
    }

    private void closeAfterDelay(){
        Handler handler = new Handler();
        handler.postDelayed(
                () -> mView.closeFeedback(),
                1000
        );
    }
}
