package kr.ac.duksung.parkingapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {
    ActivityResultLauncher<Intent> launcher;
    private long time;
    EditText loginId, loginPw;
    private String id, pw;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loginId = (EditText) findViewById(R.id.IdText);
        loginPw = (EditText) findViewById(R.id.PasswordText);
        Button loginButton = (Button) findViewById(R.id.loginButton);
        Log.d("IP", getString(R.string.ip));

        Log.d("TEST", "시작");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getString(R.string.ip))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        crud_RetrofitAPI lresult = retrofit.create(crud_RetrofitAPI.class);

        HashMap<String, Object> lparam = new HashMap<String, Object>();

        Log.d("POST", "ongoing");

        View.OnKeyListener keyListener = new View.OnKeyListener() {
            String id = loginId.getText().toString();
            String pw = loginPw.getText().toString();
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(pw))
                    loginButton.setEnabled(true);
                else
                    loginButton.setEnabled(false);
                return false;
            }
        };
        loginId.setOnKeyListener(keyListener);
        loginPw.setOnKeyListener(keyListener);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                id=loginId.getText().toString();
                pw=loginPw.getText().toString();
                lparam.put("userid",id);
                lparam.put("password",pw);
                lresult.postloginData(lparam).enqueue(new Callback<crud_LoginResult>() {
                    @Override
                    public void onResponse(Call<crud_LoginResult> call, Response<crud_LoginResult> response) {
                        if(response.isSuccessful()){
                            crud_LoginResult data = response.body();
                            Log.d("POST: ", "로그인 되었습니다!");
                            moveHome(1);
                        }
                    }

                    @Override
                    public void onFailure(Call<crud_LoginResult> call, Throwable t) {
                        Log.d("POST: ", "Failed!!!!");
                        t.printStackTrace();
                        Toast.makeText(getApplicationContext(),"아이디와 비밀번호를 확인해주세요.",Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });

    }

    private void moveHome(int sec) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                Toast.makeText(getApplicationContext(), id+" 님 환영합니다!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }, 1000 * sec); //sec 초만큼 딜레이를 준 후 시작한다는 뜻
    }

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - time >= 2000) {
            time = System.currentTimeMillis();
            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            builder.setTitle("종료");
            builder.setMessage("시스템을 종료하시겠습니까?");
            builder.setPositiveButton("네", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try {
                        // finish 후 다른 Activity 뜨지 않도록 함
                        moveTaskToBack(true);
                        // 현재 액티비티 종료
                        finish();
                        // 모든 루트 액티비티 종료
                        finishAffinity();
                        // 인텐트 애니 종료
                        overridePendingTransition(0, 0);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }


                }
            });
            builder.setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    return;
                }
            });
            builder.show();
        }
    }

}