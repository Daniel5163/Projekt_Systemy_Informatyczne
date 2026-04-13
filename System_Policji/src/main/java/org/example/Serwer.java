package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;

public class Serwer {
    public static void main(String[] args) {
        AuthServer server = new AuthServer();
        server.start();
    }
}
// Singleton, klasy są tu tworzone raz
class AuthServer {
    private static final int PORT = 5556;
    private final UserManager userManager;
    private final DriverManager driverManager;
    private final MessageManager messageManager;
    private final LicensePlateManager licensePlateManager;
    private final ReportManager reportManager;
    private final TicketManager ticketManager;
    private final ProblemManager problemManager;

    public AuthServer() {
        this.userManager = new UserManager();
        this.driverManager = new DriverManager();
        this.messageManager = new MessageManager();
        this.licensePlateManager = new LicensePlateManager();
        this.reportManager = new ReportManager();
        this.ticketManager = new TicketManager(driverManager);
        this.problemManager = new ProblemManager();
    }
    //fasadka
    public void start() {
        userManager.loadAccounts();
        messageManager.loadMessages();
        driverManager.loadDrivers();
        licensePlateManager.loadLicensePlates();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println(" Serwer uruchomiony na porcie: " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            System.out.println(" Błąd uruchamiania serwera");
            e.printStackTrace();
        }
    }

    private void handleClient(Socket socket) {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) {

            String request = input.readLine();
            System.out.println(" Otrzymano komendę: " + request);

            if (request == null || request.isEmpty()) {
                output.println("ERROR:Pusta komenda");
                return;
            }

            String[] tokens = request.split("\\s+");
            if (tokens.length < 1) {
                output.println("ERROR:Niepoprawny format komendy");
                return;
            }
            //Wzorzec command
            switch (tokens[0].toUpperCase()) {
                case "GET_USERS" -> messageManager.getUsers(tokens, output, userManager);
                case "SEND_MSG" -> messageManager.sendMessage(tokens, output, userManager);
                case "GET_MSGS" -> messageManager.getMessages(tokens, output);
                case "SAVE_REPORT" -> reportManager.saveReport(tokens, output, userManager);
                case "LOGIN" -> userManager.login(tokens, output);
                case "LOGOUT" -> userManager.logout(tokens, output);
                case "ADD_USER" -> userManager.addUser(tokens, output);
                case "REMOVE_USER" -> userManager.removeUser(tokens, output);
                case "ISSUE_TICKET" -> ticketManager.issueTicket(tokens, output, userManager);
                case "CHECK_DRIVER" -> driverManager.checkDriver(tokens, output, userManager);
                case "CHECK_PLATE" -> licensePlateManager.checkLicensePlate(tokens, output, userManager);
                case "CHECK_USER" -> userManager.checkUser(tokens, output);
                case "REPORT_PROBLEM" -> problemManager.reportProblem(tokens, output, userManager);
                default -> output.println("ERROR:Nieznane polecenie");
            }
        } catch (Exception e) {
            System.out.println(" Błąd podczas obsługi klienta.");
            e.printStackTrace();
        }
    }
}

class UserManager {
    private static final String ACCOUNTS_FILE = "accounts.txt";
    private final Map<String, String> passwords = new ConcurrentHashMap<>();
    private final Map<String, String> roles = new ConcurrentHashMap<>();
    private final Map<String, Boolean> loggedInUsers = new ConcurrentHashMap<>();

    public void loadAccounts() {
        Path accountsPath = Paths.get(System.getProperty("user.dir"), ACCOUNTS_FILE);
        System.out.println(" Używana lokalizacja kont: " + accountsPath);

        if (Files.notExists(accountsPath)) {
            System.out.println(" Plik " + accountsPath + " nie istnieje! Tworzenie domyślnych kont.");
            createDefaultAccountsFile(accountsPath);
        }

        try {
            passwords.clear();
            roles.clear();
            loggedInUsers.clear();

            List<String> lines = Files.readAllLines(accountsPath);
            for (String line : lines) {
                String[] parts = line.trim().split(",");
                if (parts.length == 3) {
                    passwords.put(parts[0], parts[1]);
                    roles.put(parts[0], parts[2]);
                    loggedInUsers.put(parts[0], false);
                }
            }
            System.out.println("✔️ Konta załadowane: " + passwords.keySet());
        } catch (IOException e) {
            System.out.println(" Błąd wczytywania kont!");
            e.printStackTrace();
        }
    }

    private void createDefaultAccountsFile(Path accountPath) {
        List<String> defaultAccounts = Arrays.asList(
                "police_man,password123,man",
                "police_chief,chiefpassword,chief"
        );

        try {
            Files.write(accountPath, defaultAccounts);
            System.out.println(" Utworzono domyślny plik z kontami: " + accountPath);
        } catch (IOException e) {
            System.out.println(" Błąd tworzenia domyślnych kont");
            e.printStackTrace();
        }
    }

    public void login(String[] tokens, PrintWriter output) {
        if (tokens.length != 3) {
            output.println("ERROR:LOGIN username password");
            return;
        }

        String username = tokens[1];
        String password = tokens[2];

        if (passwords.containsKey(username) && passwords.get(username).equals(password)) {
            loggedInUsers.put(username, true);
            output.println("SUCCESS:" + roles.get(username));
            System.out.println(" Zalogowano: " + username);
        } else {
            output.println("ERROR:Niepoprawne dane logowania");
            System.out.println(" Nieudana próba logowania: " + username);
        }
    }

    public void logout(String[] tokens, PrintWriter output) {
        String username = tokens[1];

        if (loggedInUsers.getOrDefault(username, false)) {
            loggedInUsers.put(username, false);
            output.println("SUCCESS:Wylogowano użytkownika");
            System.out.println(" Wylogowano: " + username);
        } else {
            output.println("ERROR:Użytkownik nie był zalogowany");
            System.out.println("️ Nieudana próba wylogowania: " + username);
        }
    }

    public void addUser(String[] tokens, PrintWriter output) {
        if (tokens.length != 5) {
            output.println("ERROR:ADD_USER chief_username new_username new_password new_role");
            return;
        }

        String chiefUsername = tokens[1];
        if (!"chief".equals(roles.get(chiefUsername))) {
            output.println("ERROR:Brak uprawnień (tylko chief może to zrobić)");
            return;
        }

        if (!loggedInUsers.getOrDefault(chiefUsername, false)) {
            output.println("ERROR:Chief nie jest zalogowany");
            return;
        }

        String newUser = tokens[2];
        if (passwords.containsKey(newUser)) {
            output.println("ERROR:Taki użytkownik istnieje");
            return;
        }

        passwords.put(newUser, tokens[3]);
        roles.put(newUser, tokens[4]);
        loggedInUsers.put(newUser, false);
        saveAccounts();
        output.println("SUCCESS:Użytkownik " + newUser + " został dodany");
        System.out.println(" Dodano użytkownika: " + newUser);
    }

    public void removeUser(String[] tokens, PrintWriter output) {
        if (tokens.length != 3) {
            output.println("ERROR:REMOVE_USER chief_username username");
            return;
        }

        String chiefUsername = tokens[1];
        if (!"chief".equals(roles.get(chiefUsername))) {
            output.println("ERROR:Brak uprawnień (tylko chief może to zrobić)");
            return;
        }

        if (!loggedInUsers.getOrDefault(chiefUsername, false)) {
            output.println("ERROR:Chief nie jest zalogowany");
            return;
        }

        String userToRemove = tokens[2];
        if (!passwords.containsKey(userToRemove)) {
            output.println("ERROR:Użytkownik nie istnieje");
            return;
        }

        passwords.remove(userToRemove);
        roles.remove(userToRemove);
        loggedInUsers.remove(userToRemove);
        saveAccounts();
        output.println("SUCCESS:Użytkownik " + userToRemove + " został usunięty");
        System.out.println(" Usunięto użytkownika: " + userToRemove);
    }

    public void checkUser(String[] tokens, PrintWriter output) {
        if (tokens.length != 3) {
            output.println("ERROR:CHECK_USER chief_username username");
            return;
        }

        String chiefUsername = tokens[1];
        if (!"chief".equals(roles.get(chiefUsername))) {
            output.println("ERROR:Brak uprawnień (tylko chief może to zrobić)");
            return;
        }

        if (!loggedInUsers.getOrDefault(chiefUsername, false)) {
            output.println("ERROR:Chief nie jest zalogowany");
            return;
        }

        String username = tokens[2];
        if (!passwords.containsKey(username)) {
            output.println("ERROR:Użytkownik nie istnieje");
            return;
        }

        String role = roles.get(username);
        String status = loggedInUsers.getOrDefault(username, false) ? "zalogowany" : "wylogowany";

        output.println("SUCCESS:Użytkownik: " + username +
                ", Rola: " + role +
                ", Status: " + status);
    }

    private void saveAccounts() {
        Path accountPath = Paths.get(System.getProperty("user.dir"), ACCOUNTS_FILE);
        List<String> accountLines = passwords.keySet().stream()
                .map(u -> u + "," + passwords.get(u) + "," + roles.get(u))
                .toList();

        try {
            Files.write(accountPath, accountLines);
            System.out.println(" Poprawnie zapisano konta.");
        } catch (IOException e) {
            System.out.println(" Błąd przy zapisie kont!");
            e.printStackTrace();
        }
    }

    public boolean isUserLoggedIn(String username) {
        return loggedInUsers.getOrDefault(username, false);
    }

    public String getUserRole(String username) {
        return roles.get(username);
    }

    public Set<String> getAllUsernames() {
        return passwords.keySet();
    }

    public boolean userExists(String username) {
        return passwords.containsKey(username);
    }
}

class DriverManager {
    private static final String DRIVERS_FILE = "drivers.txt";
    private final Map<String, Driver> drivers = new ConcurrentHashMap<>();

    public void loadDrivers() {
        Path path = Paths.get(System.getProperty("user.dir"), DRIVERS_FILE);
        System.out.println(" Używana lokalizacja kierowców: " + path);

        if (Files.notExists(path)) {
            System.out.println(" Plik " + path + " nie istnieje! Tworzenie domyślnych kierowców.");
            createDefaultDrivers(path);
        }

        try {
            drivers.clear();
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length == 5) {
                    drivers.put(parts[0], new Driver(parts[0], parts[1], parts[2],
                            parts[3].equals("yes"), Integer.parseInt(parts[4])));
                }
            }
            System.out.println("Kierowcy załadowani: " + drivers.keySet());
        } catch (IOException e) {
            System.out.println(" Błąd wczytywania kierowców!");
            e.printStackTrace();
        }
    }

    private void createDefaultDrivers(Path path) {
        List<String> defaultDrivers = Arrays.asList(
                "123456789,Jan,Kowalski,no,0",
                "987654321,Aleksandra,Nowak,yes,12"
        );

        try {
            Files.write(path, defaultDrivers);
            System.out.println(" Utworzono domyślny plik z kierowcami: " + path);
        } catch (IOException e) {
            System.out.println(" Błąd tworzenia domyślnych kierowców");
            e.printStackTrace();
        }
    }

    public void checkDriver(String[] tokens, PrintWriter output, UserManager userManager) {
        if (tokens.length != 3) {
            output.println("ERROR:CHECK_DRIVER username driver_id");
            return;
        }

        String username = tokens[1];
        if (!userManager.isUserLoggedIn(username)) {
            output.println("ERROR:Użytkownik nie jest zalogowany");
            return;
        }

        String userRole = userManager.getUserRole(username);
        if (!"man".equals(userRole) && !"chief".equals(userRole)) {
            output.println("ERROR:Tylko policjanci i przełożeni mogą sprawdzać kierowców");
            return;
        }

        String driverId = tokens[2];
        Driver driver = drivers.get(driverId);
        if (driver == null) {
            output.println("ERROR:Nie znaleziono kierowcy o podanym ID");
            return;
        }

        String status = driver.hasWarrant ?
                "OSTRZEŻENIE: Kierowca " + driver.name + " " + driver.surname + " ma nakaz aresztowania!" :
                "Kierowca " + driver.name + " " + driver.surname + " nie ma nakazu aresztowania";

        output.println("SUCCESS:" + status);
        System.out.println(" Sprawdzono kierowcę " + driverId + " dla " + username);
    }

    public void saveDrivers() {
        Path path = Paths.get(System.getProperty("user.dir"), DRIVERS_FILE);
        List<String> lines = drivers.values().stream()
                .map(d -> String.join(",", d.id, d.name, d.surname,
                        d.hasWarrant ? "yes" : "no", String.valueOf(d.penaltyPoints)))
                .toList();

        try {
            Files.write(path, lines);
            System.out.println(" Poprawnie zapisano kierowców.");
        } catch (IOException e) {
            System.out.println(" Błąd przy zapisie kierowców!");
            e.printStackTrace();
        }
    }

    public Driver getDriver(String driverId) {
        return drivers.get(driverId);
    }

    public void updateDriverPoints(String driverId, int points) {
        Driver driver = drivers.get(driverId);
        if (driver != null) {
            driver.penaltyPoints = points;
            saveDrivers();
        }
    }

    static class Driver {
        public final String id;
        public final String name;
        public final String surname;
        public final boolean hasWarrant;
        public int penaltyPoints;

        public Driver(String id, String name, String surname, boolean hasWarrant, int penaltyPoints) {
            this.id = id;
            this.name = name;
            this.surname = surname;
            this.hasWarrant = hasWarrant;
            this.penaltyPoints = penaltyPoints;
        }
    }
}

class MessageManager {
    private static final String MESSAGES_FILE = "messages.txt";
    private final Map<String, List<String>> messages = new ConcurrentHashMap<>();

    public void loadMessages() {
        Path path = Paths.get(System.getProperty("user.dir"), MESSAGES_FILE);
        if (Files.notExists(path)) return;

        try {
            messages.clear();
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                String[] parts = line.split(",", 3);
                if (parts.length == 3) {
                    messages.computeIfAbsent(parts[1], k -> new ArrayList<>())
                            .add(parts[0] + ": " + parts[2].replace("_", " "));
                }
            }
        } catch (IOException e) {
            System.out.println(" Błąd wczytywania wiadomości");
        }
    }

    public void getUsers(String[] tokens, PrintWriter output, UserManager userManager) {
        if (tokens.length != 2) {
            output.println("ERROR:GET_USERS username");
            return;
        }

        String username = tokens[1];
        if (!userManager.isUserLoggedIn(username)) {
            output.println("ERROR:Użytkownik nie jest zalogowany");
            return;
        }

        List<String> users = new ArrayList<>(userManager.getAllUsernames());
        users.remove(username);
        output.println("SUCCESS:" + String.join(",", users));
    }
    //observer cz1 nie do końca jawny
    public void sendMessage(String[] tokens, PrintWriter output, UserManager userManager) {
        if (tokens.length < 4) {
            output.println("ERROR:SEND_MSG sender recipient message");
            return;
        }

        String sender = tokens[1];
        if (!userManager.isUserLoggedIn(sender)) {
            output.println("ERROR:Nadawca nie jest zalogowany");
            return;
        }

        String recipient = tokens[2];
        if (!userManager.userExists(recipient)) {
            output.println("ERROR:Odbiorca nie istnieje");
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(tokens, 3, tokens.length));
        saveMessage(sender, recipient, message);
        output.println("SUCCESS:Wiadomość wysłana do " + recipient);
    }

    public void getMessages(String[] tokens, PrintWriter output) {
        if (tokens.length != 2) {
            output.println("ERROR:GET_MSGS username");
            return;
        }

        String username = tokens[1];
        List<String> userMessages = messages.getOrDefault(username, Collections.emptyList());
        output.println("SUCCESS:" + String.join(";;", userMessages));
    }
    //observer cz2
    private void saveMessage(String sender, String recipient, String content) {
        Path path = Paths.get(System.getProperty("user.dir"), MESSAGES_FILE);
        String message = LocalDateTime.now() + "," + recipient + "," + content;

        try {
            Files.write(path, (message + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);


            messages.computeIfAbsent(recipient, k -> new ArrayList<>())
                    .add(sender + ": " + content.replace("_", " "));
        } catch (IOException e) {
            System.out.println(" Błąd zapisu wiadomości");
        }
    }
}

class LicensePlateManager {
    private static final String PLATES_FILE = "plates.txt";

    public void loadLicensePlates() {
        Path path = Paths.get(System.getProperty("user.dir"), PLATES_FILE);
        System.out.println(" Używana lokalizacja numerów rejestracyjnych: " + path);

        if (Files.notExists(path)) {
            System.out.println(" Plik " + path + " nie istnieje! Tworzenie domyślnych numerów.");
            createDefaultPlatesFile(path);
        }
    }

    private void createDefaultPlatesFile(Path path) {
        List<String> defaultPlates = Arrays.asList(
                "ABC1234,yes,Skradziony w Warszawie 2023-05-15",
                "XYZ9876,no,",
                "GDA5432,yes,Skradziony w Gdańsku 2023-06-20"
        );

        try {
            Files.write(path, defaultPlates);
            System.out.println("️ Utworzono domyślny plik z numerami rejestracyjnymi: " + path);
        } catch (IOException e) {
            System.out.println(" Błąd tworzenia domyślnych numerów rejestracyjnych");
            e.printStackTrace();
        }
    }

    public void checkLicensePlate(String[] tokens, PrintWriter output, UserManager userManager) {
        if (tokens.length != 3) {
            output.println("ERROR:CHECK_PLATE username plate_number");
            return;
        }

        String username = tokens[1];
        if (!userManager.isUserLoggedIn(username)) {
            output.println("ERROR:Użytkownik nie jest zalogowany");
            return;
        }

        String plateNumber = tokens[2];
        Path path = Paths.get(System.getProperty("user.dir"), PLATES_FILE);

        try {
            List<String> plates = Files.readAllLines(path);
            for (String plate : plates) {
                String[] parts = plate.split(",", 3);
                if (parts[0].equalsIgnoreCase(plateNumber)) {
                    if ("yes".equals(parts[1])) {
                        output.println("OSTRZEŻENIE: Pojazd " + plateNumber + " jest w bazie skradzionych! " + parts[2]);
                    } else {
                        output.println("SUCCESS:Pojazd " + plateNumber + " nie figuruje w bazie skradzionych");
                    }
                    return;
                }
            }
            output.println("SUCCESS:Pojazd " + plateNumber + " nie figuruje w bazie skradzionych");
        } catch (IOException e) {
            output.println("ERROR:Błąd podczas sprawdzania bazy pojazdów");
            e.printStackTrace();
        }
    }
}

class ReportManager {
    public void saveReport(String[] tokens, PrintWriter output, UserManager userManager) {
        if (tokens.length < 4) {
            output.println("ERROR:SAVE_REPORT username role report_content");
            return;
        }

        String username = tokens[1];
        if (!userManager.isUserLoggedIn(username)) {
            output.println("ERROR:Użytkownik nie jest zalogowany");
            return;
        }

        String role = tokens[2];
        String reportContent = String.join(" ", Arrays.copyOfRange(tokens, 3, tokens.length))
                .replace("_", " ");

        LocalDate today = LocalDate.now();
        String dateString = today.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));


        Path reportsDir = Paths.get(System.getProperty("user.dir"), "reports");
        try {
            Files.createDirectories(reportsDir);
        } catch (IOException e) {
            output.println("ERROR:Nie można utworzyć folderu raportów");
            return;
        }

// troche composite
        int reportNumber = 1;
        Path reportFile;
        do {
            reportFile = reportsDir.resolve(dateString + "-report" + reportNumber + ".txt");
            reportNumber++;
        } while (Files.exists(reportFile));

        // Save report
        try {
            String reportData = "Autor: " + username + "\n" +
                    "Rola: " + role + "\n" +
                    "Data: " + LocalDateTime.now() + "\n\n" +
                    reportContent;

            Files.write(reportFile, reportData.getBytes());
            output.println("SUCCESS:Raport zapisany jako " + reportFile.getFileName());
            System.out.println(" Zapisano raport: " + reportFile);
        } catch (IOException e) {
            output.println("ERROR:Błąd podczas zapisywania raportu");
            e.printStackTrace();
        }
    }
}

class TicketManager {
    private static final String TICKETS_FILE = "tickets.txt";
    private final DriverManager driverManager;

    public TicketManager(DriverManager driverManager) {
        this.driverManager = driverManager;
    }

    public void issueTicket(String[] tokens, PrintWriter output, UserManager userManager) {
        if (tokens.length != 6) {
            output.println("ERROR:ISSUE_TICKET username driver_id points fine_amount reason");
            return;
        }

        String username = tokens[1];
        if (!userManager.isUserLoggedIn(username)) {
            output.println("ERROR:Użytkownik nie jest zalogowany");
            return;
        }

        String userRole = userManager.getUserRole(username);
        if (!"man".equals(userRole) && !"chief".equals(userRole)) {
            output.println("ERROR:Tylko policjanci i przełożeni mogą wystawiać mandaty");
            return;
        }

        String driverId = tokens[2];
        int pointsToAdd;
        double fineAmount;

        try {
            pointsToAdd = Integer.parseInt(tokens[3]);
            fineAmount = Double.parseDouble(tokens[4]);
        } catch (NumberFormatException e) {
            output.println("ERROR:Nieprawidłowa wartość punktów lub kwoty");
            return;
        }

        String reason = tokens[5].replace("_", " ");
        DriverManager.Driver driver = driverManager.getDriver(driverId);
        if (driver == null) {
            output.println("ERROR:Nie znaleziono kierowcy");
            return;
        }

        int newPoints = driver.penaltyPoints + pointsToAdd;
        if (newPoints < 0) newPoints = 0;
        driverManager.updateDriverPoints(driverId, newPoints);
        saveTicket(username, driverId, pointsToAdd, fineAmount, reason);

        output.println("SUCCESS:Mandat wystawiony. Nowe punkty: " + newPoints + ", grzywna: $" + fineAmount);
        System.out.println(" Wystawiono mandat dla " + driverId + " przez " + username);
    }

    private void saveTicket(String officer, String driverId, int points, double fineAmount, String reason) {
        Path path = Paths.get(System.getProperty("user.dir"), TICKETS_FILE);
        String ticketData = String.join(",",
                LocalDateTime.now().toString(),
                officer,
                driverId,
                String.valueOf(points),
                String.valueOf(fineAmount),
                reason
        );

        try {
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            Files.write(path, (ticketData + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
            System.out.println(" Zapisano mandat: " + ticketData);
        } catch (IOException e) {
            System.out.println(" Błąd przy zapisie mandatu!");
            e.printStackTrace();
        }
    }
}

class ProblemManager {
    private static final String PROBLEMS_FILE = "problems.txt";

    public void reportProblem(String[] tokens, PrintWriter output, UserManager userManager) {
        if (tokens.length < 3) {
            output.println("ERROR:REPORT_PROBLEM username problem_description");
            return;
        }

        String username = tokens[1];
        if (!userManager.isUserLoggedIn(username)) {
            output.println("ERROR:Użytkownik nie jest zalogowany");
            return;
        }

        String problem = String.join(" ", Arrays.copyOfRange(tokens, 2, tokens.length))
                .replace("_", " ");

        Path path = Paths.get(System.getProperty("user.dir"), PROBLEMS_FILE);
        String report = LocalDateTime.now() + "," + username + "," + problem;

        try {
            Files.write(path, (report + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            output.println("SUCCESS:Problem zgłoszony. Dziękujemy!");
            System.out.println("️ Zgłoszono problem przez " + username + ": " + problem);
        } catch (IOException e) {
            output.println("ERROR:Błąd podczas zgłaszania problemu");
            e.printStackTrace();
        }
    }
}