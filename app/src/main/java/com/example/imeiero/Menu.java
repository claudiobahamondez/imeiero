package com.example.imeiero;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class Menu extends Activity {

    Button elBotonI, elBotonE;
    Bundle losExtras;
    String usuario, token_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        losExtras = getIntent().getExtras();
        try {
            usuario = losExtras.getString("loggedUser");
            token_id = losExtras.getString("tokenId");
        }catch (Exception ex){
            abrirActivityLog();
        }

        elBotonI = (Button) findViewById(R.id.buttonINV);
        elBotonE = (Button) findViewById(R.id.buttonEMP);

        elBotonI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user = usuario;
                String token = token_id;
                if(user.length()>3){
                    abrirActivityInventario(user,token);
                }else{
                    alertaDeError("Algo no esta bien!");
                }
            }
        });

        elBotonE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user = usuario;
                String token = token_id;
                if(user.length()>3){
                    abrirActivityEmpaque(user, token);
                }else{
                    alertaDeError("En serio. Algo no esta bien!");
                }
            }
        });


    }

    public void abrirActivityInventario (String elUsuario, String elToken){
        Intent i = new Intent(this, ActivityInventario.class);
        i.putExtra("loggedUser", elUsuario);
        i.putExtra("tokenId", elToken);
        startActivity(i);
        finish();
    }

    public void abrirActivityEmpaque(String elUsuario, String elToken){
        Intent i = new Intent(this, ActivityEmpaque.class);
        i.putExtra("loggedUser", elUsuario);
        i.putExtra("tokenId", elToken);
        startActivity(i);
        finish();
    }

    public void alertaDeError(String error){
        MediaPlayer mp = MediaPlayer.create(this,R.raw.error);
        mp.start();
        Toast.makeText(Menu.this,
                error,
                Toast.LENGTH_SHORT).show();
    }

    public void abrirActivityLog (){
        Intent i = new Intent(this, Log.class);
        startActivity(i);
        finish();
    }

}


