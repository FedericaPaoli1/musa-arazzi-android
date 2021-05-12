package com.musaarazzi;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.musaarazzi.augmentedimages.ArRecognitionFragment;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";
    private ImageView imageView;
    private TextView textView;
    private RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.imageView = findViewById(R.id.opere_image_view);
        this.textView = findViewById(R.id.opere_text_view2);
        this.relativeLayout = findViewById(R.id.relativeLayout);
        this.imageView.setImageBitmap(ArRecognitionFragment.loadImage(getAssets(), ArRecognitionFragment.IMAGE_NAME));
        this.textView.setText(ArRecognitionFragment.DEFAULT_IMAGE_NAME);

        Log.d(TAG, "Start button tapped");
        this.relativeLayout.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ArRecognitionActivity.class)));
    }
}
