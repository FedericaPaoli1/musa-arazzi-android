package com.musaarazzi.augmentedimage;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.ImageInsufficientQualityException;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArRecognitionFragment extends ArFragment {
    private static final String TAG = "ArRecognitionFragment";

    public static final String IMAGE_NAME = "arazzo.jpg";

    public static final String DEFAULT_IMAGE_NAME = "Arazzo";

    // Runtime check for the OpenGL level available at runtime to avoid Sceneform crashing the
    // application.
    private static final double MIN_OPENGL_VERSION = 3.0;

    private AugmentedImageDatabase augmentedImageDatabase;
    private Bitmap augmentedImageBitmap;
    public static List<Integer> perfectSquares = new ArrayList<>(Arrays.asList(2, 4, 9, 16, 25, 36, 49, 64));
    public static int chunksNumberIndex = perfectSquares.size() - 1;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Check for Sceneform being supported on this device.  This check will be integrated into
        // Sceneform eventually.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(getContext(), "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
        }

        String openGlVersionString =
                ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 or later");
            Toast.makeText(getContext(), "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        // Turn off the plane discovery since we're only looking for images (Turn on only if it can be done before the onUpdate)
         getPlaneDiscoveryController().hide();
         getPlaneDiscoveryController().setInstructionView(null);
        getArSceneView().getPlaneRenderer().setEnabled(false);
        return view;
    }

    @Override
    protected Config getSessionConfiguration(Session session) {
        Config config = super.getSessionConfiguration(session);
        if (!setupAugmentedImageDatabase(config, session)) {
            Toast.makeText(getContext(), "Could not setup augmented image database", Toast.LENGTH_LONG).show();
        }
        return config;
    }

    private boolean setupAugmentedImageDatabase(Config config, Session session) {
        AssetManager assetManager = getContext() != null ? getContext().getAssets() : null;
        if (assetManager == null) {
            Log.e(TAG, "Context is null, cannot intitialize image database.");
            return false;
        }

        this.augmentedImageBitmap = loadImage(assetManager, IMAGE_NAME);
        if (this.augmentedImageBitmap == null) {
            return false;
        }

        this.augmentedImageDatabase = new AugmentedImageDatabase(session);
        this.augmentedImageDatabase.addImage(DEFAULT_IMAGE_NAME, this.augmentedImageBitmap);

        while (this.augmentedImageDatabase.getNumImages() < this.perfectSquares.get(this.chunksNumberIndex) + 1) {
            // while the number of images in the database is less than the number of chunks of the perfect square considered
            splitImage(this.augmentedImageBitmap, this.perfectSquares.get(this.chunksNumberIndex));
        }
        Toast.makeText(getContext(), "Perfect square " + this.perfectSquares.get(this.chunksNumberIndex), Toast.LENGTH_LONG).show();
        Log.d(TAG, "Chunks number: " + this.perfectSquares.get(this.chunksNumberIndex));
        config.setAugmentedImageDatabase(this.augmentedImageDatabase);

        return this.augmentedImageDatabase.getNumImages() >= 1;
    }

    public static Bitmap loadImage(AssetManager assetManager, String filename) {
        try (InputStream is = assetManager.open(filename)) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e(TAG, "IO exception loading augmented image bitmap.", e);
        }
        return null;
    }


    private void splitImage(Bitmap bitmap, int chunkNumbers) {

        //For the number of rows and columns of the grid to be displayed
        int rows, cols;

        //For height and width of the small image chunks
        int chunkHeight, chunkWidth;

        // int counter = 1;

        rows = cols = (int) Math.sqrt(chunkNumbers);
        chunkHeight = bitmap.getHeight() / rows;
        chunkWidth = bitmap.getWidth() / cols;


        //xCoord and yCoord are the pixel positions of the image chunks
        int yCoord = 0;
        for (int x = 0; x < rows; x++) {
            int xCoord = 0;
            for (int y = 0; y < cols; y++) {
                try {
                    this.augmentedImageDatabase.addImage("chunk_at_r" + x + "c" + y, Bitmap.createBitmap(bitmap, xCoord, yCoord, chunkWidth, chunkHeight));
                    // 480: to speed up the augmented image detection
                    // TODO find a way to generalize with android.hardware.camera2
                } catch (ImageInsufficientQualityException e) {
                    this.chunksNumberIndex = perfectSquares.indexOf(chunkNumbers) - 1;
                    return;
                }
                // counter++;
                xCoord += chunkWidth;
            }
            yCoord += chunkHeight;
        }
    }
}
