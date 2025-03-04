package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RabbitMQService {

    private static final String QUEUE_NAME = "BO1_to_HO";

    public static void main(String[] argv) throws Exception {
        // RabbitMQ connection
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection rabbitConnection = factory.newConnection();
        Channel channel = rabbitConnection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        // Callback to handle incoming messages
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Received '" + message + "'");

            // Database connection details for HO
            String url = "jdbc:mysql://localhost:3306/HO"; // Replace with your HO database name
            String user = "guest"; // Replace with your MySQL username
            String password = "110203"; // Replace with your MySQL password

            try (java.sql.Connection dbConnection = DriverManager.getConnection(url, user, password)) {
                // Parse the message (assuming it's in JSON-like format)
                String[] parts = message.replace("{", "").replace("}", "").split(",");
                int id = Integer.parseInt(parts[0].split(":")[1].trim());
                String date = parts[1].split(":")[1].replace("\"", "").trim();
                String region = parts[2].split(":")[1].replace("\"", "").trim();
                String product = parts[3].split(":")[1].replace("\"", "").trim();
                int qty = Integer.parseInt(parts[4].split(":")[1].trim());
                double cost = Double.parseDouble(parts[5].split(":")[1].trim());
                double amt = Double.parseDouble(parts[6].split(":")[1].trim());
                double tax = Double.parseDouble(parts[7].split(":")[1].trim());
                double total = Double.parseDouble(parts[8].split(":")[1].trim());

                // Insert the data into the HO database
                String sql = "INSERT INTO product_sales (id, date, region, product, qty, cost, amt, tax, total) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pst = dbConnection.prepareStatement(sql)) {
                    pst.setInt(1, id);
                    pst.setString(2, date);
                    pst.setString(3, region);
                    pst.setString(4, product);
                    pst.setInt(5, qty);
                    pst.setDouble(6, cost);
                    pst.setDouble(7, amt);
                    pst.setDouble(8, tax);
                    pst.setDouble(9, total);
                    pst.executeUpdate();
                    System.out.println(" [x] Inserted data into HO database");
                }
            } catch (Exception ex) {
                Logger lgr = Logger.getLogger(RabbitMQService.class.getName());
                lgr.log(Level.SEVERE, "Error processing message: " + message, ex);
            }
        };

        // Start consuming messages from the queue
        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});
    }
}