package com.example.nttdocomo.open_iarea;

import android.location.Location;

/**
 * LocationクラスにOpen iAreaのリザルトコードとエラーメッセージを追加したクラス.
 */

public class OpeniAreaLocation extends Location{
    /**XMLファイル解析後、リザルトコードを格納する*/
    private int resultcode;
    /**errorの場合、エラーメッセージを格納する*/
    private String errorMessage;

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
    /**resultcodeの取得*/
    public int get_resultcode(){
        return resultcode;
    }
    /**エラーメッセージの取得*/
    public String get_errorMessage(){
        return errorMessage;
    }
}
