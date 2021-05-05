import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Agents {
     /* Checks to see if an agent ID exists */
     public static boolean checkID(int id, Connection con) {
        try {
            if (id != 0) {
                PreparedStatement stmt = con.prepareStatement("SELECT ID FROM AGENTS");
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
            System.out.println("Agent ID list not successfully parsed.");
        }
        return false;
    }

    /* Asks the user for an agent ID and checks if it exists */
    public static int getAndCheckID(Connection con, Scanner scan) {
        int id = 0;
        while (!Agents.checkID(id, con)) {
            try {
                System.out.println("What is your ID? Press 0 to abort.");
                id = scan.nextInt();
                scan.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Please input numbers only");
                scan.next();
            }
            if (!Agents.checkID(id, con)) {
                if(id == 0) {
                    break;
                }
                System.out.println("This is not a valid agent ID. Please input another.");
            }
        }
        return id;
    }

    /* Checks overdue bills in the system */
    public static void checkOverdueBills(Connection con) {
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT DISTINCT ID, POLICY_ID, AMOUNT_DUE" +
            " FROM PAYMENTS, POL_PAYMENTS" +
            " WHERE DATE_OF_PAYMENT IS NULL AND POLICY_ID = (SELECT POLICY_ID FROM POL_PAYMENTS WHERE payment_id = payments.id) AND WAS_PAYMENT_MADE = 'N'");
            ResultSet rset = stmt.executeQuery();
            con.commit();

            if (!rset.isBeforeFirst()) {
                System.out.println("No overdue bills in the system.\n");
            } else {
                System.out.println("\nOverdue bills:\n");
                while (rset != null && rset.next()) {
                    System.out.printf("Payment ID: %d\nPolicy ID: %d\nAmount Due: %5.2f\n\n", rset.getInt("ID"), rset.getInt("POLICY_ID"),
                            rset.getFloat("AMOUNT_DUE"));
                }
            }
            stmt.close();
            rset.close();
        } catch(SQLException e) {
            System.out.println("Could not pull overdue bills");
        }
        
    }

    /* Checks pending claims of customers associated with agent ID */
    public static void checkPendingClaims(int agent_id, Connection con) {
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT POLICY_ID, CLAIM_ID, DESCRIPTION_OF_EVENT"
            + " FROM CLAIM_POLICY_PAYMENT INNER JOIN CLAIMS"
            + " ON CLAIM_POLICY_PAYMENT.CLAIM_ID = CLAIMS.ID"
            + " WHERE CLAIM_PAYMENT_ID IS NULL AND POLICY_ID IN"
            + " (SELECT POLICY_ID FROM CUST_POLICY WHERE CUST_ID IN (SELECT CUST_ID FROM CUSTOMER_SERVICE WHERE AGENT_ID = ?))");
            stmt.setInt(1, agent_id);
            ResultSet rset = stmt.executeQuery();
            con.commit();

            if (!rset.isBeforeFirst()) {
                System.out.println("\nNo pending claims in the system pertaining to your customers.\n");
            } else {
                System.out.println("\nOverdue bills connected to your customers:\n");
                while (rset != null && rset.next()) {
                    System.out.printf("Policy ID: %d\nClaim ID: %d\nDescription of event: %s\n\n", rset.getInt("POLICY_ID"), rset.getInt("CLAIM_ID"),
                            rset.getString("DESCRIPTION_OF_EVENT"));
                }
            }
            stmt.close();
            rset.close();
        } catch(SQLException e) {
            System.out.println("Could not pull overdue bills");
            }
    }

    /* Generates revenue associated with agent ID */
    public static void generateRevenue(int agent_id, Connection con) {
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT SUM(AMOUNT_DUE) FROM PAYMENTS"
            + " WHERE WAS_PAYMENT_MADE = 'Y' AND PAYMENTS.ID IN"
            + " (SELECT PAYMENT_ID FROM POL_PAYMENTS WHERE POLICY_ID IN"
            + " (SELECT POLICY_ID FROM CUST_POLICY WHERE CUST_ID IN"
            + " (SELECT CUST_ID FROM CUSTOMER_SERVICE WHERE AGENT_ID = ?)))");
            stmt.setInt(1, agent_id);
            ResultSet rset = stmt.executeQuery();
            con.commit();

            System.out.println("\nRevenue generated by agent:\n");
            while (rset != null && rset.next()) {
                System.out.printf("%5.2f", rset.getFloat("SUM(AMOUNT_DUE)"));
            }
            stmt.close();
            rset.close();
        } catch(SQLException e) {
            System.out.println("Could not pull revenue generated by agent");
        }
    }

    /* Lists all customers associated with agent ID */
    public static void listCustomers(int agent_id, Connection con) {
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT ID, NAME, ADDRESS, PHONE_NUMBER, CREDIT_SCORE, AGE FROM CUSTOMERS WHERE ID IN (SELECT CUST_ID FROM CUSTOMER_SERVICE WHERE AGENT_ID = ?)");
            stmt.setInt(1, agent_id);
            ResultSet rset = stmt.executeQuery();
            con.commit();

            if (!rset.isBeforeFirst()) {
                System.out.println("\nNo customers associated with agent.\n");
            } else {
                System.out.println("\nCustomers associated with agent:\n");
                while (rset != null && rset.next()) {
                    System.out.printf("Customer ID: %d\nName: %s\nAddress: %s\nPhone Number: %s\nCredit Score: %d\nAge: %d\n\n", rset.getInt("ID"), rset.getString("NAME"), rset.getString("ADDRESS"), rset.getString("PHONE_NUMBER"), rset.getInt("CREDIT_SCORE"), rset.getInt("AGE"));
                }
            }
            stmt.close();
            rset.close();
        } catch(SQLException e) {
            System.out.println("Could not list associated customers.");
        }
    }
}
