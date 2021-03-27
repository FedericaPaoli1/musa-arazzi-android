package com.musaarazzi.augmentedimage;

import android.content.Context;
import android.util.Log;

import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.musaarazzi.R;
import com.musaarazzi.common.utils.Chapter;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings({"AndroidApiChecker"})
public class ArRecognitionNode extends AnchorNode {

    private static final String TAG = "AugmentedImageNode";

    private static CompletableFuture<ViewRenderable> bordersModel;

    public ArRecognitionNode(Context context) {
        // Upon construction, start loading the models for the corners of the frame.

        if (bordersModel == null) {
            bordersModel = ViewRenderable.builder()
                    .setView(context, R.layout.borders_renderable)
                    .build();
        }
    }


    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    public void setImage(AugmentedImage image, Chapter chapter) {

        if (!bordersModel.isDone()) {
            CompletableFuture.allOf(bordersModel)
                    .thenAccept((Void aVoid) -> setImage(image, chapter))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
        }

        setAnchor(image.createAnchor(image.getCenterPose()));

        Node modelNode;

        modelNode = new Node();
        modelNode.setParent(this);
        modelNode.setLocalRotation(new Quaternion(90f, 0f, 0f, -90f));
        // TODO set the correct position of node

        switch (chapter.getTitle()) {
            case "Introduzione":
                modelNode.setRenderable(bordersModel.getNow(null));
                break;
            case "Primo capitolo":
                break;
            case "Secondo capitolo":
                break;
            case "Terzo capitolo":
                break;
            case "Quarto capitolo":
                break;
        }
    }
}
