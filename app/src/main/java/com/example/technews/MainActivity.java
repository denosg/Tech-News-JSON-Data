package com.example.technews;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();
    static ArrayList<String> links = new ArrayList<>();

    ArrayAdapter arrayAdapter;
    public SQLiteDatabase articlesDB;

    //Asynchronous (runs in background)
    public class DownloadTask extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try{

                url = new URL(urls[0]);

                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = urlConnection.getInputStream();

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

                int data = inputStreamReader.read();

                while(data != -1){
                    char current = (char) data;
                    result += current;
                    data = inputStreamReader.read();
                }

                //Saves all the numbers provided in a JSONArray
                JSONArray jsonArray = new JSONArray(result);

                int maxNumberItems = 10;

                if (jsonArray.length() < 10){
                    maxNumberItems = jsonArray.length();
                }

                //Deletes the database articles before adding the new ones to save on memory
                articlesDB.execSQL("DELETE FROM articles");

                for (int i = 0; i < maxNumberItems; i++){
                    String articleId = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+articleId+".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();

                    String articleInfo = "";
                    String articleTitle;
                    String articleUrl;

                    inputStream = urlConnection.getInputStream();

                    inputStreamReader = new InputStreamReader(inputStream);

                    data = inputStreamReader.read();

                    while(data != -1){
                        char current = (char) data;
                        articleInfo += current;
                        data = inputStreamReader.read();
                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        articleTitle = jsonObject.getString("title");
                        articleUrl = jsonObject.getString("url");

                        Log.i("articleTitle ", articleTitle);
                        Log.i("articleUrl ", articleUrl);

                        String sql = "INSERT INTO articles (articleId, title, link) VALUES (?, ?, ?)";
                        SQLiteStatement statement = articlesDB.compileStatement(sql);
                        statement.bindString(1, articleId);
                        statement.bindString(2, articleTitle);
                        statement.bindString(3, articleUrl);

                        statement.execute();
                    }
                }

                Log.i("URL content",result);

            }catch (Exception e){
                e.printStackTrace();
            }

            return result;
        }

        /*@Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }*/
    }

    public void updateListView(){
        Cursor c = articlesDB.rawQuery("SELECT * FROM articles",null);

        int linkIndex = c.getColumnIndex("link");
        int titleIndex = c.getColumnIndex("title");

        if (c.moveToFirst()) {
            //Deletes the previous titles from the array
            titles.clear();
            //Deletes the previous links from the array
            links.clear();

            do {

                titles.add(c.getString(titleIndex));
                links.add(c.getString(linkIndex));

            }while (c.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        articlesDB = this.openOrCreateDatabase("articles", MODE_PRIVATE,null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, link VARCHAR)");
        Log.i("dataBaseIsCreated: ", "true");

        ListView listView = findViewById(R.id.listView);

        DownloadTask downloadTask = new DownloadTask();

        try {

            downloadTask.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

        }catch (Exception e){
            e.printStackTrace();
        }

        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,titles);

        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), ArticleActivity.class);

                intent.putExtra("articleIdFromList",i);

                startActivity(intent);
            }
        });
        updateListView();
    }
}