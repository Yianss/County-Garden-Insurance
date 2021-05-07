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
                if (id == 0) {
                    break;
                }
                System.out.println("This is not a valid adjuster ID. Please input another.");
            }
        }
        return id;
    }

    /* Lists the current unserviced claims */
    public static void identifyUnservicedClaims(Connection con, Scanner scan) {
        try {
            PreparedStatement stmt = con
                    .prepareStatement("SELECT CLAIM_ID, POLICY_ID, CLAIM_AMOUNT, DATE_OF_CLAIM, DESCRIPTION_OF_EVENT"
                            + " FROM CLAIM_POLICY_PAYMENT"
                            + " LEFT JOIN CLAIMS ON CLAIM_POLICY_PAYMENT.CLAIM_ID = CLAIMS.ID"
                            + " LEFT JOIN CLAIM_PAYMENT ON CLAIM_POLICY_PAYMENT.CLAIM_PAYMENT_ID = CLAIM_PAYMENT.ID"
                            + " WHERE CLAIM_POLICY_PAYMENT.CLAIM_PAYMENT_ID IS NULL");

            ResultSet rset = stmt.executeQuery();
            con.commit();
            int[] unserviced_claims_ids = new int[1000];
            int runner = 0;
            if (!rset.isBeforeFirst()) {
                System.out.println("\nNo unserviced claims.\n");
            } else {
                System.out.println("\nUnserviced claims:\n");
                while (rset != null && rset.next()) {
                    System.out.printf(
                            "Claim ID: %d\nPolicy ID: %d\nClaim Amount: %5.2f\nDate of Claim: %s\nDescription of Event: %s\n\n",
                            rset.getInt("CLAIM_ID"), rset.getInt("POLICY_ID"), rset.getFloat("CLAIM_AMOUNT"),
                            rset.getString("DATE_OF_CLAIM"), rset.getString("DESCRIPTION_OF_EVENT"));
                    unserviced_claims_ids[runner] = rset.getInt("CLAIM_ID");
                    runner++;
                }
                serviceUnservicedClaims(con, scan, unserviced_claims_ids);
            }
            stmt.close();
            rset.close();
        } catch (SQLException e) {
            System.out.println("Could not fetch unserviced claims.");
        }
    }

    /*
     * Lets the adjuster service unserviced claims by receiving a list of unserviced
     * claim ids from identifyUnservicedClaims
     */
    public static void serviceUnservicedClaims(Connection con, Scanner scan, int[] unserviced_claims_ids) {
        int unserviced_claim_id = 0;
        try {
            System.out.println("\nInput the ID of the claim that you would like to service. Press 0 to abort.");
            boolean valid_id = false;
            while (!valid_id) {
                try {
                    unserviced_claim_id = scan.nextInt();
                    for (int i = 0; i < unserviced_claims_ids.length; i++) {
                        if (unserviced_claim_id == 0) {
                            valid_id = true;
                        } else if (unserviced_claim_id == unserviced_claims_ids[i]) {
                            valid_id = true;
                        }
                    }
                    if (!valid_id) {
                        System.out.println("Not a valid claim ID.");
                    }
                } catch (InputMismatchException e) {
                    System.out.println("Please input only integers.");
                    scan.next();
                }
            }

            if (unserviced_claim_id != 0) {
                float payout = 0;
                boolean valid_input = false;
                while (!valid_input) {
                    try {
                        scan.nextLine();
                        System.out.println("How much are you going to payout?");
                        payout = scan.nextFloat();
                        scan.nextLine();
                        valid_input = true;
                    } catch (InputMismatchException e) {
                        System.out.println("Please input only numbers.");
                        scan.next();
                    }
                }

                int newClaimPaymentID = getNewClaimPaymentID(con);

                PreparedStatement stmt = con
                        .prepareStatement("INSERT INTO CLAIM_PAYMENT (ID, AMOUNT_PAID) VALUES (?,?)");
                stmt.setInt(1, newClaimPaymentID);
                stmt.setFloat(2, payout);
                int update = stmt.executeUpdate();

                if (update == 1) {
                    System.out.println("Successfully added claim payment.");
                }

                PreparedStatement stmt2 = con
                        .prepareStatement("UPDATE CLAIM_POLICY_PAYMENT SET CLAIM_PAYMENT_ID = ? WHERE CLAIM_ID = ?");
                stmt2.setInt(1, newClaimPaymentID);
                stmt2.setInt(2, unserviced_claim_id);
                stmt2.executeUpdate();

                if (update == 1) {
                    System.out.println("Successfully linked claim payment to policy and claim.");
                }

                con.commit();
                stmt.close();
                stmt2.close();
            } else {
                System.out.println("Claims not serviced.");
            }
        } catch (SQLException e) {
            System.out.println("Could not service claims.");
        }
    }

    /* Creates a new claim payment ID */
    public static int getNewClaimPaymentID(Connection con) {
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT MAX(ID)+1 FROM CLAIM_PAYMENT");
            ResultSet rset = stmt.executeQuery();
            con.commit();
            rset.next();
            int val = rset.getInt(1);
            stmt.close();
            rset.close();
            return val;
        } catch (SQLException e) {
            System.out.println("Unable to fetch new claim payment ID.");
        }
        return -1;
    }

    /* Identifies open claims and lets the adjuster assign a remediation firm */
    public static void assignRemediationFirms(Connection con, Scanner scan, int adjuster_id) {
        int[] open_claims_ids = new int[1000];
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT ID, CLAIM_AMOUNT, DATE_OF_CLAIM, DESCRIPTION_OF_EVENT"
                    + " FROM CLAIMS LEFT JOIN ADJUSTMENT ON CLAIMS.ID = ADJUSTMENT.CLAIM_ID"
                    + " WHERE ADJUSTMENT.OUTSOURCED_COMPANY_NAME IS NULL");
            ResultSet rset = stmt.executeQuery();
            con.commit();

            int runner = 0;
            if (!rset.isBeforeFirst()) {
                System.out.println("\nNo open claims.\n");
            } else {
                System.out.println("\nOpen claims:\n");
                while (rset != null && rset.next()) {
                    System.out.printf(
                            "Claim ID: %d\nClaim Amount: %5.2f\nDate of Claim: %s\nDescription of Event: %s\n\n",
                            rset.getInt("ID"), rset.getFloat("CLAIM_AMOUNT"), rset.getString("DATE_OF_CLAIM"),
                            rset.getString("DESCRIPTION_OF_EVENT"));
                    open_claims_ids[runner] = rset.getInt("ID");
                    runner++;
                }
            }
            stmt.close();
            rset.close();
        } catch (SQLException e) {
            System.out.println("Could not fetch firms with open claims.");
        }

        int open_claim_id = 0;
        System.out.println("Input the ID of the claim that you would like to assign a firm to. Press 0 to abort.");
        boolean valid_id = false;
        while (!valid_id) {
            try {
                open_claim_id = scan.nextInt();
                scan.nextLine();
                for (int i = 0; i < open_claims_ids.length; i++) {
                    if (open_claim_id == 0) {
                        valid_id = true;
                    } else if (open_claim_id == open_claims_ids[i]) {
                        valid_id = true;
                    }
                }
                if (!valid_id) {
                    System.out.println("Not a valid claim ID.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Please input only integers.");
                scan.next();
            }

        }

        if (open_claim_id != 0) {
            String[] available_companies = new String[1000];
            try {
                PreparedStatement stmt = con
                        .prepareStatement("SELECT NAME, TYPE, CONTACT_INFORMATION FROM OUTSOURCED_COMPANY");
                ResultSet rset = stmt.executeQuery();

                int runner = 0;
                if (!rset.isBeforeFirst()) {
                    System.out.println("\nNo outsourced companies.\n");
                } else {
                    System.out.println("\nOutsourced companies:\n");
                    while (rset != null && rset.next()) {
                        System.out.printf("Name: %s\nType: %s\nContact Information: %s\n\n", rset.getString("NAME"),
                                rset.getString("TYPE"), rset.getString("CONTACT_INFORMATION"));
                        available_companies[runner] = rset.getString("NAME");
                        runner++;
                    }
                }
                stmt.close();
                rset.close();

            } catch (SQLException e) {
                System.out.println("Could not fetch outsourced companies.");
            }

            String chosen_outsourced_company = "";
            System.out.println("\nInput the name of the company that you would like to assign (case sensitive):");
            boolean valid_name = false;
            while (!valid_name) {
                chosen_outsourced_company = scan.nextLine();
                for (int i = 0; i < available_companies.length; i++) {
                    if (chosen_outsourced_company.equals(available_companies[i])) {
                        valid_name = true;
                    }
                }
                if (!valid_name) {
                    System.out.println("Not a valid outsourced company name.");
                }
            }

            try {
                PreparedStatement stmt = con.prepareStatement(
                        "INSERT INTO ADJUSTMENT(CLAIM_ID, ADJUSTER_ID, OUTSOURCED_COMPANY_NAME)" + " VALUES(?,?,?)");
                stmt.setInt(1, open_claim_id);
                stmt.setInt(2, adjuster_id);
                stmt.setString(3, chosen_outsourced_company);
                int update = stmt.executeUpdate();
                con.commit();
                if (update == 1) {
                    System.out.println("Successfully added outsourced company to claim.");
                } else {
                    System.out.println("Could not add outsourced company to claim.");
                }
            } catch (SQLException e) {
                System.out.println(e/* "Could not attach outsourced company to claim." */);
            }
        } else {
            System.out.println("No outsourced companies added.");
        }
    }
}
