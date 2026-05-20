package org.example;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import java.security.MessageDigest;

import java.sql.*;

public class Server {
    public static void main(String[] args) {

        System.setProperty("prism.order", "sw");

        try {
            var conn = DataBase.connect();
            System.out.println("POŁĄCZONO Z MYSQL!");
        } catch (Exception e) {
            System.out.println("BŁĄD MYSQL");
            e.printStackTrace();
        }

        new AuthServer().start();
    }
}

class AuthServer {
    private static final int PORT = 5556;

    private final UserManager userManager = new UserManager();
    private final DriverManager driverManager = new DriverManager();
    private final TicketManager ticketManager = new TicketManager();
    private final MessageManager messageManager = new MessageManager();
    private final ReportManager reportManager = new ReportManager();
    private final PlateManager plateManager = new PlateManager();
    private final StatisticsManager statisticsManager = new StatisticsManager();
    private final PatrolManager patrolManager = new PatrolManager();
    private final CitizenManager citizenManager = new CitizenManager();

    public void start() {

        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Server działa na porcie " + PORT);

            while (true) {
                Socket socket = server.accept();
                new Thread(() -> handle(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handle(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String req = in.readLine();
            if (req == null) return;
            String[] t = req.split("\\|");

            switch (t[0]) {

                case "LOGIN" -> userManager.login(t, out);
                case "LOGOUT" -> userManager.logout(t, out);

                case "ADD_USER" -> userManager.addUser(t, out);

                case "CHECK_DRIVER" -> driverManager.checkDriver(t, out, userManager);
                case "CHECK_PLATE" -> plateManager.checkPlate(t, out, userManager);

                case "ISSUE_TICKET" -> ticketManager.issue(t, out, userManager);
                case "SAVE_REPORT" -> reportManager.save(t, out, userManager);

                case "SEND_MSG" -> messageManager.send(t, out, userManager);
                case "GET_MSGS" -> messageManager.get(t, out);
                case "GET_USERS" -> messageManager.users(t, out, userManager);

                case "GET_STATS" -> statisticsManager.get(t, out, userManager);

                case "ADD_INCIDENT" -> patrolManager.addIncident(t, out, userManager);
                case "GET_INCIDENTS" -> patrolManager.get(t, out, userManager);
                case "ASSIGN_PATROL" -> patrolManager.assign(t, out, userManager);

                case "GET_POINTS" -> citizenManager.getPoints(t, out, driverManager);
                case "GET_TICKETS" -> citizenManager.getTickets(t, out);
                case "PAY_TICKET" -> citizenManager.payTicket(t, out);
                case "GET_INCIDENT_STATUS" -> citizenManager.incidentStatus(t, out);

                case "CHECK_USER" -> out.println("SUCCESS:OK");

                case "GET_PENDING_USERS" -> userManager.getPending(t, out);
                case "APPROVE_USER" -> userManager.approveUser(t, out);

                case "GET_ALL_USERS" -> userManager.getAllUsers(t, out);
                case "DELETE_USER" -> userManager.deleteUser(t, out);

                case "DELETE_PENDING" -> userManager.deletePending(t, out);

                case "CHANGE_PASSWORD" -> userManager.changePassword(t, out);

                case "CREATE_PATROL" -> patrolManager.createPatrol(t, out, userManager);
                case "GET_POLICEMEN" -> patrolManager.getPolicemen(out, userManager);

                case "GET_PATROLS" -> patrolManager.getPatrols(out, userManager, t);

                case "DELETE_PATROL" -> patrolManager.deletePatrol(t, out, userManager);

                case "SEND_PATROL" -> patrolManager.sendPatrol(t, out, userManager);

                case "GET_CLOSEST_PATROL" -> patrolManager.getClosestPatrol(t, out);

                case "SEND_PATROL_INCIDENT" -> patrolManager.sendPatrolFromIncident(t, out, userManager);

                case "GET_PATROLS_DISPATCHES" -> patrolManager.getPatrolsWithDispatches(t, out, userManager);

                case "ADD_PATROL_LOCATION" -> patrolManager.addPatrolLocation(t, out, userManager);

                case "GET_PENDING_REPORTS" -> reportManager.getPendingReports(t, out, userManager);
                case "APPROVE_REPORT" -> reportManager.approveReport(t, out, userManager);

                case "REJECT_REPORT" -> reportManager.rejectReport(t, out, userManager);

                case "GET_MY_REPORTS" -> reportManager.getMyReports(t, out, userManager);

                case "RESUBMIT_REPORT" -> reportManager.resubmitReport(t, out, userManager);

                case "FIX_REPORT" -> reportManager.resubmitReport(t, out, userManager);

                case "GET_MY_PATROL"->patrolManager.getMyPatrol(t, out, userManager);

                case "GET_PATROL_LOCATIONS" -> patrolManager.getPatrolLocations(t, out, userManager);

                case "GET_CONVERSATION" -> messageManager.getConversation(t, out);

                default -> out.println("ERROR");

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class UserManager {

    private final Map<String, String> pass = new ConcurrentHashMap<>();
    private final Map<String, String> role = new ConcurrentHashMap<>();
    private final Map<String, Boolean> logged = new ConcurrentHashMap<>();

    private final List<String[]> pendingUsers = new ArrayList<>();

    public void login(String[] t, PrintWriter out) {

        try (Connection conn = DataBase.connect()) {

            String sql = "SELECT password, role FROM users WHERE username = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, t[1]);

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                out.println("ERROR:Brak użytkownika");
                return;
            }

            String dbPass = rs.getString("password");
            String dbRole = rs.getString("role");

            String inputPass = t[2];
            String inputHash = hash(inputPass);

            boolean ok = dbPass.equals(inputHash);

            if (!ok) {
                ok = dbPass.equals(inputPass);
            }

            if (ok) {

                logged.put(t[1], true);
                role.put(t[1], dbRole);

                if (dbPass.equals(inputPass)) {

                    String update = "UPDATE users SET password = ? WHERE username = ?";
                    PreparedStatement ups = conn.prepareStatement(update);
                    ups.setString(1, inputHash);
                    ups.setString(2, t[1]);
                    ups.executeUpdate();
                }

                out.println("SUCCESS:" + dbRole);

            } else {
                out.println("ERROR:Złe hasło");
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }

    public void logout(String[] t, PrintWriter out) {
        logged.put(t[1], false);
        out.println("SUCCESS:Logout");
    }

    public void addUser(String[] t, PrintWriter out) {

        String newUser = t[2];
        String password = hash(t[3]);
        String roleNew = t[4];

        String extra = t.length > 5 ? t[5] : null;

        try (Connection conn = DataBase.connect()) {

            if ("citizen".equals(roleNew)) {

                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO users(username, password, role, police_id, pesel) VALUES (?, ?, ?, ?, ?)"
                );

                ps.setString(1, newUser);
                ps.setString(2, password);
                ps.setString(3, roleNew);
                ps.setString(4, null);
                ps.setString(5, extra);

                ps.executeUpdate();

                out.println("SUCCESS:Konto utworzone");

            } else {

                pendingUsers.add(new String[]{newUser, password, roleNew, extra});

                out.println("SUCCESS:Do zatwierdzenia");
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }

    public void approveUser(String[] t, PrintWriter out) {

        String chief = t[1];
        String userToApprove = t[2];

        if (!logged(chief) || !isChief(chief)) {
            out.println("ERROR:Tylko komendant");
            return;
        }

        Iterator<String[]> it = pendingUsers.iterator();

        while (it.hasNext()) {

            String[] u = it.next();

            if (u[0].equals(userToApprove)) {

                String username = u[0];
                String password = u[1];
                String roleNew = u[2];
                String policeId = u.length > 3 ? u[3] : null;

                try (Connection conn = DataBase.connect()) {

                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO users(username, password, role, police_id) VALUES (?, ?, ?, ?)"
                    );

                    ps.setString(1, username);
                    ps.setString(2, password);
                    ps.setString(3, roleNew);
                    ps.setString(4, policeId);

                    ps.executeUpdate();

                    pass.put(username, password);
                    role.put(username, roleNew);
                    logged.put(username, false);

                    it.remove();

                    out.println("SUCCESS:Użytkownik zatwierdzony");

                    return;

                } catch (Exception e) {
                    e.printStackTrace();
                    out.println("ERROR:Baza danych");
                    return;
                }
            }
        }

        out.println("ERROR:Nie znaleziono");
    }

    public boolean logged(String u) {
        return logged.getOrDefault(u, false);
    }

    public String role(String u) {
        return role.get(u);
    }

    public boolean isPolice(String u) {
        String r = role(u);
        return "man".equals(r) || "chief".equals(r);
    }

    public boolean isChief(String u) {
        return "chief".equals(role(u));
    }

    public Set<String> users() {
        return pass.keySet();
    }

    public void getPending(String[] t, PrintWriter out) {

        String user = t[1];

        if (!logged(user) || !isChief(user)) {
            out.println("ERROR:Tylko komendant");
            return;
        }

        if (pendingUsers.isEmpty()) {
            out.println("SUCCESS:");
            return;
        }

        List<String> list = new ArrayList<>();

        for (String[] u : pendingUsers) {

            String username = u[0];
            String role = u[2];
            String policeId = u.length > 3 ? u[3] : "-";

            list.add(username + " | " + role + " | ID: " + policeId);
        }

        out.println("SUCCESS:" + String.join(";;", list));
    }

    public void getAllUsers(String[] t, PrintWriter out) {

        String chief = t[1];

        if (!logged(chief) || !isChief(chief)) {
            out.println("ERROR:Tylko komendant");
            return;
        }

        try (Connection conn = DataBase.connect()) {

            String sql = "SELECT username, role, police_id FROM users";
            PreparedStatement ps = conn.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();

            List<String> users = new ArrayList<>();

            while (rs.next()) {

                String u = rs.getString("username");
                String r = rs.getString("role");
                String id = rs.getString("police_id");

                users.add(u + " | " + r + " | ID: " + (id != null ? id : "-"));
            }

            out.println("SUCCESS:" + String.join(";;", users));

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza danych");
        }
    }

    public void deleteUser(String[] t, PrintWriter out) {

        String chief = t[1];
        String userToDelete = t[2];

        if (!logged(chief) || !isChief(chief)) {
            out.println("ERROR:Tylko komendant");
            return;
        }

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM users WHERE username = ?"
            );

            ps.setString(1, userToDelete);

            int rows = ps.executeUpdate();

            if (rows > 0) {

                pass.remove(userToDelete);
                role.remove(userToDelete);
                logged.remove(userToDelete);

                out.println("SUCCESS:Użytkownik usunięty z bazy");

            } else {
                out.println("ERROR:Nie znaleziono użytkownika");
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza danych");
        }
    }

    public void deletePending(String[] t, PrintWriter out) {

        String chief = t[1];
        String userToDelete = t[2];

        if (!logged(chief) || !isChief(chief)) {
            out.println("ERROR:Tylko komendant");
            return;
        }

        boolean removed = pendingUsers.removeIf(u -> u[0].equals(userToDelete));

        if (removed) {
            out.println("SUCCESS:Usunięto z oczekujących");
        } else {
            out.println("ERROR:Nie znaleziono");
        }
    }

    public void changePassword(String[] t, PrintWriter out) {

        if (t.length < 4) {
            out.println("ERROR:Za mało danych");
            return;
        }

        String username = t[1];
        String oldPass = t[2];
        String newPass = t[3];

        if (!logged(username)) {
            out.println("ERROR:Nie jesteś zalogowany");
            return;
        }

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT password FROM users WHERE username = ?"
            );

            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                out.println("ERROR:Użytkownik nie istnieje");
                return;
            }

            String dbPass = rs.getString("password");

            if (!dbPass.equals(oldPass)) {
                out.println("ERROR:Niepoprawne stare hasło");
                return;
            }

            PreparedStatement update = conn.prepareStatement(
                    "UPDATE users SET password = ? WHERE username = ?"
            );

            update.setString(1, newPass);
            update.setString(2, username);

            update.executeUpdate();

            pass.put(username, newPass);

            out.println("SUCCESS:Hasło zmienione");

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza danych");
        }
    }

    public void getPolicemen(PrintWriter out) {

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT username FROM users WHERE role = 'man'"
            );

            ResultSet rs = ps.executeQuery();

            List<String> list = new ArrayList<>();

            while (rs.next()) {
                list.add(rs.getString("username"));
            }

            out.println("SUCCESS:" + String.join(";;", list));

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }

    private String hash(String password) {
        try {
            java.security.MessageDigest md =
                    java.security.MessageDigest.getInstance("SHA-256");

            byte[] hash = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();

            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException("Błąd hashowania", e);
        }
    }
}

class DriverManager {

    public void checkDriver(String[] t, PrintWriter out, UserManager u) {

        if (!u.logged(t[1])) {
            out.println("ERROR:Nie zalogowany");
            return;
        }

        String peselInput = t[2];

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM drivers WHERE pesel = ?"
            );

            ps.setString(1, peselInput);

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                out.println("ERROR:Brak kierowcy");
                return;
            }

            String driverId = rs.getString("id");
            String name = rs.getString("name");
            String surname = rs.getString("surname");
            boolean warrant = rs.getBoolean("warrant");

            PreparedStatement ps2 = conn.prepareStatement(
                    "SELECT COALESCE(SUM(points),0) FROM tickets WHERE driver_id = ?"
            );

            ps2.setString(1, driverId);

            ResultSet rs2 = ps2.executeQuery();

            int points = rs2.next() ? rs2.getInt(1) : 0;

            String status = warrant ? "UWAGA: Poszukiwany!" : "OK";

            out.println("SUCCESS:" + name + " " + surname +
                    " | Punkty: " + points +
                    " | " + status);

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }
}

    class TicketManager {

        public void issue(String[] t, PrintWriter out, UserManager u) {

            try (Connection conn = DataBase.connect()) {

                String user = t[1];

                if (!u.logged(user) || !u.isPolice(user)) {
                    out.println("ERROR:Brak dostępu");
                    return;
                }

                if (t.length < 8) {
                    out.println("ERROR:Za mało danych");
                    return;
                }

                String pesel = t[2];
                String name = t[3];
                String surname = t[4];
                int points = Integer.parseInt(t[5]);
                int fine = Integer.parseInt(t[6]);
                String reason = t[7];

                if (pesel.length() != 11) {
                    out.println("ERROR:Niepoprawny PESEL");
                    return;
                }

                int driverId = getOrCreateDriver(conn, pesel, name, surname);

                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO tickets " +
                                "(driver_id, police_user, points, fine, reason, paid, issued_at) " +
                                "VALUES (?, ?, ?, ?, ?, ?, CURDATE())"
                );


                ps.setInt(1, driverId);
                ps.setString(2, user);
                ps.setInt(3, points);
                ps.setInt(4, fine);
                ps.setString(5, reason);
                ps.setBoolean(6, false);

                ps.executeUpdate();

                out.println("SUCCESS:Mandat wystawiony");

            } catch (Exception e) {
                e.printStackTrace();
                out.println("ERROR:" + e.getMessage());
            }
        }

        private int getOrCreateDriver(Connection conn, String pesel, String name, String surname) throws Exception {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM drivers WHERE pesel = ?"
            );

            ps.setString(1, pesel);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

            PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO drivers(pesel, name, surname) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );

            insert.setString(1, pesel);
            insert.setString(2, name);
            insert.setString(3, surname);

            insert.executeUpdate();

            ResultSet keys = insert.getGeneratedKeys();

            if (keys.next()) {
                return keys.getInt(1);
            }

            return -1;
        }

    public void pay(String[] t, PrintWriter out) {

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE tickets SET paid = true WHERE id = ?"
            );

            ps.setString(1, t[1]);

            int rows = ps.executeUpdate();

            if (rows == 0) {
                out.println("ERROR:Nie znaleziono mandatu");
            } else {
                out.println("SUCCESS:Opłacono mandat");
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }
}

class Ticket {

    private String id;
    private String driverId;
    private String policeUser;
    private int points;
    private String reason;
    private boolean paid;

    public Ticket(String id, String driverId, String policeUser,
                  int points, String reason, boolean paid) {
        this.id = id;
        this.driverId = driverId;
        this.policeUser = policeUser;
        this.points = points;
        this.reason = reason;
        this.paid = paid;
    }

    public String getId() { return id; }
    public String getDriverId() { return driverId; }
    public String getPoliceUser() { return policeUser; }
    public int getPoints() { return points; }
    public String getReason() { return reason; }
    public boolean isPaid() { return paid; }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public String toFileLine() {
        return id + "," + driverId + "," + policeUser + "," +
                points + "," + reason + "," + paid;
    }

    public static Ticket fromLine(String line) {
        String[] s = line.split(",", 6);

        return new Ticket(
                s[0],
                s[1],
                s[2],
                Integer.parseInt(s[3]),
                s[4],
                Boolean.parseBoolean(s[5])
        );
    }
}

class MessageManager {

    public void send(String[] t, PrintWriter out, UserManager u) {
        if (t.length < 4) {
            out.println("ERROR:Za mało danych");
            return;
        }

        String sender = t[1];
        String receiver = t[2];
        String message = String.join(" ", Arrays.copyOfRange(t, 3, t.length));

        if (!u.logged(sender)) {
            out.println("ERROR:Nie jesteś zalogowany");
            return;
        }

        if (sender.equals(receiver)) {
            out.println("ERROR:Nie możesz wysłać wiadomości do siebie");
            return;
        }

        try (Connection conn = DataBase.connect()) {
            String sql = "INSERT INTO messages(sender, receiver, message, sent_at) VALUES(?,?,?,NOW())";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setString(3, message);

            ps.executeUpdate();

            out.println("SUCCESS:Wiadomość wysłana");
        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Błąd bazy danych");
        }
    }

    public void get(String[] t, PrintWriter out) {
        if (t.length < 2) {
            out.println("ERROR:Brak użytkownika");
            return;
        }

        String user = t[1];

        try (Connection conn = DataBase.connect()) {
            String sql = """
                SELECT sender, message, sent_at 
                FROM messages 
                WHERE receiver = ? 
                ORDER BY sent_at DESC
                """;

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, user);

            ResultSet rs = ps.executeQuery();

            List<String> messages = new ArrayList<>();

            while (rs.next()) {
                messages.add(
                        rs.getString("sender") + "|" +
                                rs.getTimestamp("sent_at") + "|" +
                                rs.getString("message")
                );
            }

            out.println("SUCCESS:" + String.join(";;", messages));

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Błąd bazy");
        }
    }

    public void getConversation(String[] t, PrintWriter out) {
        if (t.length < 3) {
            out.println("ERROR:Za mało danych");
            return;
        }

        String user1 = t[1];
        String user2 = t[2];

        try (Connection conn = DataBase.connect()) {
            String sql = """
                SELECT sender, message, sent_at 
                FROM messages 
                WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?)
                ORDER BY sent_at ASC
                """;

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, user1);
            ps.setString(2, user2);
            ps.setString(3, user2);
            ps.setString(4, user1);

            ResultSet rs = ps.executeQuery();

            List<String> messages = new ArrayList<>();

            while (rs.next()) {
                messages.add(
                        rs.getString("sender") + "|" +
                                rs.getTimestamp("sent_at") + "|" +
                                rs.getString("message")
                );
            }

            out.println("SUCCESS:" + String.join(";;", messages));

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Błąd bazy");
        }
    }

    public void users(String[] t, PrintWriter out, UserManager u) {
        try (Connection conn = DataBase.connect()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT username, role FROM users ORDER BY username"
            );

            ResultSet rs = ps.executeQuery();
            List<String> users = new ArrayList<>();

            while (rs.next()) {
                String username = rs.getString("username");
                String role = rs.getString("role");
                users.add(username + "|" + role);
            }

            if (users.isEmpty()) {
                out.println("SUCCESS:");
            } else {
                out.println("SUCCESS:" + String.join(";;", users));
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Błąd bazy");
        }
    }
}

class StatisticsManager {

    public void get(String[] t, PrintWriter out, UserManager u) {

        String user = t[1];

        if (!u.logged(user) || !u.isChief(user)) {
            out.println("ERROR:Tylko komendant");
            return;
        }

        String mode = t.length > 2 ? t[2] : "ALL";

        String where = "";

        switch (mode) {

            case "DAY" ->
                    where = "WHERE DATE(issued_at) = CURDATE()";

            case "WEEK" ->
                    where = "WHERE issued_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)";

            case "MONTH" ->
                    where = "WHERE issued_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)";

            default ->
                    where = "";
        }

        try (Connection conn = DataBase.connect()) {

            String sql =
                    "SELECT DATE(issued_at) AS day, reason, COUNT(*) AS total " +
                            "FROM tickets " +
                            where + " " +
                            "GROUP BY DATE(issued_at), reason " +
                            "ORDER BY day DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            List<String> stats = new ArrayList<>();

            while (rs.next()) {
                stats.add(
                        rs.getString("day") + " | " +
                                rs.getString("reason") + " = " +
                                rs.getInt("total")
                );
            }

            if (stats.isEmpty()) {
                out.println("SUCCESS:Brak danych");
            } else {
                out.println("SUCCESS:" + String.join(",", stats));
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }
}

class PatrolManager {

    private static final String FILE = "incidents.txt";

    public void add(String[] t, PrintWriter out) {
        if (t.length < 2) {
            out.println("ERROR:Brak opisu");
            return;
        }

        String desc = String.join(" ", Arrays.copyOfRange(t, 1, t.length));

        String line = LocalDateTime.now() + ",UNASSIGNED," + desc;

        try {
            Files.write(Paths.get(FILE),
                    (line + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            out.println("SUCCESS:Zgłoszenie przyjęte");

        } catch (IOException e) {
            out.println("ERROR");
        }
    }

    public void get(String[] t, PrintWriter out, UserManager u) {

        String user = t[1];

        if (!u.logged(user) || !u.isPolice(user)) {
            out.println("ERROR:Brak dostępu");
            return;
        }

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, user, address, reason, status FROM incidents ORDER BY id DESC"
            );

            ResultSet rs = ps.executeQuery();

            List<String> list = new ArrayList<>();

            while (rs.next()) {

                String id = rs.getString("id");
                String uName = rs.getString("user");
                String address = rs.getString("address");
                String reason = rs.getString("reason");
                String status = rs.getString("status");

                String formatted =
                        "ID: " + id +
                                " | User: " + uName +
                                " | Adres: " + address +
                                " | Powód: " + reason +
                                " | Status: " + status;

                list.add(formatted);
            }

            if (list.isEmpty()) {
                out.println("SUCCESS:Brak zgłoszeń");
            } else {
                out.println("SUCCESS:" + String.join(";;", list));
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }

    public void assign(String[] t, PrintWriter out, UserManager u) {

        if (t.length != 3) {
            out.println("ERROR:ASSIGN_PATROL id user");
            return;
        }

        String user = t[2];

        if (!u.logged(user) || !u.isPolice(user)) {
            out.println("ERROR:Brak dostępu");
            return;
        }

        int id = Integer.parseInt(t[1]);

        try {
            Path p = Paths.get(FILE);
            List<String> lines = Files.readAllLines(p);

            if (id >= lines.size()) {
                out.println("ERROR:Brak zgłoszenia");
                return;
            }

            String[] parts = lines.get(id).split(",", 3);

            lines.set(id, parts[0] + "," + user + "," + parts[2]);

            Files.write(p, lines);

            out.println("Patrol przypisany");

        } catch (IOException e) {
            out.println("ERROR");
        }
    }

    public void getPolicemen(PrintWriter out, UserManager u) {

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT username, police_id FROM users WHERE role = 'man'"
            );

            ResultSet rs = ps.executeQuery();

            List<String> list = new ArrayList<>();

            while (rs.next()) {

                String username = rs.getString("username");
                String id = rs.getString("police_id");

                if (id == null || id.isBlank()) {
                    id = "-";
                }

                list.add(username + "|" + id);
            }

            out.println("SUCCESS:" + String.join(";;", list));

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }

    public void createPatrol(String[] t, PrintWriter out, UserManager u) {

        System.out.println("CREATE_PATROL: " + Arrays.toString(t));

        if (t.length < 3) {
            out.println("ERROR:Za mało danych");
            return;
        }

        String chief = t[1];

        if (!u.logged(chief) || !u.isChief(chief)) {
            out.println("ERROR:Brak dostępu");
            return;
        }

        String membersRaw = String.join(",", Arrays.copyOfRange(t, 2, t.length));

        try (Connection conn = DataBase.connect()) {

            conn.setAutoCommit(false);

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO patrols(created_by, members) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );

            ps.setString(1, chief);
            ps.setString(2, membersRaw);

            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();

            if (!keys.next()) {
                conn.rollback();
                out.println("ERROR:Nie utworzono patrolu");
                return;
            }

            int patrolId = keys.getInt(1);

            String[] members = membersRaw.split(",");

            PreparedStatement deleteOld = conn.prepareStatement(
                    "DELETE FROM patrol_members WHERE username = ?"
            );

            PreparedStatement insertMember = conn.prepareStatement(
                    "INSERT INTO patrol_members(patrol_id, username, police_id) VALUES (?, ?, ?)"
            );

            for (String m : members) {

                String[] parts = m.split("\\|");

                String username = parts[0];
                String policeId = parts.length > 1 ? parts[1] : null;

                deleteOld.setString(1, username);
                deleteOld.executeUpdate();

                insertMember.setInt(1, patrolId);
                insertMember.setString(2, username);
                insertMember.setString(3, policeId);

                insertMember.addBatch();
            }

            insertMember.executeBatch();

            conn.commit();

            out.println("SUCCESS:Patrol utworzony (ID=" + patrolId + ")");

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }

    public void getPatrols(PrintWriter out, UserManager u, String[] t) {

        String user = t[1];

        if (!u.logged(user) || !u.isChief(user)) {
            out.println("ERROR:Brak dostępu");
            return;
        }

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT p.id, p.created_by, p.created_at, pm.username, pm.police_id " +
                            "FROM patrols p " +
                            "JOIN patrol_members pm ON p.id = pm.patrol_id " +
                            "ORDER BY p.id"
            );

            ResultSet rs = ps.executeQuery();

            Map<Integer, List<String>> patrolMap = new LinkedHashMap<>();
            Map<Integer, String> infoMap = new HashMap<>();

            while (rs.next()) {

                int id = rs.getInt("id");
                String createdBy = rs.getString("created_by");
                String createdAt = rs.getString("created_at");

                String username = rs.getString("username");
                String policeId = rs.getString("police_id");

                String member = username + " (ID: " + policeId + ")";

                patrolMap.computeIfAbsent(id, k -> new ArrayList<>()).add(member);

                infoMap.put(id, "Patrol #" + id + " | utworzył: " + createdBy + " | " + createdAt);
            }

            List<String> result = new ArrayList<>();

            for (Integer id : patrolMap.keySet()) {

                String header = infoMap.get(id);
                String members = String.join(", ", patrolMap.get(id));

                result.add(header + " -> [" + members + "]");
            }

            if (result.isEmpty()) {
                out.println("SUCCESS:Brak patroli");
            } else {
                out.println("SUCCESS:" + String.join(";;", result));
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }

    public void deletePatrol(String[] t, PrintWriter out, UserManager u) {

        if (t.length < 3) {
            out.println("ERROR:Brak danych");
            return;
        }

        String chief = t[1];

        if (!u.logged(chief) || !u.isChief(chief)) {
            out.println("ERROR:Brak dostępu");
            return;
        }

        int patrolId;

        try {
            patrolId = Integer.parseInt(t[2]);
        } catch (Exception e) {
            out.println("ERROR:Niepoprawne ID");
            return;
        }

        try (Connection conn = DataBase.connect()) {

            conn.setAutoCommit(false);

            PreparedStatement ps1 = conn.prepareStatement(
                    "DELETE FROM patrol_members WHERE patrol_id = ?"
            );

            ps1.setInt(1, patrolId);

            ps1.executeUpdate();

            PreparedStatement ps2 = conn.prepareStatement(
                    "DELETE FROM patrols WHERE id = ?"
            );

            ps2.setInt(1, patrolId);

            int rows = ps2.executeUpdate();

            if (rows == 0) {
                conn.rollback();
                out.println("ERROR:Nie znaleziono patrolu");
                return;
            }

            conn.commit();

            out.println("SUCCESS:Patrol usunięty");

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }

    public void sendPatrol(String[] t, PrintWriter out, UserManager u) {

        if (t.length < 4) {
            out.println("ERROR:Za mało danych");
            return;
        }

        String chief = t[1];

        if (!u.logged(chief) || !u.isChief(chief)) {
            out.println("ERROR:Brak dostępu");
            return;
        }

        int patrolId;
        String address = t[3];

        try {
            patrolId = Integer.parseInt(t[2]);
        } catch (Exception e) {
            out.println("ERROR:Złe ID patrolu");
            return;
        }

        if (address == null || address.isBlank()) {
            out.println("ERROR:Pusty adres");
            return;
        }

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO patrol_dispatches " +
                            "(patrol_id, incident_id, target_address, latitude, longitude) " +
                            "VALUES (?, NULL, ?, 0, 0)"
            );

            ps.setInt(1, patrolId);
            ps.setString(2, address);

            ps.executeUpdate();

            out.println("SUCCESS:Patrol wysłany");

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }

    public void getClosestPatrol(String[] t, PrintWriter out) {

        if (t.length < 3) {
            out.println("ERROR:Za mało danych");
            return;
        }

        double targetLat = Double.parseDouble(t[1]);
        double targetLon = Double.parseDouble(t[2]);

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT pm.patrol_id, pl.username, pl.latitude, pl.longitude " +
                            "FROM patrol_locations pl " +
                            "JOIN patrol_members pm ON pl.username = pm.username"
            );

            ResultSet rs = ps.executeQuery();

            double bestDistance = Double.MAX_VALUE;
            int bestPatrol = -1;
            String bestOfficer = "";

            while (rs.next()) {

                int patrolId = rs.getInt("patrol_id");

                double lat = rs.getDouble("latitude");
                double lon = rs.getDouble("longitude");

            }

            if (bestPatrol == -1) {
                out.println("ERROR:Brak patroli");
                return;
            }

            out.println(
                    "SUCCESS:Najbliższy patrol #" +
                            bestPatrol +
                            " | Funkcjonariusz: " +
                            bestOfficer +
                            " | Odległość: " +
                            String.format("%.2f", bestDistance) +
                            " km"
            );

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }

    public void addIncident(String[] t, PrintWriter out, UserManager u) {

        if (t.length < 4) {
            out.println("ERROR:Za mało danych");
            return;
        }

        String user = t[1];
        String address = t[2];
        String reason = t[3];

        if (!u.logged(user)) {
            out.println("ERROR:Nie zalogowany");
            return;
        }

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO incidents(user, address, reason, status) VALUES (?, ?, ?, ?)"
            );

            ps.setString(1, user);
            ps.setString(2, address);
            ps.setString(3, reason);
            ps.setString(4, "UNASSIGNED");

            ps.executeUpdate();

            out.println("SUCCESS:Zgłoszenie zapisane");

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }

    public void sendPatrolFromIncident(String[] t, PrintWriter out, UserManager u) {

        if (t.length < 4) {
            out.println("ERROR:Za mało danych");
            return;
        }

        String chief = t[1];

        if (!u.logged(chief) || !u.isChief(chief)) {
            out.println("ERROR:Brak dostępu");
            return;
        }

        int patrolId;
        int incidentId;

        try {
            patrolId = Integer.parseInt(t[2]);
            incidentId = Integer.parseInt(t[3]);
        } catch (Exception e) {
            out.println("ERROR:Złe ID");
            return;
        }

        try (Connection conn = DataBase.connect()) {

            conn.setAutoCommit(false);

            PreparedStatement ps1 = conn.prepareStatement(
                    "SELECT address FROM incidents WHERE id = ?"
            );

            ps1.setInt(1, incidentId);
            ResultSet rs = ps1.executeQuery();

            if (!rs.next()) {
                conn.rollback();
                out.println("ERROR:Nie znaleziono zgłoszenia");
                return;
            }

            String address = rs.getString("address");

            PreparedStatement ps2 = conn.prepareStatement(
                    "INSERT INTO patrol_dispatches " +
                            "(patrol_id, incident_id, target_address, latitude, longitude) " +
                            "VALUES (?, ?, ?, 0, 0)"
            );

            ps2.setInt(1, patrolId);
            ps2.setInt(2, incidentId);
            ps2.setString(3, address);

            ps2.executeUpdate();

            PreparedStatement ps3 = conn.prepareStatement(
                    "UPDATE incidents SET status = 'ASSIGNED' WHERE id = ?"
            );

            ps3.setInt(1, incidentId);
            ps3.executeUpdate();

            conn.commit();

            out.println("SUCCESS:Patrol wysłany na zgłoszenie");

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }

    public void getIncidents(String[] t, PrintWriter out, UserManager u) {

        String user = t[1];

        if (!u.logged(user)) {
            out.println("ERROR:Brak dostępu");
            return;
        }

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, user, address, reason, status FROM incidents ORDER BY id DESC"
            );

            ResultSet rs = ps.executeQuery();

            List<String> list = new ArrayList<>();

            while (rs.next()) {

                list.add(
                        "ID: " + rs.getInt("id") +
                                " | User: " + rs.getString("user") +
                                " | Adres: " + rs.getString("address") +
                                " | Powód: " + rs.getString("reason") +
                                " | Status: " + rs.getString("status")
                );
            }

            out.println("SUCCESS:" + String.join(";;", list));

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }

    public void getPatrolsWithDispatches(String[] t, PrintWriter out, UserManager u) {

        String user = t[1];

        if (!u.logged(user)) {
            out.println("ERROR:Brak dostępu");
            return;
        }

        try (Connection conn = DataBase.connect()) {

            String sql =
                    "SELECT p.id AS patrol_id, p.created_by, p.created_at, " +
                            "GROUP_CONCAT( " +
                            "CONCAT(d.incident_id, '|', d.target_address, '|', d.created_at) " +
                            "SEPARATOR '@@' " +
                            ") AS dispatches " +
                            "FROM patrols p " +
                            "LEFT JOIN patrol_dispatches d ON p.id = d.patrol_id " +
                            "GROUP BY p.id, p.created_by, p.created_at " +
                            "ORDER BY p.id DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            List<String> result = new ArrayList<>();

            while (rs.next()) {

                int patrolId = rs.getInt("patrol_id");
                String createdBy = rs.getString("created_by");
                String createdAt = rs.getString("created_at");
                String dispatches = rs.getString("dispatches");

                if (dispatches == null) dispatches = "";

                result.add(
                        patrolId + "|" +
                                createdBy + "|" +
                                createdAt + "|" +
                                dispatches
                );
            }

            out.println("SUCCESS:" + String.join(";;", result));

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }

    public void addPatrolLocation(String[] t, PrintWriter out, UserManager u) {
        if (t.length < 4) {
            out.println("ERROR:Za mało danych");
            return;
        }

        String user = t[1];
        double lat, lon;

        try {
            lat = Double.parseDouble(t[2]);
            lon = Double.parseDouble(t[3]);
        } catch (Exception e) {
            out.println("ERROR:Złe współrzędne");
            return;
        }

        if (!u.logged(user) || !u.isPolice(user)) {
            out.println("ERROR:Brak dostępu");
            return;
        }

        try (Connection conn = DataBase.connect()) {
            conn.setAutoCommit(false);

            PreparedStatement findPatrol = conn.prepareStatement(
                    "SELECT patrol_id FROM patrol_members WHERE username = ? LIMIT 1"
            );
            findPatrol.setString(1, user);
            ResultSet rs = findPatrol.executeQuery();

            int patrolId;
            if (rs.next()) {
                patrolId = rs.getInt("patrol_id");
            } else {
                out.println("ERROR:Brak przypisanego patrolu");
                return;
            }

            PreparedStatement delete = conn.prepareStatement(
                    "DELETE FROM patrol_locations WHERE patrol_id = ?"
            );
            delete.setInt(1, patrolId);
            delete.executeUpdate();

            PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO patrol_locations(patrol_id, username, latitude, longitude, updated_at) " +
                            "VALUES (?, ?, ?, ?, NOW())"
            );
            insert.setInt(1, patrolId);
            insert.setString(2, user);
            insert.setDouble(3, lat);
            insert.setDouble(4, lon);
            insert.executeUpdate();

            conn.commit();
            out.println("SUCCESS:Lokalizacja zapisana");

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza: " + e.getMessage());
        }
    }

    public void getMyPatrol(String[] t, PrintWriter out, UserManager u) {

        if (t.length < 2) {
            out.println("ERROR:BAD_REQUEST");
            return;
        }

        String username = t[1];

        if (!u.logged(username) || !u.isPolice(username)) {
            out.println("ERROR:BRAK_DOSTEPU");
            return;
        }

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM patrols WHERE members LIKE ?"
            );

            ps.setString(1, "%" + username + "%");

            ResultSet rs = ps.executeQuery();

            List<String> list = new ArrayList<>();

            while (rs.next()) {

                String row =
                        "Patrol #" + rs.getInt("id") +
                                " | Dowódca: " + rs.getString("created_by") +
                                " | Funkcjonariusze: " + rs.getString("members");

                list.add(row);
            }

            if (list.isEmpty()) {
                out.println("SUCCESS:BRAK_PATROLU");
            } else {
                out.println("SUCCESS:" + String.join(";;", list));
            }

        } catch (Exception e) {

            e.printStackTrace();

            out.println("ERROR:DB_ERROR");
        }
    }

    public void getPatrolLocations(String[] t, PrintWriter out, UserManager u) {
        String user = t[1];

        if (!u.logged(user)) {
            out.println("ERROR:Brak dostępu");
            return;
        }

        try (Connection conn = DataBase.connect()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT patrol_id, username, latitude, longitude FROM patrol_locations"
            );
            ResultSet rs = ps.executeQuery();

            List<String> list = new ArrayList<>();
            while (rs.next()) {
                list.add(rs.getInt("patrol_id") + "|" +
                        rs.getString("username") + "|" +
                        rs.getDouble("latitude") + "|" +
                        rs.getDouble("longitude"));
            }
            out.println("SUCCESS:" + String.join(";;", list));
        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }
}

class PlateManager {

    public void checkPlate(String[] t, PrintWriter out, UserManager u) {

        if (t.length < 3) {
            out.println("ERROR:Za mało danych");
            return;
        }

        String officer = t[1];
        String plate = t[2];

        if (!u.logged(officer) || !u.isPolice(officer)) {
            out.println("ERROR:Brak dostępu");
            return;
        }

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM vehicles WHERE plate = ?"
            );

            ps.setString(1, plate);

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                out.println("ERROR:Brak pojazdu");
                return;
            }

            String brand = rs.getString("brand");
            String model = rs.getString("model");
            String color = rs.getString("color");

            String vin = rs.getString("vin");

            int year = rs.getInt("production_year");

            String ownerName = rs.getString("owner_name");
            String ownerSurname = rs.getString("owner_surname");
            String ownerPesel = rs.getString("owner_pesel");

            String registration = rs.getString("registration_number");

            Date inspection = rs.getDate("inspection_valid_until");
            Date insurance = rs.getDate("insurance_valid_until");

            boolean stolen = rs.getBoolean("stolen");
            boolean wanted = rs.getBoolean("wanted");

            String stolenNote = rs.getString("stolen_note");
            String notes = rs.getString("notes");

            StringBuilder response = new StringBuilder();

            response.append("POJAZD: ")
                    .append(brand)
                    .append(" ")
                    .append(model);

            response.append(" | Kolor: ")
                    .append(color);

            response.append(" | Rok: ")
                    .append(year);

            response.append(" | VIN: ")
                    .append(vin);

            response.append(" | Nr rej.: ")
                    .append(registration);

            response.append(" | Przegląd do: ")
                    .append(inspection);

            response.append(" | OC do: ")
                    .append(insurance);

            response.append(" | Właściciel: ")
                    .append(ownerName)
                    .append(" ")
                    .append(ownerSurname);

            response.append(" | PESEL: ")
                    .append(ownerPesel);

            if (stolen) {

                response.append(" | UWAGA: KRADZIONY");

                if (stolenNote != null && !stolenNote.isBlank()) {

                    response.append(" (")
                            .append(stolenNote)
                            .append(")");
                }
            }

            if (wanted) {
                response.append(" | POSZUKIWANY");
            }

            if (notes != null && !notes.isBlank()) {

                response.append(" | Notatki: ")
                        .append(notes);
            }

            out.println("SUCCESS:" + response);

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }
}

class ReportManager {

    public void save(String[] t, PrintWriter out, UserManager u) {

        if (t.length < 4) {
            out.println("ERROR:Za mało danych (SAVE_REPORT)");
            return;
        }

        String user = t[1];
        String role = t[2];

        if (!u.logged(user)) {
            out.println("ERROR:Nie zalogowany");
            return;
        }

        String content = String.join(" ",
                Arrays.copyOfRange(t, 3, t.length));

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO reports(username, role, content, status) VALUES (?, ?, ?, 'PENDING')"
            );

            ps.setString(1, user);
            ps.setString(2, role);
            ps.setString(3, content);

            ps.executeUpdate();

            out.println("SUCCESS:Raport wysłany");

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }

    public void getPendingReports(String[] t, PrintWriter out, UserManager u) {

        if (t == null || t.length < 2) {
            out.println("ERROR:BAD_REQUEST");
            return;
        }

        String user = t[1];

        if (!u.logged(user) || !u.isChief(user)) {
            out.println("ERROR:ACCESS_DENIED");
            return;
        }

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, username, role, content, created_at " +
                            "FROM reports " +
                            "WHERE status = 'PENDING' " +
                            "ORDER BY id DESC"
            );

            ResultSet rs = ps.executeQuery();

            List<String> list = new ArrayList<>();

            while (rs.next()) {

                String row =
                        rs.getInt("id") + "|" +
                                rs.getTimestamp("created_at") + "|" +
                                rs.getString("username") + "|" +
                                rs.getString("role") + "|" +
                                rs.getString("content");

                list.add(row);
            }

            if (list.isEmpty()) {
                out.println("SUCCESS:EMPTY");
            } else {
                out.println("SUCCESS:" + String.join(";;", list));
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:DB_ERROR");
        }
    }

    public void approveReport(String[] t, PrintWriter out, UserManager u) {

        if (t == null || t.length < 3) {
            out.println("ERROR:BAD_REQUEST");
            return;
        }

        String chief = t[1];

        if (!u.logged(chief) || !u.isChief(chief)) {
            out.println("ERROR:ACCESS_DENIED");
            return;
        }

        int id;

        try {
            id = Integer.parseInt(t[2]);
        } catch (NumberFormatException e) {
            out.println("ERROR:INVALID_ID");
            return;
        }

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE reports SET status='APPROVED', updated_at=NOW() WHERE id=?"
            );

            ps.setInt(1, id);

            int rows = ps.executeUpdate();

            if (rows == 0) {
                out.println("ERROR:NOT_FOUND");
            } else {
                out.println("SUCCESS:APPROVED");
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:DB_ERROR");
        }
    }

    public void rejectReport(String[] t, PrintWriter out, UserManager u) {

        if (t == null || t.length < 3) {
            out.println("ERROR:BAD_REQUEST");
            return;
        }

        String chief = t[1];

        if (!u.logged(chief) || !u.isChief(chief)) {
            out.println("ERROR:ACCESS_DENIED");
            return;
        }

        int id;

        try {
            id = Integer.parseInt(t[2]);
        } catch (NumberFormatException e) {
            out.println("ERROR:INVALID_ID");
            return;
        }

        String comment = (t.length > 3)
                ? String.join(" ", Arrays.copyOfRange(t, 3, t.length))
                : "";

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE reports " +
                            "SET status='REVISION', comment=?, updated_at=NOW() " +
                            "WHERE id=?"
            );

            ps.setString(1, comment);
            ps.setInt(2, id);

            int rows = ps.executeUpdate();

            if (rows == 0) {
                out.println("ERROR:NOT_FOUND");
            } else {
                out.println("SUCCESS:REJECTED");
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:DB_ERROR");
        }
    }

    public void getMyReports(String[] t, PrintWriter out, UserManager u) {

        if (t.length < 2) {
            out.println("ERROR:BAD_REQUEST");
            return;
        }

        String username = t[1];

        if (!u.logged(username)) {
            out.println("ERROR:NOT_LOGGED");
            return;
        }

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, content, status, comment, created_at, updated_at " +
                            "FROM reports " +
                            "WHERE username=? " +
                            "ORDER BY id DESC"
            );

            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();

            List<String> list = new ArrayList<>();

            while (rs.next()) {

                String row =
                        rs.getInt("id") + "|" +
                                rs.getString("status") + "|" +
                                (rs.getString("comment") == null
                                        ? "-"
                                        : rs.getString("comment")) + "|" +
                                rs.getTimestamp("created_at") + "|" +
                                rs.getString("content");

                list.add(row);
            }

            if (list.isEmpty()) {
                out.println("SUCCESS:EMPTY");
            } else {
                out.println("SUCCESS:" + String.join(";;", list));
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:DB_ERROR");
        }
    }

    public void resubmitReport(String[] t, PrintWriter out, UserManager u) {

        if (t.length < 4) {
            out.println("ERROR:BAD_REQUEST");
            return;
        }

        String user = t[1];

        if (!u.logged(user)) {
            out.println("ERROR:NOT_LOGGED");
            return;
        }

        int reportId;

        try {
            reportId = Integer.parseInt(t[2]);
        } catch (Exception e) {
            out.println("ERROR:BAD_ID");
            return;
        }

        String newContent = String.join(" ",
                Arrays.copyOfRange(t, 3, t.length));

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE reports " +
                            "SET content=?, status='PENDING', comment=NULL, updated_at=NOW() " +
                            "WHERE id=? AND username=? AND status='REVISION'"
            );

            ps.setString(1, newContent);
            ps.setInt(2, reportId);
            ps.setString(3, user);

            int rows = ps.executeUpdate();

            if (rows == 0) {
                out.println("ERROR:NOT_ALLOWED");
            } else {
                out.println("SUCCESS:UPDATED");
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:DB");
        }
    }
}

class CitizenManager {

    public void getPoints(String[] t, PrintWriter out, DriverManager driverManager) {

        String pesel = t[1];

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(t.points), 0) " +
                            "FROM tickets t " +
                            "JOIN drivers d ON t.driver_id = d.id " +
                            "WHERE d.pesel = ?"
            );

            ps.setString(1, pesel);

            ResultSet rs = ps.executeQuery();

            int points = 0;

            if (rs.next()) {
                points = rs.getInt(1);
            }

            out.println("SUCCESS:Punkty=" + points);

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }

    public void getTickets(String[] t, PrintWriter out) {

        String pesel = t[1];

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT t.* " +
                            "FROM tickets t " +
                            "JOIN drivers d ON t.driver_id = d.id " +
                            "WHERE d.pesel = ?"
            );

            ps.setString(1, pesel);

            ResultSet rs = ps.executeQuery();

            List<String> list = new ArrayList<>();

            while (rs.next()) {

                String id = rs.getString("id");
                int points = rs.getInt("points");
                String reason = rs.getString("reason");
                boolean paid = rs.getBoolean("paid");

                list.add(
                        id + "," +
                                points + "," +
                                reason + "," +
                                paid
                );
            }

            if (list.isEmpty()) {
                out.println("SUCCESS:EMPTY");
            } else {
                out.println("SUCCESS:" + String.join(";;", list));
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }

    public void payTicket(String[] t, PrintWriter out) {

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE tickets SET paid = true WHERE id = ?"
            );

            ps.setString(1, t[1]);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                out.println("SUCCESS:Opłacono mandat");
            } else {
                out.println("ERROR:Nie znaleziono");
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }

    public void incidentStatus(String[] t, PrintWriter out) {

        try (Connection conn = DataBase.connect()) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM incidents WHERE user = ?"
            );

            ps.setString(1, t[1]);

            ResultSet rs = ps.executeQuery();

            List<String> list = new ArrayList<>();

            while (rs.next()) {
                list.add(
                        rs.getString("date") + "," +
                                rs.getString("status") + "," +
                                rs.getString("description")
                );
            }

            if (list.isEmpty()) {
                out.println("EMPTY:Brak zgłoszeń");
            } else {
                out.println(String.join(";;", list));
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR:Baza");
        }
    }
}