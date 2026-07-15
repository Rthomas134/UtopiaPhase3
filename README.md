# UTOPIA Order Management System (Phase 3)

Java order management system for managing UTOPIA drive-thru customer orders, now with a Swing GUI and JUnit unit tests covering the core DMS logic.

## Features

* Graphical interface (Swing) — all interaction happens through the GUI, no console input
* Load order records from a pipe-separated text file, with a Browse button or manual path entry
* Display all order records in a table, refreshable on demand
* Create new orders through a form dialog
* Remove orders by selecting a row in the table
* Update any field of a selected order (customer name, item, quantity, pickup lane)
* Mark an order Complete and calculate its prep time (custom action)
* Validate customer name, menu item, quantity, pickup lane, and order status before any change — invalid input is rejected with a clear message, the program never crashes
* The program only closes when the user clicks Exit
* Unit tests (JUnit 5) verify file loading, add, remove, update, and the custom action at the logic layer — each with a passing case and a rejected/invalid case

## File Format

Each order record should be formatted like this:
1001|Maya Johnson|Burger Combo|2|17.98|Pending|2026-07-06T10:15:00|1|NONE

Order:
1. Order ID
2. Customer Name
3. Item Name
4. Quantity
5. Order Total
6. Order Status
7. Order Placed At
8. Pickup Lane
9. Completed At
Each field is separated by a pipe (`|`).
## How to Run from Terminal
javac src/com/utopia/dms/*.java -d out
jar cfe UtopiaOrderGui.jar com.utopia.dms.UtopiaOrderGui -C out .
java -jar UtopiaOrderGui.jar

The GUI window will open. Enter (or Browse to) the full path to `sample_orders.txt` in the file path field and click Load Data.
