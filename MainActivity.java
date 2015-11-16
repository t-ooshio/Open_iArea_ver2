/**
 * Open iArea測位を行うアプリ
 * 機能
 * 繰り返し測位（回数指定なし、測位停止ボタンを押すまで続ける）
 * 測位間隔設定（測位停止から次の測位開始までの時間）
 * ログ出力（時間,緯度,経度）
 */
package com.example.nttdocomo.open_iarea;

import android.app.Activity;
import android.content.Intent;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * Open iArea測位を行うクラス
 * 測位の実行・ログへの書き込みを行う
 */
public class MainActivity extends Activity{

    /**基地局測位が許可されていない場合のエラーコード*/
    private final int ERROR_NOT_PERMIT_iArea = 4002;

    /**ファイル名用日付フォーマット;*/
    private SimpleDateFormat FILENAME_SDF = new SimpleDateFormat("yyyyMMdd-kkmmssSSS");
    //測位中フラグ TRUE:測位中 FALSE:停止中 (今回は未使用、測位中は設定変更できないなどに使いたい)
    private Boolean isPositioning= TRUE;
    //測位繰り返しフラグ(TRUE:繰り返し続行 FALSE:停止)
    private Boolean isLoop=TRUE;
    //最初の測位かのフラグ(TRUE:最初の測位 FALSE:初回以外)
    private Boolean isFirst;

    //logファイル用
    private OpeniAreaLocation result_location = new OpeniAreaLocation("GPS");

    private MyLog myLog = new MyLog();
    //log出力時間用
    private Jtime jtime = new Jtime();
    //log出力文字列
    private String log_str;

    //log用TAG
    private String myTAG="SioApp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * :UIパーツ
         */
        //測位開始ボタン
        final Button button_start = (Button)findViewById(R.id.btn_start);
        button_start.setEnabled(true);
        //測位停止ボタン
        final Button button_stop = (Button)findViewById(R.id.btn_stop);
        button_stop.setEnabled(false);

        //測位状態表示テキスト
        final TextView tv_state = (TextView)findViewById(R.id.tv_state);

        //緯度・経度・測位時間表示
        final TextView tv_result_lat = (TextView)findViewById(R.id.txt_result_lat);
        final TextView tv_result_lng = (TextView)findViewById(R.id.txt_result_lng);
        final TextView tv_result_time = (TextView)findViewById(R.id.tv_result_time);

        //測位間隔エディット用
        final EditText editTextInterval = (EditText)findViewById(R.id.editText_interval);

        /**
         * 測位用
         */
        final OpeniAreaHttpConnect mOpeniAreaHttpConnect = new OpeniAreaHttpConnect(this);
        final OpeniAreaLocation[] location = new OpeniAreaLocation[1];
        final CellId cellId = new CellId();

        /*
        handler
        threadから応答が来たら結果を表示する
        */
        final Handler handler = new Handler() {
            public void handleMessage(Message msg) {
                //結果の画面出力

                location[0] = (OpeniAreaLocation) msg.obj;
                //OpeniAreaが許可されていない場合、許可するためのページに誘導
                if (location[0].get_resultcode() == ERROR_NOT_PERMIT_iArea) {
                    Uri uri = Uri.parse(location[0].get_errorMessage());
                    Intent i = new Intent(Intent.ACTION_VIEW, uri);
                    show_Toast("ログイン後、基地局測位を許可してください");
                    startActivity(i);
                    button_start.setEnabled(true);
                    button_stop.setEnabled(false);

                    isPositioning=FALSE;
                    tv_state.setText(R.string.txt_suspension);
                    isLoop=FALSE;

                } else {
                    tv_result_lat.setText(String.valueOf(location[0].getLatitude()));
                    tv_result_lng.setText(String.valueOf(location[0].getLongitude()));
                    tv_result_time.setText(jtime.getjTime((location[0].getTime())));
                }

                //ログ出力
                log_str = String.valueOf(String.valueOf(jtime.getjTime(location[0].getTime())) + ","
                        + location[0].getLatitude()) + ","
                        + String.valueOf(location[0].getLongitude()
                +"," + location[0].get_log_cellId());
                myLog.addLog(log_str);

            }
        };

        /*
        スレッドの作成
        http通信は別スレッドで行う必要があるため作成
         */
        button_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button_start.setEnabled(false);
                button_stop.setEnabled(true);

                myLog.initLog("time,latitude,longitude,CellID,PhysicalCellID");
                isLoop = TRUE;
                isPositioning = TRUE;
                isFirst = TRUE;
                tv_state.setText(R.string.txt_Positioning);
                show_Toast("測位開始");
                //測位処理開始
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isLoop) {
                            if (!isFirst) {
                                try {
                                    Thread.sleep(Integer.parseInt(String.valueOf(editTextInterval.getText())) * 1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            //OpeniArea測位開始
                            mOpeniAreaHttpConnect.connection();

                            //CellID、物理CellIDの取得
                            TelephonyManager telephonyManager;
                            telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);

                            result_location = (OpeniAreaLocation) mOpeniAreaHttpConnect.getLocation();
                            result_location.set_log_cellId(cellId.getAllCellId(telephonyManager));
                            Message msg = new Message();
                            msg.what = 99;
                            //msg.obj = mOpeniAreaHttpConnect.getLocation();
                            msg.obj = result_location;
                            handler.sendMessage(msg);
                            isFirst = FALSE;
                        }
                    }
                }).start();
            }
        });

        button_stop.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                button_start.setEnabled(true);
                button_stop.setEnabled(false);

                isPositioning=FALSE;
                tv_state.setText(R.string.txt_suspension);
                show_Toast("測位終了");
                isLoop=FALSE;
            }
        });
    }

    /**
    Toastの表示
     */
    private void show_Toast(String str){
        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}