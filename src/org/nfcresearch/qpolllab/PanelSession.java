package org.nfcresearch.qpolllab;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.SystemClock;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import org.json.JSONObject;

/** Android ISO-DEP transport; link segmentation and WTX remain in the platform stack. */
public final class PanelSession {
    public interface Events {void emit(String kind,String message);}
    public static void run(Tag tag,String mode,File jobFile,File stopFile,Events events) throws Exception {
        byte[] config=read(jobFile,8192);JSONObject job=new JSONObject(new String(config,"UTF-8"));
        byte[] image=null;
        if(mode.equals("refresh")) {
            File data=new File(job.getString("imageFile")).getCanonicalFile();
            if(!data.getParentFile().equals(jobFile.getCanonicalFile().getParentFile()))throw new IllegalArgumentException("图像快照路径错误");
            image=read(data,PanelProtocol.IMAGE_BYTES);
            if(image.length!=PanelProtocol.IMAGE_BYTES||!PanelProtocol.toHex(MessageDigest.getInstance("SHA-256").digest(image)).equalsIgnoreCase(job.getString("sha256")))
                throw new IllegalArgumentException("预览与图片快照校验不符，未发送 APDU");
        }
        final long deadline=SystemClock.elapsedRealtime()+(mode.equals("refresh")?180000:15000);
        PanelProtocol.Control control=new PanelProtocol.Control() {
            public long now(){return SystemClock.elapsedRealtime();}
            public void sleep(long ms)throws Exception{Thread.sleep(ms);}
            public void check()throws Exception {
                if(stopFile.exists())throw new InterruptedException("已停止；当前屏幕画面需目视确认");
                if(now()>deadline)throw new IllegalStateException("本轮通信超过总预算，未重试写入");
            }
            public void event(String kind,String message){events.emit(kind,message);}
        };
        control.check();IsoDep iso=IsoDep.get(tag);
        if(iso==null)throw new IllegalArgumentException("目标未提供 ISO-DEP，未发送 APDU");
        try {
            iso.connect();
            if(iso.getMaxTransceiveLength()<257)throw new IllegalArgumentException("平台单次 APDU 长度不足 257 字节");
            PanelProtocol protocol=new PanelProtocol((command,timeout)->{
                control.check();int remaining=(int)Math.max(1,deadline-control.now());iso.setTimeout(Math.min(timeout,remaining));
                return iso.transceive(command);
            },control);
            if(mode.equals("identity"))protocol.identify();
            else protocol.write(image,job.getInt("powerMs"),job.optInt("pollMs",PanelProtocol.DEFAULT_POLL_MS));
        } finally {try{iso.close();}catch(Exception e){events.emit("CLOSE_ERROR","关闭 ISO-DEP："+e);}}
    }
    private static byte[] read(File file,int max)throws Exception {
        try(FileInputStream input=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[4096];int n;
            while((n=input.read(buffer))!=-1){if(out.size()+n>max)throw new IllegalArgumentException("文件超出大小上限");out.write(buffer,0,n);}return out.toByteArray();
        }
    }
}
