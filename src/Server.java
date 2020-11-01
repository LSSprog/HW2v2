import com.sun.prism.impl.Disposer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class Server {
    public static final int PORT = 1313;
    private AuthService authService;
    private Set<ClientHandler> clientHandlerSet;

    Connection connection;

    public Server() {
        this(PORT);
    } // не понял что это за строка

    public Server(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);

            dataBaseSqlServer();
            getSQL();

            authService = new BasicAuthService((com.mysql.jdbc.Connection) connection);
            System.out.println("Auth is started up");

            clientHandlerSet = new HashSet<>();

            while (true) {
                System.out.println("Waiting for connection");
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket);
                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AuthService getAuthService() {
        return authService;
    }

    public synchronized boolean isBusy(AuthService.Record record) {
        for (ClientHandler ch : clientHandlerSet) {
            if (ch.getRecord().equals(record)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void subscribe(ClientHandler ch) {
        clientHandlerSet.add(ch);
    }

    public synchronized void unsubscribe(ClientHandler ch) {
        clientHandlerSet.remove(ch);
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler ch : clientHandlerSet) {
            ch.sendMsg(message);
        }
    }

    public void personalMessage(String message, String nameFrom, String nameTo) {
        for (ClientHandler ch : clientHandlerSet) {
            if (ch.getRecord().getName().equals(nameTo) || ch.getRecord().getName().equals(nameFrom)) {
                ch.sendMsg(message);
                /*for (ClientHandler chFrom: clientHandlerSet) {
                    if(ch.getRecord().equals(record)){
                        ch.sendMsg(String.format("%s:",record.getName(), message));
                    }
                }//record.getName();*/
            }
        }
    }

    public void dataBaseSqlServer() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver not found");
        }
        //Connection connection;
        try {
            DriverManager.registerDriver(new com.mysql.jdbc.Driver());
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/nickpaschat", "root", "");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new RuntimeException("Driver Registration error");
        }
    }

    public void getSQL() {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM record");
            List<AuthService.Record> men = new ArrayList<>();
            while (resultSet.next()) {
                men.add(new AuthService.Record(
                                resultSet.getInt("id"),
                                resultSet.getString("name"),
                                resultSet.getString("login"),
                                resultSet.getString("passW")
                        )
                );
            }
            System.out.println(men);

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }
}
