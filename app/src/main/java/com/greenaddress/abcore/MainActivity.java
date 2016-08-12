package com.greenaddress.abcore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getName();
    private DownloadInstallCoreResponseReceiver mDownloadInstallCoreResponseReceiver;
    private RPCResponseReceiver mRpcResponseReceiver;
    private ProgressBar mPB;
    private Button mButton;
    private TextView mTvStatus;
    private TextView mTvDetails;
    private Switch mSwitchCore;
    private View mContent;

    private void postStart() {
        // SHOW FEE AND OTHER NODE INFO
        mButton.setVisibility(View.GONE);
        mTvStatus.setText("Bitcoin Core is running, please switch Core OFF to stop it.");

        mSwitchCore.setVisibility(View.VISIBLE);
        mSwitchCore.setText("Switch Core off");
        if (!mSwitchCore.isChecked())
            mSwitchCore.setChecked(true);

        setSwitch();
    }

    private void postConfigure() {

        mPB.setVisibility(View.GONE);
        mTvDetails.setText("Bitcoin core fetched and configured");
        mTvStatus.setText("Bitcoin Core is not running, please switch Core ON to start it");
        mButton.setVisibility(View.GONE);
        setSwitch();
    }

    private void setSwitch() {
        mSwitchCore.setVisibility(View.VISIBLE);
        mSwitchCore.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                if (isChecked) {
                    mTvDetails.setVisibility(View.GONE);
                    startService(new Intent(MainActivity.this, ABCoreService.class));
                    postStart();
                    mSwitchCore.setText("Switch Core off");
                } else {
                    final Intent i = new Intent(MainActivity.this, RPCIntentService.class);
                    i.putExtra("stop", "yep");
                    startService(i);
                    postConfigure();
                    mSwitchCore.setText("Switch Core on");
                }
            }
        });
    }

    private void reset() {

        mSwitchCore.setVisibility(View.GONE);
        mSwitchCore.setText("Switch Core on");

        mTvStatus.setText("");
        mTvDetails.setText("");
        mPB.setVisibility(View.GONE);

        try {
            // throws if the arch is unsupported
            Utils.getArch();
        } catch (final Utils.UnsupportedArch e) {
            mButton.setVisibility(View.GONE);
            final String msg = String.format("Architeture %s is unsupported", e.arch);
            mTvStatus.setText(msg);
            showSnackMsg(msg, Snackbar.LENGTH_INDEFINITE);
            return;
        }

        // rpc check to see if core is already running!
        startService(new Intent(this, RPCIntentService.class));
    }

    private void showSnackMsg(final String msg) {
        showSnackMsg(msg, Snackbar.LENGTH_LONG);
    }

    private void showSnackMsg(final String msg, final int length) {
        if (msg != null && !msg.trim().isEmpty())
            Snackbar.make(mContent, msg, length).show();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        mPB = (ProgressBar) findViewById(R.id.progressBar);
        mTvStatus = (TextView) findViewById(R.id.textView);
        mButton = (Button) findViewById(R.id.button);
        mTvDetails = (TextView) findViewById(R.id.textViewDetails);
        mSwitchCore = (Switch) findViewById(R.id.switchCore);
        mContent = findViewById(android.R.id.content);
        setSupportActionBar(toolbar);
        reset();
    }

    private String getSpeed(final int bytesPerSec) {
        if (bytesPerSec == 0)
            return "";
        else if (bytesPerSec > 1024 * 1024 * 1024)
            return String.format("%s MB/s", bytesPerSec / (1024 * 1024 * 1024));
        else if (bytesPerSec > 1024 * 1024)
            return String.format("%s KB/s", bytesPerSec / (1024 * 1024));
        else if (bytesPerSec > 1024)
            return String.format("%s KB/s", bytesPerSec / 1024);
        else
            return String.format("%s B/s", bytesPerSec);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mDownloadInstallCoreResponseReceiver);
        unregisterReceiver(mRpcResponseReceiver);
        mDownloadInstallCoreResponseReceiver = null;
        mRpcResponseReceiver = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        final IntentFilter downloadFilter = new IntentFilter(DownloadInstallCoreResponseReceiver.ACTION_RESP);
        if (mDownloadInstallCoreResponseReceiver == null)
            mDownloadInstallCoreResponseReceiver = new DownloadInstallCoreResponseReceiver();
        downloadFilter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(mDownloadInstallCoreResponseReceiver, downloadFilter);


        final IntentFilter rpcFilter = new IntentFilter(RPCResponseReceiver.ACTION_RESP);
        if (mRpcResponseReceiver == null)
            mRpcResponseReceiver = new RPCResponseReceiver();
        rpcFilter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(mRpcResponseReceiver, rpcFilter);
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

    public class DownloadInstallCoreResponseReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP =
                "com.greenaddress.intent.action.MESSAGE_PROCESSED";

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String text = intent.getStringExtra(DownloadInstallCoreIntentService.PARAM_OUT_MSG);
            switch (text) {
                case "OK":
                    postConfigure();
                    break;
                case "exception":
                    final String exe = intent.getStringExtra("exception");
                    Log.i(TAG, exe);
                    showSnackMsg(exe);

                    mButton.setEnabled(true);
                    mPB.setVisibility(View.GONE);
                    mPB.setProgress(0);
                    mTvStatus.setText("Please select SETUP BITCOIN CORE to download and configure Core");

                    if (mSwitchCore.isChecked())
                        mSwitchCore.setChecked(false);

                    reset();
                    break;
                case "ABCOREUPDATE":

                    mTvStatus.setText(String.format("%s %s", getSpeed(intent.getIntExtra("ABCOREUPDATESPEED", 0)), intent.getStringExtra("ABCOREUPDATETXT")));

                    mPB.setVisibility(View.VISIBLE);
                    mPB.setMax(intent.getIntExtra("ABCOREUPDATEMAX", 100));
                    mPB.setProgress(intent.getIntExtra("ABCOREUPDATE", 0));

                    mButton.setEnabled(false);
                    mTvStatus.setText("Please wait. Fetching, unpacking and configuring bitcoin core...");

                    break;
            }
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
                    final String relative = String.format("/bitcoin-%s/bin/%s", Packages.CORE_V, "bitcoind");
                    final boolean requiresDownload = !new File(Utils.getDir(context).getAbsolutePath() + relative).exists();

                    final String exe = intent.getStringExtra("exception");
                    Log.i(TAG, exe);

                    if (requiresDownload) {
                        final float internal = Utils.megabytesAvailable(Utils.getDir(MainActivity.this));
                        final float external = Utils.megabytesAvailable(Utils.getLargestFilesDir(MainActivity.this));

                        if (internal > 70) {
                            mTvStatus.setText("Please select SETUP BITCOIN CORE to download and configure Core");
                            mButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(final View v) {
                                    mButton.setEnabled(false);
                                    mPB.setVisibility(View.VISIBLE);
                                    mPB.setProgress(0);
                                    mTvStatus.setText("Please wait. Fetching, unpacking and configuring bitcoin core...");

                                    startService(new Intent(MainActivity.this, DownloadInstallCoreIntentService.class));
                                }
                            });
                        } else {
                            final String msg = String.format("You have %sMB but need about 70MB available in the internal memory", internal);
                            mTvStatus.setText(msg);
                            mButton.setVisibility(View.GONE);
                            showSnackMsg(msg, Snackbar.LENGTH_INDEFINITE);
                        }

                        if (external < 70000) {
                            final String msg = String.format("You have %sMB but need about 70GB available in the external memory", external);
                            mTvStatus.setText(msg);
                            // button.setVisibility(View.GONE);
                            showSnackMsg(msg);
                        }
                    } else
                        postConfigure();
                    break;
            }
        }
    }
}