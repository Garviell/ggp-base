package org.ggp.base.util.unityupdate;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.util.http.HttpReader;
import org.ggp.base.util.http.HttpWriter;

public final class Update extends Thread {
    private final int port;
    private final Gamer gamer;
    private ServerSocket listener;

    public Update(int port, Gamer gamer) throws IOException{
        System.out.println("Unity update constructor");
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
        System.out.println("Starting up the update listener");
        try {
            Socket connection = listener.accept();
            String in = HttpReader.readAsServer(connection);
            if (in.length() == 0) {
                throw new IOException("Empty message received.");
            }
            while (listener != null) {
                HttpWriter.writeAsServer(connection, "darude");
                sleep(200);
            }
            connection.close();
        } catch (Exception e) {
            System.out.println("Well something went wrong");
        }
    }
}
