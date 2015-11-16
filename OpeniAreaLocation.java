package com.example.nttdocomo.open_iarea;

import android.location.Location;

/**
 * LocationクラスにOpen iAreaのリザルトコードとエラーメッセージを追加したクラス.
 * 2015/11/16 string log_cellIDを追加:セルIDと物理セルIDをログに出力するための変数
 */

public class OpeniAreaLocation extends Location{
    /**XMLファイル解析後、リザルトコードを格納する*/
    private int resultcode;
    /**errorの場合、エラーメッセージを格納する*/
    private String errorMessage;
    //セルIDと物理セルIDのログ出力用
    private String log_cellId;

    /**providerのセット*/
    public OpeniAreaLocation(String provider) {
        super(provider);
    }



    /**resultcodeのセット*/
    public void set_resultcode(int resultcode){
        this.resultcode = resultcode;
    }
    /**エラーメッセージのセット*/
    public void set_errorMessage(String message){
        this.errorMessage = message;
    }
    /**セルID・物理セルIDのログセット*/
    public void set_log_cellId(String log_cellId){
        this.log_cellId = log_cellId;
    }
    /**resultcodeの取得*/
    public int get_resultcode(){
        return resultcode;
    }
    /**エラーメッセージの取得*/
    public String get_errorMessage(){
        return errorMessage;
    }
    /**セルID・物理セルIDのログセット*/
    public String get_log_cellId(){
        return log_cellId;
    }

}
