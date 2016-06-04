package com.carterswebs.stormy;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    private CurrentWeather mCurrentWeather;

    @BindView(R.id.timeLabel) TextView mTimeLabel;
    @BindView(R.id.temperatureLabel) TextView mTemperatureLabel;
    @BindView(R.id.humidityValue) TextView mHumidityValue;
    @BindView(R.id.precipValue) TextView mPrecipValue;
    @BindView(R.id.summaryLabel) TextView mSummaryLabel;
    @BindView(R.id.iconImageView) ImageView mIconImageView;
    @BindView(R.id.refreshImageView) ImageView mRefreshImageView;
    @BindView(R.id.updateInfoProgressBar) ProgressBar mUpdateInfoProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mUpdateInfoProgressBar.setVisibility(View.INVISIBLE);

        final double latitude = 37.8267;
        final double longitude = -122.423;
        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getForecast(latitude, longitude);
            }
        });

        getForecast(latitude, longitude);

        Log.d(TAG, "Main UI Code is running!");
    }

    private void getForecast(double latitude, double longitude) {
        String apiKey = "083e9bcd955dd3f8767357f8f45b0d04";
        String forecastUrl = "https://api.forecast.io/forecast/" + apiKey + "/"
                + latitude + "," + longitude;
        if (isNetworkAvailable()) {
            toggleRefresh();

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(forecastUrl)
                    .build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    alertUserAboutError();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    try {
                        String jsonData = response.body().string();
                        Log.v(TAG,jsonData);
                        if (response.isSuccessful()) {
                            mCurrentWeather = getCurrentDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });
                        } else {
                            alertUserAboutError();
                        }
                    }
                    catch (IOException e) {
                        Log.e(TAG, "IO Exception caught: ", e);
                    }
                    catch (JSONException e) {
                        Log.e(TAG, "JSON Exception caught: ", e);
                    }

                }
            });
        }
        else {
            Toast.makeText(this, R.string.network_unavailable_message, Toast.LENGTH_LONG).show();
        }
    }

    private void toggleRefresh() {
        switch (mUpdateInfoProgressBar.getVisibility()) {
            case View.INVISIBLE:
                mUpdateInfoProgressBar.setVisibility(View.VISIBLE);
                mRefreshImageView.setVisibility(View.INVISIBLE);
                break;
            case View.VISIBLE:
                mUpdateInfoProgressBar.setVisibility(View.INVISIBLE);
                mRefreshImageView.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void updateDisplay() {

        // Temperature
        int currentTemperature = mCurrentWeather.getTemperature();
        String temperatureFormat = getResources().getString(R.string.integerOutput);
        String temperatureLabel = String.format(temperatureFormat,currentTemperature);
        mTemperatureLabel.setText(temperatureLabel);

        // Time
        String currentTime = mCurrentWeather.getFormattedTime();
        String timeFormat = getString(R.string.timeString);
        String timeLabel = String.format(timeFormat,currentTime);
        mTimeLabel.setText(timeLabel);

        // Humidity
        String humidityFormat = getString(R.string.percentageOutput);
        int currentHumidity = mCurrentWeather.getHumidity();
        String humidityLabel = String.format(humidityFormat,currentHumidity);
        mHumidityValue.setText(humidityLabel);

        //Precipitation Chance
        String precipitationFormat = getString(R.string.percentageOutput);
        int currentPrecipitationChance = mCurrentWeather.getPrecipitationChance();
        String precipitationLabel = String.format(precipitationFormat, currentPrecipitationChance);
        mPrecipValue.setText(precipitationLabel);

        // Summary Text
        mSummaryLabel.setText(mCurrentWeather.getSummary());

        // Icon Update
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), mCurrentWeather.getIconId(), null);
        mIconImageView.setImageDrawable(drawable);
    }

    private CurrentWeather getCurrentDetails(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        Log.i(TAG, "From JSON: " + timezone);

        JSONObject currently = forecast.getJSONObject("currently");
        CurrentWeather currentWeather = new CurrentWeather();

        currentWeather.setHumidity(currently.getDouble("humidity"));
        currentWeather.setTime(currently.getLong("time"));
        currentWeather.setIcon(currently.getString("icon"));
        currentWeather.setPrecipitationChance(currently.getDouble("precipProbability"));
        currentWeather.setSummary(currently.getString("summary"));
        currentWeather.setTemperature(currently.getDouble("temperature"));
        currentWeather.setTimeZone(timezone);

        Log.d(TAG, currentWeather.getFormattedTime());

        return currentWeather;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;

        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }

        return isAvailable;
    }

    private void alertUserAboutError() {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(), "error_dialog");
    }
}
