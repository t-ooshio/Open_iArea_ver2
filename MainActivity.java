/**
 * Open iAreaを行うアプリ
 */
package com.example.nttdocomo.open_iarea;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * Open iArea測位を行うクラス
 * 測位の実行・ログへの書き込みを行う
 */
public class MainActivity extends Activity{

    /**基地局測位が許可されていない場合のエラーコード*/
    private final int ERROR_NOT_PERMIT_iArea = 4002;
    /**log出力用 File*/
    private File newfile;
    /**log出力用 File書き込み*/
    private FileWriter writer;
    /**ファイル名用日付フォーマット;*/
    private SimpleDateFormat FILENAME_SDF = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ssm-SSS");
    /**timer用*/
    private Timer timer;
    //繰り返し間隔(テキストボックスから秒で受け取り、1000倍してミリ病にして使う)
    private int repeat_interval;
    //測位中フラグ TRUE:測位中 FALSE:停止中 (今回は未使用、測位中は設定変更できないなどに使いたい)
    private Boolean isPositioning= TRUE;
    //測位繰り返しフラグ(TRUE:繰り返し続行 FALSE:停止)
    private Boolean isLoop=TRUE;

    //logファイル用
    private FileOutputStream fos;
    private OutputStreamWriter osw;
    private BufferedWriter bw;

    private String file_tmp;
    private String LOGDIR;
    private String PkgName;

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
        //測位停止ボタン
        final Button button_stop = (Button)findViewById(R.id.btn_stop);

        //測位状態表示テキスト
        final TextView tv_state = (TextView)findViewById(R.id.tv_state);

        //緯度・経度・測位時間表示
        final TextView tv_result_lat = (TextView)findViewById(R.id.txt_result_lat);
        final TextView tv_result_lng = (TextView)findViewById(R.id.txt_result_lng);
        final TextView tv_result_time = (TextView)findViewById(R.id.tv_result_time);

        //測位間隔エディット用
        final EditText editTextInterval = (EditText)findViewById(R.id.editText_interval);

        /**
         * 制御用変数
         */
        //測位間隔
        //未使用
        final int interval;

        /**
         * 測位用
         */
        final OpeniAreaHttpConnect mOpeniAreaHttpConnect = new OpeniAreaHttpConnect(this);
        final OpeniAreaLocation[] location = new OpeniAreaLocation[1];

        /*log出力用初期処理*/
        file_tmp = FILENAME_SDF.format(new Date());
        PkgName = this.getPackageName();
        LOGDIR = Environment.getExternalStorageDirectory().getPath();
        newfile = new File("/sdcard/"+PkgName + "/" + file_tmp + ".txt");
        newfile.getParentFile().mkdir();
        try{
            //ログ出力
            fos = new FileOutputStream(newfile, true);
            osw = new OutputStreamWriter(fos, "Shift-JIS");
            bw = new BufferedWriter(osw);

            bw.write("測位日時,緯度,経度\n");
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        /*
        handler
        threadから応答が来たら結果を表示する
        */
        final Handler handler = new Handler(){
            public void handleMessage(Message msg){
                //結果の画面出力

                location[0] = (OpeniAreaLocation)msg.obj;
                //OpeniAreaが許可されていない場合、許可するためのページに誘導
                if(location[0].get_resultcode() == ERROR_NOT_PERMIT_iArea){
                    Uri uri = Uri.parse(location[0].get_errorMessage());
                    Intent i = new Intent(Intent.ACTION_VIEW,uri);
                    show_Toast("ログイン後、基地局測位を許可してください");
                    startActivity(i);
                }else {
                    tv_result_lat.setText(String.valueOf(location[0].getLatitude()));
                    tv_result_lng.setText(String.valueOf(location[0].getLongitude()));
                    tv_result_time.setText(String.valueOf(getjTime(location[0].getTime())));
                }
                try{
                    //ログ出力
                    fos = new FileOutputStream(newfile, true);
                    osw = new OutputStreamWriter(fos, "Shift-JIS");
                    bw = new BufferedWriter(osw);

                    bw.write(String.valueOf(String.valueOf(getjTime(location[0].getTime())) + ","
                            + location[0].getLatitude()) + ","
                            + String.valueOf(location[0].getLongitude() + "\n"));
                    bw.flush();
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }            }
        };

        /*
        スレッドの作成
        http通信は別スレッドで行う必要があるため作成
         */
        button_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoop=TRUE;
                isPositioning=TRUE;
                tv_state.setText(R.string.txt_Positioning);
                show_Toast("測位開始");
                //測位処理開始
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(isLoop) {
                            mOpeniAreaHttpConnect.connection();
                            Message msg = new Message();
                            msg.what = 99;
                            msg.obj = mOpeniAreaHttpConnect.getLocation();
                            handler.sendMessage(msg);
                            try {
                                //ミリ秒→秒に変換
                                Thread.sleep(Integer.parseInt(String.valueOf(editTextInterval.getText()))*1000);
                                Log.d(myTAG,"待機中");
                            }catch(InterruptedException e){
                                Log.d(myTAG,"Sleep Error");
                            }
                        }
                    }
                }).start();
            }
        });
        button_stop.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                isPositioning=FALSE;
                tv_state.setText(R.string.txt_suspension);
                show_Toast("測位終了");
                isLoop=FALSE;
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    /**
    時間を取得して日本時間に変換する
     */
    private String getjTime(long t){
        String jtime;
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd kk:mm:ss");
        TimeZone timezone = TimeZone.getTimeZone("Asia/Tokyo");
        Locale locale = Locale.JAPAN;
        Calendar calendar = Calendar.getInstance(timezone, locale);
        //■↓これがないとNG
        calendar.setTimeZone(timezone);
        format.setTimeZone(timezone);
        jtime = format.format(t);
        return jtime;
    }

    /**
    Toastの表示
     */
    private void show_Toast(String str){
        Toast.makeText(this,str,Toast.LENGTH_LONG).show();
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