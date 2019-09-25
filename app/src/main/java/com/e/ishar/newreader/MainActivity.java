package com.e.ishar.newreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    static ArrayList<String> titles = new ArrayList<>();
    static ArrayList<String> cURL = new ArrayList<>();
    ArrayList<String> jsonArr = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    ListView newsList;
    SQLiteDatabase myDatabase;
    public class DownloadJSONdata extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... strings) {
            int len = strings.length;
            String result ="";
            URL url;
            HttpURLConnection urlConnection = null;
            try {
                for(int i=0;i<20;i++) {
                    result ="";
                    url = new URL(strings[i]);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    InputStream in = urlConnection.getInputStream();
                    InputStreamReader reader = new InputStreamReader(in);
                    int data = reader.read();
                    char current;
                    while (data != -1) {
                        current = (char) data;
                        result += current;
                        data = reader.read();
                    }
                    //Log.i("Reslut",result);
                    jsonArr.add(result);
                }
                return result;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            /*
            try {

                JSONObject jsonObject = new JSONObject(s);
                String title = jsonObject.getString("title");
                String url = jsonObject.getString("url");
                Log.i(title,url);
            } catch (JSONException e) {
                e.printStackTrace();
            }*/

            for(int i=0;i<jsonArr.size();i++){
                String title ="";
                String url="";
                try {
                    JSONObject jsonObject = new JSONObject(jsonArr.get(i));
                    title = jsonObject.getString("title");
                    url=jsonObject.getString("url");;
                    //Log.i("JSON",jsonObject.getString("url"));
                } catch (JSONException e) {
                    //Log.i("Error occured",e.getMessage());

                    e.printStackTrace();
                }
                titles.add(title);
                cURL.add(url);

                try {
                    String query = "INSERT INTO newsinfo (dtitle,durl) VALUES (" + "'" + title + "'" + "," + "'" + url + "'" + ")";
                    myDatabase.execSQL(query);
                    //Log.i("Query", query);
                    /*
                    String query = "INSERT INTO newsinfo (dtitle,durl) VALUES (?,?)"
                    SQLiteStatement statement = myDatabase.compileStatement(sql);
                    statement.bindString(1,title);
                    statement.bindString(2,url);
                    statement.execute();
                     */
                }
                catch (Exception e){
                    Log.i("Error",e.getMessage());
                }
            }
            arrayAdapter = new ArrayAdapter(getApplicationContext(),android.R.layout.simple_list_item_1,titles);
            newsList.setAdapter(arrayAdapter);
        }
    }
    public class DownloadContent extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... strings) {
            String result ="";
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(strings[0]);
                urlConnection =(HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();
                char current ;
                while(data != -1){
                    current=(char)data;
                    if((current != '[') &&(current != ']')) {
                        result += current;
                    }
                    data = reader.read();
                }
                result = result.trim();
                //Result string is actually a JSON array, so can directly convert into a JSON array and
                // loop through to get individual items

                return result;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        newsList = (ListView)findViewById(R.id.newsList);
        newsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(getApplicationContext(),webViewActivity.class);
                    intent.putExtra("position",position);
                    startActivity(intent);
            }
        });
        try {
            myDatabase = this.openOrCreateDatabase("News", MODE_PRIVATE, null);
            myDatabase.execSQL("CREATE TABLE IF NOT EXISTS newsinfo (dtitle VARCHAR,durl VARCHAR)");
            //Add a primary key Id which will be auto-increment
            //myDatabase.execSQL("DELETE FROM newsinfo");
            Cursor countCursor = myDatabase.rawQuery("SELECT count(*) FROM newsinfo",null);
            countCursor.moveToFirst();
            int count = countCursor.getInt(0);
            if (count > 0) {
                Cursor c = myDatabase.rawQuery("SELECT * FROM newsinfo", null);
                int titleIndex = c.getColumnIndex("dtitle");
                int urlIndex = c.getColumnIndex("durl");
                c.moveToFirst();
                Log.i("Info1","Inhere");
                int rowCount = 0;
                while (rowCount < 20) {
                    titles.add(c.getString(titleIndex));
                    cURL.add(c.getString(urlIndex));
                    Log.i("Info2",c.getString(titleIndex) + c.getString(urlIndex));
                    Log.i("Val","Hello");
                    c.moveToNext();
                    rowCount += 1;
                }
                arrayAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, titles);
                newsList.setAdapter(arrayAdapter);
                Log.i("Size 1" ,Integer.toString(titles.size()));
                Log.i("Size 2" ,Integer.toString(cURL.size()));
            } else {
                DownloadContent task = new DownloadContent();
                DownloadJSONdata jtask = new DownloadJSONdata();
                String result = null;
                try {
                    result = task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty").get();
                    String strArr[] = result.split(",");
                    String urlArr[] = new String[strArr.length];
                    int i = 0;
                    for (String a : strArr) {
                        //for each value generate the url and download JSON content
                        String halfUrl = "https://hacker-news.firebaseio.com/v0/item/";
                        String ohalf = ".json?print=pretty";
                        String fullUrl = halfUrl + a.trim() + ohalf;
                        urlArr[i] = fullUrl;
                        i++;
                        //Log.i("URL",fullUrl);
                        //jtask.execute(fullUrl);
                    }
                    jtask.execute(urlArr);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e){
            Log.i("Error",e.getMessage());
        }
    }

}
