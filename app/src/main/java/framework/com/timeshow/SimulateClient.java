package framework.com.timeshow;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by wangjf on 9/14/17.
 */

public class SimulateClient implements Runnable{
    private static final int PORT = 9999;
    private Socket socket = null;
    private BufferedReader in = null;
    private PrintWriter out ;
    private String content = "";
    private FeedbackText mFeedbackTxt;
    private Context mContext;

    public Handler mHandler ;
    public void setup(FeedbackText feedBack, Context ctx){
        mFeedbackTxt = feedBack;
        mContext=ctx;
    }
    private void init(){
        mHandler = new Handler(mContext.getMainLooper()) {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                mFeedbackTxt.TextFeedBack(content);
            }
        };
        try {
            socket = new Socket(InetAddress.getLocalHost(), PORT);
            in = new BufferedReader(new InputStreamReader(socket
                    .getInputStream()));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream())), true);
        } catch (IOException ex) {
            ex.printStackTrace();
            ShowDialog("login exception" + ex.getMessage());
        }

    }
    public void ShowDialog(String msg) {
        new AlertDialog.Builder(mContext).setTitle("notification").setMessage(msg)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }
    public void sendCmd(final String cmd){

        new Thread(){
            @Override
            public void run(){
                out.println(cmd);
            }
        }.start();
    }
    @Override
    public void run() {
        init();
        try {
            while (true) {
                if (!socket.isClosed()) {
                    if (socket.isConnected()) {
                        if (!socket.isInputShutdown()) {
                            if ((content = in.readLine()) != null) {
                                content += "\n";
                                mHandler.sendMessage(mHandler.obtainMessage());
                            } else {

                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
