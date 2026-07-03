package gr.hua.dit.dras.dto;

import gr.hua.dit.dras.entities.Notification;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class NotificationDTO {
    private Integer id;
    private String title;
    private String message;
    private Boolean read;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public NotificationDTO() {}

    public NotificationDTO(Notification notification) {
        this.id = notification.getId();
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.read = notification.getIsRead();
        this.createdAt = notification.getCreatedAt();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
