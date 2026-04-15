package org.example;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    public static void main(String[] args) {
        new AuthServer().start();
    }
}

class AuthServer {
    private static final int PORT = 5556;

    private final UserManager userManager = new UserManager();
    private final DriverManager driverManager = new DriverManager();
    private final TicketManager ticketManager = new TicketManager(driverManager);
    private final MessageManager messageManager = new MessageManager();
    private final ReportManager reportManager = new ReportManager();
    private final PlateManager plateManager = new PlateManager();
    private final StatisticsManager statisticsManager = new StatisticsManager();
    private final PatrolManager patrolManager = new PatrolManager();
    private final CitizenManager citizenManager = new CitizenManager();


    public void start() {
        userManager.load();
        driverManager.load();

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
        } catch (IOException e) { }
    }

    public void login(String[] t, PrintWriter out) {
        if (pass.containsKey(t[1]) && pass.get(t[1]).equals(t[2])) {
            logged.put(t[1], true);
            out.println("SUCCESS:" + role.get(t[1]));
        } else {
            out.println("ERROR:Login");
        }
    }

    public void logout(String[] t, PrintWriter out) {
        logged.put(t[1], false);
        out.println("SUCCESS:Logout");
    }

    public void addUser(String[] t, PrintWriter out) {
        if (!logged(t[1]) || !isChief(t[1])) {
            out.println("ERROR:Tylko komendant");
            return;
        }

        pass.put(t[2], t[3]);
        role.put(t[2], t[4]);
        logged.put(t[2], false);
        save();

        out.println("SUCCESS:Dodano");
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
        } catch (IOException e) {}
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
}

class DriverManager {

    private static final String FILE = "drivers.txt";
    private static final String TICKETS_FILE = "tickets.txt";

    static class Driver {
        String id;
        String name;
        String surname;
        boolean warrant;

        Driver(String id, String name, String surname, boolean warrant) {
            this.id = id;
            this.name = name;
            this.surname = surname;
            this.warrant = warrant;
        }
    }

    private final Map<String, Driver> drivers = new HashMap<>();

    public void load() {
        try {
            Path p = Paths.get(FILE);

            if (!Files.exists(p)) {
                Files.write(p, List.of(
                        "123456789,Jan,Kowalski,no",
                        "987654321,Aleksandra,Nowak,yes"
                ));
            }

            drivers.clear();

            for (String l : Files.readAllLines(p)) {
                String[] s = l.split(",");

                if (s.length < 4) continue;

                Driver d = new Driver(
                        s[0],
                        s[1],
                        s[2],
                        s[3].equalsIgnoreCase("yes")
                );

                drivers.put(d.id, d);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int calculatePoints(String driverId) {
        int sum = 0;

        try {
            Path p = Paths.get(TICKETS_FILE);

            if (!Files.exists(p)) return 0;

            for (String line : Files.readAllLines(p)) {
                Ticket t = Ticket.fromLine(line);

                if (t.getDriverId().equals(driverId)) {
                    sum += t.getPoints();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return sum;
    }

    public void checkDriver(String[] t, PrintWriter out, UserManager u) {
        if (!u.logged(t[1])) {
            out.println("ERROR:Nie zalogowany");
            return;
        }

        Driver d = drivers.get(t[2]);

        if (d == null) {
            out.println("ERROR:Brak kierowcy");
            return;
        }

        int points = calculatePoints(d.id);

        String status = d.warrant ?
                "UWAGA: Poszukiwany!" :
                "OK";

        out.println("SUCCESS:" +
                d.name + " " + d.surname +
                " | Punkty: " + points +
                " | " + status);
    }

    public Driver getDriver(String id) {
        return drivers.get(id);
    }
}

class TicketManager {

    private static final String FILE = "tickets.txt";
    private final DriverManager driverManager;

    public TicketManager(DriverManager driverManager) {
        this.driverManager = driverManager;
    }

    public void issue(String[] t, PrintWriter out, UserManager u) {

        if (t.length < 7) {
            out.println("ERROR: format user ticketId driverId points fine reason");
            return;
        }

        String user = t[1];
        String ticketId = t[2];
        String driverId = t[3];

        int points;
        try {
            points = Integer.parseInt(t[4]);
        } catch (Exception e) {
            out.println("ERROR: punkty muszą być liczbą");
            return;
        }

        String fine = t[5];
        String reason = t[6];

        if (!u.logged(user) || !u.isPolice(user)) {
            out.println("ERROR:Brak dostępu");
            return;
        }

        try {
            if (Files.exists(Paths.get(FILE))) {
                for (String line : Files.readAllLines(Paths.get(FILE))) {
                    if (line.startsWith(ticketId + ",")) {
                        out.println("ERROR: ID już istnieje");
                        return;
                    }
                }
            }
        } catch (IOException e) {
            out.println("ERROR:Plik");
            return;
        }

        Ticket ticket = new Ticket(
                ticketId,
                driverId,
                user,
                points,
                reason,
                false
        );

        save(ticket);

        out.println("SUCCESS:Mandat wystawiony ID=" + ticketId);
    }

    private void save(Ticket ticket) {
        try {
            Files.write(
                    Paths.get(FILE),
                    (ticket.toFileLine() + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pay(String[] t, PrintWriter out) {

        String ticketId = t[1];

        try {
            if (!Files.exists(Paths.get(FILE))) {
                out.println("ERROR:Brak mandatów");
                return;
            }

            List<Ticket> updated = new ArrayList<>();
            boolean found = false;

            for (String line : Files.readAllLines(Paths.get(FILE))) {

                Ticket ticket = Ticket.fromLine(line);

                if (ticket.getId().equals(ticketId)) {
                    ticket.setPaid(true);
                    found = true;
                }

                updated.add(ticket);
            }

            if (!found) {
                out.println("ERROR:Nie znaleziono mandatu");
                return;
            }

            List<String> lines = new ArrayList<>();
            for (Ticket tck : updated) {
                lines.add(tck.toFileLine());
            }

            Files.write(Paths.get(FILE), lines);

            out.println("Opłacono mandat");

        } catch (Exception e) {
            out.println("ERROR");
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

    // przypisanie patrolu
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

    private static final String FILE = "tickets.txt";

    public void getPoints(String[] t, PrintWriter out, DriverManager dm) {

        String id = t[1];

        int points = dm.calculatePoints(id);

        out.println("SUCCESS:Punkty=" + points);
    }

    public void getTickets(String[] t, PrintWriter out) {

        String driverId = t[1];

        try {
            Path p = Paths.get(FILE);

            if (!Files.exists(p)) {
                out.println("EMPTY:Brak mandatów w systemie");
                return;
            }

            List<String> result = new ArrayList<>();

            for (String line : Files.readAllLines(p)) {
                Ticket ticket = Ticket.fromLine(line);

                if (ticket.getDriverId().equals(driverId)) {
                    result.add(line);
                }
            }

            if (result.isEmpty()) {
                out.println("EMPTY:Nie masz żadnych mandatów");
            } else {
                out.println("SUCCESS:" + String.join(";;", result));
            }

        } catch (Exception e) {
            out.println("ERROR:Błąd pobierania mandatów");
        }
    }

    public void payTicket(String[] t, PrintWriter out) {

        String ticketId = t[1];

        try {
            Path p = Paths.get(FILE);

            if (!Files.exists(p)) {
                out.println("EMPTY:Brak mandatów");
                return;
            }

            List<Ticket> updated = new ArrayList<>();
            boolean found = false;

            for (String line : Files.readAllLines(p)) {

                Ticket ticket = Ticket.fromLine(line);

                if (ticket.getId().equals(ticketId)) {
                    ticket.setPaid(true);
                    found = true;
                }

                updated.add(ticket);
            }

            if (!found) {
                out.println("ERROR:Nie znaleziono mandatu");
                return;
            }

            List<String> lines = new ArrayList<>();
            for (Ticket tck : updated) {
                lines.add(tck.toFileLine());
            }

            Files.write(p, lines);

            out.println("Mandat opłacony");

        } catch (Exception e) {
            out.println("ERROR:Błąd płatności");
        }
    }

    public void incidentStatus(String[] t, PrintWriter out) {

        String user = t[1];

        try {
            Path p = Paths.get("incidents.txt");

            if (!Files.exists(p)) {
                out.println("EMPTY:Brak zgłoszeń");
                return;
            }

            List<String> result = new ArrayList<>();

            for (String line : Files.readAllLines(p)) {
                if (line.contains(user)) {
                    result.add(line);
                }
            }

            if (result.isEmpty()) {
                out.println("EMPTY:Brak Twoich zgłoszeń");
            } else {
                out.println(String.join(";;", result));
            }

        } catch (Exception e) {
            out.println("ERROR:Problem z incydentami");
        }
    }
}


