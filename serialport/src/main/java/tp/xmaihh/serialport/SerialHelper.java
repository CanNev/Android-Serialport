package tp.xmaihh.serialport;

import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

import android_serialport_api.SerialPort;
import tp.xmaihh.serialport.bean.ComBean;
import tp.xmaihh.serialport.utils.ByteUtil;

public abstract class SerialHelper {
    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    private SendThread mSendThread;
    private String sPort = "/dev/ttyS3";
    private int iBaudRate = 9600;
    private boolean _isOpen = false;
    private byte[] _bLoopData = {48};
    private int iDelay = 500;


    public SerialHelper(String sPort, int iBaudRate) {
        this.sPort = sPort;
        this.iBaudRate = iBaudRate;
    }

    public void open()
            throws SecurityException, IOException, InvalidParameterException {
        this.mSerialPort = new SerialPort(new File(this.sPort), this.iBaudRate, 0);
        this.mOutputStream = this.mSerialPort.getOutputStream();
        this.mInputStream = this.mSerialPort.getInputStream();
        this.mReadThread = new ReadThread();
        this.mReadThread.start();
        this.mSendThread = new SendThread();
        this.mSendThread.setSuspendFlag();
        this.mSendThread.start();
        this._isOpen = true;
    }

    public void close() {
        if (this.mReadThread != null) {
            this.mReadThread.interrupt();
        }
        if (this.mSerialPort != null) {
            this.mSerialPort.close();
            this.mSerialPort = null;
        }
        this._isOpen = false;
    }

    public void send(byte[] bOutArray) {
        try {
            this.mOutputStream.write(bOutArray);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendHex(String sHex) {
        byte[] bOutArray = ByteUtil.HexToByteArr(sHex);
        send(bOutArray);
    }

    public void sendTxt(String sTxt) {
        byte[] bOutArray = sTxt.getBytes();
        send(bOutArray);
    }

    private class ReadThread
            extends Thread {
        private ReadThread() {
        }

        public void run() {
            super.run();
            while (!isInterrupted()) {
                try {
                    if (SerialHelper.this.mInputStream == null) {
                        return;
                    }
                    int available = SerialHelper.this.mInputStream.available();

                    if (available > 0) {
                        byte[] buffer = new byte['?'];
                        int size = SerialHelper.this.mInputStream.read(buffer);
                        if (size > 0) {
                            ComBean ComRecData = new ComBean(SerialHelper.this.sPort, buffer, size);
                            SerialHelper.this.onDataReceived(ComRecData);
                        }
                    } else {
                        SystemClock.sleep(50);
                    }
                } catch (Throwable e) {
                    Log.e("error", e.getMessage());
                    return;
                }
            }
        }
    }

    private class SendThread
            extends Thread {
        public boolean suspendFlag = true;

        private SendThread() {
        }

        public void run() {
            super.run();
            while (!isInterrupted()) {
                synchronized (this) {
                    while (this.suspendFlag) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                SerialHelper.this.send(SerialHelper.this.getbLoopData());
                try {
                    Thread.sleep(SerialHelper.this.iDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void setSuspendFlag() {
            this.suspendFlag = true;
        }

        public synchronized void setResume() {
            this.suspendFlag = false;
            notify();
        }
    }

    public int getBaudRate() {
        return this.iBaudRate;
    }

    public boolean setBaudRate(int iBaud) {
        if (this._isOpen) {
            return false;
        }
        this.iBaudRate = iBaud;
        return true;
    }

    public boolean setBaudRate(String sBaud) {
        int iBaud = Integer.parseInt(sBaud);
        return setBaudRate(iBaud);
    }

    public String getPort() {
        return this.sPort;
    }

    public boolean setPort(String sPort) {
        if (this._isOpen) {
            return false;
        }
        this.sPort = sPort;
        return true;
    }

    public boolean isOpen() {
        return this._isOpen;
    }

    public byte[] getbLoopData() {
        return this._bLoopData;
    }

    public void setbLoopData(byte[] bLoopData) {
        this._bLoopData = bLoopData;
    }

    public void setTxtLoopData(String sTxt) {
        this._bLoopData = sTxt.getBytes();
    }

    public void setHexLoopData(String sHex) {
        this._bLoopData = ByteUtil.HexToByteArr(sHex);
    }

    public int getiDelay() {
        return this.iDelay;
    }

    public void setiDelay(int iDelay) {
        this.iDelay = iDelay;
    }

    public void startSend() {
        if (this.mSendThread != null) {
            this.mSendThread.setResume();
        }
    }

    public void stopSend() {
        if (this.mSendThread != null) {
            this.mSendThread.setSuspendFlag();
        }
    }

    protected abstract void onDataReceived(ComBean paramComBean);
}
