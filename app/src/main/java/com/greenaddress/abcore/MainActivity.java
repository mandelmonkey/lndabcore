package com.greenaddress.abcore;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import lnrpc.Rpc.NewAddressRequest.AddressType;

import com.google.protobuf.ByteString;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lndmobile.Callback;
import lndmobile.Lndmobile;
import lnrpc.Rpc.*;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getName();
    private RPCResponseReceiver mRpcResponseReceiver;
    private TextView mTvStatus;
    private Switch mSwitchCore;
    private Switch mSwitchLND;
    private Button mGetInfo;
    private ProgressBar mProgressBar;


    private void postDetection() {

        mProgressBar.setVisibility(View.GONE);
        mSwitchCore.setVisibility(View.VISIBLE);
        mSwitchLND.setVisibility(View.VISIBLE);
    }

    private void preDetection() {
        mProgressBar.setVisibility(View.VISIBLE);
        mSwitchCore.setVisibility(View.GONE);
        mSwitchLND.setVisibility(View.GONE);
    }

    private void postStart() {
        mSwitchCore.setOnCheckedChangeListener(null);
        postDetection();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        final String useDistribution = prefs.getString("usedistribution", prefs.getBoolean("useknots", false) ? "knots" : "core");
        mTvStatus.setText(getString(R.string.runningturnoff, useDistribution, useDistribution.equals("knots") ? Packages.BITCOIN_KNOTS_NDK : Packages.BITCOIN_NDK));
        if (!mSwitchCore.isChecked())
            mSwitchCore.setChecked(true);
        mSwitchCore.setText(R.string.switchcoreoff);
        setSwitch();

    }

    private void postConfigure() {
        mSwitchCore.setOnCheckedChangeListener(null);
        postDetection();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        final String useDistribution = prefs.getString("usedistribution", prefs.getBoolean("useknots", false) ? "knots" : "core");
        mTvStatus.setText(getString(R.string.stoppedturnon, useDistribution, useDistribution.equals("knots") ? Packages.BITCOIN_KNOTS_NDK : Packages.BITCOIN_NDK));
        if (mSwitchCore.isChecked())
            mSwitchCore.setChecked(false);
        mSwitchCore.setText("Start bitcoinD");
        setSwitch();

        setSwitchLND();
    }
    public void generateAddress(){
        NewAddressRequest.Builder req = NewAddressRequest.newBuilder();
        req.setType(AddressType.WITNESS_PUBKEY_HASH);

        Lndmobile.newAddress(req.build().toByteArray(), new Callback() {
            @Override
            public void onError(Exception e) {
                try {
                    Log.e("LND", e.getLocalizedMessage());
                } catch (Exception e2) {
                }
            }

            @Override
            public void onResponse(byte[] bytes) {

                try {
                    NewAddressResponse response = NewAddressResponse.parseFrom(bytes);
                Log.d("LND",response.getAddress());

                } catch (Exception e) {

                }


            }
        });




    }

    public void getWalletBalance(){

        Lndmobile.walletBalance(WalletBalanceRequest.getDefaultInstance().toByteArray(), new Callback() {
            @Override
            public void onError(Exception e) {
                try {
                    Log.e("LND", e.getLocalizedMessage());
                } catch (Exception e2) {
                }
            }

            @Override
            public void onResponse(byte[] bytes) {

                try {
                    WalletBalanceResponse response = WalletBalanceResponse.parseFrom(bytes);
                    Log.d("LND","confirmed balance "+response.getConfirmedBalance()+"");
                    Log.d("LND","unconfirmed balance "+response.getUnconfirmedBalance()+"");


                } catch (Exception e) {

                }


            }
        });




    }

    public void openChannel(View view){

        EditText pubkeyField = findViewById(R.id.channelField);

        OpenChannelRequest.Builder req = OpenChannelRequest.newBuilder();
        req.setNodePubkeyString(pubkeyField.getText().toString());
        req.setLocalFundingAmount(100000);



        Lndmobile.openChannelSync(req.build().toByteArray(), new Callback() {
            @Override
            public void onError(Exception e) {
                try {
                    Log.e("LND", e.getLocalizedMessage());
                } catch (Exception e2) {
                }
            }

            @Override
            public void onResponse(byte[] bytes) {

                try {
                    ChannelPoint response = ChannelPoint.parseFrom(bytes);

                    Log.d("LND","funding tx id "+response.getFundingTxidStr());


                } catch (Exception e) {

                }


            }
        });




    }

    public void sendPayment(View view){

        EditText paymentReq = findViewById(R.id.sendPaymentField);

        SendRequest.Builder sendReqBuilder = SendRequest.newBuilder();

        sendReqBuilder.setPaymentRequest(paymentReq.getText().toString());



        Lndmobile.sendPaymentSync(sendReqBuilder.build().toByteArray(), new Callback() {
            @Override
            public void onError(Exception e) {
                try {
                    Log.e("LND", e.getLocalizedMessage());
                } catch (Exception e2) {
                }
            }

            @Override
            public void onResponse(byte[] bytes) {

                try {
                    SendResponse response = SendResponse.parseFrom(bytes);
                    Log.d("LND","sendResponse "+response.toString());


                } catch (Exception e) {

                }


            }
        });




    }
    public void connectPeer(View view){

       EditText peerField = findViewById(R.id.peerField);
       String[] parts = peerField.getText().toString().split("@");
       Log.d("LND",parts[0]+"@"+parts[1]);
        LightningAddress.Builder lnAddress = LightningAddress.newBuilder();
        lnAddress.setHost(parts[1]);
        lnAddress.setPubkey(parts[0]);

            ConnectPeerRequest.Builder req = ConnectPeerRequest.newBuilder();
            req.setAddr(lnAddress.build());
        Lndmobile.connectPeer(req.build().toByteArray(), new Callback() {
            @Override
            public void onError(Exception e) {
                try {
                    Log.e("LND", e.getLocalizedMessage());
                } catch (Exception e2) {
                }
            }

            @Override
            public void onResponse(byte[] bytes) {

                try {
                   ConnectPeerResponse response = ConnectPeerResponse.parseFrom(bytes);
                    Log.d("LND","connectPeer "+response.toString());


                } catch (Exception e) {

                }


            }
        });




    }
    public void getInfo(View view) {
        // Do something in response to button click
        Log.d("LND", "getting info");
        Lndmobile.getInfo( GetInfoRequest.getDefaultInstance().toByteArray(), new Callback() {
            @Override
            public void onError(Exception e) {
                try {
                    Log.e("LND", e.getLocalizedMessage());
                } catch (Exception e2) {
                }
            }

            @Override
            public void onResponse(byte[] bytes) {

                try {
                    GetInfoResponse response = GetInfoResponse.parseFrom(bytes);


                        JSONObject resJson = new JSONObject();
                        resJson.put("identity_pubkey", response.getIdentityPubkey());
                        resJson.put("num_active_channels", response.getNumActiveChannels());
                        resJson.put("alias", response.getAlias());
                        resJson.put("testnet", response.getTestnet());
                        resJson.put("synced_to_chain", response.getSyncedToChain());
                        resJson.put("block_height", response.getBlockHeight());


                        try {

                            int urisCount = response.getUrisCount();
                            JSONArray urisArray = new JSONArray();

                            for (int i2 = 0; i2 < urisCount; i2++) {

                                urisArray.put(response.getUris(i2));
                            }

                            resJson.put("uris", urisArray);

                            Log.d("LND",resJson.toString());

                           // generateAddress();
                            getWalletBalance();
                        } catch (Exception e) {

                        }

                } catch (Exception e) {

                }


            }
        });

    }

    private void setSwitch() {
        mSwitchCore.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                preDetection();
                if (isChecked) {
                    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    final SharedPreferences.Editor e = prefs.edit();
                    e.putBoolean("magicallystarted", false);
                    e.apply();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(new Intent(MainActivity.this, ABCoreService.class));
                    } else {
                        startService(new Intent(MainActivity.this, ABCoreService.class));
                    }
                }
                else {
                    final Intent i = new Intent(MainActivity.this, RPCIntentService.class);
                    i.putExtra("stop", "yep");
                    startService(i);
                }
            }
        });
    }

    private void setSwitchLND() {
        Log.d("LND","set switch");
        mSwitchLND.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
               startLND();

                Log.d("LND","starting lnd");
            }
        });
    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        mTvStatus = findViewById(R.id.textView);
        mSwitchCore = findViewById(R.id.switchCore);
        mSwitchLND = findViewById(R.id.switchLND);
        mProgressBar = findViewById(R.id.progressBar);
        mGetInfo = findViewById(R.id.getInfo);
        setSupportActionBar(toolbar);
        setSwitch();
        setSwitchLND();
        final File appDir = this.getFilesDir();
    Log.d(TAG,"context "+appDir);
//uncomment this to delete lnd directory
/*
        File dir = new File(appDir+"");
        if (dir.isDirectory())
        {
            Log.d("LND","is direc");
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++)
            {
                Log.d("LND",children[i]);
                new File(dir, children[i]).delete();
            }
            deleteRecursive(dir);
        }else{
            Log.e("LND","not direc");
        }

*/
        copyConfig(appDir);

    }

    public void generateSeedAndCreateWallet(){

        try {


            GenSeedRequest req = GenSeedRequest.getDefaultInstance();
            Lndmobile.genSeed(req.toByteArray(), new Callback() {
                @Override
                public void onError(Exception e) {
                    try {

                    } catch (Exception e2) {
                    }
                }

                @Override
                public void onResponse(byte[] bytes) {

                    try {
                        GenSeedResponse response = GenSeedResponse.parseFrom(bytes);

                        Log.d("LND phrase", response.getCipherSeedMnemonicList().toString());

                        InitWalletRequest.Builder req = InitWalletRequest.newBuilder();

                        req.addAllCipherSeedMnemonic(response.getCipherSeedMnemonicList());

                        ByteString passwordBS = ByteString.copyFrom("password", "UTF-8");


                        req.setWalletPassword(passwordBS);


                        Lndmobile.initWallet(req.build().toByteArray(), new Callback() {
                            @Override
                            public void onError(Exception e) {
                                try {
                                    Log.e("LND",e.getLocalizedMessage());
                                } catch (Exception e2) {
                                }
                            }

                            @Override
                            public void onResponse(byte[] bytes) {

                                try {
                                    InitWalletResponse res = InitWalletResponse.parseFrom(bytes);
                                    Log.d("LND", res.toString());
                                }
                                catch(Exception e){

                                }


                            }
                        });






                    }
                    catch(Exception e){

                    }


                }
            });

        }
        catch (Exception e){
            Log.e("LND",e.getLocalizedMessage());

        }

    }

    public void unlockWalletWithPassword(){

        try {


            ByteString passwordBS = ByteString.copyFrom("password", "UTF-8");


            UnlockWalletRequest.Builder req = UnlockWalletRequest.newBuilder();
            req.setWalletPassword(passwordBS);

            Log.d("LND", "unlocking wallet");

            Lndmobile.unlockWallet(req.build().toByteArray(), new Callback() {
                @Override
                public void onError(Exception e) {
                    try {
                        Log.e("LND", e.getLocalizedMessage());
                    } catch (Exception e2) {
                    }
                }

                @Override
                public void onResponse(byte[] bytes) {

                    try {
                        UnlockWalletResponse res = UnlockWalletResponse.parseFrom(bytes);
                        Log.d("LND", res.toString());
                    } catch (Exception e) {

                    }


                }
            });

        }
        catch (Exception e){

        }

    }

    public void startLND(){
        final File appDir = this.getFilesDir();
        copyConfig(appDir);
        Runnable startLnd = new Runnable() {
            @Override
            public void run() {
                Lndmobile.start(appDir+"", new Callback() {
                    @Override
                    public void onError(Exception e) {
                        try {
                            JSONObject json = new JSONObject();
                            json.put("error", true);
                            json.put("response", e.getLocalizedMessage());

                           Log.e("LND",json.toString());
                        } catch (Exception e2) {
                            Log.e("LND",e2.getLocalizedMessage());
                        }
                    }

                    @Override
                    public void onResponse(byte[] bytes) {

                        Log.d("LND", "started");

                        //first time run this function
                        //generateSeedAndCreateWallet();

                        //second time run this function
                        unlockWalletWithPassword();



                    }
                });
            }
        };
        new Thread(startLnd).start();
    }

    private void copyConfig(File appDir) {
        File conf = new File(appDir, "lnd.conf");
        AssetManager am = this.getAssets();

        try (InputStream in = am.open("lnd.conf")) {
            copy(in, conf);
            Log.d(TAG,"conf copied");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void copy(InputStream in, File dst) throws IOException {
        try (OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        if (mRpcResponseReceiver != null)
            unregisterReceiver(mRpcResponseReceiver);
        mRpcResponseReceiver = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!Utils.isBitcoinCoreConfigured(this)) {
            startActivity(new Intent(this, DownloadActivity.class));
            return;
        }

        final IntentFilter rpcFilter = new IntentFilter(RPCResponseReceiver.ACTION_RESP);
        if (mRpcResponseReceiver == null)
            mRpcResponseReceiver = new RPCResponseReceiver();
        rpcFilter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(mRpcResponseReceiver, rpcFilter);

        preDetection();
        startService(new Intent(this, RPCIntentService.class));
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.configuration:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.peerview:
                startActivity(new Intent(this, PeerActivity.class));
                return true;
            case R.id.synchronization:
                startActivity(new Intent(this, ProgressActivity.class));
                return true;
            case R.id.debug:
                startActivity(new Intent(this, LogActivity.class));
                return true;
            case R.id.console:
                startActivity(new Intent(this, ConsoleActivity.class));
                return true;
            case R.id.about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public class RPCResponseReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP =
                "com.greenaddress.intent.action.RPC_PROCESSED";

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String text = intent.getStringExtra(RPCIntentService.PARAM_OUT_MSG);
            switch (text) {
                case "OK":
                    postStart();
                    break;
                case "exception":
                    final String exe = intent.getStringExtra("exception");
                    if (exe != null)
                        Log.i(TAG, exe);
                    postConfigure();
            }
        }
    }
}
