package com.musaarazzi;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import android.util.Pair;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;

import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.musaarazzi.augmentedimage.ArRecognitionFragment;
import com.musaarazzi.augmentedimage.ArRecognitionNode;
import com.musaarazzi.common.utils.Chapter;
import com.musaarazzi.common.utils.ChaptersService;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.Collection;
import java.util.List;

public class ArRecognitionActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "AugmentedImageActivity";
    private ArFragment arFragment;


    private List<Chapter> chapters;

    private TextView chapterName;
    private ImageButton playButton;
    private ImageButton pauseButton;
    private TextView artworkName;

    private ProgressBar progressBar;

    private Pair<AugmentedImage, ArRecognitionNode> currentAugmentedImageNode = null;
    private boolean isModelAdded = false;

    private Thread thread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.chapters = ChaptersService.readJson(TAG, getApplicationContext());
        this.chapters.get(0).setSelected(true);

        this.artworkName = findViewById(R.id.artworkNameTextView);
        this.artworkName.setText(ArRecognitionFragment.DEFAULT_IMAGE_NAME);

        this.chapterName = findViewById(R.id.chapterNameTextView);
        this.chapterName.setText(this.chapters.get(0).getTitle());

        this.playButton = findViewById(R.id.playButton);
        this.pauseButton = findViewById(R.id.pauseButton);

        this.progressBar = findViewById(R.id.progressBar);

        this.thread = new Thread() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    playButton.setVisibility(View.GONE);
                    pauseButton.setVisibility(View.VISIBLE);
                });
                while (true) {
                    Chapter chapter = detectChapter();
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
                        runOnUiThread(() -> changeChapter(chapter, true));
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

        this.arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        this.arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        Chapter c = detectChapter();
        switch (v.getId()) {
            case R.id.backButton:
                // TODO going back from other activities, maybe from virtual mode
                break;
            case R.id.virtualModeButton:
                // TODO go to virtual mode
                break;
            case R.id.subtitlesButton:
                // show subtitles
                break;
            case R.id.backwardButton:
                this.thread.interrupt();
                changeChapter(c, false);
                synchronized (this.thread) {
                    this.thread.notify();
                }
                break;
            case R.id.playButton:
                synchronized (this.thread) {
                    this.thread.notify();
                }
                this.playButton.setVisibility(View.GONE);
                this.pauseButton.setVisibility(View.VISIBLE);
                break;
            case R.id.pauseButton:
                Log.d(TAG, "Paused");
                this.thread.interrupt();
                this.pauseButton.setVisibility(View.GONE);
                this.playButton.setVisibility(View.VISIBLE);
                break;
            case R.id.forwardButton:
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

    private void onUpdateFrame(FrameTime frameTime) {

        Frame frame = this.arFragment.getArSceneView().getArFrame();

        // If there is no frame, just return.
        if (frame == null) {
            return;
        }

        Collection<AugmentedImage> updatedAugmentedImages =
                frame.getUpdatedTrackables(AugmentedImage.class);
        for (AugmentedImage augmentedImage : updatedAugmentedImages) {
            if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {
                Log.d(TAG, "Detected Image " + augmentedImage.getName());
                Chapter chapter = detectChapter();
                // Create a new anchor for newly found images.
                if (!this.isModelAdded) {
                    ArRecognitionNode node = new ArRecognitionNode(this);
                    node.setImage(augmentedImage, chapter);

                    this.currentAugmentedImageNode = new Pair<>(augmentedImage, node);

                    this.progressBar.setVisibility(View.GONE);
                    if (this.currentAugmentedImageNode != null && !this.thread.isAlive()) {
                        this.thread.start();
                    }
                    this.arFragment.getArSceneView().getScene().addChild(node);
                    this.isModelAdded = true;
                }
            }
        }
    }

    private void changeChapter(Chapter c, boolean isForward) {
        this.isModelAdded = false;
        this.arFragment.getArSceneView().getScene().removeChild(this.currentAugmentedImageNode.second);
        if (isForward) {
            if (c != null && this.chapters.indexOf(c) < this.chapters.size() - 1) {
                c.setSelected(false);
                this.chapters.get(this.chapters.indexOf(c) + 1).setSelected(true);
                this.chapterName.setText(this.chapters.get(this.chapters.indexOf(c) + 1).getTitle());
            }
        } else {
            if (c != null && this.chapters.indexOf(c) > 0) {
                c.setSelected(false);
                this.chapters.get(this.chapters.indexOf(c) - 1).setSelected(true);
                this.chapterName.setText(this.chapters.get(this.chapters.indexOf(c) - 1).getTitle());
            }
        }
    }

    private Chapter detectChapter() {
        return this.chapters.stream().filter(ch -> ch.getSelected()).findFirst().orElse(null);
    }
}
