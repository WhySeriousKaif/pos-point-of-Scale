# MySQL Database Commands Guide

## Quick Reference for Molla POS System Database

**Database Details:**
- Database Name: `pos_db`
- Username: `root`
- Password: `root123`
- Host: `localhost`
- Port: `3306`

---

## 1. Connect to MySQL

### Option 1: Connect directly to database
```bash
mysql -u root -proot123 pos_db
```

### Option 2: Connect to MySQL server (then select database)
```bash
mysql -u root -proot123
```

### Option 3: Connect with interactive prompt (more secure)
```bash
mysql -u root -p
# Then enter password when prompted: root123
```

---

## 2. Basic Commands (After Connecting)

### Show all databases
```sql
SHOW DATABASES;
```

### Select/Use your database
```sql
USE pos_db;
```

### Show all tables in current database
```sql
SHOW TABLES;
```s

### Show table structure
```sql
DESCRIBE user;
-- OR
SHOW COLUMNS FROM user;
```

---

## 3. View Data from User Table

### View all users
```sql
SELECT * FROM user;
```

### View users with formatted output
```sql
SELECT * FROM user\G
```

### View specific columns only
```sql
SELECT id, email, full_name, role, created_at FROM user;
```

### View users with readable dates
```sql
SELECT 
    id,
    email,
    full_name,
    role,
    DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS created_at,
    DATE_FORMAT(last_login_at, '%Y-%m-%d %H:%i:%s') AS last_login_at
FROM user;
```

### Count total users
```sql
SELECT COUNT(*) AS total_users FROM user;
```

### View users by role
```sql
SELECT role, COUNT(*) AS count 
FROM user 
GROUP BY role;
```

---

## 4. Search/Filter Data

### Find user by email
```sql
SELECT * FROM user WHERE email = 'user@example.com';
```

### Find user by ID
```sql
SELECT * FROM user WHERE id = 1;
```

### Find users by role
```sql
SELECT * FROM user WHERE role = 'ROLE_USER';
```

### Find users created today
```sql
SELECT * FROM user 
WHERE DATE(created_at) = CURDATE();
```

### Find users created in last 7 days
```sql
SELECT * FROM user 
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY);
```

### Search by name (partial match)
```sql
SELECT * FROM user 
WHERE full_name LIKE '%John%';
```

---

## 5. Useful Queries

### View users with their last login (most recent first)
```sql
SELECT 
    id,
    email,
    full_name,
    role,
    last_login_at
FROM user
ORDER BY last_login_at DESC;
```

### View users who never logged in
```sql
SELECT * FROM user WHERE last_login_at IS NULL;
```

### View users created in last month
```sql
SELECT * FROM user 
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 MONTH);
```

### View user statistics
```sql
SELECT 
    COUNT(*) AS total_users,
    COUNT(DISTINCT role) AS unique_roles,
    MIN(created_at) AS first_user_created,
    MAX(created_at) AS last_user_created
FROM user;
```

---

## 6. One-Line Commands (From Terminal)

### View all users (without entering MySQL)
```bash
mysql -u root -proot123 pos_db -e "SELECT * FROM user;"
```

### View user count
```bash
mysql -u root -proot123 pos_db -e "SELECT COUNT(*) AS total_users FROM user;"
```

### View users by role
```bash
mysql -u root -proot123 pos_db -e "SELECT role, COUNT(*) AS count FROM user GROUP BY role;"
```

### View specific user by email
```bash
mysql -u root -proot123 pos_db -e "SELECT * FROM user WHERE email='user@example.com';"
```

### Export data to file
```bash
mysql -u root -proot123 pos_db -e "SELECT * FROM user;" > users_export.txt
```

---

## 7. Formatting Output

### Display results in table format (default)
```sql
SELECT * FROM user;
```

### Display results vertically (one field per line)
```sql
SELECT * FROM user\G
```

### Set column names display
```sql
SELECT * FROM user\G
```

### Limit number of rows
```sql
SELECT * FROM user LIMIT 10;
```

### Skip first N rows and show next M rows
```sql
SELECT * FROM user LIMIT 5, 10;  -- Skip 5, show next 10
```

---

## 8. Exit MySQL

```sql
EXIT;
-- OR
QUIT;
-- OR press Ctrl+D
```

---

## 9. Common Workflow Example

```bash
# Step 1: Connect to database
mysql -u root -proot123 pos_db

# Step 2: (Inside MySQL) View all tables
SHOW TABLES;

# Step 3: View user table structure
DESCRIBE user;

# Step 4: View all users
SELECT * FROM user;

# Step 5: View specific user
SELECT * FROM user WHERE email = 'test@example.com';

# Step 6: Exit
EXIT;
```

---

## 10. Troubleshooting

### If you get "Access denied"
- Check username and password
- Make sure MySQL server is running: `sudo service mysql start` (Linux) or check MySQL service status

### If you get "Unknown database"
- Create the database first: `CREATE DATABASE pos_db;`
- Or check database name spelling

### If table doesn't exist
- Make sure your Spring Boot application has run at least once
- Check if `spring.jpa.hibernate.ddl-auto=update` is set in application.properties

---

## 11. Quick Check Script

Save this as `check_db.sh`:

```bash
#!/bin/bash
echo "=== Checking pos_db Database ==="
echo ""
echo "Total Users:"
mysql -u root -proot123 pos_db -e "SELECT COUNT(*) AS total FROM user;"
echo ""
echo "Users by Role:"
mysql -u root -proot123 pos_db -e "SELECT role, COUNT(*) AS count FROM user GROUP BY role;"
echo ""
echo "Recent Users (Last 5):"
mysql -u root -proot123 pos_db -e "SELECT id, email, full_name, role, created_at FROM user ORDER BY created_at DESC LIMIT 5;"
```

Make it executable and run:
```bash
chmod +x check_db.sh
./check_db.sh
```

---

## 12. Security Note

⚠️ **For Production**: Never use passwords in command line. Use:
```bash
mysql -u root -p
# Then enter password when prompted
```

Or use MySQL configuration file (`~/.my.cnf`):
```ini
[client]
user=root
password=root123
```

Then just run: `mysql pos_db`

---

## Quick Reference Card

| Task | Command |
|------|---------|
| Connect | `mysql -u root -proot123 pos_db` |
| Show tables | `SHOW TABLES;` |
| View all users | `SELECT * FROM user;` |
| Count users | `SELECT COUNT(*) FROM user;` |
| Find by email | `SELECT * FROM user WHERE email='...';` |
| Find by ID | `SELECT * FROM user WHERE id=1;` |
| Exit | `EXIT;` or `QUIT;` |

---

**Happy Querying! 🚀**

