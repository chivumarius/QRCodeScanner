package com.chivumarius.qrcodescanner;



import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



/* ▼ THIS "CLASS"
        → "ACT" AS A "TEMPLATE" ▼ */
public class bottom_dialog extends BottomSheetDialogFragment {

    // ▼ "DECLARATION" OF "WIDGETS IDS"  OF "BOTTOM_DIALOG. XML" FILE ▼
    private TextView title,link,btn_visit;
    private ImageView close;
    private String fetchurl;


    // ▼ "ON CREATE VIEW()" METHOD ▼
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstancesState){
        // ▼ LINKING "BOTTOM_DIALOG. XML" FILE → TO "BOTTOM_DIALOG. JAVA" FILE ▼
        View view = inflater.inflate(R.layout.bottom_dialog, container,false);

        // ▼ "INIALIZATION" OF "WIDGETS IDS"  OF "BOTTOM_DIALOG. XML" FILE ▼
        title = view.findViewById(R.id.txt_title);
        link = view.findViewById(R.id.txt_link);
        btn_visit = view.findViewById(R.id.visit);
        close = view.findViewById(R.id.close);


        // ▼ FUNCTIONALITY OF "BUTTONS" ▼
        // ▼ SETTING THE "TEXT" OF THE "TITLE" AND "LINK" ▼
        title.setText(fetchurl);


        // ▼ "CLICK LISTENER" FOR THE "BTN_VISIT" BUTTON ▼
        btn_visit.setOnClickListener(new View.OnClickListener() {

            // ▼ "ON CLICK()" METHOD ▼
            @Override
            public void onClick(View v) {
                // ▼ "REDIRECTING" THE "USER"  TO THE "URL" FROM "QR CODE" ▼
                Intent intent = new Intent("android.intent.action.VIEW");
                intent.setData(Uri.parse(fetchurl));
                startActivity(intent);
            }
        });


        // ▼ "CLICK LISTENER" FOR THE "CLOSE" BUTTON ▼
        close.setOnClickListener(new View.OnClickListener(){

            // ▼ "ON CLICK()" METHOD ▼
            @Override
            public void onClick(View v){
                dismiss();
            }
        });


        // ▼ RETURNING THE "VIEW" OBJECT ▼
        return view;
    }




    // ▼ "FETCH URL()" METHOD ▼
    public void fetchurl(String url){

        // ▼ USING THE BACKGROUND SERVICE" → TO "FETCH" THE "URL" IN THE "BACKGROUND" ▼
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        // ▼ "CALLING" THE "EXECUTE()" METHOD ▼
        executorService.execute(new Runnable() {

            // ▼ "RUB()" METHOD ▼
            @Override
            public void run() {
                fetchurl = url;
            }
        });
    }
}
