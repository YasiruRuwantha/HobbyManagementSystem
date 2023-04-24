import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;

public class HobbyManager {
    private static Connection connection;
    private static boolean loggedIn = false;

    public static void main(String[] args) throws SQLException {
        connectToDatabase();
        Scanner scanner = new Scanner(System.in);

        login(scanner);

        while (loggedIn) {
            System.out.println("Please select an option:");
            System.out.println("1. Add new user");
            System.out.println("2. Add new hobby");
            System.out.println("3. Assign hobbies to user");
            System.out.println("4. View all user list");
            System.out.println("5. Filters the list of users based on a hobby name:");
            System.out.println("6. View user's hobbies");
            System.out.println("7. Logout");

            int choice = scanner.nextInt();
            scanner.nextLine();

                  switch (choice) {
                case 1:
                    addUser(scanner);
                    break;
                case 2:
                    addHobby(scanner);
                    break;
                case 3:
                    assignHobbies(scanner);
                    break;
                case 4:
                    listAllUsers();
                    break;
                case 5:
                    listUsersByHobby(scanner);
                    break;
                case 6:
                    viewHobbies(scanner);
                    break;
                case 7:
                    loggedIn = false;
                    System.out.println("Logged out.");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
                    break;
            }
        }
    }

    private static void connectToDatabase() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/hobby_manager";
        String user = "root";
        String password = "";
        connection = DriverManager.getConnection(url, user, password);
    }

    private static void disconnectFromDatabase() throws SQLException {
        connection.close();
    }

    private static void login(Scanner scanner) throws SQLException {
        System.out.println("Please enter your email:");
        String email = scanner.nextLine();

        System.out.println("Please enter your password:");
        String password = scanner.nextLine();

        PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE email = ? AND password = ?");
        statement.setString(1, email);
        statement.setString(2, password);
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            System.out.println("Login successful!");
            loggedIn = true;
        } else {
            System.out.println("Invalid email or password. Please try again.");
        }
    }

    private static void addUser(Scanner scanner) throws SQLException {
        System.out.println("Please enter the user's name:");
        String name = scanner.nextLine();

        System.out.println("Please enter the user's email:");
        String email = scanner.nextLine();

        System.out.println("Please enter the user's password:");
        String password = scanner.nextLine();

        PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM users WHERE email = ?");
        selectStatement.setString(1, email);
        ResultSet resultSet = selectStatement.executeQuery();

        if (resultSet.next()) {
            System.out.println("User with this email already exists in the database!");
        } else {
            PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO users (name, email, password) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            insertStatement.setString(1, name);
            insertStatement.setString(2, email);
            insertStatement.setString(3, password);
            insertStatement.executeUpdate();

            ResultSet generatedKeys = insertStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                int userId = generatedKeys.getInt(1);
                boolean addAnotherNumber = true;

                while (addAnotherNumber) {
                    System.out.println("Please enter a name for the phone number:");
                    String phoneName = scanner.nextLine();

                    System.out.println("Please enter the phone number:");
                    String phoneNumber = scanner.nextLine();

                    PreparedStatement phoneStatement = connection.prepareStatement("INSERT INTO phone_numbers (user_id, phone_name, phone_number) VALUES (?, ?, ?)");
                    phoneStatement.setInt(1, userId);
                    phoneStatement.setString(2, phoneName);
                    phoneStatement.setString(3, phoneNumber);
                    phoneStatement.executeUpdate();

                    System.out.println("Phone number added successfully!");

                    System.out.println("Do you want to add another phone number? (Y/N)");
                    String answer = scanner.nextLine();
                    if (!answer.equalsIgnoreCase("Y")) {
                        addAnotherNumber = false;
                    }
                }

                System.out.println("User added successfully!");
            }
        }
    }

    private static void addHobby(Scanner scanner) throws SQLException {
        System.out.println("Please enter the hobby name:");
        String name = scanner.nextLine();

        PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM hobbies WHERE name = ?");
        selectStatement.setString(1, name);
        ResultSet resultSet = selectStatement.executeQuery();

        if (resultSet.next()) {
            System.out.println("Hobby already exists!");
            return;
        }

        PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO hobbies (name) VALUES (?)");
        insertStatement.setString(1, name);
        insertStatement.executeUpdate();

        System.out.println("Hobby added successfully!");
    }

    private static void assignHobbies(Scanner scanner) throws SQLException {
        System.out.println("Please enter the user's email:");
        String email = scanner.nextLine();

        PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE email = ?");
        statement.setString(1, email);
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            int userId = resultSet.getInt("id");
            ArrayList<Hobby> hobbies = getHobbies();
            for (int i = 0; i < hobbies.size(); i++) {
                System.out.println((i + 1) + ". " + hobbies.get(i).getName());
            }

            System.out.println("Please enter the hobby number(s) to assign (separated by commas):");
            String[] hobbyNumbers = scanner.nextLine().split(",");

            for (String hobbyNumber : hobbyNumbers) {
                int index = Integer.parseInt(hobbyNumber.trim()) - 1;
                int hobbyId = hobbies.get(index).getId();

                // Check if the hobby is already assigned to the user
                statement = connection.prepareStatement("SELECT * FROM user_hobbies WHERE user_id = ? AND hobby_id = ?");
                statement.setInt(1, userId);
                statement.setInt(2, hobbyId);
                ResultSet result = statement.executeQuery();
                if (result.next()) {
                    System.out.println(hobbies.get(index).getName() + " is already assigned to this user.");
                } else {
                    statement = connection.prepareStatement("INSERT INTO user_hobbies (user_id, hobby_id) VALUES (?, ?)");
                    statement.setInt(1, userId);
                    statement.setInt(2, hobbyId);
                    statement.executeUpdate();
                }
            }

            System.out.println("Hobbies assigned successfully!");
        } else {
            System.out.println("User not found. Please try again.");
        }
    }

    private static void listUsersByHobby(Scanner scanner) throws SQLException {
        System.out.println("Please enter the hobby name to filter users by (leave blank for all users):");
        String hobbyName = scanner.nextLine().trim();

        String query = "SELECT u.name, u.email, h.name as hobby " +
                "FROM users u " +
                "LEFT JOIN user_hobbies uh ON u.id = uh.user_id " +
                "LEFT JOIN hobbies h ON uh.hobby_id = h.id ";
        if (!hobbyName.isEmpty()) {
            query += "WHERE h.name = ?";
        }

        PreparedStatement statement = connection.prepareStatement(query);
        if (!hobbyName.isEmpty()) {
            statement.setString(1, hobbyName);
        }
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            String userName = resultSet.getString("name");
            String userEmail = resultSet.getString("email");
            String userHobby = resultSet.getString("hobby");

            System.out.println(userName + " (" + userEmail + ") - " + userHobby);
        }
    }


    private static void listAllUsers() throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM users");
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            int userId = resultSet.getInt("id");
            String name = resultSet.getString("name");
            String email = resultSet.getString("email");

            System.out.println("User ID: " + userId);
            System.out.println("Name: " + name);
            System.out.println("Email: " + email);

            PreparedStatement phoneStatement = connection.prepareStatement("SELECT * FROM phone_numbers WHERE user_id = ?");
            phoneStatement.setInt(1, userId);
            ResultSet phoneResultSet = phoneStatement.executeQuery();

            if (phoneResultSet.next()) {
                System.out.println("Phone numbers:");
                do {
                    String phoneName = phoneResultSet.getString("phone_name");
                    String phoneNumber = phoneResultSet.getString("phone_number");
                    System.out.println("- " + phoneName + ": " + phoneNumber);
                } while (phoneResultSet.next());
            }

            PreparedStatement hobbyStatement = connection.prepareStatement("SELECT * FROM hobbies h INNER JOIN user_hobbies uh ON h.id = uh.hobby_id WHERE uh.user_id = ?");
            hobbyStatement.setInt(1, userId);
            ResultSet hobbyResultSet = hobbyStatement.executeQuery();

            if (hobbyResultSet.next()) {
                System.out.println("Hobbies:");
                do {
                    String hobbyName = hobbyResultSet.getString("name");
                    System.out.println("- " + hobbyName);
                } while (hobbyResultSet.next());
            }

            System.out.println();
        }
    }




    private static void viewHobbies(Scanner scanner) throws SQLException {
        System.out.println("Please enter the user's email:");
        String email = scanner.nextLine();

        PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE email = ?");
        statement.setString(1, email);
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            int userId = resultSet.getInt("id");
            ArrayList<Hobby> hobbies = getHobbiesForUser(userId);

            if (hobbies.size() > 0) {
                System.out.println("Hobbies for " + resultSet.getString("name") + ":");
                for (Hobby hobby : hobbies) {
                    System.out.println("- " + hobby.getName());
                }
            } else {
                System.out.println("No hobbies found for user.");
            }
        } else {
            System.out.println("User not found. Please try again.");
        }
    }

    private static ArrayList<Hobby> getHobbies() throws SQLException {
        ArrayList<Hobby> hobbies = new ArrayList<>();

        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM hobbies");

        while (resultSet.next()) {
            Hobby hobby = new Hobby(resultSet.getInt("id"), resultSet.getString("name"));
            hobbies.add(hobby);
        }

        return hobbies;
    }

    private static ArrayList<Hobby> getHobbiesForUser(int userId) throws SQLException {
        ArrayList<Hobby> hobbies = new ArrayList<>();

        PreparedStatement statement = connection.prepareStatement("SELECT * FROM user_hobbies WHERE user_id = ?");
        statement.setInt(1, userId);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            int hobbyId = resultSet.getInt("hobby_id");

            statement = connection.prepareStatement("SELECT * FROM hobbies WHERE id = ?");
            statement.setInt(1, hobbyId);
            ResultSet hobbyResult = statement.executeQuery();

            if (hobbyResult.next()) {
                Hobby hobby = new Hobby(hobbyResult.getInt("id"), hobbyResult.getString("name"));
                hobbies.add(hobby);
            }
        }

        return hobbies;
    }


}