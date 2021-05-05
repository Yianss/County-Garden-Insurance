import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Customer {
    public Customer() {
    }

    /* Checks to see if a customer ID exists */
    public static boolean checkID(int id, Connection con) {
        try {
            if (id != 0) {
                PreparedStatement stmt = con.prepareStatement("SELECT ID FROM CUSTOMERS");
                ResultSet rset = stmt.executeQuery();
                con.commit();
                while (rset.next() && rset != null) {
                    if (rset.getInt("ID") == id) {
                        return true;
                    }
                }
                rset.close();

            }
        } catch (SQLException e) {
            System.out.println("Customer ID list not successfully parsed.");
        }
        return false;
    }

    /* Asks the user for a customer ID and checks if it exists */
    public static int getAndCheckID(Connection con, Scanner scan) {
        int id = 0;
        while (!Customer.checkID(id, con)) {
            try {
                System.out.println("What is your ID? Press 0 to abort.");
                id = scan.nextInt();
                scan.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Please input numbers only");
                scan.next();
            }
            if (!Customer.checkID(id, con)) {
                if(id == 0) {
                    break;
                }
                System.out.println("This is not a valid customer ID. Please input another.");
            }
        }
        return id;
    }

    /* Creates a new Customer ID */
    public static int getNewID(Connection con) {
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT MAX(ID)+1 FROM CUSTOMERS");
            ResultSet rset = stmt.executeQuery();
            con.commit();
            rset.next();
            int val = rset.getInt(1);
            stmt.close();
            rset.close();
            return val;
        } catch (SQLException e) {
            System.out.println("Unable to fetch new customer ID.");
        }
        return -1;
    }

    /* Asks the user for all the information to create a new Customer and creates it*/
    public static void createNewCustomer(Connection con, Scanner scan) {
        String cust_name = "";
        String cust_address = "";
        String cust_phone = "";
        int cust_credit_score = 0;
        int cust_age = 0;

        int format_test = 0;
        while (format_test == 0) {
            scan.nextLine();
            System.out.println(
                    "\nWelcome new customer!\nPlease input your first and last name on the line below (maximum 50 characters):");
            cust_name = scan.nextLine();
            if (cust_name.length() > 50) {
                System.out.println("Name can only be 50 characters maximum.");
            } else
                format_test = 1;
        }
        format_test = 0;
        while (format_test == 0) {
            System.out.println("\nPlease input your address on the line below (maximum 50 characters):");
            cust_address = scan.nextLine();
            if (cust_address.length() > 50) {
                System.out.println("Address can only be 50 characters maximum.");
            } else
                format_test = 1;
        }
        format_test = 0;
        while (format_test == 0) {
            System.out.println("\nPlease input your phone number on the line below (maximum 10 characters):");
            cust_phone = scan.nextLine();
            if (cust_phone.length() > 10) {
                System.out.println("Phone number can only be 10 characters maximum.");
            } else
                format_test = 1;

            for (int i = 0; i < cust_phone.length(); i++) {
                if (!Character.isDigit(cust_phone.charAt(i))) {
                    System.out.println("Phone number contains non digits.");
                    format_test = 0;
                }
            }
        }
        format_test = 0;
        while (format_test == 0) {
            int inputmismatch = 1;
            while (inputmismatch == 1) {
                try {
                    System.out.println("\nPlease input your credit score on the line below:");
                    cust_credit_score = scan.nextInt();
                    scan.nextLine();
                } catch (InputMismatchException e) {
                    System.out.println("Please input only numbers.");
                    scan.next();
                }
                inputmismatch = 0;
            }
            if (cust_credit_score < 350 || cust_credit_score > 850) {
                System.out.println("Credit score outside of range.");
            } else
                format_test = 1;
        }
        format_test = 0;
        while (format_test == 0) {
            boolean inputmismatch = true;
            while (inputmismatch) {
                try {
                    System.out.println("\nPlease input your age on the line below:");
                    cust_age = scan.nextInt();
                    scan.nextLine();
                } catch (InputMismatchException e) {
                    System.out.println("Please input only numbers.");
                    scan.next();
                }
                inputmismatch = false;
            }
            if (cust_age < 18) {
                System.out.println("Customer is too young.");
            } else
                format_test = 1;
        }

        try {
            PreparedStatement stmt = con.prepareStatement(
                    "INSERT INTO CUSTOMERS (ID, NAME, ADDRESS, PHONE_NUMBER, CREDIT_SCORE, AGE) VALUES (?, ?, ?, ?, ?, ?)");
            int cust_id = Customer.getNewID(con);
            stmt.setInt(1, cust_id);
            stmt.setString(2, cust_name);
            stmt.setString(3, cust_address);
            stmt.setString(4, cust_phone);
            stmt.setInt(5, cust_credit_score);
            stmt.setInt(6, cust_age);
            int update = stmt.executeUpdate();
            stmt.close();

            boolean cust_added = false;
            if (update == 1) {
                System.out.println("\nCustomer added successfully.\n");
                cust_added = true;
            } else {
                System.out.println("\nCustomer was not successfully added.\n");
            }

            if (cust_added) {
                PreparedStatement stmt3 = con.prepareStatement("SELECT ID, NAME FROM AGENTS");
                ResultSet rset2 = stmt3.executeQuery();

                int[] agents = new int[1000]; //Unoptimized
                int runner = 0;

                while (rset2.next()) {
                    System.out.printf("%s, ID: %d\n", rset2.getString("NAME"), rset2.getInt("ID"));
                    agents[runner] = rset2.getInt("ID");
                    runner++;
                }
                System.out.println();
                boolean pickAgent = false;
                int agent_picked = 0;
                while (!pickAgent) {
                    System.out.println("Please select the ID of the agent you would like to have.");
                    try {
                        agent_picked = scan.nextInt();
                        scan.nextLine();
                    } catch (InputMismatchException e) {
                        System.out.println("Please input only numbers");
                        scan.next();
                    }
                    for (int i = 0; i < agents.length; i++) {
                        if (agent_picked == agents[i]) {
                            pickAgent = true;
                            break;
                        }
                    }
                }
                try {
                    stmt3 = con.prepareStatement("INSERT INTO CUSTOMER_SERVICE (CUST_ID, AGENT_ID) VALUES (?,?)");
                    stmt3.setInt(1, cust_id);
                    stmt3.setInt(2, agent_picked);
                    update = stmt3.executeUpdate();
                    con.commit();

                    if (update == 1) {
                        System.out.println("\nAgent successfully linked to customer.\n");
                    } else {
                        System.out.println("\nAgent not successfully linked to customer.\n");
                    }
                } catch (SQLException e) {
                    System.out.println("Agent not successfully linked to customer.");
                }
                stmt3.close();
            }
        } catch (SQLException e) {
            System.out.println("\nCustomer was not successfully added.\n");
        }
    }

}