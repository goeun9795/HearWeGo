package com.android.hearwego;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SurroundingChoice extends AppCompatActivity {

    private View decorView; //full screen 객체 선언
    private int	uiOption; //full screen 객체 선언
    public String addressData;
    public String nameData;
    public String distanceData;
    public String latData;
    public String longData;

    /*텍스트뷰 선언*/
    TextView nameText;
    TextView addressText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.surrounding_choice);

        ActionBar actionBar = getSupportActionBar(); //액션바(패키지명) 숨김처리
        actionBar.hide();


        /*전체 화면 모드 -> 소프트 키 없앰*/
        decorView = getWindow().getDecorView();
        uiOption = getWindow().getDecorView().getSystemUiVisibility();
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH )
            uiOption |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN )
            uiOption |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT )
            uiOption |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility( uiOption );

        Button button_add_bookmark = findViewById(R.id.add_bookmark);
        Button button_set_destination = findViewById(R.id.set_destination);

        Button button_previous = findViewById(R.id.previous); //이전 버튼 객체 참조
        Button button_home = findViewById(R.id.home); // 홈 버튼 객체 참조

        /*이전 버튼 누를 시 화면 전환*/
        button_previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        /*홈 버튼 누를 시 화면 전환*/
        button_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SurroundingChoice.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });

        /*인텐트 받아들임*/
        Intent intent = getIntent();
        nameData = intent.getStringExtra("name");
        addressData = intent.getStringExtra("address");
        distanceData = intent.getStringExtra("distance");
        latData = intent.getStringExtra("latitude");
        longData = intent.getStringExtra("longitude");



        Log.d("현재-위치!", nameData + addressData);

        /*목적지 이름, 주소 텍스트 설정 (주소는 동까지만 뜸. )*/
        nameText = findViewById(R.id.destination);
        nameText.setText(nameData);
        addressText = findViewById(R.id.destination_address);
        addressText.setText(addressData + " " + distanceData);

        /*'목적지로 설정' 버튼 눌렀을 때*/
        button_set_destination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*인텐트 보냄*/
                Intent dest_intent = new Intent(SurroundingChoice.this, RouteGuideActivity.class);
                dest_intent.putExtra("name", nameData);
                dest_intent.putExtra("latitude", latData);
                dest_intent.putExtra("longitude", longData);
                startActivity(dest_intent);
            }

        });

        //즐겨찾기 등록 버튼을 눌렀을 때
        button_add_bookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent bookmark_intent = new Intent(SurroundingChoice.this, AddBookmarkActivity.class);
                bookmark_intent.putExtra("locname", nameData);
                bookmark_intent.putExtra("latitude", latData);
                bookmark_intent.putExtra("longitude", longData);
                startActivity(bookmark_intent);
            }
        });


    }


}