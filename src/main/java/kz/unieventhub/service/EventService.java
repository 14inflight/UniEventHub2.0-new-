package kz.unieventhub.service;

import kz.unieventhub.domain.*;
import kz.unieventhub.repo.InMemoryDatabase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public final class EventService {
    private final InMemoryDatabase db;

    public EventService(InMemoryDatabase db) {
        this.db = Objects.requireNonNull(db);
    }

    // ---------- LIST ----------
    public List<Event> listApprovedEvents() {
        return db.events.values().stream()
                .filter(Event::isApproved)
                .sorted(Comparator.comparing(Event::getDateTime))
                .toList();
    }

    public List<Event> listAllEvents() {
        return db.events.values().stream()
                .sorted(Comparator.comparing(Event::getDateTime))
                .toList();
    }

    public List<String> listVenueNames() {
        return db.venues.values().stream()
                .map(Venue::getName)
                .distinct()
                .sorted()
                .toList();
    }

    public List<String> listCategoryNames() {
        return Arrays.stream(Category.values())
                .map(Enum::name)
                .toList();
    }

    // ---------- SEARCH / FILTER (APPROVED ONLY) ----------
    // dayFilter: TODAY / WEEK / ALL
    // venueFilter: "ALL" or exact venue name
    // categoryFilter: "ALL" or enum name
    public List<Event> searchApproved(String query,
                                      String venueFilter,
                                      String dayFilter,
                                      String categoryFilter,
                                      boolean onlyFree) {

        String q = (query == null) ? "" : query.trim().toLowerCase();

        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);

        return db.events.values().stream()
                .filter(Event::isApproved)
                .filter(e -> {
                    if (q.isBlank()) return true;
                    String hay = (e.getTitle() + " " + e.getDescription() + " " + e.getCategory()).toLowerCase();
                    return hay.contains(q);
                })
                .filter(e -> {
                    if (venueFilter == null || venueFilter.equalsIgnoreCase("ALL")) return true;
                    return e.getVenue().getName().equalsIgnoreCase(venueFilter);
                })
                .filter(e -> {
                    if (dayFilter == null || dayFilter.equalsIgnoreCase("ALL")) return true;

                    LocalDate d = e.getDateTime().toLocalDate();
                    if (dayFilter.equalsIgnoreCase("TODAY")) return d.equals(today);
                    if (dayFilter.equalsIgnoreCase("WEEK")) return !d.isBefore(today) && !d.isAfter(weekEnd);

                    return true;
                })
                .filter(e -> {
                    if (categoryFilter == null || categoryFilter.equalsIgnoreCase("ALL")) return true;
                    return e.getCategory().name().equalsIgnoreCase(categoryFilter);
                })
                .filter(e -> !onlyFree || e.getFreeSpots() > 0)
                .sorted(Comparator.comparing(Event::getDateTime))
                .toList();
    }

    // ---------- CREATE ----------
    public Event createEvent(Organizer organizer,
                             String title,
                             String description,
                             LocalDateTime dateTime,
                             Venue venue,
                             int capacity,
                             Category category) {

        Objects.requireNonNull(organizer, "organizer");
        Objects.requireNonNull(venue, "venue");
        Objects.requireNonNull(dateTime, "dateTime");

        Event e = new Event(
                UUID.randomUUID(),
                title,
                description,
                dateTime,
                venue,
                organizer.getId(),
                capacity,
                category
        );

        db.events.put(e.getId(), e);
        db.eventRegistrations.putIfAbsent(e.getId(), new HashSet<>());
        return e;
    }

    // ---------- APPROVAL ----------
    public void approveEvent(Event event, boolean approved) {
        Objects.requireNonNull(event, "event");
        event.setApproved(approved);
    }

    // ---------- EDIT ----------
    public void editEvent(User editor,
                          Event event,
                          String newTitle,
                          String newDescription,
                          LocalDateTime newDateTime,
                          Category newCategory,
                          Integer newCapacity,
                          Boolean newApproved) {

        Objects.requireNonNull(editor, "editor");
        Objects.requireNonNull(event, "event");

        boolean isAdmin = editor.getRole() == Role.ADMIN;
        boolean isOwnerOrganizer = editor.getRole() == Role.ORGANIZER
                && event.getOrganizerId().equals(editor.getId());

        if (!isAdmin && !isOwnerOrganizer) {
            throw new IllegalStateException("You are not allowed to edit this event.");
        }

        if (newTitle != null) event.updateTitle(newTitle);
        if (newDescription != null) event.updateDescription(newDescription);
        if (newDateTime != null) event.updateDateTime(newDateTime);
        if (newCategory != null) event.updateCategory(newCategory);
        if (newCapacity != null) event.updateCapacity(newCapacity);

        if (newApproved != null) {
            if (!isAdmin) throw new IllegalStateException("Only admin can approve/reject events.");
            event.setApproved(newApproved);
        }
    }
}
