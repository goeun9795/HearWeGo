package com.android.hearwego;

import android.Manifest;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapView;
import com.skt.Tmap.poi_item.TMapPOIItem;

import java.util.Locale;

public class FacilityActivity extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback {

    private View decorView; //full screen 객체 선언
    private int	uiOption; //full screen 객체 선언

    String appKey = "l7xx59d0bb77ddfc45efb709f48d1b31715c"; //appKey

    /*TMAP 필요 변수 선언*/
    TMapGpsManager tMapGps = null;
    TMapView tMapView = null;
    TMapData tMapData = null;
    TMapPoint nowPoint = null;

    /*SKT 타워 위도와 현재 위치의 위도를 비교하기 위한 변수*/
    String SKT_latitude = Double.toString(37.566474);
    String n_latitude = null;

    /*버튼선언*/
    Button button_facility1;
    Button button_facility2;
    Button button_facility3;
    Button button_facility4;
    Button button_facility5;
    Button button_facility6;
    Button button_facility7;
    Button button_facility8;
    Button button_facility9;
    Button button_facility10;

    int FacNum = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.category_facility);

        /*Tmap 기본설정*/
        tMapData = new TMapData();
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey(appKey);

        /*위치 권한 요청*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        /*TMapGPS 설정*/
        tMapGps = new TMapGpsManager(this);
        tMapGps.setMinTime(1000);
        tMapGps.setMinDistance(5);
        tMapGps.setProvider(TMapGpsManager.NETWORK_PROVIDER);
        tMapGps.OpenGps();

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

        /*공공기관 버튼 id 지정*/
        button_facility1 = (Button)findViewById(R.id.facility1);
        button_facility2 = (Button)findViewById(R.id.facility2);
        button_facility3 = (Button)findViewById(R.id.facility3);
        button_facility4 = (Button)findViewById(R.id.facility4);
        button_facility5 = (Button)findViewById(R.id.facility5);
        button_facility6 = (Button)findViewById(R.id.facility6);
        button_facility7 = (Button)findViewById(R.id.facility7);
        button_facility8 = (Button)findViewById(R.id.facility8);
        button_facility9 = (Button)findViewById(R.id.facility9);
        button_facility10 = (Button)findViewById(R.id.facility10);

        Button button_previous = findViewById(R.id.previous); //이전 이미지 버튼 객체 참조
        Button button_home = findViewById(R.id.home); // 홈 이미지 버튼 객체 참조

        //이전 버튼 누를 시 화면 전환
        button_previous.setOnClickListener(v -> {
            Intent intent = new Intent(FacilityActivity.this, SurroundingActivity.class);
            startActivity(intent);
            finish();
        });

        //홈 버튼 누를 시 화면 전환
        button_home.setOnClickListener(v -> {
            Intent intent = new Intent(FacilityActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });


    }

    /*현재 위치가 변화될 때 사용되는 함수*/
    @Override
    public void onLocationChange(Location location) {
        if(FacNum == 1){
            tMapView.setLocationPoint(location.getLongitude(), location.getLatitude());
            nowPoint = tMapView.getLocationPoint();
            Log.d("공공기관-현재위치", nowPoint.toString());

            /*Tmap 기본 위치가 SKT 타워로 설정되어있음.
             * SKT 타워 주변의 공공기관이 뜨지 않게 만들기 위해서
             * SKT 타워 경도와 진짜 현재 위치의 경도를 비교*/
            n_latitude = Double.toString(nowPoint.getLatitude());
            if(n_latitude.equals(SKT_latitude)){
                Log.d("현재위치-SKT타워O", "실행되었습니다.");
            } else{
                //현재 위치 탐색 완료 후 주변 공공기관 찾기 시작
                Log.d("현재위치-SKT타워X", "실행되었습니다.");
                //주변 반경 2km 지정, 가까운 순서대로 출력, 버튼이 10개라 10개의 공공기관을 가져온다.
                tMapData.findAroundNamePOI(nowPoint, "공공기관", 2, 10, arrayList -> {
                    FacNum = 0;
                    try {
                        for(int i = 0;i<10;i++){
                            TMapPOIItem item = arrayList.get(i);
                            Log.d("공공기관-현재위치이름", item.getPOIName());
                            switch (i){
                                case 0:
                                    button_facility1.setText(getString(R.string.getPOIname, item.getPOIName(), String.format(Locale.getDefault(),"%.2f", item.getDistance(nowPoint))));
                                    button_facility1.setOnClickListener(v -> {
                                        tMapGps.CloseGps();
                                        Intent fac_intent = new Intent(FacilityActivity.this, SurroundingChoice.class);
                                        fac_intent.putExtra("name", item.getPOIName());
                                        fac_intent.putExtra("address", item.getPOIAddress());
                                        fac_intent.putExtra("latitude", item.noorLat);
                                        fac_intent.putExtra("longitude", item.noorLon);
                                        fac_intent.putExtra("distance", String.format(Locale.getDefault(), "%.2f", item.getDistance(nowPoint))+"M");
                                        startActivity(fac_intent);
                                        finish();
                                    });
                                    break;
                                case 1:
                                    button_facility2.setText(getString(R.string.getPOIname, item.getPOIName(), String.format(Locale.getDefault(),"%.2f", item.getDistance(nowPoint))));
                                    button_facility2.setOnClickListener(v -> {
                                        tMapGps.CloseGps();
                                        Intent fac_intent = new Intent(FacilityActivity.this, SurroundingChoice.class);
                                        fac_intent.putExtra("name", item.getPOIName());
                                        fac_intent.putExtra("address", item.getPOIAddress());
                                        fac_intent.putExtra("latitude", item.noorLat);
                                        fac_intent.putExtra("longitude", item.noorLon);
                                        fac_intent.putExtra("distance", String.format(Locale.getDefault(), "%.2f", item.getDistance(nowPoint))+"M");
                                        startActivity(fac_intent);
                                        finish();
                                    });
                                    break;
                                case 2:
                                    button_facility3.setText(getString(R.string.getPOIname, item.getPOIName(), String.format(Locale.getDefault(),"%.2f", item.getDistance(nowPoint))));
                                    button_facility3.setOnClickListener(v -> {
                                        tMapGps.CloseGps();
                                        Intent fac_intent = new Intent(FacilityActivity.this, SurroundingChoice.class);
                                        fac_intent.putExtra("name", item.getPOIName());
                                        fac_intent.putExtra("address", item.getPOIAddress());
                                        fac_intent.putExtra("latitude", item.noorLat);
                                        fac_intent.putExtra("longitude", item.noorLon);
                                        fac_intent.putExtra("distance", String.format(Locale.getDefault(),"%.2f", item.getDistance(nowPoint))+"M");
                                        startActivity(fac_intent);
                                        finish();
                                    });
                                    break;
                                case 3:
                                    button_facility4.setText(getString(R.string.getPOIname, item.getPOIName(), String.format(Locale.getDefault(),"%.2f", item.getDistance(nowPoint))));
                                    button_facility4.setOnClickListener(v -> {
                                        tMapGps.CloseGps();
                                        Intent fac_intent = new Intent(FacilityActivity.this, SurroundingChoice.class);
                                        fac_intent.putExtra("name", item.getPOIName());
                                        fac_intent.putExtra("address", item.getPOIAddress());
                                        fac_intent.putExtra("latitude", item.noorLat);
                                        fac_intent.putExtra("longitude", item.noorLon);
                                        fac_intent.putExtra("distance", String.format(Locale.getDefault(),"%.2f", item.getDistance(nowPoint))+"M");
                                        startActivity(fac_intent);
                                        finish();
                                    });
                                    break;
                                case 4:
                                    button_facility5.setText(getString(R.string.getPOIname, item.getPOIName(), String.format(Locale.getDefault(),"%.2f", item.getDistance(nowPoint))));
                                    button_facility5.setOnClickListener(v -> {
                                        tMapGps.CloseGps();
                                        Intent fac_intent = new Intent(FacilityActivity.this, SurroundingChoice.class);
                                        fac_intent.putExtra("name", item.getPOIName());
                                        fac_intent.putExtra("address", item.getPOIAddress());
                                        fac_intent.putExtra("latitude", item.noorLat);
                                        fac_intent.putExtra("longitude", item.noorLon);
                                        fac_intent.putExtra("distance", String.format(Locale.getDefault(),"%.2f", item.getDistance(nowPoint))+"M");
                                        startActivity(fac_intent);
                                        finish();
                                    });
                                    break;
                                case 5:
                                    button_facility6.setText(getString(R.string.getPOIname, item.getPOIName(), String.format(Locale.getDefault(),"%.2f", item.getDistance(nowPoint))));
                                    button_facility6.setOnClickListener(v -> {
                                        tMapGps.CloseGps();
                                        Intent fac_intent = new Intent(FacilityActivity.this, SurroundingChoice.class);
                                        fac_intent.putExtra("name", item.getPOIName());
                                        fac_intent.putExtra("address", item.getPOIAddress());
                                        fac_intent.putExtra("latitude", item.noorLat);
                                        fac_intent.putExtra("longitude", item.noorLon);
                                        fac_intent.putExtra("distance", String.format(Locale.getDefault(),"%.2f", item.getDistance(nowPoint))+"M");
                                        startActivity(fac_intent);
                                        finish();
                                    });
                                    break;
                                case 6:
                                    button_facility7.setText(getString(R.string.getPOIname, item.getPOIName(), String.format(Locale.getDefault(),"%.2f", item.getDistance(nowPoint))));
                                    button_facility7.setOnClickListener(v -> {
                                        tMapGps.CloseGps();
                                        Intent fac_intent = new Intent(FacilityActivity.this, SurroundingChoice.class);
                                        fac_intent.putExtra("name", item.getPOIName());
                                        fac_intent.putExtra("address", item.getPOIAddress());
                                        fac_intent.putExtra("latitude", item.noorLat);
                                        fac_intent.putExtra("longitude", item.noorLon);
                                        fac_intent.putExtra("distance", String.format(Locale.getDefault(),"%.2f", item.getDistance(nowPoint))+"M");
                                        startActivity(fac_intent);
                                        finish();
                                    });
                                    break;
                                case 7:
                                    button_facility8.setText(getString(R.string.getPOIname, item.getPOIName(), String.format(Locale.getDefault(),"%.2f", item.getDistance(nowPoint))));
                                    button_facility8.setOnClickListener(v -> {
                                        tMapGps.CloseGps();
                                        Intent fac_intent = new Intent(FacilityActivity.this, SurroundingChoice.class);
                                        fac_intent.putExtra("name", item.getPOIName());
                                        fac_intent.putExtra("address", item.getPOIAddress());
                                        fac_intent.putExtra("latitude", item.noorLat);
                                        fac_intent.putExtra("longitude", item.noorLon);
                                        fac_intent.putExtra("distance", String.format(Locale.getDefault(),"%.2f", item.getDistance(nowPoint))+"M");
                                        startActivity(fac_intent);
                                        finish();
                                    });
                                    break;
                                case 8:
                                    button_facility9.setText(getString(R.string.getPOIname, item.getPOIName(), String.format(Locale.getDefault(),"%.2f", item.getDistance(nowPoint))));
                                    button_facility9.setOnClickListener(v -> {
                                        tMapGps.CloseGps();
                                        Intent fac_intent = new Intent(FacilityActivity.this, SurroundingChoice.class);
                                        fac_intent.putExtra("name", item.getPOIName());
                                        fac_intent.putExtra("address", item.getPOIAddress());
                                        fac_intent.putExtra("latitude", item.noorLat);
                                        fac_intent.putExtra("longitude", item.noorLon);
                                        fac_intent.putExtra("distance", String.format(Locale.getDefault(),"%.2f", item.getDistance(nowPoint))+"M");
                                        startActivity(fac_intent);
                                        finish();
                                    });
                                    break;
                                case 9:
                                    button_facility10.setText(getString(R.string.getPOIname, item.getPOIName(), String.format(Locale.getDefault(),"%.2f", item.getDistance(nowPoint))));
                                    button_facility10.setOnClickListener(v -> {
                                        tMapGps.CloseGps();
                                        Intent fac_intent = new Intent(FacilityActivity.this, SurroundingChoice.class);
                                        fac_intent.putExtra("name", item.getPOIName());
                                        fac_intent.putExtra("address", item.getPOIAddress());
                                        fac_intent.putExtra("latitude", item.noorLat);
                                        fac_intent.putExtra("longitude", item.noorLon);
                                        fac_intent.putExtra("distance", String.format(Locale.getDefault(),"%.2f", item.getDistance(nowPoint))+"M");
                                        startActivity(fac_intent);
                                        finish();
                                    });
                                    break;
                                default:
                                    Log.d("공공기관-오류", "해당하는 버튼이 없습니다.");
                            }
                        }
                    }catch (IndexOutOfBoundsException e){
                        e.printStackTrace();
                    }

                });
            }
        }


        }

    }

