package com.musaarazzi.augmentedimages;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CorrectPositionNode extends AnchorNode {

    private static final String TAG = "CorrectPositionNode";

    private CompletableFuture<ModelRenderable> airLocationModel;

    private final Context context;

    public CorrectPositionNode(Context context) {
        this.context = context;
    }


    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    public void setPointer(AugmentedImage image, String chapterPosition) {

        if (this.getAnchor() == null) {
            setAnchor(image.createAnchor(image.getCenterPose()));
        }

        if (airLocationModel == null) {
            airLocationModel =
                    ModelRenderable.builder()
                            .setSource(context, Uri.parse("models/arrow-pointer.sfb"))
                            .build();
        }

        if (!airLocationModel.isDone()) {
            airLocationModel
                    .thenAccept((ModelRenderable renderable) -> setPointer(image, chapterPosition))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
            return;
        }

        Node airLocationModelNode;

        airLocationModelNode = new Node();

        airLocationModelNode.setLocalPosition(new Vector3(getNewX(image, chapterPosition), 0f, image.getExtentZ() + 0.5f));
        airLocationModelNode.setLocalScale(new Vector3(8f, 8f, 8f));

        airLocationModelNode.setParent(this);

        try {
            airLocationModel.get().setShadowCaster(false);
            airLocationModel.get().setShadowReceiver(false);
            airLocationModelNode.setRenderable(airLocationModel.get());
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    private float getNewX(AugmentedImage image, String chapterPosition) {
        int augmentedImageColumnCoordinate = Integer.parseInt(image.getName().split("_")[2].split("r")[1].split("c")[1]);
        int chapterColumnCoordinate = Integer.parseInt(chapterPosition.split("r")[1].split("c")[1]);
        int numbersToIncrementOrDecrement = chapterColumnCoordinate - augmentedImageColumnCoordinate;
        return image.getExtentX() * numbersToIncrementOrDecrement;
    }
}