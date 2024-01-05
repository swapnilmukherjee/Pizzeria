package cpsc4620;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnectionTest {

    public static void main(String[] args) {
        // Define database connection parameters
        String url = "jdbc:mysql://Endpoint/DB_Name"; // Replace with your database URL
        String username = "Username"; // Replace with your database username
        String password = "Password"; // Replace with your database password

        try {
            // Attempt to establish a database connection
            Connection connection = DriverManager.getConnection(url, username, password);

            // If the connection is successful, print a success message
            System.out.println("Connected to the database!");

            // Don't forget to close the connection when done
            connection.close();
        } catch (SQLException e) {
            // Handle any potential database connection errors here
            System.err.println("Database connection error: " + e.getMessage());
        }
    }
}
