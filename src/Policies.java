import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Policies {
    public Policies() {

    }

    /*
     * Creates a new Policy ID by finding the max policy ID value and incrementing
     * by 1
     */
    public static int getNewID(Connection con) {
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT MAX(ID)+1 FROM POLICIES");
            ResultSet rset = stmt.executeQuery();
            con.commit();
            rset.next();
            int val = rset.getInt(1);
            stmt.close();
            rset.close();
            return val;
        } catch (SQLException e) {
            System.out.println("Unable to fetch new policy ID.");
        }
        return -1;
    }

    /* Checks to see if a policy ID is valid */
    public static boolean checkID(int id, Connection con) {
        try {
            if (id != 0) {
                PreparedStatement stmt = con.prepareStatement("SELECT ID FROM POLICIES");
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
            System.out.println("Policy ID list not successfully parsed.");
        }
        return false;
    }

    /* Checks to see if a policy ID is valid against a list of policies */
    public static boolean checkID(int id, Connection con, int [] valid_policies) {
        if(id == 0) 
                return false;
        for(int i = 0; i < valid_policies.length; i++)
            if(id == valid_policies[i])
                return true;
        return false;
    }

    /* Asks the user for a policy ID, checks if it is valid and exists */
    public static int getAndCheckID(Connection con, Scanner scan) {
        int id = 0;
        while (!Policies.checkID(id, con)) {
            try {
                System.out.println("What is the policy ID? Input 0 to abort.");
                id = scan.nextInt();
                scan.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Please input numbers only");
                scan.next();
            }
            if (!Policies.checkID(id, con)) {
                if (id == 0) {
                    break;
                }
                System.out.println("This is not a valid policy ID. Please input another.");
            }
        }
        return id;
    }

    /* Returns all policies connected to the customer ID given */
    public static ResultSet getCustomerPolicies(int id, Connection con) {
        if (id == 0)
            return null;
        try {
            PreparedStatement stmt = con.prepareStatement(
                    "SELECT ID, POLICY_TYPE AS \"POLICY TYPE\", ACTIVE_POLICY AS \"ACTIVE POLICY\", PRICE, START_DATE AS \"START DATE\", EXPIRATION_DATE AS \"EXPIRATION DATE\""
                            + "FROM POLICIES WHERE ID IN (SELECT POLICY_ID FROM CUST_POLICY WHERE CUST_ID = ?)");
            stmt.setInt(1, id);
            ResultSet rset = stmt.executeQuery();
            con.commit();
            return rset;
        } catch (SQLException e) {
            System.out.println("Could not print customer policies.");
        }
        return null;
    }

    /* Adds a new policy and attaches it to the customer ID */
    public static void addPolicy(int cust_id, Connection con, Scanner scan) {
        System.out.println("\nWhat type of policy would you like to add?");
        String policy_type = scan.nextLine();
        while (policy_type.length() > 20) {
            System.out.println("Policy types can only be 20 characters long. Please input another: ");
            policy_type = scan.nextLine();
        }

        float policy_price = 0;
        boolean policy_price_valid = false;
        while (!policy_price_valid) {
            policy_price_valid = true;
            try {
                System.out.println("\nWhat is the price of this policy?");
                policy_price = scan.nextFloat();
            } catch (InputMismatchException e) {
                System.out.println("Please input only numbers.");
                policy_price_valid = false;
                scan.next();
            }
            if (policy_price < 0) {
                System.out.println("Policies cannot be negative.");
                policy_price_valid = false;
            }
        }

        char policy_active = '0';
        while (policy_active != 'Y' && policy_active != 'N' && policy_active != 'y' && policy_active != 'n') {
            System.out.println("\nIs this policy active? (Y\\N)");
            policy_active = scan.next().charAt(0);
            scan.nextLine();
        }
        policy_active = Character.toUpperCase(policy_active);
        String policy_active_s = Character.toString(policy_active);

        String startdate = "";
        String enddate = "";
        while (!startdate.matches("\\d\\d\\-[A-Za-z][A-Za-z][A-Za-z]\\-\\d\\d")
                || !enddate.matches("\\d\\d\\-[A-Za-z][A-Za-z][A-Za-z]\\-\\d\\d")) {
            System.out.println("\nInput starting and expiration dates for this policy:");
            System.out.print("Enter start date in standard format (dd-MON-yy): ");
            startdate = scan.next();
            System.out.println();
            System.out.print("\nEnter expiration date in standard format (dd-MON-yy): ");
            enddate = scan.next();

            if (!startdate.matches("\\d\\d\\-[A-Za-z][A-Za-z][A-Za-z]\\-\\d\\d")
                    || !enddate.matches("\\d\\d\\-[A-Za-z][A-Za-z][A-Za-z]\\-\\d\\d")) {
                System.out.println("\nStart date or Expiration date not inputted in the correct format.\n");
            }
        }

        try {
            PreparedStatement stmt = con.prepareStatement(
                    "INSERT INTO POLICIES(ID, POLICY_TYPE, ACTIVE_POLICY, PRICE, START_DATE, EXPIRATION_DATE) VALUES(?,?,?,?,?,?)");
            int policy_id = Policies.getNewID(con);
            stmt.setInt(1, policy_id);
            stmt.setString(2, policy_type);
            stmt.setString(3, policy_active_s);
            stmt.setFloat(4, policy_price);
            stmt.setString(5, startdate);
            stmt.setString(6, enddate);

            int update = stmt.executeUpdate();

            if (update == 1) {
                System.out.println("\nPolicy added successfully.\n");
            } else {
                System.out.println("\nPolicy was not successfully added.\n");
            }

            stmt = con.prepareStatement("INSERT INTO CUST_POLICY(POLICY_ID, CUST_ID) VALUES (?,?)");
            stmt.setInt(1, policy_id);
            stmt.setInt(2, cust_id);

            update = stmt.executeUpdate();
            con.commit();

            if (update == 1) {
                System.out.println("\nPolicy successfully linked to customer.\n");
            } else {
                System.out.println("\nPolicy was not successfully linked to customer.\n");
            }
        } catch (SQLException e) {
            System.out.println("Policy was not successfully added.");
        }
    }

    /* Removes a policy and removes the connection to the customer */
    public static void removePolicy(int cust_id, int policy_id, Connection con, int[] valid_policy_list) {
        try {
            boolean valid_policy = false;
            for (int i = 0; i < valid_policy_list.length; i++) {
                if (policy_id == valid_policy_list[i]) {
                    valid_policy = true;
                }
            }

            if (valid_policy) {

                if (policy_id == 0) {
                    System.out.println("No policies removed.");
                } else {
                    PreparedStatement stmt = con.prepareStatement("DELETE FROM POLICIES WHERE ID = ?");
                    stmt.setInt(1, policy_id);

                    int update = stmt.executeUpdate();
                    if (update == 1) {
                        System.out.println("Successfully removed the policy.");
                    } else {
                        System.out.println("Did not successfully remove the policy.");
                    }

                    stmt = con.prepareStatement("DELETE FROM CUST_POLICY WHERE POLICY_ID = ? AND CUST_ID = ?");
                    stmt.setInt(1, policy_id);
                    stmt.setInt(2, cust_id);

                    update = stmt.executeUpdate();
                    con.commit();
                    if (update == 1) {
                        System.out.println("Successfully removed the policy from the customer.");
                    } else {
                        System.out.println("Did not successfully remove the policy from the customer.");
                    }
                }
            } else {
                System.out.println("Policy ID does not match account. Removal aborted.");
            }
        } catch (SQLException e) {
            System.out.println("Could not remove policy.");
        }
    }
}
