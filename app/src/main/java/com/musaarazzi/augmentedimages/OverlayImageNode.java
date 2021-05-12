package com.musaarazzi.augmentedimages;

import android.content.Context;
import android.util.Log;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.FixedWidthViewSizer;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.musaarazzi.R;
import com.musaarazzi.common.utils.Chapter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class OverlayImageNode extends AnchorNode {

    private static final String TAG = "OverlayImageNode";

    private final int rowsColumnsNumber;

    private CompletableFuture<ViewRenderable> bordersModel;
    private CompletableFuture<ViewRenderable> chapterOneModel;
    private CompletableFuture<ViewRenderable> chapterTwoModel;
    private CompletableFuture<ViewRenderable> chapterThreeModel;
    private CompletableFuture<ViewRenderable> chapterFourModel;
    private CompletableFuture<ViewRenderable> chapterFiveModel;

    private final Context context;

    public OverlayImageNode(Context context) {
        this.context = context;

        int splitsNumber = ArRecognitionFragment.perfectSquares.get(ArRecognitionFragment.chunksNumberIndex);
        this.rowsColumnsNumber = (int) Math.sqrt(splitsNumber);
    }


    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    public void setImage(AugmentedImage image, Chapter chapter) {
        float originalImageWidth = image.getExtentX() * rowsColumnsNumber;
        float originalImageHeight = image.getExtentZ() * rowsColumnsNumber;


        if (this.getAnchor() == null) {
            setAnchor(image.createAnchor(image.getCenterPose()));
        }

        if (bordersModel == null) {
            bordersModel = ViewRenderable.builder()
                    .setView(context, R.layout.borders_renderable)
                    .setVerticalAlignment(ViewRenderable.VerticalAlignment.CENTER)
                    .setSizer(new FixedWidthViewSizer(originalImageWidth))
                    .build();
        }
        if (!bordersModel.isDone()) {
            bordersModel
                    .thenAccept((ViewRenderable renderable) -> setImage(image, chapter))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
            return;
        }

        if (chapterOneModel == null) {
            chapterOneModel = ViewRenderable.builder()
                    .setView(context, R.layout.chapter_one_renderable)
                    .setVerticalAlignment(ViewRenderable.VerticalAlignment.CENTER)
                    .setSizer(new FixedWidthViewSizer(originalImageWidth))
                    .build();
        }
        if (!chapterOneModel.isDone()) {
            chapterOneModel
                    .thenAccept((ViewRenderable renderable) -> setImage(image, chapter))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
            return;
        }

        if (chapterTwoModel == null) {
            chapterTwoModel = ViewRenderable.builder()
                    .setView(context, R.layout.chapter_two_renderable)
                    .setVerticalAlignment(ViewRenderable.VerticalAlignment.CENTER)
                    .setSizer(new FixedWidthViewSizer(originalImageWidth))
                    .build();
        }
        if (!chapterTwoModel.isDone()) {
            chapterTwoModel
                    .thenAccept((ViewRenderable renderable) -> setImage(image, chapter))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
            return;
        }

        if (chapterThreeModel == null) {
            chapterThreeModel = ViewRenderable.builder()
                    .setView(context, R.layout.chapter_three_renderable)
                    .setVerticalAlignment(ViewRenderable.VerticalAlignment.CENTER)
                    .setSizer(new FixedWidthViewSizer(originalImageWidth))
                    .build();
        }
        if (!chapterThreeModel.isDone()) {
            chapterThreeModel
                    .thenAccept((ViewRenderable renderable) -> setImage(image, chapter))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
            return;
        }

        if (chapterFourModel == null) {
            chapterFourModel = ViewRenderable.builder()
                    .setView(context, R.layout.chapter_four_renderable)
                    .setVerticalAlignment(ViewRenderable.VerticalAlignment.CENTER)
                    .setSizer(new FixedWidthViewSizer(originalImageWidth))
                    .build();
        }
        if (!chapterFourModel.isDone()) {
            chapterFourModel
                    .thenAccept((ViewRenderable renderable) -> setImage(image, chapter))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
            return;
        }

        if (chapterFiveModel == null) {
            chapterFiveModel = ViewRenderable.builder()
                    .setView(context, R.layout.chapter_five_renderable)
                    .setVerticalAlignment(ViewRenderable.VerticalAlignment.CENTER)
                    .setSizer(new FixedWidthViewSizer(originalImageWidth))
                    .build();
        }
        if (!chapterFiveModel.isDone()) {
            chapterFiveModel
                    .thenAccept((ViewRenderable renderable) -> setImage(image, chapter))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
            return;
        }

        Node modelNode;

        modelNode = new Node();

        // set correct dimension for all overlay images
        try {
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone((ConstraintLayout) bordersModel.get().getView().findViewById(R.id.overlay_layout));
            constraintSet.setDimensionRatio(R.id.borders_view, originalImageWidth + ":" + originalImageHeight);
            constraintSet.applyTo(bordersModel.get().getView().findViewById(R.id.overlay_layout));


            constraintSet = new ConstraintSet();
            constraintSet.clone((ConstraintLayout) chapterOneModel.get().getView().findViewById(R.id.overlay_layout));
            constraintSet.setDimensionRatio(R.id.chapter_one_view, originalImageWidth + ":" + originalImageHeight);
            constraintSet.applyTo(chapterOneModel.get().getView().findViewById(R.id.overlay_layout));

            constraintSet = new ConstraintSet();
            constraintSet.clone((ConstraintLayout) chapterTwoModel.get().getView().findViewById(R.id.overlay_layout));
            constraintSet.setDimensionRatio(R.id.chapter_two_view, originalImageWidth + ":" + originalImageHeight);
            constraintSet.applyTo(chapterTwoModel.get().getView().findViewById(R.id.overlay_layout));

            constraintSet = new ConstraintSet();
            constraintSet.clone((ConstraintLayout) chapterThreeModel.get().getView().findViewById(R.id.overlay_layout));
            constraintSet.setDimensionRatio(R.id.chapter_three_view, originalImageWidth + ":" + originalImageHeight);
            constraintSet.applyTo(chapterThreeModel.get().getView().findViewById(R.id.overlay_layout));

            constraintSet = new ConstraintSet();
            constraintSet.clone((ConstraintLayout) chapterFourModel.get().getView().findViewById(R.id.overlay_layout));
            constraintSet.setDimensionRatio(R.id.chapter_four_view, originalImageWidth + ":" + originalImageHeight);
            constraintSet.applyTo(chapterFourModel.get().getView().findViewById(R.id.overlay_layout));

            constraintSet = new ConstraintSet();
            constraintSet.clone((ConstraintLayout) chapterFiveModel.get().getView().findViewById(R.id.overlay_layout));
            constraintSet.setDimensionRatio(R.id.chapter_five_view, originalImageWidth + ":" + originalImageHeight);
            constraintSet.applyTo(chapterFiveModel.get().getView().findViewById(R.id.overlay_layout));

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        modelNode.setLocalPosition(new Vector3(getNewX(image), 0.0f, getNewZ(image)));
        modelNode.setLocalRotation(Quaternion.axisAngle(Vector3.right(), -90));

        modelNode.setParent(this);

        try {
            switch (chapter.getTitle()) {
                case "Introduzione":
                    modelNode.setRenderable(bordersModel.get());
                    break;
                case "Primo capitolo":
                    modelNode.setRenderable(chapterOneModel.get());
                    break;
                case "Secondo capitolo":
                    modelNode.setRenderable(chapterTwoModel.get());
                    break;
                case "Terzo capitolo":
                    modelNode.setRenderable(chapterThreeModel.get());
                    break;
                case "Quarto capitolo":
                    modelNode.setRenderable(chapterFourModel.get());
                    break;
                case "Quinto capitolo":
                    modelNode.setRenderable(chapterFiveModel.get());
                    break;
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

    }


    private float getNewX(AugmentedImage image) {
        int center = this.rowsColumnsNumber / 2;
        int augmentedImageColumnCoordinate = Integer.parseInt(image.getName().split("_")[2].split("r")[1].split("c")[1]);
        if (this.rowsColumnsNumber % 2 == 0) {
            int numbersToIncrementOrDecrement = (center - augmentedImageColumnCoordinate) - 1;
            return (image.getExtentX() * numbersToIncrementOrDecrement) + (image.getExtentX() * 0.5f);
        } else {
            int numbersToIncrementOrDecrement = (center - augmentedImageColumnCoordinate);
            return (image.getExtentX() * numbersToIncrementOrDecrement);
        }
    }

    private float getNewZ(AugmentedImage image) {
        int center = this.rowsColumnsNumber / 2;
        int augmentedImageRowCoordinate = Integer.parseInt(image.getName().split("_")[2].split("r")[1].split("c")[0]);
        if (rowsColumnsNumber % 2 == 0) {
            int numbersToIncrementOrDecrement = (center - augmentedImageRowCoordinate) - 1;
            return (image.getExtentZ() * numbersToIncrementOrDecrement) + (image.getExtentZ() * 0.5f);
        } else {
            int numbersToIncrementOrDecrement = (center - augmentedImageRowCoordinate);
            return (image.getExtentZ() * numbersToIncrementOrDecrement);
        }
    }

}