import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.InputMismatchException;
import java.util.Scanner;

public class InsuredItems {

    /* Gets all existing insured items under the customer's policies */
    public static ResultSet getInsuredItems(int cust_id, Connection con) {
        if (cust_id == 0)
            return null;
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT ID, ITEM, VALUE"
                    + " FROM INSURED_ITEMS WHERE ID IN (SELECT INSURED_ITEMS_ID FROM INSURANCE WHERE POLICY_ID IN (SELECT POLICY_ID FROM CUST_POLICY WHERE CUST_ID = ?))");
            stmt.setInt(1, cust_id);
            ResultSet rset = stmt.executeQuery();
            con.commit();
            return rset;
        } catch (SQLException e) {
            System.out.println("Could not print customer insured items.");
        }
        return null;
    }

    /* Checks to see if a insured_item ID is valid */
    public static boolean checkID(int id, Connection con) {
        try {
            if (id != 0) {
                PreparedStatement stmt = con.prepareStatement("SELECT ID FROM INSURED_ITEMS");
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
            System.out.println("Insured Item ID list not successfully parsed.");
        }
        return false;
    }

    /* Asks the user for a insured item ID, checks if it is valid and exists */
    public static int getAndCheckID(Connection con, Scanner scan) {
        int id = 0;
        while (!InsuredItems.checkID(id, con)) {
            try {
                System.out.println("What is the insured item ID? Input 0 to abort.");
                id = scan.nextInt();
                scan.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Please input numbers only");
                scan.next();
            }
            if (!InsuredItems.checkID(id, con)) {
                if (id == 0) {
                    break;
                }
                System.out.println("This is not a valid insured item ID. Please input another.");
            }
        }
        return id;
    }

    /* Adds an insured item */
    public static void addInsuredItem(int cust_id, Connection con, Scanner scan) {

        System.out.println("");
        ResultSet rset = Policies.getCustomerPolicies(cust_id, con);
        try {
            int[] valid_policy_list = new int[500];
            int counter = 0;
            if (!rset.isBeforeFirst()) {
                System.out.println("No policies attached to the account.\n");
            } else {
                System.out.println("\nPolicies attached to the account. Choose one to attach the item to.\n");
                while (rset != null && rset.next()) {
                    System.out.printf(
                            "ID: %d\nPolicy Type: %s\nActive Policy?: %s\nPrice: %5.2f\nStart Date: %s\nExpiration Date: %s\n\n",
                            rset.getInt("ID"), rset.getString("POLICY TYPE"), rset.getString("ACTIVE POLICY"),
                            rset.getFloat("PRICE"), rset.getDate("START DATE"), rset.getDate("EXPIRATION DATE"));
                    valid_policy_list[counter] = rset.getInt("ID");
                    counter++;
                }

                rset.close();

                int policy_id = 0;
                boolean valid_policy_id = false;
                while (!valid_policy_id) {
                    try {
                        policy_id = scan.nextInt();
                    } catch (InputMismatchException e) {
                        System.out.println("Please input only numbers.");
                        scan.next();
                    }

                    if (!Policies.checkID(policy_id, con, valid_policy_list)) {
                        if (policy_id == 0) {
                            System.out.println("Policy not added");
                            break;
                        } else {
                            System.out.println("Policy ID not valid. Please try again. Press 0 to abort.");
                        }
                    } else {
                        valid_policy_id = true;
                    }
                }

                scan.nextLine();

                if (valid_policy_id) {
                    System.out.println("What is the name of the item you want to insure? (maximum 50 characters)");
                    String insured_item_name = scan.nextLine();
                    while (insured_item_name.length() > 50) {
                        System.out.println("Name too long. Please input a shorter name.");
                        insured_item_name = scan.nextLine();
                    }

                    boolean valid_value = false;
                    float insured_item_value = 0;
                    while (!valid_value) {
                        System.out.println("What is the value of this item?");
                        try {
                            insured_item_value = scan.nextFloat();
                        } catch (InputMismatchException e) {
                            System.out.println("Please input only numbers.");
                            scan.next();
                        }
                        if (insured_item_value > 0) {
                            valid_value = true;
                        } else {
                            System.out.println("Item cannot have a negative value");
                        }
                    }

                    int new_item_id = getNewID(con);
                    try {
                        PreparedStatement stmt = con
                                .prepareStatement("INSERT INTO INSURED_ITEMS(ID, ITEM, VALUE) VALUES (?,?,?)");
                        stmt.setInt(1, new_item_id);
                        stmt.setString(2, insured_item_name);
                        stmt.setFloat(3, insured_item_value);

                        int update = stmt.executeUpdate();
                        stmt.close();

                        if (update == 1) {
                            System.out.println("Successfully added item.");
                        } else {
                            System.out.println("Did not successfully add item.");
                        }
                    } catch (SQLException e) {
                        System.out.println("Did not successfully add item.");
                    }

                    try {
                        PreparedStatement stmt = con
                                .prepareStatement("INSERT INTO INSURANCE(POLICY_ID, INSURED_ITEMS_ID) VALUES (?,?)");
                        stmt.setInt(1, policy_id);
                        stmt.setInt(2, new_item_id);

                        int update = stmt.executeUpdate();
                        con.commit();
                        stmt.close();

                        if (update == 1) {
                            System.out.println("Successfully linked item to policy.");
                        } else {
                            System.out.println("Did not successfully link item to policy.");
                        }
                    } catch (SQLException e) {
                        System.out.println("Could not successfully link insured item to policy.");
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("Could not fetch customer policies.");
        }

    }

    /* Creates a new Insured Item ID */
    public static int getNewID(Connection con) {
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT MAX(ID)+1 FROM INSURED_ITEMS");
            ResultSet rset = stmt.executeQuery();
            con.commit();
            rset.next();
            int val = rset.getInt(1);
            stmt.close();
            rset.close();
            return val;
        } catch (SQLException e) {
            System.out.println("Unable to fetch new insured item ID.");
        }
        return -1;
    }

    /* Removes an insured item */
    public static void removeInsuredItem(int cust_id, Connection con, Scanner scan) {
        System.out.println("What item would you like to remove?:");

        ResultSet rset = getInsuredItems(cust_id, con);
        int[] valid_item_list = new int[500];
        int counter = 0;
        try {
            if (!rset.isBeforeFirst()) {
                System.out.println("No insured items attached to the account.\n");
            } else {
                System.out.println("\nInsured items attached to the account. Choose one to remove. Press 0 to abort.\n");
                while (rset != null && rset.next()) {
                    System.out.printf("ID: %d\nItem: %s\nValue: %5.2f\n\n", rset.getInt("ID"), rset.getString("ITEM"),
                            rset.getFloat("VALUE"));
                    valid_item_list[counter] = rset.getInt("ID");
                    counter++;
                }
            }
            rset.close();
        } catch (SQLException e) {
            System.out.println("Could not query insured items.");
        }

        int item_id = 0;
        boolean valid_item_id = false;
        while (!valid_item_id) {
            try {
                item_id = scan.nextInt();
            } catch (InputMismatchException e) {
                System.out.println("Please input only numbers.");
                scan.next();
            }

            if (!InsuredItems.checkID(item_id, con, valid_item_list)) {
                if (item_id == 0) {
                    System.out.println("Item not removed.");
                    break;
                } else {
                    System.out.println("Item ID not valid. Please try again. Press 0 to abort.");
                }
            } else {
                valid_item_id = true;
            }
        }

        scan.nextLine();

        int update = 0;
        try {
            PreparedStatement stmt = con.prepareStatement("DELETE FROM INSURED_ITEMS WHERE ID = ?");
            stmt.setInt(1, item_id);
            update = stmt.executeUpdate();

            if (update == 1) {
                System.out.println("Successfully removed item.");
            } else {
                System.out.println("Could not successfully remove item.");
            }
            stmt.close();
        } catch (SQLException e) {
            System.out.println("Could not successfully remove item.");
        }

        try {
            PreparedStatement stmt = con.prepareStatement("DELETE FROM INSURANCE WHERE INSURED_ITEMS_ID = ?");
            stmt.setInt(1, item_id);
            update = stmt.executeUpdate();

            if (update == 1) {
                System.out.println("Successfully removed item from policy.");
            } else {
                System.out.println("Could not successfully remove item from policy.");
            }
            stmt.close();
        } catch (SQLException e) {
            System.out.println("Could not successfully remove item from policy.");
        }

        try {
            PreparedStatement stmt = con.prepareStatement(
                    "DELETE FROM CLAIMS WHERE ID = (SELECT CLAIM_ID FROM PROTECTION WHERE INSURED_ITEMS_ID = ?)");
            stmt.setInt(1, item_id);
            update = stmt.executeUpdate();

            stmt = con.prepareStatement("DELETE FROM PROTECTION WHERE INSURED_ITEMS_ID = ?");
            stmt.setInt(1, item_id);
            update = stmt.executeUpdate();
            con.commit();
            if (update == 1) {
                System.out.println("Successfully removed item from past claims.");
            } else {
                System.out.println("Could not successfully remove item from past claims. Item may not be linked to any claims.");
            }
            stmt.close();
        } catch (SQLException e) {
            System.out.println("Could not successfully remove item from past claims. Item may not be linked to any claims.");
        }

    }

    /* Checks to see if a item ID is valid against a list of item ID's */
    public static boolean checkID(int id, Connection con, int[] valid_items) {
        if (id == 0)
            return false;
        for (int i = 0; i < valid_items.length; i++)
            if (id == valid_items[i])
                return true;
        return false;
    }

    public static void replaceInsuredItem(int cust_id, Connection con, Scanner scan) {
        System.out.println("What item would you like to replace?:");

        ResultSet rset = getInsuredItems(cust_id, con);
        int[] valid_item_list = new int[500];
        int counter = 0;
        try {
            if (!rset.isBeforeFirst()) {
                System.out.println("No insured items attached to the account.\n");
            } else {
                System.out.println("\nInsured items attached to the account. Choose one to replace. Press 0 to abort.\n");
                while (rset != null && rset.next()) {
                    System.out.printf("ID: %d\nItem: %s\nValue: %5.2f\n\n", rset.getInt("ID"), rset.getString("ITEM"),
                            rset.getFloat("VALUE"));
                    valid_item_list[counter] = rset.getInt("ID");
                    counter++;
                }
            }
            rset.close();
        } catch (SQLException e) {
            System.out.println("Could not query insured items.");
        }

        int item_id = 0;
        boolean valid_item_id = false;
        while (!valid_item_id) {
            try {
                item_id = scan.nextInt();
            } catch (InputMismatchException e) {
                System.out.println("Please input only numbers.");
                scan.next();
            }

            if (!InsuredItems.checkID(item_id, con, valid_item_list)) {
                if (item_id == 0) {
                    System.out.println("Item not replaced.");
                    break;
                } else {
                    System.out.println("Item ID not valid. Please try again. Press 0 to abort.");
                }
            } else {
                valid_item_id = true;
            }
        }

        scan.nextLine();

        if (valid_item_id) {
            System.out.println("What is the name of the item you want to insure? (maximum 50 characters)");
            String insured_item_name = scan.nextLine();
            while (insured_item_name.length() > 50) {
                System.out.println("Name too long. Please input a shorter name.");
                insured_item_name = scan.nextLine();
            }

            boolean valid_value = false;
            float insured_item_value = 0;
            while (!valid_value) {
                System.out.println("What is the value of this item?");
                try {
                    insured_item_value = scan.nextFloat();
                } catch (InputMismatchException e) {
                    System.out.println("Please input only numbers.");
                    scan.next();
                }
                if (insured_item_value > 0) {
                    valid_value = true;
                } else {
                    System.out.println("Item cannot have a negative value");
                }
            }

            try {
                PreparedStatement stmt = con
                    .prepareStatement("UPDATE INSURED_ITEMS SET ITEM = ?, VALUE = ? WHERE ID = ?");
                stmt.setString(1, insured_item_name);
                stmt.setFloat(2, insured_item_value);
                stmt.setInt(3, item_id);

                int update = stmt.executeUpdate();
                con.commit();

                if (update == 1) {
                    System.out.println("Successfully replaced insured item.");
                } else {
                    System.out.println("Did not successfully replace insured item.");
                }
            } catch (SQLException e) {
                System.out.println(/*"Could not replace item."*/ e);
            }
        }
    }
}
