package edu.harvard.cs50.pokedex;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

public class PokemonActivity extends AppCompatActivity {
    private TextView nameTextView;
    private TextView numberTextView;
    private TextView type1TextView;
    private TextView type2TextView;
    private Button catchButton;
    private ImageView pokemonImageView;
    private TextView descriptionTextView;

    private boolean isCaught;
    private String pokemonName;

    private String url;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokemon);

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        pokemonName = getIntent().getStringExtra("pokemonName");
        url = getIntent().getStringExtra("url");
        nameTextView = findViewById(R.id.pokemon_name);
        numberTextView = findViewById(R.id.pokemon_number);
        type1TextView = findViewById(R.id.pokemon_type1);
        type2TextView = findViewById(R.id.pokemon_type2);
        catchButton = findViewById(R.id.pokemon_catch);
        pokemonImageView = findViewById(R.id.pokemon_imageView);
        descriptionTextView = findViewById(R.id.pokemon_description);

        load();

        SharedPreferences sharedPreferences = getSharedPreferences("edu.harvard.cs50.pokedex.myPref", Context.MODE_PRIVATE);
        isCaught = sharedPreferences.getBoolean(pokemonName, false);

        if (isCaught) {
            catchButton.setText("Release");
        }
    }

    public void load() {
        type1TextView.setText("");
        type2TextView.setText("");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    nameTextView.setText(response.getString("name"));
                    numberTextView.setText(String.format("#%03d", response.getInt("id")));

                    JSONArray typeEntries = response.getJSONArray("types");
                    for (int i = 0; i < typeEntries.length(); i++) {
                        JSONObject typeEntry = typeEntries.getJSONObject(i);
                        int slot = typeEntry.getInt("slot");
                        String type = typeEntry.getJSONObject("type").getString("name");

                        if (slot == 1) {
                            type1TextView.setText(type);
                        } else if (slot == 2) {
                            type2TextView.setText(type);
                        }
                    }

                    // pokemon image url
                    String front_default = response.getJSONObject("sprites").getString("front_default");
                    new DownloadSpriteTask().execute(front_default);

                    // separate API call to retrieve the description of the selected Pokemon
                    String urlDescription = "https://pokeapi.co/api/v2/pokemon-species/" + response.getInt("id");
                    JsonObjectRequest request1 = new JsonObjectRequest(Request.Method.GET, urlDescription, null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONArray flavorTextEntries = response.getJSONArray("flavor_text_entries");
                                for (int i = 0; i < flavorTextEntries.length(); i++) {
                                    JSONObject flavorTextEntry = flavorTextEntries.getJSONObject(i);
                                    String language = flavorTextEntry.getJSONObject("language").getString("name");

                                    if (language.equals("en")) {
                                        String text = flavorTextEntry.getString("flavor_text");
                                        descriptionTextView.setText(text);
                                    }
                                }
                            } catch (JSONException e) {
                                Log.e("cs50", "Pokemon json error", e);
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    });

                    requestQueue.add(request1);

                } catch (JSONException e) {
                    Log.e("cs50", "Pokemon json error", e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("cs50", "Pokemon details error", error);
            }
        });

        requestQueue.add(request);
    }

    public void toggleCatch(View view) {

        if (isCaught) {
            catchButton.setText("Catch");
            isCaught = false;
        } else {
            catchButton.setText("Release");
            isCaught = true;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("edu.harvard.cs50.pokedex.myPref", Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(pokemonName, isCaught).apply();
    }

    private class DownloadSpriteTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                URL url = new URL(strings[0]);
                return BitmapFactory.decodeStream(url.openStream());
            } catch (IOException e) {
                Log.e("cs50", "Download sprite error", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            // load the bitmap into the ImageView!
            pokemonImageView.setImageBitmap(bitmap);
        }
    }
}
