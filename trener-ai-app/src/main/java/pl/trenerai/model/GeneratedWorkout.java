package pl.trenerai.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name="generated_workouts")
public class GeneratedWorkout {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="user_id") private AppUser user;
    private String name; private LocalDate plannedDate; @Column(columnDefinition = "TEXT") private String zwoContent; @Column(nullable=false) private Instant createdAt=Instant.now(); @Column(nullable=false, columnDefinition = "boolean default false") private boolean deleted=false;
    protected GeneratedWorkout(){} public GeneratedWorkout(AppUser user,String name,LocalDate date,String content){this.user=user;this.name=name;plannedDate=date;zwoContent=content;}
    public Long getId(){return id;} public String getName(){return name;} public LocalDate getPlannedDate(){return plannedDate;} public String getZwoContent(){return zwoContent;} public AppUser getUser(){return user;} public Instant getCreatedAt(){return createdAt;} public boolean isDeleted(){return deleted;} public void markDeleted(){deleted=true;}
}
