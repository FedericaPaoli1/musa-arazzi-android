package com.musaarazzi.augmentedimage;

import android.content.Context;
import android.util.Log;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.musaarazzi.R;
import com.musaarazzi.common.utils.Chapter;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings({"AndroidApiChecker"})
public class ArRecognitionNode extends AnchorNode {

    private static final String TAG = "ArRecognitionNode";

    private AugmentedImage image;

    private final int splitsNumber;

    private final float originalImageWidth = 1.05f;
    private final float originalImageHeight = 0.945f;

    private static CompletableFuture<ViewRenderable> bordersModel;

    public ArRecognitionNode(Context context) {
        // Upon construction, start loading the models for the corners of the frame.

        this.splitsNumber = ArRecognitionFragment.perfectSquares.get(ArRecognitionFragment.chunksNumberIndex);

        if (bordersModel == null) {
            bordersModel = ViewRenderable.builder()
                    .setView(context, R.layout.borders_renderable)
                    .build();
        }
    }


    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    public void setImage(AugmentedImage image, Chapter chapter) {

        this.image = image;

        if (!bordersModel.isDone()) {
            CompletableFuture.allOf(bordersModel)
                    .thenAccept((Void aVoid) -> setImage(image, chapter))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
        }

        float[] xyPositions = calculatePosition();

        double hypotenuse = Math.sqrt(Math.pow(image.getCenterPose().tz(), 2) + Math.pow(xyPositions[0], 2));
        float alpha = (float) Math.toDegrees(Math.acos(Math.abs(xyPositions[0] / hypotenuse)));

        setAnchor(image.createAnchor(new Pose(Pose.makeTranslation(xyPositions[0], xyPositions[1], image.getCenterPose().tz()).getTranslation(), Pose.makeRotation(1, 1, 0, -alpha).getRotationQuaternion())));

        Node modelNode;

        modelNode = new Node();
        modelNode.setParent(this);

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

    private float[] calculatePosition() {
        int rowsColumnsNumber = (int) Math.sqrt(this.splitsNumber);

        int lastRow = rowsColumnsNumber - 1;
        int center = rowsColumnsNumber / 2;

        double originalImageSingleWidth = this.originalImageWidth / rowsColumnsNumber;
        double originalImageSingleHeight = this.originalImageHeight / rowsColumnsNumber;

        int augmentedImageRowCoordinate = Integer.parseInt(this.image.getName().split("_")[2].split("r")[1].split("c")[0]);
        int augmentedImageColumnCoordinate = Integer.parseInt(this.image.getName().split("_")[2].split("r")[1].split("c")[1]);

        int[] numbersToIncrementOrDecrement = new int[2];
        numbersToIncrementOrDecrement[0] = augmentedImageRowCoordinate - lastRow;
        numbersToIncrementOrDecrement[1] = center - augmentedImageColumnCoordinate;

        float widthIncrementOrDecrement = (float) (originalImageSingleWidth * numbersToIncrementOrDecrement[1]);
        float heightIncrementOrDecrement = (float) (originalImageSingleHeight * numbersToIncrementOrDecrement[0]);

        float[] newPositions = new float[2];
        newPositions[0] = widthIncrementOrDecrement - this.image.getCenterPose().tx();
        newPositions[1] = heightIncrementOrDecrement - this.image.getCenterPose().ty();

        return newPositions;
    }
}
