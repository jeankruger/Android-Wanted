package fr.chronosweb.android.wanted.common;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

/**
 * Created by Jean Kruger on 12/07/14.
 * http://www.chronos-web.fr
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

public class WearSyncActivityTools implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    private final static String TAG = "WearSyncActivityTools";
    private GoogleApiClient mGoogleApiClient;

    private WearSyncToolsCallback mWearSyncToolsCallback;
    private boolean mIsConnected = false;
    private boolean mDisconnectOnCloseDistantActivity = false;

    public WearSyncActivityTools(Context context, WearSyncToolsCallback wearSyncToolsCallback){
        mWearSyncToolsCallback = wearSyncToolsCallback;
        GoogleApiClient.Builder googleApiClientBuilder = new GoogleApiClient.Builder(context);
        googleApiClientBuilder.addConnectionCallbacks(this);
        googleApiClientBuilder.addOnConnectionFailedListener(this);
        googleApiClientBuilder.addApi(Wearable.API);
        mGoogleApiClient = googleApiClientBuilder.build();
    }

    public boolean isConnected(){
        return mIsConnected;
    }

    public void connect(){
        if (!mIsConnected && !mGoogleApiClient.isConnecting()){
            mWearSyncToolsCallback.onConnecting();
            if (!mGoogleApiClient.isConnected()){
                mGoogleApiClient.connect();
            }else{
                this.sendMessage(Constants.OPEN_ACTIVITY_PATH);
            }
        }
    }

    public void closeDistantActivity(){
        mDisconnectOnCloseDistantActivity = false;
        this.sendMessage(Constants.CLOSE_ACTIVITY_PATH);
    }

    public void disconnect(){
        mDisconnectOnCloseDistantActivity = true;
        this.sendMessage(Constants.CLOSE_ACTIVITY_PATH);
    }

    public void sendMessage(String message){
        new WearMessenger().execute(message);
    }

    public void putDataItem(PutDataMapRequest request){
        if (mGoogleApiClient.isConnected()){
            Wearable.DataApi.putDataItem(mGoogleApiClient, request.asPutDataRequest());
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        this.sendMessage(Constants.OPEN_ACTIVITY_PATH);
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mWearSyncToolsCallback.onConnectionFailed();
    }

    public interface WearSyncToolsCallback{
        public void onConnecting();
        public void onConnected();
        public void onConnectionFailed();
        public void onDisconnected();
    }

    private class WearMessenger extends AsyncTask<String, Void, String> {
        private void sendMessageToWear(String message){
            if (mGoogleApiClient.isConnected()){
                List<Node> nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await().getNodes();
                if (nodes != null && !nodes.isEmpty()){
                    for(Node node : nodes){
                        Log.d(TAG, "Send message " + message + " to " + node.getId());
                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), message, null).await();
                        if (result.getStatus().isSuccess()){
                            if (!mIsConnected){
                                mIsConnected = true;
                                mWearSyncToolsCallback.onConnected();
                            }
                        }else{
                            Log.d(TAG, "Message failed " + message + " to " + node.getId());
                            mWearSyncToolsCallback.onConnectionFailed();
                        }
                    }
                }else{
                    mIsConnected = false;
                    Log.d(TAG, "No nodes founds");
                    mWearSyncToolsCallback.onConnectionFailed();
                }
            }else{
                mIsConnected = false;
                Log.d(TAG, "GoogleApiClient is not connected");
                mWearSyncToolsCallback.onConnectionFailed();
            }
        }

        @Override
        protected String doInBackground(String... params) {
            for(String param : params){
                this.sendMessageToWear(param);
            }
            return params[params.length - 1];
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s.equals(Constants.CLOSE_ACTIVITY_PATH) && mGoogleApiClient != null){
                if (mDisconnectOnCloseDistantActivity){
                    mGoogleApiClient.disconnect();
                }
                mIsConnected = false;
                mWearSyncToolsCallback.onDisconnected();
            }
        }
    };
}