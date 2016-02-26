package org.ggp.base.util.unityupdate;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.ggp.base.player.gamer.statemachine.unity.UnityGamer;
import org.ggp.base.util.http.HttpReader;
import org.ggp.base.util.http.HttpWriter;

public final class Update extends Thread {
    private final int port;
    private final UnityGamer gamer;
    private ServerSocket listener;

    public Update(int port, UnityGamer gamer) throws IOException{
        System.out.println("Unity update constructor on port: " + port);
        while(listener == null) {
            try {
                listener = new ServerSocket(port, 0, InetAddress.getByName(null));
            } catch (IOException ex) {
                listener = null;
                port++;
                System.err.println("Failed to start gamer on port: " +
                        (port-1) + " trying port " + port);
            }
        }
        this.port = port;
        this.gamer = gamer;
    }

    public final int getUpdatePort(){
        return port;
    }

    public void shutdown() {
        try {
            listener.close();
            listener = null;
        } catch (IOException e) {
            ;
        }
    }

    @Override
    public void run(){
        System.out.println("Update Listener ready to recieve connections");
        try {
            // if (in.length() == 0) {
            //     throw new IOException("Empty message received.");
            // }
            Socket connection = listener.accept();
            String in = HttpReader.readAsServer(connection);
            System.out.println(in);
            PrintWriter pw = new PrintWriter(connection.getOutputStream(), true);
            while (listener != null) {
                pw.println(gamer.getEvaluation());
                sleep(2000);
            }
            connection.close();
        } catch (Exception e) {
            System.out.println("Well something went wrong");
        }
    }
}
