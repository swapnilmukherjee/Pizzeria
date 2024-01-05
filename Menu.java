package cpsc4620;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * This file is where the front end magic happens.
 *
 * You will have to write the methods for each of the menu options.
 *
 * This file should not need to access your DB at all, it should make calls to the DBNinja that will do all the connections.
 *
 * You can add and remove methods as you see necessary. But you MUST have all of the menu methods (including exit!)
 *
 * Simply removing menu methods because you don't know how to implement it will result in a major error penalty (akin to your program crashing)
 *
 * Speaking of crashing. Your program shouldn't do it. Use exceptions, or if statements, or whatever it is you need to do to keep your program from breaking.
 *
 */

public class Menu {

	public static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public Menu() throws SQLException, IOException {
    }

    public static void main(String[] args) throws SQLException, IOException {

		System.out.println("Welcome to Pizzas-R-Us!");

		int menu_option = 0;

		// present a menu of options and take their selection

		PrintMenu();
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String option = reader.readLine();
		menu_option = Integer.parseInt(option);

		while (menu_option != 9) {
			switch (menu_option) {
			case 1:// enter order
				EnterOrder();
				break;
			case 2:// view customers
				viewCustomers();
				break;
			case 3:// enter customer
				EnterCustomer();
				break;
			case 4:// view order
				// open/closed/date
				ViewOrders();
				break;
			case 5:// mark order as complete
				MarkOrderAsComplete();
				break;
			case 6:// view inventory levels
				ViewInventoryLevels();
				break;
			case 7:// add to inventory
				AddInventory();
				break;
			case 8:// view reports
				PrintReports();
				break;
			}
			PrintMenu();
			option = reader.readLine();
			menu_option = Integer.parseInt(option);
		}

	}

	// allow for a new order to be placed

	//// HashMap to get order type
	static HashMap<Integer,String> orderType = new HashMap<Integer,String>();

	public static void EnterOrder() throws SQLException, IOException
	{
		orderType.put(1, DBNinja.dine_in);
		orderType.put(2, DBNinja.pickup);
		orderType.put(3, DBNinja.delivery);
		/*
		 * EnterOrder should do the following:
		 *
		 * Ask if the order is delivery, pickup, or dinein
		 *   if dine in....ask for table number
		 *   if pickup...
		 *   if delivery...
		 *
		 * Then, build the pizza(s) for the order (there's a method for this)
		 *  until there are no more pizzas for the order
		 *  add the pizzas to the order
		 *
		 * Apply order discounts as needed (including to the DB)
		 *
		 * return to menu
		 *
		 * make sure you use the prompts below in the correct order!
		 */
		String order_type = "";
		//// add try catch block

		Pizza pizza = new Pizza();
		 // User Input Prompts...
		System.out.println("Is this order for: \n1.) Dine-in\n2.) Pick-up\n3.) Delivery\nEnter the number of your choice:");
		//// input order type
		order_type = orderType.get(Integer.parseInt(reader.readLine()));
		ArrayList<Pizza> pizzaArrayList = new ArrayList<>();


		if (order_type.equals(DBNinja.dine_in)) {
			System.out.println("What is the table number for this order?");
			int tableNumber = Integer.parseInt(reader.readLine());
			System.out.println("Let's build a pizza!");
			pizza = buildPizza(DBNinja.getMaxOrderId() +1);  // fetches latest orderid and increments

			//System.out.println("adding pizza to db");
			pizzaArrayList.add(pizza);

			DineinOrder dineinOrder = new DineinOrder(DBNinja.getMaxOrderId() +1, 0,
					LocalDateTime.now().toString(), pizza.getCustPrice(), pizza.getBusPrice(), 0, tableNumber);
			dineinOrder.setOrderType(order_type);
			dineinOrder.setCustID(tableNumber);
			System.out.println("Enter -1 to stop adding pizzas...Enter anything else to continue adding pizzas to the order.");
			while(!reader.readLine().equals("-1")){
				Pizza additionalPizza = new Pizza();
				additionalPizza = buildPizza(dineinOrder.getOrderID());
				pizzaArrayList.add(additionalPizza);
				System.out.println("Enter -1 to stop adding pizzas...Enter anything else to continue adding pizzas to the order.");
			};

			System.out.println("Do you want to add discounts to this order? Enter y/n?");
			if (reader.readLine().equals("y")) {
				 addDiscounts(dineinOrder); //// add discounts to orders
			}
			DBNinja.addOrder(dineinOrder);
			pizzaArrayList.forEach(s -> {
				try {
					DBNinja.addPizza(s);
				} catch (SQLException | IOException e) {
					throw new RuntimeException(e);
				}
            });

			double[] totalOfOrder = DBNinja.getTotalBusAndCusPrice(dineinOrder.getOrderID());
			dineinOrder.setBusPrice(totalOfOrder[0]);
			dineinOrder.setCustPrice(totalOfOrder[1]);
			DBNinja.updateOrderCosts(dineinOrder);

			dineinOrder.getDiscountList().forEach(s -> {
				try {
					DBNinja.useOrderDiscount(dineinOrder,s);
				} catch (SQLException | IOException e) {
					throw new RuntimeException(e);
				}
			});

		} else if (order_type.equals(DBNinja.delivery)) {
			System.out.println("Is this order for an existing customer? Answer y/n: ");
			int customerID=0;
			StringBuilder address = new StringBuilder();
			if(reader.readLine().equals("y")){
				System.out.println("Here's a list of the current customers: ");
				viewCustomers();
				System.out.println("Which customer is this order for? Enter ID Number:");
				customerID = Integer.parseInt(reader.readLine());
			}else{
				System.out.println("Please Enter the Customer name (First Name <space> Last Name)");
				String[] nameArray = reader.readLine().split(" ");
				System.out.println("What is this Customer's phone number (##########) (no dash/space)");
				String phoneNum = reader.readLine();
				System.out.println("What is the House/Apt Number for this order? (e.g., 111)");
				address.append(reader.readLine()); address.append(" ");
				System.out.println("What is the Street for this order? (e.g., Smile Street)");
				address.append(reader.readLine()); address.append(",");
				System.out.println("What is the City for this order? (e.g., Greenville)");
				address.append(reader.readLine()); address.append(",");
				System.out.println("What is the State for this order? (e.g., SC)");
				address.append(reader.readLine()); address.append(",");
				System.out.println("What is the Zip Code for this order? (e.g., 20605)");
				address.append(reader.readLine()); address.append(",");
				Customer cus = new Customer(1,nameArray[0],nameArray[1],phoneNum);
				String[] addressArray = address.toString().split(",");
				cus.setAddress(addressArray[0],addressArray[1],addressArray[2],addressArray[3]);

				DBNinja.addCustomer(cus);
				customerID = DBNinja.getPickDelCustID(cus.getFName(),cus.getLName(),cus.getPhone());

			}
			System.out.println("Let's build a pizza!");
			pizza = buildPizza(DBNinja.getMaxOrderId() +1);  // fetches latest orderid and increments

			//System.out.println("adding pizza to db");
			pizzaArrayList.add(pizza);


			DeliveryOrder deliveryOrder = new DeliveryOrder(DBNinja.getMaxOrderId() +1, customerID,
					LocalDateTime.now().toString(), pizza.getCustPrice(), pizza.getBusPrice(), 0, address.toString());
			deliveryOrder.setOrderType(order_type);
			System.out.println("Enter -1 to stop adding pizzas...Enter anything else to continue adding pizzas to the order.");
			while(!reader.readLine().equals("-1")){
				Pizza additionalPizza = new Pizza();
				additionalPizza = buildPizza(deliveryOrder.getOrderID());
				pizzaArrayList.add(additionalPizza);
				System.out.println("Enter -1 to stop adding pizzas...Enter anything else to continue adding pizzas to the order.");
			};

			System.out.println("Do you want to add discounts to this order? Enter y/n?");
			if (reader.readLine().equals("y")) {
				addDiscounts(deliveryOrder); //// add discounts to orders
			}
			DBNinja.addOrder(deliveryOrder);
			pizzaArrayList.forEach(s -> {
				try {
					DBNinja.addPizza(s);
				} catch (SQLException | IOException e) {
					throw new RuntimeException(e);
				}
			});
			double[] totalOfOrder = DBNinja.getTotalBusAndCusPrice(deliveryOrder.getOrderID());
			deliveryOrder.setBusPrice(totalOfOrder[0]);
			deliveryOrder.setCustPrice(totalOfOrder[1]);
			DBNinja.updateOrderCosts(deliveryOrder);
			deliveryOrder.getDiscountList().forEach(s -> {
				try {
					DBNinja.useOrderDiscount(deliveryOrder,s);
				} catch (SQLException | IOException e) {
					throw new RuntimeException(e);
				}
			});

		} else if (order_type.equals(DBNinja.pickup)) {
			System.out.println("Is this order for an existing customer? Answer y/n: ");
			int customerID=0;
			if(reader.readLine().equals("y")) {
				System.out.println("Here's a list of the current customers: ");
				viewCustomers();
				System.out.println("Which customer is this order for? Enter ID Number:");
				customerID = Integer.parseInt(reader.readLine());
			}else{
				System.out.println("Please Enter the Customer name (First Name <space> Last Name)");
				String[] nameArray = reader.readLine().split(" ");
				System.out.println("What is this Customer's phone number (##########) (no dash/space)");
				String phoneNum = reader.readLine();
				Customer cus = new Customer(2,nameArray[0],nameArray[1],phoneNum);
				DBNinja.addCustomer(cus);
				customerID = DBNinja.getPickDelCustID(cus.getFName(),cus.getLName(),cus.getPhone());
			}
			//////

			System.out.println("Let's build a pizza!");
			pizza = buildPizza(DBNinja.getMaxOrderId() +1);  // fetches latest orderid and increments

			pizzaArrayList.add(pizza);


			PickupOrder pickupOrder = new PickupOrder(DBNinja.getMaxOrderId() +1, customerID,
					LocalDateTime.now().toString(), pizza.getCustPrice(), pizza.getBusPrice(), 1,0);
			pickupOrder.setOrderType(order_type);
			System.out.println("Enter -1 to stop adding pizzas...Enter anything else to continue adding pizzas to the order.");
			while(!reader.readLine().equals("-1")){
				Pizza additionalPizza = new Pizza();
				additionalPizza = buildPizza(pickupOrder.getOrderID());
				pizzaArrayList.add(additionalPizza);
				System.out.println("Enter -1 to stop adding pizzas...Enter anything else to continue adding pizzas to the order.");
			};

			System.out.println("Do you want to add discounts to this order? Enter y/n?");
			if (reader.readLine().equals("y")) {
				addDiscounts(pickupOrder); //// add discounts to orders
			}
			DBNinja.addOrder(pickupOrder);
			pizzaArrayList.forEach(s -> {
				try {
					DBNinja.addPizza(s);
				} catch (SQLException | IOException e) {
					throw new RuntimeException(e);
				}
			});
			double[] totalOfOrder = DBNinja.getTotalBusAndCusPrice(pickupOrder.getOrderID());
			pickupOrder.setBusPrice(totalOfOrder[0]);
			pickupOrder.setCustPrice(totalOfOrder[1]);
			DBNinja.updateOrderCosts(pickupOrder);
			pickupOrder.getDiscountList().forEach(s -> {
				try {
					DBNinja.useOrderDiscount(pickupOrder,s);
				} catch (SQLException | IOException e) {
					throw new RuntimeException(e);
				}
			});




			///////
		} else {
			System.out.println("ERROR: I don't understand your input for: Is this order an existing customer?");
		}

		pizzaArrayList.forEach(s-> {
			s.getToppings().forEach(t -> {
				try {
					DBNinja.useTopping(s,t,true);
				} catch (SQLException | IOException e) {
					throw new RuntimeException(e);
				}
			});
			s.getDiscounts().forEach(d -> {
				try {
					DBNinja.usePizzaDiscount(s,d);
				} catch (SQLException | IOException e) {
					throw new RuntimeException(e);
				}
            });
		});


		System.out.println("Finished adding order...Returning to menu...");

    /*
		System.out.println("Is this order for an existing customer? Answer y/n: ");
		System.out.println("Here's a list of the current customers: ");
		System.out.println("Which customer is this order for? Enter ID Number:");
		System.out.println("ERROR: I don't understand your input for: Is this order an existing customer?");
		System.out.println("What is the table number for this order?");
		System.out.println("Let's build a pizza!");
		System.out.println("Enter -1 to stop adding pizzas...Enter anything else to continue adding pizzas to the order.");

		System.out.println("Which Order Discount do you want to add? Enter the DiscountID. Enter -1 to stop adding Discounts: ");
		System.out.println("What is the House/Apt Number for this order? (e.g., 111)");
		System.out.println("What is the Street for this order? (e.g., Smile Street)");
		System.out.println("What is the City for this order? (e.g., Greenville)");
		System.out.println("What is the State for this order? (e.g., SC)");
		System.out.println("What is the Zip Code for this order? (e.g., 20605)");
*/


	}

	public static void addDiscounts(Order o) throws SQLException, IOException {
		ArrayList<Discount> currDiscounts = getDiscountList();
		o.setDiscountList(currDiscounts);
		for(Discount discount : currDiscounts){
			if(discount.isPercent()){
				double discAmount = o.getCustPrice() - (o.getCustPrice()* (discount.getAmount()/100));
				o.setCustPrice(discAmount > 0 ? discAmount : 0);
			}
			else{
				double discAmount = o.getCustPrice() - discount.getAmount();
				o.setCustPrice(discAmount > 0 ? discAmount : 0);
			}
		}
	}
	public static ArrayList<Discount> getDiscountList() throws SQLException, IOException{
		ArrayList<Discount> discountArrayList = DBNinja.getDiscountList();
		Map<Integer, Discount> discountMap = discountArrayList.stream()
				.collect(Collectors.toMap(Discount::getDiscountID, Function.identity()));

		System.out.println("Which Discount do you want to add? Enter the DiscountID. Enter -1 to stop adding Discounts: ");
		ArrayList<Discount> currDiscounts = new ArrayList<>();
		while (true) {
			discountArrayList.forEach(s -> System.out.println("DiscountID : " + s.getDiscountID() + "| " + s.getDiscountName() + ", Amount= " + s.getAmount()
					+ ", isPercent= " + s.isPercent()));
			String topS = reader.readLine();
			if (topS.equals("-1")) {
				break;
			}
			try {
				int discountID = Integer.parseInt(topS);
				Discount t = discountMap.get(discountID);
				if (t != null) {
					currDiscounts.add(t);
					System.out.println("Discount added. Do you want to add more discounts? Enter another DiscountID or -1 to stop:");
				} else {
					System.out.println("No discount found with ID: " + discountID + ". Please try again.");
				}
			} catch (NumberFormatException e) {
				System.out.println("Please enter a valid DiscountID or -1 to stop.");
			}
		}

		return currDiscounts;
	}


	public static void viewCustomers() throws SQLException, IOException
	{
		/*
		 * Simply print out all of the customers from the database.
		 */
		ArrayList<Customer> customerArrayList = DBNinja.getCustomerList();
		customerArrayList.forEach(s -> System.out.println(s.toString()));



	}


	// Enter a new customer in the database
	public static void EnterCustomer() throws SQLException, IOException
	{
		/*
		 * Ask for the name of the customer:
		 *   First Name <space> Last Name
		 *
		 * Ask for the  phone number.
		 *   (##########) (No dash/space)
		 *
		 * Once you get the name and phone number, add it to the DB
		 */
		StringBuilder address = new StringBuilder();
		System.out.println("Please Enter the Customer name (First Name <space> Last Name)");
		String[] nameArray = reader.readLine().split(" ");
		System.out.println("What is this Customer's phone number (##########) (no dash/space)");
		String phoneNum = reader.readLine();
		System.out.println("What is the House/Apt Number for this order? (e.g., 111)");
		address.append(reader.readLine()); address.append(" ");
		System.out.println("What is the Street for this order? (e.g., Smile Street)");
		address.append(reader.readLine()); address.append(",");
		System.out.println("What is the City for this order? (e.g., Greenville)");
		address.append(reader.readLine()); address.append(",");
		System.out.println("What is the State for this order? (e.g., SC)");
		address.append(reader.readLine()); address.append(",");
		System.out.println("What is the Zip Code for this order? (e.g., 20605)");
		address.append(reader.readLine()); address.append(",");
		Customer cus = new Customer(1,nameArray[0],nameArray[1],phoneNum);
		String[] addressArray = address.toString().split(",");
		cus.setAddress(addressArray[0],addressArray[1],addressArray[2],addressArray[3]);

		DBNinja.addCustomer(cus);

		// User Input Prompts...
//		 System.out.println("What is this customer's name (first <space> last");
//		 System.out.println("What is this customer's phone number (##########) (No dash/space)");


	}

	// View any orders that are not marked as completed
	public static void ViewOrders() throws SQLException, IOException
	{
		/*
		* This method allows the user to select between three different views of the Order history:
		* The program must display:
		* a.	all open orders
		* b.	all completed orders
		* c.	all the orders (open and completed) since a specific date (inclusive)
		*
		* After displaying the list of orders (in a condensed format) must allow the user to select a specific order for viewing its details.
		* The details include the full order type information, the pizza information (including pizza discounts), and the order discounts.
		*
		*/
		System.out.println("Would you like to:\n(a) display all orders [open or closed]\n(b) display all open orders\n(c) display all completed [closed] orders\n(d) display orders since a specific date");

		String inputLine = reader.readLine(); // Read the line once and store it

		if (inputLine.equals("a")) {
			getAllPizzaDetails("null");
		} else if (inputLine.equals("b")) {
			getPizzaDetails(false);
		} else if (inputLine.equals("c")) {
			getPizzaDetails(true);
		} else if (inputLine.equals("d")) {
			System.out.println("What is the date you want to restrict by? (FORMAT= YYYY-MM-DD)");
			getAllPizzaDetails(reader.readLine()); // Only here we read a new line for additional input
		}else{
			System.out.println("Incorrect entry, returning to menu.");
		}

		// User Input Prompts...

		//System.out.println("I don't understand that input, returning to menu");


		//System.out.println("No orders to display, returning to menu.");



	}

	public static void getAllPizzaDetails(String dateRestrict) throws SQLException, IOException {
		List<Order> orders = DBNinja.getAllOrders(dateRestrict);
		if (!orders.isEmpty()) {
			orders.forEach(order -> System.out.println(order.toSimplePrint()));
			System.out.println("Which order would you like to see in detail? Enter the number (-1 to exit): ");
			int orderID = Integer.parseInt(reader.readLine());
			if (orderID != -1){
				StringBuilder ans = new StringBuilder(DBNinja.getOrderById(orderID).toString());
				if(DBNinja.getOrderById(orderID).getOrderType().toLowerCase().contains("dine")){
					ans.append("| Customer was sat at table number ").append(DBNinja.getTableNumber(orderID));
				}
				ans.append("\n");
				ans.append(DBNinja.getDiscountByOrderID(orderID));
				ans.append("\n");
				DBNinja.getPizzaDetails(orderID).forEach(s -> {
					ans.append(s.toString());
					ans.append("\n");
					try {
						ans.append(DBNinja.getDiscountByPizzaID(s.getPizzaID()));
					} catch (SQLException e) {
						throw new RuntimeException(e);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
				System.out.println(ans);
			}
		}else {
			System.out.println("No orders to display, returning to menu.");
		}
	}

	public static void getPizzaDetails(boolean isComplete) throws IOException, SQLException {
		DBNinja.getOrders(isComplete).forEach(s -> System.out.println(s.toSimplePrint()));
		System.out.println("Which order would you like to see in detail? Enter the number (-1 to exit): ");
		int orderID = Integer.parseInt(reader.readLine());
		if (orderID != -1){
			StringBuilder ans = new StringBuilder(DBNinja.getOrderById(orderID).toString());
			if(DBNinja.getOrderById(orderID).getOrderType().toLowerCase().contains("dine")){
				ans.append("| Customer was sat at table number ").append(DBNinja.getTableNumber(orderID));
			}
			ans.append("\n");
			ans.append(DBNinja.getDiscountByOrderID(orderID));
			ans.append("\n");
			DBNinja.getPizzaDetails(orderID).forEach(s -> {
				ans.append(s.toString());
				ans.append("\n");
				try {
					ans.append(DBNinja.getDiscountByPizzaID(s.getPizzaID()));
				} catch (SQLException e) {
					throw new RuntimeException(e);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			System.out.println(ans);


		}
	}


	// When an order is completed, we need to make sure it is marked as complete
	public static void MarkOrderAsComplete() throws SQLException, IOException
	{
		/*
		 * All orders that are created through java (part 3, not the orders from part 2) should start as incomplete
		 *
		 * When this method is called, you should print all of the "opoen" orders marked
		 * and allow the user to choose which of the incomplete orders they wish to mark as complete
		 *
		 */



		// User Input Prompts...
		//System.out.println("There are no open orders currently... returning to menu...");
		Order order = new Order();
		DBNinja.getOrders(false).forEach(s -> System.out.println(s.toSimplePrint()));
		System.out.println("Which order would you like mark as complete? Enter the OrderID: ");
		order.setOrderID(Integer.parseInt(reader.readLine()));
		DBNinja.completeOrder(order);
		System.out.println("Incorrect entry, not an option");




	}

	public static void ViewInventoryLevels() throws SQLException, IOException
	{
		/*
		 * Print the inventory. Display the topping ID, name, and current inventory
		*/

		DBNinja.printInventory();


	}


	public static void AddInventory() {
		try {
			/*
			 * This should print the current inventory and then ask the user which topping (by ID) they want to add more to and how much to add
			 */

			DBNinja.printInventory();
			System.out.println("Which topping do you want to add inventory to? Enter the number: ");
			int inventoryID = Integer.parseInt(reader.readLine());
			System.out.println("How many units would you like to add? ");
			double inventoryAmt = Double.parseDouble(reader.readLine());
			Topping topping = new Topping();
			topping.setTopID(inventoryID);
			DBNinja.addToInventory(topping, inventoryAmt);

			// Additional logic...

		} catch (NumberFormatException e) {
			System.out.println("Incorrect entry, not an option");
			// You might also want to log the exception or perform other actions
		} catch (SQLException e) {
			System.out.println("Database error occurred: " + e.getMessage());
			// Additional error handling...
		} catch (IOException e) {
			System.out.println("Input/output error occurred: " + e.getMessage());
			// Additional error handling...
		} catch (Exception e) {
			System.out.println("An unexpected error occurred: " + e.getMessage());
			// Additional error handling...
		}
	}


	static ArrayList<Topping> toppingList;

	static {
		toppingList = DBNinja.getToppingList();
	}

	// A method that builds a pizza. Used in our add new order method
	public static Pizza buildPizza(int orderID) throws SQLException, IOException
	{

		/*
		 * This is a helper method for first menu option.
		 *
		 * It should ask which size pizza the user wants and the crustType.
		 *
		 * Once the pizza is created, it should be added to the DB.
		 *
		 * We also need to add toppings to the pizza. (Which means we not only need to add toppings here, but also our bridge table)
		 *
		 * We then need to add pizza discounts (again, to here and to the database)
		 *
		 * Once the discounts are added, we can return the pizza
		 */
		HashMap<String,String> pizzaSize = new HashMap<>();
		pizzaSize.put("1",DBNinja.size_s);
		pizzaSize.put("2",DBNinja.size_m);
		pizzaSize.put("3",DBNinja.size_l);
		pizzaSize.put("4",DBNinja.size_xl);

		HashMap<String,String> pizzaCrust = new HashMap<>();
		pizzaCrust.put("1",DBNinja.crust_thin);
		pizzaCrust.put("2",DBNinja.crust_orig);
		pizzaCrust.put("3",DBNinja.crust_pan);
		pizzaCrust.put("4",DBNinja.crust_gf);

		String date = String.valueOf(LocalDateTime.now());

		Pizza pizza = new Pizza();
		ArrayList<Topping> currToppings = new ArrayList<>();
		ArrayList<Discount> currDiscounts = new ArrayList<>();

		pizza.setOrderID(orderID);

		// User Input Prompts...
		System.out.println("What size is the pizza?");
		System.out.println("1."+DBNinja.size_s);
		System.out.println("2."+DBNinja.size_m);
		System.out.println("3."+DBNinja.size_l);
		System.out.println("4."+DBNinja.size_xl);
		//// read pizza size, fetch from map, set in pizza object
		System.out.println("Enter the corresponding number: ");
		pizza.setSize(pizzaSize.get(reader.readLine()));

		System.out.println("What crust for this pizza?");
		System.out.println("1."+DBNinja.crust_thin);
		System.out.println("2."+DBNinja.crust_orig);
		System.out.println("3."+DBNinja.crust_pan);
		System.out.println("4."+DBNinja.crust_gf);
		System.out.println("Enter the corresponding number: ");
		pizza.setCrustType(pizzaCrust.get(reader.readLine()));
		pizza.setBusPrice(DBNinja.getBaseBusPrice(pizza.getSize(),pizza.getCrustType()));
		pizza.setCustPrice(DBNinja.getBaseCustPrice(pizza.getSize(),pizza.getCrustType()));

		System.out.println("Available Toppings:");

		System.out.println("ID    Name			CurINVT");
		toppingList.forEach(s -> System.out.println( s.getTopID() + "    " + s.getTopName() + "			" + s.getCurINVT()));

		System.out.println("Which topping do you want to add? Enter the TopID. Enter -1 to stop adding toppings: ");
		int top = Integer.parseInt(reader.readLine());
		if(top != -1){
			for(Topping t : toppingList){
				if(t.getTopID() == top){  //// topping id is in list
					double topUnit = pizza.getSize().equals(DBNinja.size_s) ? t.getPerAMT() :
							(pizza.getSize().equals(DBNinja.size_m) ? t.getMedAMT() :
									(pizza.getSize().equals(DBNinja.size_l) ? t.getLgAMT() : t.getXLAMT()));

					if(t.getCurINVT() - topUnit >= 0){
						currToppings.add(t);
						pizza.setCustPrice(pizza.getCustPrice() + (t.getCustPrice() * topUnit));
						pizza.setBusPrice(pizza.getBusPrice() + (t.getBusPrice() * topUnit));
						t.setCurINVT((int) (t.getCurINVT() - topUnit));
						//DBNinja.useTopping(pizza,t,true);
					}else{
						System.out.println("We don't have enough of that topping to add it...");
					}
					break;
				}
			}
		}

		System.out.println("Do you want to add extra topping? Enter y/n");
		if (reader.readLine().equals("y")) {
			Map<Integer, Topping> toppingMap = toppingList.stream()
					.collect(Collectors.toMap(Topping::getTopID, Function.identity()));
			printToppingList();

			System.out.println("Which topping do you want to add? Enter the TopID. Enter -1 to stop adding toppings: ");
			while (true) {
				top = Integer.parseInt(reader.readLine());
				if (top == -1) {
					break;
				}
				Topping t = toppingMap.get(top);
				if (t != null) {
					double topUnit = getToppingAmount(pizza, t);
					if (t.getCurINVT() - topUnit >= 0) {
						currToppings.add(t);
						pizza.setCustPrice(pizza.getCustPrice() + (t.getCustPrice() * topUnit));
						pizza.setBusPrice(pizza.getBusPrice() + (t.getBusPrice() * topUnit));
						// Assuming you update the inventory here
						t.setCurINVT((int) (t.getCurINVT() - topUnit));
						//DBNinja.useTopping(pizza,t,true);

					} else {
						System.out.println("We don't have enough of that topping to add it...");
					}
				}
				System.out.println("Which topping do you want to add? Enter the TopID. Enter -1 to stop adding toppings: ");
			}
		}

		pizza.setToppings(currToppings);

		System.out.println("Do you want to add discounts to this Pizza? Enter y/n?");
		if (reader.readLine().equals("y")) {
			ArrayList<Discount> discountArrayList = DBNinja.getDiscountList();
			Map<Integer, Discount> discountMap = discountArrayList.stream()
					.collect(Collectors.toMap(Discount::getDiscountID, Function.identity()));

			System.out.println("Which Pizza Discount do you want to add? Enter the DiscountID. Enter -1 to stop adding Discounts: ");
			while (true) {
				discountArrayList.forEach(s -> System.out.println("DiscountID : " + s.getDiscountID() + "| " + s.getDiscountName() + ", Amount= " + s.getAmount()
						+ ", isPercent= " + s.isPercent()));
				String topS = reader.readLine();
				if (topS.equals("-1")) {
					break;
				}
				try {
					int discountID = Integer.parseInt(topS);
					Discount t = discountMap.get(discountID);
					if (t != null) {
						currDiscounts.add(t);
						System.out.println("Discount added. Do you want to add more discounts? Enter another DiscountID or -1 to stop:");
					} else {
						System.out.println("No discount found with ID: " + discountID + ". Please try again.");
					}
				} catch (NumberFormatException e) {
					System.out.println("Please enter a valid DiscountID or -1 to stop.");
				}
			}
		}
		pizza.setDiscounts(currDiscounts);

		for(Discount discount : currDiscounts){
			//DBNinja.usePizzaDiscount(pizza,discount);
			if(discount.isPercent()){
				double discAmount = pizza.getCustPrice() - (pizza.getCustPrice()* (discount.getAmount()/100));
				pizza.setCustPrice(discAmount > 0 ? discAmount : 0);
			}
			else{
				double discAmount = pizza.getCustPrice() - discount.getAmount();
				pizza.setCustPrice(discAmount > 0 ? discAmount : 0);
			}
		}


		return pizza;
	}

	private static void printToppingList() {
		toppingList.forEach(s -> System.out.println(s.getTopID() + "    " + s.getTopName() + "            " + s.getCurINVT()));
	}

	private static double getToppingAmount(Pizza pizza, Topping t) {
		switch (pizza.getSize()) {
			case DBNinja.size_s:
				return t.getPerAMT();
			case DBNinja.size_m:
				return t.getMedAMT();
			case DBNinja.size_l:
				return t.getLgAMT();
			default:
				return t.getXLAMT();
		}
	}

	public static void PrintReports() throws SQLException, NumberFormatException, IOException
	{
		/*
		 * This method asks the use which report they want to see and calls the DBNinja method to print the appropriate report.
		 *
		 */

		// User Input Prompts...
		System.out.println("Which report do you wish to print? Enter\n(a) ToppingPopularity\n(b) ProfitByPizza\n(c) ProfitByOrderType:");
		String option = reader.readLine();

		if (option.equalsIgnoreCase("a")){
			DBNinja.printToppingPopReport();
		}else if(option.equalsIgnoreCase("b")){
			DBNinja.printProfitByPizzaReport();
		}else if(option.equalsIgnoreCase("c")){
			DBNinja.printProfitByOrderType();
		}else{
			System.out.println("I don't understand that input... returning to menu...");
		}



	}

	//Prompt - NO CODE SHOULD TAKE PLACE BELOW THIS LINE
	// DO NOT EDIT ANYTHING BELOW HERE, THIS IS NEEDED TESTING.
	// IF YOU EDIT SOMETHING BELOW, IT BREAKS THE AUTOGRADER WHICH MEANS YOUR GRADE WILL BE A 0 (zero)!!

	public static void PrintMenu() {
		System.out.println("\n\nPlease enter a menu option:");
		System.out.println("1. Enter a new order");
		System.out.println("2. View Customers ");
		System.out.println("3. Enter a new Customer ");
		System.out.println("4. View orders");
		System.out.println("5. Mark an order as completed");
		System.out.println("6. View Inventory Levels");
		System.out.println("7. Add Inventory");
		System.out.println("8. View Reports");
		System.out.println("9. Exit\n\n");
		System.out.println("Enter your option: ");
	}

	/*
	 * autograder controls....do not modiify!
	 */

	public final static String autograder_seed = "6f1b7ea9aac470402d48f7916ea6a010";


	private static void autograder_compilation_check() {

		try {
			Order o = null;
			Pizza p = null;
			Topping t = null;
			Discount d = null;
			Customer c = null;
			ArrayList<Order> alo = null;
			ArrayList<Discount> ald = null;
			ArrayList<Customer> alc = null;
			ArrayList<Topping> alt = null;
			double v = 0.0;
			String s = "";

			DBNinja.addOrder(o);
			DBNinja.addPizza(p);
			DBNinja.useTopping(p, t, false);
			DBNinja.usePizzaDiscount(p, d);
			DBNinja.useOrderDiscount(o, d);
			DBNinja.addCustomer(c);
			DBNinja.completeOrder(o);
			alo = DBNinja.getOrders(false);
			o = DBNinja.getLastOrder();
			alo = DBNinja.getOrdersByDate("01/01/1999");
			ald = DBNinja.getDiscountList();
			d = DBNinja.findDiscountByName("Discount");
			alc = DBNinja.getCustomerList();
			c = DBNinja.findCustomerByPhone("0000000000");
			alt = DBNinja.getToppingList();
			t = DBNinja.findToppingByName("Topping");
			DBNinja.addToInventory(t, 1000.0);
			v = DBNinja.getBaseCustPrice("size", "crust");
			v = DBNinja.getBaseBusPrice("size", "crust");
			DBNinja.printInventory();
			DBNinja.printToppingPopReport();
			DBNinja.printProfitByPizzaReport();
			DBNinja.printProfitByOrderType();
			s = DBNinja.getCustomerName(0);
		} catch (SQLException | IOException e) {
			e.printStackTrace();
		}
	}


}


