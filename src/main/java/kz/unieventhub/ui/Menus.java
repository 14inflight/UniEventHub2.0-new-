package kz.unieventhub.ui;

import kz.unieventhub.domain.*;
import kz.unieventhub.service.BookingService;
import kz.unieventhub.service.EventService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class Menus {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ConsoleIO io;
    private final EventService eventService;
    private final BookingService bookingService;

    public Menus(ConsoleIO io,
                 EventService eventService,
                 BookingService bookingService) {
        this.io = io;
        this.eventService = eventService;
        this.bookingService = bookingService;
    }

    /* ====================== STUDENT ====================== */

    public void studentMenu(Student student) {

        String query = "";
        String venueFilter = "ALL";
        String dayFilter = "ALL";
        String categoryFilter = "ALL";
        boolean onlyFree = false;

        while (true) {

            io.println("\n==============================");
            io.println(" STUDENT: " + student.getName());
            io.println("==============================");

            io.println("Search: " + (query.isBlank() ? "(none)" : "\"" + query + "\"")
                    + " | Venue: " + venueFilter
                    + " | Date: " + dayFilter
                    + " | Category: " + categoryFilter
                    + " | Free only: " + (onlyFree ? "ON" : "OFF"));

            io.println("1) Browse events");
            io.println("2) Search text");
            io.println("3) Filter by venue");
            io.println("4) Filter by date");
            io.println("5) Filter by category");
            io.println("6) Toggle 'Only free spots'");
            io.println("7) Register");
            io.println("8) My registrations");
            io.println("9) Cancel registration");
            io.println("0) Logout");

            int c = io.readInt("Choose: ", 0, 9);

            try {
                if (c == 0) return;

                if (c == 1) {
                    browseStudentEvents(student, query, venueFilter, dayFilter, categoryFilter, onlyFree);
                } else if (c == 2) {
                    query = io.readLine("Enter search text: ");
                } else if (c == 3) {
                    venueFilter = pickVenueFilter();
                } else if (c == 4) {
                    dayFilter = pickDateFilter();
                } else if (c == 5) {
                    categoryFilter = pickCategoryFilter();
                } else if (c == 6) {
                    onlyFree = !onlyFree;
                } else if (c == 7) {
                    registerFlow(student, query, venueFilter, dayFilter, categoryFilter, onlyFree);
                } else if (c == 8) {
                    listMy(student);
                } else if (c == 9) {
                    cancelFlow(student);
                }

            } catch (Exception ex) {
                io.println("Error: " + ex.getMessage());
            }
        }
    }

    private void browseStudentEvents(Student student,
                                     String query,
                                     String venueFilter,
                                     String dayFilter,
                                     String categoryFilter,
                                     boolean onlyFree) {

        List<Event> events = eventService.searchApproved(
                query, venueFilter, dayFilter, categoryFilter, onlyFree);

        printEventsWithNumbers(events, student);
        io.readLine("Press Enter...");
    }

    private void registerFlow(Student student,
                              String query,
                              String venueFilter,
                              String dayFilter,
                              String categoryFilter,
                              boolean onlyFree) {

        List<Event> events = eventService.searchApproved(
                query, venueFilter, dayFilter, categoryFilter, onlyFree);

        if (events.isEmpty()) {
            io.println("No events found.");
            return;
        }

        printEventsWithNumbers(events, student);

        int idx = io.readInt("Choose event (0 = cancel): ", 0, events.size());
        if (idx == 0) return;

        Event chosen = events.get(idx - 1);
        bookingService.register(student, chosen);
        io.println("Registered!");
    }

    private void cancelFlow(Student student) {

        List<Event> mine = bookingService.listMyEvents(student);
        if (mine.isEmpty()) {
            io.println("No registrations.");
            return;
        }

        for (int i = 0; i < mine.size(); i++) {
            io.println((i + 1) + ") " + mine.get(i));
        }

        int idx = io.readInt("Choose to cancel (0 = back): ", 0, mine.size());
        if (idx == 0) return;

        bookingService.cancel(student, mine.get(idx - 1));
        io.println("Canceled.");
    }

    private void listMy(Student student) {

        List<Event> mine = bookingService.listMyEvents(student);

        if (mine.isEmpty()) {
            io.println("(empty)");
        } else {
            for (Event e : mine) {
                io.println("• " + e);
            }
        }

        io.readLine("Press Enter...");
    }

    /* ====================== ORGANIZER ====================== */

    public void organizerMenu(Organizer organizer,
                              Map<UUID, Venue> venues) {

        while (true) {

            io.println("\n==============================");
            io.println(" ORGANIZER: " + organizer.getName());
            io.println("==============================");

            io.println("1) View my events");
            io.println("2) Create event");
            io.println("3) Edit my event");
            io.println("0) Logout");

            int c = io.readInt("Choose: ", 0, 3);

            try {
                if (c == 0) return;

                if (c == 1) listOrganizerEvents(organizer);
                else if (c == 2) createEventGuided(organizer, venues);
                else if (c == 3) editOrganizerEvent(organizer);

            } catch (Exception ex) {
                io.println("Error: " + ex.getMessage());
            }
        }
    }

    private void listOrganizerEvents(Organizer organizer) {

        List<Event> mine = eventService.listAllEvents()
                .stream()
                .filter(e -> e.getOrganizerId().equals(organizer.getId()))
                .toList();

        if (mine.isEmpty()) {
            io.println("(empty)");
        } else {
            for (Event e : mine) {
                io.println("• " + e);
            }
        }

        io.readLine("Press Enter...");
    }

    private void editOrganizerEvent(Organizer organizer) {

        List<Event> mine = eventService.listAllEvents()
                .stream()
                .filter(e -> e.getOrganizerId().equals(organizer.getId()))
                .toList();

        if (mine.isEmpty()) {
            io.println("No events.");
            return;
        }

        for (int i = 0; i < mine.size(); i++) {
            io.println((i + 1) + ") " + mine.get(i));
        }

        int idx = io.readInt("Choose event (0 = back): ", 0, mine.size());
        if (idx == 0) return;

        editFlow(organizer, mine.get(idx - 1));
    }

    private void createEventGuided(Organizer organizer,
                                   Map<UUID, Venue> venues) {

        List<Venue> list = new ArrayList<>(venues.values());

        for (int i = 0; i < list.size(); i++) {
            Venue v = list.get(i);
            io.println((i + 1) + ") " + v.getName()
                    + " (cap=" + v.getCapacity() + ")");
        }

        int vIdx = io.readInt("Venue: ", 1, list.size());
        Venue venue = list.get(vIdx - 1);

        String title = io.readLine("Title: ");
        String desc = io.readLine("Description: ");
        LocalDateTime dt = readDateTime();
        int cap = io.readInt("Capacity (1.." + venue.getCapacity() + "): ",
                1, venue.getCapacity());

        Category cat = pickCategoryEnum();

        Event e = eventService.createEvent(
                organizer, title, desc, dt, venue, cap, cat);

        io.println("Created: " + e);
    }

    /* ====================== ADMIN ====================== */

    public void adminMenu(Admin admin) {

        while (true) {

            io.println("\n==============================");
            io.println(" ADMIN: " + admin.getName());
            io.println("==============================");

            io.println("1) View all events");
            io.println("2) Approve/Reject");
            io.println("3) Edit event");
            io.println("0) Logout");

            int c = io.readInt("Choose: ", 0, 3);

            try {
                if (c == 0) return;

                if (c == 1) printAllEvents();
                else if (c == 2) approveFlow();
                else if (c == 3) editAnyEvent(admin);

            } catch (Exception ex) {
                io.println("Error: " + ex.getMessage());
            }
        }
    }

    private void editAnyEvent(Admin admin) {

        List<Event> events = eventService.listAllEvents();
        if (events.isEmpty()) return;

        for (int i = 0; i < events.size(); i++) {
            io.println((i + 1) + ") " + events.get(i));
        }

        int idx = io.readInt("Choose event (0 = back): ", 0, events.size());
        if (idx == 0) return;

        editFlow(admin, events.get(idx - 1));
    }

    /* ====================== EDIT FLOW ====================== */

    private void editFlow(User editor, Event e) {

        String newTitle = io.readLine("New title (Enter = skip): ");
        if (newTitle.isBlank()) newTitle = null;

        eventService.editEvent(editor, e, newTitle,
                null, null, null, null, null);

        io.println("Updated: " + e);
    }

    /* ====================== HELPERS ====================== */

    private void printAllEvents() {
        for (Event e : eventService.listAllEvents()) {
            io.println("• " + e);
        }
    }

    private void approveFlow() {
        List<Event> events = eventService.listAllEvents();
        if (events.isEmpty()) return;

        for (int i = 0; i < events.size(); i++) {
            io.println((i + 1) + ") " + events.get(i));
        }

        int idx = io.readInt("Choose: ", 1, events.size());
        Event e = events.get(idx - 1);

        int v = io.readInt("1) Approve 2) Reject: ", 1, 2);
        eventService.approveEvent(e, v == 1);
    }

    private void printEventsWithNumbers(List<Event> events,
                                        Student student) {

        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);
            boolean reg = bookingService
                    .isRegistered(e.getId(), student.getId());

            io.println((i + 1) + ") " + e
                    + (reg ? " (registered)" : ""));
        }
    }

    private LocalDateTime readDateTime() {
        while (true) {
            try {
                return LocalDateTime.parse(
                        io.readLine("yyyy-MM-dd HH:mm: "), FMT);
            } catch (Exception ex) {
                io.println("Invalid format.");
            }
        }
    }

    private String pickVenueFilter() {
        List<String> venues = eventService.listVenueNames();
        io.println("0) ALL");
        for (int i = 0; i < venues.size(); i++) {
            io.println((i + 1) + ") " + venues.get(i));
        }

        int idx = io.readInt("Choose: ", 0, venues.size());
        return idx == 0 ? "ALL" : venues.get(idx - 1);
    }

    private String pickDateFilter() {
        io.println("1) TODAY");
        io.println("2) WEEK");
        io.println("3) ALL");

        int idx = io.readInt("Choose: ", 1, 3);

        if (idx == 1) return "TODAY";
        if (idx == 2) return "WEEK";
        return "ALL";
    }

    private String pickCategoryFilter() {
        List<String> cats = eventService.listCategoryNames();
        io.println("0) ALL");

        for (int i = 0; i < cats.size(); i++) {
            io.println((i + 1) + ") " + cats.get(i));
        }

        int idx = io.readInt("Choose: ", 0, cats.size());
        return idx == 0 ? "ALL" : cats.get(idx - 1);
    }

    private Category pickCategoryEnum() {
        Category[] cats = Category.values();

        for (int i = 0; i < cats.length; i++) {
            io.println((i + 1) + ") " + cats[i]);
        }

        int idx = io.readInt("Choose category: ", 1, cats.length);
        return cats[idx - 1];
    }
}
