package com.android.hearwego;

import android.Manifest;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.SetOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddBookmarkByAddressActivity extends AppCompatActivity {

    //STT
    Intent intent;
    SpeechRecognizer mRecognizer;
    Button sttBtnAddress;
    Button sttBtnKeyword;
    Button saveBookmark;
    TextView textViewAddress;
    TextView textViewKeyword;
    final int PERMISSION = 1;
    TextToSpeech textToSpeech;
    String addressText;
    String keyword;
    Double latitude;
    Double longitude;
    String locname;
    Geocoder geocoder = new Geocoder(this);
    List<Address> list;

    private View decorView; //full screen 객체 선언
    private int	uiOption; //full screen 객체 선언

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_bookmark_byaddress);

        ActionBar actionBar = getSupportActionBar(); //액션바(패키지명) 숨김처리
        actionBar.hide();

        /*전체 화면 모드 -> 소프트 키 없앰*/
        decorView = getWindow().getDecorView();
        uiOption = getWindow().getDecorView().getSystemUiVisibility();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            uiOption |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            uiOption |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            uiOption |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOption);

        //TextToSpeech 기본 설정
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR){
                    textToSpeech.setLanguage(Locale.KOREAN);
                }
            }
        });

        //STT
        // 퍼미션 체크
        if ( Build.VERSION.SDK_INT >= 23 ) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET,
                    Manifest.permission.RECORD_AUDIO},PERMISSION);
        }
        // xml의 버튼과 텍스트 뷰 연결
        textViewAddress = (TextView)findViewById(R.id.sttResult_address);
        textViewKeyword = (TextView)findViewById(R.id.sttResult_keyword);
        sttBtnAddress = (Button) findViewById(R.id.mic_button_address);
        sttBtnKeyword = (Button) findViewById(R.id.mic_button_keyword);
        saveBookmark = (Button) findViewById(R.id.save_bookmark);

        // RecognizerIntent 객체 생성
        Intent intent;
        intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");

        // 버튼을 클릭 이벤트 - 객체에 Context와 listener를 할당한 후 실행
        sttBtnAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                textToSpeech.speak( "즐겨찾기로 추가 할 장소의 주소를 음성으로 입력해주세요", TextToSpeech.QUEUE_FLUSH, null);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mRecognizer = SpeechRecognizer.createSpeechRecognizer(AddBookmarkByAddressActivity.this);
                        mRecognizer.setRecognitionListener(addressListner);
                        mRecognizer.startListening(intent);
                    }},4000);
            }
        });

        sttBtnKeyword.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                textToSpeech.speak( "즐겨찾기 주소에 대한 키워드를 음성으로 입력해주세요", TextToSpeech.QUEUE_FLUSH, null);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mRecognizer = SpeechRecognizer.createSpeechRecognizer(AddBookmarkByAddressActivity.this);
                        mRecognizer.setRecognitionListener(keywordListner);
                        mRecognizer.startListening(intent);
                    }},4000);
            }
        });

        Button button_previous = findViewById(R.id.previous); //이전 이미지 버튼 객체 참조
        Button button_home = findViewById(R.id.home); // 홈 이미지 버튼 객체 참조

        //이전 버튼 누를 시 화면 전환
        button_previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddBookmarkByAddressActivity.this, BookmarkActivity.class);
                startActivity(intent);
                finish();
            }
        });

        //홈 버튼 누를 시 화면 전환
        button_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddBookmarkByAddressActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });

}

    //stt
    //RecognizerIntent 객체에 할당할 listener 생성
    private RecognitionListener addressListner = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Toast.makeText(getApplicationContext(), "음성 인식을 시작합니다.",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResults(Bundle results) {
            //인식 결과가 준비되면 호출
            //말을 하면 ArrayList에 단어를 넣고 textView에 단어를 이어줍니다.
            ArrayList<String> matches =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            for (int i = 0; i < matches.size(); i++){
                addressText = matches.get(i);
                textViewAddress.setText(addressText);   // 음성 인식한 데이터를 text로 변환해 표시
            }
            TextView t = (TextView) findViewById(R.id.sttResult_address);
            String tInput = t.getText().toString();
            textToSpeech.speak(tInput + "으로 주소가 입력 되었습니다.", TextToSpeech.QUEUE_FLUSH, null);

            try {
                list = geocoder.getFromLocationName(addressText,10);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("TAG","주소 변환에서 에러발생");
            }
            latitude = list.get(0).getLatitude();
            longitude = list.get(0).getLongitude();
            locname = list.get(0).getFeatureName();
            list.remove(0);
        }


        @Override
        public void onBeginningOfSpeech() {}

        @Override
        public void onRmsChanged(float rmsdB) {}

        @Override
        public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {}

        @Override
        public void onError(int error) {
            String message;

            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    message = "오디오 에러";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "클라이언트 에러";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "퍼미션 없음";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "네트워크 에러";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "네트웍 타임아웃";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "찾을 수 없음";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RECOGNIZER 가 바쁨";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "서버가 이상함";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "말하는 시간초과";
                    break;
                default:
                    message = "알 수 없는 오류임";
                    break;
            }

            Toast.makeText(getApplicationContext(), "에러가 발생했습니다. : "
                    + message,Toast.LENGTH_SHORT).show();
        }



        @Override
        public void onPartialResults(Bundle partialResults) {}

        @Override
        public void onEvent(int eventType, Bundle params) {}
    };


    //stt_keyword
    //RecognizerIntent 객체에 할당할 listener 생성
    private RecognitionListener keywordListner = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Toast.makeText(getApplicationContext(), "음성 인식을 시작합니다.",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResults(Bundle results) {
            //인식 결과가 준비되면 호출
            //말을 하면 ArrayList에 단어를 넣고 textView에 단어를 이어줍니다.
            ArrayList<String> matches =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            for (int i = 0; i < matches.size(); i++){
                keyword = matches.get(i);
                textViewKeyword.setText(keyword);   // 음성 인식한 데이터를 text로 변환해 표시
            }
            TextView v = (TextView) findViewById(R.id.sttResult_keyword);
            String vInput = v.getText().toString();
            textToSpeech.speak(vInput + "으로 주소에 대한 키워드가 입력 되었습니다.", TextToSpeech.QUEUE_FLUSH, null);
        }


        @Override
        public void onBeginningOfSpeech() {}

        @Override
        public void onRmsChanged(float rmsdB) {}

        @Override
        public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {
            // 즐겨찾기에 저장
            saveBookmark.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    TextView tv = (TextView) findViewById(R.id.sttResult_keyword);
                    String tvInput = tv.getText().toString();
                    textToSpeech.speak( tvInput+"이 즐겨찾기로 저장되었습니다.", TextToSpeech.QUEUE_FLUSH, null);
                    GeoPoint geoPoint = new GeoPoint(latitude, longitude);
                    Map<String, Object> docData = new HashMap<>();
                    Map<String, String> lnData = new HashMap<>();
                    Map<String, GeoPoint> geoData = new HashMap<>();
                    lnData.put(keyword,locname);
                    geoData.put(keyword,geoPoint);
                    docData.put("locnames",lnData);
                    docData.put("geopoints",geoData);
                    ((LogoActivity) LogoActivity.context_logo).db.collection("users").
                            document(((LogoActivity) LogoActivity.context_logo).userID)
                            .set(docData, SetOptions.merge());
                    ((LogoActivity) LogoActivity.context_logo).ref.update("keywords", FieldValue.arrayUnion(keyword));
                    ((LogoActivity) LogoActivity.context_logo).ref.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            LogoActivity.user = documentSnapshot.toObject(User.class);
                        }
                    });
                    Intent intent = new Intent(AddBookmarkByAddressActivity.this, HomeActivity.class);
                    startActivity(intent);
                }
            });


        }

        @Override
        public void onError(int error) {
            String message;

            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    message = "오디오 에러";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "클라이언트 에러";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "퍼미션 없음";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "네트워크 에러";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "네트웍 타임아웃";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "찾을 수 없음";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RECOGNIZER 가 바쁨";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "서버가 이상함";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "말하는 시간초과";
                    break;
                default:
                    message = "알 수 없는 오류임";
                    break;
            }

            Toast.makeText(getApplicationContext(), "에러가 발생했습니다. : "
                    + message,Toast.LENGTH_SHORT).show();
        }



        @Override
        public void onPartialResults(Bundle partialResults) {}

        @Override
        public void onEvent(int eventType, Bundle params) {}
    };
    //tts
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(textToSpeech!=null){
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }
}
