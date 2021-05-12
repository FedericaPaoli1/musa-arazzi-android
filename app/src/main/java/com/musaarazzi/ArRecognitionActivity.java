package com.musaarazzi;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.ux.ArFragment;
import com.musaarazzi.augmentedimages.ArRecognitionFragment;
import com.musaarazzi.augmentedimages.CorrectOrientationNode;
import com.musaarazzi.augmentedimages.OverlayImageNode;
import com.musaarazzi.augmentedimages.CorrectPositionNode;
import com.musaarazzi.common.utils.Chapter;
import com.musaarazzi.common.utils.ChaptersService;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class ArRecognitionActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ArRecognitionActivity";
    private ArFragment arFragment;

    private List<Chapter> chapters;

    // Layout components
    private TextView chapterName;
    private ImageButton playButton;
    private ImageButton pauseButton;
    private LinearLayout subtitlesLinearLayout;
    private TextView subtitles;
    private ImageButton closeSubsButton;
    private ProgressBar progressBar;

    private TextToSpeech textToSpeech;

    private Dialog dialog;
    private TextView dialogTitle;
    private TextView dialogMessage;


    // Control conditions for the onUpdate method
    private Pair<AugmentedImage, OverlayImageNode> currentArRecognitionNode = null;
    private Pair<AugmentedImage, CorrectPositionNode> currentCorrectPositionNode = null;
    private Pair<AugmentedImage, CorrectOrientationNode> currentCorrectOrientationNode = null;
    private boolean isOverlayImageModelNodeAdded = false;
    private boolean isCorrectPositionNodeAdded = false;
    private boolean isCorrectOrientationNodeAdded = false;
    private boolean isFarFromTheChapterPosition = false;
    private boolean isRotationNeeded = false;
    private final List<Chapter> chaptersSelectedList = new LinkedList<>();
    private final List<AugmentedImage> augmentedImages = new LinkedList<>();
    private int counter = 0;
    private boolean exit = false;

    // Tolerance for the user displacement must be increased for a larger artwork
    private static final float POSITIVE_TOLERANCE = 0;
    private static final float NEGATIVE_TOLERANCE = 0;

    private Vibrator vibrator;

    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arrecognition);

        this.chapters = ChaptersService.readJson(TAG, getApplicationContext());
        this.chapters.get(0).setSelected(true);

        // Layout components initial settings
        TextView artworkName = findViewById(R.id.artworkName);
        artworkName.setText(ArRecognitionFragment.DEFAULT_IMAGE_NAME);
        this.chapterName = findViewById(R.id.chapterName);
        this.chapterName.setText(this.chapters.get(0).getTitle());
        this.playButton = findViewById(R.id.play_button);
        this.pauseButton = findViewById(R.id.pause_button);
        this.subtitlesLinearLayout = findViewById(R.id.subtitles_linearLayout);
        this.subtitles = findViewById(R.id.subtitles_text);
        this.subtitles.setText(this.chapters.get(0).getText());
        this.closeSubsButton = findViewById(R.id.subtitles_close_button);
        this.progressBar = findViewById(R.id.progressBar);

        // Set dialog to show for user displacement
        this.dialog = new Dialog(this);
        this.dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.dialog.setContentView(R.layout.dialog_layout);
        this.dialogTitle = this.dialog.findViewById(R.id.dialog_title);
        this.dialogMessage = this.dialog.findViewById(R.id.dialog_message);
        this.dialog.setCancelable(false);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        this.textToSpeech = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int ttsLang = textToSpeech.setLanguage(Locale.ITALIAN);

                if (ttsLang == TextToSpeech.LANG_MISSING_DATA
                        || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "The Language is not supported");
                } else {
                    Log.i(TAG, "Language Supported");
                }
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String s) {
                        runOnUiThread(() -> {
                            playButton.setVisibility(View.GONE);
                            pauseButton.setVisibility(View.VISIBLE);
                        });
                    }

                    @Override
                    public void onDone(String s) {
                        runOnUiThread(() -> {
                            pauseButton.setVisibility(View.GONE);
                            playButton.setVisibility(View.VISIBLE);
                            changeChapter(detectChapter(), true);
                        });
                    }

                    @Override
                    public void onStop(String utteranceId, boolean interrupted) {
                        super.onStop(utteranceId, interrupted);
                        runOnUiThread(() -> {
                            pauseButton.setVisibility(View.GONE);
                            playButton.setVisibility(View.VISIBLE);
                        });
                    }

                    @Override
                    public void onError(String s) {
                    }
                });
            } else {
                Toast.makeText(getApplicationContext(), "TextToSpeech initialization failed!", Toast.LENGTH_SHORT).show();
            }
        });

        this.vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        this.mediaPlayer = MediaPlayer.create(this, R.raw.notification);

        this.arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ar_fragment);

        if (this.arFragment != null)
            this.arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        textToSpeech.shutdown();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        Chapter c = detectChapter();
        switch (v.getId()) {
            case R.id.back_button:
                Log.d(TAG, "Back button tapped");
                finish();
                break;
            case R.id.virtualMode_button:
                Log.d(TAG, "Virtual mdoe button tapped");
                // turn to virtual mode
                break;
            case R.id.subtitles:
                Log.d(TAG, "Subtitles button tapped");
                this.subtitlesLinearLayout.setVisibility(View.VISIBLE);
                this.closeSubsButton.setVisibility(View.VISIBLE);
                break;
            case R.id.backward_button:
                Log.d(TAG, "Backward button tapped");
                textToSpeech.stop();
                changeChapter(c, false);
                break;
            case R.id.play_button:
                Log.d(TAG, "Play button tapped");
                this.exit = false;
                String text = subtitles.getText().toString().trim();
                if (!text.isEmpty()) {
                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts");
                } else {
                    Toast.makeText(getApplicationContext(), "Text cannot be empty", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.pause_button:
                Log.d(TAG, "Pause button tapped");
                textToSpeech.stop();
                break;
            case R.id.forward_button:
                Log.d(TAG, "Forward button tapped");
                textToSpeech.stop();
                changeChapter(c, true);
                break;
            case R.id.subtitles_close_button:
                Log.d(TAG, "Subtitles close button tapped");
                this.closeSubsButton.setVisibility(View.GONE);
                this.subtitlesLinearLayout.setVisibility(View.GONE);
                break;
            default:
                throw new RuntimeException("Unknown button");
        }
    }

    private void onUpdateFrame(FrameTime frameTime) {

        Frame frame = this.arFragment.getArSceneView().getArFrame();

        Camera camera;
        if (frame != null) {
            camera = frame.getCamera();
        } else {
            return; // If there is no frame, just return.
        }

        Collection<AugmentedImage> updatedAugmentedImages =
                frame.getUpdatedTrackables(AugmentedImage.class);

        if (!this.exit) { // Exit when the last chapter has been shown
            for (AugmentedImage augmentedImage : updatedAugmentedImages) {
                if (augmentedImage.getTrackingMethod() == AugmentedImage.TrackingMethod.FULL_TRACKING) {
                    Chapter chapter = detectChapter();

                    // Nodes to guide the user
                    OverlayImageNode overlayImageNode = new OverlayImageNode(this);
                    CorrectPositionNode correctPositionNode = new CorrectPositionNode(this);
                    CorrectOrientationNode correctOrientationNode = new CorrectOrientationNode(this);

                    // For all chapters except the introduction one
                    if (!chapter.getTitle().equalsIgnoreCase("Introduzione")) {

                        if (counter == 0 && !chaptersSelectedList.contains(chapter)) { // Operations to do only at the start of each chapter
                            chaptersSelectedList.add(chapter);
                            this.isFarFromTheChapterPosition = getColumnsDistance(augmentedImage, chapter.getPosition()) < NEGATIVE_TOLERANCE || getColumnsDistance(augmentedImage, chapter.getPosition()) > POSITIVE_TOLERANCE;
                            this.isRotationNeeded = getRotationNeed(getRowsDistance(augmentedImage, chapter.getPosition()), getColumnsDistance(augmentedImage, chapter.getPosition()));
                        }

                        // User displacement logic
                        if (this.isFarFromTheChapterPosition) {

                            Log.d(TAG, "User position is far from the chapter position");
                            this.pauseButton.performClick();

                            addCorrectPositionNode(augmentedImage, chapter, correctPositionNode);

                            if (getColumnsDistance(augmentedImage, chapter.getPosition()) < NEGATIVE_TOLERANCE || getColumnsDistance(augmentedImage, chapter.getPosition()) > POSITIVE_TOLERANCE) {
                                showDialog(augmentedImage, chapter);

                                if (!this.augmentedImages.contains(augmentedImage) && counter > 0) {
                                    this.augmentedImages.add(augmentedImage); // List used to control the vibration when the user frames a new augmented image
                                    vibrator.vibrate(VibrationEffect.createOneShot(counter, VibrationEffect.DEFAULT_AMPLITUDE)); // Vibrate for counter milliseconds
                                }
                                counter++;
                            } else {
                                dismissDialog();

                                this.isFarFromTheChapterPosition = false;

                                break;
                            }
                        } else {
                            // The correct position has been found
                            Log.d(TAG, "User position is correct");
                            if (counter > 0)
                                vibrator.vibrate(VibrationEffect.createOneShot(counter, VibrationEffect.DEFAULT_AMPLITUDE)); // last vibration for a certain chapter displacement

                            removeCorrectPositionNode();

                            counter = 0;
                        }

                        // User phone rotation logic
                        if (camera.getTrackingState() == TrackingState.TRACKING && !this.isFarFromTheChapterPosition && this.isRotationNeeded) {
                            Log.d(TAG, "Phone is not correctly rotated");
                            if (getRotationNeed(getRowsDistance(augmentedImage, chapter.getPosition()), getColumnsDistance(augmentedImage, chapter.getPosition()))) {

                                removeCorrectOrientationNode(); // Remove node to replace the arrow with the correct rotation

                                this.pauseButton.performClick();

                                addCorrectOrientationNode(camera, augmentedImage, chapter, correctOrientationNode); // Replace arrow with the correct rotation

                            } else {
                                Log.d(TAG, "Phone rotation is correct");
                                // The correct phone rotation has been found

                                removeCorrectOrientationNode();

                                mediaPlayer.start(); // Play sound to tell the user that the correct phone rotation has been found

                                this.isRotationNeeded = false;

                            }
                        }
                    }

                    // All chapters logic
                    if (!this.isOverlayImageModelNodeAdded && !this.isFarFromTheChapterPosition && !this.isRotationNeeded) {

                        releaseMediaPlayer(); // Stop playing rotation sound if it was playing

                        this.augmentedImages.add(augmentedImage); // New augmented image framed

                        this.progressBar.setVisibility(View.GONE); // Remove the progress bar (only at the activity start)

                        addOverlayImageNode(augmentedImage, chapter, overlayImageNode);

                        // Play tts after 3 seconds to give the user time to view the overlay image
                        playButtonWith3SecondsDelay();
                    }
                }
            }
        }
    }

    private void playButtonWith3SecondsDelay() {
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> playButton.performClick());
                    }
                },
                3000
        );
    }

    private void addOverlayImageNode(AugmentedImage augmentedImage, Chapter chapter, OverlayImageNode overlayImageNode) {
        overlayImageNode.setImage(augmentedImage, chapter);
        overlayImageNode.setParent(this.arFragment.getArSceneView().getScene());
        this.currentArRecognitionNode = new Pair<>(augmentedImage, overlayImageNode);
        this.arFragment.getArSceneView().getScene().addChild(overlayImageNode);
        this.isOverlayImageModelNodeAdded = true;
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer.isLooping()) {
            mediaPlayer.release();
        }
    }

    private void removeCorrectOrientationNode() {
        if (this.isCorrectOrientationNodeAdded) {
            this.arFragment.getArSceneView().getScene().removeChild(this.currentCorrectOrientationNode.second);
            this.isCorrectOrientationNodeAdded = false;
        }
    }

    private void addCorrectOrientationNode(Camera camera, AugmentedImage augmentedImage, Chapter chapter, CorrectOrientationNode correctOrientationNode) {
        correctOrientationNode.setArrow(this.arFragment.getArSceneView().getSession(), camera, getRowsDistance(augmentedImage, chapter.getPosition()), getColumnsDistance(augmentedImage, chapter.getPosition()));
        correctOrientationNode.setParent(this.arFragment.getArSceneView().getScene());
        this.currentCorrectOrientationNode = new Pair<>(augmentedImage, correctOrientationNode);
        this.arFragment.getArSceneView().getScene().addChild(correctOrientationNode);
        this.isCorrectOrientationNodeAdded = true;
    }

    private void removeCorrectPositionNode() {
        if (this.isCorrectPositionNodeAdded) {
            this.isCorrectPositionNodeAdded = false;
            this.arFragment.getArSceneView().getScene().removeChild(this.currentCorrectPositionNode.second);
        }
    }

    private void dismissDialog() {
        if (this.dialog.isShowing()) {
            this.dialog.dismiss();
        }
    }

    private void showDialog(AugmentedImage augmentedImage, Chapter chapter) {
        if (!this.dialog.isShowing()) {
            this.dialogTitle.setText(R.string.dialog_title);
            if (isTheShiftToTheRight(getColumnsDistance(augmentedImage, chapter.getPosition()))) {
                this.dialogMessage.setText(R.string.right_displacement_dialog_text);
            } else {
                this.dialogMessage.setText(R.string.left_displacement_dialog_text);
            }
            this.dialog.show();
        }
    }

    private void addCorrectPositionNode(AugmentedImage augmentedImage, Chapter chapter, CorrectPositionNode correctPositionNode) {
        if (!this.isCorrectPositionNodeAdded) {
            correctPositionNode.setPointer(augmentedImage, chapter.getPosition());
            correctPositionNode.setParent(this.arFragment.getArSceneView().getScene());
            this.currentCorrectPositionNode = new Pair<>(augmentedImage, correctPositionNode);
            this.arFragment.getArSceneView().getScene().addChild(correctPositionNode);
            this.isCorrectPositionNodeAdded = true;
        }
    }

    private void changeChapter(Chapter chapter, boolean isForward) {
        // Clear all lists used on the onUpdate method
        chaptersSelectedList.clear();
        this.augmentedImages.clear();

        exitIfTheChapterIsTheLastOne(chapter);

        removeOverlayImageNode();

        if (!chapter.getTitle().equalsIgnoreCase("Introduzione")) {
            removeCorrectPositionNode();
        }

        if (isForward) {
            // Change the chapter with the following one
            if (this.chapters.indexOf(chapter) < this.chapters.size() - 1) {
                chapter.setSelected(false);
                this.chapters.get(this.chapters.indexOf(chapter) + 1).setSelected(true);
                this.chapterName.setText(this.chapters.get(this.chapters.indexOf(chapter) + 1).getTitle());
                this.subtitles.setText(this.chapters.get(this.chapters.indexOf(chapter) + 1).getText());
            }
        } else {
            // Change the chapter with the previous one
            if (this.chapters.indexOf(chapter) > 0) {
                chapter.setSelected(false);
                this.chapters.get(this.chapters.indexOf(chapter) - 1).setSelected(true);
                this.chapterName.setText(this.chapters.get(this.chapters.indexOf(chapter) - 1).getTitle());
                this.subtitles.setText(this.chapters.get(this.chapters.indexOf(chapter) - 1).getText());
            }
        }
    }

    private void removeOverlayImageNode() {
        if (this.isOverlayImageModelNodeAdded) {
            this.isOverlayImageModelNodeAdded = false;
            this.arFragment.getArSceneView().getScene().removeChild(this.currentArRecognitionNode.second);
        }
    }

    private void exitIfTheChapterIsTheLastOne(Chapter c) {
        if (c.getTitle().equalsIgnoreCase("Quinto capitolo")) {
            this.pauseButton.performClick();
            this.exit = true;
        }
    }

    private Chapter detectChapter() {
        return this.chapters.stream().filter(Chapter::getSelected).findFirst().orElse(null);
    }

    private int getColumnsDistance(AugmentedImage image, String chapterPosition) {
        int augmentedImageColumnCoordinate = Integer.parseInt(image.getName().split("_")[2].split("r")[1].split("c")[1]);
        int chapterColumnCoordinate = Integer.parseInt(chapterPosition.split("r")[1].split("c")[1]);
        return chapterColumnCoordinate - augmentedImageColumnCoordinate;
    }

    private int getRowsDistance(AugmentedImage image, String chapterPosition) {
        int augmentedImageRowCoordinate = Integer.parseInt(image.getName().split("_")[2].split("r")[1].split("c")[0]);
        int chapterRowCoordinate = Integer.parseInt(chapterPosition.split("r")[1].split("c")[0]);
        return chapterRowCoordinate - augmentedImageRowCoordinate;
    }

    private boolean getRotationNeed(int rowsDistance, int columnsDistance) {
        return rowsDistance != 0 || columnsDistance != 0;
    }

    private boolean isTheShiftToTheRight(int distance) {
        return distance > 0;
    }
}