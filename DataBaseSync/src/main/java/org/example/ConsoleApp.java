package org.example;

import com.rabbitmq.client.*;
import com.rabbitmq.client.Connection;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConsoleApp {

    private static final String QUEUE_NAME = "BO_to_HO";

    private static final String DB_USER = "user";
    private static final String DB_PASSWORD = "password";

    private static final String BO1_DB_URL = "jdbc:mysql://localhost:3307/sales_db";
    private static final String BO2_DB_URL = "jdbc:mysql://localhost:3308/sales_db";
    private static final String HO_DB_URL  = "jdbc:mysql://localhost:3306/sales_db";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.println("Main Menu:");
            System.out.println("1. BO");
            System.out.println("2. HO");
            System.out.println("3. Exit");
            System.out.print("Enter your choice: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    System.out.println("Select a branch office to produce data:");
                    System.out.println("1. BO1");
                    System.out.println("2. BO2");
                    System.out.print("Enter your choice: ");
                    String boChoice = scanner.nextLine();

                    String dbUrl;
                    String boName;

                    if ("1".equals(boChoice)) {
                        dbUrl = BO1_DB_URL;
                        boName = "BO1";
                    } else if ("2".equals(boChoice)) {
                        dbUrl = BO2_DB_URL;
                        boName = "BO2";
                    } else {
                        System.out.println("Invalid input. Returning to main menu...");
                        continue;
                    }

                    runProducer(dbUrl, boName, scanner);
                    break;

                case "2":
                    runConsumer();
                    break;

                case "3":
                    exit = true;
                    System.out.println("Exiting program. Goodbye!");
                    break;

                default:
                    System.out.println("Invalid choice. Please enter 1, 2, or 3.");
                    break;
            }
        }
        scanner.close();
    }

    private static void runProducer(String dbUrl, String boName, Scanner scanner) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (java.sql.Connection dbConnection = DriverManager.getConnection(dbUrl, DB_USER, DB_PASSWORD);
             Connection rabbitConnection = factory.newConnection();
             Channel channel = rabbitConnection.createChannel()) {

            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            System.out.println(boName + " Producer started. Type 'exit' at any prompt to quit.");

            while (true) {
                String id = UUID.randomUUID().toString();
                String date = java.time.LocalDate.now().toString();

                System.out.print("Enter product: ");
                String product = scanner.nextLine();
                if (product.equalsIgnoreCase("exit")) break;

                System.out.print("Enter quantity: ");
                String qtyStr = scanner.nextLine();
                if (qtyStr.equalsIgnoreCase("exit")) break;
                int qty = Integer.parseInt(qtyStr);

                System.out.print("Enter cost: ");
                String costStr = scanner.nextLine();
                if (costStr.equalsIgnoreCase("exit")) break;
                double cost = Double.parseDouble(costStr);

                System.out.print("Enter tax: ");
                String taxStr = scanner.nextLine();
                if (taxStr.equalsIgnoreCase("exit")) break;
                double tax = Double.parseDouble(taxStr);

                double amount = cost * qty;
                double total = amount + tax;

                String insertSql = "INSERT INTO sales (id, date, product, qty, cost, amt, tax, total) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pst = dbConnection.prepareStatement(insertSql)) {
                    insertToDatabase(id, date, product, qty, cost, tax, amount, total, pst);
                    System.out.println("Sale inserted into local database.");
                } catch (SQLException e) {
                    System.err.println("Error inserting into DB: " + e.getMessage());
                }

                String message = String.format(
                        "{\"id\": \"%s\", \"date\": \"%s\", \"product\": \"%s\", \"qty\": %d, \"cost\": %.2f, \"amt\": %.2f, \"tax\": %.2f, \"total\": %.2f, \"source\": \"%s\"}",
                        id, date, product, qty, cost, amount, tax, total, boName
                );

                channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
                System.out.println("Sent message: " + message);
            }

        } catch (Exception ex) {
            Logger.getLogger(ConsoleApp.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private static void insertToDatabase(String id, String date, String product, int qty, double cost, double tax, double amount, double total, PreparedStatement pst) throws SQLException {
        pst.setString(1, id);
        pst.setString(2, date);
        pst.setString(3, product);
        pst.setInt(4, qty);
        pst.setDouble(5, cost);
        pst.setDouble(6, amount);
        pst.setDouble(7, tax);
        pst.setDouble(8, total);
        pst.executeUpdate();
    }

    private static void insertToHODatabase(String id, String date, String product, int qty, double cost, double tax, double amount, double total, String source, PreparedStatement pst) throws SQLException {
        pst.setString(1, id);
        pst.setString(2, date);
        pst.setString(3, product);
        pst.setInt(4, qty);
        pst.setDouble(5, cost);
        pst.setDouble(6, amount);
        pst.setDouble(7, tax);
        pst.setDouble(8, total);
        pst.setString(9, source);
        pst.executeUpdate();
    }

    private static void runConsumer() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try {
            Connection rabbitConnection = factory.newConnection();
            Channel channel = rabbitConnection.createChannel();
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            System.out.println("HO Consumer started. Waiting for messages...");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("Received message: " + message);

                try {
                    String content = message.substring(1, message.length() - 1); // remove { }
                    String[] parts = content.split(",\\s*");

                    String id = null;
                    String date = "";
                    String product = "";
                    int qty = 0;
                    double cost = 0.0, amt = 0.0, tax = 0.0, total = 0.0;
                    String source = "";

                    for (String part : parts) {
                        String[] keyValue = part.split(":", 2);
                        String key = keyValue[0].replaceAll("\"", "").trim();
                        String value = keyValue[1].replaceAll("\"", "").trim();

                        switch (key) {
                            case "id":
                                id = value;
                                break;
                            case "date":
                                date = value;
                                break;
                            case "product":
                                product = value;
                                break;
                            case "qty":
                                qty = Integer.parseInt(value);
                                break;
                            case "cost":
                                cost = Double.parseDouble(value);
                                break;
                            case "amt":
                                amt = Double.parseDouble(value);
                                break;
                            case "tax":
                                tax = Double.parseDouble(value);
                                break;
                            case "total":
                                total = Double.parseDouble(value);
                                break;
                            case "source":
                                source = value;
                                break;
                        }
                    }

                    try (java.sql.Connection hoDbConnection = DriverManager.getConnection(HO_DB_URL, DB_USER, DB_PASSWORD)) {
                        String insertSql = "INSERT INTO sales (id, date, product, qty, cost, amt, tax, total, source) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement pst = hoDbConnection.prepareStatement(insertSql)) {
                            insertToHODatabase(id, date, product, qty, cost, tax, amt, total, source, pst);
                            System.out.println("Inserted sale into HO database.");
                        }
                    } catch (SQLException e) {
                        System.err.println("Error inserting into HO DB: " + e.getMessage());
                    }
                } catch (Exception e) {
                    System.err.println("Error processing message: " + e.getMessage());
                }
            };

            channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});

        } catch (Exception ex) {
            Logger.getLogger(ConsoleApp.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}
