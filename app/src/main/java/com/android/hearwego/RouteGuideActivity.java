package com.android.hearwego;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.odsay.odsayandroidsdk.API;
import com.odsay.odsayandroidsdk.ODsayData;
import com.odsay.odsayandroidsdk.ODsayService;
import com.odsay.odsayandroidsdk.OnResultCallbackListener;
import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;

public class RouteGuideActivity extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback {

    private View decorView; //full screen 객체 선언
    private int uiOption; //full screen 객체 선언

    /*ODSAY 변수 선언*/
    ODsayService oDsayService = null;

    /*텍스트뷰 선언*/
    TextView destination_text;
    TextView guide_text;
    TextView guide_text2;
    TextView guide_text3;
    TextView reDistance_text;

    String appKey = "l7xx59d0bb77ddfc45efb709f48d1b31715c"; //appKey

    /*TMAP 필요 변수 선언*/
    TMapGpsManager tMapGpsManager = null;
    TMapView tMapView = null;
    TMapData tMapData = null;
    TMapPoint nowPoint = null;

    /*TTS 변수 설정*/
    TextToSpeech textToSpeech;

    /*버튼 선언*/
    Button nowgps_btn; //현재 위치 확인 버튼
    Button button_walk; //도보 출발지 지정 버튼
    Button button_subway; //지하철 출발지 지정 버튼
    Button button_previous; //이전 버튼
    Button button_home; //홈 버튼

    /*경도, 위도 변수 선언*/
    Double latitude; //사용자 현재 위도 변수
    Double longitude; //사용자 현재 경도 변수
    String reDistnace;

    /*JSON 받아오기 위한 변수 선언*/
    String latData;//주변시설에서 받아온 목적지 위도
    String longData;//주변시설에서 받아온 목적지 경도
    String uu = null;
    URL url = null;
    HttpURLConnection urlConnection = null;

    /*JSON 변수 선언*/
    JSONObject root = null;
    //도보 경로 안내 구현 위한 배열
    ArrayList<TMapPoint> LatLngArrayList = new ArrayList<>();
    ArrayList<String> DescriptionList = new ArrayList<>();
    //지하철 경로 안내 구현 위한 배열
    ArrayList<String> StationNameList = new ArrayList<>();
    ArrayList<TMapPoint> SubLatLngList = new ArrayList<>();
    //지하철 환승 구현 위한 배열
    ArrayList<String> LaneList = new ArrayList<>(); //지하철 노선명
    ArrayList<String> tranStationList = new ArrayList<>(); //환승역 이름
    ArrayList<Integer> transIndexList = new ArrayList<>(); //환승역 인덱스

    /*실시간 음성안내를 위한 변수 선언*/
    int index = 1;
    int check = 1;
    Double g_latitude = 0.0;
    Double g_longitude = 0.0;
    String description = "";

    /*지하철 경로 구현 위한 변수 선언*/
    int type = 1;
    int subway = 0;
    int subIndex = 0;
    int subwayCount = 0;
    int transIndex = 0;
    int trans_cnt = 0;
    Double s_latitude = 0.0;
    Double s_longitude = 0.0;

    int locNum = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_guide);

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

        /*Odsay 기본 선언*/
        oDsayService = ODsayService.init(getApplicationContext(), "AY0v5KMZR/Ot2fKklprVDD0MW4D7Xnm7o+441agI080");
        oDsayService.setConnectionTimeout(5000);
        oDsayService.setReadTimeout(5000);

        /*TextToSpeech 기본 설정*/
        textToSpeech = new TextToSpeech(getApplicationContext(), status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.KOREAN);
                textToSpeech.setPitch(1.5f);
                textToSpeech.setSpeechRate(1.0f);
            }
        });

        /*TMAP 기본 설정*/
        tMapData = new TMapData();
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey(appKey);
        tMapGpsManager = new TMapGpsManager(this);
        tMapGpsManager.setMinTime(1000);
        tMapGpsManager.setMinDistance(5);
        tMapGpsManager.setProvider(TMapGpsManager.NETWORK_PROVIDER);
        tMapGpsManager.OpenGps();

        /*버튼 설정*/
        nowgps_btn = findViewById(R.id.button2_nowgps);
        button_walk = findViewById(R.id.button_setStartPoint);
        button_subway = findViewById(R.id.button_setSubPoint);
        button_previous = findViewById(R.id.previous); //이전 이미지 버튼 객체 참조
        button_home = findViewById(R.id.home); // 홈 이미지 버튼 객체 참조

        /*인텐트 받아들임*/ //인텐트
        Intent intent = getIntent();
        String nameData = intent.getStringExtra("name");
        latData = intent.getStringExtra("latitude");
        longData = intent.getStringExtra("longitude");
        destination_text = findViewById(R.id.destination_text);
        destination_text.setText(nameData);
        guide_text = findViewById(R.id.guide_message);
        guide_text2 = findViewById(R.id.guide_message2);
        guide_text3 = findViewById(R.id.guide_message3);
        reDistance_text = findViewById(R.id.distance);

        /*현재 위치 확인 버튼 누를 시*/
        nowgps_btn.setOnClickListener(v -> {
            TMapPoint nowpoint = tMapView.getLocationPoint();

            tMapData.convertGpsToAddress(nowpoint.getLatitude(), nowpoint.getLongitude(), s -> textToSpeech.speak("현재 위치는 " + s + "입니다", TextToSpeech.QUEUE_FLUSH, null));
        });

        /*도보 출발지 설정 버튼 누를 시*/
        button_walk.setOnClickListener(v -> {
            TMapPoint nowpoint = tMapView.getLocationPoint();
            String latitude = Double.toString(nowpoint.getLatitude());
            String longitude = Double.toString(nowpoint.getLongitude());

            getRoute(longitude, latitude, longData, latData);
            button_walk.setVisibility(View.INVISIBLE);
            button_subway.setVisibility(View.INVISIBLE);
        });

        /*지하철 버튼 누를 시*/
        button_subway.setOnClickListener(v -> {
            getSubwayStation();
            button_walk.setVisibility(View.INVISIBLE);
            button_subway.setVisibility(View.INVISIBLE);
        });

        //이전 버튼 누를 시 화면 전환
        button_previous.setOnClickListener(v -> {
            locNum = 0;
            Intent intent1 = new Intent(RouteGuideActivity.this, HomeActivity.class);
            startActivity(intent1);
            finish();
        });

        //홈 버튼 누를 시 화면 전환
        button_home.setOnClickListener(v -> {
            locNum = 0;
            Intent intent12 = new Intent(RouteGuideActivity.this, HomeActivity.class);
            startActivity(intent12);
            finish();
        });

    }

    /*지하철 경로 JSON 파일을 가져오는 함수*/
    public void getSubwayStation() {
        TMapPoint nowpoint = tMapView.getLocationPoint();
        String latitude = Double.toString(nowpoint.getLatitude());
        String longitude = Double.toString(nowpoint.getLongitude());
        subway = 1;

        OnResultCallbackListener onResultCallbackListener = new OnResultCallbackListener() {
            @Override
            public void onSuccess(ODsayData oDsayData, API api) {
                try {
                    if (api == API.SEARCH_PUB_TRANS_PATH) {
                        JSONObject result = oDsayData.getJson().getJSONObject("result");
                        JSONObject path = (JSONObject) result.getJSONArray("path").get(0);
                        JSONObject info = path.getJSONObject("info");
                        subwayCount = info.getInt("subwayTransitCount");
                        for (int i = 1; i <= subwayCount; i++) {
                            JSONObject subpath = (JSONObject) path.getJSONArray("subPath").get(2 * i - 1);
                            JSONObject lane = (JSONObject) subpath.getJSONArray("lane").get(0);
                            String subName = lane.getString("name");
                            LaneList.add(subName);
                            JSONObject passStopList = subpath.getJSONObject("passStopList");
                            JSONArray stations = passStopList.getJSONArray("stations");
                            for (int j = 0; j < stations.length(); j++) {
                                JSONObject station = (JSONObject) stations.get(j);
                                String stationName = station.getString("stationName");
                                StationNameList.add(stationName);

                                double longitude = Double.parseDouble(station.getString("x"));
                                double latitude = Double.parseDouble(station.getString("y"));
                                SubLatLngList.add(new TMapPoint(latitude, longitude));
                            }

                        }
                        if (subwayCount > 1) {
                            for (int i = 0; i < StationNameList.size() - 1; i++) {
                                if (StationNameList.get(i).equals(StationNameList.get(i + 1))) {
                                    tranStationList.add(StationNameList.get(i));
                                    transIndexList.add(i);
                                }
                            }

                            Log.d("JSON-ODSAY-환승역", tranStationList.toString());
                            Log.d("JSON-ODSAY-환승역", transIndexList.toString());
                        }

                        Log.d("JSON-ODSAY-역이름", StationNameList.toString());
                        Log.d("JSON-ODSAY-위경도", SubLatLngList.toString());
                        Log.d("JSON-ODSAY-LANE", LaneList.toString());

                        guide_text2.setText(getString(R.string.guidetext, StationNameList.get(0)));
                        getRoute(longitude, latitude, Double.toString(SubLatLngList.get(0).getLongitude()), Double.toString(SubLatLngList.get(0).getLatitude()));

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onError(int i, String s, API api) {
                if (api == API.SEARCH_PUB_TRANS_PATH) {
                    textToSpeech.speak("해당 목적지로 향하는 지하철 경로가 존재하지 않습니다.", TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        };
        oDsayService.requestSearchPubTransPath(longitude, latitude, longData, latData, "0", "0", "1", onResultCallbackListener);
    }

    /*보행자 경로 JSON 파일을 가져오는 함수*/
    public void getRoute(String startX, String startY, String endX, String endY) {

        try {
            String startName = URLEncoder.encode("출발지", "UTF-8");
            String endName = URLEncoder.encode("도착지", "UTF-8");

            uu = "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1&callback=result&appKey=" + appKey
                    + "&startX=" + startX + "&startY=" + startY + "&endX=" + endX + "&endY=" + endY
                    + "&startName=" + startName + "&endName=" + endName;

            System.out.println(uu);
            url = new URL(uu);


        } catch (UnsupportedEncodingException | MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Accept-Charset", "utf-8");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            Log.d("JSON확인", urlConnection.toString());
            NetworkTask networkTask = new NetworkTask(uu, null);
            networkTask.execute();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*Asynctask 클래스 NetworkTask 생성*/
    @SuppressLint("StaticFieldLeak")
    public class NetworkTask extends AsyncTask<Void, Void, String> {
        private final String url;
        private final ContentValues values;


        public NetworkTask(String url, ContentValues values) {
            this.url = url;
            this.values = values;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {
            String result; //요청 결과를 저장하는 변수
            RequestHttpURLConnection requestHttpURLConnection = new RequestHttpURLConnection();
            result = requestHttpURLConnection.request(url, values); //url로부터 결과를 얻어온다.
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try{
                //doInBackground()로 부터 리턴된 값이 onPostExecute()의 매개변수로 넘어오므로 s를 출력한다.
                try {

                    //전체 데이터를 제이슨 객체로 변환
                    root = new JSONObject(s);
                    Log.d("JSON확인", "제일 상위" + root);
                    LatLngArrayList.clear();
                    DescriptionList.clear();

                    JSONArray featuresArray = root.getJSONArray("features"); //총 경로 횟수를 featuresArray에 저장
                    Log.d("JSON확인-feaIndex", Integer.toString(featuresArray.length()));
                    for (int i = 0; i < featuresArray.length(); i++) {
                        JSONObject featuresIndex = (JSONObject) featuresArray.get(i);
                        JSONObject geometry = featuresIndex.getJSONObject("geometry");
                        JSONObject properties = featuresIndex.getJSONObject("properties");
                        String type = geometry.getString("type");
                        JSONArray coordinatesArray = geometry.getJSONArray("coordinates");

                        if (type.equals("Point")) {
                            double f_longitude = Double.parseDouble(coordinatesArray.get(0).toString());
                            double f_latitude = Double.parseDouble(coordinatesArray.get(1).toString());
                            String description = properties.getString("description");
                            DescriptionList.add(description);
                            LatLngArrayList.add(new TMapPoint(f_latitude, f_longitude));
                            if (i == featuresArray.length() - 1) {
                                LatLngArrayList.add(new TMapPoint(f_latitude, f_longitude));
                            }
                        }
                    }
                    Log.d("JSON-ODSAY", LatLngArrayList.toString());
                    Log.d("JSON-ODSAY", DescriptionList.toString());

                    /*첫번째 설명, 남은 거리 구하기 위함*/
                    description = DescriptionList.get(0);
                    guide_text.setText(description);
                    g_latitude = LatLngArrayList.get(1).getLatitude(); //위도
                    g_longitude = LatLngArrayList.get(1).getLongitude(); //경도
                    reDistnace = calcDistance(latitude, longitude, g_latitude, g_longitude);
                    reDistance_text.setText(getString(R.string.reDistance, reDistnace));
                    textToSpeech.speak(description, TextToSpeech.QUEUE_FLUSH, null);


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } catch (NullPointerException e){
                textToSpeech.speak("해당 목적지로 향하는 도보 경로가 존재하지 않습니다.", TextToSpeech.QUEUE_FLUSH, null);
            }

        }

    }

    /*남은 거리 구하는 함수*/
    public String calcDistance(double lat1, double long1, double lat2, double long2) {
        double EARTH_R, Rad, radLat1, radLat2, radDist;
        double distance, ret;

        EARTH_R = 6372.8 * 1000;
        Rad = Math.PI / 180;
        radLat1 = Rad * lat1;
        radLat2 = Rad * lat2;
        radDist = Rad * (long1 - long2);

        distance = Math.sin(radLat1) * Math.sin(radLat2);
        distance = distance + Math.cos(radLat1) * Math.cos(radLat2) * Math.cos(radDist);
        ret = EARTH_R * Math.acos(distance);

        double rslt = Math.round(ret);

        return Double.toString(rslt);
    }

    /*사용자 위치가 변경되면 실행되는 함수*/
    @Override
    public void onLocationChange(Location location) {
        if (locNum == 1) {
            //현재 위치의 위도, 경도를 받아옴
            tMapView.setLocationPoint(location.getLongitude(), location.getLatitude());
            tMapView.setCenterPoint(location.getLongitude(), location.getLatitude());
            nowPoint = tMapView.getLocationPoint();
            latitude = nowPoint.getLatitude();
            longitude = nowPoint.getLongitude();
            Log.d("JSON-LatLng", nowPoint.toString());
            if (root != null && type == 1) {
                if (index < DescriptionList.size()) {
                    getDescription();
                }
            }
            if (subway == 1 && type == 2) {
                getSubRoute();
            }
        }
    }

    public void getDescription() {
        Log.d("JSON확인-check", Integer.toString(check));
        Log.d("JSON확인-index", Integer.toString(index));
        double latitude_gap;
        double longitude_gap;

        /*GPS와 경유지의 위도, 경도 차이 계산*/
        latitude_gap = Math.abs(latitude - g_latitude);
        longitude_gap = Math.abs(longitude - g_longitude);

        /*현재 위치와 경유지까지의 거리 계산*/
        reDistnace = calcDistance(latitude, longitude, g_latitude, g_longitude);
        reDistance_text.setText(getString(R.string.reDistance, reDistnace));
        textToSpeech.speak("다음 목적지까지 " + reDistnace + "미터 남았습니다.", TextToSpeech.QUEUE_FLUSH, null);

        /*위도 경도 차이가 0.00001보다 작을 경우 실행*/
        if (latitude_gap <= 0.00001 || longitude_gap <= 0.00001) {
            if (index < LatLngArrayList.size() - 1) {
                g_latitude = LatLngArrayList.get(index + 1).getLatitude();
                g_longitude = LatLngArrayList.get(index + 1).getLongitude();
            }
            reDistnace = calcDistance(latitude, longitude, g_latitude, g_longitude);
            reDistance_text.setText(getString(R.string.reDistance, reDistnace));
            guide_text.setText(DescriptionList.get(index));
            textToSpeech.speak(DescriptionList.get(index), TextToSpeech.QUEUE_FLUSH, null);

            if (index == DescriptionList.size() - 1) {
                if (subway == 1) {
                    type = 2;
                    Log.d("JSON실행-sub=1,type=2", "실행??");
                } else {
                    tMapGpsManager.CloseGps();
                }
            }
            if (index < DescriptionList.size() - 1) {
                index = index + 1;
                check = check + 1;
            }
        }

    }

    public void getSubRoute() {
        if (subwayCount == 1) { //환승이 없는 경우
            double latitude_gap;
            double longitude_gap;

            s_latitude = SubLatLngList.get(subIndex).getLatitude();
            s_longitude = SubLatLngList.get(subIndex).getLongitude();

            /*현재 위치와 지하철역 위도,경도 차이 계산*/
            latitude_gap = Math.abs(latitude - s_latitude);
            longitude_gap = Math.abs(longitude - s_longitude);

            if (latitude_gap <= 0.00005 || longitude_gap <= 0.00005) {
                reDistance_text.setText(StationNameList.get(subIndex));
                textToSpeech.speak("이번역은 " + StationNameList.get(subIndex)+"역입니다.", TextToSpeech.QUEUE_FLUSH, null);

                if (subIndex == StationNameList.size() - 1) {
                    guide_text.setText("도착역입니다.");
                    Log.d("JSON-Station", latData + longData);
                    Handler handler = new Handler();
                    handler.postDelayed(() -> textToSpeech.speak(guide_text.getText().toString(), TextToSpeech.QUEUE_FLUSH, null), 2500);
                    handler.postDelayed(() -> {
                        getRoute(Double.toString(s_longitude), Double.toString(s_latitude), longData, latData);
                        type = 1;
                        subway = 2;
                    },4000);

                } else {
                    guide_text.setText(getString(R.string.guidetext2, StationNameList.get(subIndex + 1)));
                    Handler handler = new Handler();
                    handler.postDelayed(() -> textToSpeech.speak(guide_text.getText().toString(), TextToSpeech.QUEUE_FLUSH, null), 2500);
                }

                if(trans_cnt == 0){
                    guide_text2.setText(getString(R.string.guidetext3, LaneList.get(0)));
                    Handler handler = new Handler();
                    handler.postDelayed(() -> textToSpeech.speak(guide_text2.getText().toString(), TextToSpeech.QUEUE_FLUSH, null), 5000);
                    trans_cnt++;
                }

                if (subIndex < StationNameList.size() - 1) {
                    Handler handler = new Handler();
                    handler.postDelayed(() -> subIndex = subIndex + 1, 5100);
                }

            }
        } else if (subwayCount > 1) {//환승이 존재하는 경우
            double latitude_gap;
            double longitude_gap;

            //다음 지하철역의 경도, 위도 정보를 가져온다.
            s_latitude = SubLatLngList.get(subIndex).getLatitude();
            s_longitude = SubLatLngList.get(subIndex).getLongitude();
            Log.d("JSON-STATION0", Integer.toString(subIndex));

            //현재 위치와 지하철역 위도,경도 차이 계산
            latitude_gap = Math.abs(latitude - s_latitude);
            longitude_gap = Math.abs(longitude - s_longitude);

            Log.d("JSON-실행0", "실행??");
            if (latitude_gap <= 0.0001 || longitude_gap <= 0.0001) {
                Log.d("JSON-실행1", "실행??");
                reDistance_text.setText(StationNameList.get(subIndex));
                textToSpeech.speak("이번역은 " + StationNameList.get(subIndex)+"역입니다.", TextToSpeech.QUEUE_FLUSH, null);
                Log.d("JSON-STATION1", Integer.toString(subIndex));

                if (subIndex == StationNameList.size() - 1) {
                    guide_text.setText("도착역입니다.");
                    Handler handler = new Handler();
                    handler.postDelayed(() -> textToSpeech.speak(guide_text.getText().toString(), TextToSpeech.QUEUE_FLUSH, null), 2500);
                    handler.postDelayed(() -> {
                        getRoute(Double.toString(s_longitude), Double.toString(s_latitude), longData, latData);
                        type = 1;
                        subway = 2;
                    },4000);
                } else {
                    Log.d("JSON-실행2", "실행??");
                    guide_text.setText(getString(R.string.guidetext2, StationNameList.get(subIndex+1)));
                    Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        Log.d("JSON-STATION2", Integer.toString(subIndex));
                        textToSpeech.speak(guide_text.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
                    }, 2500);

                }

                Log.d("JSON-실행3", "실행??");
                if(trans_cnt == 0){
                    guide_text2.setText(getString(R.string.guidetext4, (subwayCount-trans_cnt-1)));
                    guide_text3.setText(getString(R.string.guidetext5, LaneList.get(transIndex)));
                    Handler handler = new Handler();
                    handler.postDelayed(() -> textToSpeech.speak(guide_text2.getText().toString() + guide_text3.getText().toString(),TextToSpeech.QUEUE_FLUSH,  null), 5000);
                    trans_cnt++;
                }

                if(transIndex < transIndexList.size()){
                    if(subIndex == (transIndexList.get(transIndex)-1)){
                        guide_text3.setText(getString(R.string.guidetext6, tranStationList.get(transIndex),LaneList.get(transIndex+1)));
                        Handler handler = new Handler();
                        handler.postDelayed(() -> textToSpeech.speak("다음역인 " + tranStationList.get(transIndex)+"에서\n"+LaneList.get(transIndex+1)+"으로 환승합니다.",
                                TextToSpeech.QUEUE_FLUSH, null), 5000);
                    } else if(subIndex == transIndexList.get(transIndex)){
                        transIndex = transIndex+1;
                        guide_text2.setText(getString(R.string.guidetext4, (subwayCount-trans_cnt-1)));
                        guide_text3.setText(getString(R.string.guidetext5, LaneList.get(transIndex)));
                        Handler handler = new Handler();
                        handler.postDelayed(() -> textToSpeech.speak(guide_text2.getText().toString() + guide_text3.getText().toString(), TextToSpeech.QUEUE_FLUSH, null), 5000);

                        trans_cnt++;
                    }
                }
                Log.d("JSON-실행4", "실행??");
                if (subIndex < StationNameList.size() - 1) {
                Handler handler = new Handler();
                handler.postDelayed(() -> {
                        subIndex = subIndex + 1;
                        Log.d("JSON-STATION3", Integer.toString(subIndex));
                }, 5100);

                }
            }


        }
    }
}



