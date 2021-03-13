package com.musaarazzi;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.Collection;
import java.util.List;

public class ARRecognitionActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ARRecognitionActivity";
    private ArFragment arFragment;

    private ProgressBar progressBar;

    private ImageView topImageView;
    private ImageView rightImageView;
    private ImageView bottomImageView;
    private ImageView leftImageView;

    private List<Integer> augmentedImageIndexes;
    private List<Integer> perfectSquares;
    private int chunksNumberIndex;

    private AugmentedImage recognizedAugmentedImage;

    private List<Utils.Chapter> chapters;

    private TextView chapterName;
    private ImageButton playButton;
    private ImageButton pauseButton;
    private TextView artworkName;

    private boolean isImagerecognized = false;

    private Thread thread;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_arrecognition);

        this.augmentedImageIndexes = ARRecognitionFragment.augmentedImageIndexes;
        this.perfectSquares = ARRecognitionFragment.perfectSquares;
        this.chunksNumberIndex = ARRecognitionFragment.chunksNumberIndex;

        this.progressBar = findViewById(R.id.progressBar);

        this.topImageView = findViewById(R.id.top_border_image);
        this.rightImageView = findViewById(R.id.right_border_image);
        this.bottomImageView = findViewById(R.id.bottom_border_image);
        this.leftImageView = findViewById(R.id.left_border_image);

        this.chapters = Utils.readJson(TAG, getApplicationContext());
        this.chapters.get(0).setSelected(true);

        this.artworkName = findViewById(R.id.artworkName);
        this.artworkName.setText(ARRecognitionFragment.DEFAULT_IMAGE_NAME);

        this.chapterName = findViewById(R.id.chapterName);
        this.chapterName.setText(this.chapters.get(0).getTitle());

        this.playButton = findViewById(R.id.play_button);
        this.pauseButton = findViewById(R.id.pause_button);


        this.thread = new Thread() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    playButton.setVisibility(View.GONE);
                    pauseButton.setVisibility(View.VISIBLE);
                });
                while (true) {
                    Utils.Chapter chapter = detectChapter();
                    try {
                        Thread.sleep(30 * 1000);
                        if (chapter.equals("Quarto capitolo")) {
                            synchronized (this) {
                                try {
                                    this.wait();
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                        changeChapter(chapter, true);
                    } catch (InterruptedException e) {
                        synchronized (this) {
                            try {
                                this.wait();
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        };
        this.thread.start();

        this.arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ar_fragment);

        this.arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdate);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        Utils.Chapter c = detectChapter();
        switch (v.getId()) {
            case R.id.back_button:
                // TODO going back from other activities, maybe from virtual mode
                break;
            case R.id.virtualMode_button:
                // TODO go to virtual mode
                break;
            case R.id.subtitles:
                // show subtitles
                break;
            case R.id.backward_button:
                this.thread.interrupt();
                changeChapter(c, false);
                synchronized (this.thread) {
                    this.thread.notify();
                }
                break;
            case R.id.play_button:
                synchronized (this.thread) {
                    this.thread.notify();
                }
                this.playButton.setVisibility(View.GONE);
                this.pauseButton.setVisibility(View.VISIBLE);
                break;
            case R.id.pause_button:
                Log.d(TAG, "Paused");
                this.thread.interrupt();
                this.pauseButton.setVisibility(View.GONE);
                this.playButton.setVisibility(View.VISIBLE);
                break;
            case R.id.forward_button:
                this.thread.interrupt();
                changeChapter(c, true);
                synchronized (this.thread) {
                    this.thread.notify();
                }
                break;
            default:
                throw new RuntimeException("Unknown button ID");
        }
    }

    private void onUpdate(FrameTime frameTime) {
        this.progressBar.setVisibility(View.GONE);

        Frame frame = this.arFragment.getArSceneView().getArFrame();

        Collection<AugmentedImage> augmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);


        for (AugmentedImage augmentedImage : augmentedImages) {
            if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {
                if (!this.isImagerecognized) {
                    if (this.augmentedImageIndexes.stream().anyMatch(i -> i == augmentedImage.getIndex())) {
                        Log.d(TAG, "Image recognized");
                        Log.d(TAG, "Chunksnumber used is: " + this.perfectSquares.get(this.chunksNumberIndex));
                        this.recognizedAugmentedImage = augmentedImage;
                        Log.d(TAG, "Found augmented image name: " + augmentedImage.getName() + " and index: " + augmentedImage.getIndex());
                        this.isImagerecognized = true;
                    }
                } else {
                    Utils.Chapter chapter = detectChapter();
                    detectShape(chapter.getTitle(), augmentedImage);
                }
            }
        }
    }

    private void detectShape(String chapterTitle, AugmentedImage augmentedImage) {
        if (chapterTitle.equals("Introduzione")) {
            Log.d(TAG, "introduzione");
            detectBorders(augmentedImage);
        } else {
            this.topImageView.setVisibility(ImageView.GONE);
            this.rightImageView.setVisibility(ImageView.GONE);
            this.bottomImageView.setVisibility(ImageView.GONE);
            this.leftImageView.setVisibility(ImageView.GONE);
            if (chapterTitle.equals("Primo capitolo")) {
                Log.d(TAG, "primo capitolo");
            } else if (chapterTitle.equals("Secondo capitolo")) {
                Log.d(TAG, "secondo capitolo");
            } else if (chapterTitle.equals("Terzo capitolo")) {
                Log.d(TAG, "terzo capitolo");
            } else if (chapterTitle.equals("Quarto capitolo")) {
                Log.d(TAG, "quarto capitolo");
            }
        }
    }

    private void changeChapter(Utils.Chapter c, boolean isForward) {
        if (isForward) {
            if (c != null && chapters.indexOf(c) < chapters.size() - 1) {
                c.setSelected(false);
                chapters.get(chapters.indexOf(c) + 1).setSelected(true);
                chapterName.setText(chapters.get(chapters.indexOf(c) + 1).getTitle());
            }
        } else {
            if (c != null && this.chapters.indexOf(c) > 0) {
                c.setSelected(false);
                this.chapters.get(this.chapters.indexOf(c) - 1).setSelected(true);
                this.chapterName.setText(this.chapters.get(this.chapters.indexOf(c) - 1).getTitle());
            }
        }
    }

    private Utils.Chapter detectChapter() {
        Utils.Chapter chapter = this.chapters.stream().filter(ch -> ch.getSelected()).findFirst().orElse(null);
        if (chapter != null) {
            switch (chapter.getTitle()) {
                case "Introduzione":
                    Log.d(TAG, "introduzione");
                    break;
                case "Primo capitolo":
                    Log.d(TAG, "primo capitolo");
                    break;
                case "Secondo capitolo":
                    Log.d(TAG, "secondo capitolo");
                    break;
                case "Terzo capitolo":
                    Log.d(TAG, "terzo capitolo");
                    break;
                case "Quarto capitolo":
                    Log.d(TAG, "quarto capitolo");
                    break;
                default:
                    throw new RuntimeException("Unknown chapter");
            }
        } else {
            throw new RuntimeException("Unknown chapter");
        }
        return chapter;
    }


    private void detectBorders(AugmentedImage augmentedImage) {
        if (augmentedImage.getName().matches(".*r0.*")) {
            if (augmentedImage.getName().matches(".*c0.*")) {
                Log.d(TAG, "Image top left edge recognized");
                // render top left edge shape
                this.topImageView.setVisibility(ImageView.VISIBLE);
                this.leftImageView.setVisibility(ImageView.VISIBLE);
                this.bottomImageView.setVisibility(ImageView.GONE);
                this.rightImageView.setVisibility(ImageView.GONE);

            } else if (augmentedImage.getName().matches(".*c" + (Math.sqrt(this.perfectSquares.get(this.chunksNumberIndex)) - 1) + ".*")) {
                Log.d(TAG, "Image top right edge recognized");
                // render top right edge shape
                this.topImageView.setVisibility(ImageView.VISIBLE);
                this.leftImageView.setVisibility(ImageView.GONE);
                this.bottomImageView.setVisibility(ImageView.GONE);
                this.rightImageView.setVisibility(ImageView.VISIBLE);
            } else {
                Log.d(TAG, "Image side recognized: top side");
                // render top line
                this.topImageView.setVisibility(ImageView.VISIBLE);
                this.leftImageView.setVisibility(ImageView.GONE);
                this.bottomImageView.setVisibility(ImageView.GONE);
                this.rightImageView.setVisibility(ImageView.GONE);
            }
        } else if (augmentedImage.getName().matches(".*r" + (Math.sqrt(this.perfectSquares.get(this.chunksNumberIndex)) - 1) + ".*")) {
            if (augmentedImage.getName().matches(".*c0.*")) {
                Log.d(TAG, "Image bottom left edge recognized");
                // render bottom left edge shape
                this.topImageView.setVisibility(ImageView.GONE);
                this.leftImageView.setVisibility(ImageView.VISIBLE);
                this.bottomImageView.setVisibility(ImageView.VISIBLE);
                this.rightImageView.setVisibility(ImageView.GONE);

            } else if (augmentedImage.getName().matches(".*c" + (Math.sqrt(this.perfectSquares.get(this.chunksNumberIndex)) - 1) + ".*")) {
                Log.d(TAG, "Image bottom right edge recognized");
                // render bottom right edge shape
                this.topImageView.setVisibility(ImageView.GONE);
                this.leftImageView.setVisibility(ImageView.GONE);
                this.bottomImageView.setVisibility(ImageView.VISIBLE);
                this.rightImageView.setVisibility(ImageView.VISIBLE);

            } else {
                Log.d(TAG, "Image side recognized: bottom side");
                // render bottom line
                this.topImageView.setVisibility(ImageView.GONE);
                this.leftImageView.setVisibility(ImageView.GONE);
                this.bottomImageView.setVisibility(ImageView.VISIBLE);
                this.rightImageView.setVisibility(ImageView.GONE);
            }
        } else if (augmentedImage.getName().matches(".*c0.*")) {
            if (augmentedImage.getName().matches(".*r0.*")) {
                Log.d(TAG, "Image top left edge recognized");
                // render top left edge shape
                this.topImageView.setVisibility(ImageView.VISIBLE);
                this.leftImageView.setVisibility(ImageView.VISIBLE);
                this.bottomImageView.setVisibility(ImageView.GONE);
                this.rightImageView.setVisibility(ImageView.GONE);

            } else if (augmentedImage.getName().matches(".*r" + (Math.sqrt(this.perfectSquares.get(this.chunksNumberIndex)) - 1) + ".*")) {
                Log.d(TAG, "Image bottom left edge recognized");
                // render bottom left edge shape
                this.topImageView.setVisibility(ImageView.GONE);
                this.leftImageView.setVisibility(ImageView.VISIBLE);
                this.bottomImageView.setVisibility(ImageView.VISIBLE);
                this.rightImageView.setVisibility(ImageView.GONE);

            } else {
                Log.d(TAG, "Image side recognized: left side");
                // render left line
                this.topImageView.setVisibility(ImageView.GONE);
                this.leftImageView.setVisibility(ImageView.VISIBLE);
                this.bottomImageView.setVisibility(ImageView.GONE);
                this.rightImageView.setVisibility(ImageView.GONE);
            }
        } else if (augmentedImage.getName().matches(".*c" + (Math.sqrt(this.perfectSquares.get(this.chunksNumberIndex)) - 1) + ".*")) {
            if (augmentedImage.getName().matches(".*r0.*")) {
                Log.d(TAG, "Image top right edge recognized");
                // render top right edge shape
                this.topImageView.setVisibility(ImageView.VISIBLE);
                this.leftImageView.setVisibility(ImageView.GONE);
                this.bottomImageView.setVisibility(ImageView.GONE);
                this.rightImageView.setVisibility(ImageView.VISIBLE);

            } else if (augmentedImage.getName().matches(".*r" + (Math.sqrt(this.perfectSquares.get(this.chunksNumberIndex)) - 1) + ".*")) {
                Log.d(TAG, "Image bottom right edge recognized");
                // render bottom right edge shape
                this.topImageView.setVisibility(ImageView.GONE);
                this.leftImageView.setVisibility(ImageView.GONE);
                this.bottomImageView.setVisibility(ImageView.VISIBLE);
                this.rightImageView.setVisibility(ImageView.VISIBLE);

            } else {
                Log.d(TAG, "Image side recognized: right side");
                // render bottom line
                this.topImageView.setVisibility(ImageView.GONE);
                this.leftImageView.setVisibility(ImageView.GONE);
                this.bottomImageView.setVisibility(ImageView.VISIBLE);
                this.rightImageView.setVisibility(ImageView.GONE);
            }
        } else {
            Log.d(TAG, "Image side not recognized");
            this.topImageView.setVisibility(ImageView.GONE);
            this.leftImageView.setVisibility(ImageView.GONE);
            this.bottomImageView.setVisibility(ImageView.GONE);
            this.rightImageView.setVisibility(ImageView.GONE);
        }
        Log.d(TAG, "All checks about sides done");
    }
}