package com.mindorks.annotation.processing.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;


import com.leo.font.lib.binder.FontBinding;
import com.vn.fa.font.FontManager;
import com.leo.font.lib.annotations.AutoScale;
import com.mindorks.lib.annotations.BindView;
import com.leo.font.lib.annotations.IgnoreScale;

import mindorks.com.annotationprocessingexample.R;

public class MainActivity extends AppCompatActivity {
    @AutoScale
    @BindView(R.id.tv_content)
    TextView tvContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvContent = findViewById(R.id.tv_content);
        FontManager.getDefault().setScale(3);
        FontBinding.bind(this);
        new MainActivity$FontBinding(this);
//        new Abc(this);
    }

    //@OnClick(R.id.bt_1)
    void bt1Click(View v) {
        tvContent.setText("Button 1 Clicked");
    }

    //@OnClick(R.id.bt_2)
    void bt2Click(View v) {
        tvContent.setText("Button 2 Clicked");
    }
    public class TestActivity  extends AppCompatActivity{
        @IgnoreScale
        @BindView(R.id.tv_content)
        public TextView tvContent;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            //FontBinding.bind(this);
            //new MainActivity$TestActivity$FontBinding(this);
            new MainActivity$TestActivity$FontBinding(this);
        }
    }
}
