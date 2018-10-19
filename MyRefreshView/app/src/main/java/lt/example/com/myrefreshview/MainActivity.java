package lt.example.com.myrefreshview;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements MyRefreshViewListView.RefreshListerner {
    private MyRefreshViewListView refresh_view;
    private ListView listView;
    @SuppressLint("HandlerLeak")
    private MyHandler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        refresh_view = findViewById(R.id.refresh_view);
        listView = findViewById(R.id.listView);
        List<Map<String,String>> list = new ArrayList<>();
        for (int i=0;i<60;i++){
            Map<String,String> map= new HashMap<>();
            map.put("text","数据"+i);
            list.add(map);
        }
        SimpleAdapter adapter = new SimpleAdapter(this,list,android.R.layout.test_list_item,new String[]{"text"},new int[]{android.R.id.text1});
        listView.setAdapter(adapter);
        refresh_view.setRefreshListerner(this);
        handler = new MyHandler();
    }

    public void onClick(View v){
        if(v.getId() == R.id.openSencond){
            startActivity(new Intent(this,SencondActivity.class));
        }
        handler.removeCallbacksAndMessages(null);
        refresh_view.onFinishRereshOrLoad();
    }

    @Override
    public void onRefresh() {
        handler.sendEmptyMessageDelayed(REFREHING,5000);
    }

    @Override
    public void onLoad() {
        handler.sendEmptyMessageDelayed(LOADING,5000);
    }

    @Override
    public void onCancel() {
        handler.sendEmptyMessage(CANCEL);
    }
    private final int REFREHING = 0;
    private final int LOADING = 1;
    private final int CANCEL = 2;
    @SuppressLint("HandlerLeak")
    private class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case REFREHING:
                    Toast.makeText(MainActivity.this,"刷新结束",Toast.LENGTH_SHORT).show();
                    refresh_view.onFinishRereshOrLoad();
                    break;
                case LOADING:
                    Toast.makeText(MainActivity.this,"加载结束",Toast.LENGTH_SHORT).show();
                    refresh_view.onFinishRereshOrLoad();
                    break;
                case CANCEL:
                    handler.removeCallbacksAndMessages(null);
                    Toast.makeText(MainActivity.this,"取消刷新",Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
}
