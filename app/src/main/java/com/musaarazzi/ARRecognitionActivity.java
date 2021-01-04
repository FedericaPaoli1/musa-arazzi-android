package com.musaarazzi;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.ImageInsufficientQualityException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ARRecognitionActivity extends AppCompatActivity {
    private static final String TAG = "ARRecognitionActivity";
    private static final double MIN_OPENGL_VERSION = 3.0;
    private ArFragment arFragment;
    private Session session;
    private ArSceneView arSceneView;
    private boolean shouldConfigureSession = false;
    private AugmentedImageDatabase augmentedImageDatabase;
    private Bitmap augmentedImageBitmap;
    private List<Integer> augmentedImageIndexes = new ArrayList<>();
    private List<Integer> perfectSquares = new ArrayList<>(Arrays.asList(4, 9, 16, 25, 36, 49, 64));
    private int chunksNumberIndex = perfectSquares.size() - 1;
    private boolean isModelAdded = false;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_arrecognition);


        this.arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ar_fragment);

        // hiding the plane discovery
        this.arFragment.getPlaneDiscoveryController().hide();
        this.arFragment.getPlaneDiscoveryController().setInstructionView(null);
        this.arFragment.getArSceneView().getPlaneRenderer().setEnabled(false);
        this.arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdate);

        this.arSceneView = this.arFragment.getArSceneView();
    }

    @Override
    protected void onDestroy() {
        if (this.session != null) {
            // Explicitly close ARCore Session to release native resources
            this.session.close();
            this.session = null;
        }

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.session == null) {
            String message = null;
            Exception exception = null;
            try {
                this.session = new Session(this);
            } catch (UnavailableArcoreNotInstalledException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update android";
                exception = e;
            } catch (SecurityException e) {
                message = "Camera permission is not granted";
                exception = e;
                // Camera permission is not granted
                // TODO avoid this exception to be thrown
            } catch (Exception e) {
                message = "AR is not supported";
                exception = e;
            }

            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Exception creating session", exception);
                return;
            }
            this.shouldConfigureSession = true;

        }
        if (this.shouldConfigureSession) {
            configureSession();
            this.shouldConfigureSession = false;

            this.arSceneView.setupSession(this.session);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.session != null) {
            this.arSceneView.pause();
            this.session.pause();
        }
    }

    private Bitmap loadImage() {
        try (InputStream is = getAssets().open("arazzo.jpg")) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e(TAG, "IO Exception while loading", e);
        }
        return null;
    }

    private boolean setupAugmentedImageDb() {
        this.augmentedImageBitmap = loadImage();
        if (this.augmentedImageBitmap == null) {
            return false;
        }

        this.augmentedImageDatabase = new AugmentedImageDatabase(this.session);
        this.augmentedImageIndexes.add(this.augmentedImageDatabase.addImage("arazzo", this.augmentedImageBitmap));

        return this.augmentedImageDatabase.getNumImages() >= 1;
    }

    private void splitImage(Bitmap bitmap, int chunkNumbers) {

        //For the number of rows and columns of the grid to be displayed
        int rows, cols;

        //For height and width of the small image chunks
        int chunkHeight, chunkWidth;

        int counter = 1;

        rows = cols = (int) Math.sqrt(chunkNumbers);
        chunkHeight = bitmap.getHeight() / rows;
        chunkWidth = bitmap.getWidth() / cols;


        //xCoord and yCoord are the pixel positions of the image chunks
        int yCoord = 0;
        for (int x = 0; x < rows; x++) {
            int xCoord = 0;
            for (int y = 0; y < cols; y++) {
                try {
                    this.augmentedImageIndexes.add(this.augmentedImageDatabase.addImage("chunk_" + counter, Bitmap.createBitmap(bitmap, xCoord, yCoord, chunkWidth, chunkHeight), 480));
                    // 480: to speed up the augmented image detection
                    // TODO find a way to generalize with android.hardware.camera2
                } catch (ImageInsufficientQualityException e) {
                    this.chunksNumberIndex = perfectSquares.indexOf(chunkNumbers) - 1;
                    return;
                }
                counter++;
                xCoord += chunkWidth;
            }
            yCoord += chunkHeight;
        }
    }

    private void onUpdate(FrameTime frameTime) {
        Frame frame = this.arFragment.getArSceneView().getArFrame();

        Collection<AugmentedImage> augmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);

        for (AugmentedImage augmentedImage : augmentedImages) {
            // TODO maybe put a progress bar until the image is recognized
            if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {
                if (this.augmentedImageIndexes.stream().anyMatch(i -> i == augmentedImage.getIndex()) && !isModelAdded) {
                    Log.d(TAG, "Image recognized");
                    Toast.makeText(this, "Image recognized", Toast.LENGTH_SHORT).show();
                    // TODO render shape
                    isModelAdded = true;
                }
            }
        }
    }

    private void configureSession() {
        Config config = new Config(session);
        if (!setupAugmentedImageDb()) {
            Toast.makeText(this, "Unable to setup augmented", Toast.LENGTH_SHORT).show();
        }
        while (this.augmentedImageDatabase.getNumImages() < this.perfectSquares.get(this.chunksNumberIndex) + 1) {
            // while the number of images in the database is less than the number of chunks of the perfect square considered
            splitImage(this.augmentedImageBitmap, this.perfectSquares.get(this.chunksNumberIndex));
        }

        config.setAugmentedImageDatabase(this.augmentedImageDatabase);
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        this.session.configure(config);
    }

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
}