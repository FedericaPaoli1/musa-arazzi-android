package com.musaarazzi;

import androidx.appcompat.app.AppCompatActivity;

import android.media.audiofx.DynamicsProcessing;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.ux.ArFragment;
import com.musaarazzi.augmentedimage.ArRecognitionFragment;
import com.musaarazzi.augmentedimage.ArRecognitionNode;
import com.musaarazzi.common.utils.Chapter;
import com.musaarazzi.common.utils.ChaptersService;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class ArRecognitionActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ArRecognitionActivity";
    private ArFragment arFragment;


    private List<Chapter> chapters;

    private Button virtualModeButton;
    private ImageButton subtitlesButton;
    private TextView chapterName;
    private ImageButton backwardButton;
    private ImageButton playButton;
    private ImageButton pauseButton;
    private ImageButton forwardButton;
    private TextView artworkName;
    private TextView subtitles;
    private ImageButton closeSubsButton;

    private TextToSpeech textToSpeech;

    private ProgressBar progressBar;

    private Pair<AugmentedImage, ArRecognitionNode> currentAugmentedImageNode = null;
    private boolean isModelAdded = false;

    private Thread thread;
    private Object speechLock = new Object();
    private volatile boolean isInterrupted = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arrecognition);

        this.chapters = ChaptersService.readJson(TAG, getApplicationContext());
        this.chapters.get(0).setSelected(true);

        this.virtualModeButton = findViewById(R.id.virtualMode_button);
        this.virtualModeButton.setClickable(false);

        this.subtitlesButton = findViewById(R.id.subtitles);
        this.subtitlesButton.setClickable(false);

        this.artworkName = findViewById(R.id.artworkName);
        this.artworkName.setText(ArRecognitionFragment.DEFAULT_IMAGE_NAME);

        this.chapterName = findViewById(R.id.chapterName);
        this.chapterName.setText(this.chapters.get(0).getTitle());

        this.backwardButton = findViewById(R.id.backward_button);
        this.backwardButton.setClickable(false);

        this.playButton = findViewById(R.id.play_button);
        this.playButton.setClickable(false);

        this.pauseButton = findViewById(R.id.pause_button);
        this.pauseButton.setClickable(false);

        this.forwardButton = findViewById(R.id.forward_button);
        this.forwardButton.setClickable(false);

        this.subtitles = findViewById(R.id.subtitles_text);
        this.subtitles.setText(this.chapters.get(0).getText());
        this.closeSubsButton = findViewById(R.id.subtitles_close_button);

        this.progressBar = findViewById(R.id.progressBar);

        this.thread = new Thread() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    virtualModeButton.setClickable(true);
                    subtitlesButton.setClickable(true);
                    backwardButton.setClickable(true);
                    playButton.setClickable(true);
                    playButton.setVisibility(View.GONE);
                    pauseButton.setClickable(true);
                    pauseButton.setVisibility(View.VISIBLE);
                    forwardButton.setClickable(true);
                });
                while (true) {
                    isInterrupted = false;
                    Chapter chapter = detectChapter();
                    try {
                        String text = subtitles.getText().toString().trim();
                        if (!text.isEmpty()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1");
                            } else {
                                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts2");
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "Text cannot be empty", Toast.LENGTH_LONG).show();
                        }
                        synchronized (speechLock) {
                            speechLock.wait();
                        }
                        if (chapter.getTitle().equals("Quinto capitolo")) {
                            synchronized (this) {
                                try {
                                    this.wait();
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                        runOnUiThread(() -> changeChapter(chapter, true));
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        synchronized (this) {
                            try {
                                isInterrupted = true;
                                this.wait();
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        };

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
                    }

                    @Override
                    public void onDone(String s) {
                        synchronized (speechLock) {
                            speechLock.notify();
                        }
                    }

                    @Override
                    public void onError(String s) {
                    }
                });
            } else {
                Toast.makeText(getApplicationContext(), "TextToSpeech Initialization failed!", Toast.LENGTH_SHORT).show();
            }
        });

        this.arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ar_fragment);

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

    @Override
    public void onClick(View v) {
        Chapter c = detectChapter();
        switch (v.getId()) {
            case R.id.back_button:
                finish();
                break;
            case R.id.virtualMode_button:
                // turn to vitual mode
                break;
            case R.id.subtitles:
                this.subtitles.setVisibility(View.VISIBLE);
                this.closeSubsButton.setVisibility(View.VISIBLE);
                break;
            case R.id.backward_button:
                if(!this.isInterrupted) {
                    this.thread.interrupt();
                }
                textToSpeech.stop();
                changeChapter(c, false);
                if (this.pauseButton.getVisibility() == View.VISIBLE) {
                    synchronized (this.thread) {
                        this.thread.notify();
                    }
                }
                break;
            case R.id.play_button:
                synchronized (this.thread) {
                    this.thread.notify();
                }
                this.playButton.setVisibility(View.GONE);
                this.pauseButton.setVisibility(View.VISIBLE);
                String text = subtitles.getText().toString().trim();
                if (!text.isEmpty()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1");
                    } else {
                        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts2");
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Text cannot be empty", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.pause_button:
                Log.d(TAG, "Paused");
                if(!this.isInterrupted) {
                    this.thread.interrupt();
                }
                textToSpeech.stop();
                this.pauseButton.setVisibility(View.GONE);
                this.playButton.setVisibility(View.VISIBLE);
                break;
            case R.id.forward_button:
                if(!this.isInterrupted) {
                    this.thread.interrupt();
                }
                textToSpeech.stop();
                changeChapter(c, true);
                if (this.pauseButton.getVisibility() == View.VISIBLE) {
                    synchronized (this.thread) {
                        this.thread.notify();
                    }
                }
                break;
            case R.id.subtitles_close_button:
                this.closeSubsButton.setVisibility(View.GONE);
                this.subtitles.setVisibility(View.GONE);
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
                Chapter chapter = detectChapter();
                // Create a new anchor for newly found images.
                Toast.makeText(getApplicationContext(), "Detected Image " + augmentedImage.getName(), Toast.LENGTH_LONG).show();
                Log.d(TAG, "Detected Image " + augmentedImage.getName());
                if (!this.isModelAdded) {
                    ArRecognitionNode node = new ArRecognitionNode(this);
                    node.setImage(augmentedImage, chapter);
                    Log.d(TAG, "Anchor pose " + node.getAnchor().getPose());
                    node.setParent(this.arFragment.getArSceneView().getScene());

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
                this.subtitles.setText(this.chapters.get(this.chapters.indexOf(c) + 1).getText());
            }
        } else {
            if (c != null && this.chapters.indexOf(c) > 0) {
                c.setSelected(false);
                this.chapters.get(this.chapters.indexOf(c) - 1).setSelected(true);
                this.chapterName.setText(this.chapters.get(this.chapters.indexOf(c) - 1).getTitle());
                this.subtitles.setText(this.chapters.get(this.chapters.indexOf(c) - 1).getText());
            }
        }
    }

    private Chapter detectChapter() {
        return this.chapters.stream().filter(ch -> ch.getSelected()).findFirst().orElse(null);
    }
}