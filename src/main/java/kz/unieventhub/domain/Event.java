package kz.unieventhub.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class Event {
    private final UUID id;

    // Editable fields (NOT final)
    private String title;
    private String description;
    private LocalDateTime dateTime;

    private final Venue venue;
    private final UUID organizerId;

    private Category category;

    private boolean approved;
    private int capacity;
    private int registeredCount;

    public Event(UUID id,
                 String title,
                 String description,
                 LocalDateTime dateTime,
                 Venue venue,
                 UUID organizerId,
                 int capacity,
                 Category category) {

        this.id = Objects.requireNonNull(id, "id");
        this.title = requireNonBlank(title, "title");
        this.description = Objects.requireNonNull(description, "description");
        this.dateTime = Objects.requireNonNull(dateTime, "dateTime");
        this.venue = Objects.requireNonNull(venue, "venue");
        this.organizerId = Objects.requireNonNull(organizerId, "organizerId");

        if (capacity <= 0) throw new IllegalArgumentException("Event capacity must be > 0");
        if (capacity > venue.getCapacity())
            throw new IllegalArgumentException("Event capacity cannot exceed venue capacity");

        this.capacity = capacity;
        this.category = (category == null) ? Category.OTHER : category;

        this.approved = true; // MVP: visible by default
        this.registeredCount = 0;
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(field + " cannot be empty");
        return value.trim();
    }

    // ===== Getters =====
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getDateTime() { return dateTime; }
    public Venue getVenue() { return venue; }
    public UUID getOrganizerId() { return organizerId; }
    public Category getCategory() { return category; }

    public boolean isApproved() { return approved; }
    public int getCapacity() { return capacity; }
    public int getRegisteredCount() { return registeredCount; }
    public int getFreeSpots() { return capacity - registeredCount; }

    public void setApproved(boolean approved) { this.approved = approved; }

    // ===== Registration counters =====
    public void incrementRegistered() {
        if (registeredCount >= capacity) throw new IllegalStateException("No free spots left");
        registeredCount++;
    }

    public void decrementRegistered() {
        if (registeredCount <= 0) return;
        registeredCount--;
    }

    // ===== Edit methods =====
    public void updateTitle(String newTitle) {
        if (newTitle != null && !newTitle.isBlank()) {
            this.title = newTitle.trim();
        }
    }

    public void updateDescription(String newDescription) {
        if (newDescription != null) {
            this.description = newDescription.trim();
        }
    }

    public void updateDateTime(LocalDateTime newDateTime) {
        if (newDateTime != null) {
            this.dateTime = newDateTime;
        }
    }

    public void updateCategory(Category newCategory) {
        this.category = (newCategory == null) ? Category.OTHER : newCategory;
    }

    public void updateCapacity(int newCapacity) {
        if (newCapacity <= 0)
            throw new IllegalArgumentException("Capacity must be > 0");
        if (newCapacity > venue.getCapacity())
            throw new IllegalArgumentException("Capacity cannot exceed venue capacity");
        if (newCapacity < registeredCount)
            throw new IllegalArgumentException("Capacity cannot be less than registered users");
        this.capacity = newCapacity;
    }

    @Override
    public String toString() {
        return title + " | " + category
                + " | " + dateTime
                + " | " + venue.getName()
                + " | " + registeredCount + "/" + capacity
                + (approved ? "" : " (PENDING)");
    }
}
