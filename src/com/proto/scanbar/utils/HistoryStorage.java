package com.proto.scanbar.utils;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;


import java.util.*;

/**
 * Created by rizky on 2/03/2015.
 */
public final class HistoryStorage {

    public interface StorageChangedListener {
        void onStorageChanged(HistoryStorage storage);
    }

    final static String PREF_NAME = "HISTORY";
    final static String PREF_KEY = "SCAN_LIST";

    final List<StorageChangedListener> storageChangedListeners;

    public HistoryStorage() {
        storageChangedListeners = new ArrayList<StorageChangedListener>(1);
    }

    public void addStorageChangedListener(StorageChangedListener listener) {
        storageChangedListeners.add(listener);
    }

    private void notifyStorageChanged() {
        for(StorageChangedListener listener:storageChangedListeners) {
            listener.onStorageChanged(this);
        }
    }

    public void saveScanHistory(Context context, List<String> scanHistory) {
        SharedPreferences settings;
        SharedPreferences.Editor editor;

        settings = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = settings.edit();

        ListIterator<String> historyIterator = scanHistory.listIterator(scanHistory.size());
        while(scanHistory.size() > 10 && historyIterator.hasPrevious()) {
            historyIterator.previous();
            historyIterator.remove();
        }

        JSONArray items = new JSONArray(scanHistory);
        editor.putString(PREF_KEY, items.toString());

        editor.commit();
        notifyStorageChanged();
    }

    public void addScanItem(Context context, String scanItem) {
        List<String> scanHistory = getScanHistory(context);

        scanHistory.add(0,scanItem);
        saveScanHistory(context, scanHistory);
        notifyStorageChanged();
    }

    public void removeScanItem(Context context, String scanItem) {
        List<String> scanHistory = getScanHistory(context);

        scanHistory.remove(scanItem);
        saveScanHistory(context,scanHistory);
        notifyStorageChanged();
    }

    public List<String> getScanHistory(Context context) {
        SharedPreferences settings;
        List<String> historyItems = new ArrayList<String>();

        settings = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if(settings.contains(PREF_KEY)) {
            try {
                JSONArray jsonScanHistory = new JSONArray(settings.getString(PREF_KEY,null));
                for(int index = 0; index < jsonScanHistory.length(); index++) {
                    historyItems.add(jsonScanHistory.getString(index));
                }
            } catch (JSONException exc) { return new ArrayList<String>(0);}
        }

        return historyItems;
    }
}
