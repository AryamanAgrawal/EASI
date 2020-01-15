package io.github.padamchopra.EASI;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GestureDetectorCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.method.ScrollingMovementMethod;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener {

    public SharedPreferences sharedPreferences;
    public SharedPreferences.Editor editor;
    static final int REQUEST_VIDEO_CAPTURE = 1;
    public String nameOfSignToAdd;
    public StorageReference mStorageRef;
    public VideoView mVideoView;
    public ArrayList<String> videoList;
    public int videoIndex;
    public boolean alreadyStartedVideos;
    private FirebaseAuth mAuth;
    GestureDetectorCompat gestureDetectorCompat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        this.gestureDetectorCompat = new GestureDetectorCompat(this, this);

        //ask permission
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 2);

        //initialise variables
        videoList = new ArrayList<String>();
        videoIndex = 0;
        alreadyStartedVideos = false;

        //load shared preferences
        sharedPreferences = getSharedPreferences("EASIpref", Context.MODE_PRIVATE);

        //initialise firebase services
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        //load view variables
        mVideoView = findViewById(R.id.signs_video_view);
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if(videoIndex<videoList.size()-1){
                    videoIndex++;
                    mVideoView.setVideoURI(Uri.parse(videoList.get(videoIndex)));
                    mVideoView.start();
                }
            }
        });

        //TextView Scroll
        TextView textView = findViewById(R.id.recognised_text_text_view);
        textView.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser==null){
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }
    }

    public void startListening(View view) {
        //reset any video variables
        videoList.clear();
        videoIndex = 0;
        alreadyStartedVideos = false;

        //ask for permission
        System.out.println("Speech recognition started");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);

        //show user a toast
        final Snackbar snackbar = Snackbar.make(view, "Listening...", Snackbar.LENGTH_INDEFINITE);

        //create new speech recognizer
        SpeechRecognizer speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        RecognitionListener listener = new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                snackbar.show();
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float v) {
            }

            @Override
            public void onBufferReceived(byte[] bytes) {
            }

            @Override
            public void onEndOfSpeech() {
                snackbar.dismiss();
            }

            @Override
            public void onError(int i) {
            }

            @Override
            public void onResults(Bundle bundle) {
                try {
                    ArrayList<String> results = new ArrayList<>(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
                    TextView textView = findViewById(R.id.recognised_text_text_view);
                    String result = results.get(0);
                    textView.setText(result);
                    pickOutPhrases(result.toLowerCase());
                } catch (Error e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Could not understand you.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {
            }

            @Override
            public void onEvent(int i, Bundle bundle) {
            }
        };

        speechRecognizer.setRecognitionListener(listener);
        Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        speechRecognizer.startListening(speechIntent);
    }

    public void pickOutPhrases(String result) {
        System.out.println(result);
        ArrayList<String> resultArrayList = new ArrayList<String>();
        String[] words = result.split(" ");

        //add phrases or one word as elements in result array list
        for (int i=0; i < words.length; i++) {
            String toSearch = words[i];
            if(sharedPreferences.contains(toSearch)){
                String temp = toSearch;
                for(int j=i+1;j<words.length;j++){
                    String temptwo = temp + " " + words[j];
                    if(sharedPreferences.contains(temptwo)){
                        temp = temptwo;
                        i = j;
                    }else{
                        break;
                    }
                }
                resultArrayList.add(temp);
            }else{
                resultArrayList.add(toSearch);
            }
        }

        System.out.println("Result Array List: " + resultArrayList);

        //TODO: Handle video loading and put all letter videos locally
        for (String phrase:resultArrayList){
            String downloadUri = sharedPreferences.getString(phrase,"z");
            if(downloadUri.equals("z")){
                String[] letters = phrase.split("");
                String letterprefix = "android.resource://" + getPackageName() + "/raw/";
                int j = 0;
                if(!alreadyStartedVideos){
                    alreadyStartedVideos = true;
                    j++;
                    videoList.add(letterprefix + getLetterVideo(letters[0]));
                    mVideoView.setVideoURI(Uri.parse(videoList.get(0)));
                }
                for(;j<letters.length;j++){
                    videoList.add(letterprefix + getLetterVideo(letters[j]));
                }
                System.out.println("Split phrase: " + phrase);
            }else{
                //Play video after fetching from URI
                if(alreadyStartedVideos){
                    videoList.add(downloadUri);
                }else{
                    videoList.add(downloadUri);
                    mVideoView.setVideoURI(Uri.parse(downloadUri));
                    alreadyStartedVideos = true;
                }
                System.out.println("Play: " + phrase);
            }
        }
        System.out.println(videoList);
        mVideoView.start();
    }

    //get letter video resource
    public int getLetterVideo(String letter){
        letter = letter.toUpperCase();
        int toReturn = 0;
        switch (letter){
            case "A":
                toReturn = R.raw.a;
                break;
            case "B":
                toReturn = R.raw.b;
                break;
            case "C":
                toReturn = R.raw.c;
                break;
            case "D":
                toReturn = R.raw.d;
                break;
            case "E":
                toReturn = R.raw.e;
                break;
            case "F":
                toReturn = R.raw.f;
                break;
            case "G":
                toReturn = R.raw.g;
                break;
            case "H":
                toReturn = R.raw.h;
                break;
            case "I":
                toReturn = R.raw.i;
                break;
            case "J":
                toReturn = R.raw.j;
                break;
            case "K":
                toReturn = R.raw.k;
                break;
            case "L":
                toReturn = R.raw.l;
                break;
            case "M":
                toReturn = R.raw.m;
                break;
            case "N":
                toReturn = R.raw.n;
                break;
            case "O":
                toReturn = R.raw.o;
                break;
            case "P":
                toReturn = R.raw.p;
                break;
            case "Q":
                toReturn = R.raw.q;
                break;
            case "R":
                toReturn = R.raw.r;
                break;
            case "S":
                toReturn = R.raw.s;
                break;
            case "T":
                toReturn = R.raw.t;
                break;
            case "U":
                toReturn = R.raw.u;
                break;
            case "V":
                toReturn = R.raw.v;
                break;
            case "W":
                toReturn = R.raw.w;
                break;
            case "X":
                toReturn = R.raw.x;
                break;
            case "Y":
                toReturn = R.raw.y;
                break;
            case "Z":
                toReturn = R.raw.z;
                break;
        }
        return toReturn;
    }

    //Add new sign to the sharedpref and database
    public void addNewSign(View view) {
        final EditText editText = findViewById(R.id.sign_title_edit_text);
        final String newSignName = editText.getText().toString().toLowerCase();
        editor = sharedPreferences.edit();

        //check if name is there or it's blank
        if (newSignName.length() < 1) {
            Snackbar.make(view, "Please add a name for your sign.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        //check if sign already exists or not
        if (sharedPreferences.getString(newSignName, "z").equals("z")) {
            Snackbar snackbar = Snackbar.make(view, "Sign already exists.", Snackbar.LENGTH_SHORT);
            snackbar.setAction("Rewrite", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    editor.remove(newSignName);
                    editor.apply();
                    addNewSign(view);
                }
            });
        }

        //finally create the sign
        String signTillNow = "";
        String[] signArray = newSignName.split(" ");
        for (int i = 0; i < signArray.length - 1; i++) {
            signTillNow = signTillNow + signArray[i] + " ";
            if (!sharedPreferences.contains(signTillNow.trim())) {
                editor.putString(signTillNow.trim(), "a");
            }
        }
        editor.apply();
        signTillNow = signTillNow + signArray[signArray.length - 1];
        nameOfSignToAdd = signTillNow.trim();
        record();
    }

    //record video
    public void record() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    //wait for video to be recorded then upload
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            Uri videoUri = intent.getData();

            try {
                final StorageReference signRef = mStorageRef.child(mAuth.getCurrentUser().getEmail() + "-" + nameOfSignToAdd + ".mp4");

                signRef.putFile(videoUri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // Get a URL to the uploaded content
                                Toast.makeText(getApplicationContext(), "Sign Added", Toast.LENGTH_SHORT).show();
                                signRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        editor.putString(nameOfSignToAdd, uri.toString());
                                        editor.apply();
                                    }
                                });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                            }
                        });
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        this.gestureDetectorCompat.onTouchEvent(motionEvent);
        return super.onTouchEvent(motionEvent);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e2.getY() - e1.getY() > 50) {
            //swipe down
            System.out.println("down");
            startListening(null);

            return true;
        } else if (e2.getX() - e1.getX() > 50) {
            //swipe right
            System.out.println("right");
            return true;
        } else if (e1.getX() - e2.getX() > 50) {
            //swipe left
            System.out.println("left");
            return true;
        } else {
            return false;
        }
    }
}
