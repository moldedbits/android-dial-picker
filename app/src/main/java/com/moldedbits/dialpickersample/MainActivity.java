package com.moldedbits.dialpickersample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.moldedbits.dialpicker.DialView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DialView dialViewLeft = (DialView) findViewById(R.id.dial_left);
        DialView dialViewTop = (DialView) findViewById(R.id.dial_top);
        DialView dialViewRight = (DialView) findViewById(R.id.dial_right);
        DialView dialViewBottom = (DialView) findViewById(R.id.dial_bottom);
        final TextView textViewLeft = (TextView) findViewById(R.id.tv_left);
        final TextView textViewTop = (TextView) findViewById(R.id.tv_top);
        final TextView textViewRight = (TextView) findViewById(R.id.tv_right);
        final TextView textViewBottom = (TextView) findViewById(R.id.tv_bottom);

        dialViewLeft.setOnDialValueChangeListener(new DialView.OnDialValueChangeListener() {
            @Override
            public void onDialValueChanged(String value, int maxValue) {
                textViewLeft.setText(value);
            }
        });
        dialViewTop.setOnDialValueChangeListener(new DialView.OnDialValueChangeListener() {
            @Override
            public void onDialValueChanged(String value, int maxValue) {
                textViewTop.setText(value);
            }
        });
        dialViewRight.setOnDialValueChangeListener(new DialView.OnDialValueChangeListener() {
            @Override
            public void onDialValueChanged(String value, int maxValue) {
                textViewRight.setText(value);
            }
        });
        dialViewBottom.setOnDialValueChangeListener(new DialView.OnDialValueChangeListener() {
            @Override
            public void onDialValueChanged(String value, int maxValue) {
                textViewBottom.setText(value);
            }
        });
    }
}
