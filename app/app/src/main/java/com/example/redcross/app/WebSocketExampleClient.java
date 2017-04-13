package com.example.redcross.app;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import java.util.Map;

import java.net.URI;
import org.java_websocket.handshake.ServerHandshake;
import android.util.Log;

/**
 * Created by kbrar on 4/12/17.
 */

public class WebSocketExampleClient extends WebSocketClient {
    public WebSocketExampleClient( URI serverUri, Draft draft, Map<String, String> headers, int timeout) {
        super( serverUri, draft, headers, timeout );
    }
    @Override
    public void onOpen( ServerHandshake handshakedata ) {
        Log.d("websocket", "open");
    }
    @Override
    public void onMessage( String message ) {
        final String msg = message;
        Log.d("websocket", msg);
        //Handle this message
    }
    @Override
    public void onClose( int code, String reason, boolean remote ) {
        Log.d("websocket", "closed");
    }
    @Override
    public void onError( Exception ex ) {
        ex.printStackTrace();
    }
}
