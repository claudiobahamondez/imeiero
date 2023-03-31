package com.example.imeiero;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

public class MainActivity2 extends Activity implements EMDKListener, StatusListener, DataListener{

    AlertDialog alerta;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;
    private EMDKManager emdkManager = null;
    public TextView statusTextView = null;
    Bundle losExtras;
    String usuario, mensaje;
    TextView lblIMEI, lblIMEITWO, txtIMEI, txtIMEITWO, txtSKU, txtRESULT, txtOLPN, lblOLPN;
    ProgressBar pb_loading;
    Button botonParaAtras;
    Conexion c;
    int currentQty;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintwo);
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
        c= new Conexion();
        currentQty=0;
        losExtras =getIntent().getExtras();
        try {
            usuario = losExtras.getString("loggedUser");
        }catch (NullPointerException ex){
            usuario = " ";
        }

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

    //some lines of code omitted for clarity

    @Override
    public void onOpened(EMDKManager emdkManager) {
        System.out.println("XQXQ onOpened");

        this.emdkManager = emdkManager;

        try {
            // Call this method to enable Scanner and its listeners
            initializeScanner();
        } catch (ScannerException e) {
            e.printStackTrace();
        }

// Toast to indicate that the user can now start scanning
        Toast.makeText(MainActivity2.this,
                mensaje,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClosed() {
        System.out.println("XQXQ onClosed");
        if (this.emdkManager != null) {
            this.emdkManager.release();
            this.emdkManager = null;
        }
    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {
        System.out.println("XQXQ onData");
        new AsyncDataUpdate().execute(scanDataCollection);
    }

    @Override
    public void onStatus(StatusData statusData) {
        System.out.println("XQXQ onStatus");
        try {
            scanner.read();
        } catch (ScannerException e) {
            e.printStackTrace();
        }
        new AsyncStatusUpdate().execute(statusData);
    }

    @Override
    protected void onDestroy() {
        System.out.println("XQXQ onDestroy");
        super.onDestroy();
        if (emdkManager != null) {
// Clean up the objects created by EMDK manager
            emdkManager.release();
            emdkManager = null;
        }
    }

    @Override
    protected void onStop() {
        System.out.println("XQXQ onStop");
        super.onStop();
        try {
            if (scanner != null) {
                // releases the scanner hardware resources for other application
                // to use. You must call this as soon as you're done with the
                // scanning.
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
        System.out.println("XQXQ onRestart");
        try {
            // Call this method to enable Scanner and its listeners
            initializeScanner();
        } catch (ScannerException e) {
            e.printStackTrace();
        }

// Toast to indicate that the user can now start scanning
        Toast.makeText(MainActivity2.this,
                "Aprieta el gatillo de la PDT para escanear",
                Toast.LENGTH_SHORT).show();
    }

    private void initializeScanner() throws ScannerException {
        System.out.println("XQXQ initializeScanner");
        if (scanner == null) {
            // Get the Barcode Manager object
            barcodeManager = (BarcodeManager) this.emdkManager
                    .getInstance(EMDKManager.FEATURE_TYPE.BARCODE);
            // Get default scanner defined on the device
            scanner = barcodeManager.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT);
            // Add data and status listeners
            scanner.addDataListener(this);
            scanner.addStatusListener(this);
            // Hard trigger. When this mode is set, the user has to manually
            // press the trigger on the device after issuing the read call.
            scanner.triggerType = Scanner.TriggerType.HARD;
            // Enable the scanner
            scanner.enable();
            // Starts an asynchronous Scan. The method will not turn ON the
            // scanner. It will, however, put the scanner in a state in which
            // the scanner can be turned ON either by pressing a hardware
            // trigger or can be turned ON automatically.
            scanner.read();
        }
    }


    // Update the scan data on UI

    // AsyncTask that configures the scanned data on background
// thread and updated the result on UI thread with scanned data and type of
// label
    public class AsyncDataUpdate extends AsyncTask<ScanDataCollection,Void,String> {
        @Override
        protected String doInBackground(ScanDataCollection... params) {
            String statusStr = "";
            ScanDataCollection scanDataCollection = params[0];

            if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
                ArrayList<ScanDataCollection.ScanData> scanData = scanDataCollection.getScanData();
                //     for(ScanDataCollection.ScanData data : scanData) {         statusStr = data.getData();  }
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
                                long veamos = Long.parseLong(result);
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
            long interLonga = Long.parseLong(imei);
            if(imei.length()==15){
                String olpn = txtOLPN.getText().toString().trim().toUpperCase();
                String item = txtSKU.getText().toString().trim();
                txtIMEI.setText(imei);
                procesandoTarea(true);
                new MainActivity2.UploadInformation().execute(imei, item, usuario,olpn);
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
                    new MainActivity2.UploadInformationFORTWO().execute(imei1, imei2, item, usuario, olpn);
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
                    validarSKU(url, item);
                }else{
                    alertaDeError("Eso no es un SKU");
                    System.out.println("Error no tiene largo 13");
                }
            }catch (NumberFormatException ex){
                System.out.println(ex.toString());
                alertaDeError("Eso no es un SKU");
            }
    }


    class UploadInformation extends AsyncTask<String, Void, String> {
        String records ="";
        String the_imei = ""; String the_sku =""; String the_name="";String the_olpn="";
        @Override
        protected String doInBackground(String... strings) {
            the_imei = strings[0];
            the_sku = strings[1];
            the_name = strings[2];
            the_olpn = strings[3];
            try{
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                Connection connection = DriverManager.getConnection(c.url,c.master,c.masterkey);
                PreparedStatement statement2 = connection.prepareStatement("INSERT INTO IMEI_STOCK (IMEI, SKU, " +
                        "USUARIO, BODEGA, OLPN) VALUES ('"+the_imei+"','"+the_sku+"','"+the_name+"','"+c.bodega+"','"+the_olpn+"') ");
                statement2.executeUpdate();


                PreparedStatement statement3 = connection.prepareStatement("UPDATE IMEI_STOCK SET " +
                        "FECHA_EMPAQUE=FECHA_MOD WHERE IMEI='"+the_imei+"' AND OLPN='"+the_olpn+"' AND FECHA_INVENTARIO IS NULL AND FECHA_EMPAQUE IS NULL");
                statement3.executeUpdate();

                records ="OK";


            } catch (Exception e) {
                records= "PROB";
                System.out.println("ERRORR2 "+e);
            }

            return null;
        }
        @Override
        protected void onPostExecute(String aVoid) {
            System.out.println(records);
            procesandoTarea(false);
            if(records.equals("OK")){
                exito(the_imei, "", the_sku, the_olpn);
            }
            if(records.equals("PROB")){
                alertaDeError("Problemas de conexion");
                procesandoTarea(false);
            }
            super.onPostExecute(aVoid);
        }
    }

    class UploadInformationFORTWO extends AsyncTask<String, Void, String> {
        String records ="";
        String the_imei = ""; String the_imei2 = "";String the_sku =""; String the_name=""; String the_olpn="";
        @Override
        protected String doInBackground(String... strings) {
            the_imei = strings[0];
            the_imei2 = strings[1];
            the_sku = strings[2];
            the_name = strings[3];
            the_olpn = strings[4];

            try{
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                Connection connection = DriverManager.getConnection(c.url,c.master,c.masterkey);
                PreparedStatement statement2 = connection.prepareStatement("INSERT INTO IMEI_STOCK (IMEI, IMEI2, SKU, " +
                        "USUARIO, BODEGA, OLPN) VALUES ('"+the_imei+"','"+the_imei2+"','"+the_sku+"','"+the_name+"','"+c.bodega+"','"+the_olpn+"') ");
                statement2.executeUpdate();


                PreparedStatement statement3 = connection.prepareStatement("UPDATE IMEI_STOCK SET " +
                        "FECHA_EMPAQUE=FECHA_MOD WHERE IMEI='"+the_imei+"' AND IMEI2='"+the_imei2+"' AND OLPN='"+the_olpn+"' AND FECHA_INVENTARIO IS NULL AND FECHA_EMPAQUE IS NULL");
                statement3.executeUpdate();

                records ="OK";


            } catch (Exception e) {
                records= "PROB";
                System.out.println("ERRORR2 "+e);
            }

            return null;
        }
        @Override
        protected void onPostExecute(String aVoid) {
            System.out.println(records);
            procesandoTarea(false);
            if(records.equals("OK")){
                exito(the_imei, the_imei2, the_sku, the_olpn);
            }
            if(records.equals("PROB")){
                alertaDeError("Problemas de conexion");
                procesandoTarea(false);
            }
            super.onPostExecute(aVoid);
        }
    }


    class UpdateImeiQuantity extends AsyncTask<String, Void, String> {
        String records ="";
        String the_sku = ""; String the_qty =""; String the_name="";
        @Override
        protected String doInBackground(String... strings) {
            the_sku = strings[0];
            the_qty = strings[1];
            the_name = strings[2];

            try{
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                Connection connection = DriverManager.getConnection(c.url,c.master,c.masterkey);
                PreparedStatement statement2 = connection.prepareStatement("INSERT INTO IMEIS_X_SKU" +
                        "(SKU, IMEIS, USUARIO) VALUES ('"+the_sku+"',"+the_qty+",'"+the_name+"') ");
                statement2.executeUpdate();

                records ="OK";
            } catch (Exception e) {
                records= "PROB";
                System.out.println("ERRORR2 "+e);
            }

            return null;
        }
        @Override
        protected void onPostExecute(String aVoid) {
            System.out.println(records);
            procesandoTarea(false);
            if(records.equals("OK")){
                hacerAlgoConElValor(Integer.parseInt(the_qty), the_sku);
            }
            if(records.equals("PROB")){
                alertaDeError("Problemas de conexion");
                procesandoTarea(false);
            }
            super.onPostExecute(aVoid);
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
        Toast.makeText(MainActivity2.this, error, Toast.LENGTH_LONG).show();
    }

    public void alertaDeExito(){
        MediaPlayer mp = MediaPlayer.create(this,R.raw.exito);
        mp.start();
    }

    public void realizarProceso(String imei, String sku, String nombre){
        new MainActivity2.UploadInformation().execute(imei,sku,nombre);
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
        txtRESULT.setText("IMEI:"+imei+", IMEI 2:"+imei2+", SKU:"+sku+", OLPN:"+olpn);
        procesandoTarea(false);
    }

    public void abrirActivityMenu (String elUsuario){
        detenerScanner();
        Intent i = new Intent(this, Menu.class);
        i.putExtra("loggedUser", elUsuario);
        startActivity(i);
        finish();
    }

    public void validarSKU(String url, String item) {
        StringRequest stringRequest = new StringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                int valor = 0;
                System.out.println("RESPUESTA :"+response);
                try {
                    JSONArray arregloJSON = new JSONArray(response);
                    JSONObject objetoJSON = arregloJSON.getJSONObject(0);
                    valor = objetoJSON.getInt("IMEIS");
                    hacerAlgoConElValor(valor, item);
                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), "ERROR", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "ERROR", Toast.LENGTH_LONG).show();
                System.out.println(error);
            }
        });
        stringRequest.setRetryPolicy(new RetryPolicy() {
            @Override
            public int getCurrentTimeout() {
                return 500000000;
            }

            @Override
            public int getCurrentRetryCount() {
                return 500000000;
            }

            @Override
            public void retry(VolleyError error) throws VolleyError {
                Toast.makeText(getApplicationContext(), "ERROR", Toast.LENGTH_LONG).show();
                System.out.println(error);
            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest);
    }

    public void hacerAlgoConElValor(int valor, String item){
        if(valor==0){
            preguntarCuantosImeiTieneElItem(item);
        }else if(valor==1){
            Toast.makeText(getApplicationContext(), "1", Toast.LENGTH_LONG).show();
            currentQty=1;
            txtSKU.setText(item);
            txtIMEI.setVisibility(View.VISIBLE);
            lblIMEI.setVisibility(View.VISIBLE);
            txtIMEITWO.setVisibility(View.INVISIBLE);
            lblIMEITWO.setVisibility(View.INVISIBLE);
        }else{
            Toast.makeText(getApplicationContext(), "2", Toast.LENGTH_LONG).show();
            currentQty=2;
            txtSKU.setText(item);
            txtIMEI.setVisibility(View.VISIBLE);
            lblIMEI.setVisibility(View.VISIBLE);
            txtIMEITWO.setVisibility(View.VISIBLE);
            lblIMEITWO.setVisibility(View.VISIBLE);
        }
    }


    public void preguntarCuantosImeiTieneElItem(String item){
        AlertDialog.Builder dialogo1 = new AlertDialog.Builder(MainActivity2.this);
        dialogo1.setTitle("Indicar información");
        dialogo1.setMessage("¿ Cuantos IMEI tiene el SKU "+item+" ?");
        dialogo1.setCancelable(false);
        dialogo1.setPositiveButton("1", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogo1, int id) {
                procesandoTarea(true);
                new MainActivity2.UpdateImeiQuantity().execute(item,"1",usuario);
            }
        });
        dialogo1.setNegativeButton("2", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogo1, int id) {
                procesandoTarea(true);
                new MainActivity2.UpdateImeiQuantity().execute(item,"2",usuario);
            }
        });
        dialogo1.show();
    }

}


