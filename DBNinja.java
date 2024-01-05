package cpsc4620;

import com.mysql.cj.xdevapi.Result;

import javax.print.DocFlavor;
import java.awt.image.AreaAveragingScaleFilter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Date;

/*
 * This file is where most of your code changes will occur You will write the code to retrieve
 * information from the database, or save information to the database
 * 
 * The class has several hard coded static variables used for the connection, you will need to
 * change those to your connection information
 * 
 * This class also has static string variables for pickup, delivery and dine-in. If your database
 * stores the strings differently (i.e "pick-up" vs "pickup") changing these static variables will
 * ensure that the comparison is checking for the right string in other places in the program. You
 * will also need to use these strings if you store this as boolean fields or an integer.
 * 
 * 
 */

/**
 * A utility class to help add and retrieve information from the database
 */

public final class DBNinja {
	private static Connection conn;

	// Change these variables to however you record dine-in, pick-up and delivery, and sizes and crusts
	public final static String pickup = "pickup";
	public final static String delivery = "delivery";
	public final static String dine_in = "dinein";

	public final static String size_s = "Small";
	public final static String size_m = "Medium";
	public final static String size_l = "Large";
	public final static String size_xl = "XLarge";

	public final static String crust_thin = "Thin";
	public final static String crust_orig = "Original";
	public final static String crust_pan = "Pan";
	public final static String crust_gf = "Gluten-Free";

	public final static String state_ordered = "Ordered";
	public final static String state_completed = "Completed";



	
	private static boolean connect_to_db() throws SQLException, IOException {

		try {
			conn = DBConnector.make_connection();
			return true;
		} catch (SQLException e) {
			return false;
		} catch (IOException e) {
			return false;
		}

	}

	public static int getPickDelCustID(String Fname, String LName, String phone) throws SQLException, IOException {
		connect_to_db();
		int custID = 0;

		try (PreparedStatement st = conn.prepareStatement("SELECT CustomerID FROM customer WHERE CustomerFirstName = ? and " +
				"CustomerLastName = ? and CustomerPhone = ? ")) {
			st.setString(1,Fname);
			st.setString(2,LName);st.setString(3,phone);

			ResultSet rs = st.executeQuery();
			if (rs.next()) {  // Assuming you need the first or only customer with this name
				custID = rs.getInt(1);
			}
		} finally {
			// Close the connection if it should be closed here. This depends on connect_to_db() implementation.
			conn.close();
		}

		return custID;
	}
	public static int getDineinCustID() throws SQLException, IOException {
		int custID = 0;

		try (PreparedStatement st = conn.prepareStatement("SELECT CustomerID FROM customer WHERE CustomerFirstName = 'dinein'")) {
			ResultSet rs = st.executeQuery();
			if (rs.next()) {  // Assuming you need the first or only customer with this name
				custID = rs.getInt(1);
			}
		} finally {
			// Close the connection if it should be closed here. This depends on connect_to_db() implementation.
			 //conn.close();
		}

		return custID;
	}

	public static void updateOrderCosts(Order o) throws SQLException, IOException{
		connect_to_db();
		PreparedStatement st = conn.prepareStatement("update orders set OrderCost= ?,OrderPrice= ? where OrderID = ?");
		st.setDouble(1,o.getBusPrice());
		st.setDouble(2,o.getCustPrice());
		st.setDouble(3,o.getOrderID());

		st.executeUpdate();
		conn.close();

	}

	public static void addOrder(Order o) throws SQLException, IOException 
	{
		try {
			connect_to_db();
			conn.setAutoCommit(false);
			//// dine in customer doesn't have ID, to bypass foreign key constraint we use this condition
				if(o.getOrderType().equals(dine_in)) {
					PreparedStatement din = conn.prepareStatement("INSERT INTO customer VALUES ('0'," +
							"'dinein','dinein','000-000-0000','dinein','dinein','dinein','00000')");
					din.executeUpdate();
				}
				LocalDateTime dateTime = LocalDateTime.parse(o.getDate());
				PreparedStatement st = conn.prepareStatement("insert into orders values (?,?,?,?,?,?,?,?)");
				st.setInt(1,o.getOrderID());
				st.setInt(2, !o.getOrderType().equals(dine_in)?o.getCustID(): getDineinCustID());
				st.setString(3,dateTime.toLocalTime().toString());
				st.setString(4,o.getIsComplete()==1?"Completed":"Preparing");
				st.setString(5,dateTime.toLocalDate().toString());
				st.setString(6,o.getOrderType());
				st.setDouble(7,o.getBusPrice());
				st.setDouble(8,o.getCustPrice());
				st.executeUpdate();
				if(o.getOrderType().equals(dine_in)){
					PreparedStatement st1 = conn.prepareStatement("insert into dinein(OrderID,TableNumber) values (?,?)");
					st1.setInt(1,o.getOrderID());
					st1.setInt(2,o.getCustID());
					st1.executeUpdate();
				}else{
					String orderType = o.getOrderType();
					try(PreparedStatement st1 = conn.prepareStatement("insert into " + orderType +" (OrderID,CustomerID) values (?,?)")){
						st1.setInt(1,o.getOrderID());
						st1.setInt(2,o.getCustID());
						st1.executeUpdate();
					}catch (Exception e){
						e.printStackTrace();
					}

				}


				conn.commit();
			System.out.println("executed order in table");

		}catch (Exception e){
			e.printStackTrace();
		}finally {
			conn.setAutoCommit(true);
			conn.close();
		}
		/*
		 * add code to add the order to the DB. Remember that we're not just
		 * adding the order to the order DB table, but we're also recording
		 * the necessary data for the delivery, dinein, and pickup tables
		 * 
		 */
	

		
		//DO NOT FORGET TO CLOSE YOUR CONNECTION
	}
	
	public static void addPizza(Pizza p) throws SQLException, IOException
	{
		connect_to_db();
		int pizzaAttributeID = 0;
		PreparedStatement st = conn.prepareStatement("select PizzaAttributesID from pizzaattributes\n" +
				"where Size= ? and Crust = ?");
		st.setString(1,p.getSize());
		st.setString(2,p.getCrustType());
		ResultSet rs = st.executeQuery();
		while (rs.next()){
			pizzaAttributeID = rs.findColumn("PizzaAttributesID");
		}

		PreparedStatement st1 = conn.prepareStatement("insert into pizza (OrderID, PizzaAttributesID, PizzaPrice, PizzaCost) values (?,?,?,?)",
				Statement.RETURN_GENERATED_KEYS);
		st1.setInt(1,p.getOrderID());
		st1.setInt(2,pizzaAttributeID);
		st1.setDouble(3,p.getCustPrice());
		st1.setDouble(4,p.getBusPrice());

		st1.executeUpdate();

		ResultSet generatedKeys = st1.getGeneratedKeys();
		int pizzaID = -1; // Default value if no key was generated
		if (generatedKeys.next()) {
			pizzaID = generatedKeys.getInt(1); // Retrieve the generated PizzaID
			p.setPizzaID(pizzaID); // Set the PizzaID in the Pizza object
		} else {
			// Handle the case where the ID wasn't generated
			throw new SQLException("Failed to retrieve generated PizzaID");
		}


		conn.close();
		//DO NOT FORGET TO CLOSE YOUR CONNECTION
	}
	
	
	public static void useTopping(Pizza p, Topping t, boolean isDoubled) throws SQLException, IOException //this method will update toppings inventory in SQL and add entities to the Pizzatops table. Pass in the p pizza that is using t topping
	{
		connect_to_db();
		/*
		 * This method should do 2 two things.
		 * - update the topping inventory every time we use t topping (accounting for extra toppings as well)
		 * - connect the topping to the pizza
		 *   What that means will be specific to your yimplementatinon.
		 * 
		 * Ideally, you should't let toppings go negative....but this should be dealt with BEFORE calling this method.
		 * 
		 */
		PreparedStatement ps = conn.prepareStatement("insert into pizzatopping values (?,?,?)");
		ps.setInt(1, p.getPizzaID());
		ps.setInt(2, t.getTopID());
		ps.setBoolean(3, isDoubled);

		ps.executeUpdate();

		//update inventory
		PreparedStatement ps1 = conn.prepareStatement("update topping set ToppingInventory = ? where ToppingID = ? ");
		ps1.setDouble(1,t.getCurINVT());
		ps1.setInt(2,t.getTopID());
		ps1.executeUpdate();

		conn.close();
		
		//DO NOT FORGET TO CLOSE YOUR CONNECTION
	}
	
	
	public static void usePizzaDiscount(Pizza p, Discount d) throws SQLException, IOException
	{
		connect_to_db();
		/*
		 * This method connects a discount with a Pizza in the database.
		 * 
		 * What that means will be specific to your implementatinon.
		 */


		PreparedStatement ps = conn.prepareStatement("insert into pizzadiscount values (?,?)");
		ps.setInt(1,p.getPizzaID());
		ps.setInt(2,d.getDiscountID());
		ps.executeUpdate();
		conn.close();
		//DO NOT FORGET TO CLOSE YOUR CONNECTION
	}
	
	public static void useOrderDiscount(Order o, Discount d) throws SQLException, IOException
	{
		connect_to_db();
		/*
		 * This method connects a discount with an order in the database
		 * 
		 * You might use this, you might not depending on where / how to want to update
		 * this information in the dabast
		 */
		
		PreparedStatement ps = conn.prepareStatement("insert into orderdiscount values (?,?)");
		ps.setInt(1,o.getOrderID());
		ps.setInt(2,d.getDiscountID());
		ps.executeUpdate();
		conn.close();
		
		
		
		//DO NOT FORGET TO CLOSE YOUR CONNECTION
	}
	
	public static void addCustomer(Customer c) throws SQLException, IOException {
		connect_to_db();
		/*
		 * This method adds a new customer to the database.
		 * 
		 */
		Statement st = conn.createStatement();
		int custID = 0;
		ResultSet rs = st.executeQuery("select max(CustomerID) + 1 as max  from customer");
		while (rs.next()){
			custID = rs.getInt(1);
		}


		PreparedStatement ps  = conn.prepareStatement("INSERT INTO customer VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
		ps.setInt(1,custID);
		ps.setString(2,c.getFName());
		ps.setString(3,c.getLName());
		ps.setString(4,c.getPhone());
		if(c.getAddress() != null && !c.getAddress().isEmpty()){
			String[] addressArray = c.getAddress().split("/n");
			ps.setString(5,addressArray[0]);
			ps.setString(6,addressArray[1]);
			ps.setString(7,addressArray[2]);
			ps.setString(8,addressArray[3]);
		}else{
			ps.setString(5,"");
			ps.setString(6,"");
			ps.setString(7,"");
			ps.setString(8,"");
		}

		ps.execute();
		
		conn.close();
		//DO NOT FORGET TO CLOSE YOUR CONNECTION
	}

	public static void completeOrder(Order o) throws SQLException, IOException {
		connect_to_db();
		/*
		 * Find the specifed order in the database and mark that order as complete in the database.
		 * 
		 */

		PreparedStatement ps = conn.prepareStatement("update orders set OrderStatus = 'Completed' where OrderID = ?");
		ps.setInt(1, o.getOrderID());
		ps.executeUpdate();

		conn.close();

		//DO NOT FORGET TO CLOSE YOUR CONNECTION
	}

	public static int getMaxOrderId() throws SQLException,IOException{
		connect_to_db();
		int maxOrderId=0;
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery("select max(OrderID) from orders");
		if(rs.next()){
			maxOrderId=rs.getInt(1);
		}

		conn.close();
		return maxOrderId;
	}

	public static Order getOrderById(int orderID) throws SQLException, IOException {
		connect_to_db();
		PreparedStatement ps = conn.prepareStatement("select * from orders where OrderID = ?");
		ps.setInt(1,orderID);
		ResultSet rs = ps.executeQuery();
		Order order = new Order();
		while (rs.next()){
			order.setOrderID(orderID);
			order.setDate(rs.getString("OrderDate"));
			order.setCustID(rs.getInt("CustomerID"));
			order.setOrderType(rs.getString("OrderType"));
			order.setCustPrice(rs.getDouble("OrderPrice"));
			order.setBusPrice(rs.getDouble("OrderCost"));
		}

		conn.close();

		return order;
	}
	public static ArrayList<Order> getAllOrders(String dateRestrict) throws SQLException, IOException {
		connect_to_db();
		/*
		 * Return an arraylist of all of the orders.
		 * 	openOnly == true => only return a list of open (ie orders that have not been marked as completed)
		 *           == false => return a list of all the orders in the database
		 * Remember that in Java, we account for supertypes and subtypes
		 * which means that when we create an arrayList of orders, that really
		 * means we have an arrayList of dineinOrders, deliveryOrders, and pickupOrders.
		 *
		 * Don't forget to order the data coming from the database appropriately.
		 *
		 */
		StringBuilder query = new StringBuilder("select * from orders");
		if(!dateRestrict.equals("null")){
			query.append(" where OrderDate >= ");
			query.append("'").append(dateRestrict).append("'");
		}
//		System.out.println(query);
		PreparedStatement ps = conn.prepareStatement(String.valueOf(query));
		ArrayList<Order> orderArrayList = new ArrayList<>();
		ResultSet rs = ps.executeQuery();
		while(rs.next()){
			int isComplete = rs.getString("OrderStatus").equals("Completed")?1:0;
			Order order = new Order(rs.getInt("OrderID"),
					rs.getInt("CustomerID"),
					rs.getString("OrderType"),
					rs.getString("OrderDate"),
					rs.getDouble("OrderPrice"),
					rs.getDouble("OrderCost"),
					isComplete);
			orderArrayList.add(order);
		}

		conn.close();
		//DO NOT FORGET TO CLOSE YOUR CONNECTION
		return orderArrayList;
	}



	public static ArrayList<Order> getOrders(boolean openOnly) throws SQLException, IOException {
		connect_to_db();
		/*
		 * Return an arraylist of all of the orders.
		 * 	openOnly == true => only return a list of open (ie orders that have not been marked as completed)
		 *           == false => return a list of all the orders in the database
		 * Remember that in Java, we account for supertypes and subtypes
		 * which means that when we create an arrayList of orders, that really
		 * means we have an arrayList of dineinOrders, deliveryOrders, and pickupOrders.
		 * 
		 * Don't forget to order the data coming from the database appropriately.
		 * 
		 */
		String query = "select * from orders";
		if (openOnly) {
			query += " where OrderStatus = 'Completed'";
		}else {
			query += " where OrderStatus = 'Preparing'";
		}
		PreparedStatement ps = conn.prepareStatement(query);
		ArrayList<Order> orderArrayList = new ArrayList<>();
		ResultSet rs = ps.executeQuery();
		while(rs.next()){
			int isComplete = rs.getString("OrderStatus").equals("Completed")?1:0;
			Order order = new Order(rs.getInt("OrderID"),
					rs.getInt("CustomerID"),
					rs.getString("OrderType"),
					rs.getString("OrderDate"),
					rs.getDouble("OrderPrice"),
					rs.getDouble("OrderCost"),
					isComplete);
			orderArrayList.add(order);
		}
			conn.close();
		//DO NOT FORGET TO CLOSE YOUR CONNECTION
		return orderArrayList;
	}
	
	public static Order getLastOrder() throws SQLException, IOException {
		/*
		 * Query the database for the LAST order added
		 * then return an Order object for that order.
		 * NOTE...there should ALWAYS be a "last order"!
		 */
		connect_to_db();
		PreparedStatement ps = conn.prepareStatement("SELECT *\n" +
				"FROM orders\n" +
				"ORDER BY order_id_column DESC\n" +
				"LIMIT 1;\n");
		ResultSet rs = ps.executeQuery();
		Order order = null;
		while (rs.next()){
			order = new Order();
			order.setOrderID(rs.findColumn("OrderID"));
			order.setCustID(rs.findColumn("CustomerID"));
			String orderStatus = rs.getString("OrderStatus");
			order.setIsComplete(orderStatus != null && orderStatus.equals("Complete") ? 1 : 0);
			order.setDate(String.valueOf(rs.findColumn("OrderDate")));
			order.setBusPrice(rs.getDouble(7));
			order.setCustPrice(rs.getDouble(8));
		}

		 return order;
	}

	public static ArrayList<Order> getOrdersByDate(String date) throws SQLException, IOException {
		/*
		 * Query the database for ALL the orders placed on a specific date
		 * and return a list of those orders.
		 *  
		 */
		connect_to_db();
		ArrayList<Order> orders = new ArrayList<>();
		PreparedStatement ps = conn.prepareStatement("select * from orders where OrderDate = ?");
		ps.setString(1,date);
		ResultSet rs = ps.executeQuery();
		while (rs.next()){
			Order order = new Order();
			order.setOrderID(rs.findColumn("OrderID"));
			order.setCustID(rs.findColumn("CustomerID"));
			String orderStatus = rs.getString("OrderStatus");
			order.setIsComplete(orderStatus != null && orderStatus.equals("Complete") ? 1 : 0);
			order.setDate(String.valueOf(rs.findColumn("OrderDate")));
			order.setBusPrice(rs.getDouble(7));
			order.setCustPrice(rs.getDouble(8));

			orders.add(order);
		}

		 return orders;
	}

	public static ArrayList<Pizza> getPizzaDetails(int orderID) throws SQLException, IOException{
		connect_to_db();
		PreparedStatement ps = conn.prepareStatement("select PizzaID,Crust,Size,pizza.OrderID,OrderStatus,OrderDate,PizzaPrice,PizzaCost from pizza\n" +
				"\t\tinner join pizzaattributes on pizza.PizzaAttributesID = pizzaattributes.PizzaAttributesID\n" +
				"\t\tinner join orders on pizza.OrderID = orders.OrderID\n" +
				"where orders.OrderID = ?");
		ps.setInt(1,orderID);
		ResultSet rs = ps.executeQuery();
		ArrayList<Pizza> pizzas= new ArrayList<>();
		while (rs.next()){
			Pizza pizza = new Pizza();
			pizza.setPizzaID(rs.getInt(1));
			pizza.setCrustType(rs.getString(2));
			pizza.setSize(rs.getString(3));
			pizza.setOrderID(rs.getInt(4));
			pizza.setPizzaState(rs.getString(5));
			pizza.setPizzaDate(rs.getString(6));
			pizza.setCustPrice(rs.getDouble(7));
			pizza.setBusPrice(rs.getDouble(8));
			pizzas.add(pizza);
		}

		conn.close();
		return pizzas;
	}
		
	public static ArrayList<Discount> getDiscountList() throws SQLException, IOException {
		ArrayList<Discount> discountList = new ArrayList<>();

		// Use try-with-resources for automatic resource management
		try {
			connect_to_db();
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("select DiscountID,\n" +
					"       DiscountName,\n" +
					"       DiscountType,\n" +
					"       if(DiscountType='Percentage',DiscountPercentage,DiscountAmount) as DiscountAmount\n" +
					"from discount");

			while (rs.next()) {
				Discount discount = new Discount();
				discount.setDiscountID(rs.getInt(1));
				discount.setDiscountName(rs.getString(2));
				discount.setPercent(rs.getString(3).equals("Percentage"));
				discount.setAmount(rs.getDouble(4));

				discountList.add(discount);
			}
		} catch (SQLException | IOException e) {
			e.printStackTrace(); // Or handle the exception as per your error handling policy
		} finally {
			try {
				if (conn != null && !conn.isClosed()) {
					conn.close();
				}
			} catch (SQLException e) {
				e.printStackTrace(); // Or handle the exception as per your error handling policy
			}
		}

		return discountList;

	}

	public static Discount findDiscountByName(String name) throws SQLException, IOException {
		/*
		 * Query the database for a discount using it's name.
		 * If found, then return an OrderDiscount object for the discount.
		 * If it's not found....then return null
		 *  
		 */
		connect_to_db();
		PreparedStatement ps = conn.prepareStatement("SELECT * FROM discount WHERE DiscountName LIKE ?");
		ps.setString(1, "%" + name + "%");
		ResultSet rs = ps.executeQuery();

		Discount cus = null;

		if (rs.next()) {
			cus = new Discount();
			cus.setDiscountID(rs.getInt(1));
			cus.setDiscountName(rs.getString(2));
			cus.setAmount(rs.getString("DiscountType").equals("Percentage")?rs.getInt(3):rs.getInt(4));
			cus.setPercent(rs.getString("DiscountType").equals("Percentage"));
		}

		rs.close();
		conn.close();

		return cus;

	}


	public static ArrayList<Customer> getCustomerList() throws SQLException, IOException {
		connect_to_db();
		/*
		 * Query the data for all the customers and return an arrayList of all the customers. 
		 * Don't forget to order the data coming from the database appropriately.
		 * 
		*/
		ArrayList<Customer> customerArrayList = new ArrayList<>();
		PreparedStatement ps  = conn.prepareStatement("select CustomerID, CustomerFirstName, CustomerLastName, CustomerPhone from customer");
		ResultSet rs = ps.executeQuery();
		while(rs.next()){
			Customer cus = new Customer(rs.getInt("CustomerID"),
					rs.getString("CustomerFirstName"),
					rs.getString("CustomerLastName"),
					rs.getString("CustomerPhone"));
			customerArrayList.add(cus);

		}


		
		
		
		
		conn.close();
		//DO NOT FORGET TO CLOSE YOUR CONNECTION
		return customerArrayList;
	}

	public static Customer findCustomerByPhone(String phoneNumber) throws SQLException, IOException {
		/*
		 * Query the database for a customer using a phone number.
		 * If found, then return a Customer object for the customer.
		 * If it's not found....then return null
		 *  
		 */
		connect_to_db();
		PreparedStatement ps = conn.prepareStatement("SELECT * FROM customers WHERE CustomerPhone LIKE ?");
		ps.setString(1, "%" + phoneNumber + "%");
		ResultSet rs = ps.executeQuery();

		Customer cus = null;

		if (rs.next()) {
			cus = new Customer();
			cus.setCustID(rs.getInt(1));
			cus.setFName(rs.getString(2));
			cus.setLName(rs.getString(3));
			cus.setPhone(rs.getString(4));
		}

		rs.close();
		conn.close();

		return cus;

	}


	public static ArrayList<Topping> getToppingList() {
		ArrayList<Topping> toppingList = new ArrayList<>();

		// Use try-with-resources for automatic resource management
		try {
			connect_to_db();
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT ToppingID, ToppingName, ToppingInventory, ToppingSmall, ToppingMedium," +
					"ToppingLarge, ToppingExtraLarge,ToppingPricePerUnit,ToppingCostPerUnit FROM topping ORDER BY ToppingID");

			while (rs.next()) {
				Topping topping = new Topping();
				topping.setTopID(rs.getInt(1));
				topping.setTopName(rs.getString(2));
				topping.setCurINVT(rs.getInt(3)); // Assuming inventory is a double
				topping.setPerAMT(rs.getDouble(4));
				topping.setMedAMT(rs.getDouble(5));
				topping.setLgAMT(rs.getDouble(6));
				topping.setXLAMT(rs.getDouble(7));
				topping.setCustPrice(rs.getDouble(8));
				topping.setBusPrice(rs.getDouble(9));

				toppingList.add(topping);
			}
		} catch (SQLException | IOException e) {
			e.printStackTrace(); // Or handle the exception as per your error handling policy
		} finally {
			try {
				if (conn != null && !conn.isClosed()) {
					conn.close();
				}
			} catch (SQLException e) {
				e.printStackTrace(); // Or handle the exception as per your error handling policy
			}
		}

		return toppingList;
	}

	public static Topping findToppingByName(String name) throws SQLException, IOException {
		/*
		 * Query the database for the topping using it's name.
		 * If found, then return a Topping object for the topping.
		 * If it's not found....then return null
		 *  
		 */
		connect_to_db();
		PreparedStatement ps = conn.prepareStatement("SELECT * FROM topping WHERE ToppingName LIKE ?");
		ps.setString(1, "%" + name + "%");
		ResultSet rs = ps.executeQuery();

		Topping topping = null;

		if (rs.next()) {
			topping = new Topping();
			topping.setTopID(rs.getInt(1));
			topping.setTopName(rs.getString(2));
			topping.setCustPrice(rs.getDouble(3));
			topping.setBusPrice(rs.getDouble(4));
			topping.setCurINVT(rs.getInt(5));
		}

		rs.close();
		conn.close();

		return topping;

	}


	public static void addToInventory(Topping t, double quantity) throws SQLException, IOException {
		connect_to_db();
		/*
		 * Updates the quantity of the topping in the database by the amount specified.
		 * 
		 * */
		PreparedStatement ps = conn.prepareStatement("UPDATE topping " +
				"SET ToppingInventory = ToppingInventory + ? \n" +
				"WHERE ToppingID = ?");
		ps.setInt(2,(t.getTopID()));
		ps.setDouble(1, quantity);
		ps.executeUpdate();
		conn.close();
	}
	
	public static double getBaseCustPrice(String size, String crust) throws SQLException, IOException {
		double cusPrice = 0;
		try {
			connect_to_db();
			PreparedStatement st = conn.prepareStatement("SELECT Price FROM pizzaattributes WHERE Size = ? AND Crust = ?");
			st.setString(1, size);  // 'size' is a variable holding the size value
			st.setString(2, crust);
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				cusPrice = rs.getDouble("Price");
			}
		} catch (SQLException | IOException e) {
			e.printStackTrace(); // Or handle the exception as per your error handling policy
		} finally {
			try {
				if (conn != null && !conn.isClosed()) {
					conn.close();
				}
			} catch (SQLException e) {
				e.printStackTrace(); // Or handle the exception as per your error handling policy
			}
		}
		return cusPrice;
	}

	public static double[] getTotalBusAndCusPrice(int orderID) throws SQLException,IOException{
		double[] totalPrice = new double[2];
		try{
			connect_to_db();
			PreparedStatement st = conn.prepareStatement("\n" +
					"select sum(PizzaCost),sum(PizzaPrice) from pizza\n" +
					"                                               where OrderID = ?\n" +
					"                                               group by OrderID");
			st.setInt(1,orderID);
			ResultSet rs = st.executeQuery();
			while (rs.next()){
				totalPrice[0] = rs.getDouble(1);
				totalPrice[1] = rs.getDouble(2);
			}
		}catch (Exception e){
			e.printStackTrace();
		}

		return totalPrice;
	}

	public static double getBaseBusPrice(String size, String crust) throws SQLException, IOException {
		double busPrice = 0;
		try {
			connect_to_db();
			PreparedStatement st = conn.prepareStatement("SELECT Cost FROM pizzaattributes WHERE Size = ? AND Crust = ?");
			st.setString(1, size);  // 'size' is a variable holding the size value
			st.setString(2, crust);
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				busPrice = rs.getDouble("Cost");
			}
		} catch (SQLException | IOException e) {
			e.printStackTrace(); // Or handle the exception as per your error handling policy
		} finally {
			try {
				if (conn != null && !conn.isClosed()) {
					conn.close();
				}
			} catch (SQLException e) {
				e.printStackTrace(); // Or handle the exception as per your error handling policy
			}
		}
		return busPrice;
	}

	public static void printInventory() throws SQLException, IOException {
		connect_to_db();
		/*
		 * Queries the database and prints the current topping list with quantities.
		 *  
		 * The result should be readable and sorted as indicated in the prompt.
		 * 
		 */
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery("select ToppingID, ToppingName, ToppingInventory from topping");
		while (rs.next()){
			System.out.println(rs.getString(1) + "  " + rs.getString(2) + " " +  rs.getString(3));
		}
		conn.close();

	}
	
	public static void printToppingPopReport() throws SQLException, IOException
	{
		connect_to_db();
		/*
		 * Prints the ToppingPopularity view. Remember that this view
		 * needs to exist in your DB, so be sure you've run your createViews.sql
		 * files on your testing DB if you haven't already.
		 * 
		 * The result should be readable and sorted as indicated in the prompt.
		 * 
		 */
		String[] headers = {"MinToppingName", "ToppingCount"};
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery("select * from ViewToppingCounts order by ToppingCount desc ");

		for (String header : headers) {
			System.out.printf("%-23s", header);
		}
		System.out.println();


		while (rs.next()) {
			System.out.printf("%-23s%-23s\n", rs.getString("MinToppingName"), rs.getString("ToppingCount"));
		}

		conn.close();

		//DO NOT FORGET TO CLOSE YOUR CONNECTION
	}
	
	public static void printProfitByPizzaReport() throws SQLException, IOException
	{
		connect_to_db();
		/*
		 * Prints the ProfitByPizza view. Remember that this view
		 * needs to exist in your DB, so be sure you've run your createViews.sql
		 * files on your testing DB if you haven't already.
		 * 
		 * The result should be readable and sorted as indicated in the prompt.
		 * 
		 */
		String[] headers = {"Size", "Crust", "Profit"};
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery("select * from ProfitByPizza");

		for (String header : headers) {
			System.out.printf("%-15s", header);
		}
		System.out.println();


		while (rs.next()) {
			System.out.printf("%-15s%-15s%-15s\n", rs.getString("Size"), rs.getString("Crust"), rs.getString("Profit"));
		}
		
		conn.close();
		
		
		//DO NOT FORGET TO CLOSE YOUR CONNECTION
	}
	
	public static void printProfitByOrderType() throws SQLException, IOException
	{
		connect_to_db();
		/*
		 * Prints the ProfitByOrderType view. Remember that this view
		 * needs to exist in your DB, so be sure you've run your createViews.sql
		 * files on your testing DB if you haven't already.
		 * 
		 * The result should be readable and sorted as indicated in the prompt.
		 * 
		 */

		String[] headers = {"customerType", "OrderMonth", "Profit"};
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery("SELECT customerType, OrderMonth, Profit\n" +
				"FROM Pizzeria.ProfitByOrderType\n" +
				"ORDER BY RIGHT(OrderMonth, 4),\n" +
				"         CASE\n" +
				"             WHEN LEFT(OrderMonth, 2) = '01' THEN 10\n" +
				"             WHEN LEFT(OrderMonth, 2) = '02' THEN 11\n" +
				"             WHEN LEFT(OrderMonth, 2) = '03' THEN 12\n" +
				"             ELSE CAST(LEFT(OrderMonth, 2) AS UNSIGNED)\n" +
				"         END,\n" +
				"         customerType");

		for (String header : headers) {
			System.out.printf("%-15s", header);
		}
		System.out.println();


		while (rs.next()) {
			System.out.printf("%-15s%-15s%-15s\n", rs.getString("customerType"), rs.getString("OrderMonth"), rs.getString("Profit"));
		}

		conn.close();
		
		
		
		
		//DO NOT FORGET TO CLOSE YOUR CONNECTION	
	}
	public static String getDiscountByPizzaID (Integer pizzaID) throws SQLException, IOException {
		connect_to_db();
		PreparedStatement ps = conn.prepareStatement("select DiscountName from discount d INNER JOIN pizzadiscount p ON d.DiscountID = p.DiscountID WHERE p.PizzaID = ?");
		ps.setInt(1, pizzaID);
		ResultSet rs = ps.executeQuery();
		StringBuilder a = new StringBuilder();
		while (rs.next()) {
			String discountName = rs.getString(1);
			if (discountName != null && !discountName.isEmpty()) {
				a.append(discountName);
			} else {
				a.append("No Pizza Discount Applied");
			}
			a.append(" ");
		}
		conn.close();
		return a.toString();
	}

	public static String getDiscountByOrderID (Integer orderID) throws SQLException, IOException {
		connect_to_db();
		PreparedStatement ps = conn.prepareStatement("select DiscountName from discount d INNER JOIN orderdiscount o ON d.DiscountID = o.DiscountID WHERE o.OrderID = ?");
		ps.setInt(1, orderID);
		ResultSet rs = ps.executeQuery();
		StringBuilder a = new StringBuilder();
		while (rs.next()) {
			String discountName = rs.getString(1);
			if (discountName != null && !discountName.isEmpty()) {
				a.append(discountName);
			} else {
				a.append("No Order Discount Applied");
			}
			a.append(" ");
		}
		conn.close();
		return a.toString();
	}
	public static int getTableNumber(int orderID) throws SQLException, IOException{
		connect_to_db();
		int tableNum =0;
		PreparedStatement ps = conn.prepareStatement("select TableNumber from dinein where OrderID = ?");
		ps.setInt(1,orderID);
		ResultSet rs = ps.executeQuery();
		while (rs.next()){
			tableNum = rs.getInt(1);
		}

		conn.close();
		return tableNum;
	}
	
	
	public static String getCustomerName(int CustID) throws SQLException, IOException
	{
	/*
		 * This is a helper method to fetch and format the name of a customer
		 * based on a customer ID. This is an example of how to interact with 
		 * your database from Java.  It's used in the model solution for this project...so the code works!
		 * 
		 * OF COURSE....this code would only work in your application if the table & field names match!
		 *
		 */

		 connect_to_db();

		/* 
		 * an example query using a constructed string...
		 * remember, this style of query construction could be subject to sql injection attacks!
		 * 
		 */
		String cname1 = "";
		String query = "Select CustomerFirstName, CustomerLastName From customer WHERE CustomerID=" + CustID + ";";
		Statement stmt = conn.createStatement();
		ResultSet rset = stmt.executeQuery(query);
		
		while(rset.next())
		{
			cname1 = rset.getString(1) + " " + rset.getString(2); 
		}

		/* 
		* an example of the same query using a prepared statement...
		* 
		*/
		String cname2 = "";
		PreparedStatement os;
		ResultSet rset2;
		String query2;
		query2 = "Select CustomerFirstName, CustomerLastName From customer WHERE CustomerID=?;";
		os = conn.prepareStatement(query2);
		os.setInt(1, CustID);
		rset2 = os.executeQuery();
		while(rset2.next())
		{
			cname2 = rset2.getString("CustomerFirstName") + " " + rset2.getString("CustomerLastName"); // note the use of field names in the getSting methods
		}

		conn.close();
		return cname1; // OR cname2
	}

	/*
	 * The next 3 private methods help get the individual components of a SQL datetime object. 
	 * You're welcome to keep them or remove them.
	 */
	private static int getYear(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
	{
		return Integer.parseInt(date.substring(0,4));
	}
	private static int getMonth(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
	{
		return Integer.parseInt(date.substring(5, 7));
	}
	private static int getDay(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
	{
		return Integer.parseInt(date.substring(8, 10));
	}

	public static boolean checkDate(int year, int month, int day, String dateOfOrder)
	{
		if(getYear(dateOfOrder) > year)
			return true;
		else if(getYear(dateOfOrder) < year)
			return false;
		else
		{
			if(getMonth(dateOfOrder) > month)
				return true;
			else if(getMonth(dateOfOrder) < month)
				return false;
			else
			{
				if(getDay(dateOfOrder) >= day)
					return true;
				else
					return false;
			}
		}
	}


}
