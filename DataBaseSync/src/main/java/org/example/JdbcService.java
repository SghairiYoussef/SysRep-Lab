package org.example;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JdbcService {

    private static final String QUEUE_NAME = "BO1_to_HO"; // Queue name for RabbitMQ

    public static void main(String[] args) {
        // Database connection details
        String url = "jdbc:mysql://localhost:3307/sales_db"; // Replace with your BO1 database name
        String user = "user"; // Replace with your MySQL username
        String password = "password"; // Replace with your MySQL password
        String query = "SELECT * FROM sales"; // Query to fetch data

        // RabbitMQ connection
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost"); // RabbitMQ server host

        try (java.sql.Connection dbConnection = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = dbConnection.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            // Connect to RabbitMQ
            try (Connection rabbitConnection = factory.newConnection();
                 Channel channel = rabbitConnection.createChannel()) {

                // Declare the queue
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);

                // Process each row in the result set
                while (rs.next()) {
                    // Create a JSON-like string from the row data
                    String message = String.format(
                            "{\"id\": %d, \"date\": \"%s\", \"region\": \"%s\", \"product\": \"%s\", \"qty\": %d, \"cost\": %.2f, \"amt\": %.2f, \"tax\": %.2f, \"total\": %.2f}",
                            rs.getInt("id"),
                            rs.getDate("date"),
                            rs.getString("region"),
                            rs.getString("product"),
                            rs.getInt("qty"),
                            rs.getDouble("cost"),
                            rs.getDouble("amt"),
                            rs.getDouble("tax"),
                            rs.getDouble("total")
                    );

                    // Send the message to RabbitMQ
                    channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
                    System.out.println(" [x] Sent '" + message + "'");
                }
            }
        } catch (Exception ex) {
            Logger lgr = Logger.getLogger(JdbcService.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}