# College E-Library Management System

An educational E-Library web portal and database management system designed specifically for the **Database Management Systems (24CS520)** course viva and demonstration.

---

## 🚀 How to Run the Project

### For Windows:
1. Double-click **`run.bat`** (or open Command Prompt in the project folder and type `run.bat`).
2. Access the website at: **`http://localhost:8080`**

### For Linux / WSL2:
1. In your terminal, run:
   ```bash
   chmod +x run.sh
   ./run.sh
   ```
2. Access the website at: **`http://localhost:8080`**

---

## 🗄️ Database Tools: DBeaver & MySQL Workbench (For DBMS Viva Demo)

*Note: The website server itself is self-contained and runs immediately without needing MySQL running.*

If demonstrating SQL queries, triggers, and views in **DBeaver** or **MySQL Workbench**:

### 1. Connection Details
* **Host**: `localhost` (or `127.0.0.1`)
* **Port**: `3307` (your WSL2 Ubuntu MySQL port) or `3306` (standard Windows MySQL)
* **Username**: `sem5e2` (or `root`)
* **Database**: `elibrary_db`

### 2. Using DBeaver with this Project:
1. **Connect**: Click `Database` $\to$ `New Database Connection` $\to$ `MySQL`, enter Host: `localhost`, Port: `3307`, Database: `elibrary_db`, User: `sem5e2`, and click Finish.
2. **Setup Schema**: In DBeaver, click `File` $\to$ `Open File...` $\to$ select `advanced_dbms_queries/schema.sql`, press `Alt + X` to execute and build the 14 tables, triggers, and views.
3. **Open Live Transactions**: Click `File` $\to$ `Open File...` $\to$ select **`live_dbms_transactions.sql`**.
   - Path in Windows: `\\wsl.localhost\Ubuntu\home\pink\e-libraryDB\live_dbms_transactions.sql`
4. **Refreshing in DBeaver**: Whenever you perform an action on the website (Reserve, Borrow, Fine, etc.):
   - Right-click the SQL Editor tab in DBeaver and select **"File $\to$ Revert"** (or press `F5`) to reload the latest statements from disk!
   - *Auto-reload Tip*: In DBeaver, go to **Window $\to$ Preferences $\to$ General $\to$ Workspace** and check **"Refresh using native hooks or polling"** so DBeaver auto-detects file updates.
5. **Decluttering / Clearing Logs**:
   - Click the **`🗑️ Clear Logs`** button on the website top navbar, OR
   - Run `./clear_sql_logs.sh` (or double-click `clear_sql_logs.bat` on Windows).

---

## 🔑 Login Accounts (USN Format)

| Role | Name | Department | USN (User ID) | Password | Demo Scenario |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **👩‍💼 Chief Librarian** | Mrs. Shwetha Rao | Chief Librarian | **`01JSTLIB001`** | **`admin123`** | Issue/return counter, approve acquisitions, collect fines, high demand alert. |
| **👨‍🎓 Student 1** | Rahul Sharma | Computer Science (CSE) | **`01JST24UCS001`** | **`pass123`** | Clean account (0/5 books). Test **12-hour reservation** & live ticking clock, unvoting. |
| **👩‍🎓 Student 2** | Priya Patel | Electronics & Comm (ECE) | **`01JST24UCS002`** | **`pass123`** | Has **4/5 books borrowed**. Demonstrates **Trigger: Max 5 books limit**. |
| **👨‍🎓 Student 3** | Amit Kumar | Mechanical Engg (MECH) | **`01JST24UCS003`** | **`pass123`** | Clean account (0/5 books). Borrow a book live, fast-forward +35 days to demonstrate overdue fine! |

---

## 📱 Connecting Mobile Phones to the Server (WSL2)

If connecting mobile phones to test the website:

1. **Right-click `setup_hotspot_forwarding.bat` and choose "Run as administrator"** (or double-click it and accept the Windows UAC admin prompt).
2. The script will automatically detect your WSL2 IP and show the exact URL to use on your phone:
   ```text
   http://192.168.1.3:8080
   ```
   *(Ensure you type `http://`, NOT `https://`)*

### ⚠️ Troubleshooting "Server took too long to respond":
1. **Never use `172.x.x.x`**: `172.27.0.1` is Windows' internal WSL virtual switch — mobile phones cannot reach it. Always use your Wi-Fi IP (`192.168.1.3` in your case).
2. **McAfee / Third-Party Antivirus Firewall**: If McAfee or another antivirus is installed, its firewall blocks external incoming connections by default. Temporarily pause the McAfee Firewall or set network trust to Private/Home.
3. **Turn on Windows Mobile Hotspot**: If your Wi-Fi router has "AP Isolation" enabled (blocking phones from talking to PCs), turn on **Mobile Hotspot** in Windows Settings (`Win + I -> Network & Internet -> Mobile Hotspot`), connect your phone to your laptop's hotspot, and re-run `setup_hotspot_forwarding.bat`.

---

## 📖 Presentation & Viva Guide
For a complete, step-by-step viva presentation script with exact verbal explanations, website clicks, and SQL queries to run in MySQL Workbench, refer to:
* **[PRESENTATION_SCRIPT.md](file:///home/pink/e-libraryDB/PRESENTATION_SCRIPT.md)**
