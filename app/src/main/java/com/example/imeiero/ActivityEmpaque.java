package com.example.imeiero;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ActivityEmpaque extends Activity implements EMDKListener, StatusListener, DataListener{

    AlertDialog alerta;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;
    private EMDKManager emdkManager = null;
    public TextView statusTextView = null;
    Bundle losExtras;
    String usuario,bodega;
    // String        token_id;
    TextView lblIMEI, lblIMEITWO, txtIMEI, txtIMEITWO, txtSKU, txtRESULT, txtOLPN, lblOLPN;
    ProgressBar pb_loading;
    Button botonParaAtras;
    int currentQty;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintwo);

        losExtras = getIntent().getExtras();
        try {
            usuario = losExtras.getString("loggedUser");
        //    token_id = losExtras.getString("tokenId");
        }catch (Exception ex){
            abrirActivityLog();
        }

        pb_loading = (ProgressBar) findViewById(R.id.progress22);
        txtIMEI = (TextView) findViewById(R.id.textIMEI22);
        txtIMEITWO = (TextView) findViewById(R.id.textIMEITWO22);
        lblIMEI = (TextView) findViewById(R.id.labelIMEI22);
        lblIMEITWO = (TextView) findViewById(R.id.labelIMEITWO22);
        txtSKU = (TextView) findViewById(R.id.textSKU22);
        txtRESULT = (TextView) findViewById(R.id.textRESULT22);
        txtOLPN = (TextView) findViewById(R.id.textOLPN);
        lblOLPN = (TextView) findViewById(R.id.labelOLPN);
        botonParaAtras = (Button) findViewById(R.id.buttonBack22);
        bodega = "100";
        currentQty=0;

        botonParaAtras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                abrirActivityMenu(usuario);
            }
        });

        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);

        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            statusTextView.setText("Falló el EMDKManager. Asi es la vida");
        }
        procesandoTarea(false);
        txtIMEI.setVisibility(View.INVISIBLE);
        lblIMEI.setVisibility(View.INVISIBLE);
        txtIMEITWO.setVisibility(View.INVISIBLE);
        lblIMEITWO.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {
        this.emdkManager = emdkManager;
        try {
            initializeScanner();
        } catch (ScannerException e) {
            e.printStackTrace();
        }
        Toast.makeText(ActivityEmpaque.this, "Escanear dato solicitado", Toast.LENGTH_SHORT).show();
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

        Toast.makeText(ActivityEmpaque.this, "Escanear dato solicitado", Toast.LENGTH_SHORT).show();
    }

    private void initializeScanner() throws ScannerException {
        if (scanner == null) {
            // Get the Barcode Manager object
            barcodeManager = (BarcodeManager) this.emdkManager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE);
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
                        alerta.dismiss();
                    }catch(NullPointerException ex){
                        System.out.println(ex.toString());
                    }
                    String item = txtSKU.getText().toString().trim();
                    if(item.length()==0){
                        currentQty=0;
                        hacerTodoLoQueEsteRelacionadoConPincharElSKU(result);
                    }else{
                        String carton = txtOLPN.getText().toString().trim();
                        if(carton.length()==0) {
                            try{
                                long interLong = Long.parseLong(result);
                            }catch(NumberFormatException ex){
                                if(result.length()==11){
                                    txtOLPN.setText(result);
                                }else{
                                    alertaDeError("Pincha un cartón valido");
                                }
                            }
                        }else{
                            if(currentQty==1){
                                trabajoConUnSoloImei(result);
                            }
                            if(currentQty==2){
                                trabajoConDosImei(result);
                            }
                        }
                    }
                }
            });
        }
    }

    private class AsyncStatusUpdate extends AsyncTask<StatusData, Void, String> {

        @Override
        protected String doInBackground(StatusData... params) {
            String statusStr = "";
            // Get the current state of scanner in background
            StatusData statusData = params[0];
            StatusData.ScannerStates state = statusData.getState();
            // Different states of Scanner
            switch (state) {
                // Scanner is IDLE
                case IDLE:
                    statusStr = "Escaner habilitado";
                    break;
                // Scanner is SCANNING
                case SCANNING:
                    statusStr = "Escaneando...";
                    break;
                // Scanner is waiting for trigger press
                case WAITING:
                    statusStr = "Esperando acción del gatillo...";
                    break;
                // Scanner is not enabled
                case DISABLED:
                    statusStr = "Escaner offside";
                    break;
                default:
                    break;
            }

            // Return result to populate on UI thread
            return statusStr;
        }

        @Override
        protected void onPostExecute(String result) {
            // Update the status text view on UI thread with current scanner
            // state
            //statusTextView.setText(result);
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

    public void trabajoConUnSoloImei(String imei){
        try{
            Long.parseLong(imei);
            if(imei.length()==15){
                String olpn = txtOLPN.getText().toString().trim().toUpperCase();
                String item = txtSKU.getText().toString().trim();
                txtIMEI.setText(imei);
                procesandoTarea(true);
                updateConUnImei(imei, item, bodega, usuario,olpn);
            }else{
                alertaDeError("Pinchar IMEI válido");
            }
        }catch (NumberFormatException ex){
            alertaDeError("Pinchar IMEI válido");
        }
    }

    public void trabajoConDosImei(String imei){
        try{
            long interLonga = Long.parseLong(imei);
            if(imei.length()==15){
                String item = txtSKU.getText().toString().trim();
                if(txtIMEI.getText().toString().trim().length()==0){
                    txtIMEI.setText(imei);
                }else{
                    String imei1 = txtIMEI.getText().toString().trim();
                    String imei2 = imei;
                    String olpn = txtOLPN.getText().toString().trim().toUpperCase();
                    txtIMEITWO.setText(imei);
                    procesandoTarea(true);
                    updateConDosImei(imei1, imei2, item, bodega, usuario, olpn);
                }
            }else{
                alertaDeError("Pinchar IMEI válido");
            }
        }catch (NumberFormatException ex){
            alertaDeError("Pinchar IMEI válido");
        }
    }

    public void hacerTodoLoQueEsteRelacionadoConPincharElSKU(String item){
            try{
                long zku = Long.parseLong(item);
                if(item.length()==13||item.length()==12||item.length()==9){
                    String url = "http://10.107.226.241/verificar_imeis_x_sku?sku=" + item;
                    validarSKU(item, usuario);
                }else{
                    alertaDeError("Eso no es un SKU");
                }
            }catch (NumberFormatException ex){
                System.out.println(ex.toString());
                alertaDeError("Eso no es un SKU");
            }
    }

    public void procesandoTarea(boolean status){
        if(status){
            pb_loading.setVisibility(View.VISIBLE);
        }else{
            pb_loading.setVisibility(View.INVISIBLE);
        }
    }

    public void JOptionPaneShowMessageDialog(String titulo, String mensaje){
        try{
            alerta.dismiss();
        }catch(NullPointerException ex){
            System.out.println(ex.toString());
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
        alerta = builder.create();
        alerta.show();
    }

    public void alertaDeError(String error){
        MediaPlayer mp = MediaPlayer.create(this,R.raw.error);
        mp.start();
        Toast.makeText(ActivityEmpaque.this, error, Toast.LENGTH_LONG).show();
    }

    public void alertaDeExito(){
        MediaPlayer mp = MediaPlayer.create(this,R.raw.exito);
        mp.start();
    }

    public void limpiaLaCosa(){
        txtIMEI.setText("");
        txtIMEITWO.setText("");
        txtSKU.setText("");
        txtOLPN.setText("");
        txtOLPN.setVisibility(View.INVISIBLE);
        txtIMEI.setVisibility(View.INVISIBLE);
        lblIMEI.setVisibility(View.INVISIBLE);
        txtIMEITWO.setVisibility(View.INVISIBLE);
        lblIMEITWO.setVisibility(View.INVISIBLE);
    }

    public void exito(String imei, String imei2, String sku, String olpn){
        alertaDeExito();
        currentQty=0;
        txtIMEI.setText("");
        txtIMEITWO.setText("");
        txtSKU.setText("");
        txtOLPN.setText("");
        txtIMEI.setVisibility(View.INVISIBLE);
        lblIMEI.setVisibility(View.INVISIBLE);
        txtIMEITWO.setVisibility(View.INVISIBLE);
        lblIMEITWO.setVisibility(View.INVISIBLE);
        String resultado = "IMEI: "+imei;
        if(!(imei2.equals(""))){
            resultado = resultado + "\nIMEI 2: "+imei2;
        }
        resultado = resultado + "\nSKU: "+sku+"\nOLPN: "+olpn;
        txtRESULT.setText(resultado);
        procesandoTarea(false);
    }

    public void abrirActivityMenu (String elUsuario){
        detenerScanner();
        Intent i = new Intent(this, Menu.class);
        i.putExtra("loggedUser", elUsuario);
        startActivity(i);
        finish();
    }

    public void abrirActivityLog (){
        detenerScanner();
        Intent i = new Intent(this, Log.class);
        startActivity(i);
        finish();
    }

    private void validarSKU(String item, String usuario) {
        String token = obtenerToken();
        StringRequest sr = new StringRequest(Request.Method.POST, "http://10.107.226.241/apis/imm/verificar_imeis_x_sku", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (!response.isEmpty()) {
                    procesandoTarea(false);
                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        JSONObject jsonResponse = jsonArray.getJSONObject(0);
                        String mensaje = jsonResponse.getString("mensaje");
                        if (mensaje.equals("OK")) {
                            int imeis = jsonResponse.getInt("imeis");
                            hacerAlgoConElValor(imeis, item);
                        } else {
                            limpiaLaCosa();
                            if(mensaje.contains("sesion")){
                                alertaDeError("Error: " + mensaje);
                                abrirActivityLog();
                            }else{
                                alertaDeError("Error: " + mensaje);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        alertaDeError("Error al interpretar los datos");
                    }
                } else {
                    procesandoTarea(false);
                    alertaDeError("Problemas de conexion");
                }
            }
        }, new Response.ErrorListener() {
            public void onErrorResponse(VolleyError error) {
                procesandoTarea(false);
                alertaDeError("Problemas de conexion (" + error.toString() + ")");
                abrirActivityLog();
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> parametros = new HashMap<String, String>();
                parametros.put("sku", item);
                parametros.put("user", usuario);
                parametros.put("token", token);
                return parametros;
            }
        };
        RequestQueue rq = Volley.newRequestQueue(this);
        rq.add(sr);
    }

    private void updateConUnImei(String imei, String item, String bodega, String usuario, String carton) {
        String token = obtenerToken();
        StringRequest sr = new StringRequest(Request.Method.POST, "http://10.107.226.241/apis/imm/emp_upd_one", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (!response.isEmpty()) {
                    procesandoTarea(false);
                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        JSONObject jsonResponse = jsonArray.getJSONObject(0);
                        String mensaje = jsonResponse.getString("mensaje");
                        if (mensaje.equals("OK")) {
                            exito(imei, "", item, carton);
                        } else {
                            limpiaLaCosa();
                            if(mensaje.contains("sesion")){
                                alertaDeError("Error: " + mensaje);
                                abrirActivityLog();
                            }else{
                                alertaDeError("Error: " + mensaje);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        alertaDeError("Error al interpretar los datos");
                    }
                } else {
                    procesandoTarea(false);
                    alertaDeError("Problemas de conexion");
                }
            }
        }, new Response.ErrorListener() {
            public void onErrorResponse(VolleyError error) {
                procesandoTarea(false);
                alertaDeError("Problemas de conexion (" + error.toString() + ")");
                abrirActivityLog();
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> parametros = new HashMap<String, String>();
                parametros.put("imei", imei);
                parametros.put("sku", item);
                parametros.put("user", usuario);
                parametros.put("warehouse", bodega);
                parametros.put("olpn", carton);
                parametros.put("token", token);
                return parametros;
            }
        };
        RequestQueue rq = Volley.newRequestQueue(this);
        rq.add(sr);
    }

    private void updateConDosImei(String imei, String imei2, String item, String bodega, String usuario, String carton) {
        String token = obtenerToken();
        StringRequest sr = new StringRequest(Request.Method.POST, "http://10.107.226.241/apis/imm/emp_upd_two", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (!response.isEmpty()) {
                    procesandoTarea(false);
                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        JSONObject jsonResponse = jsonArray.getJSONObject(0);
                        String mensaje = jsonResponse.getString("mensaje");
                        if (mensaje.equals("OK")) {
                            exito(imei, imei2, item, carton);
                        } else {
                            limpiaLaCosa();
                            if(mensaje.contains("sesion")){
                                alertaDeError("Error: " + mensaje);
                                abrirActivityLog();
                            }else{
                                alertaDeError("Error: " + mensaje);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        alertaDeError("Error al interpretar los datos");
                    }
                } else {
                    procesandoTarea(false);
                    alertaDeError("Problemas de conexion");
                    abrirActivityLog();
                }
            }
        }, new Response.ErrorListener() {
            public void onErrorResponse(VolleyError error) {
                procesandoTarea(false);
                alertaDeError("Problemas de conexion (" + error.toString() + ")");
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> parametros = new HashMap<String, String>();
                parametros.put("imei", imei);
                parametros.put("imei2", imei2);
                parametros.put("sku", item);
                parametros.put("user", usuario);
                parametros.put("olpn", carton);
                parametros.put("warehouse", bodega);
                parametros.put("token", token);
                return parametros;
            }
        };
        RequestQueue rq = Volley.newRequestQueue(this);
        rq.add(sr);
    }

    private void updateImeiQty(String item, String cantidad, String usuario) {
        String token = obtenerToken();
        StringRequest sr = new StringRequest(Request.Method.POST, "http://10.107.226.241/apis/imm/update_imei_qty", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                System.out.println("RESPONSE "+response);
                if (!response.isEmpty()) {
                    procesandoTarea(false);
                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        JSONObject jsonResponse = jsonArray.getJSONObject(0); // Obtener el primer objeto del arreglo
                        String mensaje = jsonResponse.getString("mensaje");
                        if (mensaje.equals("OK")) {
                            hacerAlgoConElValor(Integer.parseInt(cantidad), item);
                        } else {
                            if(mensaje.contains("sesion")){
                                alertaDeError("Error: " + mensaje);
                                abrirActivityLog();
                            }else{
                                alertaDeError("Error: " + mensaje);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        alertaDeError("Error al interpretar los datos");
                    }
                } else {
                    procesandoTarea(false);
                    alertaDeError("Problemas de conexion");
                }
            }
        }, new Response.ErrorListener() {
            public void onErrorResponse(VolleyError error) {
                procesandoTarea(false);
                alertaDeError("Problemas de conexion (" + error.toString() + ")");
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> parametros = new HashMap<String, String>();
                parametros.put("sku", item);
                parametros.put("user", usuario);
                parametros.put("qty", cantidad);
                parametros.put("token", token );
                return parametros;
            }
        };
        RequestQueue rq = Volley.newRequestQueue(this);
        rq.add(sr);
    }

    public void hacerAlgoConElValor(int valor, String item){
        if(valor==0){
            preguntarCuantosImeiTieneElItem(item);
        }else if(valor==1){
            Toast.makeText(getApplicationContext(), "Escanear Carton e IMEI", Toast.LENGTH_LONG).show();
            currentQty=1;
            txtSKU.setText(item);
            txtIMEI.setVisibility(View.VISIBLE);
            lblIMEI.setVisibility(View.VISIBLE);
            txtIMEITWO.setVisibility(View.INVISIBLE);
            lblIMEITWO.setVisibility(View.INVISIBLE);
        }else{
            Toast.makeText(getApplicationContext(), "Escanear Carton y los IMEI", Toast.LENGTH_LONG).show();
            currentQty=2;
            txtSKU.setText(item);
            txtIMEI.setVisibility(View.VISIBLE);
            lblIMEI.setVisibility(View.VISIBLE);
            txtIMEITWO.setVisibility(View.VISIBLE);
            lblIMEITWO.setVisibility(View.VISIBLE);
        }
    }

    public void preguntarCuantosImeiTieneElItem(String item){
        AlertDialog.Builder dialogo1 = new AlertDialog.Builder(ActivityEmpaque.this);
        dialogo1.setTitle("Indicar información");
        dialogo1.setMessage("¿ Cuantos IMEI tiene el SKU "+item+" ?");
        dialogo1.setCancelable(false);
        dialogo1.setPositiveButton("1", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogo1, int id) {
                procesandoTarea(true);
                updateImeiQty(item,"1",usuario);
            }
        });
        dialogo1.setNegativeButton("2", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogo1, int id) {
                procesandoTarea(true);
                updateImeiQty(item,"2",usuario);
            }
        });
        dialogo1.show();
    }

    private String obtenerToken() {
        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        return sharedPreferences.getString("token", "");
    }

}


