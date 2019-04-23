package com.akimchenko.antony.mediocr.utils;

import android.view.View;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.snackbar.Snackbar;
import org.jetbrains.annotations.NotNull;

public class MoveUpwardBehavior extends CoordinatorLayout.Behavior<View> {

    @Override
    public boolean layoutDependsOn(@NotNull CoordinatorLayout parent, @NotNull View child, @NotNull View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(@NotNull CoordinatorLayout parent, @NotNull View child, @NotNull View dependency) {
        float translationY = Math.min(0, dependency.getTranslationY() - dependency.getHeight());
        child.setTranslationY(translationY);
        return true;
    }
}