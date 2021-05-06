import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Interface {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        /* Welcomes the user and asks for login info */
        System.out.print("Welcome to the County Garden Insurance Main Menu.\nPlease insert your username: ");
        String uname = scan.next();
        System.out.println();
        System.out.print("Please insert your password: ");
        String pass = scan.next();
        scan.nextLine();
        System.out.println();

        String url = "jdbc:oracle:thin:@edgar1.cse.lehigh.edu:1521:cse241";

        int program_runner = 1;

        do {
            try {
                /*
                 * Attempts to establish connection and asks the user what type of user they are
                 */
                Connection con = DriverManager.getConnection(url, uname, pass);
                con.setAutoCommit(false);

                int usertype = 0;

                System.out.println("\nWhat type of user are you signing in as?\n\n"
                        + "1. Corporate Management\n2. Customer\n3. Agent\n4. Adjuster\n5. Exit the program");
                boolean validchoice = false;
                while (!validchoice) {
                    try {
                        usertype = scan.nextInt();
                    } catch (InputMismatchException e) {
                        System.out.println("Please input only numbers.");
                        scan.next();
                    }
                    validchoice = true;
                }

                switch (usertype) {
                    case 1:
                        break;
                    case 2: /* Customer Portal */
                        int customer = 1;
                        while (customer == 1) {
                            System.out.println(
                                    "\n\nWelcome to the customer portal. Please select an option.\n\n1. Create a new customer\n2. Add a policy\n3. Drop a policy\n4. Pay a bill\n"
                                            + "5. Make a claim\n6. Add an insured item\n7. Remove an insured item\n8. Replace an insured item\n9. Back to main menu");
                            validchoice = false;
                            int custchoice = 0;
                            while (!validchoice) {
                                try {
                                    custchoice = scan.nextInt();
                                } catch (InputMismatchException e) {
                                    System.out.println("Please input only numbers.");
                                    scan.next();
                                }
                                validchoice = true;
                            }
                            switch (custchoice) {
                                case 1: /* Create a new customer */
                                    Customer.createNewCustomer(con, scan);
                                    break;
                                case 2: /* Add a policy */
                                    int cust_id = Customer.getAndCheckID(con, scan);
                                    if (cust_id != 0)
                                        Policies.addPolicy(cust_id, con, scan);
                                    break;
                                case 3: /* Drop a policy */
                                    cust_id = Customer.getAndCheckID(con, scan);

                                    ResultSet rset = Policies.getCustomerPolicies(cust_id, con);
                                    int[] valid_policy_list = new int[500];
                                    int counter = 0;
                                    if (!rset.isBeforeFirst()) {
                                        System.out.println("No policies attached to the account.\n");
                                        break;
                                    } else {
                                        System.out
                                                .println("\nPolicies attached to the account. Choose one to remove.\n");
                                        while (rset != null && rset.next()) {
                                            System.out.printf(
                                                    "ID: %d\nPolicy Type: %s\nActive Policy?: %s\nPrice: %5.2f\nStart Date: %s\nExpiration Date: %s\n\n",
                                                    rset.getInt("ID"), rset.getString("POLICY TYPE"),
                                                    rset.getString("ACTIVE POLICY"), rset.getFloat("PRICE"),
                                                    rset.getDate("START DATE"), rset.getDate("EXPIRATION DATE"));
                                            valid_policy_list[counter] = rset.getInt("ID");
                                            counter++;
                                        }
                                    }

                                    // Get the policy ID from the user and check if it is valid
                                    int policy_to_remove = Policies.getAndCheckID(con, scan);

                                    if (policy_to_remove != 0) {
                                        Policies.removePolicy(cust_id, policy_to_remove, con, valid_policy_list);
                                    } else {
                                        System.out.println("No policies removed");
                                    }

                                    break;
                                case 4: /* Pay a bill */
                                    cust_id = Customer.getAndCheckID(con, scan);

                                    rset = Payments.getUnpaidBills(cust_id, con);

                                    int[] valid_bill_list = new int[500];
                                    counter = 0;
                                    if (!rset.isBeforeFirst()) {
                                        System.out.println("No unpaid bills attached to the account.\n");
                                        break;
                                    } else {
                                        System.out.println(
                                                "\nUnpaid bills attached to the account. Choose one to remove.\n");
                                        while (rset != null && rset.next()) {
                                            System.out.printf("ID: %d\nPaid?: %s\nAmount Due: %5.2f\n\n",
                                                    rset.getInt("ID"), rset.getString("PAYMENT DUE"),
                                                    rset.getFloat("AMOUNT DUE"));
                                            valid_bill_list[counter] = rset.getInt("ID");
                                            counter++;
                                        }
                                    }

                                    int bill_to_pay_id = Payments.getAndCheckID(con, scan);

                                    if (bill_to_pay_id != 0) {
                                        Payments.payBills(cust_id, bill_to_pay_id, con, valid_bill_list);
                                    } else {
                                        System.out.println("No bills paid.");
                                    }

                                    break;
                                case 5: /* Make a claim */
                                    cust_id = Customer.getAndCheckID(con, scan);
                                    // Gets the item id of the item they want to create a claim about
                                    System.out.println("\nWhich item would you like to make a claim about?: ");

                                    rset = InsuredItems.getInsuredItems(cust_id, con);

                                    // Creates a list of insured items associated with that customer and prints them
                                    int[] valid_insured_item_list = new int[500];
                                    counter = 0;
                                    if (!rset.isBeforeFirst()) {
                                        System.out.println("No insured items attached to any policies.\n");
                                        break;
                                    } else {
                                        System.out.println(
                                                "\nInsured items attached to the account. Choose one to remove.\n");
                                        while (rset != null && rset.next()) {
                                            System.out.printf("ID: %d\nItem: %s\nValue: %5.2f\n\n", rset.getInt("ID"),
                                                    rset.getString("ITEM"), rset.getFloat("VALUE"));
                                            valid_insured_item_list[counter] = rset.getInt("ID");
                                            counter++;
                                        }
                                    }

                                    int insured_item_id = InsuredItems.getAndCheckID(con, scan);

                                    if (insured_item_id != 0) {
                                        Claims.makeClaim(cust_id, insured_item_id, con, scan);
                                    } else {
                                        System.out.println("Claim not made.");
                                    }
                                    break;
                                case 6: /* Add an insured item */
                                    cust_id = Customer.getAndCheckID(con, scan);

                                    if (cust_id != 0) {
                                        InsuredItems.addInsuredItem(cust_id, con, scan);
                                    } else {
                                        System.out.println("Insured item not added.");
                                    }
                                    break;
                                case 7: /* Remove an insured item */
                                    cust_id = Customer.getAndCheckID(con, scan);

                                    if (cust_id != 0) {
                                        InsuredItems.removeInsuredItem(cust_id, con, scan);
                                    } else {
                                        System.out.println("Insured item not removed.");
                                    }
                                    break;
                                case 8: /* Replace an insured item */
                                    cust_id = Customer.getAndCheckID(con, scan);

                                    if (cust_id != 0) {
                                        InsuredItems.replaceInsuredItem(cust_id, con, scan);
                                    } else {
                                        System.out.println("Insured item not replaced.");
                                    }
                                    break;
                                case 9: /* Back to main menu */
                                    customer = 0;
                                    break;
                            }
                        }
                    case 3:
                        int agent = 1;
                        while (agent == 1) {
                            System.out.println(
                                    "\n\nWelcome to the agent portal. Please select an option.\n\n1. Identify customers with overdue bills. \n2. Customers with pending claims that have not been"
                                            + " serviced recently. \n3. Compute revenue generated by the agent.\n4. Show your customers.\n5. Back to main menu.\n");
                            validchoice = false;
                            int agentchoice = 0;
                            while (!validchoice) {
                                try {
                                    agentchoice = scan.nextInt();
                                } catch (InputMismatchException e) {
                                    System.out.println("Please input only numbers.");
                                    scan.next();
                                }
                                validchoice = true;
                            }
                            switch (agentchoice) {
                                case 1: /* Checks all overdue bills in the system */
                                    int agent_id = Agents.getAndCheckID(con, scan);
                                    if (agent_id != 0)
                                        Agents.checkOverdueBills(con);
                                    else {
                                        System.out.println("Overdue bills not queried.");
                                    }
                                    break;
                                case 2: /* Checks customers pending claims associated with the agent ID */
                                    agent_id = Agents.getAndCheckID(con, scan);
                                    if (agent_id != 0)
                                        Agents.checkPendingClaims(agent_id, con);
                                    else {
                                        System.out.println("Customers with pending claims not queried.");
                                    }
                                    break;
                                case 3: /* Checks the revenue generated associated by the agent ID */
                                    agent_id = Agents.getAndCheckID(con, scan);
                                    if (agent_id != 0)
                                        Agents.generateRevenue(agent_id, con);
                                    else {
                                        System.out.println("Revenue generated by agent not queried.");
                                    }
                                    break;
                                case 4: /* Lists customers associated with the agent */
                                    agent_id = Agents.getAndCheckID(con, scan);
                                    if (agent_id != 0)
                                        Agents.listCustomers(agent_id, con);
                                    else {
                                        System.out.println("Customers not listed.");
                                    }
                                    break;
                                case 5:
                                    agent = 0;
                                    break;
                            }
                        }
                        break;
                    case 4:
                        int adjuster = 1;
                        while (adjuster == 1) {
                            System.out.println(
                                    "\n\nWelcome to the adjuster portal. Please select an option.\n\n1. Identify and service claims that have not been serviced recently. \n2. Assign remediation firms or body shops to open claims."
                                            + "\n3. Adjust the price of a claim payout.\n4. Back to main menu.\n");
                            validchoice = false;
                            int adjusterchoice = 0;
                            while (!validchoice) {
                                try {
                                    adjusterchoice = scan.nextInt();
                                    scan.nextLine();
                                } catch (InputMismatchException e) {
                                    System.out.println("Please input only numbers.");
                                    scan.next();
                                }
                                validchoice = true;
                            }
                            switch(adjusterchoice) {
                                case 1:
                                    int adjuster_id = Adjusters.getAndCheckID(con, scan);
                                    if (adjuster_id != 0)
                                        Adjusters.identifyUnservicedClaims(con, scan);
                                    else {
                                        System.out.println("Unserviced claims not queried.");
                                    }
                                    break;
                                case 2:
                                    break;
                                case 3:
                                    break;
                                case 4:
                                    adjuster = 0;
                                    break;
                            }
                        }
                        break;
                    case 5:
                        con.close();
                        scan.close();
                        program_runner = 0;
                        break;
                }

            } catch (SQLException e) {
                System.out.println(
                        "Connection to database failed. Username or password may be incorrect. Please try again.\n");

                System.out.print("\nPlease insert your username: ");
                uname = scan.next();
                System.out.println();
                System.out.print("Enter your password: ");
                pass = scan.next();
                System.out.println();
            }

        } while (program_runner == 1);

    }
}
