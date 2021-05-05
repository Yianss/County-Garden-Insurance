import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Payments {
     /* Checks to see if a payment ID exists */
     public static boolean checkID(int id, Connection con) {
        try {
            if (id != 0) {
                PreparedStatement stmt = con.prepareStatement("SELECT ID FROM PAYMENTS");
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
            System.out.println("Payment ID list not successfully parsed.");
        }
        return false;
    }

    /* Asks the user for a payment ID and checks if it exists */
    public static int getAndCheckID(Connection con, Scanner scan) {
        int id = 0;
        while (!Payments.checkID(id, con)) {
            try {
                System.out.println("What is the payment/bill ID? Press 0 to abort.");
                id = scan.nextInt();
                scan.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Please input numbers only");
                scan.next();
            }
            if (!Payments.checkID(id, con)) {
                if(id == 0) {
                    break;
                }
                System.out.println("This is not a valid payment/bill ID. Please input another.");
            }
        }
        return id;
    }

    /* Pays the bill associated with the bill ID */
    public static void payBills(int cust_id, int bill_id, Connection con, int [] valid_bill_list) {
        try {
            boolean valid_bill = false;
            for (int i = 0; i < valid_bill_list.length; i++) {
                if (bill_id == valid_bill_list[i]) {
                    valid_bill = true;
                }
            }

            if (valid_bill) {

                if (bill_id == 0) {
                    System.out.println("No bills paid.");
                } else {
                    PreparedStatement stmt = con.prepareStatement("DELETE FROM PAYMENTS WHERE ID = ?");
                    stmt.setInt(1, bill_id);

                    int update = stmt.executeUpdate();
                    if (update == 1) {
                        System.out.println("Successfully paid the bill.");
                    } else {
                        System.out.println("Did not successfully pay the bill.");
                    }

                    stmt = con.prepareStatement("DELETE FROM POL_PAYMENTS WHERE PAYMENT_ID = ? AND POLICY_ID = (SELECT POLICY_ID FROM CUST_POLICY WHERE CUST_ID = ?)");
                    stmt.setInt(1, bill_id);
                    stmt.setInt(2, cust_id);

                    update = stmt.executeUpdate();
                    con.commit();
                    if (update == 1) {
                        System.out.println("Successfully removed the bill from the customer.");
                    } else {
                        System.out.println("Did not successfully remove the bill from the customer.");
                    }
                }
            } else {
                System.out.println("Payment/Bill ID does not match account. Removal aborted.");
            }

        } catch (SQLException e) {
            System.out.println("Could not remove payment.");
        }
    }

    /* Checks for all unpaid bills associated with the customer ID and returns a ResultSet */
    public static ResultSet getUnpaidBills(int cust_id, Connection con) {
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT ID, WAS_PAYMENT_MADE AS \"PAYMENT DUE\", AMOUNT_DUE AS \"AMOUNT DUE\" " +
                "FROM PAYMENTS WHERE WAS_PAYMENT_MADE = 'N' AND ID IN (SELECT PAYMENT_ID FROM POL_PAYMENTS WHERE POLICY_ID IN "
                +"(SELECT POLICY_ID FROM CUST_POLICY WHERE CUST_ID = ?))");
            
            stmt.setInt(1, cust_id);
            ResultSet rset = stmt.executeQuery();
            con.commit();
            return rset;
        } catch(SQLException e) {
            System.out.println("Could not fetch unpaid bills.");
        }
        return null;
    }
}
