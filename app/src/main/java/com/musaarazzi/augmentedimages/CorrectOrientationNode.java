package com.musaarazzi.augmentedimages;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.ar.core.Camera;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CorrectOrientationNode extends AnchorNode {

    private static final String TAG = "CorrectOrientationNode";

    private CompletableFuture<ModelRenderable> orientationModel;

    private final Context context;

    public CorrectOrientationNode(Context context) {
        this.context = context;

    }


    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    public void setArrow(Session session, Camera camera, int rowsDifference, int columnsDifference) {


        if (this.getAnchor() == null) {
            setAnchor(session.createAnchor(camera.getPose().compose(Pose.makeTranslation(0, 0f, -0.3f)).extractTranslation()));
        }

        if (orientationModel == null) {
            orientationModel =
                    ModelRenderable.builder()
                            .setSource(context, Uri.parse("models/arrow.sfb"))
                            .build();
        }

        if (!orientationModel.isDone()) {
            orientationModel
                    .thenAccept((ModelRenderable renderable) -> setArrow(session, camera, rowsDifference, columnsDifference))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
            return;
        }

        Node orientationModelNode;
        orientationModelNode = new Node();

        orientationModelNode.setLocalScale(new Vector3(0.04f, 0.04f, 0.04f));
        modifyArrow(rowsDifference, columnsDifference, orientationModelNode);


        orientationModelNode.setParent(this);

        try {
            orientationModel.get().setShadowCaster(false);
            orientationModel.get().setShadowReceiver(false);
            orientationModelNode.setRenderable(orientationModel.get());
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void modifyArrow(int rowsDifference, int columnsDifference, Node orientationModelNode) {
        if (columnsDifference == 0) {
            if (isUpward(rowsDifference)) {
                orientationModelNode.setLocalRotation(Quaternion.axisAngle(Vector3.right(), 90));
            } else {
                orientationModelNode.setLocalRotation(Quaternion.axisAngle(Vector3.right(), -90));
            }
        } else if (rowsDifference == 0) {
            if (isToTheRight(columnsDifference)) {
                orientationModelNode.setLocalRotation(Quaternion.multiply(Quaternion.axisAngle(Vector3.right(), 90), Quaternion.axisAngle(Vector3.up(), -90)));
            } else {
                orientationModelNode.setLocalRotation(Quaternion.multiply(Quaternion.axisAngle(Vector3.right(), 90), Quaternion.axisAngle(Vector3.up(), 90)));
            }
        } else {
            double diagonal = Math.sqrt(Math.pow(columnsDifference, 2) + Math.pow(rowsDifference, 2));
            float alpha = (float) Math.acos(Math.abs(columnsDifference) / diagonal);
            alpha = alpha * (180 / (float) Math.PI); // Conversion from radians to degrees
            if (columnsDifference > 0) {
                alpha += -90;
            }
            if (rowsDifference < 0) {
                orientationModelNode.setLocalRotation(Quaternion.multiply(Quaternion.axisAngle(Vector3.right(), 90), Quaternion.axisAngle(Vector3.up(), alpha)));
            } else {
                orientationModelNode.setLocalRotation(Quaternion.multiply(Quaternion.axisAngle(Vector3.right(), -90), Quaternion.axisAngle(Vector3.up(), alpha)));
            }
        }
    }

    private boolean isUpward(int rowsDifference) {
        return rowsDifference < 0;
    }

    private boolean isToTheRight(int columnsDifference) {
        return columnsDifference > 0;
    }

}