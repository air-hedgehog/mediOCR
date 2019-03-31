package com.akimchenko.antony.mediocr.fragments;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableSingleObserver;

public class TestClass {
    TestClass() {
        Single<String> singleOCR = Single.create(emitter -> {
            String result = startOCR();
        });

        Disposable disposable = singleOCR.subscribeWith(new DisposableSingleObserver<String>() {

            @Override
            public void onSuccess(String result) {
                // work with the resulting todos
            }

            @Override
            public void onError(Throwable e) {
                // handle the error case
            }
        });

        disposable.dispose();
    }

    private String startOCR() {
        return null;
    }
}
