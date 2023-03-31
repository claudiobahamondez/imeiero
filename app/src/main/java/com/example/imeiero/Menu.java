package com.example.imeiero;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class Menu extends Activity {

    Button elBotonI, elBotonE;
    Bundle losExtras;
    String usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        losExtras =getIntent().getExtras();
        try {
            usuario = losExtras.getString("loggedUser");
        }catch (NullPointerException ex){
            usuario = " ";
        }

        elBotonI = (Button) findViewById(R.id.buttonINV);
        elBotonE = (Button) findViewById(R.id.buttonEMP);

        elBotonI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = usuario;
                if(name.length()>3){
                    abrirActivityMain(name);
                }else{
                    alertaDeError("Algo no esta bien!");
                }
            }
        });

        elBotonE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = usuario;
                if(name.length()>3){
                    abrirActivityMain2(name);
                }else{
                    alertaDeError("En serio. Algo no esta bien!");
                }
            }
        });


    }

    public void abrirActivityMain (String elUsuario){
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("loggedUser", elUsuario);
        startActivity(i);
        finish();
    }

    public void abrirActivityMain2 (String elUsuario){
        Intent i = new Intent(this, MainActivity2.class);
        i.putExtra("loggedUser", elUsuario);
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

}


