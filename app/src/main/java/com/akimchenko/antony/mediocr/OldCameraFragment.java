package com.akimchenko.antony.mediocr;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.akimchenko.antony.mediocr.fragments.PreviewFragment;
import com.akimchenko.antony.mediocr.utils.Utils;

import java.io.File;
import java.util.Calendar;

public class OldCameraFragment extends Fragment {

    private Camera camera;
    private FrameLayout previewLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_old_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;
        previewLayout = view.findViewById(R.id.texture_view);
        ImageView captureButton = view.findViewById(R.id.capture_button);
        ImageView flashButton = view.findViewById(R.id.flash_button);
        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO вынести общую часть со вспышкой и акселерометром в родительский класс для камеры
            }
        });
        captureButton.setOnClickListener(v -> camera.takePicture(null, null, pictureCallback));
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        try {
            camera = Camera.open();
        } catch (Exception e) {
            Log.e(this.getClass().getName(), e.getMessage());
        }
        if (camera == null) return;
        OldCameraPreview cameraPreview = new OldCameraPreview(activity, camera);
        previewLayout.removeAllViews();
        previewLayout.addView(cameraPreview);
    }

    @Override
    public void onPause() {
        super.onPause();
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        camera.release();
    }


    private Camera.PictureCallback pictureCallback = (data, camera) -> {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (bitmap == null) {
            Toast.makeText(activity, "Captured image is empty", Toast.LENGTH_LONG).show();
        } else {
            File defaultDir = new File(Environment.getExternalStorageDirectory(), activity.getString(R.string.default_folder_name));
            if (!defaultDir.exists() || !defaultDir.isDirectory())
                defaultDir.mkdirs();
            File file = new File(defaultDir, Calendar.getInstance().getTimeInMillis() + ".jpg");
            Utils.writeBitmapToFile(bitmap, file);
            PreviewFragment previewFragment = new PreviewFragment();
            Bundle args = new Bundle();
            args.putString(PreviewFragment.ARG_IMAGE_FILE, file.getPath());
            previewFragment.setArguments(args);
            activity.pushFragment(previewFragment);
        }
    };
}
