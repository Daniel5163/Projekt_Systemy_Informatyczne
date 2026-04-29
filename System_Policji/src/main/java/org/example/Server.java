package org.example;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import java.sql.*;

public class Server {
    public static void main(String[] args) {

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
        userManager.load();

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

    private void handle(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String req = in.readLine();
            if (req == null) return;

            String[] t = req.split(" ");

            switch (t[0]) {

                case "LOGIN" -> userManager.login(t, out);
                case "LOGOUT" -> userManager.logout(t, out);

                case "ADD_USER" -> userManager.addUser(t, out);
                case "REMOVE_USER" -> userManager.removeUser(t, out);

                case "CHECK_DRIVER" -> driverManager.checkDriver(t, out, userManager);
                case "CHECK_PLATE" -> plateManager.checkPlate(t, out, userManager);

                case "ISSUE_TICKET" -> ticketManager.issue(t, out, userManager);
                case "SAVE_REPORT" -> reportManager.save(t, out, userManager);

                case "SEND_MSG" -> messageManager.send(t, out, userManager);
                case "GET_MSGS" -> messageManager.get(t, out);
                case "GET_USERS" -> messageManager.users(t, out, userManager);

                case "GET_STATS" -> statisticsManager.get(t, out, userManager);

                case "ADD_INCIDENT" -> patrolManager.add(t, out);
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

    public void load() {
        try {
            Path p = Paths.get("accounts.txt");

            if (!Files.exists(p)) {
                Files.write(p, List.of(
                        "admin,admin,chief",
                        "policja,123,man",
                        "jan,123,citizen"
                ));
            }

            for (String l : Files.readAllLines(p)) {
                String[] s = l.split(",");

                pass.put(s[0], s[1]);
                role.put(s[0], s[2]);
                logged.put(s[0], false);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void login(String[] t, PrintWriter out) {

        try (Connection conn = DataBase.connect()) {

            String sql = "SELECT password, role FROM users WHERE username = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, t[1]);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                String dbPass = rs.getString("password");
                String dbRole = rs.getString("role");

                if (dbPass.equals(t[2])) {
                    logged.put(t[1], true);
                    role.put(t[1], dbRole);
                    out.println("SUCCESS:" + dbRole);
                } else {
                    out.println("ERROR:Login");
                }

            } else {
                out.println("ERROR:Brak użytkownika");
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
        String password = t[3];
        String roleNew = t[4];

        String extra = t.length > 5 ? t[5] : null;

        if ("citizen".equals(roleNew)) {

            try (Connection conn = DataBase.connect()) {

                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO users(username, password, role, police_id, pesel) VALUES (?, ?, ?, ?, ?)"
                );

                ps.setString(1, newUser);
                ps.setString(2, password);
                ps.setString(3, roleNew);
                ps.setString(4, null);
                ps.setString(5, extra);

                ps.executeUpdate();

                pass.put(newUser, password);
                role.put(newUser, roleNew);
                logged.put(newUser, false);

                out.println("SUCCESS:Konto utworzone");

            } catch (Exception e) {
                e.printStackTrace();
                out.println("ERROR:Baza");
            }

        } else {
            pendingUsers.add(new String[]{newUser, password, roleNew, extra});
            out.println("SUCCESS:Do zatwierdzenia");
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

    public void removeUser(String[] t, PrintWriter out) {

        if (!logged(t[1]) || !isChief(t[1])) {
            out.println("ERROR:Tylko komendant");
            return;
        }

        pass.remove(t[2]);
        role.remove(t[2]);
        logged.remove(t[2]);

        save();

        out.println("SUCCESS:Usunięto");
    }

    private void save() {
        try {
            List<String> l = new ArrayList<>();

            for (String u : pass.keySet()) {
                l.add(u + "," + pass.get(u) + "," + role.get(u));
            }

            Files.write(Paths.get("accounts.txt"), l);

        } catch (IOException e) {
            e.printStackTrace();
        }
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

}

class DriverManager {

    public void checkDriver(String[] t, PrintWriter out, UserManager u) {

        if (!u.logged(t[1])) {
            out.println("ERROR:Nie zalogowany");
            return;
        }

        String user = t[1];
        String peselInput = t[2];

        try (Connection conn = DataBase.connect()) {

            PreparedStatement psUser = conn.prepareStatement(
                    "SELECT pesel FROM users WHERE username = ?"
            );

            psUser.setString(1, user);

            ResultSet rsUser = psUser.executeQuery();

            if (!rsUser.next()) {
                out.println("ERROR:Brak użytkownika");
                return;
            }

            String peselDb = rsUser.getString("pesel");

            if (peselDb == null || !peselDb.equals(peselInput)) {
                out.println("ERROR:Niepoprawny PESEL");
                return;
            }

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

            out.println("SUCCESS:" +
                    name + " " + surname +
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

                String ticketId = t[2];
                String pesel = t[3];
                int points = Integer.parseInt(t[4]);
                String reason = t[6];

                int driverId = getOrCreateDriver(conn, pesel);

                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO tickets (id, driver_id, police_user, points, reason, paid) " +
                                "VALUES (?, ?, ?, ?, ?, false)"
                );

                ps.setString(1, ticketId);

                if (driverId == -1) {
                    ps.setNull(2, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(2, driverId);
                }

                ps.setString(3, user);
                ps.setInt(4, points);
                ps.setString(5, reason);

                ps.executeUpdate();

                out.println("SUCCESS:OK");

            } catch (Exception e) {
                e.printStackTrace();
                out.println("ERROR:" + e.getMessage());
            }
        }

        private int getOrCreateDriver(Connection conn, String pesel) throws Exception {

            PreparedStatement psUser = conn.prepareStatement(
                    "SELECT id FROM users WHERE pesel = ?"
            );

            psUser.setString(1, pesel);
            ResultSet rsUser = psUser.executeQuery();

            if (!rsUser.next()) {
                return -1;
            }

            int userId = rsUser.getInt(1);

            PreparedStatement psDriver = conn.prepareStatement(
                    "SELECT id FROM drivers WHERE user_id = ?"
            );

            psDriver.setInt(1, userId);
            ResultSet rsDriver = psDriver.executeQuery();

            if (rsDriver.next()) {
                return rsDriver.getInt(1);
            }

            PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO drivers(user_id) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS
            );

            ins.setInt(1, userId);
            ins.executeUpdate();

            ResultSet keys = ins.getGeneratedKeys();
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
    private final Map<String, List<String>> msgs = new HashMap<>();

    public void send(String[] t, PrintWriter out, UserManager u) {
        msgs.computeIfAbsent(t[2], k -> new ArrayList<>())
                .add(t[1] + ":" + t[3]);
        out.println("OK");
    }

    public void get(String[] t, PrintWriter out) {
        out.println("SUCCESS:" + String.join(",", msgs.getOrDefault(t[1], List.of())));
    }

    public void users(String[] t, PrintWriter out, UserManager u) {
        out.println("SUCCESS:" + String.join(",", u.users()));
    }
}

class StatisticsManager {

    public void get(String[] t, PrintWriter out, UserManager u) {

        String user = t[1];

        if (!u.logged(user) || !u.isChief(user)) {
            out.println("ERROR:Tylko komendant");
            return;
        }

        try {
            Path p = Paths.get("tickets.txt");

            if (!Files.exists(p)) {
                out.println("SUCCESS:Brak danych");
                return;
            }

            Map<String, Integer> stats = new HashMap<>();

            for (String l : Files.readAllLines(p)) {
                String[] s = l.split(",", 6);

                if (s.length == 6) {
                    String reason = s[5];
                    stats.put(reason, stats.getOrDefault(reason, 0) + 1);
                }
            }

            String result = stats.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .reduce((a, b) -> a + "," + b)
                    .orElse("Brak danych");

            out.println("SUCCESS:" + result);

        } catch (IOException e) {
            out.println("ERROR:Statystyki");
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

        try {
            Path p = Paths.get(FILE);

            if (!Files.exists(p)) {
                out.println("SUCCESS:Brak zgłoszeń");
                return;
            }

            List<String> lines = Files.readAllLines(p);

            for (int i = 0; i < lines.size(); i++) {
                lines.set(i, i + ":" + lines.get(i));
            }

            out.println(String.join(";;", lines));

        } catch (IOException e) {
            out.println("ERROR");
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
}

class PlateManager {
    private static final String FILE = "plates.txt";

    public void checkPlate(String[] t, PrintWriter out, UserManager u) {
        if (!u.logged(t[1])) {
            out.println("ERROR:Nie zalogowany");
            return;
        }

        try {
            Path p = Paths.get(FILE);

            if (!Files.exists(p)) {
                Files.write(p, List.of(
                        "ABC123,yes,kradziony",
                        "XYZ999,no,"
                ));
            }

            for (String l : Files.readAllLines(p)) {
                String[] s = l.split(",", 3);

                if (s[0].equalsIgnoreCase(t[2])) {
                    if ("yes".equals(s[1])) {
                        out.println("WARNING:Pojazd kradziony " + s[2]);
                    } else {
                        out.println("SUCCESS:Pojazd OK");
                    }
                    return;
                }
            }

            out.println("SUCCESS:Brak w bazie");

        } catch (IOException e) {
            out.println("ERROR");
        }
    }
}

class ReportManager {
    public void save(String[] t, PrintWriter out, UserManager u) {

        if (!u.logged(t[1])) {
            out.println("ERROR:Nie zalogowany");
            return;
        }

        if (t.length < 4) {
            out.println("ERROR:Za mało danych");
            return;
        }

        String content = String.join(" ",
                Arrays.copyOfRange(t, 3, t.length));

        String report = LocalDateTime.now() +
                " | " + t[1] +
                " | " + content;

        try {
            Files.write(Paths.get("reports.txt"),
                    (report + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);

            out.println("Raport zapisany");

        } catch (IOException e) {
            out.println("ERROR:Zapis raportu");
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
                            "JOIN users u ON d.user_id = u.id " +
                            "WHERE u.pesel = ?"
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
                            "LEFT JOIN drivers d ON t.driver_id = d.id " +
                            "LEFT JOIN users u ON d.user_id = u.id " +
                            "WHERE u.pesel = ?"
            );

            ps.setString(1, pesel);

            ResultSet rs = ps.executeQuery();

            List<String> list = new ArrayList<>();

            while (rs.next()) {

                String driverId = rs.getString("driver_id");
                String points = String.valueOf(rs.getInt("points"));
                String reason = rs.getString("reason");
                boolean paid = rs.getBoolean("paid");

                list.add(
                        rs.getString("id") + "," +
                                (driverId != null ? driverId : "BRAK") + "," +
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