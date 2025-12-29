package com.molla.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;

    private  String phone;

    private String address;

    private String email;

    @ElementCollection
    private List<String> workingDays;

    private LocalTime openTime;
    private LocalTime closeTime;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne
    private Store store; //many branches belong to one store

    @OneToOne(cascade = CascadeType.REMOVE)
    private  User manager; //one branch has one manager

    @PreUpdate
    protected void onUpdate(){
        updatedAt=LocalDateTime.now();
    }
    @PrePersist
    protected void onCreate(){
        createdAt=LocalDateTime.now();
    }


}
