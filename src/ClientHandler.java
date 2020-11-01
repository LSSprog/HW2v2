import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Objects;

public class ClientHandler {
    private AuthService.Record record;
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        doAuth();
                        readMsg();
                    } catch (IOException | SQLException e) {
                        e.printStackTrace();
                    } finally {
                        closeConnection();
                    }
                }
            })
                    .start();


        } catch (IOException e) {
            throw new RuntimeException("Client handler was not created");
        }
    }
    public AuthService.Record getRecord() {
        return record;
    }

    public void doAuth() throws IOException, SQLException {
        while (true) {
            System.out.println("Waiting do Auth");
            String message = in.readUTF();
            if (message.startsWith("/auth")) {
                String[] words = message.split("\\s");
                AuthService.Record posibleRecord = server.getAuthService().findRecord(words[1], words[2]);
                if (posibleRecord != null) {
                    if (!server.isBusy(posibleRecord)){
                        record = posibleRecord;
                        sendMsg("/AuthOK for " +record.getName());
                        server.broadcastMessage("Logged-in: " + record.getName());
                        server.subscribe(this);
                        break;
                    } else {
                        sendMsg(String.format("Current user [%s] is already occupied", posibleRecord.getName()));
                    }
                } else {
                    sendMsg(String.format("User no found"));
                }
            }
        }

    }

    public void sendMsg (String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readMsg () {
        while (true){
            String message = null;
            try {
                message = in.readUTF();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(String.format("Пришло сообщение от %s: %s", record.getName(), message));
            if (message.equals("/end")) {
                sendMsg("/end");
                return;
            }
            if (message.startsWith("/w")) {
                String[] words = message.split("\\s");
                String name = words[1];
                message = message.substring(3, message.length());
                server.personalMessage(String.format("%s: %s",record.getName(), message), record.getName(), name);

            } else {
                server.broadcastMessage(String.format("%s: %s",record.getName(),message));
            }

        }
    }

    public void closeConnection() {
        server.unsubscribe(this);
        server.broadcastMessage(record.getName() + "left chat");
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientHandler that = (ClientHandler) o;
        return record.equals(that.record) &&
                server.equals(that.server) &&
                socket.equals(that.socket) &&
                in.equals(that.in) &&
                out.equals(that.out);
    }

    @Override
    public int hashCode() {
        return Objects.hash(record, server, socket, in, out);
    }
}
