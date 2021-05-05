import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Claims {

    /* Creates a new claim ID */
    public static int getNewID(Connection con) {
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT MAX(ID)+1 FROM CLAIMS");
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

    /* Allows the user to make a new claim */
    public static void makeClaim(int cust_id, int insured_item_id, Connection con, Scanner scan) {
        float claim_price = 0;
        boolean claim_price_valid = false;
        while (!claim_price_valid) {
            claim_price_valid = true;
            try {
                System.out.println("\nWhat amount are you claiming in damages?");
                claim_price = scan.nextFloat();
            } catch (InputMismatchException e) {
                System.out.println("Please input only numbers.");
                claim_price_valid = false;
                scan.next();
            }
            if (claim_price < 0) {
                System.out.println("Claims cannot be negative.");
                claim_price_valid = false;
            }
        }

        String claimdate = "";
        while (!claimdate.matches("\\d\\d\\-[A-Za-z][A-Za-z][A-Za-z]\\-\\d\\d")) {
            System.out.print("Enter the date of the claim in standard format (dd-MON-yy): ");
            claimdate = scan.next();
            scan.nextLine();
            System.out.println();

            if (!claimdate.matches("\\d\\d\\-[A-Za-z][A-Za-z][A-Za-z]\\-\\d\\d")) {
                System.out.println("\nClaim date not inputted in the correct format.\n");
            }
        }

        boolean valid_desc = false;
        String description_of_event = "";
        while(!valid_desc) {

            System.out.println("Please enter a short description of the event (maximum 100 characters):");
            description_of_event = scan.nextLine();

            if(description_of_event.length() <= 100)
                valid_desc = true;
            else {
                System.out.println("Description too long. Please shorten it.");
            }
        }

        // Create new claim
        int new_claim_id = Claims.getNewID(con);
        int update = 0;
        try {
            PreparedStatement stmt = con.prepareStatement("INSERT INTO CLAIMS(ID, CLAIM_AMOUNT, DATE_OF_CLAIM, DESCRIPTION_OF_EVENT) VALUES(?,?,?,?)");
            stmt.setInt(1,new_claim_id);
            stmt.setFloat(2, claim_price);
            stmt.setString(3, claimdate);
            stmt.setString(4, description_of_event);
            update = stmt.executeUpdate();
            stmt.close();

            if(update == 1) {
                System.out.println("Successfully added claim.");
            }
            else {
                System.out.println("Could not successfully add claim.");
            }
        } catch(SQLException e) {
            System.out.println("Could not successfully add claim.");
        }

        //Connect claim to customer
        try {
            PreparedStatement stmt = con.prepareStatement("INSERT INTO CUST_CLAIM(CLAIM_ID, CUSTOMER_ID) VALUES(?,?)");
            stmt.setInt(1, new_claim_id);
            stmt.setInt(2, cust_id);
            update = stmt.executeUpdate();
            stmt.close();

            if(update == 1) {
                System.out.println("Successfully linked claim with customer.");
            }
            else {
                System.out.println("Could not successfully link claim with customer.");
            }
        } catch(SQLException e) {
            System.out.println("Could not successfully link claim with customer.");
        }

        // Connect claim to policy by first fetching the policy ID associated with the insured item
        try {
            PreparedStatement stmt = con.prepareStatement("INSERT INTO CLAIM_POLICY_PAYMENT(POLICY_ID, CLAIM_ID) VALUES(?,?)");
            PreparedStatement stmt2 = con.prepareStatement("SELECT POLICY_ID FROM INSURANCE WHERE INSURED_ITEMS_ID = ?");
            stmt2.setInt(1, insured_item_id);
            ResultSet rset = stmt2.executeQuery();
            rset.next();
            int policy_id = rset.getInt(1);
            
            stmt.setInt(1, policy_id);
            stmt.setInt(2, new_claim_id);
            update = stmt.executeUpdate();
            stmt.close();
            stmt2.close();
            rset.close();

            if(update == 1) {
                System.out.println("Successfully linked claim with policy.");
            }
            else {
                System.out.println("Could not successfully link claim with policy.");
            }
        } catch(SQLException e) {
            System.out.println("Could not successfully link claim with policy.");
        }

        // Connect claim to insured items
        try {
            PreparedStatement stmt = con.prepareStatement("INSERT INTO PROTECTION(INSURED_ITEMS_ID, CLAIM_ID) VALUES(?,?)");
            
            stmt.setInt(1, insured_item_id);
            stmt.setInt(2, new_claim_id);
            update = stmt.executeUpdate();
            stmt.close();

            if(update == 1) {
                System.out.println("Successfully linked claim with insured item.");
            }
            else {
                System.out.println("Could not successfully link claim with insured item.");
            }
            con.commit();
        } catch(SQLException e) {
            System.out.println("Could not successfully link claim with insured item.");
        }
    }
}
