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

public class ActivityInventario extends Activity implements EMDKListener, StatusListener, DataListener {

    AlertDialog alerta;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;
    private EMDKManager emdkManager = null;
    public TextView statusTextView = null;
    Bundle losExtras;
    String usuario,bodega;
   // String        token_id;
    TextView lblIMEI, lblIMEITWO, txtIMEI, txtIMEITWO, txtSKU, txtRESULT;
    ProgressBar pb_loading;
    Button botonParaAtras;
    int currentQty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        losExtras = getIntent().getExtras();
        try {
            usuario = losExtras.getString("loggedUser");
           // token_id = losExtras.getString("tokenId");
        }catch (Exception ex){
            abrirActivityLog();
        }

        bodega = "100";
        pb_loading = (ProgressBar) findViewById(R.id.progress);
        txtIMEI = (TextView) findViewById(R.id.textIMEI);
        txtIMEITWO = (TextView) findViewById(R.id.textIMEITWO);
        lblIMEI = (TextView) findViewById(R.id.labelIMEI);
        lblIMEITWO = (TextView) findViewById(R.id.labelIMEITWO);
        txtSKU = (TextView) findViewById(R.id.textSKU);
        txtRESULT = (TextView) findViewById(R.id.textRESULT);
        botonParaAtras = (Button) findViewById(R.id.buttonBack1);
        currentQty = 0;
        losExtras = getIntent().getExtras();


        botonParaAtras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                abrirActivityMenu(usuario);
            }
        });

        EMDKResults results = EMDKManager.getEMDKManager(
                getApplicationContext(), this);

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
        Toast.makeText(ActivityInventario.this, "Escanear dato solicitado", Toast.LENGTH_SHORT).show();
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

        Toast.makeText(ActivityInventario.this, "Aprieta el gatillo de la PDT para escanear", Toast.LENGTH_SHORT).show();
    }

    private void initializeScanner() throws ScannerException {
        System.out.println("XQXQ initializeScanner");
        if (scanner == null) {
            barcodeManager = (BarcodeManager) this.emdkManager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE);
            scanner = barcodeManager.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT);
            scanner.addDataListener(this);
            scanner.addStatusListener(this);
            scanner.triggerType = Scanner.TriggerType.HARD;
            scanner.enable();
            scanner.read();
        }
    }

    public class AsyncDataUpdate extends AsyncTask<ScanDataCollection, Void, String> {

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
                    try {
                        alerta.dismiss();
                    } catch (NullPointerException ex) {
                        System.out.println(ex.toString());
                    }

                    String item = txtSKU.getText().toString().trim();
                    if (item.length() == 0) {
                        currentQty = 0;
                        hacerTodoLoQueEsteRelacionadoConPincharElSKU(result);
                    } else {
                        if (currentQty == 1) {
                            trabajoConUnSoloImei(result);
                        }
                        if (currentQty == 2) {
                            trabajoConDosImei(result);
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
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }

    }

    public void detenerScanner() {
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

    public void trabajoConUnSoloImei(String imei) {
        try {
            long interLong = Long.parseLong(imei);
            if (imei.length() == 15) {
                String item = txtSKU.getText().toString().trim();
                txtIMEI.setText(imei);
                procesandoTarea(true);
                updateConUnImei(imei, item, bodega, usuario);
            }
        } catch (NumberFormatException ex) {
            alertaDeError("Pinchar IMEI válido");
        }
    }

    public void trabajoConDosImei(String imei) {
        try {
            long interLong = Long.parseLong(imei);
            if (imei.length() == 15) {
                String item = txtSKU.getText().toString().trim();
                if (txtIMEI.getText().toString().trim().length() == 0) {
                    txtIMEI.setText(imei);
                } else {
                    String imei1 = txtIMEI.getText().toString().trim();
                    String imei2 = imei;
                    txtIMEITWO.setText(imei);
                    procesandoTarea(true);
                    updateConDosImei(imei1, imei2, item, bodega, usuario);
                }
            }
        } catch (NumberFormatException ex) {
            alertaDeError("Pinchar IMEI válido");
        }
    }

    public void hacerTodoLoQueEsteRelacionadoConPincharElSKU(String item) {
        try {
            long zku = Long.parseLong(item);
            if (item.length() == 13 || item.length() == 12 || item.length() == 9) {
                String url = "http://10.107.226.241/apis/imm/verificar_imeis_x_sku";
                validarSKU(item, usuario);
            } else {
                alertaDeError("Eso no es un SKU");
            }
        } catch (NumberFormatException ex) {
            alertaDeError("Eso no es un SKU");
        }
    }

    public void procesandoTarea(boolean status) {
        if (status) {
            pb_loading.setVisibility(View.VISIBLE);
        } else {
            pb_loading.setVisibility(View.INVISIBLE);
        }
    }

    public void JOptionPaneShowMessageDialog(String titulo, String mensaje) {
        try {
            alerta.dismiss();
        } catch (NullPointerException ex) {
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

    public void limpiaLaCosa(){
        txtIMEI.setText("");
        txtIMEITWO.setText("");
        txtSKU.setText("");
        txtIMEI.setVisibility(View.INVISIBLE);
        lblIMEI.setVisibility(View.INVISIBLE);
        txtIMEITWO.setVisibility(View.INVISIBLE);
        lblIMEITWO.setVisibility(View.INVISIBLE);
    }

    public void alertaDeError(String error) {
        MediaPlayer mp = MediaPlayer.create(this, R.raw.error);
        mp.start();
        Toast.makeText(ActivityInventario.this, error, Toast.LENGTH_LONG).show();
    }

    public void alertaDeExito() {
        MediaPlayer mp = MediaPlayer.create(this, R.raw.exito);
        mp.start();
    }

    public void exito(String imei, String imei2, String sku) {
        alertaDeExito();
        currentQty = 0;
        txtIMEI.setText("");
        txtIMEITWO.setText("");
        txtSKU.setText("");
        txtIMEI.setVisibility(View.INVISIBLE);
        lblIMEI.setVisibility(View.INVISIBLE);
        txtIMEITWO.setVisibility(View.INVISIBLE);
        lblIMEITWO.setVisibility(View.INVISIBLE);
        String resultado = "IMEI: "+imei;
        if(!(imei2.equals(""))){
            resultado = resultado + "\nIMEI 2: "+imei2;
        }
        resultado = resultado + "\nSKU: "+sku;
        txtRESULT.setText(resultado);
        procesandoTarea(false);
    }

    public void abrirActivityMenu(String elUsuario) {
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

    private void updateConUnImei(String imei, String item, String bodega, String usuario) {
        String token = obtenerToken();
        StringRequest sr = new StringRequest(Request.Method.POST, "http://10.107.226.241/apis/imm/inv_upd_one", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (!response.isEmpty()) {
                    procesandoTarea(false);
                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        JSONObject jsonResponse = jsonArray.getJSONObject(0);
                        String mensaje = jsonResponse.getString("mensaje");
                        if (mensaje.equals("OK")) {
                            exito(imei, "", item);
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
                parametros.put("token", token);
                return parametros;
            }
        };
        RequestQueue rq = Volley.newRequestQueue(this);
        rq.add(sr);
    }

    private void updateConDosImei(String imei, String imei2, String item, String bodega, String usuario) {
        String token = obtenerToken();
        StringRequest sr = new StringRequest(Request.Method.POST, "http://10.107.226.241/apis/imm/inv_upd_two", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (!response.isEmpty()) {
                    procesandoTarea(false);
                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        JSONObject jsonResponse = jsonArray.getJSONObject(0);
                        String mensaje = jsonResponse.getString("mensaje");
                        if (mensaje.equals("OK")) {
                            exito(imei, imei2, item);
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
                parametros.put("imei2", imei2);
                parametros.put("sku", item);
                parametros.put("user", usuario);
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
                        JSONObject jsonResponse = jsonArray.getJSONObject(0);
                        String mensaje = jsonResponse.getString("mensaje");
                        if (mensaje.equals("OK")) {
                            hacerAlgoConElValor(Integer.parseInt(cantidad), item);
                        } else {
                            limpiaLaCosa();
                            if(mensaje.contains("sesion")){
                                alertaDeError("Error: " + mensaje);
                                abrirActivityLog();
                            } else {
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
                parametros.put("qty", cantidad);
                parametros.put("token", token);
                return parametros;
            }
        };
        RequestQueue rq = Volley.newRequestQueue(this);
        rq.add(sr);
    }

    public void hacerAlgoConElValor(int valor, String item) {
        if (valor == 0) {
            preguntarCuantosImeiTieneElItem(item);
        } else if (valor == 1) {
            Toast.makeText(getApplicationContext(), "Escanear IMEI", Toast.LENGTH_LONG).show();
            currentQty = 1;
            txtSKU.setText(item);
            txtIMEI.setVisibility(View.VISIBLE);
            lblIMEI.setVisibility(View.VISIBLE);
            txtIMEITWO.setVisibility(View.INVISIBLE);
            lblIMEITWO.setVisibility(View.INVISIBLE);
        } else {
            Toast.makeText(getApplicationContext(), "Escanar los IMEI", Toast.LENGTH_LONG).show();
            currentQty = 2;
            txtSKU.setText(item);
            txtIMEI.setVisibility(View.VISIBLE);
            lblIMEI.setVisibility(View.VISIBLE);
            txtIMEITWO.setVisibility(View.VISIBLE);
            lblIMEITWO.setVisibility(View.VISIBLE);
        }
    }

    public void preguntarCuantosImeiTieneElItem(String item) {
        AlertDialog.Builder dialogo1 = new AlertDialog.Builder(ActivityInventario.this);
        dialogo1.setTitle("Indicar información");
        dialogo1.setMessage("¿ Cuantos IMEI tiene el SKU " + item + " ?");
        dialogo1.setCancelable(false);
        dialogo1.setPositiveButton("1", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogo1, int id) {
                procesandoTarea(true);
                updateImeiQty(item, "1", usuario);
            }
        });
        dialogo1.setNegativeButton("2", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogo1, int id) {
                procesandoTarea(true);
                updateImeiQty(item, "2", usuario);
            }
        });
        dialogo1.show();
    }

    private String obtenerToken() {
        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        return sharedPreferences.getString("token", "");
    }

}