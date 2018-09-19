package com.github.qingmei2.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.github.qingmei2.activity.LoginActivity;

import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;

import static android.app.Activity.RESULT_OK;

public class NavigatorFragment extends Fragment {

    private static final String TAG = "NavigatorFragment";

    private PublishSubject<Boolean> resultSubject;
    private PublishSubject<Boolean> cancelSubject;
    private PublishSubject<Boolean> attachSubject = PublishSubject.create();

    public Single<Boolean> startLoginForResult(FragmentActivity activity) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(TAG);
        if (fragment == null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(this, TAG).commitAllowingStateLoss();
            return startLoginSingle();
        } else {
            return ((NavigatorFragment) fragment).startLoginSingle();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        attachSubject.onNext(true);
        attachSubject.onComplete();
    }

    public Single<Boolean> startLoginSingle() {
        resultSubject = PublishSubject.create();
        cancelSubject = PublishSubject.create();
        startLogin();
        return resultSubject
                .takeUntil(cancelSubject)
                .single(false);
    }

    @SuppressLint("CheckResult")
    public void startLogin() {
        if (!isAdded()) {
            attachSubject.subscribe(__ -> startLoginForResult());
        } else {
            startLoginForResult();
        }
    }

    private void startLoginForResult() {
        startActivityForResult(new Intent(getContext(), LoginActivity.class), 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            boolean isLogin = data.getBooleanExtra(LoginActivity.EXTRA_SUCCESS, false);
            resultSubject.onNext(isLogin);
            resultSubject.onComplete();
        } else {
            cancelSubject.onNext(true);
        }
    }
}
