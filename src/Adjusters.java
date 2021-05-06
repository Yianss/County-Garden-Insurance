import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Adjusters {
    /* Checks to see if an adjuster ID exists */
    public static boolean checkID(int id, Connection con) {
        try {
            if (id != 0) {
                PreparedStatement stmt = con.prepareStatement("SELECT ID FROM ADJUSTERS");
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
            System.out.println("Adjuster ID list not successfully parsed.");
        }
        return false;
    }

    /* Asks the user for an adjuster ID and checks if it exists */
    public static int getAndCheckID(Connection con, Scanner scan) {
        int id = 0;
        while (!Adjusters.checkID(id, con)) {
            try {
                System.out.println("What is your ID? Press 0 to abort.");
                id = scan.nextInt();
                scan.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Please input numbers only");
                scan.next();
            }
            if (!Adjusters.checkID(id, con)) {
                if(id == 0) {
                    break;
                }
                System.out.println("This is not a valid adjuster ID. Please input another.");
            }
        }
        return id;
    }

    /* Lists the current unserviced claims */
    public static void identifyUnservicedClaims(Connection con) {
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT CLAIM_ID, POLICY_ID, CLAIM_AMOUNT, DATE_OF_CLAIM, DESCRIPTION_OF_EVENT"
                + " FROM CLAIM_POLICY_PAYMENT"
                + " LEFT JOIN CLAIMS ON CLAIM_POLICY_PAYMENT.CLAIM_ID = CLAIMS.ID"
                + " LEFT JOIN CLAIM_PAYMENT ON CLAIM_POLICY_PAYMENT.CLAIM_PAYMENT_ID = CLAIM_PAYMENT.ID"
                + " WHERE CLAIM_POLICY_PAYMENT.CLAIM_PAYMENT_ID IS NULL");
            
            ResultSet rset = stmt.executeQuery();
            con.commit();

            if (!rset.isBeforeFirst()) {
                System.out.println("\nNo unserviced claims.\n");
            } else {
                System.out.println("\nUnserviced claims:\n");
                while (rset != null && rset.next()) {
                    System.out.printf("Claim ID: %d\nPolicy ID: %d\nClaim Amount: %5.2f\nDate of Claim: %s\nDescription of Event: %s\n\n", rset.getInt("CLAIM_ID"),
                    rset.getInt("POLICY_ID"), rset.getFloat("CLAIM_AMOUNT"),
                    rset.getString("DATE_OF_CLAIM"), rset.getString("DESCRIPTION_OF_EVENT"));
                }
            }
            stmt.close();
            rset.close();
        } catch (SQLException e) {
            System.out.println("Could not fetch unserviced claims.");
        }
    }
}
