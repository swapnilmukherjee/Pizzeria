-- Swapnil Mukherjee, Apurva Ratnaparkhi

DROP SCHEMA IF EXISTS Pizzeria;
CREATE SCHEMA Pizzeria;
USE Pizzeria;

CREATE TABLE customer (
  CustomerID INT AUTO_INCREMENT PRIMARY KEY,
  CustomerFirstName VARCHAR(255) NOT NULL,
  CustomerLastName VARCHAR(255) NOT NULL,
  CustomerPhone VARCHAR(20),
  CustomerStreetName VARCHAR(255),
  CustomerCity VARCHAR(100),
  CustomerState VARCHAR(100),
  CustomerZipCode VARCHAR(20)
);

CREATE TABLE discount (
  DiscountID INT AUTO_INCREMENT PRIMARY KEY,
  DiscountName VARCHAR(255) NOT NULL,
  DiscountType VARCHAR(50) NOT NULL,
  DiscountPercentage DECIMAL(5, 2),
  DiscountAmount DECIMAL(5, 2)
);

CREATE TABLE pizzaattributes (
  PizzaAttributesID INT AUTO_INCREMENT PRIMARY KEY,
  Size VARCHAR(50) NOT NULL,
  Crust VARCHAR(50) NOT NULL,
  Price DECIMAL(5, 2) NOT NULL,
  Cost DECIMAL(5, 2) NOT NULL
);

CREATE TABLE orders (
  OrderID INT AUTO_INCREMENT PRIMARY KEY,
  CustomerID INT,
  OrderTime TIME NOT NULL,
  OrderStatus VARCHAR(50) NOT NULL,
  OrderDate DATE NOT NULL,
  OrderType VARCHAR(50) NOT NULL,
  OrderCost DECIMAL(5, 2),
  OrderPrice DECIMAL(5, 2),
  FOREIGN KEY (CustomerID) REFERENCES customer(CustomerID)
);

CREATE TABLE pizza (
  PizzaID INT AUTO_INCREMENT PRIMARY KEY,
  OrderID INT,
  PizzaAttributesID INT,
  PizzaPrice DECIMAL(5, 2) NOT NULL,
  PizzaCost DECIMAL(5, 2) NOT NULL,
  FOREIGN KEY (OrderID) REFERENCES orders(OrderID),
  FOREIGN KEY (PizzaAttributesID) REFERENCES pizzaattributes(PizzaAttributesID)
);

CREATE TABLE topping (
  ToppingID INT AUTO_INCREMENT PRIMARY KEY,
  ToppingName VARCHAR(100) NOT NULL,
  ToppingPricePerUnit DECIMAL(4, 2) NOT NULL,
  ToppingCostPerUnit DECIMAL(4, 2) NOT NULL,
  ToppingInventory INT NOT NULL,
  ToppingInventoryMinimum INT NOT NULL,
  ToppingSmall DECIMAL(3, 2) NOT NULL,
  ToppingMedium DECIMAL(3, 2) NOT NULL,
  ToppingLarge DECIMAL(3, 2) NOT NULL,
  ToppingExtraLarge DECIMAL(3, 2) NOT NULL
);

CREATE TABLE pizzatopping (
  PizzaID INT,
  ToppingID INT,
  PizzaToppingAdditional BOOLEAN NOT NULL,
  PRIMARY KEY (PizzaID, ToppingID),
  FOREIGN KEY (PizzaID) REFERENCES pizza(PizzaID),
  FOREIGN KEY (ToppingID) REFERENCES topping(ToppingID)
);

CREATE TABLE orderdiscount (
  OrderID INT,
  DiscountID INT,
  PRIMARY KEY (OrderID, DiscountID),
  FOREIGN KEY (OrderID) REFERENCES orders(OrderID),
  FOREIGN KEY (DiscountID) REFERENCES discount(DiscountID)
);

CREATE TABLE pizzadiscount (
  PizzaID INT,
  DiscountID INT,
  PRIMARY KEY (PizzaID, DiscountID),
  FOREIGN KEY (PizzaID) REFERENCES pizza(PizzaID),
  FOREIGN KEY (DiscountID) REFERENCES discount(DiscountID)
);

CREATE TABLE pickup (
  OrderID INT,
  CustomerID INT,
  PRIMARY KEY (OrderID),
  FOREIGN KEY (OrderID) REFERENCES orders(OrderID),
  FOREIGN KEY (CustomerID) REFERENCES customer(CustomerID)
);

CREATE TABLE delivery (
  OrderID INT,
  CustomerID INT,
  PRIMARY KEY (OrderID),
  FOREIGN KEY (OrderID) REFERENCES orders(OrderID),
  FOREIGN KEY (CustomerID) REFERENCES customer(CustomerID)
);

CREATE TABLE dinein (
  OrderID INT,
  TableNumber INT,
  PRIMARY KEY (OrderID),
  FOREIGN KEY (OrderID) REFERENCES orders(OrderID)
);
