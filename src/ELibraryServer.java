import com.sun.net.httpserver.*;
import models.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ELibraryServer {
    private static final int PORT = 8080;
    private static final LibraryStore store = new LibraryStore();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);

        // Core API Endpoints
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/me", new MeHandler());
        server.createContext("/api/books", new BooksHandler());
        server.createContext("/api/reserve", new ReserveHandler());
        server.createContext("/api/cancel-reservation", new CancelReservationHandler());
        server.createContext("/api/borrow", new BorrowHandler());
        server.createContext("/api/return", new ReturnHandler());
        server.createContext("/api/collect-fine", new CollectFineHandler());
        server.createContext("/api/librarian-data", new LibrarianDataHandler());
        server.createContext("/api/requests", new RequestsHandler());
        server.createContext("/api/request-book", new RequestBookHandler());
        server.createContext("/api/vote-request", new VoteRequestHandler());
        server.createContext("/api/unvote-request", new UnvoteRequestHandler());
        server.createContext("/api/procure-request", new ProcureRequestHandler());
        server.createContext("/api/lab-query", new LabQueryHandler());
        server.createContext("/api/sql-logs", new SqlLogsHandler());
        server.createContext("/api/clear-sql-logs", new ClearSqlLogsHandler());
        server.createContext("/api/simulate", new SimulateHandler());
        server.createContext("/api/reset", new ResetHandler());

        // Static Files Handler
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("=================================================================");
        System.out.println("   COLLEGE E-LIBRARY PORTAL (DBMS 24CS520)");
        System.out.println("   Local Access (Laptop):       http://localhost:" + PORT);
        System.out.println("   Mobile/Hotspot Access:       http://<windows-wifi-ip>:" + PORT);
        System.out.println("   Live DBMS Transactions Log:  live_dbms_transactions.sql");
        System.out.println("=================================================================");
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String responseJson) throws IOException {
        byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) != -1) {
                bos.write(buf, 0, len);
            }
            return bos.toString(StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> getQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                try {
                    map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                } catch (Exception ignored) {}
            }
        }
        return map;
    }

    // =========================================================================
    // Static File Serving
    // =========================================================================
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            File file = new File("public" + path);
            if (!file.exists() || file.isDirectory()) {
                file = new File("public/index.html");
            }

            if (!file.exists()) {
                String notFound = "<h1>404 Not Found</h1>";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes());
                }
                return;
            }

            String contentType = "text/plain";
            String lower = file.getName().toLowerCase();
            if (lower.endsWith(".html")) contentType = "text/html; charset=UTF-8";
            else if (lower.endsWith(".css")) contentType = "text/css; charset=UTF-8";
            else if (lower.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";
            else if (lower.endsWith(".json")) contentType = "application/json; charset=UTF-8";
            else if (lower.endsWith(".svg")) contentType = "image/svg+xml";
            else if (lower.endsWith(".png")) contentType = "image/png";

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            exchange.sendResponseHeaders(200, fileBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        }
    }

    // =========================================================================
    // API: Login Handler (Supports USN or Email)
    // =========================================================================
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readBody(exchange);
            Map<String, String> data = JsonUtil.parseSimpleJson(body);

            User user = null;
            if (data.containsKey("userId")) {
                try {
                    int uid = Integer.parseInt(data.get("userId"));
                    user = store.getUser(uid);
                } catch (Exception ignored) {}
            } else if (data.containsKey("usn") || data.containsKey("email")) {
                String id = data.containsKey("usn") ? data.get("usn") : data.get("email");
                user = store.getUserByUsnOrEmail(id);
                if (user != null && data.containsKey("password") && !user.getPassword().equals(data.get("password"))) {
                    user = null; // invalid password
                }
            }

            if (user == null) {
                sendJsonResponse(exchange, 401, "{\"error\":\"Invalid USN or Password. Example: 01JST24UCS001 / pass123\"}");
                return;
            }

            StringBuilder sb = new StringBuilder("{");
            sb.append("\"userId\":").append(user.getUserId()).append(",");
            sb.append("\"usn\":\"").append(JsonUtil.escape(user.getUsn())).append("\",");
            sb.append("\"name\":\"").append(JsonUtil.escape(user.getName())).append("\",");
            sb.append("\"email\":\"").append(JsonUtil.escape(user.getEmail())).append("\",");
            sb.append("\"role\":\"").append(user.getRole()).append("\",");
            sb.append("\"branchCode\":\"").append(user.getBranchCode() != null ? user.getBranchCode() : "").append("\",");
            sb.append("\"semester\":").append(user.getSemester() != null ? user.getSemester() : "null").append(",");
            sb.append("\"phone\":\"").append(JsonUtil.escape(user.getPhone())).append("\"");
            sb.append("}");

            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    // =========================================================================
    // API: Student State Summary (/api/me?userId=X)
    // =========================================================================
    static class MeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = getQueryParams(exchange.getRequestURI().getQuery());
            int userId = 0;
            try { userId = Integer.parseInt(params.get("userId")); } catch (Exception ignored) {}

            User user = store.getUser(userId);
            if (user == null) {
                sendJsonResponse(exchange, 404, "{\"error\":\"User not found\"}");
                return;
            }

            store.checkAndExpireReservations();

            StringBuilder sb = new StringBuilder("{");
            sb.append("\"user\":{");
            sb.append("\"userId\":").append(user.getUserId()).append(",");
            sb.append("\"usn\":\"").append(JsonUtil.escape(user.getUsn())).append("\",");
            sb.append("\"name\":\"").append(JsonUtil.escape(user.getName())).append("\",");
            sb.append("\"email\":\"").append(JsonUtil.escape(user.getEmail())).append("\",");
            sb.append("\"role\":\"").append(user.getRole()).append("\",");
            sb.append("\"branchCode\":\"").append(user.getBranchCode() != null ? user.getBranchCode() : "").append("\",");
            sb.append("\"semester\":").append(user.getSemester() != null ? user.getSemester() : "null").append(",");
            sb.append("\"phone\":\"").append(JsonUtil.escape(user.getPhone())).append("\",");
            sb.append("\"isReservationBlocked\":").append(user.isReservationBlocked()).append(",");
            sb.append("\"reservationBlockedUntil\":").append(user.getReservationBlockedUntil() != null ? "\"" + user.getReservationBlockedUntil().format(TIME_FMT) + "\"" : "null");
            sb.append("},");

            // Fines
            List<Fine> userFines = store.getFinesForUser(userId);

            // Active Loans
            List<Loan> userLoans = store.getActiveLoansForUser(userId);
            sb.append("\"activeLoans\":[");
            for (int i = 0; i < userLoans.size(); i++) {
                Loan l = userLoans.get(i);
                Book b = store.getBook(l.getBookId());
                BookCopy c = store.getCopy(l.getCopyId());

                Fine matchingFine = null;
                for (Fine f : userFines) {
                    if (f.getLoanId() == l.getLoanId()) {
                        matchingFine = f;
                        break;
                    }
                }
                boolean isFinePaid = (matchingFine != null && "PAID".equalsIgnoreCase(matchingFine.getStatus()));
                double fineAmt = (matchingFine != null) ? matchingFine.getAmount() : l.getCalculatedFine();

                if (i > 0) sb.append(",");
                sb.append("{");
                sb.append("\"loanId\":").append(l.getLoanId()).append(",");
                sb.append("\"copyId\":").append(l.getCopyId()).append(",");
                sb.append("\"bookId\":").append(l.getBookId()).append(",");
                sb.append("\"bookTitle\":\"").append(b != null ? JsonUtil.escape(b.getTitle()) : "Unknown").append("\",");
                sb.append("\"branchCode\":\"").append(b != null ? b.getBranchCode() : "").append("\",");
                sb.append("\"copyCode\":\"").append(c != null ? JsonUtil.escape(c.getCopyCode()) : "Unknown").append("\",");
                sb.append("\"borrowDate\":\"").append(l.getBorrowDate().format(TIME_FMT)).append("\",");
                sb.append("\"dueDate\":\"").append(l.getDueDate().format(TIME_FMT)).append("\",");
                sb.append("\"dueEpoch\":").append(l.getDueDate().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()).append(",");
                sb.append("\"isOverdue\":").append(l.isOverdue()).append(",");
                sb.append("\"isFinePaid\":").append(isFinePaid).append(",");
                sb.append("\"finePaidAmount\":").append(fineAmt).append(",");
                sb.append("\"daysOverdue\":").append(l.getDaysOverdue()).append(",");
                sb.append("\"calculatedFine\":").append(l.getCalculatedFine());
                sb.append("}");
            }
            sb.append("],");

            // Pending Reservations
            List<Reservation> userRes = store.getPendingReservationsForUser(userId);
            sb.append("\"pendingReservations\":[");
            for (int i = 0; i < userRes.size(); i++) {
                Reservation r = userRes.get(i);
                Book b = store.getBook(r.getBookId());
                BookCopy c = store.getCopy(r.getCopyId());
                if (i > 0) sb.append(",");
                sb.append("{");
                sb.append("\"reservationId\":").append(r.getReservationId()).append(",");
                sb.append("\"copyId\":").append(r.getCopyId()).append(",");
                sb.append("\"bookId\":").append(r.getBookId()).append(",");
                sb.append("\"bookTitle\":\"").append(b != null ? JsonUtil.escape(b.getTitle()) : "Unknown").append("\",");
                sb.append("\"copyCode\":\"").append(c != null ? JsonUtil.escape(c.getCopyCode()) : "Unknown").append("\",");
                sb.append("\"reservedAt\":\"").append(r.getReservedAt().format(TIME_FMT)).append("\",");
                sb.append("\"expiresAt\":\"").append(r.getExpiresAt().format(TIME_FMT)).append("\",");
                sb.append("\"expiresEpoch\":").append(r.getExpiresAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
                sb.append("}");
            }
            sb.append("],");

            // Fines
            sb.append("\"fines\":[");
            for (int i = 0; i < userFines.size(); i++) {
                Fine f = userFines.get(i);
                if (i > 0) sb.append(",");
                sb.append("{");
                sb.append("\"fineId\":").append(f.getFineId()).append(",");
                sb.append("\"loanId\":").append(f.getLoanId()).append(",");
                sb.append("\"daysOverdue\":").append(f.getDaysOverdue()).append(",");
                sb.append("\"amount\":").append(f.getAmount()).append(",");
                sb.append("\"status\":\"").append(f.getStatus()).append("\",");
                sb.append("\"assessedAt\":\"").append(f.getAssessedAt().format(TIME_FMT)).append("\"");
                sb.append("}");
            }
            sb.append("],");
            sb.append("\"totalUnpaidFine\":").append(store.getTotalUnpaidFineForUser(userId)).append(",");
            sb.append("\"hasOverdueLoans\":").append(store.hasOverdueLoans(userId));
            sb.append("}");

            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    // =========================================================================
    // API: Books Catalog with Dynamic Filter Logging (/api/books)
    // =========================================================================
    static class BooksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            store.checkAndExpireReservations();
            Map<String, String> params = getQueryParams(exchange.getRequestURI().getQuery());
            String branch = params.get("branch");
            String genre = params.get("genre");
            String minRatingStr = params.get("minRating");
            double minRating = 0.0;
            if (minRatingStr != null) {
                try { minRating = Double.parseDouble(minRatingStr); } catch (Exception ignored) {}
            }
            String search = params.get("search");
            String syncParam = params.get("sync");
            boolean isBackgroundSync = "true".equalsIgnoreCase(syncParam);

            // Re-introduce Catalog Filter & Search SELECT query for demonstration in DBeaver/Workbench
            // (Skipped only on automatic 2-second background polling to prevent spamming identical queries)
            if (!isBackgroundSync && (branch != null || genre != null || minRating > 0.0 || (search != null && !search.isEmpty()))) {
                StringBuilder filterSql = new StringBuilder("SELECT b.book_id, b.title, a.author_name, br.branch_code, g.genre_name, b.rating\n");
                filterSql.append("FROM books b\n");
                filterSql.append("JOIN authors a ON b.author_id = a.author_id\n");
                filterSql.append("JOIN branches br ON b.branch_id = br.branch_id\n");
                filterSql.append("JOIN genres g ON b.genre_id = g.genre_id\nWHERE 1=1 ");
                if (branch != null && !branch.isEmpty() && !branch.equalsIgnoreCase("ALL")) filterSql.append(String.format("AND br.branch_code = '%s' ", branch));
                if (genre != null && !genre.isEmpty() && !genre.equalsIgnoreCase("ALL")) filterSql.append(String.format("AND g.genre_name LIKE '%%%s%%' ", genre));
                if (minRating > 0.0) filterSql.append(String.format("AND b.rating >= %.1f ", minRating));
                if (search != null && !search.isEmpty()) filterSql.append(String.format("AND (b.title LIKE '%%%s%%' OR a.author_name LIKE '%%%s%%') ", search, search));
                filterSql.append("ORDER BY b.rating DESC;");
                store.logSql("Catalog Filter & Search Query (Run in DBeaver to show filtered rows)", filterSql.toString());
            }

            StringBuilder sb = new StringBuilder("[");
            int idx = 0;
            for (Book b : store.getAllBooks()) {
                // Apply filter
                if (branch != null && !branch.isEmpty() && !branch.equalsIgnoreCase("ALL") && !b.getBranchCode().equalsIgnoreCase(branch)) continue;
                if (genre != null && !genre.isEmpty() && !genre.equalsIgnoreCase("ALL") && !b.getGenreName().toLowerCase().contains(genre.toLowerCase())) continue;
                if (minRating > 0.0 && b.getRating() < minRating) continue;
                if (search != null && !search.isEmpty()) {
                    String q = search.toLowerCase();
                    if (!b.getTitle().toLowerCase().contains(q) && !b.getAuthor().toLowerCase().contains(q) && !b.getIsbn().contains(q)) {
                        continue;
                    }
                }

                if (idx > 0) sb.append(",");
                sb.append("{");
                sb.append("\"bookId\":").append(b.getBookId()).append(",");
                sb.append("\"title\":\"").append(JsonUtil.escape(b.getTitle())).append("\",");
                sb.append("\"isbn\":\"").append(JsonUtil.escape(b.getIsbn())).append("\",");
                sb.append("\"branchCode\":\"").append(b.getBranchCode()).append("\",");
                sb.append("\"subjectName\":\"").append(JsonUtil.escape(b.getSubjectName())).append("\",");
                sb.append("\"genreName\":\"").append(JsonUtil.escape(b.getGenreName())).append("\",");
                sb.append("\"publisher\":\"").append(JsonUtil.escape(b.getPublisher())).append("\",");
                sb.append("\"author\":\"").append(JsonUtil.escape(b.getAuthor())).append("\",");
                sb.append("\"edition\":\"").append(JsonUtil.escape(b.getEdition())).append("\",");
                sb.append("\"pubYear\":").append(b.getPubYear()).append(",");
                sb.append("\"rating\":").append(b.getRating()).append(",");
                sb.append("\"description\":\"").append(JsonUtil.escape(b.getDescription())).append("\",");
                sb.append("\"totalCopies\":").append(b.getTotalCopiesCount()).append(",");
                sb.append("\"availableCopies\":").append(b.getAvailableCopiesCount()).append(",");
                sb.append("\"copies\":[");
                List<BookCopy> copies = b.getCopies();
                for (int j = 0; j < copies.size(); j++) {
                    BookCopy c = copies.get(j);
                    if (j > 0) sb.append(",");
                    sb.append("{");
                    sb.append("\"copyId\":").append(c.getCopyId()).append(",");
                    sb.append("\"copyCode\":\"").append(JsonUtil.escape(c.getCopyCode())).append("\",");
                    sb.append("\"status\":\"").append(c.getStatus()).append("\",");
                    sb.append("\"rackLocation\":\"").append(JsonUtil.escape(c.getRackLocation())).append("\"");
                    sb.append("}");
                }
                sb.append("]}");
                idx++;
            }
            sb.append("]");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    // =========================================================================
    // API: Student Reserve Book (/api/reserve)
    // =========================================================================
    static class ReserveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            Map<String, String> data = JsonUtil.parseSimpleJson(readBody(exchange));
            try {
                int userId = Integer.parseInt(data.get("userId"));
                int bookId = Integer.parseInt(data.get("bookId"));
                Reservation res = store.reserveBook(userId, bookId);

                StringBuilder sb = new StringBuilder("{");
                sb.append("\"success\":true,");
                sb.append("\"message\":\"Book reserved successfully! Valid for 12 hours.\",");
                sb.append("\"reservationId\":").append(res.getReservationId()).append(",");
                sb.append("\"expiresAt\":\"").append(res.getExpiresAt().format(TIME_FMT)).append("\"");
                sb.append("}");
                sendJsonResponse(exchange, 200, sb.toString());
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
            }
        }
    }

    static class CancelReservationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            Map<String, String> data = JsonUtil.parseSimpleJson(readBody(exchange));
            try {
                int userId = Integer.parseInt(data.get("userId"));
                int resId = Integer.parseInt(data.get("reservationId"));
                store.cancelReservation(userId, resId);
                sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Reservation cancelled.\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
            }
        }
    }

    static class BorrowHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            Map<String, String> data = JsonUtil.parseSimpleJson(readBody(exchange));
            try {
                int librarianId = Integer.parseInt(data.get("librarianId"));
                int studentId = Integer.parseInt(data.get("studentId"));
                int copyId = Integer.parseInt(data.get("copyId"));

                Loan loan = store.borrowBook(librarianId, studentId, copyId);
                StringBuilder sb = new StringBuilder("{");
                sb.append("\"success\":true,");
                sb.append("\"message\":\"Book issued successfully! 1-Month countdown started.\",");
                sb.append("\"loanId\":").append(loan.getLoanId()).append(",");
                sb.append("\"dueDate\":\"").append(loan.getDueDate().format(TIME_FMT)).append("\"");
                sb.append("}");
                sendJsonResponse(exchange, 200, sb.toString());
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
            }
        }
    }

    // =========================================================================
    // API: Return Book (with immediate fine collection option)
    // =========================================================================
    static class ReturnHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            Map<String, String> data = JsonUtil.parseSimpleJson(readBody(exchange));
            try {
                int librarianId = Integer.parseInt(data.get("librarianId"));
                User libUser = store.getUser(librarianId);
                if (libUser == null || !"LIBRARIAN".equalsIgnoreCase(libUser.getRole())) {
                    sendJsonResponse(exchange, 403, "{\"error\":\"Access Denied: Only library staff can process returns.\"}");
                    return;
                }
                int loanId = Integer.parseInt(data.get("loanId"));
                boolean collectFine = "true".equalsIgnoreCase(data.get("collectFineNow"));

                Map<String, Object> res = store.returnBook(librarianId, loanId, collectFine);
                StringBuilder sb = new StringBuilder("{");
                sb.append("\"success\":true,");
                sb.append("\"fineAmount\":").append(res.get("fineAmount")).append(",");
                sb.append("\"daysOverdue\":").append(res.get("daysOverdue")).append(",");
                sb.append("\"fineCollected\":").append(res.get("fineCollected")).append(",");
                sb.append("\"isFinePaid\":").append(res.get("isFinePaid"));
                sb.append("}");
                sendJsonResponse(exchange, 200, sb.toString());
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
            }
        }
    }

    // API: Explicit Collect Fine (Librarian Desk Only)
    static class CollectFineHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            Map<String, String> data = JsonUtil.parseSimpleJson(readBody(exchange));
            try {
                int librarianId = Integer.parseInt(data.get("librarianId"));
                User libUser = store.getUser(librarianId);
                if (libUser == null || !"LIBRARIAN".equalsIgnoreCase(libUser.getRole())) {
                    sendJsonResponse(exchange, 403, "{\"error\":\"Access Denied: Only library staff can collect fines at the counter.\"}");
                    return;
                }
                int fineId = Integer.parseInt(data.get("fineId"));
                store.collectFine(librarianId, fineId);
                sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Fine collected successfully! Overdue lock removed.\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
            }
        }
    }

    // =========================================================================
    // API: Librarian Dashboard Data (/api/librarian-data)
    // =========================================================================
    static class LibrarianDataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            store.checkAndExpireReservations();

            StringBuilder sb = new StringBuilder("{");
            sb.append("\"totalFinesCollected\":").append(store.getTotalFinesCollected()).append(",");

            // High demand
            List<Map<String, Object>> highDemand = store.getHighDemandBooks();
            sb.append("\"highDemandBooks\":[");
            for (int i = 0; i < highDemand.size(); i++) {
                Map<String, Object> hd = highDemand.get(i);
                if (i > 0) sb.append(",");
                sb.append("{");
                sb.append("\"bookId\":").append(hd.get("bookId")).append(",");
                sb.append("\"title\":\"").append(JsonUtil.escape((String) hd.get("title"))).append("\",");
                sb.append("\"author\":\"").append(JsonUtil.escape((String) hd.get("author"))).append("\",");
                sb.append("\"branchCode\":\"").append(hd.get("branchCode")).append("\",");
                sb.append("\"rating\":").append(hd.get("rating")).append(",");
                sb.append("\"totalBorrows\":").append(hd.get("totalBorrows")).append(",");
                sb.append("\"availableCopies\":").append(hd.get("availableCopies")).append(",");
                sb.append("\"totalCopies\":").append(hd.get("totalCopies"));
                sb.append("}");
            }
            sb.append("],");

            // Pending Reservations
            List<Reservation> pendingRes = store.getPendingReservations();
            sb.append("\"pendingReservations\":[");
            for (int i = 0; i < pendingRes.size(); i++) {
                Reservation r = pendingRes.get(i);
                User u = store.getUser(r.getUserId());
                Book b = store.getBook(r.getBookId());
                BookCopy c = store.getCopy(r.getCopyId());
                if (i > 0) sb.append(",");
                sb.append("{");
                sb.append("\"reservationId\":").append(r.getReservationId()).append(",");
                sb.append("\"userId\":").append(r.getUserId()).append(",");
                sb.append("\"studentUsn\":\"").append(u != null ? JsonUtil.escape(u.getUsn()) : "").append("\",");
                sb.append("\"studentName\":\"").append(u != null ? JsonUtil.escape(u.getName()) : "Unknown").append("\",");
                sb.append("\"copyId\":").append(r.getCopyId()).append(",");
                sb.append("\"copyCode\":\"").append(c != null ? JsonUtil.escape(c.getCopyCode()) : "").append("\",");
                sb.append("\"bookTitle\":\"").append(b != null ? JsonUtil.escape(b.getTitle()) : "").append("\",");
                sb.append("\"reservedAt\":\"").append(r.getReservedAt().format(TIME_FMT)).append("\",");
                sb.append("\"expiresAt\":\"").append(r.getExpiresAt().format(TIME_FMT)).append("\",");
                sb.append("\"expiresEpoch\":").append(r.getExpiresAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
                sb.append("}");
            }
            sb.append("],");

            // Active On-Time Loans only (Overdue loans are moved to Outstanding Fines & Overdue Counter)
            List<Loan> allActiveLoans = store.getAllActiveLoans();
            sb.append("\"activeLoans\":[");
            int aIdx = 0;
            for (Loan l : allActiveLoans) {
                if (l.isOverdue()) {
                    continue; // Exclude overdue loans from active loans table!
                }
                User u = store.getUser(l.getUserId());
                Book b = store.getBook(l.getBookId());
                BookCopy c = store.getCopy(l.getCopyId());
                if (aIdx > 0) sb.append(",");
                sb.append("{");
                sb.append("\"loanId\":").append(l.getLoanId()).append(",");
                sb.append("\"userId\":").append(l.getUserId()).append(",");
                sb.append("\"studentUsn\":\"").append(u != null ? JsonUtil.escape(u.getUsn()) : "").append("\",");
                sb.append("\"studentName\":\"").append(u != null ? JsonUtil.escape(u.getName()) : "Unknown").append("\",");
                sb.append("\"copyId\":").append(l.getCopyId()).append(",");
                sb.append("\"copyCode\":\"").append(c != null ? JsonUtil.escape(c.getCopyCode()) : "").append("\",");
                sb.append("\"bookTitle\":\"").append(b != null ? JsonUtil.escape(b.getTitle()) : "").append("\",");
                sb.append("\"borrowDate\":\"").append(l.getBorrowDate().format(TIME_FMT)).append("\",");
                sb.append("\"dueDate\":\"").append(l.getDueDate().format(TIME_FMT)).append("\",");
                sb.append("\"dueEpoch\":").append(l.getDueDate().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()).append(",");
                sb.append("\"isOverdue\":false,");
                sb.append("\"daysOverdue\":0,");
                sb.append("\"calculatedFine\":0.0");
                sb.append("}");
                aIdx++;
            }
            sb.append("],");

            // Students
            sb.append("\"students\":[");
            int sIdx = 0;
            for (User u : store.getAllUsers()) {
                if ("STUDENT".equalsIgnoreCase(u.getRole())) {
                    if (sIdx > 0) sb.append(",");
                    sb.append("{");
                    sb.append("\"userId\":").append(u.getUserId()).append(",");
                    sb.append("\"usn\":\"").append(JsonUtil.escape(u.getUsn())).append("\",");
                    sb.append("\"name\":\"").append(JsonUtil.escape(u.getName())).append("\",");
                    sb.append("\"branchCode\":\"").append(u.getBranchCode() != null ? u.getBranchCode() : "").append("\",");
                    sb.append("\"activeLoansCount\":").append(store.getActiveLoansForUser(u.getUserId()).size()).append(",");
                    sb.append("\"hasOverdue\":").append(store.hasOverdueLoans(u.getUserId())).append(",");
                    sb.append("\"unpaidFineAmount\":").append(store.getTotalUnpaidFineForUser(u.getUserId())).append(",");
                    sb.append("\"isReservationBlocked\":").append(u.isReservationBlocked());
                    sb.append("}");
                    sIdx++;
                }
            }
            sb.append("],");

            // Outstanding Student Fines & Overdue Loans (Unified Overdue Counter)
            sb.append("\"unpaidFines\":[");
            int fIdx = 0;
            Set<Integer> processedLoanIds = new HashSet<>();

            // 1. First, include all active loans that ARE overdue:
            for (Loan l : allActiveLoans) {
                if (l.isOverdue()) {
                    processedLoanIds.add(l.getLoanId());
                    User u = store.getUser(l.getUserId());
                    Book b = store.getBook(l.getBookId());
                    BookCopy c = store.getCopy(l.getCopyId());

                    Fine matchingFine = null;
                    for (Fine f : store.getFinesForUser(l.getUserId())) {
                        if (f.getLoanId() == l.getLoanId()) {
                            matchingFine = f;
                            break;
                        }
                    }

                    boolean isFinePaid = (matchingFine != null && "PAID".equalsIgnoreCase(matchingFine.getStatus()));
                    long daysOverdue = l.getDaysOverdue();
                    double fineAmount = matchingFine != null ? matchingFine.getAmount() : l.getCalculatedFine();
                    int fineId = matchingFine != null ? matchingFine.getFineId() : 0;

                    if (fIdx > 0) sb.append(",");
                    sb.append("{");
                    sb.append("\"fineId\":").append(fineId).append(",");
                    sb.append("\"loanId\":").append(l.getLoanId()).append(",");
                    sb.append("\"isBookReturned\":false,");
                    sb.append("\"isFinePaid\":").append(isFinePaid).append(",");
                    sb.append("\"userId\":").append(l.getUserId()).append(",");
                    sb.append("\"studentUsn\":\"").append(u != null ? JsonUtil.escape(u.getUsn()) : "").append("\",");
                    sb.append("\"studentName\":\"").append(u != null ? JsonUtil.escape(u.getName()) : "Unknown").append("\",");
                    sb.append("\"bookTitle\":\"").append(b != null ? JsonUtil.escape(b.getTitle()) : "Unknown Book").append("\",");
                    sb.append("\"copyCode\":\"").append(c != null ? JsonUtil.escape(c.getCopyCode()) : "").append("\",");
                    sb.append("\"daysOverdue\":").append(daysOverdue).append(",");
                    sb.append("\"amount\":").append(fineAmount).append(",");
                    sb.append("\"assessedAt\":\"").append(l.getDueDate().format(TIME_FMT)).append("\"");
                    sb.append("}");
                    fIdx++;
                }
            }

            // 2. Second, include any unpaid fines whose book has already been returned:
            for (User u : store.getAllUsers()) {
                for (Fine f : store.getFinesForUser(u.getUserId())) {
                    if ("UNPAID".equalsIgnoreCase(f.getStatus()) && !processedLoanIds.contains(f.getLoanId())) {
                        Loan l = store.getLoan(f.getLoanId());
                        Book b = (l != null) ? store.getBook(l.getBookId()) : null;
                        BookCopy c = (l != null) ? store.getCopy(l.getCopyId()) : null;
                        boolean isReturned = (l == null || "RETURNED".equalsIgnoreCase(l.getStatus()));

                        if (fIdx > 0) sb.append(",");
                        sb.append("{");
                        sb.append("\"fineId\":").append(f.getFineId()).append(",");
                        sb.append("\"loanId\":").append(f.getLoanId()).append(",");
                        sb.append("\"isBookReturned\":").append(isReturned).append(",");
                        sb.append("\"isFinePaid\":false,");
                        sb.append("\"userId\":").append(u.getUserId()).append(",");
                        sb.append("\"studentUsn\":\"").append(JsonUtil.escape(u.getUsn())).append("\",");
                        sb.append("\"studentName\":\"").append(JsonUtil.escape(u.getName())).append("\",");
                        sb.append("\"bookTitle\":\"").append(b != null ? JsonUtil.escape(b.getTitle()) : "Overdue Book").append("\",");
                        sb.append("\"copyCode\":\"").append(c != null ? JsonUtil.escape(c.getCopyCode()) : "").append("\",");
                        sb.append("\"daysOverdue\":").append(f.getDaysOverdue()).append(",");
                        sb.append("\"amount\":").append(f.getAmount()).append(",");
                        sb.append("\"assessedAt\":\"").append(f.getAssessedAt().format(TIME_FMT)).append("\"");
                        sb.append("}");
                        fIdx++;
                    }
                }
            }
            sb.append("]");

            sb.append("}");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    // =========================================================================
    // API: Book Requests, Voting, and Procure
    // =========================================================================
    static class RequestsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder sb = new StringBuilder("[");
            int idx = 0;
            for (BookRequest req : store.getAllBookRequests()) {
                if (idx > 0) sb.append(",");
                sb.append("{");
                sb.append("\"requestId\":").append(req.getRequestId()).append(",");
                sb.append("\"title\":\"").append(JsonUtil.escape(req.getTitle())).append("\",");
                sb.append("\"author\":\"").append(JsonUtil.escape(req.getAuthor())).append("\",");
                sb.append("\"branchCode\":\"").append(req.getBranchCode()).append("\",");
                sb.append("\"genreName\":\"").append(JsonUtil.escape(req.getGenreName())).append("\",");
                sb.append("\"suggestedByName\":\"").append(JsonUtil.escape(req.getSuggestedByName())).append("\",");
                sb.append("\"votesCount\":").append(req.getVotesCount()).append(",");
                sb.append("\"status\":\"").append(req.getStatus()).append("\",");
                sb.append("\"createdAt\":\"").append(req.getCreatedAt().format(TIME_FMT)).append("\",");
                sb.append("\"votedUserIds\":[");
                int vIdx = 0;
                for (Integer uid : req.getVotedUserIds()) {
                    if (vIdx > 0) sb.append(",");
                    sb.append(uid);
                    vIdx++;
                }
                sb.append("]}");
                idx++;
            }
            sb.append("]");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    static class RequestBookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            Map<String, String> data = JsonUtil.parseSimpleJson(readBody(exchange));
            try {
                int userId = Integer.parseInt(data.get("userId"));
                String title = data.get("title");
                String author = data.get("author");
                String branch = data.get("branchCode");
                String genre = data.get("genreName");
                store.requestNewBook(userId, title, author, branch, genre);
                sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Book request added!\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
            }
        }
    }

    static class VoteRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            Map<String, String> data = JsonUtil.parseSimpleJson(readBody(exchange));
            try {
                int userId = Integer.parseInt(data.get("userId"));
                int reqId = Integer.parseInt(data.get("requestId"));
                boolean voted = store.voteBookRequest(userId, reqId);
                sendJsonResponse(exchange, 200, "{\"success\":" + voted + ",\"message\":\"Vote recorded!\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
            }
        }
    }

    static class UnvoteRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            Map<String, String> data = JsonUtil.parseSimpleJson(readBody(exchange));
            try {
                int userId = Integer.parseInt(data.get("userId"));
                int reqId = Integer.parseInt(data.get("requestId"));
                boolean unvoted = store.unvoteBookRequest(userId, reqId);
                sendJsonResponse(exchange, 200, "{\"success\":" + unvoted + ",\"message\":\"Vote removed!\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
            }
        }
    }

    static class ProcureRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            Map<String, String> data = JsonUtil.parseSimpleJson(readBody(exchange));
            try {
                int librarianId = Integer.parseInt(data.get("librarianId"));
                int reqId = Integer.parseInt(data.get("requestId"));
                Book newBook = store.procureBookRequest(librarianId, reqId);
                sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Book procured and added to catalog with 2 copies!\",\"bookTitle\":\"" + JsonUtil.escape(newBook.getTitle()) + "\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
            }
        }
    }

    // =========================================================================
    // API: Lab Query Execution (/api/lab-query?queryId=query1)
    // =========================================================================
    static class LabQueryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = getQueryParams(exchange.getRequestURI().getQuery());
            String queryId = params.get("queryId");
            Map<String, Object> queryData = store.executeLabQuery(queryId != null ? queryId : "query1");

            StringBuilder sb = new StringBuilder("{");
            sb.append("\"title\":\"").append(JsonUtil.escape((String) queryData.get("title"))).append("\",");
            sb.append("\"concept\":\"").append(JsonUtil.escape((String) queryData.get("concept"))).append("\",");
            sb.append("\"sql\":\"").append(JsonUtil.escape((String) queryData.get("sql"))).append("\",");
            sb.append("\"rows\":[");

            @SuppressWarnings("unchecked")
            List<Map<String, String>> rows = (List<Map<String, String>>) queryData.get("rows");
            if (rows != null) {
                for (int i = 0; i < rows.size(); i++) {
                    Map<String, String> row = rows.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{");
                    int kIdx = 0;
                    for (Map.Entry<String, String> entry : row.entrySet()) {
                        if (kIdx > 0) sb.append(",");
                        sb.append("\"").append(entry.getKey()).append("\":\"").append(JsonUtil.escape(entry.getValue())).append("\"");
                        kIdx++;
                    }
                    sb.append("}");
                }
            }
            sb.append("]}");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    // =========================================================================
    // API: Live SQL Logs & Simulation
    // =========================================================================
    static class SqlLogsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<String> logs = store.getRecentSqlLogs();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < logs.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(JsonUtil.escape(logs.get(i))).append("\"");
            }
            sb.append("]");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    static class ClearSqlLogsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            store.clearSqlLogs();
            sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"SQL transaction logs cleared successfully!\"}");
        }
    }

    static class SimulateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            Map<String, String> data = JsonUtil.parseSimpleJson(readBody(exchange));
            String type = data.get("type");
            Map<String, Object> res = store.simulateTime(type);
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"success\":true,");
            sb.append("\"message\":\"").append(JsonUtil.escape((String) res.get("message"))).append("\"");
            sb.append("}");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    static class ResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            store.resetAllData();
            sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"All data has been reset to seed values.\"}");
        }
    }
}
