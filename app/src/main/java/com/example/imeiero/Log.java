package com.example.imeiero;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.Scanner.DataListener;
import com.symbol.emdk.barcode.Scanner.StatusListener;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.StatusData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class Log extends Activity implements EMDKListener, StatusListener, DataListener {
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;
    private EMDKManager emdkManager = null;
    TextView txtUsuario;
    TextView tvPassword;
    AlertDialog alertaX;
    public TextView statusTextView2 = null;
    ProgressBar pb_loading;
    private Button botonLoggear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        tvPassword = findViewById(R.id.tvPassword);
        tvPassword.setTransformationMethod(new PasswordTransformationMethod());
        txtUsuario = (TextView) findViewById(R.id.textUsuario);
        statusTextView2 = findViewById(R.id.textViewStatus0);
        pb_loading = (ProgressBar) findViewById(R.id.progressBar0);
        botonLoggear = (Button) findViewById(R.id.buttonLogin);
        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);

        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            statusTextView2.setText("Falló el EMDKManager. Asi es la vida");
        }

        txtUsuario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                txtUsuario.setText("");
                tvPassword.setText("");
            }
        });

        botonLoggear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String usuario = txtUsuario.getText().toString();
                String key = tvPassword.getText().toString();
                procesandoTarea(true);
                validarUsuario("http://10.107.226.241/apis/imm/log", usuario, key);
            }
        });
        procesandoTarea(false);
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {
        this.emdkManager = emdkManager;
        try {
            initializeScanner();
        } catch (ScannerException e) {
            e.printStackTrace();
        }
        Toast.makeText(Log.this,
                "Escanear usuario de Active para comenzar",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClosed() {
        if (this.emdkManager != null) {
            this.emdkManager.release();
            this.emdkManager = null;
        }
    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {
        new AsyncDataUpdate().execute(scanDataCollection);
    }

    @Override
    public void onStatus(StatusData statusData) {

        try {
            scanner.read();
        } catch (ScannerException e) {
            e.printStackTrace();
        }
        new AsyncStatusUpdate().execute(statusData);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            if (scanner != null) {
                scanner.removeDataListener(this);
                scanner.removeStatusListener(this);
                scanner.disable();
                scanner = null;
            }
        } catch (ScannerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        try {
            initializeScanner();
        } catch (ScannerException e) {
            e.printStackTrace();
        }
        Toast.makeText(Log.this,
                "Aprieta el gatillo de la PDT para escanear",
                Toast.LENGTH_SHORT).show();
    }

    private void initializeScanner() throws ScannerException {
        if (scanner == null) {
            barcodeManager = (BarcodeManager) this.emdkManager
                    .getInstance(EMDKManager.FEATURE_TYPE.BARCODE);
            scanner = barcodeManager.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT);
            scanner.addDataListener(this);
            scanner.addStatusListener(this);
            scanner.triggerType = Scanner.TriggerType.HARD;
            scanner.enable();
            scanner.read();
        }
    }

    public class AsyncDataUpdate extends AsyncTask<ScanDataCollection,Void,String> {
        @Override
        protected String doInBackground(ScanDataCollection... params) {
            String statusStr = "";
            ScanDataCollection scanDataCollection = params[0];

            if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
                ArrayList<ScanDataCollection.ScanData> scanData = scanDataCollection.getScanData();
                statusStr = scanData.get(0).getData();
            }
            return statusStr;
        }

        @Override
        protected void onPostExecute(final String result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try{
                        alertaX.dismiss();
                    }catch(NullPointerException ex){
                        System.out.println(ex.toString());
                    }
                    if(txtUsuario.getText().toString().replace(" ","").trim().length()==0){
                        String usuarioText = result;
                        txtUsuario.setText(usuarioText);
                    }else{
                        String passText = result;
                        tvPassword.setText(passText);
                    }
                }
            });
        }
    }

    private class AsyncStatusUpdate extends AsyncTask<StatusData, Void, String> {

        @Override
        protected String doInBackground(StatusData... params) {
            String statusStr = "";
            StatusData statusData = params[0];
            StatusData.ScannerStates state = statusData.getState();
            switch (state) {
                case IDLE:
                    statusStr = "Escaner habilitado";
                    break;
                case SCANNING:
                    statusStr = "Escaneando...";
                    break;
                case WAITING:
                    statusStr = "Esperando acción del gatillo...";
                    break;
                case DISABLED:
                    statusStr = "Escaner offside";
                    break;
                default:
                    break;
            }
            return statusStr;
        }

        @Override
        protected void onPostExecute(String result) {
            statusTextView2.setText(result);
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    public void detenerScanner(){
        if (this.emdkManager != null) {
            this.emdkManager.release();
            this.emdkManager = null;
        }
        try {
            if (scanner != null) {
                scanner.removeDataListener(this);
                scanner.removeStatusListener(this);
                scanner.disable();
                scanner = null;
            }
        } catch (ScannerException e) {
            e.printStackTrace();
        }
    }

    public void procesandoTarea(boolean status){
        if(status){
            pb_loading.setVisibility(View.VISIBLE);
        }else{
            pb_loading.setVisibility(View.INVISIBLE);
        }
    }

    public void abrirActivityMain (String elUsuario, String token_id){
        detenerScanner();
        Intent i = new Intent(this, Menu.class);
        i.putExtra("loggedUser", elUsuario);
        i.putExtra("tokenId", token_id);
        startActivity(i);
        finish();
    }

    public void JOptionPaneShowMessageDialog(String titulo, String mensaje){
        if (Log.this.alertaX != null) {
            Log.this.alertaX.dismiss();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(titulo);
        builder.setMessage(mensaje);
        builder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        alertaX = builder.create();
        alertaX.show();
    }

    public void alertaDeError(){
        MediaPlayer mp = MediaPlayer.create(this,R.raw.error);
        mp.start();
    }

    private void validarUsuario(String URL, String usuario, String clave){
        StringRequest sr = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>(){
            @Override
            public void onResponse(String response){
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    String status = jsonResponse.getString("response");
                    String mensaje = jsonResponse.getString("mensaje");
                    if(status.equals("ok")){
                        procesandoTarea(false);
                        // Acciones a realizar si el inicio de sesión es exitoso
                        String token_id = jsonResponse.getString("token");
                        guardarToken(token_id);
                        // Navegar a otra actividad o actualizar la UI
                        System.out.println(token_id);
                        abrirActivityMain(usuario,token_id);
                        //Toast.makeText(getApplicationContext(), "Inicio de sesión exitoso", Toast.LENGTH_LONG).show();
                        System.out.println(response);
                    } else {
                        // Acciones a realizar si hay un error
                        procesandoTarea(false);
                        alertaDeError();
                        Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    alertaDeError();
                    procesandoTarea(false);
                    e.printStackTrace();
                    // Manejo de error en caso de que la respuesta JSON no sea válida
                    Toast.makeText(getApplicationContext(), "Error en la respuesta del servidor", Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener(){
            public void onErrorResponse(VolleyError error){
                alertaDeError();
                procesandoTarea(false);
                Toast.makeText(getApplicationContext(), "Error. Ajustar fecha o comunicarse con el soporte de la app.", Toast.LENGTH_LONG).show();
            }
        }){
            @Override
            protected Map<String,String> getParams() throws AuthFailureError {
                Map<String,String> parametros = new HashMap<String,String>();
                parametros.put("usuario", usuario);
                parametros.put("clave", clave);
                return parametros;
            }
        };
        RequestQueue rq = Volley.newRequestQueue(this);
        rq.add(sr);
    }


    private void guardarToken(String token) {
        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("token", token);
        editor.apply();
    }


}


