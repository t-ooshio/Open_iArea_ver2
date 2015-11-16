package com.example.nttdocomo.open_iarea;

import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by NTT docomo on 2015/11/16.
 */
public class CellId {
    private TelephonyManager telephonyManager;
    private List<CellInfo> cellinfolist;
    private CellInfoLte cellinfolte;

    //cellIDのリスト
    private Set<Integer> set_cellid = new HashSet<>();
    private Iterator<Integer> it_cellid;
    //物理セルIDのリスト
    private Set<Integer> set_p_cellid = new HashSet<>();
    private Iterator<Integer> it_p_cellid;

    //セルIDタグ
    private String tag_cellid = "CellID:";
    //物理セルIDタグ
    private String tag_p_cellid = "pCellID:";

    //区切り文字
    final private String delimiter = ",";
    /**
     * CellIDを取得し、CellIDと物理CellIDのリストにそれぞれ保存する
     * 例：cellid0000000
     * @param telephonyManager 例：telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE)
     */
    public void scanCellId(TelephonyManager telephonyManager){

        cellinfolist = telephonyManager.getAllCellInfo();
        for(CellInfo cellinfo : cellinfolist){
            if(cellinfo instanceof CellInfoLte){
                cellinfolte = (CellInfoLte)cellinfo;
                CellIdentityLte cellIdentifyLte = cellinfolte.getCellIdentity();
                //cellidの取得
                set_cellid.add(cellIdentifyLte.getCi());
                //物理cellidの取得
                set_p_cellid.add(cellIdentifyLte.getPci());
            }
        }
        //重複削除
        it_cellid = set_cellid.iterator();
        it_p_cellid = set_p_cellid.iterator();
    }

    /**
     *
     * @param telephonyManager telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE)
     * @return csv形式でcellID、物理セルIDのリストを返す。例:CellID:00000,CellID:11111,pCellID:222
     *
     */
    public String getAllCellId(TelephonyManager telephonyManager){
        set_cellid.clear();
        set_p_cellid.clear();

        String cellId="";
        scanCellId(telephonyManager);

        //cellIDが空以外の場合、戻り値に追加
        while(it_cellid.hasNext()) {
            cellId = cellId + tag_cellid + String.valueOf(it_cellid.next() + delimiter);
        }

        //p_cellIDが空以外の場合、戻り値に追加
        while(it_p_cellid.hasNext()) {
            cellId = cellId + tag_p_cellid + String.valueOf(it_p_cellid.next() + delimiter);
        }

        //最後の区切り文字を削除
        cellId = cellId.substring(0,cellId.length() - 1);
        return cellId;
    }
}