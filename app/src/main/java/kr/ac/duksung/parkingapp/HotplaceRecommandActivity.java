package kr.ac.duksung.parkingapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HotplaceRecommandActivity extends AppCompatActivity implements OnMapReadyCallback, Overlay.OnClickListener {

    // 위치 권한을 받아오기 위함
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] PERMSSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private static double[][] placeLocation;
    private static String[] placeName;
    private static String[] placeAddress;
    private static String[] placeProperty;
    private static FusedLocationSource mLocationSource;
    private static NaverMap mNaverMap;
    private InfoWindow mInfoWindow;
    private int placenum;
    ImageView forwardArrow;
    private boolean isDataLoaded=false;
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private Marker [] markerList = new Marker[100];

    private Button closeButton;
    private static Map< String, String > propertyarr = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        placeLocation = new double[10][2];
        placeName = new String[10];
        placeAddress = new String[10];
        placeProperty = new String[10];
        // 서버 연결 시작
        Log.d("TEST2", "시작");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getString(R.string.ip))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        crud_RetrofitAPI result = retrofit.create(crud_RetrofitAPI.class);

        //POST
        HashMap<String, Object> param = new HashMap<>();
        param.put("plotid", 1);

        result.postPlaceData(param).enqueue(new Callback<List<crud_hotplaceResult>>() {
            @Override
            public void onResponse(Call<List<crud_hotplaceResult>> call, Response<List<crud_hotplaceResult>> response) {
                if(response.isSuccessful()) {
                    List<crud_hotplaceResult> data = response.body();
                    Log.d("GETPLACE", "POST 성공 " + data.size() + data.get(1).getPlaceProperty());
                    placenum = data.size();
                    for (int i =0;i<data.size();i++) {
                        Log.d("PLACEGET: ", data.get(i).getPlaceName());
                        Log.d("PLACEGET: ", data.get(i).getPlaceLatitude()+" "+ data.get(i).getPlaceLongitude());
                        Log.d("PLACEGET: ", data.get(i).getPlaceAddress());
                        Log.d("PLACEGET: ", data.get(i).getPlaceProperty());

                        placeLocation[i][0] = Double.valueOf(data.get(i).getPlaceLatitude());
                        placeLocation[i][1] = Double.valueOf(data.get(i).getPlaceLongitude());
                        placeName[i]=data.get(i).getPlaceName();
                        placeAddress[i]=data.get(i).getPlaceAddress();
                        placeProperty[i] = data.get(i).getPlaceProperty();
                        isDataLoaded = true;

                    }

                    //지도 객체 생성
                    FragmentManager fm = getSupportFragmentManager();
                    MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.recommandMap);
                    //위치를 반환하는 구현체인 FusedLocationSource 생성
                    mLocationSource = new FusedLocationSource(HotplaceRecommandActivity.this, PERMISSION_REQUEST_CODE);
                    Log.d("PLACE", "SUCCESS");
                    if(mapFragment==null){
                        mapFragment = MapFragment.newInstance();
                        fm.beginTransaction().add(R.id.recommandMap, mapFragment).commit();
                    }
                    //getMapAsync를 호출하여 비동기로 onMapReady 콜백 메서드 호출
                    //onMapReady에서 NaverMap 객체를 받음
                    mapFragment.getMapAsync(HotplaceRecommandActivity.this);
                }
            }

            @Override
            public void onFailure(Call<List<crud_hotplaceResult>> call, Throwable t) {
                Log.d("TEST", "POST 실패");
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                String exceptionAsString = sw.toString();
                Log.e("TEST", exceptionAsString);
                t.printStackTrace();
            }
        });
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotplace_recommand);

        slidingUpPanelLayout = findViewById(R.id.sliding_layout);
        final ListView listView = findViewById(R.id.listView);
        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                placeName));



        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                for (int i = 0; i < markerList.length; i++) {
                    if (placeName[position].equals((String) markerList[i].getTag())) {
                        // 마커를 클릭한 것처럼 동작
                        Log.d("TAG", (String)markerList[i].getTag());
                        // SlidingUpPanelLayout를 닫기 위해
                        slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                        //dragView.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                        mNaverMap.moveCamera(CameraUpdate.scrollTo(new LatLng(placeLocation[i][0], placeLocation[i][1]))
                                .animate(CameraAnimation.Easing, 300)); // 위치로 이동하는 애니메이션 설정
                        markerList[i].performClick();
                        break;
                    }
                }
            }
        });
        // 어댑터 선언 및 리스트 뷰에 지정
        /*
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,placeName){
            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view.findViewById(android.R.id.text1);
                tv.setTextColor(getResources().getColor(R.color.dark_black));
                return view;
            }
        };

         */


        forwardArrow = findViewById(R.id.recommandBar_forwardArrow);

        forwardArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });



    }//onCreate

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        if (isDataLoaded) {

            //NaverMAP 객체 받아서 NaverMap 객체에 위치 소스 지정
            mNaverMap = naverMap;
            mNaverMap.setLocationSource(mLocationSource);
            //권한확인. 결과는 onRequestPermissionsResult 콜백 매서드 호출
            ActivityCompat.requestPermissions(this, PERMSSIONS, PERMISSION_REQUEST_CODE);

            // 마커 표시하기

            // 리스트 별로 마커 차례대로 표시함
            for (int i=0; i<10; i++) {
                markerList[i] = new Marker();
                markerList[i].setTag(placeName[i]);
                markerList[i].setSubCaptionText(placeAddress[i]);
                markerList[i].setPosition(new LatLng(placeLocation[i][0], placeLocation[i][1]));
                try{
                    markerList[i].setMap(naverMap);
                    Log.d( "TAG", "setMap 성공");
                }
                catch (Exception e) {
                    e.printStackTrace();
                    Log.d( "TAG", "예외 발생");
                }
                markerList[i].setOnClickListener(this);
                markerList[i].setWidth(100);
                markerList[i].setHeight(100);


                // marker2 = 노란색 marker3 = 보라색 none_marker = 회색
                if(placeProperty[i].equals("핫플"))    {
                    markerList[i].setIcon(OverlayImage.fromResource(R.drawable.m_hotple));
                }
                else if(placeProperty[i].equals("음식점")){
                    markerList[i].setIcon(OverlayImage.fromResource(R.drawable.m_restaurant));
                }
                else if(placeProperty[i].equals("주유소")) {
                    markerList[i].setIcon(OverlayImage.fromResource(R.drawable.m_oil));
                }
                propertyarr.put(placeName[i], placeProperty[i]);

            }
            markerList[11] = new Marker();
            markerList[11].setTag("예약한 주차장");
            markerList[11].setPosition(new LatLng(R.string.latitude, R.string.longitude));
            markerList[11].setMap(naverMap);
            markerList[11].setIcon(OverlayImage.fromResource(R.drawable.m_parkinglot));
        }







        //InfoWindow 객체 생성
        mInfoWindow = new InfoWindow();
        mInfoWindow.setAdapter(new InfoWindow.DefaultTextAdapter(this){
            @NonNull
            @Override
            public CharSequence getText(@NonNull InfoWindow infoWindow){
                return (CharSequence)infoWindow.getMarker().getTag();
            }
        });//mInfoWindow.setAdapter

    }//onMapReady(NaverMap)

    public void showAlertDialog(Marker marker) {
        // 클릭 시 다이얼로그 생성
        AlertDialog.Builder d = new AlertDialog.Builder(HotplaceRecommandActivity.this);
        View view = LayoutInflater.from(HotplaceRecommandActivity.this).inflate(
                R.layout.layout_dialog_hotplace,
                (LinearLayout)findViewById(R.id.layoutDialog_hotplace)
        );

        d.setView(view);

        //제목
        ((TextView)view.findViewById(R.id.textTitle)).setText((CharSequence) marker.getTag());
        //종류
        ((TextView)view.findViewById(R.id.typeText)).setText(propertyarr.get((CharSequence)marker.getTag()));

        ImageView icon_image = findViewById(R.id.icon_dialog_hotplace);

        /*

        if( propertyarr.get((CharSequence)marker.getTag()).equals("음식점") ){
            icon_image.setImageResource(R.drawable.m_restaurant);
        } else if ( propertyarr.get((CharSequence)marker.getTag()).equals("핫플") ) {
            icon_image.setImageResource(R.drawable.m_hotple);
        } else {
            icon_image.setImageResource(R.drawable.m_oil);
        }

         */


        //주소
        ((TextView)view.findViewById(R.id.addressText)).setText((CharSequence) marker.getSubCaptionText());
        //버튼 - 취소
        closeButton = (Button) view.findViewById(R.id.close);

        // 다이얼로그 창
        AlertDialog alertDialog = d.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));







        alertDialog.show();

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });
    }//showAlertDialog

    //내 위치 받아와서 네이버 지도에 표시해줌
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //request code와 권한획득 여부 확인
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
                Log.d("PLACE", "SUCCESS");
            }
        }
    }//onRequestPermissionsResult


    @Override
    public boolean onClick(@NonNull Overlay overlay) {

        if (overlay instanceof Marker) {
            Marker marker = (Marker) overlay;

            if (marker.getInfoWindow() != null) {
                mInfoWindow.close();
            } else {
                mInfoWindow.open(marker);
                // mInfoWindow 클릭 시 상세정보 띄우기
                mInfoWindow.setOnClickListener(new Overlay.OnClickListener() {
                    @Override
                    public boolean onClick(@NonNull Overlay overlay) {
                        showAlertDialog(marker);
                        return true;
                    }
                });
            }
            return true;
        }
        return false;
    }//onClick(NaverMap)
}