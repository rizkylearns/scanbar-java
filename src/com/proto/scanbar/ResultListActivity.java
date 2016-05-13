package com.proto.scanbar;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import com.proto.scanbar.utils.HistoryStorage;

/**
 * Created by rizky on 2/03/2015.
 */
public class ResultListActivity extends ListActivity {


    @Override
    public void onCreate(Bundle result) {
        super.onCreate(result);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreHistory(null);
    }

    private void restoreHistory(Bundle savedInstance) {
        HistoryStorage storage = new HistoryStorage();


        Bundle extras = savedInstance;
        if(extras == null) {
            extras = getIntent().getExtras();
        }

        if(extras != null) {
            storage.addScanItem(this, extras.getString("message"));
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, storage.getScanHistory(this));
        setListAdapter(adapter);
        storage.addStorageChangedListener(new HistoryStorage.StorageChangedListener() {
            @Override
            public void onStorageChanged(HistoryStorage storage) {
                adapter.notifyDataSetChanged();
            }
        });
    }
}
