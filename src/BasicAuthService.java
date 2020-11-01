import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.ResultSet;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

// import java.util.Optional; - у меня не добавилась такая, не пойму нужна ли...


public class BasicAuthService implements AuthService {

    private Connection connection;

    public BasicAuthService(Connection connection) {
        this.connection = connection;
    }

    /*private final Set<Record> recordSet;

    public BasicAuthService() {
        recordSet = new HashSet<>();
        recordSet.add(new Record(1L, "BB", "l1", "p1"));
        recordSet.add(new Record(2L, "KK", "l2", "p2"));
        recordSet.add(new Record(3L, "NN", "l3", "p3"));
        recordSet.add(new Record(4L, "LL", "l4", "p4"));
    }*/

    @Override
    public Record findRecord(String login, String password) {
        /*for (Record rec:recordSet) {
            if (rec.getLogin().equals(login) && rec.getPassword().equals(password)) {
                return rec;
            }

        }*/
        PreparedStatement statement = null;
        try {
            statement = (PreparedStatement) connection.prepareStatement("SELECT id, name FROM record WHERE login = ? and passW = ?");

            statement.setString(1, login);
            statement.setString(2, password);
            statement.execute();
            ResultSet resultSet = (ResultSet) statement.getResultSet();
            resultSet.first(); /// так и не понял почему без этой строки не работает, ведь вытаскивается из БД итак всего одна строка
            int id = resultSet.getInt(1);
            String name = resultSet.getString(2);
            Record rec = new Record(id, name, login, password);
            System.out.println(rec);
            return rec;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return null;
    }
}
