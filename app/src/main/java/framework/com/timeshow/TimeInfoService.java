package framework.com.timeshow;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by wangjf on 9/14/17.
 */

public class TimeInfoService extends Service {
    static final String TAG="TimeInfoService";
    static final int MSG_TIME_INITIALIZE = 1;
    static final int MSG_TIME_BEGIN_VR = 2;
    static final int MSG_TIME_SUBMIT_FIRST_FRAME = 3;
    static final int MSG_TIME_TIMEWARP_THREAD_START = 4;
    static final int MSG_TIME_TIMEWARP_FIRST_LEFT = 5;
    static final int MSG_TIME_TIMEWARP_FIRST_RIGHT = 6;

    private static final int PORT = 9999;
    private List<Socket> mList = new ArrayList<Socket>();
    private ServerSocket server = null;
    private ExecutorService mExecutorService = null; //thread pool


    //extend config and status.
    //static final int MSG_CONF_WARP_THREAD_FIFO=7;
    class InfoHandler extends Handler{
        //get message from SVR.
        @Override
        public void handleMessage(Message msg){
            //we want to define this obj is JSON object.
            Log.d(TAG,msg.obj.toString());
        }

    }
    @Override
    public void onCreate(){
        Log.d(TAG,"Service onCreate be called");
        super.onCreate();
        //start one deamon to monitor command port.
        mMonitorDeamon.start();
    }

    final Messenger mMessenger = new Messenger(new InfoHandler());
    @Override
    public IBinder onBind(Intent intent){
        Log.d(TAG,"Service onBind be called");
        return mMessenger.getBinder();
    }
    //main monitor daemon
    Thread  mMonitorDeamon =new Thread(new Runnable() {

        @Override
        public void run() {
            try {
                server = new ServerSocket(PORT);
                mExecutorService = Executors.newCachedThreadPool();  //create a thread pool
                Log.d(TAG,"network daemon started");
                Socket client = null;
                while(true) {
                    client = server.accept();
                    //把客户端放入客户端集合中
                    mList.add(client);
                    mExecutorService.execute(new Worker(client)); //start a new thread to handle the connection
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

    });
    //real worker.
    class Worker implements Runnable {
        private Socket socket;
        private BufferedReader in = null;
        private String msg = "";
        HashMap<String,Runnable> ActionList = new HashMap<String,Runnable>();
        private final String CMD_EXIT = "exit";
        private final String CMD_START_APP = "start";

        class ActionExit implements Runnable{
            @Override
            public void run() {
                Log.d(TAG,"client exit");
                mList.remove(socket);
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                msg = "user:" + socket.getInetAddress()
                        + "exit total:" + mList.size();
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sendmsg();
            }
        }
        class ActionStartApp implements Runnable{
            String appName ;
            @Override
            public void run() {
                try {
                    appName = in.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d(TAG,"start app:" + appName);
                //we do start app command .
            }
        }
        private void InitActionTable() {
            ActionList.put(CMD_EXIT,new ActionExit());
            ActionList.put(CMD_START_APP,new ActionStartApp());
        }
        public Worker(Socket socket) {
            InitActionTable();
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //客户端只要一连到服务器，便向客户端发送下面的信息。
                msg = "服务器地址：" +this.socket.getInetAddress() + "come toal:"
                        +mList.size()+"（服务器发送）";
                Log.d(TAG,msg);
                this.sendmsg();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void run() {
            try {
                while(true) {
                    msg = in.readLine();
                    if (ActionList.containsKey(msg)){
                        ActionList.get(msg).run();
                    }
                    if (msg.equals(CMD_EXIT)) {
                        Log.d(TAG,"exit worker thread");
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        /**
         * 循环遍历客户端集合，给每个客户端都发送信息。
         */
        public void sendmsg() {
            Log.d(TAG,"Sending out "+msg);
            int num =mList.size();
            for (int index = 0; index < num; index ++) {
                Socket mSocket = mList.get(index);
                PrintWriter pout = null;
                try {
                    pout = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(mSocket.getOutputStream())),true);
                    pout.println(msg);
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}


